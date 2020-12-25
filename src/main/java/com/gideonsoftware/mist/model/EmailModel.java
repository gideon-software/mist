/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2018 Gideon Software
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

package com.gideonsoftware.mist.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.model.data.EmailServer;
import com.gideonsoftware.mist.model.data.GmailServer;
import com.gideonsoftware.mist.model.data.ImapServer;

public class EmailModel {
    private static Logger log = LogManager.getLogger();

    // Preferences
    public final static String PREF_ADDRESSES_IGNORE = "email.addresses.ignore";
    public final static String PREF_AUTOTHANK_ENABLED = "email.autothank.enabled";
    public final static String PREF_AUTOTHANK_SUBJECTS = "email.autothank.subjects";
    public final static String PREF_EMAILSERVERS_COUNT = "email.emailservers.count";

    // Default values
    private final static String[] DEFAULT_ADDRESSES_IGNORE = new String[] { "mailer-daemon@*" };
    private final static boolean DEFAULT_AUTOTHANK_ENABLED = true;
    private final static String[] DEFAULT_AUTOTHANK_SUBJECTS = new String[] { "Thank" };

    // Property change values
    private final static PropertyChangeSupport pcs = new PropertyChangeSupport(EmailModel.class);
    public final static String PROP_EMAILSERVER_ADDED = "emailmodel.emailserver.added";
    public final static String PROP_EMAILSERVER_REMOVED = "emailmodel.emailserver.removed";
    public final static String PROP_EMAILSERVERS_INIT = "emailmodel.emailservers.init";
    public final static String PROP_IMPORTING = "emailmodel.importstatus.importing";
    public final static String PROP_MESSAGES_LOADING = "emailmodel.messages.loading";

    private static List<EmailServer> emailServers = new ArrayList<EmailServer>();

    private static boolean importing = false;
    private static boolean messagesLoading = false;

    static {
        // Set default preferences
        MIST.getPrefs().setDefault(PREF_AUTOTHANK_ENABLED, DEFAULT_AUTOTHANK_ENABLED);
        MIST.getPrefs().setDefault(PREF_AUTOTHANK_SUBJECTS, DEFAULT_AUTOTHANK_SUBJECTS);
        MIST.getPrefs().setDefault(PREF_ADDRESSES_IGNORE, DEFAULT_ADDRESSES_IGNORE);
    }

    /**
     * No instantiation allowed!
     */
    private EmailModel() {
    }

    /**
     * Adds a new email server to the model.
     * 
     * @param server
     *            The email server to add
     * 
     */
    public static void addEmailServer(EmailServer server) {
        log.trace("addEmailServer({})", server);
        emailServers.add(server);
        MIST.getPrefs().setValue(PREF_EMAILSERVERS_COUNT, emailServers.size());
        pcs.firePropertyChange(PROP_EMAILSERVER_ADDED, null, server);
    }

    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        log.trace("addPropertyChangeListener({})", listener);
        pcs.addPropertyChangeListener(listener);
    }

    public static void disconnectServers() {
        log.trace("disconnectServers()");
        for (EmailServer emailServer : emailServers) {
            if (emailServer != null && emailServer.isConnected())
                emailServer.disconnect();
        }
    }

    public static boolean doesSubjectStartWithPhraseInList(String subject, String[] list) {
        for (String entry : list)
            if (subject.toLowerCase().startsWith(entry.toLowerCase()))
                return true;
        return false;
    }

    public static int getCurrentMessageNumberTotal() {
        int currentMessages = 0;
        for (EmailServer emailServer : emailServers)
            if (emailServer.isConnected())
                currentMessages += emailServer.getCurrentMessageNumber();
        return currentMessages;
    }

    public static EmailServer getEmailServer(int serverId) {
        return emailServers.get(serverId);
    }

    public static int getEmailServerCount() {
        return emailServers.size();
    }

    public static String getEmailServerType(int serverId) {
        if (serverId < emailServers.size()) {
            EmailServer emailServer = emailServers.get(serverId);
            // Return value from instantiated server; if the server is null, skip it
            if (emailServer != null)
                return emailServers.get(serverId).getType();
            else
                return null;
        } else {
            // Return value from preferences (because we're probably trying to instantiate the server)
            return MIST.getPrefs().getString(EmailServer.getPrefName(EmailServer.PREF_TYPE, serverId));
        }
    }

    public static int getEnabledEmailServerCount() {
        int count = 0;
        for (EmailServer emailServer : emailServers)
            if (emailServer != null && emailServer.isEnabled())
                count++;
        return count;
    }

    public static int getMessageCountTotal() {
        int totalMessages = 0;
        for (EmailServer emailServer : emailServers)
            if (emailServer.isConnected())
                totalMessages += emailServer.getTotalMessages();
        return totalMessages;
    }

    public static void init() {
        log.trace("init()");
        // Load email servers
        loadEmailServers();
    }

    public static boolean isEmailInIgnoreList(String email) {
        log.trace("isEmailInList({})", email);
        String[] ignoreList = MIST.getPrefs().getStrings(PREF_ADDRESSES_IGNORE);
        return isEmailInList(email, ignoreList);
    }

    public static boolean isEmailInList(String email, String[] list) {
        log.trace("isEmailInList({},{})", email, list);
        for (String entry : list) {
            if (entry.contains("*") || entry.contains("?")) {
                // Wildcard match
                String regex = entry.toLowerCase().replace(".", "\\.").replace("*", ".*").replace('?', '.');
                if (email.toLowerCase().matches(regex))
                    return true;
            } else {
                // Standard string match
                if (entry.equalsIgnoreCase(email))
                    return true;
            }
        }
        return false;
    }

    public static boolean isImporting() {
        return importing;
    }

    public static boolean isMessagesLoading() {
        return messagesLoading;
    }

    private static void loadEmailServers() {
        log.trace("loadEmailServers()");

        // First disconnect any existing servers
        for (EmailServer emailServer : emailServers) {
            if (emailServer != null)
                emailServer.disconnect();
            emailServer = null;
        }
        emailServers.clear(); // Clear the list

        MIST.getPrefs().setDefault(PREF_EMAILSERVERS_COUNT, 0);
        int emailServerCount = MIST.getPrefs().getInt(PREF_EMAILSERVERS_COUNT);
        for (int i = 0; i < emailServerCount; i++) {
            String type = getEmailServerType(i);
            if (type == null)
                continue; // Something's wrong; skip it
            // Email servers will configure themselves from saved preferences
            EmailServer server = null;
            if (EmailServer.TYPE_IMAP.equals(type))
                server = new ImapServer(i);
            else if (EmailServer.TYPE_GMAIL.equals(type))
                server = new GmailServer(i);
            addEmailServer(server);
        }
        pcs.firePropertyChange(PROP_EMAILSERVERS_INIT, null, emailServers.size());
    }

    /**
     * Removes an email server from the model, including stored preferences, if it exists.
     * 
     * @param server
     *            The email server to remove
     * 
     */
    public static void removeEmailServer(EmailServer server) {
        log.trace("removeEmailServer({})", server);
        int id = server.getId();
        server.disconnect();
        emailServers.remove(server);
        MIST.getPrefs().setValue(PREF_EMAILSERVERS_COUNT, emailServers.size());

        server.clearPreferences(); // So the server is no longer stored in preferences
        pcs.firePropertyChange(PROP_EMAILSERVER_REMOVED, null, id);
        server = null; // Needed?

        // Because server IDs have contiguous IDs (0, 1, 2, etc.), we must reassign IDs
        // The easiest way to do this is to work directly on the preferences, then reload the servers
        for (int i = id; i < getEmailServerCount(); i++)
            MIST.getPrefs().replacePrefNames(EmailServer.getPrefPrefix(i + 1), EmailServer.getPrefPrefix(i));
        // Clear last one
        MIST.getPrefs().setToDefaultIfContains(EmailServer.getPrefPrefix(getEmailServerCount()));

        EmailModel.loadEmailServers();
    }

    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        log.trace("removePropertyChangeListener({})", listener);
        pcs.removePropertyChangeListener(listener);
    }

    /**
     * Called by email servers when they're done importing; checks whether all servers are done
     */
    public static void serverImportComplete() {
        log.trace("serverImportComplete()");
        // Check all enabled email server statuses to see if they're *all* done
        boolean stillImporting = false;
        for (EmailServer emailServer : emailServers) {
            if (!emailServer.isEnabled())
                continue;
            else if (!emailServer.isImportComplete())
                stillImporting = true;
        }
        setImporting(stillImporting);

        // If we're done importing and there were no messages, tell the user
        if (getMessageCountTotal() == 0) {
            String msg = "There were no messages to import.";
            log.info(msg);
            Display.getDefault().asyncExec(() -> {
                MessageDialog.openInformation(MIST.getView().getShell(), "Import complete", msg);
            });
        }
    }

    /**
     * Called by email servers when they're done loading messages; checks whether all servers are done
     */
    public static void serverMessagesLoaded() {
        log.trace("serverMessagesLoaded()");
        // Check all enabled email server statuses to see if all messages have been loaded
        boolean stillLoading = false;
        for (EmailServer emailServer : emailServers) {
            if (!emailServer.isEnabled())
                continue;
            else if (emailServer.isLoadingMessages())
                stillLoading = true;
        }
        setMessagesLoading(stillLoading);
    }

    private static void setImporting(boolean importing) {
        log.trace("setImporting({})", importing);
        boolean oldImporting = EmailModel.importing;
        EmailModel.importing = importing;
        pcs.firePropertyChange(PROP_IMPORTING, oldImporting, importing);
    }

    private static void setMessagesLoading(boolean loading) {
        log.trace("setMessagesLoading({})", loading);
        boolean oldLoading = EmailModel.messagesLoading;
        EmailModel.messagesLoading = loading;
        pcs.firePropertyChange(PROP_MESSAGES_LOADING, oldLoading, loading);
    }

    /**
     * Starts the master email import service.
     */
    public static void startImportService() {
        log.trace("startImportService()");
        setImporting(true);
        for (EmailServer emailServer : emailServers)
            if (emailServer.isEnabled())
                emailServer.startImportService();
    }

    public static void stopImportService() {
        log.trace("stopImportService()");
        for (EmailServer emailServer : emailServers)
            emailServer.stopImportService();
        // setImporting(false) will eventually be called once all servers have completed
    }

}
