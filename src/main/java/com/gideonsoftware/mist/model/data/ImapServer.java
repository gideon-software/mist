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
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;

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

    public final static String PREF_HOST = "host";
    public final static String PREF_PASSWORD = "password";
    public final static String PREF_PASSWORD_PROMPT = "password.prompt";
    public final static String PREF_PORT = "port";
    public final static String PREF_USESSL = "usessl";

    public final static int DEFAULT_PORT_IMAP = 143;
    public final static int DEFAULT_PORT_IMAPS = 993;

    private String host;
    private String password;
    private boolean passwordPrompt;
    private String port;
    private boolean useSsl;

    public ImapServer(int id) {
        super(id, EmailServer.TYPE_IMAP);
    }

    @Override
    public void connect(boolean selectFolder) throws EmailServerException {
        log.trace("{{}} connect()", getNickname());
        log.debug(
            "{{}} Connecting to {} server at '{}:{}'...",
            getNickname(),
            useSsl ? "IMAPS" : "IMAP",
            getHost(),
            getPort());

        store = null;
        folder = null;
        currentMessageNumber = 0;
        totalMessages = 0;

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

    public String getHost() {
        return host;
    }

    @Override
    public Message getNextMessage() throws EmailServerException {
        log.trace("{{}} getNextMessage()", getNickname());
        try {
            return folder.getMessage(++currentMessageNumber);
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
    public void init() {
        log.trace("{{}} init()", getNickname());
        super.init();

        //
        // Load settings from preferences, providing reasonable defaults
        //

        Preferences prefs = MIST.getPrefs();
        setHost(prefs.getString(getPrefName(PREF_HOST)));

        // Set default password prompt
        prefs.setDefault(getPrefName(PREF_PASSWORD_PROMPT), true);
        setPasswordPrompt(prefs.getBoolean(getPrefName(PREF_PASSWORD_PROMPT)));
        setPassword(passwordPrompt ? "" : prefs.getString(getPrefName(PREF_PASSWORD)));

        // Set default SSL use
        prefs.setDefault(PREF_USESSL, true);
        setUseSsl(prefs.getBoolean(PREF_USESSL));

        // Set default port
        prefs.setDefault(getPrefName(PREF_PORT), useSsl ? DEFAULT_PORT_IMAPS : DEFAULT_PORT_IMAP);
        setPort(prefs.getString(getPrefName(PREF_PORT)));
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

    public boolean isUseSsl() {
        return useSsl;
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

    public void setPasswordPrompt(boolean prompt) {
        this.passwordPrompt = prompt;
        MIST.getPrefs().setValue(getPrefName(PREF_PASSWORD_PROMPT), prompt);
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
