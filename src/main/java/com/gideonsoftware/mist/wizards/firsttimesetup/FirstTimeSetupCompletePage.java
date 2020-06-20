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

import static com.gideonsoftware.mist.util.ui.GridLayoutUtil.applyGridLayout;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class FirstTimeSetupCompletePage extends WizardPage {
    private static Logger log = LogManager.getLogger();

    public FirstTimeSetupCompletePage() {
        super("MIST Setup Complete!");
        log.trace("FirstTimeSetupCompletePage()");
        setTitle("MIST Setup Complete!");
        // setDescription("");
    }

    @Override
    public void createControl(Composite parent) {
        log.trace("createControl({})", parent);

        Composite comp = new Composite(parent, SWT.NONE);
        applyGridLayout(comp);

        String msg = """
            Setup is complete! Once you press 'Finish', you'll be ready to begin importing email into TntConnect.

            You can also add more email accounts and configure other settings from the MIST 'Settings' menu.
            """;
        (new Label(comp, SWT.NONE)).setText(msg);

        setControl(comp); // Needed for page to work properly
        setPageComplete(true);
    }
}
