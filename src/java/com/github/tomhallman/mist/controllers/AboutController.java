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

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.util.ui.EmailLinkListener;
import com.github.tomhallman.mist.util.ui.WebpageLinkListener;
import com.github.tomhallman.mist.views.AboutView;

public class AboutController {
    private static Logger log = LogManager.getLogger();

    private AboutView view;

    public AboutController(AboutView view) {
        log.trace("AboutController({})", view);
        this.view = view;
        view.create();
        view.getFacebookButton().addSelectionListener(
            new WebpageLinkListener(view.getShell(), "MIST's Facebook page", MIST.FACEBOOK));
        view.getHomepageLink().addSelectionListener(
            new WebpageLinkListener(view.getShell(), "MIST's homepage", MIST.HOMEPAGE));
        view.getMailingListLink().addSelectionListener(new EmailLinkListener(view.getShell(), MIST.USERLIST));
    }

    public int openView() {
        return view.open();
    }
}
