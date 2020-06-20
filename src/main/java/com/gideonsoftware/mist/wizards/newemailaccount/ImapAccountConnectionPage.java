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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.gideonsoftware.mist.model.EmailModel;
import com.gideonsoftware.mist.model.data.ImapServer;
import com.gideonsoftware.mist.util.Util;
import com.gideonsoftware.mist.util.ui.SmartWizardPage;
import com.gideonsoftware.mist.util.ui.ViewUtil;

public class ImapAccountConnectionPage extends SmartWizardPage {
    private static Logger log = LogManager.getLogger();

    // Controls
    private Text hostText = null;
    private Button useSslButton = null;
    private Text portText = null;
    private Text usernameText = null;
    private Text passwordText = null;

    // New IMAP server
    private ImapServer imapServer = null;

    public ImapAccountConnectionPage() {
        super("Add IMAP Account: Enter Connection Settings");
        log.trace("ImapAccountConnectionPage()");
        setTitle("Add IMAP Account: Enter Connection Settings");
        setDescription("Please enter your IMAP connection settings.");
    }

    @Override
    public boolean canFlipToNextPage() {
        log.trace("canFlipToNextPage()");
        return true;
    }

    @Override
    public void createControl(Composite parent) {
        log.trace("createControl({})", parent);

        Composite comp = new Composite(parent, SWT.NONE);
        applyGridLayout(comp).numColumns(2);

        // Host
        (new Label(comp, SWT.NONE)).setText("Your IMAP (email) host:");
        hostText = new Text(comp, SWT.BORDER | SWT.SINGLE);
        applyGridData(hostText).withHorizontalFill();
        new Label(comp, SWT.NONE);
        (new Label(comp, SWT.NONE)).setText("Ex: imap.myministry.com");

        // Use SSL checkbox
        new Label(comp, SWT.NONE);
        useSslButton = new Button(comp, SWT.CHECK);
        useSslButton.setText("Use SSL (IMAPS)");
        useSslButton.setSelection(true);
        useSslButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            log.trace("useSslButton.widgetSelected({})", e);
            if (portText == null)
                return;

            // Change default port (if port is blank or already on alternative default port)
            if (useSslButton.getSelection()
                && (portText.getText().isBlank()
                    || Integer.valueOf(portText.getText()) == ImapServer.DEFAULT_PORT_IMAP))
                portText.setText(String.valueOf(ImapServer.DEFAULT_PORT_IMAPS));
            else if (!useSslButton.getSelection()
                && (portText.getText().isBlank()
                    || Integer.valueOf(portText.getText()) == ImapServer.DEFAULT_PORT_IMAPS))
                portText.setText(String.valueOf(ImapServer.DEFAULT_PORT_IMAP));
        }));

        // Port
        new Label(comp, SWT.NONE);
        Composite portComp = new Composite(comp, SWT.NONE);
        applyGridLayout(portComp).numColumns(2).marginWidth(0).marginTop(0);
        (new Label(portComp, SWT.NONE)).setText("Port:");
        portText = new Text(portComp, SWT.BORDER | SWT.SINGLE);
        portText.setText(String.valueOf(ImapServer.DEFAULT_PORT_IMAPS));
        applyGridData(portText).widthHint(ViewUtil.getTextWidth(portText) * 5);

        // Username
        (new Label(comp, SWT.NONE)).setText("Your username:");
        usernameText = new Text(comp, SWT.BORDER | SWT.SINGLE);
        applyGridData(usernameText).withHorizontalFill();

        // Password
        (new Label(comp, SWT.NONE)).setText("Your password:");
        passwordText = new Text(comp, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
        applyGridData(passwordText).withHorizontalFill();

        setControl(comp); // Needed for page to work properly
    }

    public ImapServer getImapServer() {
        return imapServer;
    }

    @Override
    public IWizardPage getNextPage() {
        log.trace("getNextPage()");

        if (hostText.getText().isBlank()) {
            setErrorMessage("Please enter the IMAP host.");
            return null;
        } else if (portText.getText().isBlank()) {
            setErrorMessage("Please enter the IMAP port.");
            return null;
        } else if (usernameText.getText().isBlank()) {
            setErrorMessage("Please enter your username.");
            return null;
        }
        return ((NewEmailAccountWizard) getWizard()).getImapAccountFolderPage();
    }

    @Override
    public boolean nextPressed() {
        log.trace("nextPressed()");

        // Test the connection
        imapServer = new ImapServer(EmailModel.getEmailServerCount());
        imapServer.setNickname(ImapServer.NEW_NICKNAME, true);
        imapServer.setHost(hostText.getText());
        imapServer.setUseSsl(useSslButton.getSelection());
        imapServer.setPort(portText.getText());
        imapServer.setUsername(usernameText.getText());
        imapServer.setPassword(passwordText.getText());
        Util.connectToEmailServer(imapServer);

        return imapServer.isConnected();
    }
}
