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

package com.github.tomhallman.mist.model.data;

import javax.mail.Folder;
import javax.mail.MessagingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EmailFolder {

    private static Logger log = LogManager.getLogger();

    // The underlying JavaX mail folder
    private Folder folder = null;

    public EmailFolder(Folder folder) {
        log.trace("EmailFolder({})", folder);
        this.folder = folder;
    }

    public String getFolderName() {
        return folder.getName();
    }

    public String getFullFolderName() {
        return folder.getFullName();
    }

    public boolean canHoldMessages() {
        try {
            return (folder.getType() & Folder.HOLDS_MESSAGES) != 0;
        } catch (MessagingException e) {
            log.error("Unable to determine if folder can hold messages.", e);
            return false;
        }
    }

    public boolean hasSubfolders() {
        try {
            return folder.list().length != 0;
        } catch (MessagingException e) {
            log.error("Unable to determine if folder has subfolders.", e);
            return false;
        }
    }
}
