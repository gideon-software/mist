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

package com.github.tomhallman.mist.preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.model.EmailModel;
import com.github.tomhallman.mist.model.data.EmailServer;
import com.github.tomhallman.mist.preferences.preferencepages.EmailPreferencePage;
import com.github.tomhallman.mist.preferences.preferencepages.EmailServerPreferencePage;
import com.github.tomhallman.mist.preferences.preferencepages.TntDbPreferencePage;

/**
 *
 */
public class MistPreferenceManager extends PreferenceManager {
    private static Logger log = LogManager.getLogger();

    private static final String PREFNODE_TNT = "tnt";
    private static final String PREFNODE_EMAIL = "email";
    private static final String PREFNODE_EMAILSERVER_PREFIX = "server";

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
        PreferenceNode serverNode = new PreferenceNode(
            getEmailServerNodeName(serverId),
            new EmailServerPreferencePage(serverId));
        addTo(PREFNODE_EMAIL, serverNode);
        if (refreshAndSelect) {
            refreshPreferenceDialogTree();
            preferenceDialog.getTreeViewer().setSelection(new StructuredSelection(serverNode));
        }
    }

    private void addNodes() {
        log.trace("addNodes()");
        addToRoot(new PreferenceNode(PREFNODE_TNT, new TntDbPreferencePage()));
        addToRoot(new PreferenceNode(PREFNODE_EMAIL, new EmailPreferencePage()));
        for (int i = 0; i < EmailModel.getEmailServerCount(); i++)
            addEmailServerNode(i, false);
    }

    /**
     * 
     * @param shell
     * @return
     */
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
     * 
     * @param serverId
     */
    public void deleteEmailServerNode(int serverId) {
        log.trace("deleteEmailServerNode({})", serverId);
        IPreferenceNode emailNode = find(PREFNODE_EMAIL);
        emailNode.remove(getEmailServerNodeName(serverId));
        preferenceDialog.getTreeViewer().setSelection(new StructuredSelection(emailNode));
        refreshPreferenceDialogTree();
    }

    private String getEmailServerNodeName(int serverId) {
        return PREFNODE_EMAILSERVER_PREFIX + serverId;
    }

    /**
     * @return The number of email servers stored in the preferences
     */
    public int getEmailServerPrefCount() {
        Preferences prefs = MIST.getPrefs();
        int i = 0;
        // Nickname can not be empty, so we can know if a server exists
        while (!prefs.getString(EmailServer.getPrefName(i, EmailServer.PREF_NICKNAME)).isEmpty())
            i++;
        return i;
    }

    /**
     * @return the preferenceDialog
     */
    public MistPreferenceDialog getPreferenceDialog() {
        return preferenceDialog;
    }

    private void refreshPreferenceDialogTree() {
        log.trace("refreshPreferenceDialogTree()");
        if (preferenceDialog != null)
            preferenceDialog.getTreeViewer().refresh(true);
    }

}
