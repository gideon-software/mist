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
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.MessageBox;

import com.gideonsoftware.mist.exceptions.EmailServerException;
import com.gideonsoftware.mist.model.data.EmailServer;
import com.gideonsoftware.mist.model.data.GmailServer;
import com.gideonsoftware.mist.preferences.fieldeditors.SmartComboFieldEditor;
import com.gideonsoftware.mist.util.ui.Images;
import com.google.api.services.gmail.model.Label;

/**
 *
 */
public class GmailServerPreferencePage extends EmailServerPreferencePage {
    private static Logger log = LogManager.getLogger();

    private SmartComboFieldEditor<String> labelEditor;
    private BooleanFieldEditor removeLabelEditor;

    public GmailServerPreferencePage(int serverId) {
        super(serverId);
        log.trace("GmailServerPreferencePage({})", serverId);
        server = new GmailServer(serverId);
        setTitle(server.getNickname());
        setImageDescriptor(ImageDescriptor.createFromImage(Images.getImage(Images.ICON_EMAIL_SERVER)));
        // setDescription("description here");
    }

    protected void addLabelEditor() {
        log.trace("addLabelEditor()");
        // Note: Label name must be treated specially, since fieldEditor doesn't know about it directly
        // See performOk
        labelEditor = new SmartComboFieldEditor<String>(
            server.getPrefName(GmailServer.PREF_LABEL_ID),
            "&Label",
            getFieldEditorParent(),
            true);
        labelEditor.setEmptySelectionAllowed(false);

        GmailServer gmailServer = (GmailServer) server;
        if (!gmailServer.getLabelName().isEmpty()) {
            // We're not connected to the gmail server, but we know the label from previous preferences
            labelEditor.add(gmailServer.getLabelId(), gmailServer.getLabelName());
            labelEditor.setSelection(gmailServer.getLabelId());
        }
        labelEditor.setEnabled(false, getFieldEditorParent());
        labelEditor.setErrorMessage("An email label must be selected.");
        Button labelEditorButton = labelEditor.getButtonControl(getFieldEditorParent());
        labelEditorButton.setImage(Images.getImage(Images.ICON_RELOAD));
        labelEditorButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                log.trace("labelEditorButton.widgetSelected({})", event);
                if (connectToServer()) {
                    MessageBox msgBox = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
                    msgBox.setMessage("Labels reloaded from Gmail.");
                    msgBox.open();
                }
            }
        });
        addField(labelEditor);
    }

    protected void addRemoveLabelEditor() {
        log.trace("addRemoveLabelEditor()");
        removeLabelEditor = new BooleanFieldEditor(
            server.getPrefName(GmailServer.PREF_LABEL_REMOVE_AFTER_IMPORT),
            "Remove label from messages after importing",
            getFieldEditorParent());
        addField(removeLabelEditor);
    }

    @Override
    protected void addUsernameEditor() {
        log.trace("addUsernameEditor()");
        usernameEditor = new StringFieldEditor(
            server.getPrefName(EmailServer.PREF_USERNAME),
            "Gmail account:",
            getFieldEditorParent());
        addField(usernameEditor);
        usernameEditor.getTextControl(getFieldEditorParent()).setEditable(false);
    }

    @Override
    protected boolean connectToServer() {
        log.trace("connectToServer()");
        boolean success = super.connectToServer();
        labelEditor.setEnabled(success, getFieldEditorParent());
        return success;
    }

    @Override
    protected void createFieldEditors() {
        log.trace("createFieldEditors()");

        addNicknameEditor();
        addEnabledEditor();
        addUsernameEditor();
        addLabelEditor();
        addRemoveLabelEditor();
        addTntUserEditor();
        addMyEmailAddressesEditor();
        addIgnoreEmailAddressesEditor();

        addSpacer();

        addRemoveButton();
    }

    @Override
    protected void onSuccessfulConnection() {
        log.trace("onSuccessfulConnection()");
        // If there was a previously selected label, use that
        String oldKey = labelEditor.getSelectionItem();

        // Populate label list
        labelEditor.removeAll();
        try {
            for (Label label : ((GmailServer) server).getLabelList())
                labelEditor.add(label.getId(), label.getName());
        } catch (EmailServerException e) {
            log.error("Unable to load label list", e);
        }

        labelEditor.setSelection(oldKey != null ? oldKey : ((GmailServer) server).getLabelId());
    }

    @Override
    public boolean performOk() {
        log.trace("performOk()");
        boolean ok = super.performOk();
        if (ok) {
            // Save the label name; the labelEditor doesn't know how to do this!
            if (labelEditor != null) // We may not have loaded the page!
                ((GmailServer) server).setLabelName(labelEditor.getSelectionValue());
        }
        return ok;
    }

    @Override
    protected void savePageSettings() {
        log.trace("savePageSettings()");
        super.savePageSettings();

        GmailServer gmailServer = (GmailServer) server;
        gmailServer.setLabelId(labelEditor.getSelectionItem());
        gmailServer.setLabelName(labelEditor.getSelectionValue());
        gmailServer.setLabelRemoveAfterImport(removeLabelEditor.getBooleanValue());
    }

}
