/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2019 Gideon Software
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.htmlparser.jericho.Source;

/**
 * 
 */
public class ImapMessage extends EmailMessage {
    private static Logger log = LogManager.getLogger();

    private Message message;

    public ImapMessage(ImapMessage imapMessage) {
        super(imapMessage);
        this.message = imapMessage.getMessage();
        // We need this for property inheritance & copy constructor functionality
    }

    public ImapMessage(ImapServer server, Message message) {
        super(server);
        log.trace("ImapMessage({},{})", server, message);

        this.message = message;

        // Subject
        try {
            String _subject = message.getSubject();
            setSubject(_subject == null ? "(no subject)" : _subject);
        } catch (MessagingException e) {
            setSubject("<error>");
            log.error("Error retrieving 'subject' from message ({})", message);
        }

        // From
        try {
            InternetAddress[] _from = (InternetAddress[]) message.getFrom();
            setFromName(_from == null || _from.length == 0 ? "Unknown" : _from[0].getPersonal());
            setFromId(_from == null || _from.length == 0 ? "Unknown" : _from[0].getAddress());
        } catch (MessagingException e) {
            setFromName("<error>");
            setFromId("<error>");
            log.error("Error retrieving 'from' from message ({})", message);
        }

        // Body
        // See http://wrongnotes.blogspot.com/2007/09/javamail-parsing-made-easy.html
        try {
            Map<String, Part> mimeTypes = findMimeTypes(message, "text/plain", "text/html");
            if (mimeTypes.containsKey("text/plain")) {
                Object content = mimeTypes.get("text/plain").getContent();
                setBody(content.toString());
            } else if (mimeTypes.containsKey("text/html")) {
                // Try to parse it
                Object content = mimeTypes.get("text/html").getContent();
                Source source = new Source(content.toString());
                setBody(source.getRenderer().toString());
            }
        } catch (MessagingException | IOException e) {
            setBody("<error>");
            log.error("Error retrieving 'body' from message ({})", message);
        }

        // Date
        try {
            setDate(LocalDateTime.ofInstant(message.getSentDate().toInstant(), ZoneId.systemDefault()));
        } catch (MessagingException e) {
            setDate(LocalDateTime.now());
            log.error("Error retrieving 'date' from message ({})", message);
        }

        // Recipients
        try {
            addRecipients(message.getAllRecipients());
        } catch (MessagingException e) {
            addRecipients(new Address[0]);
            log.error("Error retrieving 'to' from message ({})", message);
        }
    }

    // See http://wrongnotes.blogspot.com/2007/09/javamail-parsing-made-easy.html
    private static void findContentTypesHelper(
        Part p,
        Map<String, Part> contentTypes,
        String... mimeTypes) throws MessagingException, UnsupportedEncodingException, IOException {
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

    // See http://wrongnotes.blogspot.com/2007/09/javamail-parsing-made-easy.html
    private static Map<String, Part> findMimeTypes(
        Part p,
        String... mimeTypes) throws MessagingException, UnsupportedEncodingException, IOException {
        Map<String, Part> parts = new HashMap<String, Part>();
        findMimeTypesHelper(p, parts, mimeTypes);
        return parts;
    }

    // See http://wrongnotes.blogspot.com/2007/09/javamail-parsing-made-easy.html
    // A little recursive helper function that actually does all the work.
    private static void findMimeTypesHelper(
        Part p,
        Map<String, Part> parts,
        String... mimeTypes) throws MessagingException, IOException {
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

    /**
     * Returns a clone of this IMAP message
     * 
     * @return a clone of this IMAP message
     * @see https://dzone.com/articles/java-cloning-even-copy-constructors-are-not-suffic
     */
    @Override
    public ImapMessage cloneObject() {
        return new ImapMessage(this);
    }

    public Message getMessage() {
        return message;
    }

}
