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

package com.github.tomhallman.mist.preferences.preferencepages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.model.EmailModel;
import com.github.tomhallman.mist.preferences.fieldeditors.AddRemoveListFieldEditor;
import com.github.tomhallman.mist.preferences.fieldeditors.ButtonFieldEditor;
import com.github.tomhallman.mist.preferences.fieldeditors.SpacerFieldEditor;

/**
 *
 */
public class EmailPreferencePage extends FieldEditorPreferencePage {
    private static Logger log = LogManager.getLogger();

    private AddRemoveListFieldEditor ignoreAddressesEditor;
    private ButtonFieldEditor addServerButton;

    public EmailPreferencePage() {
        super(FieldEditorPreferencePage.GRID);
        log.trace("EmailPreferencePage()");
        setTitle("Email");
        // setDescription("Preferences that apply to all email servers");
        noDefaultAndApplyButton();
    }

    @Override
    protected void createFieldEditors() {
        // Add Email Server button
        addServerButton = new ButtonFieldEditor("&Add Email Server", getFieldEditorParent());
        addServerButton.getButton().addSelectionListener(new AddServerListener());
        addField(addServerButton);

        // Spacer
        SpacerFieldEditor spacer = new SpacerFieldEditor(getFieldEditorParent());
        addField(spacer);

        // Email addresses to ignore
        ignoreAddressesEditor = new AddRemoveListFieldEditor(
            EmailModel.GLOBAL_ADDRESSES_IGNORE,
            "Email addresses to &ignore:",
            getFieldEditorParent());
        ignoreAddressesEditor.setAddDialogMessage("Add email address to ignore");
        ignoreAddressesEditor.setAddDialogDescription("Emails to or from these addresses will not be imported");
        addField(ignoreAddressesEditor);
    }

    protected class AddServerListener extends SelectionAdapter {
        public void widgetSelected(SelectionEvent e) {
            log.trace("AddServerListener.widgetSelected({})", e);
            // We "add a server" here by creating it in preferences, then refreshing the dialog
            // This breaks assumptions about how preferences are usually handled, so this is a
            // bit of hack.
            // TODO: Use a wizard here instead
            int serverId = MIST.getPreferenceManager().getEmailServerPrefCount();
            String prefName = EmailModel.getPrefName(serverId, EmailModel.NICKNAME);
            MIST.getPrefs().setValue(prefName, "New Email Server");
            // This refreshes the PreferenceDialog so the new server shows up
            MIST.getPreferenceManager().addEmailServerNode(serverId, true);
        }
    }

}
