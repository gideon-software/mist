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

package com.gideonsoftware.mist.util.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Shell;

/**
 *
 */
public class MistProgressMonitorDialog extends ProgressMonitorDialog {
    private static Logger log = LogManager.getLogger();

    private String title;

    public MistProgressMonitorDialog(Shell parent) {
        super(parent);
        log.trace("MistProgressMonitorDialog({})", parent);
        this.title = null;
    }

    public MistProgressMonitorDialog(Shell parent, String title) {
        super(parent);
        log.trace("MistProgressMonitorDialog({},{})", parent, title);
        this.title = title;
    }

    protected void configureShell(final Shell shell) {
        super.configureShell(shell);
        if (title != null)
            shell.setText(title);
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title
     *            the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }
}
