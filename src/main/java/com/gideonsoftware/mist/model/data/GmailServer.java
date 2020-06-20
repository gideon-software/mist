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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

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
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.LabelColor;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import com.google.api.services.gmail.model.ModifyThreadRequest;
import com.google.api.services.oauth2.Oauth2Scopes;

/**
 * 
 */
public class GmailServer extends EmailServer implements PropertyChangeListener {
    private static Logger log = LogManager.getLogger();

    // Preferences
    public final static String PREF_LABEL_ID = "label.id";
    public final static String PREF_LABEL_NAME = "label.name";
    public final static String PREF_LABEL_REMOVE_AFTER_IMPORT = "label.removeafterimport";
    public final static String PREF_UNIQUE_ID = "uniqueid";

    public final static String NEW_NICKNAME = "Gmail";

    // Google authorization data
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE_PATH = "google/client_secrets.json";
    private static final List<String> SCOPES = Arrays.asList(
        GmailScopes.GMAIL_MODIFY, // Needed to read mail & modify labels
        "openid", // Needed to get id_token for user
        Oauth2Scopes.USERINFO_EMAIL); // Needed to get user's email
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

    private List<Message> messages;

    private String labelId;
    private String labelName;

    private Gmail gmailService;

    private boolean labelRemoveAfterImport = true;
    private String uniqueId = "";

    /**
     * As of 2020-06, there is either a bug in MIST or in Gmail such that removing all labels from a thread does NOT
     * remove the label from the thread itself. This results in trying to reimport the same messages over and over.
     * 
     * Our solution is to maintain a map of thread IDs which contain a set of message IDs. As each message is removed,
     * check to see if that thread's message set is empty. If it is, remove the label from the thread as well.
     */
    private HashMap<String, HashSet<String>> threadMap = null;

    public GmailServer(int id) {
        super(id, EmailServer.TYPE_GMAIL);

        gmailService = null;

        //
        // Load values from preferences & set defaults
        //

        Preferences prefs = MIST.getPrefs();
        labelId = prefs.getString(getPrefName(PREF_LABEL_ID));
        labelName = prefs.getString(getPrefName(PREF_LABEL_NAME));

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
    // @see https://stackoverflow.com/a/13016081/1307022
    private Credential authorize() throws IOException {
        log.trace("{{}} authorize()", getNickname());

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT,
            JSON_FACTORY,
            CLIENT_SECRETS,
            SCOPES).setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline")
                // We also want to get the id_token response to get the username
                // id_token is an OpenID Connect ID, which is a JSON Web Token (JWT)
                .setCredentialCreatedListener(new AuthorizationCodeFlow.CredentialCreatedListener() {
                    @Override
                    public void onCredentialCreated(
                        Credential credential,
                        TokenResponse tokenResponse) throws IOException {
                        log.trace(
                            "{{}} CredentialCreatedListener.onCredentialCreated({},{})",
                            getNickname(),
                            credential,
                            tokenResponse);
                        storeIdTokenValues(credential, tokenResponse);
                    }
                }).addRefreshListener(new CredentialRefreshListener() {
                    @Override
                    public void onTokenErrorResponse(
                        Credential credential,
                        TokenErrorResponse tokenErrorResponse) throws IOException {
                        log.trace(
                            "{{}} CredentialRefreshListener.onTokenErrorResponse({},{})",
                            getNickname(),
                            credential,
                            tokenErrorResponse);
                        // credential = null; // TODO: Something's wrong here! Handle this better!
                    }

                    @Override
                    public void onTokenResponse(Credential credential, TokenResponse tokenResponse) throws IOException {
                        log.trace(
                            "{{}} CredentialRefreshListener.onTokenResponse({},{})",
                            getNickname(),
                            credential,
                            tokenResponse);
                        storeIdTokenValues(credential, tokenResponse);
                    }
                }).build();

        // Authorize
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize(uniqueId);

        // Get the access token
        credential.refreshToken(); // Don't need to call this if it hasn't expired, but it doesn't hurt to do so
        return credential;
    }

    @Override
    public void connect() throws EmailServerException {
        log.trace("{{}} connect()", getNickname());

        if (isConnected()) {
            log.trace("{{}} Already connected", getNickname());
            return;
        }

        log.debug("{{}} Connecting to Gmail...", getNickname());

        messages = null;
        currentMessageNumber = 0;
        totalMessages = 0;

        Credential credential = null;
        try {
            credential = authorize();
        } catch (TokenResponseException e) {
            throw new EmailServerException(e.getDetails().getErrorDescription(), e);
        } catch (IOException e) {
            throw new EmailServerException(e);
        }

        // Build a new authorized API client service
        gmailService = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential) //
            .setApplicationName(MIST.getAppNameWithVersion()).build();

        // Add property change listeners
        TntDb.addPropertyChangeListener(this);
        HistoryModel.addPropertyChangeListener(this);

        // Init threadMap; see note
        threadMap = new HashMap<String, HashSet<String>>();
    }

    public void createLabel(String labelName) throws EmailServerException {
        log.trace("{{}} createLabel({})", getNickname(), labelName);

        if (labelName == null || labelName.isBlank())
            throw new EmailServerException("Label name cannot be blank");

        if (!isConnected())
            throw new EmailServerException(String.format("{%s} Not connected", getNickname()));

        // Create the new label
        Label label = new Label().setName(labelName).setLabelListVisibility("labelShow").setMessageListVisibility(
            "show");
        LabelColor labelColor = new LabelColor();
        labelColor.setTextColor("#000000");
        labelColor.setBackgroundColor("#16a766");
        label.setColor(labelColor);
        try {
            // https://developers.google.com/gmail/api/v1/reference/users/labels/create
            label = gmailService.users().labels().create("me", label).execute();
            setLabelId(label.getId());
            setLabelName(label.getName());
        } catch (IOException e) {
            throw new EmailServerException(e);
        }
    }

    @Override
    public void disconnect() {
        log.trace("{{}} disconnect()", getNickname());
        gmailService = null;
        TntDb.removePropertyChangeListener(this);
        HistoryModel.removePropertyChangeListener(this);
    }

    public String getLabelId() {
        if (labelId == null)
            return "";
        return labelId;
    }

    public List<Label> getLabelList() throws EmailServerException {
        log.trace("{{}} getLabelList()", getNickname());

        ListLabelsResponse listResponse;
        try {
            listResponse = gmailService.users().labels().list("me").execute();
        } catch (IOException e) {
            throw new EmailServerException(e);
        }
        List<Label> labelList = listResponse.getLabels();
        labelList.sort((label1, label2) -> label1.getName().compareTo(label2.getName()));
        return labelList;
    }

    public String getLabelName() {
        if (labelName == null)
            return "";
        return labelName;
    }

    @Override
    public EmailMessage getNextMessage() throws EmailServerException {
        log.trace("{{}} getNextMessage()", getNickname());

        Message message = messages.get(currentMessageNumber++);

        // First load the full message, as we've thus far we only have a snippet
        try {
            message = gmailService.users().messages().get("me", message.getId()).setFormat("full").execute();
        } catch (IOException e) {
            throw new EmailServerException(e);
        }

        // Use the GmailMessage class to parse the message
        return new GmailMessage(GmailServer.this, message);
    }

    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public boolean hasNextMessage() {
        if (messages == null)
            return false;
        return currentMessageNumber < messages.size();
    }

    @Override
    public boolean isConnected() {
        return gmailService != null;
    }

    public boolean isLabelRemoveAfterImport() {
        return labelRemoveAfterImport;
    }

    @Override
    public void loadMessageList() throws EmailServerException {
        log.trace("{{}} loadMessageList()", getNickname());

        log.debug("{{}} Retrieving messages with label '{}'", getNickname(), getLabelName());
        List<String> labelIds = Arrays.asList(getLabelId());
        List<com.google.api.services.gmail.model.Thread> threads = new ArrayList<com.google.api.services.gmail.model.Thread>();
        messages = new ArrayList<Message>();
        try {

            /*
             * When you label an email in Gmail, that email and all emails in that thread UP TO THAT POINT are
             * labeled, but not subsequent messages that may come in (even though it appears otherwise in the Gmail
             * interface.) So, we must get the messages with this label AND all other messages in their threads as well.
             */

            // Get all threads with this label
            ListThreadsResponse listThreadsResponse = gmailService.users().threads().list("me").setLabelIds(labelIds)
                .execute();
            while (listThreadsResponse.getThreads() != null) {
                threads.addAll(listThreadsResponse.getThreads());
                String pageToken = listThreadsResponse.getNextPageToken();
                if (pageToken != null) {
                    listThreadsResponse = gmailService.users().threads().list("me").setLabelIds(labelIds) //
                        .setPageToken(pageToken).execute();
                } else {
                    break;
                }
            }

            // Get all messages associated with the threads
            for (com.google.api.services.gmail.model.Thread minThread : threads) {
                com.google.api.services.gmail.model.Thread fullThread = gmailService.users().threads().get(
                    "me",
                    minThread.getId()).setFormat("minimal").execute();
                messages.addAll(fullThread.getMessages());

                // See note on threadMap
                HashSet<String> messageIdSet = new HashSet<String>(fullThread.getMessages().size());
                for (Message message : fullThread.getMessages())
                    messageIdSet.add(message.getId());
                threadMap.put(minThread.getId(), messageIdSet);
            }

        } catch (IOException e) {
            throw new EmailServerException(e);
        }

        totalMessages = messages.size();
        currentMessageNumber = 0;
        log.debug("{{}} Retrieved {} message(s) from {} thread(s)", getNickname(), totalMessages, threads.size());
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
                    try {
                        removeLabel((GmailMessage) history.getMessageSource());
                    } catch (EmailServerException e) {
                        Util.reportError("Gmail error", "Could not remove label", e);
                    }
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
                    try {
                        removeLabel((GmailMessage) msg);
                    } catch (EmailServerException e) {
                        Util.reportError("Gmail error", "Could not remove label", e);
                    }
                }
            }
        }

    }

    public void removeLabel(GmailMessage gmailMessage) throws EmailServerException {
        log.trace("{{}} removeLabel({})", getNickname(), gmailMessage);
        ModifyMessageRequest modRequest = new ModifyMessageRequest().setRemoveLabelIds(Arrays.asList(getLabelId()));
        try {
            gmailService.users().messages().modify("me", gmailMessage.getMessage().getId(), modRequest).execute();

            // See note on threadMap
            HashSet<String> messageSet = threadMap.get(gmailMessage.getMessage().getThreadId());
            messageSet.remove(gmailMessage.getMessage().getId());
            if (messageSet.isEmpty()) {
                log.debug("{{}} removing thread label from thread containing message: {}", getNickname(), gmailMessage);
                ModifyThreadRequest modRequestThr = new ModifyThreadRequest().setRemoveLabelIds(
                    Arrays.asList(getLabelId()));
                gmailService.users().threads().modify("me", gmailMessage.getMessage().getThreadId(), modRequestThr)
                    .execute();
            }
        } catch (IOException e) {
            throw new EmailServerException(e);
        }
    }

    public void setLabelId(String labelId) {
        this.labelId = labelId;
        if (labelId != null)
            MIST.getPrefs().setValue(getPrefName(PREF_LABEL_ID), labelId);
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
        if (labelName != null)
            MIST.getPrefs().setValue(getPrefName(PREF_LABEL_NAME), labelName);
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
        log.trace("{{}} storeIdTokenValues({},{})", getNickname(), credential, tokenResponse);

        // Note: can't cast TokenResponse to GoogleTokenResponse here
        String idTokenString = (String) tokenResponse.get("id_token");
        GoogleIdToken idToken = GoogleIdToken.parse(JSON_FACTORY, idTokenString);

        try {
            if (!TOKEN_VERIFIER.verify(idToken))
                throw new EmailServerException("Google security token not verified: " + idToken.toString());
        } catch (EmailServerException | GeneralSecurityException e) {
            // credential = null; // TODO
            Util.reportError("Security error", "Invalid Google token!", e);
        }
        setUsername(idToken.getPayload().getEmail());
    }

}
