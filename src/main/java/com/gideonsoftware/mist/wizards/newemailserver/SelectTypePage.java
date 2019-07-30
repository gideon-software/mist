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

package com.gideonsoftware.mist.wizards.newemailserver;

import static com.gideonsoftware.mist.util.ui.GridLayoutUtil.applyGridLayout;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class SelectTypePage extends WizardPage {
    private static Logger log = LogManager.getLogger();

    private Button typeImapRadio;
    private Button typeGmailRadio;

    public SelectTypePage() {
        super("Select Server Type");
        log.trace("SelectTypePage()");
        setTitle("Select Server Type");
        setDescription("Select which email server type to add.");
    }

    @Override
    public void createControl(Composite parent) {
        log.trace("createControl({})", parent);

        Composite comp = new Composite(parent, SWT.NONE);
        applyGridLayout(comp);

        typeImapRadio = new Button(comp, SWT.RADIO);
        typeImapRadio.setText("IMAP / IMAPS (IMAP over SSL) server");
        typeImapRadio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                log.trace("typeImapRadio.widgetSelected()");
                NewEmailServerWizard wizard = (NewEmailServerWizard) getWizard();
                wizard.setType(NewEmailServerWizard.TYPE_IMAP);
            }
        });

        typeGmailRadio = new Button(comp, SWT.RADIO);
        typeGmailRadio.setText("Gmail server");
        typeGmailRadio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                log.trace("typeGmailRadio.widgetSelected()");
                NewEmailServerWizard wizard = (NewEmailServerWizard) getWizard();
                wizard.setType(NewEmailServerWizard.TYPE_GMAIL);
            }
        });

        typeImapRadio.setSelection(true); // Wizard defaults to IMAP

        setControl(comp); // Needed for page to work properly
    }

    public boolean isTypeGmail() {
        return typeGmailRadio.getSelection();
    }

    public boolean isTypeImap() {
        return typeImapRadio.getSelection();
    }
}
