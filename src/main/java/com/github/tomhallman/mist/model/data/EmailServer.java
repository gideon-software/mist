/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2010 Tom Hallman
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
 * For more information, visit https://github.com/tomhallman/mist
 */

package com.github.tomhallman.mist.model.data;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.exceptions.EmailServerException;
import com.github.tomhallman.mist.model.EmailModel;
import com.github.tomhallman.mist.model.MessageModel;
import com.github.tomhallman.mist.preferences.Preferences;
import com.github.tomhallman.mist.util.Util;

public class EmailServer implements Cloneable {
    private static Logger log = LogManager.getLogger();

    public final static int DEFAULT_PORT = 993;
    public static final String NEW_NICKNAME = "New Email Server";

    private final static String PREF_PREFIX = "emailserver";
    public final static String PREF_ADDRESSES_IGNORE = "addresses.ignore";
    public final static String PREF_ADDRESSES_MY = "addresses.my";
    public final static String PREF_FOLDER = "folder";
    public final static String PREF_HOST = "host";
    public final static String PREF_PASSWORD = "password";
    public final static String PREF_PASSWORD_PROMPT = "password.prompt";
    public final static String PREF_PORT = "port";
    public final static String PREF_NICKNAME = "nickname";
    public final static String PREF_USERNAME = "username";
    public final static String PREF_TNT_USERID = "tnt.user.id";
    public final static String PREF_TNT_USERNAME = "tnt.user.username";

    // Import controls
    private boolean stopImporting = false;
    private boolean importComplete = false;

    // Server properties
    private int id;

    private String host;
    private String nickname;
    private String password;
    private boolean passwordPrompt;
    private String port;
    private String username;
    private Integer tntUserId;
    /**
     * Cached copy of user's Tnt username. Helps avoid expensive call to DB when viewing settings.
     * <p>
     * If tntUserId != 0, this should have a value.
     */
    private String tntUsername;
    private String[] ignoreAddresses;
    private String[] myAddresses;
    private String folderName;

    private Store store;
    private Folder folder;
    private int currentMessageNumber;
    private int totalMessages;

    public EmailServer(int id) {
        log.trace("EmailServer({})", id);
        setId(id);
        init();
    }

    public static String getPrefPrefix(int id) {
        return String.format("%s.%s", PREF_PREFIX, id);
    }

    public void addIgnoreAddress(String email) {
        log.trace("addIgnoreAddress({})", email);
        String[] newIgnoreAddresses = new String[ignoreAddresses.length + 1];
        System.arraycopy(ignoreAddresses, 0, newIgnoreAddresses, 0, ignoreAddresses.length);
        newIgnoreAddresses[newIgnoreAddresses.length] = email;
        setIgnoreAddresses(newIgnoreAddresses);
    }

    public void clearPreferences() {
        log.trace("clearPreferences()");
        MIST.getPrefs().setToDefaultIfContains(getPrefName("")); // Will clear all prefs matching this emailserver
    }

    public void connect(boolean selectFolder) throws EmailServerException {
        log.trace("{{}} connect()", getNickname());
        log.debug("{{}} Connecting to email server at '{}:{}'...", getNickname(), getHost(), getPort());

        Session sess = Session.getDefaultInstance(getConnectionProperties(), null);
        store = null;
        folder = null;
        currentMessageNumber = 0;
        totalMessages = 0;

        try {
            store = sess.getStore("imaps"); // IMAPS = port 993; TODO: Allow IMAP too
        } catch (NoSuchProviderException e) {
            throw new EmailServerException(e);
        }

        try {
            store.connect(getHost(), getUsername(), getPassword());
        } catch (MessagingException e) {
            // Important: if there is was failed email connection and the user had been prompted
            // for email, clear the password now. That way they'll be asked again next time.
            if (isPasswordPrompt())
                setPassword(null);
            store = null;
            throw new EmailServerException(e);
        }

        if (selectFolder) {
            try {
                folder = store.getFolder(getFolder());
                folder.open(Folder.READ_ONLY);
                totalMessages = folder.getMessageCount();
            } catch (MessagingException e) {
                folder = null;
                disconnect();
                throw new EmailServerException(e);
            }
        }
    }

    public void disconnect() {
        log.trace("{{}} disconnect()", getNickname());
        if (store != null) {
            try {
                store.close();
            } catch (MessagingException e) {
                log.warn("{{}} Unable to disconnect from message store", getNickname(), e);
            } finally {
                store = null;
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof EmailServer))
            return false;
        EmailServer server2 = (EmailServer) obj;
        return server2.getId() == id;
    }

    public EmailFolder[] getCompleteFolderList() {
        log.trace("{{}} getCompleteFolderList()", getNickname());
        if (store == null) {
            log.error("{{}} Not connected to email server", getNickname());
            return new EmailFolder[0];
        }
        EmailFolder[] emailFolders = null;
        try {
            Folder[] folders = store.getDefaultFolder().list("*");
            emailFolders = new EmailFolder[folders.length];
            for (int i = 0; i < folders.length; i++)
                emailFolders[i] = new EmailFolder(folders[i]);
            return emailFolders;
        } catch (MessagingException | IllegalStateException e) {
            log.error("{{}} Unable to retrieve folder list", getNickname());
            return new EmailFolder[0];
        }
    }

    public Properties getConnectionProperties() {
        log.trace("{{}} getConnectionProperties()", getNickname());
        Properties props = new Properties();
        props.setProperty("mail.host", getHost());
        props.setProperty("mail.port", getPort());
        props.setProperty("mail.user", getUsername());
        props.setProperty("mail.password", getPassword());

        // Don't verify server identity. This is required for self-signed
        // certificates, which missionaries may very well use ;)
        // Source: http://stackoverflow.com/a/5730201/1307022
        props.setProperty("mail.imaps.ssl.checkserveridentity", "false");
        props.setProperty("mail.imaps.ssl.trust", "*");

        return props;
    }

    public int getCurrentMessageNumber() {
        return currentMessageNumber;
    }

    public String getFolder() {
        return folderName;
    }

    public String getHost() {
        return host;
    }

    public int getId() {
        return id;
    }

    public String[] getIgnoreAddresses() {
        return ignoreAddresses;
    }

    public String[] getMyAddresses() {
        return myAddresses;
    }

    public javax.mail.Message getNextMessage() throws EmailServerException {
        log.trace("{{}} getNextMessage()", getNickname());
        try {
            return folder.getMessage(++currentMessageNumber);
        } catch (MessagingException e) {
            throw new EmailServerException(e);
        }
    }

    public String getNickname() {
        // This should never be null, as it's used even in logging for the email server
        if (nickname != null)
            return nickname;
        if (host != null)
            return host;
        return "ID:" + id;
    }

    public String getPassword() {
        return password;
    }

    public String getPort() {
        return port;
    }

    public String getPrefName(String name) {
        return String.format("%s.%s.%s", PREF_PREFIX, id, name);
    }

    public String getSelectedFolder() throws EmailServerException {
        log.trace("{{}} getSelectedFolder()", getNickname());
        if (folder == null)
            throw new EmailServerException("[" + getNickname() + "] Email folder is not selected");
        return folder.getName();
    }

    public Integer getTntUserId() {
        return tntUserId;
    }

    public String getTntUsername() {
        return tntUsername;
    }

    public int getTotalMessages() {
        return totalMessages;
    }

    public String getUsername() {
        return username;
    }

    public boolean hasNextMessage() {
        return currentMessageNumber < totalMessages;
    }

    public void init() {
        log.trace("{{}} init()", getNickname());

        // Close any old connections
        if (isConnected())
            disconnect();

        //
        // Load settings from preferences, providing reasonable defaults
        //

        Preferences prefs = MIST.getPrefs();
        setFolderName(prefs.getString(getPrefName(EmailServer.PREF_FOLDER)));
        setHost(prefs.getString(getPrefName(EmailServer.PREF_HOST)));
        setUsername(prefs.getString(getPrefName(EmailServer.PREF_USERNAME)));
        setNickname(prefs.getString(getPrefName(EmailServer.PREF_NICKNAME)));
        setTntUserId(prefs.getInt(getPrefName(EmailServer.PREF_TNT_USERID)));
        setTntUsername(prefs.getString(getPrefName(EmailServer.PREF_TNT_USERNAME)));
        setIgnoreAddresses(prefs.getStrings(getPrefName(EmailServer.PREF_ADDRESSES_IGNORE)));
        setMyAddresses(prefs.getStrings(getPrefName(EmailServer.PREF_ADDRESSES_MY)));

        // Set default password prompt
        prefs.setDefault(getPrefName(EmailServer.PREF_PASSWORD_PROMPT), true);
        setPasswordPrompt(prefs.getBoolean(getPrefName(EmailServer.PREF_PASSWORD_PROMPT)));
        setPassword(passwordPrompt ? "" : prefs.getString(getPrefName(EmailServer.PREF_PASSWORD)));

        // Set default port
        prefs.setDefault(getPrefName(EmailServer.PREF_PORT), DEFAULT_PORT);
        setPort(prefs.getString(getPrefName(EmailServer.PREF_PORT)));

        // Initialize non-preference data
        folder = null;
        store = null;
        currentMessageNumber = 0;
        totalMessages = 0;

        stopImporting = false;
        importComplete = false;
    }

    public boolean isConnected() {
        return store != null;
    }

    public boolean isEmailInIgnoreList(String email) {
        log.trace("isEmailInIgnoreList({})", email);
        String[] ignoreList = MIST.getPrefs().getStrings(getPrefName(PREF_ADDRESSES_IGNORE));
        return EmailModel.isEmailInList(email, ignoreList);
    }

    public boolean isImportComplete() {
        return importComplete;
    }

    public boolean isPasswordNeeded() {
        log.trace("isPasswordNeeded()");
        // If there is no email password prompt, no password is needed
        if (!isPasswordPrompt() && !getPassword().isEmpty())
            return false;

        // If there is an email password prompt, do we already have the password
        // from a previous successful connection? (email password is not empty)
        // Note: A failed prompted connection sets the password to empty as well
        if (getPassword().isEmpty())
            return true;
        else
            return false;
    }

    public boolean isPasswordPrompt() {
        return passwordPrompt;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
        if (folderName != null)
            MIST.getPrefs().setValue(getPrefName(PREF_FOLDER), folderName);
    }

    public void setHost(String host) {
        this.host = host;
        if (host != null)
            MIST.getPrefs().setValue(getPrefName(PREF_HOST), host);
    }

    private void setId(int id) {
        this.id = id;
    }

    public void setIgnoreAddresses(String[] ignoreAddresses) {
        this.ignoreAddresses = ignoreAddresses;
        if (ignoreAddresses != null)
            MIST.getPrefs().setValues(getPrefName(PREF_ADDRESSES_IGNORE), ignoreAddresses);
    }

    public void setMyAddresses(String[] myAddresses) {
        this.myAddresses = myAddresses;
        if (myAddresses != null)
            MIST.getPrefs().setValues(getPrefName(PREF_ADDRESSES_MY), myAddresses);
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
        if (nickname != null)
            MIST.getPrefs().setValue(getPrefName(PREF_NICKNAME), nickname);
    }

    public void setPassword(String password) {
        this.password = password;
        if (password != null)
            MIST.getPrefs().setValue(getPrefName(PREF_PASSWORD), password);
    }

    public void setPasswordPrompt(boolean prompt) {
        this.passwordPrompt = prompt;
        MIST.getPrefs().setValue(getPrefName(PREF_PASSWORD_PROMPT), prompt);
    }

    public void setPort(String port) {
        this.port = port;
        MIST.getPrefs().setValue(getPrefName(PREF_PORT), port);
    }

    public void setTntUserId(Integer tntUserId) {
        this.tntUserId = tntUserId;
        if (tntUserId != null)
            MIST.getPrefs().setValue(getPrefName(PREF_TNT_USERID), tntUserId);
    }

    public void setTntUsername(String tntUsername) {
        this.tntUsername = tntUsername;
        if (tntUsername != null)
            MIST.getPrefs().setValue(getPrefName(PREF_TNT_USERNAME), tntUsername);
    }

    public void setUsername(String username) {
        this.username = username;
        if (username != null)
            MIST.getPrefs().setValue(getPrefName(PREF_USERNAME), username);
    }

    /**
     * Starts the email import service for this server.
     * 
     * @param shell
     *            the shell for notifying the user of the connection taking place; if null, no notification will take
     *            place
     */
    public void startImportService(Shell shell) {
        log.trace("{{}} startImportService({})", getNickname(), shell);

        stopImporting = false;
        importComplete = false;
        Util.connectToEmailServer(shell, this, true);

        Thread importThread = new Thread() {
            @Override
            public void run() {
                log.trace("=== Email Server '{}' Import Service Started ===", nickname);
                log.info("{{}} Folder '{}' contains {} messages", nickname, folderName, totalMessages);

                while (hasNextMessage() && !stopImporting) {
                    log.debug(
                        "{{}} Processing message {}/{}",
                        nickname,
                        currentMessageNumber + 1, // +1 because we're ABOUT to get it in getNextMessage()
                        totalMessages);

                    Message message = null;
                    try {
                        // Add Message to message queue
                        message = getNextMessage();
                        MessageModel.addMessage(new EmailMessage(EmailServer.this, message));
                    } catch (EmailServerException e) {
                        Display.getDefault().syncExec(new Runnable() {
                            @Override
                            public void run() {
                                String msg = String.format(
                                    "Can't retrieve message %s on server '%s'",
                                    currentMessageNumber,
                                    nickname);
                                Util.reportError("Email server error", msg, e);
                            }
                        });
                    }
                }

                importComplete = true;
                disconnect();

                if (totalMessages == 0) {
                    String msg = String.format("'%s' had no messages to import.", nickname);
                    log.info(msg);
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            MessageDialog.openInformation(MIST.getView().getShell(), "Import complete", msg);
                        }
                    });
                }

                EmailModel.serverComplete();
                log.trace("=== Email Server '{}' Import Service Stopped ===", nickname);

            } // run()
        };
        if (isConnected()) {
            importThread.setName(String.format("ESImport%s", id));
            importThread.start();
        } else
            importComplete = true;
    }

    public void stopImportService() {
        stopImporting = true;
    }

    @Override
    public String toString() {
        return String.format("EmailServer {%s}", getNickname());
    }

}
