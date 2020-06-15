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

package com.gideonsoftware.mist.preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.model.EmailModel;
import com.gideonsoftware.mist.model.data.EmailServer;
import com.gideonsoftware.mist.preferences.preferencepages.EmailPreferencePage;
import com.gideonsoftware.mist.preferences.preferencepages.GmailServerPreferencePage;
import com.gideonsoftware.mist.preferences.preferencepages.ImapServerPreferencePage;
import com.gideonsoftware.mist.preferences.preferencepages.LoggingPreferencePage;
import com.gideonsoftware.mist.preferences.preferencepages.TntDbPreferencePage;
import com.gideonsoftware.mist.preferences.preferencepages.UpdatesPreferencePage;

/**
 *
 */
public class MistPreferenceManager extends PreferenceManager {
    private static Logger log = LogManager.getLogger();

    private static final String PREFNODE_TNT = "tnt";
    private static final String PREFNODE_EMAIL = "email";
    private static final String PREFNODE_LOGGING = "logging";
    private static final String PREFNODE_UPDATES = "updates";

    private static MistPreferenceManager manager;
    private MistPreferenceDialog preferenceDialog;

    /**
     * 
     */
    public MistPreferenceManager() {
        super();
        log.trace("MistPreferenceManager()");
    }

    /**
     * Thread-safe, lazily-initialized singleton creator.
     * 
     * @see https://en.wikipedia.org/wiki/Singleton_pattern
     * @return a handle to the Preferences object for this user
     */
    public static synchronized MistPreferenceManager getInstance() {
        if (manager == null) {
            manager = new MistPreferenceManager();
        }
        return manager;
    }

    public void addEmailServerNode(int serverId, boolean refreshAndSelect) {
        log.trace("addEmailServerNode({},{})", serverId, refreshAndSelect);

        PreferenceNode serverNode;

        String type = EmailModel.getEmailServerType(serverId);
        if (EmailServer.TYPE_IMAP.equals(type))
            serverNode = new SmartPreferenceNode(
                EmailServer.getPrefPrefix(serverId),
                new ImapServerPreferencePage(serverId));
        else // if (EmailServer.TYPE_GMAIL.equals(type))
            serverNode = new SmartPreferenceNode(
                EmailServer.getPrefPrefix(serverId),
                new GmailServerPreferencePage(serverId));

        addTo(PREFNODE_EMAIL, serverNode);
        if (refreshAndSelect) {
            refreshPreferenceDialogTree();
            preferenceDialog.getTreeViewer().setSelection(new StructuredSelection(serverNode));
        }
    }

    private void addNodes() {
        log.trace("addNodes()");
        addToRoot(new SmartPreferenceNode(PREFNODE_TNT, new TntDbPreferencePage()));
        addToRoot(new SmartPreferenceNode(PREFNODE_EMAIL, new EmailPreferencePage()));
        reloadEmailServerNodes();
        addToRoot(new SmartPreferenceNode(PREFNODE_LOGGING, new LoggingPreferencePage()));
        addToRoot(new SmartPreferenceNode(PREFNODE_UPDATES, new UpdatesPreferencePage()));
    }

    public MistPreferenceDialog createPreferenceDialog(Shell shell) {
        log.trace("createPreferenceDialog({})", shell);
        removeAll();
        addNodes();
        preferenceDialog = new MistPreferenceDialog(shell, this);
        preferenceDialog.setPreferenceStore(MIST.getPrefs());
        preferenceDialog.setHelpAvailable(true);
        preferenceDialog.create();
        preferenceDialog.getTreeViewer().expandAll();
        return preferenceDialog;
    }

    /**
     * @return the preferenceDialog
     */
    public MistPreferenceDialog getPreferenceDialog() {
        return preferenceDialog;
    }

    public void postEmailServerRemoved() {
        log.trace("postEmailServerRemoved()");

        // Ready the current page for exit by bypassing dialog error checking
        MIST.getPreferenceManager().getPreferenceDialog().clearCurrentPage();

        // Reload email server nodes
        reloadEmailServerNodes();

        // Select the email node
        preferenceDialog.getTreeViewer().setSelection(new StructuredSelection(find(PREFNODE_EMAIL)));

        // Refresh the tree
        refreshPreferenceDialogTree();
    }

    private void refreshPreferenceDialogTree() {
        log.trace("refreshPreferenceDialogTree()");
        if (preferenceDialog != null)
            preferenceDialog.getTreeViewer().refresh(true);
    }

    private void reloadEmailServerNodes() {
        log.trace("loadEmailServerNodes()");

        // First remove all the nodes
        IPreferenceNode emailNode = find(PREFNODE_EMAIL);
        for (IPreferenceNode node : emailNode.getSubNodes())
            emailNode.remove(node);

        // Then add them all back
        for (int i = 0; i < EmailModel.getEmailServerCount(); i++)
            addEmailServerNode(i, false);
    }

}
