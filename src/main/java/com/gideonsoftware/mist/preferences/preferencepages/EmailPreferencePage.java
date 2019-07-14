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

package com.gideonsoftware.mist.preferences.preferencepages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.model.EmailModel;
import com.gideonsoftware.mist.model.data.EmailServer;
import com.gideonsoftware.mist.model.data.ImapServer;
import com.gideonsoftware.mist.preferences.fieldeditors.AddEditRemoveListFieldEditor;
import com.gideonsoftware.mist.preferences.fieldeditors.ButtonFieldEditor;
import com.gideonsoftware.mist.preferences.fieldeditors.SpacerFieldEditor;
import com.gideonsoftware.mist.util.ui.Images;

/**
 *
 */
public class EmailPreferencePage extends FieldEditorPreferencePage {
    private static Logger log = LogManager.getLogger();

    private BooleanFieldEditor useAutoThankEditor;
    private AddEditRemoveListFieldEditor thankSubjectEditor;

    private ButtonFieldEditor addServerButton;
    private AddEditRemoveListFieldEditor ignoreAddressesEditor;

    public EmailPreferencePage() {
        super(FieldEditorPreferencePage.GRID);
        log.trace("EmailPreferencePage()");
        setTitle("Email");
        setImageDescriptor(ImageDescriptor.createFromImage(Images.getImage(Images.ICON_EMAIL)));
        // TODO: setDescription("Preferences that apply to all email servers");
        noDefaultAndApplyButton();
    }

    @Override
    protected void createFieldEditors() {
        // Add Email Server button
        addServerButton = new ButtonFieldEditor("&Add Email Server", getFieldEditorParent());
        addServerButton.getButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                log.trace("addServerButton.widgetSelected({})", e);
                // TODO: Use a wizard here instead!!

                EmailServer server = new ImapServer(EmailModel.getEmailServerCount());
                server.setNickname(EmailServer.NEW_NICKNAME);
                EmailModel.addEmailServer(server);

                // This refreshes the PreferenceDialog so the new server shows up
                MIST.getPreferenceManager().addEmailServerNode(server.getId(), true);
            }
        });
        addField(addServerButton);

        // Spacer
        addField(new SpacerFieldEditor(getFieldEditorParent()));

        // Auto-thank checkbox & list
        useAutoThankEditor = new BooleanFieldEditor(
            EmailModel.PREF_AUTOTHANK_ENABLED,
            " Automatically mark emails I send as \"Thank\" when:",
            getFieldEditorParent());
        addField(useAutoThankEditor);

        thankSubjectEditor = new AddEditRemoveListFieldEditor(
            EmailModel.PREF_AUTOTHANK_SUBJECTS,
            "...the subject line begins with:",
            getFieldEditorParent());
        thankSubjectEditor.setDialogMessage("Subject lines that begin with this phrase will be marked as \"Thank\"");
        thankSubjectEditor.setDialogDescription(
            "This phrase is not case-sensitive (e.g. Thank and thank are equivalent)");
        addField(thankSubjectEditor);
        // Set initial state
        thankSubjectEditor.setEnabled(
            MIST.getPrefs().getBoolean(EmailModel.PREF_AUTOTHANK_ENABLED),
            getFieldEditorParent());

        // Spacer
        addField(new SpacerFieldEditor(getFieldEditorParent()));

        // Email addresses to ignore
        ignoreAddressesEditor = new AddEditRemoveListFieldEditor(
            EmailModel.PREF_ADDRESSES_IGNORE,
            "Email addresses to &ignore:",
            getFieldEditorParent());
        ignoreAddressesEditor.setAddDialogMessage("Add email address to ignore");
        ignoreAddressesEditor.setEditDialogMessage("Edit email address to ignore");
        ignoreAddressesEditor.setDialogDescription(
            "Emails to or from these addresses will not be imported;"
                + System.lineSeparator()
                + "(Use * for any string and ? for any character)");
        addField(ignoreAddressesEditor);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        log.trace("propertyChange({})", event);
        super.propertyChange(event);
        if (event.getProperty().equals(FieldEditor.VALUE)) {
            if (event.getSource().equals(useAutoThankEditor))
                thankSubjectEditor.setEnabled((Boolean) event.getNewValue(), getFieldEditorParent());
        }

    }

}
