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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.model.EmailModel;
import com.gideonsoftware.mist.tntapi.TntDb;
import com.gideonsoftware.mist.util.ui.CocoaSWTUIEnhancer;
import com.gideonsoftware.mist.views.AboutView;
import com.gideonsoftware.mist.views.MainMenuView;

public class MainMenuController {
    private static Logger log = LogManager.getLogger();

    public MainMenuController(MainMenuView view, MainWindowController mainController) {
        log.trace("MainMenuController({},{})", view, mainController);

        Listener exitListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                log.trace("exitListener.handleEvent({})", event);
                event.doit = mainController.closeView();
            }
        };

        Listener aboutListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                log.trace("aboutListener.handleEvent({})", event);
                AboutView aboutView = new AboutView(view.getShell());
                AboutController aboutController = new AboutController(aboutView);
                aboutController.openView();
            }
        };

        Listener editSettingsListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                log.trace("editSettingsListener.handleEvent({})", event);
                if (EmailModel.isImporting()) {
                    String msg = "Settings may not be modified while import is running.";
                    log.debug(msg);
                    MessageBox msgBox = new MessageBox(view.getShell(), SWT.ICON_INFORMATION | SWT.OK);
                    msgBox.setMessage(msg);
                    msgBox.open();
                    return;
                }
                TntDb.stopImportService(); // if running
                new SettingsController(view.getShell()).openView();
            }
        };

        Listener testListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                log.trace("testListener.handleEvent({})", event);
                //
                // Test stuff goes here
                //
/*
                // Load SQL
                Path sqlFile = Path.of("C:\\Users\\hallm\\git\\mist\\devel\\test.sql");
                String sqlStr = "";
                try {
                    sqlStr = Files.readString(sqlFile);
                } catch (IOException e) {
                    log.error(e);
                }

                // Try inserting SQL
                try {
                    TntDb.startImportService(); // Connects, etc.
                    TntDb.runQuery(sqlStr);
                    TntDb.stopImportService(); // Disconnects?
                } catch (SQLException e) {
                    log.error(e);
                }
*/
            }
        };

        if (Util.isMac()) {
            // Mac - use Mac Application menu
            final CocoaSWTUIEnhancer enhancer = new CocoaSWTUIEnhancer(MIST.APP_NAME);
            enhancer.hookApplicationMenu(
                view.getShell().getDisplay(),
                exitListener,
                aboutListener,
                editSettingsListener);
            view.getTestItem().addListener(SWT.Selection, testListener);
        } else {
            // Windows or Linux
            view.getExitItem().addListener(SWT.Selection, exitListener);
            view.getEditSettingsItem().addListener(SWT.Selection, editSettingsListener);
            view.getAboutItem().addListener(SWT.Selection, aboutListener);
            view.getTestItem().addListener(SWT.Selection, testListener);
        }
    }
}
