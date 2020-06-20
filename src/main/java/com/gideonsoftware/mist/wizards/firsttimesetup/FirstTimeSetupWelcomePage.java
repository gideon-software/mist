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

package com.gideonsoftware.mist.wizards.firsttimesetup;

import static com.gideonsoftware.mist.util.ui.GridDataUtil.applyGridData;
import static com.gideonsoftware.mist.util.ui.GridLayoutUtil.applyGridLayout;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class FirstTimeSetupWelcomePage extends WizardPage {
    private static Logger log = LogManager.getLogger();

    public FirstTimeSetupWelcomePage() {
        super("Welcome to MIST!");
        log.trace("FirstTimeSetupWelcomePage()");
        setTitle("Welcome to MIST!");
        setDescription("The eMail Import System for TntConnect");
    }

    @Override
    public void createControl(Composite parent) {
        log.trace("createControl({})", parent);

        Composite comp = new Composite(parent, SWT.NONE);
        applyGridLayout(comp);
        applyGridData(comp).withFill();

        String welcomeMsg = """
            MIST is designed to quickly and easily import email messages sent to and received from your TntConnect contacts.

            There are just two steps required to get you started:
            1) Select your TntConnect database
            2) Add an email account to get messages from

            Press Next to begin the setup process.
            """;
        (new Label(comp, SWT.NONE)).setText(welcomeMsg);

        setControl(comp); // Needed for page to work properly
    }

    @Override
    public IWizardPage getNextPage() {
        log.trace("getNextPage()");
        FirstTimeSetupWizard wizard = ((FirstTimeSetupWizard) getWizard());
        return wizard.getSelectTntDbPage();
    }
}
