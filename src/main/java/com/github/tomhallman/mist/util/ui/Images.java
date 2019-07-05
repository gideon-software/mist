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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.tntapi.entities.History;

public class Images {
    private static Logger log = LogManager.getLogger();

    public static final String LOGO_MIST = "logo-mist";
    public static final String LOGO_FACEBOOK = "logo-facebook";

    public static final String ICON_ABOUT = "icon-about";
    public static final String ICON_EMAIL = "icon-email";
    public static final String ICON_EMAIL_SERVER = "icon-email-server";
    public static final String ICON_EXIT = "icon-exit";
    public static final String ICON_IGNORE_CONTACT = "icon-ignore-contact";
    public static final String ICON_IMPORT_START = "icon-import-start";
    public static final String ICON_IMPORT_STOP = "icon-import-stop";
    public static final String ICON_LOG = "icon-log";
    public static final String ICON_MANUAL = "icon-manual";
    public static final String ICON_MATCH_CONTACT = "icon-match-contact";
    public static final String ICON_MESSAGE_TO_ME = "icon-msg-to-me";
    public static final String ICON_MESSAGE_FROM_ME = "icon-msg-from-me";
    public static final String ICON_RELOAD = "icon-reload";
    public static final String ICON_SETTINGS = "icon-settings";
    public static final String ICON_TNT = "icon-tnt";

    public static final String ICON_STATUS_ADDED = "icon-status-added";
    public static final String ICON_STATUS_ERROR = "icon-status-error";
    public static final String ICON_STATUS_EXISTS = "icon-status-exists";
    public static final String ICON_STATUS_CONTACT_NOT_FOUND = "icon-status-contact-not-found";
    public static final String ICON_STATUS_MULTIPLE_CONTACTS_FOUND = "icon-status-multiple-contacts-found";

    public static final String ICON_MIST_16 = "icon-mist-16x16";
    public static final String ICON_MIST_32 = "icon-mist-32x32";
    public static final String ICON_MIST_48 = "icon-mist-48x48";
    public static final String ICON_MIST_64 = "icon-mist-64x64";
    public static final String ICON_MIST_128 = "icon-mist-128x128";

    static ImageRegistry imageRegistry = null;

    public static Image getImage(String key) {
        return imageRegistry.get(key);
    }

    public static Image getStatusImage(int status) {
        Image image = null;
        switch (status) {
            case History.STATUS_ADDED:
                image = getImage(ICON_STATUS_ADDED);
                break;
            case History.STATUS_CONTACT_NOT_FOUND:
                image = getImage(ICON_STATUS_CONTACT_NOT_FOUND);
                break;
            case History.STATUS_ERROR:
                image = getImage(ICON_STATUS_ERROR);
                break;
            case History.STATUS_EXISTS:
                image = getImage(ICON_STATUS_EXISTS);
                break;
            case History.STATUS_MULTIPLE_CONTACTS_FOUND:
                image = getImage(ICON_STATUS_MULTIPLE_CONTACTS_FOUND);
                break;
        }
        return image;
    }

    public static void init() {
        log.trace("init()");

        imageRegistry = new ImageRegistry();
        String imgLoc = "views/images/";

        // Load logo / social icons
        put(LOGO_MIST, imgLoc + "mist-logo.png");
        put(LOGO_FACEBOOK, imgLoc + "facebook-50x50.png");

        // Load icons
        put(ICON_ABOUT, imgLoc + "icons/mist/mist-16x16.png");
        put(ICON_EMAIL, imgLoc + "icons/envelope-regular.png");
        put(ICON_EMAIL_SERVER, imgLoc + "icons/inbox-solid.png");
        put(ICON_EXIT, imgLoc + "icons/sign-out-alt-solid.png");
        put(ICON_IGNORE_CONTACT, imgLoc + "icons/user-slash-solid.png");
        put(ICON_IMPORT_START, imgLoc + "icons/play-solid.png");
        put(ICON_IMPORT_STOP, imgLoc + "icons/stop-solid.png");
        put(ICON_LOG, imgLoc + "icons/pencil-alt-solid.png");
        put(ICON_MANUAL, imgLoc + "icons/book-solid.png");
        put(ICON_MATCH_CONTACT, imgLoc + "icons/user-plus-solid.png");
        put(ICON_MESSAGE_TO_ME, imgLoc + "icons/angle-left-solid.png");
        put(ICON_MESSAGE_FROM_ME, imgLoc + "icons/angle-right-solid.png");
        put(ICON_RELOAD, imgLoc + "icons/redo-alt-solid-mod.png");
        put(ICON_SETTINGS, imgLoc + "icons/cog-solid.png");
        put(ICON_TNT, imgLoc + "icons/tnt.png");

        // Load status icons
        put(ICON_STATUS_ADDED, imgLoc + "icons/star-solid.png");
        put(ICON_STATUS_CONTACT_NOT_FOUND, imgLoc + "icons/question-circle-solid.png");
        put(ICON_STATUS_ERROR, imgLoc + "icons/times-circle-solid.png");
        put(ICON_STATUS_EXISTS, imgLoc + "icons/star-regular.png");
        put(ICON_STATUS_MULTIPLE_CONTACTS_FOUND, imgLoc + "icons/exclamation-triangle-solid.png");

        // Load application icons
        put(ICON_MIST_16, imgLoc + "icons/mist/mist-16x16.png");
        put(ICON_MIST_32, imgLoc + "icons/mist/mist-32x32.png");
        put(ICON_MIST_48, imgLoc + "icons/mist/mist-48x48.png");
        put(ICON_MIST_64, imgLoc + "icons/mist/mist-64x64.png");
        put(ICON_MIST_128, imgLoc + "icons/mist/mist-128x128.png");
    }

    protected static void put(String name, String path) {
        imageRegistry.put(name, ImageDescriptor.createFromURL(MIST.class.getResource(path)).createImage());
    }
}
