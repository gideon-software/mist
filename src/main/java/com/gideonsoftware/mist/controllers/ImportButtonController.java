/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2010 Gideon Software
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

package com.gideonsoftware.mist.controllers;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.MessageBox;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.model.EmailModel;
import com.gideonsoftware.mist.model.HistoryModel;
import com.gideonsoftware.mist.model.MessageModel;
import com.gideonsoftware.mist.tntapi.TntDb;
import com.gideonsoftware.mist.views.ImportButtonView;

public class ImportButtonController {
    private static Logger log = LogManager.getLogger();

    public ImportButtonController(ImportButtonView view) {
        log.trace("ImportButtonController({})", view);

        view.getImportButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                log.trace("importButton.widgetSelected({})", event);

                if (EmailModel.isImporting()) {
                    // Stop import
                    EmailModel.stopImportService();

                } else {
                    // Start import

                    // Check for lock file
                    if (isLockFileFound()) {
                        MessageBox msgBox = new MessageBox(view.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
                        msgBox.setMessage(
                            "It appears that TntConnect is currently using your TntConnect database. "
                                + "It is recommended that you close TntConnect before using MIST to "
                                + "import email. Otherwise it is possible for data corruption to occur.\n\n"
                                + "Press OK to continue or Cancel to stop the import.");
                        if (msgBox.open() != SWT.OK)
                            return;
                    }

                    //
                    // Begin importing!
                    //

                    // Clear last import info
                    HistoryModel.init();
                    MessageModel.init();

                    // Start Tnt import service (which runs until MIST closes or Tnt settings change)
                    TntDb.startImportService(view.getShell());
                    if (!TntDb.isConnected())
                        return;

                    // Start email import service
                    EmailModel.startImportService(view.getShell());
                }
            }
        });
    }

    public boolean isLockFileFound() {
        String dbPath = MIST.getPrefs().getString(TntDb.PREF_TNT_DBPATH);
        String ext = dbPath.substring(dbPath.indexOf("."));
        String lockfile = dbPath.replace(ext, ".lock");
        return new File(lockfile).exists();
    }
}
