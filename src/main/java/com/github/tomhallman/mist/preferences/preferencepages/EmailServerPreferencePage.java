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

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.model.EmailModel;
import com.github.tomhallman.mist.model.data.EmailFolder;
import com.github.tomhallman.mist.model.data.EmailServer;
import com.github.tomhallman.mist.preferences.fieldeditors.AddEditRemoveListFieldEditor;
import com.github.tomhallman.mist.preferences.fieldeditors.ButtonFieldEditor;
import com.github.tomhallman.mist.preferences.fieldeditors.ForgettablePasswordFieldEditor;
import com.github.tomhallman.mist.preferences.fieldeditors.SmartComboFieldEditor;
import com.github.tomhallman.mist.preferences.fieldeditors.SpacerFieldEditor;
import com.github.tomhallman.mist.tntapi.TntDb;
import com.github.tomhallman.mist.tntapi.UserManager;
import com.github.tomhallman.mist.tntapi.entities.User;
import com.github.tomhallman.mist.util.Util;
import com.github.tomhallman.mist.util.ui.Images;

/**
 *
 */
public class EmailServerPreferencePage extends FieldEditorPreferencePage {
    private static Logger log = LogManager.getLogger();

    private StringFieldEditor nicknameEditor;
    private StringFieldEditor hostEditor;
    private IntegerFieldEditor portEditor;
    private StringFieldEditor usernameEditor;
    private ForgettablePasswordFieldEditor passwordEditor;
    private ButtonFieldEditor connectButton;
    private SmartComboFieldEditor<String> folderEditor;
    private SmartComboFieldEditor<Integer> tntUserEditor;
    private AddEditRemoveListFieldEditor myEmailAddressesEditor;
    private AddEditRemoveListFieldEditor ignoreAddressesEditor;
    private ButtonFieldEditor deleteButton;

    private EmailServer server;

    public EmailServerPreferencePage(int serverId) {
        super(FieldEditorPreferencePage.GRID);
        log.trace("EmailServerPreferencePage({})", serverId);
        server = new EmailServer(serverId);
        setTitle(server.getNickname());
        setImageDescriptor(ImageDescriptor.createFromImage(Images.getImage(Images.ICON_EMAIL_SERVER)));
        // setDescription("description here");
        noDefaultAndApplyButton();
    }

    protected boolean connectToServer() {
        savePageSettings();

        Util.connectToEmailServer(getShell(), server, false);
        boolean success = server.isConnected();
        if (success) {
            // Save the password
            passwordEditor.setPassword(server.getPassword());
            passwordEditor.setPrompt(server.isPasswordPrompt());

            // If there was a previously selected key, use that
            String oldKey = folderEditor.getSelectionItem();

            // Populate folder list
            folderEditor.removeAll();
            for (EmailFolder emailFolder : server.getCompleteFolderList())
                if (emailFolder.canHoldMessages())
                    folderEditor.add(emailFolder.getFullFolderName(), emailFolder.getFullFolderName());

            folderEditor.setSelection(oldKey != null ? oldKey : server.getFolder());

            // All done with the server for now
            server.disconnect();
        }
        folderEditor.setEnabled(success, getFieldEditorParent());
        return success;
    }

    @Override
    protected void createFieldEditors() {
        log.trace("createFieldEditors()");

        // Nickname
        nicknameEditor = new StringFieldEditor(
            server.getPrefName(EmailServer.PREF_NICKNAME),
            "&Nickname:",
            StringFieldEditor.UNLIMITED,
            StringFieldEditor.VALIDATE_ON_KEY_STROKE,
            getFieldEditorParent());
        nicknameEditor.setEmptyStringAllowed(false);
        nicknameEditor.setErrorMessage("Nickname may not be empty.");
        nicknameEditor.getTextControl(getFieldEditorParent()).addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                // Change the nickname dynamically
                Text text = (Text) event.widget;
                setTitle(text.getText());
                ((PreferenceDialog) getContainer()).getTreeViewer().refresh();
            }
        });
        addField(nicknameEditor);

        // Host
        hostEditor = new StringFieldEditor(server.getPrefName(EmailServer.PREF_HOST), "&Host:", getFieldEditorParent());
        hostEditor.setEmptyStringAllowed(false);
        hostEditor.setErrorMessage("Host may not be empty.");
        addField(hostEditor);

        // Port
        portEditor = new IntegerFieldEditor(
            server.getPrefName(EmailServer.PREF_PORT),
            "P&ort:",
            getFieldEditorParent());
        portEditor.setEmptyStringAllowed(false);
        portEditor.setErrorMessage("Port must be a number.");
        addField(portEditor);

        // Username
        usernameEditor = new StringFieldEditor(
            server.getPrefName(EmailServer.PREF_USERNAME),
            "&Username:",
            getFieldEditorParent());
        usernameEditor.setEmptyStringAllowed(false);
        usernameEditor.setErrorMessage("Username may not be empty.");
        addField(usernameEditor);

        // Password
        passwordEditor = new ForgettablePasswordFieldEditor(
            server.getPrefName(EmailServer.PREF_PASSWORD),
            server.getPrefName(EmailServer.PREF_PASSWORD_PROMPT),
            "&Password:",
            getFieldEditorParent());
        addField(passwordEditor);

        // Connect button
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
                    msgBox.setMessage("Connection successful!");
                    msgBox.open();
                }
            }
        });
        addField(connectButton);

        // Spacer
        addField(new SpacerFieldEditor(getFieldEditorParent()));

        // Folder
        folderEditor = new SmartComboFieldEditor<String>(
            server.getPrefName(EmailServer.PREF_FOLDER),
            "&Folder:",
            getFieldEditorParent(),
            true);
        folderEditor.setEmptySelectionAllowed(false);
        if (!server.getFolder().isEmpty()) {
            // We're not connected to the email server, but we know the folder from previous preferences
            folderEditor.add(server.getFolder(), server.getFolder());
            folderEditor.setSelection(server.getFolder());
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

        // Tnt User ID (and username)
        // Note: Tnt Username must be treated specially, since fieldEditor doesn't know about it directly
        // See performOk
        String tntUserIdPrefName = server.getPrefName(EmailServer.PREF_TNT_USERID);
        tntUserEditor = new SmartComboFieldEditor<Integer>(
            tntUserIdPrefName,
            "&TntConnect User:",
            getFieldEditorParent(),
            true);
        tntUserEditor.setEmptySelectionAllowed(false);
        tntUserEditor.setErrorMessage("A TntConnect user must be selected.");
        Button tntUserEditorButton = tntUserEditor.getButtonControl(getFieldEditorParent());
        tntUserEditorButton.setImage(Images.getImage(Images.ICON_RELOAD));
        tntUserEditorButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                log.trace("tntUserEditorButton.widgetSelected({})", event);
                // Try to populate the control from the Tnt DB
                Util.connectToTntDatabase(getShell());

                if (TntDb.isConnected()) {
                    // If there was a previously selected key, use that
                    Integer oldKey = tntUserEditor.getSelectionItem();

                    // Populate user list
                    tntUserEditor.removeAll();
                    try {
                        for (User user : UserManager.getUserList())
                            tntUserEditor.add(user.getId(), user.getUsername());
                        tntUserEditor.setEnabled(true, getFieldEditorParent());
                    } catch (SQLException e) {
                        log.warn("Could not add users to user list", e);
                        tntUserEditor.setEnabled(false, getFieldEditorParent());
                    }
                    tntUserEditor.setSelection(oldKey != null ? oldKey : server.getTntUserId());

                    MessageBox msgBox = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
                    msgBox.setMessage("Users reloaded from TntConnect.");
                    msgBox.open();
                }
            }
        });
        if (server.getTntUserId() != 0) {
            // Load cached username into combo and select
            tntUserEditor.add(server.getTntUserId(), server.getTntUsername());
            tntUserEditor.setSelection(server.getTntUserId());
        }
        tntUserEditor.setEnabled(false, getFieldEditorParent());
        addField(tntUserEditor);

        // My email addresses
        myEmailAddressesEditor = new AddEditRemoveListFieldEditor(
            server.getPrefName(EmailServer.PREF_ADDRESSES_MY),
            "My email &addresses:",
            getFieldEditorParent());
        myEmailAddressesEditor.setAddDialogMessage("Add email address");
        myEmailAddressesEditor.setAddDialogMessage("Edit email address");
        myEmailAddressesEditor.setDialogDescription(
            "These are used to determine whether you wrote to a contact or a contact wrote to you");
        myEmailAddressesEditor.setMinListSize(1);
        myEmailAddressesEditor.setErrorMessage("'My email addresses' must contain at least one email address.");
        addField(myEmailAddressesEditor);

        // Email addresses to ignore
        ignoreAddressesEditor = new AddEditRemoveListFieldEditor(
            server.getPrefName(EmailServer.PREF_ADDRESSES_IGNORE),
            "Email addresses to &ignore:",
            getFieldEditorParent());
        ignoreAddressesEditor.setAddDialogMessage("Add email address to ignore");
        ignoreAddressesEditor.setEditDialogMessage("Edit email address to ignore");
        ignoreAddressesEditor.setDialogDescription(
            "Emails to or from these addresses will not be imported;"
                + System.lineSeparator()
                + "(Use * for any string and ? for any character)");
        addField(ignoreAddressesEditor);

        // Spacer
        addField(new SpacerFieldEditor(getFieldEditorParent()));

        // Delete button
        deleteButton = new ButtonFieldEditor("&Delete this Email Server...", getFieldEditorParent());
        deleteButton.getButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                log.trace("deleteButton.widgetSelected({})", event);

                // Check with the user
                MessageBox msgBox = new MessageBox(getShell(), SWT.YES | SWT.NO | SWT.ICON_WARNING);
                msgBox.setMessage("Are you sure you want to DELETE this email server?");
                if (msgBox.open() == SWT.YES) {
                    // Remove server from model
                    EmailModel.removeEmailServer(server);
                    MIST.getPreferenceManager().postEmailServerRemoved();
                }
            }
        });
        addField(deleteButton);
    }

    @Override
    public boolean performOk() {
        boolean ok = super.performOk();
        if (ok) {
            // Save the Tnt username; the tntUserEditor doesn't know to do this!
            if (tntUserEditor != null) // We may not have loaded the page!
                MIST.getPrefs().setValue(
                    server.getPrefName(EmailServer.PREF_TNT_USERNAME),
                    tntUserEditor.getSelectionValue());
        }
        return ok;
    }

    private void savePageSettings() {
        server.setNickname(nicknameEditor.getStringValue());
        server.setHost(hostEditor.getStringValue());
        server.setPort(portEditor.getStringValue());
        server.setUsername(usernameEditor.getStringValue());
        server.setPassword(passwordEditor.getPassword());
        server.setPasswordPrompt(passwordEditor.isPrompt());
        server.setFolderName(folderEditor.getSelectionItem());
        server.setTntUserId(tntUserEditor.getSelectionItem());
        server.setTntUsername(tntUserEditor.getSelectionValue());
        server.setMyAddresses(myEmailAddressesEditor.getItems());
        server.setIgnoreAddresses(ignoreAddressesEditor.getItems());
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            nicknameEditor.setFocus();
            if (EmailServer.NEW_NICKNAME.equals(server.getNickname()))
                nicknameEditor.getTextControl(getFieldEditorParent()).selectAll();
        } else {
            savePageSettings();
        }
    }

}
