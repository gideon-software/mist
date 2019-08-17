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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.api.client.util.Base64;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;

import net.htmlparser.jericho.Source;

/**
 * 
 */
public class GmailMessage extends EmailMessage {
    private static Logger log = LogManager.getLogger();

    private Message message;

    public GmailMessage(GmailMessage gmailMessage) {
        super(gmailMessage);
        this.message = gmailMessage.getMessage();
        // We need this for property inheritance & copy constructor functionality
    }

    public GmailMessage(GmailServer server, Message message) {
        super(server);
        log.trace("GmailMessage({},{})", server, message);

        this.message = message;

        // Date
        setDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(message.getInternalDate()), ZoneId.systemDefault()));

        // Body
        try {
            Map<String, MessagePart> mimeTypes = findMimeTypes(message.getPayload(), "text/plain", "text/html");

            if (mimeTypes.containsKey("text/plain")) {
                String content = mimeTypes.get("text/plain").getBody().getData();
                byte[] bodyBytes = Base64.decodeBase64(content);
                String text = new String(bodyBytes, "UTF-8");
                setBody(text);
            } else if (mimeTypes.containsKey("text/html")) {
                // Get the text
                String content = mimeTypes.get("text/html").getBody().getData();
                byte[] bodyBytes = Base64.decodeBase64(content);
                String text = new String(bodyBytes, "UTF-8");
                // Try to parse it
                setBody(new Source(text).getRenderer().toString());
            }
        } catch (MessagingException | IOException e) {
            setBody("<error>");
            log.error("Error retrieving 'body' from message ({})", message);
        }

        MessagePart messagePart = message.getPayload();
        if (messagePart != null) {
            List<MessagePartHeader> headers = messagePart.getHeaders();
            for (MessagePartHeader header : headers) {
                switch (header.getName()) {

                    // Subject
                    case "Subject":
                        setSubject(header.getValue()); // TODO: Test blank subject line
                        break;

                    // From
                    case "From":
                        try {
                            InternetAddress _from = new InternetAddress(header.getValue());
                            setFromName(_from == null ? "Unknown" : _from.getPersonal());
                            setFromId(_from == null ? "Unknown" : _from.getAddress());
                        } catch (AddressException e) {
                            setFromName("Unknown");
                            setFromId("Unknown");
                            log.error("Error retrieving 'from' from message ({})", message);
                        }
                        break;

                    // Recipients
                    case "To":
                        try {
                            InternetAddress[] _to = InternetAddress.parse(header.getValue());
                            setRecipients(_to);
                        } catch (AddressException e) {
                            setRecipients(new String[] { "Unknown" });
                            log.error("Error retrieving 'to' from message ({})", message);
                        }
                        break;

                    default:
                        break;
                }
            }
        }
    }

    // See http://wrongnotes.blogspot.com/2007/09/javamail-parsing-made-easy.html
    private static void findContentTypesHelper(
        MessagePart p,
        Map<String, MessagePart> contentTypes,
        String... mimeTypes) throws MessagingException, IOException {
        if (p.getParts() != null) {
            for (MessagePart part : p.getParts())
                findContentTypesHelper(part, contentTypes, mimeTypes);
        } else {
            for (String mimeType : mimeTypes)
                if (p.getMimeType().equals(mimeType))
                    contentTypes.put(mimeType, p);
        }
    }

    // See http://wrongnotes.blogspot.com/2007/09/javamail-parsing-made-easy.html
    private static Map<String, MessagePart> findMimeTypes(
        MessagePart p,
        String... mimeTypes) throws MessagingException, UnsupportedEncodingException, IOException {
        Map<String, MessagePart> parts = new HashMap<String, MessagePart>();
        findMimeTypesHelper(p, parts, mimeTypes);
        return parts;
    }

    // A little recursive helper function that actually does all the work.
    // See http://wrongnotes.blogspot.com/2007/09/javamail-parsing-made-easy.html
    private static void findMimeTypesHelper(
        MessagePart p,
        Map<String, MessagePart> parts,
        String... mimeTypes) throws MessagingException, IOException {
        if (p.getParts() != null) {
            for (MessagePart part : p.getParts())
                findContentTypesHelper(part, parts, mimeTypes);
        } else {
            for (String mimeType : mimeTypes)
                if (p.getMimeType().equals(mimeType) && !parts.containsKey(mimeType))
                    parts.put(mimeType, p);
        }
    }

    /**
     * Returns a clone of this gmail message
     * 
     * @return a clone of this gmail message
     * @see https://dzone.com/articles/java-cloning-even-copy-constructors-are-not-suffic
     */
    @Override
    public GmailMessage cloneObject() {
        return new GmailMessage(this);
    }

    public Message getMessage() {
        return message;
    }

}
