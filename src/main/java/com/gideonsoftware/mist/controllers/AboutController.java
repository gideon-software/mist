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

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.util.ui.EmailLinkListener;
import com.gideonsoftware.mist.util.ui.WebpageLinkListener;
import com.gideonsoftware.mist.views.AboutView;

public class AboutController {
    private static Logger log = LogManager.getLogger();

    private AboutView view;

    public AboutController(AboutView view) {
        log.trace("AboutController({})", view);
        this.view = view;
        view.create();
        view.getFacebookLink().addSelectionListener(new WebpageLinkListener("MIST's Facebook page", MIST.FACEBOOK));
        view.getHomepageLink().addSelectionListener(new WebpageLinkListener("MIST's homepage", MIST.HOMEPAGE));
        view.getMailingListLink().addSelectionListener(new EmailLinkListener(MIST.EMAIL_SUPPORT));
    }

    public int openView() {
        return view.open();
    }
}
