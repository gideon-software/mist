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

package com.gideonsoftware.mist.preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.widgets.Shell;

import com.gideonsoftware.mist.util.ui.Images;

/**
 *
 */
public class MistPreferenceDialog extends PreferenceDialog {
    private static Logger log = LogManager.getLogger();

    /**
     * @param parentShell
     * @param manager
     */
    public MistPreferenceDialog(Shell parentShell, PreferenceManager manager) {
        super(parentShell, manager);
        log.trace("MistPreferenceDialog({},{})", parentShell, manager);
    }

    /**
     * Clears the current page; useful for disabling error handling before a page switch
     */
    public void clearCurrentPage() {
        super.setCurrentPage(null);
    }

    @Override
    public void create() {
        super.create();
        getShell().setImage(Images.getImage(Images.ICON_SETTINGS));
        if (!Util.isMac())
            getShell().setText("Settings"); // As opposed to "Preferences"
    }

    /**
     * For some reason, the {@link PreferenceDialog} implementation of {@link #cancelPressed()} calls
     * {@link #handleSave()}. We definitely don't want that, so we're overriding it here. We'll handle
     * the saving ourselves after this method returns.
     */
    @Override
    protected void handleSave() {
        log.trace("handleSave() <does nothing>");
    }

}
