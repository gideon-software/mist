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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

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

                    // Clear last import info
                    HistoryModel.init();
                    MessageModel.init();

                    // Start Tnt import service (which runs until MIST closes or Tnt settings change)
                    TntDb.startImportService();
                    if (!TntDb.isConnected())
                        return;

                    // Start email import service
                    EmailModel.startImportService();
                }
            }
        });
    }
}
