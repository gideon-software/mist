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
import com.gideonsoftware.mist.model.data.GmailServer;
import com.gideonsoftware.mist.util.Util;
import com.gideonsoftware.mist.util.ui.Images;
import com.gideonsoftware.mist.util.ui.SmartCombo;
import com.gideonsoftware.mist.util.ui.SmartWizardPage;
import com.gideonsoftware.mist.util.ui.ViewUtil;

public class GmailAccountLabelPage extends SmartWizardPage {
    private static Logger log = LogManager.getLogger();

    private Button selectLabelRadio = null;
    private Button newLabelRadio = null;
    private SmartCombo<String> labelCombo = null;
    private Button labelRefreshButton = null;
    private Text newLabelText = null;
    private Button removeLabelCheckbox = null;

    public GmailAccountLabelPage() {
        super("Add Gmail Account: Select Label");
        log.trace("GmailAccountLabelPage()");
        setTitle("Add Gmail Account: Select Label");
        setDescription("Please select a Gmail label.");
    }

    @Override
    public void createControl(Composite parent) {
        log.trace("createControl({})", parent);

        Composite comp = new Composite(parent, SWT.NONE);
        applyGridLayout(comp).numColumns(2);

        Label label = new Label(comp, SWT.NONE);
        label.setText("MIST imports all email from a selected label into your TntConnect database.");
        applyGridData(label).horizontalSpan(2);
        label = new Label(comp, SWT.NONE);
        label.setText("Which label should MIST import from?");
        applyGridData(label).horizontalSpan(2);

        // The radio buttons need to be in the same composite

        selectLabelRadio = new Button(comp, SWT.RADIO);
        selectLabelRadio.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            log.trace("selectLabelRadio.widgetSelected({})", e);
            doSelect(true);
        }));

        Composite labelListComp = new Composite(comp, SWT.NONE);
        applyGridLayout(labelListComp).numColumns(2).marginWidth(0).marginHeight(0);
        labelCombo = new SmartCombo<String>(labelListComp, SWT.BORDER | SWT.READ_ONLY);
        applyGridData(labelCombo).widthHint(ViewUtil.getTextWidth(labelCombo) * 30);
        labelCombo.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            log.trace("labelCombo.widgetSelected({})", e);
            doSelect(true);
        }));

        labelRefreshButton = new Button(labelListComp, SWT.PUSH);
        labelRefreshButton.setImage(Images.getImage(Images.ICON_RELOAD));
        labelRefreshButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            log.trace("labelRefreshButton.widgetSelected({})", e);
            if (loadLabels()) {
                MessageBox msgBox = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
                msgBox.setMessage("Labels reloaded from IMAP server.");
                msgBox.open();
            }
        }));

        newLabelRadio = new Button(comp, SWT.RADIO);
        newLabelRadio.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            log.trace("newLabelRadio.widgetSelected({})", e);
            doSelect(false);
        }));

        Composite newLabelComp = new Composite(comp, SWT.NONE);
        applyGridLayout(newLabelComp).numColumns(2).marginWidth(0).marginHeight(0);
        label = new Label(newLabelComp, SWT.NONE);
        label.setText("Create a new label: ");
        // Allow clicking on the label text (like a regular radio button)
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                super.mouseUp(e);
                if (e.getSource() instanceof Label)
                    doSelect(false);
            }
        });

        newLabelText = new Text(newLabelComp, SWT.BORDER | SWT.SINGLE);
        newLabelText.setText("Import with MIST");
        applyGridData(newLabelText).widthHint(ViewUtil.getTextWidth(newLabelText) * 16);
        newLabelText.addModifyListener(e -> {
            log.trace("newLabelText.modifyText({})", e);
            doSelect(false);
        });

        label = new Label(comp, SWT.NONE);
        label.setText("");
        applyGridData(label).horizontalSpan(2);

        removeLabelCheckbox = new Button(comp, SWT.CHECK);
        removeLabelCheckbox.setText("Remove label from messages after importing");
        removeLabelCheckbox.setSelection(true);
        applyGridData(removeLabelCheckbox).horizontalSpan(2);

        label = new Label(comp, SWT.NONE);
        label.setText("");
        applyGridData(label).horizontalSpan(2);

        label = new Label(comp, SWT.NONE);
        label.setText(
            String.format(
                "Note: to import email from multiple labels on the same Gmail account,%n"
                    + "you should set up an additional Gmail account in MIST."));
        applyGridData(label).horizontalSpan(2);

        setControl(comp); // Needed for page to work properly
        setPageComplete(false);
    }

    private void doSelect(boolean isSelectLabelRadioSelected) {
        selectLabelRadio.setSelection(isSelectLabelRadioSelected);
        newLabelRadio.setSelection(!isSelectLabelRadioSelected);
        labelCombo.setEnabled(isSelectLabelRadioSelected);
        labelRefreshButton.setEnabled(isSelectLabelRadioSelected);
        newLabelText.setEnabled(!isSelectLabelRadioSelected);
        boolean isPageComplete = selectLabelRadio.getSelection() && !labelCombo.getText().isBlank()
            || newLabelRadio.getSelection() && !newLabelText.getText().isBlank();
        setPageComplete(isPageComplete);
    }

    private GmailServer getGmailServer() {
        return ((NewEmailAccountWizard) getWizard()).getGmailAccountConnectionPage().getGmailServer();
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

    public boolean isRemoveLabelChecked() {
        return removeLabelCheckbox.getSelection();
    }

    private boolean loadLabels() {
        // If there was a previously selected label key, use that
        String oldKey = labelCombo.getSelectionItem();

        // Populate label list
        labelCombo.removeAll();
        try {
            for (com.google.api.services.gmail.model.Label label : getGmailServer().getLabelList())
                labelCombo.add(label.getId(), label.getName());
        } catch (EmailServerException e) {
            Util.reportError("Unable to load label list", "Unable to load label list", e);
            return false;
        }

        if (oldKey != null)
            labelCombo.select(oldKey);
        return true;
    }

    @Override
    protected boolean nextPressed() {
        log.trace("nextPressed()");

        if (selectLabelRadio.getSelection()) {
            // Select existing label
            getGmailServer().setLabelId(labelCombo.getSelectionItem());
            getGmailServer().setLabelName(labelCombo.getSelectionValue());
        } else {
            // Create a new label
            try {
                getGmailServer().createLabel(newLabelText.getText());
                // Server label ID & label name are set in createLabel()
            } catch (EmailServerException e) {
                Util.reportError("Unable to create new label", "Unable to create new label", e);
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

        if (!selectLabelRadio.getSelection() && !newLabelRadio.getSelection()) {
            // First time loading
            loadLabels();
            doSelect(true);
        }

        super.setVisible(visible);
    }

}
