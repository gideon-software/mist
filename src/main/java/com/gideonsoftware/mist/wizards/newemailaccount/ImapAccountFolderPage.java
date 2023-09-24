/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2020 Gideon Software
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

package com.gideonsoftware.mist.wizards.newemailaccount;

import static com.gideonsoftware.mist.util.ui.GridDataUtil.applyGridData;
import static com.gideonsoftware.mist.util.ui.GridLayoutUtil.applyGridLayout;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import com.gideonsoftware.mist.exceptions.EmailServerException;
import com.gideonsoftware.mist.model.data.ImapServer;
import com.gideonsoftware.mist.util.Util;
import com.gideonsoftware.mist.util.ui.Images;
import com.gideonsoftware.mist.util.ui.SmartCombo;
import com.gideonsoftware.mist.util.ui.SmartWizardPage;
import com.gideonsoftware.mist.util.ui.ViewUtil;

import jakarta.mail.Folder;
import jakarta.mail.MessagingException;

public class ImapAccountFolderPage extends SmartWizardPage {
    private static Logger log = LogManager.getLogger();

    private Button selectFolderRadio = null;
    private Button newFolderRadio = null;
    private SmartCombo<String> folderCombo = null;
    private Button folderRefreshButton = null;
    private Text newFolderText = null;

    public ImapAccountFolderPage() {
        super("Add IMAP Account: Select Folder");
        log.trace("ImapAccountFolderPage()");
        setTitle("Add IMAP Account: Select Folder");
        setDescription("Please select an IMAP folder.");
    }

    @Override
    public void createControl(Composite parent) {
        log.trace("createControl({})", parent);

        Composite comp = new Composite(parent, SWT.NONE);
        applyGridLayout(comp).numColumns(2);

        Label label = new Label(comp, SWT.NONE);
        label.setText("MIST imports all email from a selected folder into your TntConnect database.");
        applyGridData(label).horizontalSpan(2);
        label = new Label(comp, SWT.NONE);
        label.setText("Which folder should MIST import from?");
        applyGridData(label).horizontalSpan(2);

        // The radio buttons need to be in the same composite

        selectFolderRadio = new Button(comp, SWT.RADIO);
        selectFolderRadio.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            log.trace("selectFolderRadio.widgetSelected({})", e);
            doSelect(true);
        }));

        Composite folderListComp = new Composite(comp, SWT.NONE);
        applyGridLayout(folderListComp).numColumns(2).marginWidth(0).marginHeight(0);
        folderCombo = new SmartCombo<String>(folderListComp, SWT.BORDER | SWT.READ_ONLY);
        applyGridData(folderCombo).widthHint(ViewUtil.getTextWidth(folderCombo) * 30);
        folderCombo.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            log.trace("folderCombo.widgetSelected({})", e);
            doSelect(true);
        }));

        folderRefreshButton = new Button(folderListComp, SWT.PUSH);
        folderRefreshButton.setImage(Images.getImage(Images.ICON_RELOAD));
        folderRefreshButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            log.trace("folderRefreshButton.widgetSelected({})", e);
            if (loadFolders()) {
                MessageBox msgBox = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
                msgBox.setMessage("Folders reloaded from IMAP server.");
                msgBox.open();
            }
        }));

        newFolderRadio = new Button(comp, SWT.RADIO);
        newFolderRadio.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            log.trace("newFolderRadio.widgetSelected({})", e);
            doSelect(false);
        }));

        Composite newFolderComp = new Composite(comp, SWT.NONE);
        applyGridLayout(newFolderComp).numColumns(2).marginWidth(0).marginHeight(0);
        label = new Label(newFolderComp, SWT.NONE);
        label.setText("Create a new folder: ");
        // Allow clicking on the label text (like a regular radio button)
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                super.mouseUp(e);
                if (e.getSource() instanceof Label)
                    doSelect(false);
            }
        });

        newFolderText = new Text(newFolderComp, SWT.BORDER | SWT.SINGLE);
        newFolderText.setText("Import with MIST");
        applyGridData(newFolderText).widthHint(ViewUtil.getTextWidth(newFolderText) * 16);
        newFolderText.addModifyListener(e -> {
            log.trace("newFolderText.modifyText({})", e);
            doSelect(false);
        });

        new Label(comp, SWT.NONE);
        label = new Label(comp, SWT.NONE);
        label.setText(
            String.format(
                "Note: to import email from multiple folders on the same email account,%n"
                    + "you should set up an additional email account in MIST."));

        applyGridData(label).horizontalSpan(2);

        setControl(comp); // Needed for page to work properly
        setPageComplete(false);
    }

    private void doSelect(boolean isSelectFolderRadioSelected) {
        selectFolderRadio.setSelection(isSelectFolderRadioSelected);
        newFolderRadio.setSelection(!isSelectFolderRadioSelected);
        folderCombo.setEnabled(isSelectFolderRadioSelected);
        folderRefreshButton.setEnabled(isSelectFolderRadioSelected);
        newFolderText.setEnabled(!isSelectFolderRadioSelected);
        boolean isPageComplete = selectFolderRadio.getSelection() && !folderCombo.getText().isBlank()
            || newFolderRadio.getSelection() && !newFolderText.getText().isBlank();
        setPageComplete(isPageComplete);
    }

    private ImapServer getImapServer() {
        return ((NewEmailAccountWizard) getWizard()).getImapAccountConnectionPage().getImapServer();
    }

    @Override
    public IWizardPage getNextPage() {
        log.trace("getNextPage()");

        NewEmailAccountWizard wizard = (NewEmailAccountWizard) getWizard();
        if (wizard.getEmailSelectTntUserPage().getUserCombo().getItemsMap().size() == 1) {
            // Skip the Tnt User Selection page entirely if there's only one TntDb user
            wizard.getEmailSelectTntUserPage().setPageComplete(true);
            return wizard.getPostEmailSetupPage();
        } else
            return wizard.getEmailSelectTntUserPage();
    }

    private boolean loadFolders() {
        // If there was a previously selected folder key, use that
        String oldKey = folderCombo.getSelectionItem();

        // Populate folder list
        folderCombo.removeAll();
        try {
            for (Folder folder : getImapServer().getFolderList())
                if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0)
                    folderCombo.add(folder.getFullName(), folder.getFullName());
        } catch (MessagingException e) {
            Util.reportError("Unable to load folder list", "Unable to load folder list", e);
            return false;
        }

        if (oldKey != null)
            folderCombo.select(oldKey);
        return true;
    }

    @Override
    protected boolean nextPressed() {
        log.trace("nextPressed()");

        if (selectFolderRadio.getSelection()) {
            // Select existing folder
            getImapServer().setFolderName(folderCombo.getSelectionValue());
        } else {
            // Create a new folder
            try {
                getImapServer().createFolder(newFolderText.getText());
                getImapServer().setFolderName(newFolderText.getText());
            } catch (EmailServerException e) {
                Util.reportError("Unable to create new folder", "Unable to create new folder", e);
                return false;
            }
        }

        return true;
    }

    @Override
    public void setVisible(boolean visible) {
        log.trace("setVisible({})", visible);

        if (!visible) {
            super.setVisible(visible);
            return;
        }

        if (!selectFolderRadio.getSelection() && !newFolderRadio.getSelection()) {
            // First time loading
            loadFolders();
            doSelect(true);
        }

        super.setVisible(visible);
    }

}
