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

public class ImageManager {
    private static Logger log = LogManager.getLogger();

    static ImageRegistry imageRegistry = null;

    public static Image getImage(String key) {
        return imageRegistry.get(key);
    }

    public static Image getStatusImage(int status, String size) {
        Image image = null;
        switch (status) {
            case History.STATUS_ADDED:
                image = getImage("status-added-" + size);
                break;
            case History.STATUS_CONTACT_NOT_FOUND:
                image = getImage("status-contact-not-found-" + size);
                break;
            case History.STATUS_ERROR:
                image = getImage("status-error-" + size);
                break;
            case History.STATUS_EXISTS:
                image = getImage("status-exists-" + size);
                break;
            case History.STATUS_MULTIPLE_CONTACTS_FOUND:
                image = getImage("status-multiple-contacts-found-" + size);
                break;
        }
        return image;
    }

    public static void init() {
        log.trace("init()");

        imageRegistry = new ImageRegistry();
        String loc = null;

        // Load logo & social icons
        loc = "views/images";
        imageRegistry.put(
            "mist-logo",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/mist-logo.png")).createImage());
        imageRegistry.put(
            "facebook",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/facebook.png")).createImage());
        imageRegistry.put(
            "google-plus",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/google-plus.png")).createImage());

        // Load icons
        loc = "views/images/icons";
        imageRegistry.put(
            "about",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/information.png")).createImage());
        imageRegistry
            .put("exit", ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/logout.png")).createImage());
        imageRegistry
            .put("import", ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/download.png")).createImage());
        imageRegistry
            .put("manual", ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/globe.png")).createImage());
        imageRegistry.put(
            "settings",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/settings.png")).createImage());

        // Load application icons
        loc = "views/images/icons/mist";
        imageRegistry.put(
            "appicon-16x16",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/mist-16x16.png")).createImage());
        imageRegistry.put(
            "appicon-32x32",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/mist-32x32.png")).createImage());
        imageRegistry.put(
            "appicon-48x48",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/mist-48x48.png")).createImage());
        imageRegistry.put(
            "appicon-64x64",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/mist-64x64.png")).createImage());
        imageRegistry.put(
            "appicon-128x128",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/mist-128x128.png")).createImage());

        // Load status icons
        loc = "views/images/icons/status";
        imageRegistry.put(
            "status-added-16x16",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/plus-16x16.png")).createImage());
        imageRegistry.put(
            "status-added-64x64",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/plus-64x64.png")).createImage());
        imageRegistry.put(
            "status-contact-not-found-16x16",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/question-16x16.png")).createImage());
        imageRegistry.put(
            "status-contact-not-found-64x64",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/question-64x64.png")).createImage());
        imageRegistry.put(
            "status-error-16x16",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/x-16x16.png")).createImage());
        imageRegistry.put(
            "status-error-64x64",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/x-64x64.png")).createImage());
        imageRegistry.put(
            "status-exists-16x16",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/empty-16x16.png")).createImage());
        imageRegistry.put(
            "status-exists-64x64",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/empty-64x64.png")).createImage());
        imageRegistry.put(
            "status-multiple-contacts-found-16x16",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/bang-16x16.png")).createImage());
        imageRegistry.put(
            "status-multiple-contacts-found-64x64",
            ImageDescriptor.createFromURL(MIST.class.getResource(loc + "/bang-64x64.png")).createImage());
    }
}
