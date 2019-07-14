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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;

import com.gideonsoftware.mist.util.Util;

/**
 * Control for displaying a simple email link.
 */
public class SimpleEmailLink extends Link {
    private static Logger log = LogManager.getLogger();

    private String email = "";

    public SimpleEmailLink(Composite parent, int style) {
        super(parent, style);
        log.trace("SimpleEmailLink()", parent, style);

        addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                log.trace("SimpleEmailLink.widgetSelected({})", event);
                if (!Program.launch("mailto:" + email)) {
                    Util.reportError(
                        "Unable to load default email client",
                        "There was a problem loading your default email client. You can manually write to: " + email);
                }
            }
        });
    }

    @Override
    protected void checkSubclass() {
        // Override the disallowing of subclassing
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        setText(String.format("<a href=\"mailto:%1$s\">%1$s</a>", email));
    }

}
