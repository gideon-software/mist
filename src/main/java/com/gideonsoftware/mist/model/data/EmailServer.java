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
import com.gideonsoftware.mist.util.ui.TipMessageBox;

public abstract class EmailServer implements Cloneable {
    private static Logger log = LogManager.getLogger();

    // Preferences
    private final static String PREF_PREFIX = "emailserver";
    public final static String PREF_ADDRESSES_IGNORE = "addresses.ignore";
    public final static String PREF_ADDRESSES_MY = "addresses.my";
    public final static String PREF_ENABLED = "enabled";
    public final static String PREF_NICKNAME = "nickname";
    public final static String PREF_USERNAME = "username";
    public final static String PREF_TNT_USERID = "tnt.user.id";
    public final static String PREF_TNT_USERNAME = "tnt.user.username";
    public final static String PREF_TYPE = "type";

    // Tips
    public final static String SHOWTIP_IMPORT_COMPLETE = "showtip.import.complete";

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

    protected boolean loadingMessages;
    protected int currentMessageNumber;
    protected int totalMessages;

    public EmailServer(int id, String type) {
        log.trace("EmailServer({})", id);
        setId(id);
        setType(type);

        // Non-preference initialization
        currentMessageNumber = 0;
        loadingMessages = false;
        totalMessages = 0;

        stopImporting = false;
        importComplete = false;

        //
        // Load values from preferences & set defaults
        //

        Preferences prefs = MIST.getPrefs();
        prefs.setDefault(getPrefName(PREF_ENABLED), true);

        enabled = prefs.getBoolean(getPrefName(PREF_ENABLED));
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

    public static String getFormattedTypeName(String type) {
        switch (type) {
            case TYPE_IMAP:
                return "IMAP";
            case TYPE_GMAIL:
                return "Gmail";
            default:
                return "Email";
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
        newIgnoreAddresses[ignoreAddresses.length] = email;
        setIgnoreAddresses(newIgnoreAddresses);
    }

    public void addMyAddress(String email) {
        log.trace("addMyAddress({})", email);
        String[] newMyAddresses = new String[myAddresses.length + 1];
        System.arraycopy(myAddresses, 0, newMyAddresses, 0, myAddresses.length);
        newMyAddresses[myAddresses.length] = email;
        setMyAddresses(newMyAddresses);
    }

    public void clearPreferences() {
        log.trace("{{}} clearPreferences()", getNickname());
        MIST.getPrefs().setToDefaultIfContains(getPrefName("")); // Will clear all prefs matching this emailserver
    }

    public abstract void connect() throws EmailServerException;

    public abstract void disconnect();

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof EmailServer))
            return false;
        EmailServer server2 = (EmailServer) obj;
        return server2.getId() == id;
    }

    public int getCurrentMessageNumber() {
        return currentMessageNumber;
    }

    public int getId() {
        return id;
    }

    public String[] getIgnoreAddresses() {
        return ignoreAddresses;
    }

    protected abstract String getImportCompleteTipMessage();

    public String[] getMyAddresses() {
        return myAddresses;
    }

    public abstract EmailMessage getNextMessage() throws EmailServerException;

    public String getNickname() {
        // This should never be null, as it's used even in logging for the email server
        if (nickname != null)
            return nickname;
        return "ID:" + id;
    }

    public String getPrefName(String name) {
        return getPrefName(name, id);
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

    public abstract boolean hasNextMessage();

    public abstract boolean isConnected();

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

    public boolean isLoadingMessages() {
        return loadingMessages;
    }

    public abstract void loadMessageList() throws EmailServerException;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        MIST.getPrefs().setValue(getPrefName(PREF_ENABLED), enabled);
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
            EmailModel.serverImportComplete();
    }

    /**
     * Set loadingMessages; also notify the EmailModel.
     */
    public void setLoadingMessages(boolean loadingMessages) {
        this.loadingMessages = loadingMessages;
        EmailModel.serverMessagesLoaded();
    }

    public void setMyAddresses(String[] myAddresses) {
        this.myAddresses = myAddresses;
        if (myAddresses != null)
            MIST.getPrefs().setValues(getPrefName(PREF_ADDRESSES_MY), myAddresses);
    }

    /**
     * Set the nickname for this email server.
     * 
     * @param nickname
     *            Nickname for this server
     */
    public void setNickname(String nickname) {
        setNickname(nickname, false);
    }

    /**
     * Set the nickname for this email server.
     * 
     * @param nickname
     *            Nickname for this server
     * @param noDuplicates
     *            If true and this nickname is a duplicate of an existing server's nickname, the next
     *            available nickname of the form "&lt;nickname&gt; &lt;num&gt;" will be used instead
     */
    public void setNickname(String nickname, boolean noDuplicates) {
        this.nickname = nickname;

        if (nickname == null)
            return;

        if (noDuplicates) {
            int num = 1;
            boolean exists;
            do {
                exists = false;
                for (int i = 0; i < EmailModel.getEmailServerCount(); i++) {
                    if (EmailModel.getEmailServer(i).getNickname().equals(nickname + (num == 1 ? "" : (" " + num)))) {
                        exists = true;
                        num++;
                    }
                }
            } while (exists);
            if (num > 1)
                this.nickname = nickname + " " + num;
        }

        MIST.getPrefs().setValue(getPrefName(PREF_NICKNAME), this.nickname);
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

    private void showImportCompleteTip() {
        String title = String.format("'%s' Import Complete", nickname);
        String message = getImportCompleteTipMessage();
        (new TipMessageBox(MIST.getView().getShell(), getPrefName(SHOWTIP_IMPORT_COMPLETE), title, message, false))
            .open();
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
        loadingMessages = false;

        Thread importThread = new Thread() {
            @Override
            public void run() {
                Util.connectToEmailServer(EmailServer.this);
                if (!isConnected()) {
                    // We didn't connect, so our import is done
                    setImportComplete(true);
                    return;
                }

                setLoadingMessages(true);
                try {
                    EmailServer.this.loadMessageList();
                } catch (EmailServerException e) {
                    Display.getDefault().syncExec(() -> {
                        String msg = String.format("Can't load messages on server '%s'", nickname);
                        Util.reportError("Email server error", msg, e);
                    });
                } finally {
                    setLoadingMessages(false);
                }

                while (!stopImporting && hasNextMessage()) {
                    if (hasNextMessage()) {
                        log.debug("{{}} Processing message {}", nickname, currentMessageNumber + 1);
                        try {
                            // Add Message to message queue
                            MessageModel.addMessage(getNextMessage());
                        } catch (EmailServerException e) {
                            Display.getDefault().syncExec(() -> {
                                String msg = String.format(
                                    "Can't retrieve message %s on server '%s'",
                                    currentMessageNumber,
                                    nickname);
                                Util.reportError("Email server error", msg, e);
                            });
                        }
                    }
                }

                // We're done with this server for now, but don't disconnect yet in case we want to make changes
                // to folders/labels, etc.

                // Import is complete
                log.trace("{{}} === Email Server Import Service Stopped ===", nickname);
                setImportComplete(true);

                if (totalMessages == 0) {
                    // If we're done because there were no messages, tell the user.
                    String msg = String.format("'%s' had no messages to import.", nickname);
                    log.debug(msg);
                    Display.getDefault().asyncExec(() -> {
                        MessageDialog.openInformation(MIST.getView().getShell(), "Import complete", msg);
                    });
                } else {
                    // There were messages; show the import complete tip
                    Display.getDefault().asyncExec(() -> {
                        showImportCompleteTip();
                    });
                }

            } // importThread.run()
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
