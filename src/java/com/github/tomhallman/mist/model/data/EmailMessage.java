/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2018 Tom Hallman
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.tomhallman.mist.exceptions.EmailMessageException;

import net.htmlparser.jericho.Source;

public class EmailMessage extends MessageSource {
    private static Logger log = LogManager.getLogger();

    public EmailMessage(EmailMessage emailMessage) {
        super(emailMessage);
        // No additional fields from MessageSource
        // But we need this for property inheritance & copy constructor functionality
    }

    public EmailMessage(EmailServer emailServer, javax.mail.Message msg) {
        log.trace("EmailMessage({},{})", emailServer, msg);

        // Server info
        setSourceId(emailServer.getId());
        setSourceName(emailServer.getNickname());

        // Subject
        try {
            String _subject = msg.getSubject();
            setSubject(_subject == null ? "(no subject)" : _subject);
        } catch (MessagingException e) {
            setSubject("<error>");
            log.error("Error retrieving 'subject' from message ({})", msg);
        }

        // From
        try {
            InternetAddress[] _from = (InternetAddress[]) msg.getFrom();
            setFromName(_from == null || _from.length == 0 ? "Unknown" : _from[0].getPersonal());
            setFromId(_from == null || _from.length == 0 ? "Unknown" : _from[0].getAddress());
        } catch (MessagingException e) {
            setFromName("<error>");
            setFromId("<error>");
            log.error("Error retrieving 'from' from message ({})", msg);
        }

        // Body
        // See http://wrongnotes.blogspot.com/2007/09/javamail-parsing-made-easy.html
        try {
            Map<String, Part> mimeTypes = findMimeTypes(msg, "text/plain", "text/html");
            if (mimeTypes.containsKey("text/plain")) {
                Object content = mimeTypes.get("text/plain").getContent();
                setBody(content.toString());
            } else if (mimeTypes.containsKey("text/html")) {
                Object content = mimeTypes.get("text/html").getContent();
                Source source = new Source(content.toString());
                setBody(source.getRenderer().toString());
            }
        } catch (MessagingException | IOException e) {
            setBody("<error>");
            log.error("Error retrieving 'body' from message ({})", msg);
        }

        // Date
        try {
            setDate(LocalDateTime.ofInstant(msg.getSentDate().toInstant(), ZoneId.systemDefault()));
        } catch (MessagingException e) {
            setDate(LocalDateTime.now());
            log.error("Error retrieving 'date' from message ({})", msg);
        }

        // Recipients
        try {
            setRecipients(msg.getAllRecipients());
        } catch (MessagingException e) {
            setRecipients(new Address[0]);
            log.error("Error retrieving 'recipients' from message ({})", msg);
        }
    }

    private static void findContentTypesHelper(
        Part p,
        Map<String, Part> contentTypes,
        String... mimeTypes) throws MessagingException, UnsupportedEncodingException, IOException {
        // See http://wrongnotes.blogspot.com/2007/09/javamail-parsing-made-easy.html
        if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; mp != null && i < mp.getCount(); i++)
                findContentTypesHelper(mp.getBodyPart(i), contentTypes, mimeTypes);
        } else {
            for (String mimeType : mimeTypes)
                if (p.isMimeType(mimeType))
                    contentTypes.put(mimeType, p);
        }
    }

    private static Map<String, Part> findMimeTypes(
        Part p,
        String... mimeTypes) throws MessagingException, UnsupportedEncodingException, IOException {
        // See http://wrongnotes.blogspot.com/2007/09/javamail-parsing-made-easy.html
        Map<String, Part> parts = new HashMap<String, Part>();
        findMimeTypesHelper(p, parts, mimeTypes);
        return parts;
    }

    private static void findMimeTypesHelper(
        Part p,
        Map<String, Part> parts,
        String... mimeTypes) throws MessagingException, IOException {
        // See http://wrongnotes.blogspot.com/2007/09/javamail-parsing-made-easy.html
        // A little recursive helper function that actually does all the work.
        if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++)
                findContentTypesHelper(mp.getBodyPart(i), parts, mimeTypes);
        } else {
            for (String mimeType : mimeTypes)
                if (p.isMimeType(mimeType) && !parts.containsKey(mimeType))
                    parts.put(mimeType, p);
        }
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

    public static String guessFromName(String name) {
        if (name == null || name.isBlank())
            return "Contact";

        // Add static-sized array to ArrayList for easier manipulation
        String[] nameArr = name.trim().split(" ");
        ArrayList<String> names = new ArrayList<String>();
        for (int i = 0; i < nameArr.length; i++)
            names.add(nameArr[i]);

        // Remove unicode characters
        while (names.size() > 0 && names.get(0).contains("=?UTF"))
            names.remove(0);

        if (names.size() == 0)
            return "Contact";
        else if (names.size() == 1)
            return name.trim();
        else {
            // Check for first initial, like "M. Night Shyamalan"
            if (names.get(0).length() <= 2) // Likely a first initial
                return String.format("%s %s", names.get(0), names.get(1));

            // Check for "Last, First"
            if (',' == names.get(0).charAt(names.get(0).length() - 1))
                return names.get(1);

            // Assume just a regular first name
            return names.get(0);
        }
    }

    /**
     * Returns a clone of this message source.
     * 
     * @return a clone of this message source
     * @see https://dzone.com/articles/java-cloning-even-copy-constructors-are-not-suffic
     */
    @Override
    public EmailMessage cloneObject() {
        return new EmailMessage(this);
    }

    public String getFromFormatted() throws EmailMessageException {
        return getAddrFormatted(getFromName(), getFromId());
    }

    public String guessFromName() {
        return guessFromName(getFromName().trim());
    }

}
