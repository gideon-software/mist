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

package com.gideonsoftware.mist.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.exceptions.HistoryException;
import com.gideonsoftware.mist.exceptions.TntDbException;
import com.gideonsoftware.mist.model.data.EmailMessage;
import com.gideonsoftware.mist.tntapi.ContactManager;
import com.gideonsoftware.mist.tntapi.entities.ContactInfo;
import com.gideonsoftware.mist.tntapi.entities.History;
import com.gideonsoftware.mist.tntapi.entities.TaskType;
import com.gideonsoftware.mist.util.Util;

public class HistoryModel {
    private static Logger log = LogManager.getLogger();

    private static boolean useAutoThank;
    private static String[] autoThankSubjectArr;

    // Property change values
    private final static PropertyChangeSupport pcs = new PropertyChangeSupport(HistoryModel.class);
    public final static String PROP_HISTORY_ADD = "historymodel.history.add";
    public final static String PROP_HISTORY_INIT = "historymodel.history.init";
    public final static String PROP_CONTACT_REMOVE = "historymodel.contact.remove";
    public final static String PROP_MESSAGE_IGNORED = "historymodel.message.ignored";

    // A list of all history added to Tnt (including errors)
    private static volatile ArrayList<History> historyArr = new ArrayList<History>();

    // Number of examined emails since init
    private static int examinedEmailsCount;

    /**
     * No instantiation allowed!
     */
    private HistoryModel() {
    }

    /**
     * Try to add in contact info from Tnt. Report errors and change status if there are no associated contacts or
     * multiple contacts.
     * 
     * @param history
     *            the history to add contact info to
     * @throws HistoryException
     *             TODO
     */
    public static void addContactInfoFromTnt(History history) throws HistoryException {
        log.trace("{{}} addContactInfoFromTnt()", history);

        try {
            int numMatches = ContactManager.getContactsByEmailCount(history.getContactInfo().getInfo());
            if (numMatches == 0) {
                log.debug(
                    "Contact not found in Tnt for '{}'. Skipping message for this contact.",
                    history.getContactInfo().getInfo());
                if (history.getStatus() == History.STATUS_NONE)
                    history.setStatus(History.STATUS_CONTACT_NOT_FOUND);
            } else if (numMatches > 1) {
                log.warn(
                    "Multiple contacts found in Tnt for '{}'. Skipping message for these contacts.",
                    history.getContactInfo().getInfo());
                if (history.getStatus() == History.STATUS_NONE)
                    history.setStatus(History.STATUS_MULTIPLE_CONTACTS_FOUND);
            } else {
                history.getContactInfo().setId(ContactManager.getContactIdByEmail(history.getContactInfo().getInfo()));
                history.getContactInfo().setName(ContactManager.getFileAs(history.getContactInfo().getId()));
            }
        } catch (TntDbException | SQLException e) {
            throw new HistoryException("There was a problem finding a contact for this message.", e);
        }
    }

    public static void addHistory(History history) {
        log.trace("addHistory({})", history);
        // Add to history list
        historyArr.add(history);
        pcs.firePropertyChange(PROP_HISTORY_ADD, null, history);
    }

    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        log.trace("addPropertyChangeListener({})", listener);
        pcs.addPropertyChangeListener(listener);
    }

    public static History[] getAllHistoryWithContactInfo(ContactInfo info) {
        log.trace("getAllHistoryWithContactInfo({})", info);
        ArrayList<History> retArr = new ArrayList<History>();
        for (Iterator<History> it = historyArr.iterator(); it.hasNext();) {
            History history = it.next();
            if (history.getContactInfo().equals(info))
                retArr.add(history);
        }
        return retArr.toArray(new History[0]);
    }

    public static int getExaminedEmailsCount() {
        return examinedEmailsCount;
    }

    /**
     * 
     * @param msg
     * @return
     */
    public static History[] getHistory(EmailMessage msg) {
        log.trace("getHistory({})", msg);

        // Increment counters
        examinedEmailsCount++;
        Util.incPrefCounter(MIST.PREF_TOTAL_EXAMINED_EMAILS, 1);

        // Is the email from someone on the ignore lists?
        if (EmailModel.isEmailInIgnoreList(msg.getFromId())) {
            log.debug("Sender is in the global ignore list ({}); skipping.", msg.getFromId());
            pcs.firePropertyChange(PROP_MESSAGE_IGNORED, null, msg);
            return null;
        } else if (EmailModel.getEmailServer(msg.getSourceId()).isEmailInIgnoreList(msg.getFromId())) {
            log.debug(
                "Sender is in the server ignore list ({} on '{}'); skipping.",
                msg.getFromId(),
                msg.getSourceName());
            pcs.firePropertyChange(PROP_MESSAGE_IGNORED, null, msg);
            return null;
        }

        History history = new History();

        // Set basic data
        history.setTaskTypeId(TaskType.EMAIL);
        history.setMessageSource(msg);
        history.setDescription(msg.getSubject());
        history.setNotes(msg.getBody());
        history.setHistoryDate(msg.getDate());

        // Tnt User ID is based on email server settings
        history.setLoggedByUserId(EmailModel.getEmailServer(msg.getSourceId()).getTntUserId());

        History[] historyArr = null;
        String[] myAddrList = EmailModel.getEmailServer(msg.getSourceId()).getMyAddresses();
        if (EmailModel.isEmailInList(msg.getFromId(), myAddrList)) {
            // If the message is TO one or more contacts, we may need multiple history entries
            historyArr = getHistoryToContact(msg, history);
        } else {
            // If the message is FROM a contact, we only record one history entry
            history = getHistoryFromContact(msg, history);
            historyArr = new History[1];
            historyArr[0] = history;
        }
        return historyArr;
    }

    /**
     * Create a History object from a message that is [possibly] from a Contact
     * 
     * @param msg
     * @param history
     * @return
     */
    private static History getHistoryFromContact(EmailMessage msg, History history) {
        log.trace("getHistoryFromContact({},{})", msg, history);
        log.debug("Processing message to me from {}", msg.getFromId());

        history.setHistoryResultId(History.RESULT_RECEIVED);
        history.getContactInfo().setName(msg.getFromName());
        history.getContactInfo().setInfo(msg.getFromId());

        try {
            addContactInfoFromTnt(history);
        } catch (HistoryException e) {
            // Something's busted... Not good. Continue in error state.
            history.setStatus(History.STATUS_ERROR);
            history.setStatusException(e);
            log.error(e);
        }

        return history;
    }

    /**
     * Create a History object from a message that is [possibly] to one or more contacts
     * 
     * @param his
     */
    private static History[] getHistoryToContact(EmailMessage msg, History his) {
        log.trace("getHistoryToContact({},{})", msg, his);

        if (msg.getRecipients() == null)
            return null;

        List<History> historyList = new ArrayList<History>();

        // We need to add history for each recipient
        for (int r = 0; r < msg.getRecipients().length; r++) {

            History history = new History(his);
            history.setHistoryResultId(History.RESULT_DONE);
            history.setStatus(History.STATUS_NONE);
            history.setStatusException(null);

            InternetAddress addr = null;
            String recipientEmail = null;
            try {
                addr = new InternetAddress(msg.getRecipients()[r].toString());
                recipientEmail = addr.getAddress().toLowerCase();
                history.getContactInfo().setInfo(recipientEmail);
                history.getContactInfo().setName(addr.getPersonal());
            } catch (AddressException e1) {
                log.warn("Unable to parse email address: {}", msg.getRecipients()[r]);
            }

            log.trace("Recipient {}/{}: {}", r + 1, msg.getRecipients().length, recipientEmail);

            String[] myAddrList = EmailModel.getEmailServer(msg.getSourceId()).getMyAddresses();
            if (EmailModel.isEmailInList(recipientEmail, myAddrList)) {
                // This address is also me; skip it
                log.debug("Message is from me to me ({}); skipping", recipientEmail);
                pcs.firePropertyChange(PROP_MESSAGE_IGNORED, null, msg);
                continue;
            } else if (EmailModel.isEmailInIgnoreList(recipientEmail)) {
                // This address is globally-ignored; skip it
                log.debug("Message is from me to a globally-ignored address ({}); skipping", recipientEmail);
                pcs.firePropertyChange(PROP_MESSAGE_IGNORED, null, msg);
                continue;
            } else if (EmailModel.getEmailServer(msg.getSourceId()).isEmailInIgnoreList(recipientEmail)) {
                // This address is server-ignored; skip it
                log.debug(
                    "Message is from me to a server-ignored address ({} on '{}'); skipping",
                    recipientEmail,
                    msg.getSourceName());
                pcs.firePropertyChange(PROP_MESSAGE_IGNORED, null, msg);
                continue;
            }

            // This message is from me to one or more people
            log.debug("Processing message from me to {}", recipientEmail);

            try {
                addContactInfoFromTnt(history);
            } catch (HistoryException e) {
                history.setStatus(History.STATUS_ERROR);
                history.setStatusException(e);
                log.error(e);
            }

            // If autoThank is enabled, check for thank here
            if (useAutoThank && EmailModel.doesSubjectStartWithPhraseInList(msg.getSubject(), autoThankSubjectArr))
                history.setThank(true);

            historyList.add(history);

        } // for r in recipients

        return historyList.toArray(new History[0]);
    }

    public static History[] getUnknownHistory() {
        log.trace("getUnknownHistory()");
        ArrayList<History> retArr = new ArrayList<History>();
        for (Iterator<History> it = historyArr.iterator(); it.hasNext();) {
            History history = it.next();
            if (history.getContactInfo().getId() == null)
                retArr.add(history);
        }
        return retArr.toArray(new History[0]);
    }

    public static void init() {
        log.trace("init()");
        historyArr.clear();
        examinedEmailsCount = 0;
        pcs.firePropertyChange(PROP_HISTORY_INIT, false, true); // Newly-initialized history array!

        useAutoThank = MIST.getPrefs().getBoolean(EmailModel.PREF_AUTOTHANK_ENABLED);
        autoThankSubjectArr = MIST.getPrefs().getStrings(EmailModel.PREF_AUTOTHANK_SUBJECTS);
    }

    /**
     * 
     * @param info
     */
    public static void removeAllHistoryWithContactInfo(ContactInfo info) {
        log.trace("removeAllHistoryWithContactInfo()", info);
        for (Iterator<History> it = historyArr.iterator(); it.hasNext();) {
            History history = it.next();
            if (history.getContactInfo().equals(info))
                it.remove();
        }
        pcs.firePropertyChange(PROP_CONTACT_REMOVE, null, info);
    }

    /**
     * TODO
     * 
     * @param ci
     * @param serverId
     */
    public static void removeAllHistoryWithContactInfo(ContactInfo info, int serverId) {
        log.trace("removeAllHistoryWithContactInfo()", info, serverId);
        for (Iterator<History> it = historyArr.iterator(); it.hasNext();) {
            History history = it.next();
            if (history.getContactInfo().equals(info) && history.getMessageSource().getSourceId().equals(serverId))
                it.remove();
        }
        pcs.firePropertyChange(PROP_CONTACT_REMOVE, null, info);
    }

    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        log.trace("removePropertyChangeListener({})", listener);
        pcs.removePropertyChangeListener(listener);
    }

}
