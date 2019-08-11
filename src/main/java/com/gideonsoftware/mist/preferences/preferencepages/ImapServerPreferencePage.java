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

import javax.mail.Folder;
import javax.mail.MessagingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.MessageBox;

import com.gideonsoftware.mist.model.data.ImapServer;
import com.gideonsoftware.mist.preferences.fieldeditors.ButtonFieldEditor;
import com.gideonsoftware.mist.preferences.fieldeditors.SmartComboFieldEditor;
import com.gideonsoftware.mist.util.ui.Images;

/**
 *
 */
public class ImapServerPreferencePage extends EmailServerPreferencePage {
    private static Logger log = LogManager.getLogger();

    private StringFieldEditor hostEditor;
    private IntegerFieldEditor portEditor;
    private StringFieldEditor passwordEditor;
    private ButtonFieldEditor connectButton;
    private SmartComboFieldEditor<String> folderEditor;
    private BooleanFieldEditor useSslEditor;

    public ImapServerPreferencePage(int serverId) {
        super(serverId);
        log.trace("ImapServerPreferencePage({})", serverId);
        server = new ImapServer(serverId);
        setTitle(server.getNickname());
        setImageDescriptor(ImageDescriptor.createFromImage(Images.getImage(Images.ICON_EMAIL_SERVER)));
        // setDescription("description here");
    }

    protected void addConnectEditor() {
        log.trace("addConnectEditor()");
        connectButton = new ButtonFieldEditor("Test &Connection", getFieldEditorParent());
        connectButton.getButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                log.trace("connectButton.widgetSelected({})", event);
                if (connectToServer()) {
                    // Notify user that the connection was successful
                    MessageBox msgBox = new MessageBox(
                        ((Button) event.getSource()).getShell(),
                        SWT.ICON_INFORMATION | SWT.OK);
                    String successPhrase = "Connection successful!";
                    msgBox.setMessage(successPhrase);
                    msgBox.open();
                }
            }
        });
        addField(connectButton);
    }

    protected void addFolderEditor() {
        log.trace("addFolderEditor()");
        folderEditor = new SmartComboFieldEditor<String>(
            server.getPrefName(ImapServer.PREF_FOLDER),
            "&Folder",
            getFieldEditorParent(),
            true);
        folderEditor.setEmptySelectionAllowed(false);

        ImapServer imapServer = (ImapServer) server;
        if (!imapServer.getFolderName().isEmpty()) {
            // We're not connected to the email server, but we know the folder from previous preferences
            folderEditor.add(imapServer.getFolderName(), imapServer.getFolderName());
            folderEditor.setSelection(imapServer.getFolderName());
        }
        folderEditor.setEnabled(false, getFieldEditorParent());
        folderEditor.setErrorMessage("An email folder must be selected.");
        Button folderEditorButton = folderEditor.getButtonControl(getFieldEditorParent());
        folderEditorButton.setImage(Images.getImage(Images.ICON_RELOAD));
        folderEditorButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                log.trace("folderEditorButton.widgetSelected({})", event);
                if (connectToServer()) {
                    MessageBox msgBox = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
                    msgBox.setMessage("Folders reloaded from email server.");
                    msgBox.open();
                }
            }
        });
        addField(folderEditor);
    }

    protected void addHostEditor() {
        log.trace("addHostEditor(");
        hostEditor = new StringFieldEditor(server.getPrefName(ImapServer.PREF_HOST), "&Host:", getFieldEditorParent());
        hostEditor.setEmptyStringAllowed(false);
        hostEditor.setErrorMessage("Host may not be empty.");
        addField(hostEditor);
    }

    protected void addPasswordEditor() {
        log.trace("addPasswordEditor()");
        passwordEditor = new StringFieldEditor(
            server.getPrefName(ImapServer.PREF_PASSWORD),
            "&Password:",
            getFieldEditorParent());
        passwordEditor.getTextControl(getFieldEditorParent()).setEchoChar('*');
        addField(passwordEditor);
    }

    protected void addPortEditor() {
        log.trace("addPortEditor()");
        portEditor = new IntegerFieldEditor(server.getPrefName(ImapServer.PREF_PORT), "P&ort:", getFieldEditorParent());
        portEditor.setEmptyStringAllowed(false);
        portEditor.setErrorMessage("Port must be a number.");
        addField(portEditor);
    }

    protected void addUseSslEditor() {
        log.trace("addUseSslEditor()");
        useSslEditor = new BooleanFieldEditor(
            server.getPrefName(ImapServer.PREF_USESSL),
            "Use SSL (IMAPS)",
            getFieldEditorParent());
        addField(useSslEditor);
    }

    @Override
    protected boolean connectToServer() {
        log.trace("connectToServer()");
        boolean success = super.connectToServer();
        folderEditor.setEnabled(success, getFieldEditorParent());
        return success;
    }

    @Override
    protected void createFieldEditors() {
        log.trace("createFieldEditors()");

        addNicknameEditor();
        addEnabledEditor();
        addHostEditor();
        addUseSslEditor();
        addPortEditor();
        addUsernameEditor();
        addPasswordEditor();
        addConnectEditor();

        addSpacer();

        addFolderEditor();
        addTntUserEditor();
        addMyEmailAddressesEditor();
        addIgnoreEmailAddressesEditor();

        addSpacer();

        addRemoveButton();
    }

    @Override
    protected void onSuccessfulConnection() {
        log.trace("onSuccessfulConnection()");
        // If there was a previously selected folder key, use that
        String oldKey = folderEditor.getSelectionItem();

        // Populate folder list
        folderEditor.removeAll();
        try {
            for (Folder folder : ((ImapServer) server).getFolderList())
                if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0)
                    folderEditor.add(folder.getFullName(), folder.getFullName());
        } catch (MessagingException e) {
            log.error("Unable to determine if folder can hold messages.", e);
        }

        folderEditor.setSelection(oldKey != null ? oldKey : ((ImapServer) server).getFolderName());
    }

    @Override
    protected void savePageSettings() {
        log.trace("savePageSettings()");
        super.savePageSettings();

        ImapServer imapServer = (ImapServer) server;
        imapServer.setFolderName(folderEditor.getSelectionItem());
        imapServer.setHost(hostEditor.getStringValue());
        imapServer.setPort(portEditor.getStringValue());
        imapServer.setPassword(passwordEditor.getStringValue());
        imapServer.setUseSsl(useSslEditor.getBooleanValue());
    }
}
