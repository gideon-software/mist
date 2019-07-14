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

import com.gideonsoftware.mist.util.Util;

public class WebpageLinkListener extends SelectionAdapter {
    private static Logger log = LogManager.getLogger();
    private String link = "";
    private String text = "";

    public WebpageLinkListener(String text, String link) {
        log.trace("WebpageLinkListener({},{})", text, link);
        this.link = link;
        this.text = text;
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        log.trace("widgetSelected({})", event);
        if (!Program.launch(link)) {
            String msg = String.format(
                "There was a problem loading %s in your default web browser. You can find it at %s",
                text,
                link);
            Util.reportError("Unable to load URL in default browser", msg);
        }
    }
}
