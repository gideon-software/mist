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
package com.github.tomhallman.mist.util.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.program.Program;

import com.github.tomhallman.mist.util.Util;

public class EmailLinkListener extends SelectionAdapter {
    private static Logger log = LogManager.getLogger();
    private String email = "";

    public EmailLinkListener() {
        log.trace("EmailLinkListener()");
        this.email = "";
    }

    public EmailLinkListener(String email) {
        log.trace("EmailLinkListener({})", email);
        this.email = email == null ? "" : email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        log.trace("EmailLinkListener.widgetSelected({})", event);
        if (!Program.launch("mailto:" + email)) {
            String msg = "There was a problem loading your default email client. You can manually write to: " + email;
            Util.reportError("Unable to load default email client", msg);
        }
    }
}
