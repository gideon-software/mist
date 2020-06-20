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

package com.gideonsoftware.mist.wizards.newemailaccount;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.model.EmailModel;
import com.gideonsoftware.mist.model.data.EmailServer;
import com.gideonsoftware.mist.model.data.GmailServer;
import com.gideonsoftware.mist.model.data.ImapServer;
import com.gideonsoftware.mist.util.Util;

/**
 * 
 */
public class NewEmailAccountWizard extends Wizard {
    private static Logger log = LogManager.getLogger();

    protected EmailSelectAccountTypePage emailSelectAccountTypePage;
    protected ImapAccountConnectionPage imapAccountConnectionPage;
    protected ImapAccountFolderPage imapAccountFolderPage;
    protected GmailAccountConnectionPage gmailAccountConnectionPage;
    protected GmailAccountLabelPage gmailAccountLabelPage;
    protected EmailSelectTntUserPage emailSelectTntUserPage;

    @Override
    public void addPages() {
        log.trace("addPages()");

        emailSelectAccountTypePage = new EmailSelectAccountTypePage();
        imapAccountConnectionPage = new ImapAccountConnectionPage();
        imapAccountFolderPage = new ImapAccountFolderPage();
        gmailAccountConnectionPage = new GmailAccountConnectionPage();
        gmailAccountLabelPage = new GmailAccountLabelPage();
        emailSelectTntUserPage = new EmailSelectTntUserPage();

        insertPagesBefore();

        addPage(emailSelectAccountTypePage);
        addPage(imapAccountConnectionPage);
        addPage(imapAccountFolderPage);
        addPage(gmailAccountConnectionPage);
        addPage(gmailAccountLabelPage);
        addPage(emailSelectTntUserPage);

        insertPagesAfter();
    }

    @Override
    public boolean canFinish() {
        log.trace("canFinish()");
        return (imapAccountFolderPage.isPageComplete() || gmailAccountLabelPage.isPageComplete())
            && emailSelectTntUserPage.isPageComplete();
    }

    public EmailSelectAccountTypePage getEmailSelectAccountTypePage() {
        return emailSelectAccountTypePage;
    }

    public EmailSelectTntUserPage getEmailSelectTntUserPage() {
        return emailSelectTntUserPage;
    }

    public GmailAccountConnectionPage getGmailAccountConnectionPage() {
        return gmailAccountConnectionPage;
    }

    public GmailAccountLabelPage getGmailAccountLabelPage() {
        return gmailAccountLabelPage;
    }

    public ImapAccountConnectionPage getImapAccountConnectionPage() {
        return imapAccountConnectionPage;
    }

    public ImapAccountFolderPage getImapAccountFolderPage() {
        return imapAccountFolderPage;
    }

    public WizardPage getPostEmailSetupPage() {
        return null;
    }

    public void insertPagesAfter() {
    }

    public void insertPagesBefore() {
    }

    @Override
    public boolean performFinish() {
        log.trace("performFinish()");

        EmailServer emailServer = null;
        if (emailSelectAccountTypePage.isTypeImap()) {
            ImapServer imapServer = getImapAccountConnectionPage().getImapServer();
            // Guess at "my address": username + domain of email server
            // (.*[\.])?(\w+\.\w+)
            Matcher m = Pattern.compile("(.*[\\.])?(\\w+\\.\\w+)").matcher(imapServer.getHost());
            if (m.matches()) {
                String hostGuess = m.group(2);
                String myAddressGuess = String.format("%s@%s", imapServer.getUsername(), hostGuess);
                imapServer.addMyAddress(myAddressGuess);
            }
            emailServer = imapServer;
        } else if (emailSelectAccountTypePage.isTypeGmail()) {
            GmailServer gmailServer = getGmailAccountConnectionPage().getGmailServer();
            gmailServer.setLabelRemoveAfterImport(getGmailAccountLabelPage().isRemoveLabelChecked());
            gmailServer.addMyAddress(gmailServer.getUsername());
            emailServer = gmailServer;
        } else {
            Util.reportError("Unknown email account type", "Unknown email account type");
            return false;
        }

        emailServer.setTntUserId(getEmailSelectTntUserPage().getTntUserId());
        emailServer.setTntUsername(getEmailSelectTntUserPage().getTntUsername());
        EmailModel.addEmailServer(emailServer);

        EmailModel.disconnectServers();
        MIST.getPrefs().savePreferences();

        return true;
    }
}
