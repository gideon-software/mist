/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2010 Tom Hallman
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
 * For more information, visit https://github.com/tomhallman/mist
 */

package com.github.tomhallman.mist.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.util.ui.CocoaSWTUIEnhancer;
import com.github.tomhallman.mist.util.ui.WebpageLinkListener;
import com.github.tomhallman.mist.views.AboutView;
import com.github.tomhallman.mist.views.MainMenuView;

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
                // TODO: Allow loading but not editing?
                // if (MIST.getMasterModel().isImporting()) {
                // log.info("Settings may not be modified during import.");
                // MessageBox msgBox = new MessageBox(view.getShell(), SWT.ICON_INFORMATION | SWT.OK);
                // msgBox.setMessage("Settings may not be modified during import.");
                // msgBox.open();
                // return;
                // }
                new SettingsController(view.getShell()).openView();
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

        } else {
            // Windows or Linux
            view.getExitItem().addListener(SWT.Selection, exitListener);
            view.getEditSettingsItem().addListener(SWT.Selection, editSettingsListener);
            view.getAboutItem().addListener(SWT.Selection, aboutListener);
        }
        // All platforms
        view.getManualItem().addSelectionListener(
            new WebpageLinkListener(view.getShell(), "the MIST User Manual", MIST.MANUAL));
    }
}
