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
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.tntapi.TntDb;

public class SettingsController {
    private static Logger log = LogManager.getLogger();

    private Shell shell; // top-level

    public SettingsController(Shell shell) {
        log.trace("SettingsController({})", shell);
        this.shell = shell;
    }

    public int openView() {
        log.trace("openView()");
        PreferenceDialog prefDlg = MIST.getPreferenceManager().createPreferenceDialog(shell);
        int ret = prefDlg.open();
        if (ret == Window.OK)
            MIST.getPrefs().savePreferences();
        else {
            MIST.getPrefs().resetPreferences();
            TntDb.init(); // Forces a reload of the DB settings
        }

        return ret;
    }
}
