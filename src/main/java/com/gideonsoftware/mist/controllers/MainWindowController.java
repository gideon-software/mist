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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.gideonsoftware.mist.views.MainWindowView;

public class MainWindowController {
    private static Logger log = LogManager.getLogger();
    private MainWindowView view = null;

    MessageDetailsController messageDetailsController;

    public MainWindowController(MainWindowView view) {
        log.trace("MainMenuController({})", view);
        this.view = view;
        view.create();
        // Catch close event
        view.getShell().addListener(SWT.Close, new Listener() {
            @Override
            public void handleEvent(Event event) {
                log.trace("closeListener.handleEvent({})", event);
                event.doit = closeView();
            }
        });
        new ImportButtonController(view.getImportButtonView());
        new ContactDetailsController(view.getContactDetailsView());
        new MessagesController(view.getMessagesView());
        messageDetailsController = new MessageDetailsController(view.getMessageDetailsView());
        new MainMenuController(view.getMainMenuView(), this);
    }

    public boolean closeView() {
        log.trace("closeView()");

        // Save any modified history text from the MessageDetailsView
        messageDetailsController.commitMessageToTntDb();
        messageDetailsController.commitSubjectToTntDb();

        /*
         * From view.close below:
         * "Note that in order to prevent recursive calls to this methodit does not call Shell#close(). As a result
         * ShellListeners will not receive a shellClosed event."
         * So we do it manually here.
         */
        view.doClose();

        return view.close();
    }

    public int openView() {
        log.trace("openView()");
        return view.open();
    }

}
