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
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

import com.gideonsoftware.mist.tntapi.TntDb;
import com.gideonsoftware.mist.util.Util;

public class SelectTntDbPage extends WizardPage {
    private static Logger log = LogManager.getLogger();

    private String tntDbPath = null;

    public SelectTntDbPage() {
        super("Select TntConnect database");
        log.trace("SelectTntDbPage()");
        setTitle("Select TntConnect database");
        setDescription("Please select your TntConnect database.");
    }

    @Override
    public void createControl(Composite parent) {
        log.trace("createControl({})", parent);

        Composite comp = new Composite(parent, SWT.NONE);
        applyGridLayout(comp).numColumns(2);

        // Path text box
        Text tntDbText = new Text(comp, SWT.BORDER | SWT.SINGLE);
        tntDbText.setEditable(false);
        applyGridData(tntDbText).withHorizontalFill();

        // Browse button
        Button browseButton = new Button(comp, SWT.PUSH);
        browseButton.setText("&Browse...");
        browseButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            log.trace("browseButton.widgetSelected({})", e);
            FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
            dialog.setFilterNames(new String[] { "TntConnect databases", "All Files" });
            dialog.setFilterExtensions(new String[] { "*.mpddb", "*.*" });
            tntDbPath = dialog.open();
            if (tntDbPath != null) {
                tntDbText.setText(Dialog.shortenText(tntDbPath, tntDbText));
                TntDb.setTntDatabasePath(tntDbPath);
                Util.connectToTntDatabase();
                if (TntDb.isConnected()) {
                    // Populate user list
                    ((FirstTimeSetupWizard) getWizard()).getEmailSelectTntUserPage().loadUsers();
                    TntDb.disconnect(); // Force a disconnect in case the user tries to open TntConnect during setup
                    setPageComplete(true);
                } else
                    setPageComplete(false);
            }
        }));

        setControl(comp); // Needed for page to work properly
        setPageComplete(false);
    }

    @Override
    public IWizardPage getNextPage() {
        log.trace("getNextPage()");
        FirstTimeSetupWizard wizard = ((FirstTimeSetupWizard) getWizard());
        return wizard.getEmailSelectAccountTypePage();
    }

    public String getTntDbPath() {
        return tntDbPath;
    }

}
