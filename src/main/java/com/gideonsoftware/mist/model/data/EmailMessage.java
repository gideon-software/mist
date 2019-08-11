/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2018 Gideon Software
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

package com.gideonsoftware.mist.model.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gideonsoftware.mist.exceptions.EmailMessageException;

public class EmailMessage extends MessageSource {
    private static Logger log = LogManager.getLogger();

    public EmailMessage(EmailMessage emailMessage) {
        super(emailMessage);
        // We need this for property inheritance & copy constructor functionality
    }

    public EmailMessage(EmailServer server) {
        super();
        log.trace("EmailMessage({})", server);

        setSourceId(server.getId());
        setSourceName(server.getNickname());
    }

    public static String getAddrFormatted(String name, String email) {
        if (name == null) {
            if (email == null) {
                return "<unknown>";
            } else {
                return email;
            }
        } else if (email == null) {
            return name;
        } else {
            return String.format("\"%s\" <%s>", name, email);
        }
    }

    /**
     * Returns a clone of this email message
     * 
     * @return a clone of this email message
     * @see https://dzone.com/articles/java-cloning-even-copy-constructors-are-not-suffic
     */
    @Override
    public EmailMessage cloneObject() {
        return new EmailMessage(this);
    }

    public String getFromFormatted() throws EmailMessageException {
        return getAddrFormatted(getFromName(), getFromId());
    }

}
