/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2010 Gideon Software
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

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.exceptions.EmailServerException;
import com.gideonsoftware.mist.model.EmailModel;
import com.gideonsoftware.mist.model.MessageModel;
import com.gideonsoftware.mist.preferences.Preferences;
import com.gideonsoftware.mist.util.Util;

public abstract class EmailServer implements Cloneable {
    private static Logger log = LogManager.getLogger();

    private final static String PREF_PREFIX = "emailserver";
    public final static String PREF_ADDRESSES_IGNORE = "addresses.ignore";
    public final static String PREF_ADDRESSES_MY = "addresses.my";
    public final static String PREF_ENABLED = "enabled";
    public final static String PREF_FOLDER = "folder";
    public final static String PREF_NICKNAME = "nickname";
    public final static String PREF_USERNAME = "username";
    public final static String PREF_TNT_USERID = "tnt.user.id";
    public final static String PREF_TNT_USERNAME = "tnt.user.username";
    public final static String PREF_TYPE = "type";

    public final static String TYPE_IMAP = "imap";
    public final static String TYPE_GMAIL = "gmail";

    // Defaults
    public static final String NEW_NICKNAME = "New Email Server";

    // Import controls
    private boolean stopImporting;
    /**
     * True if an import has been started via startImportService() and completed (whether it "worked" or not.)
     * False otherwise.
     */
    private boolean importComplete;

    // Server properties
    private int id;

    protected boolean enabled;
    protected String nickname;
    protected String username;
    protected Integer tntUserId;
    /**
     * Cached copy of user's Tnt username. Helps avoid expensive call to DB when viewing settings.
     * <p>
     * If tntUserId != 0, this should have a value.
     */
    protected String tntUsername;
    protected String type;
    protected String[] ignoreAddresses;
    protected String[] myAddresses;
    protected String folderName;

    protected Store store;
    protected Folder folder;
    protected int currentMessageNumber;
    protected int totalMessages;

    public EmailServer(int id, String type) {
        log.trace("EmailServer({})", id);
        setId(id);
        setType(type);

        // Non-preference initialization
        folder = null;
        store = null;
        currentMessageNumber = 0;
        totalMessages = 0;

        stopImporting = false;
        importComplete = false;

        //
        // Load values from preferences & set defaults
        //

        Preferences prefs = MIST.getPrefs();
        prefs.setDefault(getPrefName(PREF_ENABLED), true);

        enabled = prefs.getBoolean(getPrefName(PREF_ENABLED));
        folderName = prefs.getString(getPrefName(PREF_FOLDER));
        username = prefs.getString(getPrefName(PREF_USERNAME));
        nickname = prefs.getString(getPrefName(PREF_NICKNAME));
        tntUserId = prefs.getInt(getPrefName(PREF_TNT_USERID));
        tntUsername = prefs.getString(getPrefName(PREF_TNT_USERNAME));
        ignoreAddresses = prefs.getStrings(getPrefName(PREF_ADDRESSES_IGNORE));
        myAddresses = prefs.getStrings(getPrefName(PREF_ADDRESSES_MY));

        String prefType = prefs.getString(getPrefName(PREF_TYPE));
        if (!type.equals(prefType)) {
            log.warn(
                "{{}} Configuration error: email server initialized to type '{}' but preferences are for type '{}'",
                getNickname(),
                type,
                prefType);
        }
    }

    public static String getPrefName(String name, int serverId) {
        return String.format("%s.%s.%s", PREF_PREFIX, serverId, name);
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
        log.trace("{{}} clearPreferences()", getNickname());
        MIST.getPrefs().setToDefaultIfContains(getPrefName("")); // Will clear all prefs matching this emailserver
    }

    public void closeFolders() {
        log.trace("{{}} closeFolders()", getNickname());
        if (store != null) {
            try {
                if (folder != null && folder.isOpen())
                    folder.close();
            } catch (MessagingException e) {
                log.warn("{{}} Unable to close folder '{}'", getNickname(), folder.getName(), e);
            } finally {
                folder = null;
            }
        }
    }

    public void closeStore() {
        log.trace("{{}} closeStore()", getNickname());
        if (store != null) {
            try {
                store.close();
            } catch (MessagingException e) {
                log.warn("{{}} Unable to close store", getNickname(), e);
            } finally {
                store = null;
            }
        }
    }

    public abstract void connect() throws EmailServerException;

    public void disconnect() {
        log.trace("{{}} disconnect()", getNickname());
        closeFolders();
        closeStore();
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

    public int getCurrentMessageNumber() {
        return currentMessageNumber;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getFolderWord() {
        if (TYPE_GMAIL.equals(type))
            return "Label";
        return "Folder";
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

    public abstract Message getNextMessage() throws EmailServerException;

    public String getNickname() {
        // This should never be null, as it's used even in logging for the email server
        if (nickname != null)
            return nickname;
        return "ID:" + id;
    }

    public String getPrefName(String name) {
        return getPrefName(name, id);
    }

    public String getSelectedFolder() throws EmailServerException {
        log.trace("{{}} getSelectedFolder()", getNickname());
        if (folder == null)
            throw new EmailServerException(String.format("{%s} Email folder is not selected", getNickname()));
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

    public String getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public boolean hasNextMessage() {
        return currentMessageNumber < totalMessages;
    }

    public boolean isConnected() {
        return store != null;
    }

    public boolean isEmailInIgnoreList(String email) {
        log.trace("isEmailInIgnoreList({})", email);
        String[] ignoreList = MIST.getPrefs().getStrings(getPrefName(PREF_ADDRESSES_IGNORE));
        return EmailModel.isEmailInList(email, ignoreList);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isImportComplete() {
        return importComplete;
    }

    public abstract void loadMessageList() throws EmailServerException;

    public void openFolder() throws EmailServerException {
        log.trace("openFolder()");

        if (store == null || !store.isConnected()) {
            throw new EmailServerException(String.format("{%s} Store not available", getNickname()));
        }

        if (folder != null && folder.isOpen()) {
            // Close & reopen the folder (to make sure messages are property expunged
            log.trace("{{}} Folder '{}' already open; closing & reopening...", getNickname(), folder.getName());
            try {
                folder.close();
            } catch (MessagingException e) {
                throw new EmailServerException(e);
            }
        }

        try {
            folder = store.getFolder(getFolderName());
            folder.open(Folder.READ_ONLY);
        } catch (MessagingException e) {
            folder = null;
            throw new EmailServerException(e);
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        MIST.getPrefs().setValue(getPrefName(PREF_ENABLED), enabled);
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
        if (folderName != null)
            MIST.getPrefs().setValue(getPrefName(PREF_FOLDER), folderName);
    }

    private void setId(int id) {
        this.id = id;
    }

    public void setIgnoreAddresses(String[] ignoreAddresses) {
        this.ignoreAddresses = ignoreAddresses;
        if (ignoreAddresses != null)
            MIST.getPrefs().setValues(getPrefName(PREF_ADDRESSES_IGNORE), ignoreAddresses);
    }

    /**
     * Set importComplete; if true, also notify the EmailModel.
     */
    private void setImportComplete(boolean importComplete) {
        this.importComplete = importComplete;
        if (importComplete)
            EmailModel.serverComplete();
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

    public void setType(String type) {
        this.type = type;
        if (type != null)
            MIST.getPrefs().setValue(getPrefName(PREF_TYPE), type);
    }

    public void setUsername(String username) {
        this.username = username;
        if (username != null)
            MIST.getPrefs().setValue(getPrefName(PREF_USERNAME), username);
    }

    /**
     * Starts the email import service for this server.
     */
    public void startImportService() {
        log.trace("{{}} startImportService()", getNickname());

        if (!isEnabled()) {
            log.warn("{{}} Cannot start import - server is disabled!");
            setImportComplete(true); // Debatable whether this should be set here...
            return;
        }

        log.trace("{{}} === Email Server Import Service Started ===", nickname);

        // Make sure we're in the proper state
        stopImporting = false;
        importComplete = false;

        Thread importThread = new Thread() {
            @Override
            public void run() {
                Util.connectToEmailServer(EmailServer.this, true, true);
                if (!isConnected()) {
                    // We didn't connect, so our import is done
                    setImportComplete(true);
                    return;
                }

                log.debug("{{}} Folder '{}' contains {} messages", nickname, folderName, totalMessages);

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

                // We're done with this server for now, but don't disconnect yet in case we want to make changes
                // to folders/labels, etc.

                // Import is complete
                log.trace("{{}} === Email Server Import Service Stopped ===", nickname);
                setImportComplete(true);

                // If we're done because there were no messages, tell the user.
                if (totalMessages == 0) {
                    String msg = String.format("'%s' had no messages to import.", nickname);
                    log.debug(msg);
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            MessageDialog.openInformation(MIST.getView().getShell(), "Import complete", msg);
                        }
                    });
                }

            } // run()
        };
        importThread.setName(String.format("ESImport%s", id));
        importThread.start();
    }

    public void stopImportService() {
        log.trace("{{}} stopImportService()", getNickname());
        stopImporting = true;
    }

    @Override
    public String toString() {
        return String.format("EmailServer {%s}", getNickname());
    }

}
