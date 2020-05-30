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

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.exceptions.EmailServerException;
import com.gideonsoftware.mist.preferences.Preferences;

/**
 * 
 */
public class ImapServer extends EmailServer {
    private static Logger log = LogManager.getLogger();

    public final static String PREF_FOLDER = "folder";
    public final static String PREF_HOST = "host";
    public final static String PREF_PASSWORD = "password";
    public final static String PREF_PORT = "port";
    public final static String PREF_USESSL = "usessl";

    public final static int DEFAULT_PORT_IMAP = 143;
    public final static int DEFAULT_PORT_IMAPS = 993;

    private String folderName;
    private String host;
    private String password;
    private String port;
    private boolean useSsl;

    private Store store;
    private Folder folder;

    public ImapServer(int id) {
        super(id, EmailServer.TYPE_IMAP);

        folder = null;
        store = null;

        //
        // Load preferences, providing reasonable defaults
        //

        Preferences prefs = MIST.getPrefs();
        folderName = prefs.getString(getPrefName(PREF_FOLDER));
        host = prefs.getString(getPrefName(PREF_HOST));
        password = prefs.getString(getPrefName(PREF_PASSWORD));

        // Set default SSL use
        prefs.setDefault(getPrefName(PREF_USESSL), true);
        useSsl = prefs.getBoolean(getPrefName(PREF_USESSL));

        // Set default port
        prefs.setDefault(getPrefName(PREF_PORT), useSsl ? DEFAULT_PORT_IMAPS : DEFAULT_PORT_IMAP);
        port = prefs.getString(getPrefName(PREF_PORT));
    }

    private void closeFolders() {
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

    private void closeStore() {
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

    @Override
    public void connect() throws EmailServerException {
        log.trace("{{}} connect()", getNickname());

        if (isConnected()) {
            log.trace("{{}} Already connected", getNickname());
            return;
        }

        // Initialize values
        store = null;
        folder = null;
        currentMessageNumber = 0;
        totalMessages = 0;

        log.debug(
            "{{}} Connecting to {} server at '{}:{}'...",
            getNickname(),
            useSsl ? "IMAPS" : "IMAP",
            getHost(),
            getPort());

        // Set connection properties
        Properties props = new Properties();
        props.setProperty("mail.host", getHost());
        props.setProperty("mail.port", getPort());
        props.setProperty("mail.user", getUsername());
        props.setProperty("mail.password", getPassword());

        if (useSsl) {
            // Don't verify server identity. This is required for self-signed
            // certificates, which missionaries may very well use ;)
            // Source: http://stackoverflow.com/a/5730201/1307022
            props.setProperty("mail.imaps.ssl.checkserveridentity", "false");
            props.setProperty("mail.imaps.ssl.trust", "*");
        }
        Session sess = Session.getInstance(props, null);

        try {
            store = sess.getStore(useSsl ? "imaps" : "imap");
        } catch (NoSuchProviderException e) {
            throw new EmailServerException(e); // Shouldn't happen...
        }

        try {
            store.connect(host, username, password);
        } catch (MessagingException e) {
            // We should ask the user for their password again next time.
            setPassword("");
            store = null;
            throw new EmailServerException(e);
        }

    }

    @Override
    public void disconnect() {
        log.trace("{{}} disconnect()", getNickname());
        closeFolders();
        closeStore();
    }

    public Folder[] getFolderList() {
        log.trace("{{}} getFolderList()", getNickname());
        if (!isConnected()) {
            log.error("{{}} Not connected to email server", getNickname());
            return new Folder[0];
        }
        try {
            return store.getDefaultFolder().list("*");
        } catch (MessagingException | IllegalStateException e) {
            log.error("{{}} Unable to retrieve folder list", getNickname());
            return new Folder[0];
        }
    }

    public String getFolderName() {
        if (folderName == null)
            return "";
        return folderName;
    }

    public String getHost() {
        return host;
    }

    @Override
    public EmailMessage getNextMessage() throws EmailServerException {
        log.trace("{{}} getNextMessage()", getNickname());
        try {
            return new ImapMessage(ImapServer.this, folder.getMessage(++currentMessageNumber));
        } catch (MessagingException e) {
            throw new EmailServerException(e);
        }
    }

    public String getPassword() {
        return password;
    }

    public String getPort() {
        return port;
    }

    @Override
    public boolean hasNextMessage() {
        return currentMessageNumber < totalMessages;
    }

    @Override
    public boolean isConnected() {
        return store != null;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    @Override
    public void loadMessageList() throws EmailServerException {
        log.trace("{{}} loadMessageList()", getNickname());
        try {
            openFolder();
            totalMessages = folder.getMessageCount();
            currentMessageNumber = 0;
        } catch (MessagingException e) {
            throw new EmailServerException(e);
        }
        log.debug("{{}} Retrieved {} message(s) from folder '{}'", getNickname(), totalMessages, getFolderName());
    }

    public void openFolder() throws EmailServerException {
        log.trace("openFolder()");

        if (!isConnected())
            throw new EmailServerException(String.format("{%s} Not connected", getNickname()));

        if (folder != null && folder.isOpen()) {
            // Close & reopen the folder (to make sure messages are property expunged
            log.trace("{{}} Folder '{}' already open; closing & reopening...", getNickname(), folder.getName());
            try {
                folder.close();
            } catch (MessagingException e) {
                throw new EmailServerException(e);
            }
        }

        if (!getFolderName().isEmpty()) {
            try {
                folder = store.getFolder(getFolderName());
                folder.open(Folder.READ_ONLY);
            } catch (MessagingException e) {
                folder = null;
                throw new EmailServerException(e);
            }
        } else {
            log.warn("{{}} Could not open folder because folder name is blank", getNickname());
        }
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

    public void setPassword(String password) {
        this.password = password;
        if (password != null)
            MIST.getPrefs().setValue(getPrefName(PREF_PASSWORD), password);
    }

    public void setPort(String port) {
        this.port = port;
        if (port != null)
            MIST.getPrefs().setValue(getPrefName(PREF_PORT), port);
    }

    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
        MIST.getPrefs().setValue(getPrefName(PREF_USESSL), useSsl);
    }

}
