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
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.widgets.Shell;

import com.github.tomhallman.mist.exceptions.EmailServerException;
import com.github.tomhallman.mist.model.EmailModel;
import com.github.tomhallman.mist.model.MessageModel;
import com.github.tomhallman.mist.util.Util;

public class EmailServer implements Cloneable {
    private static Logger log = LogManager.getLogger();

    // Import controls
    private boolean stopImporting = false;
    private boolean importComplete = false;

    // Server properties
    private int id;

    private int curMsgNum;
    private String folderName;

    private String host;
    private String myName;
    private String nickname;
    private String password;
    private boolean passwordPrompt;
    private String port;
    private String username;
    private Integer tntUserId;
    private String[] ignoreAddresses;
    private String[] myAddresses;
    private Folder folder;

    private Store store;

    public EmailServer() {
        log.trace("EmailServer()");
        init();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        log.trace("{{}} clone()", getNickname());
        EmailServer es2 = (EmailServer) super.clone();
        // Deep copy non-primitives (arrays of primitives also ok)
        // We're only concerned with settings here; not the folder/store stuff, etc.
        return es2;
    }

    public void connect(boolean selectFolder) throws EmailServerException {
        log.trace("{{}} connect()", getNickname());
        log.debug("{{}} Connecting to email server at '{}:{}'...", getNickname(), getHost(), getPort());

        Session sess = Session.getDefaultInstance(getConnectionProperties(), null);
        store = null;
        folder = null;

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
            curMsgNum = 0;
            try {
                folder = store.getFolder(getFolder());
                folder.open(Folder.READ_ONLY);
            } catch (MessagingException e) {
                folder = null;
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
        } catch (MessagingException e) {
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

    public int getCurrentMessageNumber() throws EmailServerException {
        log.trace("{{}} getCurrentMessageNumber() : {}", getNickname(), curMsgNum);
        if (curMsgNum == -1)
            throw new EmailServerException("[" + getNickname() + "] Current message is invalid");
        return curMsgNum;
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

    public int getMessageCount() throws EmailServerException {
        log.trace("{{}} getMessageCount()", getNickname());
        if (folder == null)
            throw new EmailServerException("[" + getNickname() + "] Email folder is not open");
        try {
            return folder.getMessageCount();
        } catch (MessagingException e) {
            throw new EmailServerException(e);
        }
    }

    public String[] getMyAddresses() {
        return myAddresses;
    }

    public String getMyName() {
        return myName;
    }

    public javax.mail.Message getNextMessage() throws EmailServerException {
        log.trace("{{}} getNextMessage()", getNickname());
        try {
            return folder.getMessage(++curMsgNum);
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
        return "";
    }

    public String getPassword() {
        return password;
    }

    public String getPort() {
        return port;
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

    public String getUsername() {
        return username;
    }

    public boolean hasNextMessage() throws EmailServerException {
        log.trace("{{}} hasNextMessage()", getNickname());
        if (folder == null)
            throw new EmailServerException("[" + getNickname() + "] Email folder is not open");
        return curMsgNum < getMessageCount();
    }

    public void init() {
        log.trace("{{}} init()", getNickname());

        // Close any old connections
        disconnect();

        id = -1;
        curMsgNum = -1;
        folderName = "";
        host = "";
        myName = "";
        nickname = "";
        password = "";
        passwordPrompt = true;
        port = "";
        username = "";
        tntUserId = 0;
        ignoreAddresses = new String[0];
        myAddresses = new String[0];

        folder = null;
        store = null;

        stopImporting = false;
        importComplete = false;
    }

    public boolean isConnected() {
        return store != null;
    }

    public boolean isImportComplete() {
        return importComplete;
    }

    public boolean isPasswordNeeded() {
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
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setIgnoreAddresses(String[] ignoreAddresses) {
        this.ignoreAddresses = ignoreAddresses;
    }

    public void setMyAddresses(String[] myAddresses) {
        this.myAddresses = myAddresses;
    }

    public void setMyName(String myName) {
        this.myName = myName;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPasswordPrompt(boolean prompt) {
        this.passwordPrompt = prompt;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setTntUserId(Integer userId) {
        tntUserId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
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

        Util.connectToEmailServer(shell, this, true);

        Thread importThread = new Thread() {
            @Override
            public void run() {

                // See if there are any messages to import
                boolean hasMoreMessages = false;
                int currentMessageNumber = 0;
                int messageCount = 0;

                try {
                    hasMoreMessages = hasNextMessage();
                    messageCount = getMessageCount();
                } catch (EmailServerException e) {
                    // TODO: Report this to the user
                    String msg = "Can't connect to folder";
                    log.error(e);
                    importComplete = true;
                    disconnect();
                    EmailModel.serverComplete();
                    return;
                }

                log.info("{{}} Folder '{}' contains {} messages", nickname, folderName, messageCount);

                while (hasMoreMessages && !stopImporting) {
                    log.debug(
                        "{{}} Processing message {} of {}",
                        nickname,
                        currentMessageNumber + 1, // 0-based index
                        messageCount);

                    try {
                        // Add Message to message queue
                        MessageModel.addMessage(new EmailMessage(EmailServer.this, getNextMessage()));
                        hasMoreMessages = hasNextMessage();
                        currentMessageNumber = getCurrentMessageNumber();
                    } catch (EmailServerException e) {
                        String msg = "Error retrieving messages";
                        // TODO: Report via reportError, but add "don't open more errors for this email server"
                        // check
                        log.error(e);
                    }
                }

                importComplete = true;
                disconnect();
                EmailModel.serverComplete();
            } // run()
        };
        importThread.start();
    }

    public void stopImportService() {
        stopImporting = true;
    }

    @Override
    public String toString() {
        return String.format("EmailServer {%s}", getNickname());
    }

}
