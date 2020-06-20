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

import static com.gideonsoftware.mist.util.ui.GridLayoutUtil.applyGridLayout;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.gideonsoftware.mist.model.data.EmailServer;

public class EmailSelectAccountTypePage extends WizardPage {
    private static Logger log = LogManager.getLogger();

    private Button typeImapRadio;
    private Button typeGmailRadio;

    public EmailSelectAccountTypePage() {
        super("Add Email Account: Select Account Type");
        log.trace("EmailSelectAccountTypePage()");
        setTitle("Add Email Account: Select Account Type");
        setDescription("Please select your email account type.");
    }

    @Override
    public void createControl(Composite parent) {
        log.trace("createControl({})", parent);

        Composite comp = new Composite(parent, SWT.NONE);
        applyGridLayout(comp);

        typeImapRadio = new Button(comp, SWT.RADIO);
        typeImapRadio.setText("IMAP: a common way to access your email across multiple devices");
        typeGmailRadio = new Button(comp, SWT.RADIO);
        typeGmailRadio.setText("Gmail: Google's email service");

        typeImapRadio.setSelection(true); // Wizard defaults to IMAP

        setControl(comp); // Needed for page to work properly
    }

    @Override
    public IWizardPage getNextPage() {
        log.trace("getNextPage()");
        NewEmailAccountWizard wizard = ((NewEmailAccountWizard) getWizard());
        if (isTypeImap())
            return wizard.getImapAccountConnectionPage();
        else
            return wizard.getGmailAccountConnectionPage();
    }

    public String getType() {
        if (isTypeImap())
            return EmailServer.TYPE_IMAP;
        else if (isTypeGmail())
            return EmailServer.TYPE_GMAIL;
        return null;
    }

    public boolean isTypeGmail() {
        return typeGmailRadio.getSelection();
    }

    public boolean isTypeImap() {
        return typeImapRadio.getSelection();
    }

}
