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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.gideonsoftware.mist.model.EmailModel;
import com.gideonsoftware.mist.model.data.GmailServer;
import com.gideonsoftware.mist.util.Util;
import com.gideonsoftware.mist.util.ui.Images;

public class GmailAccountConnectionPage extends WizardPage {
    private static Logger log = LogManager.getLogger();

    // New Gmail server
    private GmailServer gmailServer = null;

    public GmailAccountConnectionPage() {
        super("Add Gmail Account: Connect Google Account");
        log.trace("GmailAccountConnectionPage()");
        setTitle("Add Gmail Account: Connect Google Account");
        setDescription("Please sign in to your Google account.");
    }

    @Override
    public void createControl(Composite parent) {
        log.trace("createControl({})", parent);

        Composite comp = new Composite(parent, SWT.NONE);
        applyGridLayout(comp);

        Button signInButton = new Button(comp, SWT.PUSH);
        signInButton.setImage(Images.getImage(Images.SIGNIN_GOOGLE));
        signInButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            log.trace("signInButton.widgetSelected({})", e);

            // TODO: What if this is pushed more than once? Reset!
            gmailServer = new GmailServer(EmailModel.getEmailServerCount());
            gmailServer.setNickname(GmailServer.NEW_NICKNAME, true);
            Util.connectToEmailServer(gmailServer);
            setPageComplete(gmailServer.isConnected());

            // Go to next page automatically
            if (canFlipToNextPage()) {
                IWizardPage page = getNextPage();
                if (page != null) {
                    getContainer().showPage(page);
                }
            }
        }));

        setControl(comp); // Needed for page to work properly
        setPageComplete(false);
    }

    public GmailServer getGmailServer() {
        return gmailServer;
    }

    @Override
    public IWizardPage getNextPage() {
        log.trace("getNextPage()");

        if (gmailServer == null || !gmailServer.isConnected())
            return null;

        return ((NewEmailAccountWizard) getWizard()).getGmailAccountLabelPage();
    }

}
