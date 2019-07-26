/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2019 Gideon Software
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For more information, visit https://www.gideonsoftware.com
 */

package com.gideonsoftware.mist.model.data;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.UUID;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.exceptions.EmailServerException;
import com.gideonsoftware.mist.model.HistoryModel;
import com.gideonsoftware.mist.preferences.Preferences;
import com.gideonsoftware.mist.tntapi.TntDb;
import com.gideonsoftware.mist.tntapi.entities.History;
import com.gideonsoftware.mist.util.Util;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.sun.mail.gimap.GmailMessage;
import com.sun.mail.gimap.GmailMsgIdTerm;
import com.sun.mail.gimap.GmailThrIdTerm;

/**
 * 
 */
public class GmailServer extends EmailServer implements PropertyChangeListener {
    private static Logger log = LogManager.getLogger();

    // Preferences
    public final static String PREF_LABEL_REMOVE_AFTER_IMPORT = "label.removeafterimport";
    public final static String PREF_UNIQUE_ID = "uniqueid";

    // Google authorization data
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE_PATH = "google/client_secrets.json";
    private static final List<String> SCOPES = Arrays.asList(
        "https://mail.google.com/", // Needed for IMAP access
        "openid", // Needed to get id_token for user
        "https://www.googleapis.com/auth/userinfo.email"); // Needed to get user's email
    private static final HttpTransport HTTP_TRANSPORT;
    private static final GoogleClientSecrets CLIENT_SECRETS;
    private static final FileDataStoreFactory DATA_STORE_FACTORY;
    private static final GoogleIdTokenVerifier TOKEN_VERIFIER;

    static {
        HttpTransport transport = null;
        try {
            transport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (IOException | GeneralSecurityException e) {
            Util.reportError("Gmail Server Error", "Error created Google HTTP transport", e);
        }
        HTTP_TRANSPORT = transport;

        GoogleClientSecrets secrets = null;
        try {
            //
            secrets = GoogleClientSecrets.load(
                JSON_FACTORY,
                new InputStreamReader(GmailServer.class.getResourceAsStream(CREDENTIALS_FILE_PATH)));
        } catch (IOException e) {
            Util.reportError("Gmail Server Error", "Error loading Google client secrets", e);
        }
        CLIENT_SECRETS = secrets;

        // Disable unhelpful warning (on Windows) while loading data store factory
        // https://stackoverflow.com/questions/30634827/warning-unable-to-change-permissions-for-everybody#comment57411339_34823324
        final java.util.logging.Logger buggyLogger = java.util.logging.Logger.getLogger(
            FileDataStoreFactory.class.getName());
        buggyLogger.setLevel(java.util.logging.Level.SEVERE);

        FileDataStoreFactory dataStoreFactory = null;
        try {
            Path secureStoreDir = Paths.get(MIST.getAppConfDir());
            dataStoreFactory = new FileDataStoreFactory(secureStoreDir.toFile());
        } catch (IOException e) {
            Util.reportError("Gmail Server Error", "Error loading data store factory", e);
        }
        DATA_STORE_FACTORY = dataStoreFactory;

        TOKEN_VERIFIER = new GoogleIdTokenVerifier.Builder(HTTP_TRANSPORT, JSON_FACTORY).setAudience(
            Collections.singletonList(CLIENT_SECRETS.getDetails().getClientId())).build();
    }

    private Long[] messageIds;
    private Folder allMailFolder;

    private boolean labelRemoveAfterImport = true;
    private String uniqueId = "";

    public GmailServer(int id) {
        super(id, EmailServer.TYPE_GMAIL);
        allMailFolder = null;
        messageIds = new Long[0];

        //
        // Load values from preferences & set defaults
        //

        Preferences prefs = MIST.getPrefs();

        // Set default label-remove-after-import
        prefs.setDefault(getPrefName(PREF_LABEL_REMOVE_AFTER_IMPORT), true);
        labelRemoveAfterImport = prefs.getBoolean(getPrefName(PREF_LABEL_REMOVE_AFTER_IMPORT));

        // Get unique, non-changing server ID (for credential storage)
        uniqueId = prefs.getString(getPrefName(PREF_UNIQUE_ID));
        if (uniqueId.isEmpty()) {
            // Generate a new unique ID (note: max len seems to be 30 for DataStoreFactory)
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 30);
            // Could be improved by verifying uniqueness among other servers
            setUniqueId(uuid);
        }
    }

    // @see https://stackoverflow.com/questions/49354891/how-do-i-get-the-user-id-token-from-a-credential-object
    private Credential authorize() throws IOException {
        log.trace("{{}} authorize()", getNickname());

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT,
            JSON_FACTORY,
            CLIENT_SECRETS,
            SCOPES).setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline")
                // We also want to get the id_token response to get the username
                // id_token is an OpenID Connect ID, which is a JSON Web Token (JWT)
                // See https://stackoverflow.com/a/13016081/1307022
                .setCredentialCreatedListener(new AuthorizationCodeFlow.CredentialCreatedListener() {
                    @Override
                    public void onCredentialCreated(
                        Credential credential,
                        TokenResponse tokenResponse) throws IOException {
                        storeIdTokenValues(credential, tokenResponse);
                    }
                }).addRefreshListener(new CredentialRefreshListener() {
                    @Override
                    public void onTokenErrorResponse(
                        Credential credential,
                        TokenErrorResponse tokenErrorResponse) throws IOException {
                        credential = null; // Something's wrong here! Maybe need to handle this better...?
                        log.error(tokenErrorResponse);
                    }

                    @Override
                    public void onTokenResponse(Credential credential, TokenResponse tokenResponse) throws IOException {
                        storeIdTokenValues(credential, tokenResponse);
                    }
                }).build();

        // Authorize
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize(uniqueId);

        // Get the access token
        // credential.refreshToken(); // Don't need to call this if it hasn't expired, but it doesn't hurt to do so
        return credential;
    }

    @Override
    public void closeFolders() {
        log.trace("{{}} closeFolders()", getNickname());
        super.closeFolders();
        if (store != null) {
            try {
                if (allMailFolder != null && allMailFolder.isOpen())
                    allMailFolder.close();
            } catch (MessagingException e) {
                log.warn("{{}} Unable to close folder '{}'", getNickname(), allMailFolder.getName(), e);
            } finally {
                allMailFolder = null;
            }
        }
    }

    /**
     * @see https://javaee.github.io/javamail/OAuth2
     */
    @Override
    public void connect() throws EmailServerException {
        log.trace("{{}} connect()", getNickname());

        if (isConnected()) {
            log.trace("{{}} Already connected", getNickname());
            return;
        }

        log.debug("{{}} Connecting to Gmail...", getNickname());

        store = null;
        folder = null;
        currentMessageNumber = 0;
        totalMessages = 0;

        Properties props = new Properties();
        props.put("mail.gimap.ssl.enable", "true");
        props.put("mail.gimap.auth.mechanisms", "XOAUTH2");
        Session sess = Session.getInstance(props, null);

        try {
            store = sess.getStore("gimap"); // Defaults to using SSL to connect to "imap.gmail.com"
        } catch (NoSuchProviderException e) {
            throw new EmailServerException(e);
        }

        Credential credential = null;
        try {
            credential = authorize();
        } catch (TokenResponseException e) {
            closeStore();
            throw new EmailServerException(e.getDetails().getErrorDescription(), e);
        } catch (IOException e) {
            closeStore();
            throw new EmailServerException(e);
        }

        try {
            store.connect("imap.gmail.com", username, credential.getAccessToken());
        } catch (MessagingException e) {
            closeStore();
            throw new EmailServerException(e);
        }

        // Add property change listeners
        TntDb.addPropertyChangeListener(this);
        HistoryModel.addPropertyChangeListener(this);
    }

    @Override
    public void disconnect() {
        log.trace("{{}} disconnect()", getNickname());
        super.disconnect();
        TntDb.removePropertyChangeListener(this);
        HistoryModel.removePropertyChangeListener(this);
    }

    @Override
    public Message getNextMessage() throws EmailServerException {
        log.trace("{{}} getNextMessage()", getNickname());
        Long msgId = messageIds[currentMessageNumber++];
        try {
            return allMailFolder.search(new GmailMsgIdTerm(msgId))[0];
        } catch (MessagingException e) {
            throw new EmailServerException(e);
        }
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public boolean isLabelRemoveAfterImport() {
        return labelRemoveAfterImport;
    }

    @Override
    public void loadMessageList() throws EmailServerException {
        log.trace("{{}} loadMessageList()", getNickname());
        try {
            // When you label an email in Gmail, that email and all emails in that thread UP TO THAT POINT are
            // labeled, but not subsequent messages (even though it looks like it in the Gmail interface.
            // Thus, we must get not only the messages in this folder, but all messages in their threads as well.

            // First, get all the thread IDs for the messages in our folder
            TreeSet<Long> thrIdSet = new TreeSet<Long>();
            for (Message msg : folder.getMessages()) {
                long thrId = ((GmailMessage) msg).getThrId();
                if (thrIdSet.add(thrId))
                    log.trace("Adding Gmail thread ID " + thrId);
            }

            // Now, go back through the thread IDs and get all the messages
            // First open the All Mail Folder
            openAllMailFolder();

            TreeSet<Long> msgIdSet = new TreeSet<Long>();
            // For each thread ID
            for (long thrId : thrIdSet) {
                // Find all messages with that thread ID
                for (Message msg : allMailFolder.search(new GmailThrIdTerm(thrId))) {
                    GmailMessage gMsg = (GmailMessage) msg;
                    // Try to add them to our message set
                    if (msgIdSet.add(gMsg.getMsgId())) {
                        log.trace(
                            String.format(
                                "Adding Gmail message ID %s from thread ID %s",
                                gMsg.getMsgId(),
                                gMsg.getThrId()));
                    }
                }
            }
            messageIds = msgIdSet.toArray(new Long[0]);
            totalMessages = messageIds.length;
            currentMessageNumber = 0;
        } catch (MessagingException e) {
            throw new EmailServerException(e);
        }
    }

    public void openAllMailFolder() throws EmailServerException {
        log.trace("{{}} openAllMailFolder()", getNickname());

        if (store == null || !store.isConnected()) {
            throw new EmailServerException(String.format("{%s} Store not available", getNickname()));
        }

        if (allMailFolder != null && allMailFolder.isOpen()) {
            log.trace("{{}} Folder 'All Mail' already open", getNickname());
            return;
        }

        try {
            allMailFolder = store.getFolder("[Gmail]/All Mail");
            // We eeed to be able to "write" to remove labels;
            // Note that for GmailServer, all email messages are loaded from allMailFolder, not folder.
            // So folder can be READ_ONLY, but allMailFolder must be READ_WRITE.
            allMailFolder.open(Folder.READ_WRITE);
        } catch (MessagingException e) {
            allMailFolder = null;
            throw new EmailServerException(e);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        log.trace("{{}} propertyChange({})", getNickname(), event);

        if (TntDb.PROP_HISTORY_PROCESSED.equals(event.getPropertyName())) {
            // History has been processed

            // If we're to remove labels after import
            if (isLabelRemoveAfterImport()) {
                History history = (History) event.getNewValue();
                // And this is the history's source server
                // And the history is added/exists
                if (getId() == history.getMessageSource().getSourceId()
                    && (history.getStatus() == History.STATUS_ADDED || history.getStatus() == History.STATUS_EXISTS)) {
                    // Remove the label
                    removeLabel(history.getMessageSource());
                }
            }

        } else if (HistoryModel.PROP_MESSAGE_IGNORED.equals(event.getPropertyName())) {
            // A message has been ignored

            // If we're to remove labels after import (and also when messages are ignored, btw!)
            if (isLabelRemoveAfterImport()) {
                MessageSource msg = (MessageSource) event.getNewValue();
                // And this is the history's source server
                if (getId() == msg.getSourceId()) {
                    // Remove the label
                    removeLabel(msg);
                }
            }
        }

    }

    public void removeLabel(MessageSource messageSource) {
        log.trace("{{}} removeLabel()", getNickname());
        Message msg = ((EmailMessage) messageSource).getMessage();
        GmailMessage gMsg = (GmailMessage) msg;
        String[] labels = { folderName };
        try {
            gMsg.setLabels(labels, false);
        } catch (MessagingException e) {
            log.error("Unable to remove label '{}' from message '{}'", folderName, messageSource, e);
        }
    }

    public void setLabelRemoveAfterImport(boolean labelRemoveAfterImport) {
        this.labelRemoveAfterImport = labelRemoveAfterImport;
        MIST.getPrefs().setValue(getPrefName(PREF_LABEL_REMOVE_AFTER_IMPORT), labelRemoveAfterImport);
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
        if (uniqueId != null && !uniqueId.isEmpty())
            MIST.getPrefs().setValue(getPrefName(PREF_UNIQUE_ID), uniqueId);
    }

    private void storeIdTokenValues(Credential credential, TokenResponse tokenResponse) throws IOException {
        GoogleTokenResponse googleTokenResponse = (GoogleTokenResponse) tokenResponse;
        GoogleIdToken idToken = googleTokenResponse.parseIdToken();
        // https://developers.google.com/identity/sign-in/web/backend-auth
        // https://developers.google.com/api-client-library/java/google-api-java-client/reference/1.20.0/jdiff/Google_API_Client_Library_for_Java_1.20.0/com/google/api/client/googleapis/auth/oauth2/GoogleTokenResponse
        try {
            if (!TOKEN_VERIFIER.verify(idToken))
                throw new EmailServerException("Google security token not verified: " + idToken.toString());
        } catch (EmailServerException | GeneralSecurityException e) {
            credential = null; // Don't allow further use
            Util.reportError("Security error", "Invalid Google token!", e);
        }
        setUsername(idToken.getPayload().getEmail());
    }

}
