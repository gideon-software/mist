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

package com.gideonsoftware.mist.preferences.preferencepages;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.model.EmailModel;
import com.gideonsoftware.mist.model.data.EmailServer;
import com.gideonsoftware.mist.preferences.fieldeditors.AddEditRemoveListFieldEditor;
import com.gideonsoftware.mist.preferences.fieldeditors.ButtonFieldEditor;
import com.gideonsoftware.mist.preferences.fieldeditors.SmartComboFieldEditor;
import com.gideonsoftware.mist.preferences.fieldeditors.SpacerFieldEditor;
import com.gideonsoftware.mist.tntapi.TntDb;
import com.gideonsoftware.mist.tntapi.UserManager;
import com.gideonsoftware.mist.tntapi.entities.User;
import com.gideonsoftware.mist.util.Util;
import com.gideonsoftware.mist.util.ui.Images;

/**
 * 
 */
public abstract class EmailServerPreferencePage extends FieldEditorPreferencePage {
	private static Logger log = LogManager.getLogger();

	protected BooleanFieldEditor enabledEditor;
	protected StringFieldEditor nicknameEditor;
	protected StringFieldEditor usernameEditor;
	protected SmartComboFieldEditor<Integer> tntUserEditor;
	protected AddEditRemoveListFieldEditor myEmailAddressesEditor;
	protected AddEditRemoveListFieldEditor ignoreAddressesEditor;
	protected ButtonFieldEditor removeButton;

	protected EmailServer server;

	public EmailServerPreferencePage(int serverId) {
		super(FieldEditorPreferencePage.GRID);
		log.trace("EmailServerPreferencePage({})", serverId);
		noDefaultAndApplyButton();
	}

	protected void addEnabledEditor() {
		log.trace("addEnabledEditor()");
		enabledEditor = new BooleanFieldEditor(server.getPrefName(EmailServer.PREF_ENABLED), "&Enabled",
				getFieldEditorParent());
		addField(enabledEditor);
	}

	protected void addIgnoreEmailAddressesEditor() {
		log.trace("addIgnoreEmailAddressesEditor()");
		ignoreAddressesEditor = new AddEditRemoveListFieldEditor(server.getPrefName(EmailServer.PREF_ADDRESSES_IGNORE),
				"Email addresses to &ignore:", getFieldEditorParent());
		ignoreAddressesEditor.setAddDialogMessage("Add email address to ignore");
		ignoreAddressesEditor.setEditDialogMessage("Edit email address to ignore");
		ignoreAddressesEditor.setDialogDescription("Emails to or from these addresses will not be imported;"
				+ System.lineSeparator() + "(Use * for any string and ? for any character)");
		addField(ignoreAddressesEditor);
	}

	protected void addMyEmailAddressesEditor() {
		log.trace("addMyEmailAddressesEditor()");
		myEmailAddressesEditor = new AddEditRemoveListFieldEditor(server.getPrefName(EmailServer.PREF_ADDRESSES_MY),
				"My email &addresses:", getFieldEditorParent());
		myEmailAddressesEditor.setAddDialogMessage("Add email address");
		myEmailAddressesEditor.setAddDialogMessage("Edit email address");
		myEmailAddressesEditor.setDialogDescription(
				"These are used to determine whether you wrote to a contact or a contact wrote to you");
		myEmailAddressesEditor.setMinListSize(1);
		myEmailAddressesEditor.setErrorMessage("'My email addresses' must contain at least one email address.");
		addField(myEmailAddressesEditor);
	}

	protected void addNicknameEditor() {
		log.trace("addNicknameEditor()");
		nicknameEditor = new StringFieldEditor(server.getPrefName(EmailServer.PREF_NICKNAME), "&Nickname:",
				StringFieldEditor.UNLIMITED, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
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
	}

	protected void addRemoveButton() {
		log.trace("addRemoveButton()");
		removeButton = new ButtonFieldEditor("&Remove this email account...", getFieldEditorParent());
		removeButton.getButton().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				log.trace("removeButton.widgetSelected({})", event);

				// Check with the user
				MessageBox msgBox = new MessageBox(getShell(), SWT.YES | SWT.NO | SWT.ICON_WARNING);
				msgBox.setMessage("Are you sure you want to remove this email account from MIST?");
				if (msgBox.open() == SWT.YES) {
					// Remove server from model
					EmailModel.removeEmailServer(server);
					MIST.getPreferenceManager().postEmailServerRemoved();
				}
			}
		});
		addField(removeButton);
	}

	protected void addSpacer() {
		log.trace("addSpacer()");
		addField(new SpacerFieldEditor(getFieldEditorParent()));
	}

	protected void addTntUserEditor() {
		log.trace("addTntUserEditor()");
		// Note: Tnt Username must be treated specially, since fieldEditor doesn't know
		// about it directly
		// See performOk
		String tntUserIdPrefName = server.getPrefName(EmailServer.PREF_TNT_USERID);
		tntUserEditor = new SmartComboFieldEditor<Integer>(tntUserIdPrefName, "&TntConnect User:",
				getFieldEditorParent(), true);
		tntUserEditor.setEmptySelectionAllowed(false);
		tntUserEditor.setErrorMessage("A TntConnect user must be selected.");
		Button tntUserEditorButton = tntUserEditor.getButtonControl(getFieldEditorParent());
		tntUserEditorButton.setImage(Images.getImage(Images.ICON_RELOAD));
		tntUserEditorButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				log.trace("tntUserEditorButton.widgetSelected({})", event);
				// Try to populate the control from the Tnt DB
				Util.connectToTntDatabase();

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
	}

	protected void addUsernameEditor() {
		log.trace("addUsernameEditor()");
		usernameEditor = new StringFieldEditor(server.getPrefName(EmailServer.PREF_USERNAME), "&Username:",
				getFieldEditorParent());
		usernameEditor.setEmptyStringAllowed(false);
		usernameEditor.setErrorMessage("Username may not be empty.");
		addField(usernameEditor);
	}

	protected boolean connectToServer() {
		log.trace("connectToServer()");
		savePageSettings();

		Util.connectToEmailServer(server);
		boolean success = server.isConnected();
		if (success) {
			// Do custom stuff!
			onSuccessfulConnection();

			// All done with the server for now
			server.disconnect();
		}
		return success;
	}

	@Override
	abstract protected void createFieldEditors();

	abstract protected void onSuccessfulConnection();

	@Override
	public boolean performOk() {
		log.trace("performOk()");
		boolean ok = super.performOk();
		if (ok) {
			// Save the Tnt username; the tntUserEditor doesn't know to do this!
			if (tntUserEditor != null) // We may not have loaded the page!
				server.setTntUsername(tntUserEditor.getSelectionValue());
		}
		return ok;
	}

	protected void savePageSettings() {
		log.trace("savePageSettings()");
		server.setEnabled(enabledEditor.getBooleanValue());
		server.setNickname(nicknameEditor.getStringValue());
		server.setUsername(usernameEditor.getStringValue());
		server.setTntUserId(tntUserEditor.getSelectionItem());
		server.setTntUsername(tntUserEditor.getSelectionValue());
		server.setMyAddresses(myEmailAddressesEditor.getItems());
		server.setIgnoreAddresses(ignoreAddressesEditor.getItems());
	}

	@Override
	public void setVisible(boolean visible) {
		log.trace("setVisible({})", visible);
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
