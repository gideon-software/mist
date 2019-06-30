/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2018 Tom Hallman
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

package com.github.tomhallman.mist.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.widgets.Shell;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.exceptions.EmailServerException;
import com.github.tomhallman.mist.model.data.EmailServer;
import com.github.tomhallman.mist.preferences.Preferences;

public class EmailModel {
    private static Logger log = LogManager.getLogger();

    // Preferences
    public final static String PREF_ADDRESSES_IGNORE = "email.addresses.ignore";

    // Property change values
    private final static PropertyChangeSupport pcs = new PropertyChangeSupport(EmailModel.class);
    public final static String PROP_EMAILSERVER_ADDED = "emailmodel.emailserver.added";
    public final static String PROP_IMPORTING = "emailmodel.importstatus.importing";

    private static EmailServer[] emailServers = new EmailServer[0];

    private static boolean importing = false;

    /**
     * No instantiation allowed!
     */
    private EmailModel() {
    }

    /**
     * Adds a new email server to the model. A new server ID is assigned.
     * 
     * @param server
     *            The email server to add.
     * 
     */
    public static void addEmailServer(EmailServer server) {
        EmailServer[] newArr = new EmailServer[emailServers.length + 1];
        int i;
        for (i = 0; i < emailServers.length; i++)
            newArr[i] = emailServers[i];
        server.setId(i);
        newArr[i] = server;
        emailServers = newArr;
        pcs.firePropertyChange(PROP_EMAILSERVER_ADDED, null, server);
    }

    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public static int getCurrentMessageNumberTotal() {
        int totalMessages = 0;
        for (EmailServer emailServer : emailServers) {
            if (emailServer.isConnected()) {
                try {
                    totalMessages += emailServer.getCurrentMessageNumber();
                } catch (EmailServerException e) {
                    log.error(e);
                    // But keep going!
                }
            }
        }
        return totalMessages;
    }

    public static EmailServer getEmailServer(int server) {
        return emailServers[server];
    }

    public static int getEmailServerCount() {
        return emailServers.length;
    }

    public static int getMessageCountTotal() {
        int totalMessages = 0;
        for (EmailServer emailServer : emailServers) {
            if (emailServer.isConnected()) {
                try {
                    totalMessages += emailServer.getMessageCount();
                } catch (EmailServerException e) {
                    log.error(e);
                    // But keep going!
                }
            }
        }
        return totalMessages;
    }

    public static void init() {
        log.trace("init()");

        importing = false;

        // Close any old connections
        for (EmailServer emailServer : emailServers)
            emailServer.disconnect();

        emailServers = new EmailServer[0];
        Preferences prefs = MIST.getPrefs();

        for (int i = 0; i < MIST.getPreferenceManager().getEmailServerPrefCount(); i++) {
            // Configure email servers from preferences
            EmailServer server = new EmailServer();
            server.setFolderName(prefs.getString(EmailServer.getPrefName(i, EmailServer.PREF_FOLDER)));
            server.setHost(prefs.getString(EmailServer.getPrefName(i, EmailServer.PREF_HOST)));
            server.setMyName(prefs.getString(EmailServer.getPrefName(i, EmailServer.PREF_MYNAME)));
            // Set default password prompt to true
            prefs.setDefault(EmailServer.getPrefName(i, EmailServer.PREF_PASSWORD_PROMPT), true);
            server.setPasswordPrompt(prefs.getBoolean(EmailServer.getPrefName(i, EmailServer.PREF_PASSWORD_PROMPT)));
            server.setPassword(
                server.isPasswordPrompt() ? "" : prefs.getString(
                    EmailServer.getPrefName(i, EmailServer.PREF_PASSWORD)));
            // Set default port to 993
            prefs.setDefault(EmailServer.getPrefName(i, EmailServer.PREF_PORT), 993);
            server.setPort(prefs.getString(EmailServer.getPrefName(i, EmailServer.PREF_PORT)));
            server.setUsername(prefs.getString(EmailServer.getPrefName(i, EmailServer.PREF_USERNAME)));
            server.setNickname(prefs.getString(EmailServer.getPrefName(i, EmailServer.PREF_NICKNAME)));
            server.setTntUserId(prefs.getInt(EmailServer.getPrefName(i, EmailServer.PREF_TNT_USERID)));
            server.setIgnoreAddresses(prefs.getStrings(EmailServer.getPrefName(i, EmailServer.PREF_ADDRESSES_IGNORE)));
            server.setMyAddresses(prefs.getStrings(EmailServer.getPrefName(i, EmailServer.PREF_ADDRESSES_MY)));
            addEmailServer(server);
        }
    }

    /**
     * TODO
     * 
     * @param email
     * @return
     */
    public static boolean isEmailInIgnoreList(String email) {
        log.trace("isEmailInList({},{})", email);
        String[] ignoreList = MIST.getPrefs().getStrings(PREF_ADDRESSES_IGNORE);
        return isEmailInList(email, ignoreList);
    }

    /**
     * TODO
     * 
     * @param email
     * @param list
     * @return
     */
    public static boolean isEmailInList(String email, String[] list) {
        for (String entry : list) {
            if (entry.contains("*") || entry.contains("?")) {
                // Wildcard match
                String regex = entry.trim().replace(".", "\\.").replace("*", ".*").replace('?', '.');
                if (email.trim().matches(regex))
                    return true;
            } else {
                // Standard string match
                if (entry.trim().equals(email.trim()))
                    return true;
            }
        }
        return false;
    }

    public static boolean isImporting() {
        return importing;
    }

    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    /**
     * Called by email servers when they're complete; checks whether all servers are done
     */
    public static void serverComplete() {
        log.trace("serverComplete()");
        // Check all email server statuses to see if they're *all* done
        boolean stillImporting = false;
        for (EmailServer emailServer : emailServers)
            if (!emailServer.isImportComplete())
                stillImporting = true;
        setImporting(stillImporting);
    }

    private static void setImporting(boolean importing) {
        log.trace("setImporting({})", importing);
        boolean oldImporting = EmailModel.importing;
        EmailModel.importing = importing;
        pcs.firePropertyChange(PROP_IMPORTING, oldImporting, importing);
    }

    /**
     * Starts the master email import service.
     * 
     * @param shell
     *            the shell for notifying the user of the connection taking place; if null, no notification will take
     *            place
     */
    public static void startImportService(Shell shell) {
        log.trace("startImportService({})", shell);
        setImporting(true);
        for (EmailServer emailServer : emailServers)
            emailServer.startImportService(shell);
    }

    public static void stopImportService() {
        log.trace("stopImportService()");
        for (EmailServer emailServer : emailServers)
            emailServer.stopImportService();
        // setImporting(false) will eventually be called once all servers have completed
    }

}
