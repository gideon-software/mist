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
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gideonsoftware.mist.tntapi.entities.ContactInfo;
import com.gideonsoftware.mist.tntapi.entities.History;

public class HistoryModel {
    private static Logger log = LogManager.getLogger();

    // Preferences
    public final static String PREF_TOTAL_IMPORTED = "history.total.imported";

    // Property change values
    private final static PropertyChangeSupport pcs = new PropertyChangeSupport(HistoryModel.class);
    public final static String PROP_HISTORY_ADD = "historymodel.history.add";
    public final static String PROP_HISTORY_INIT = "historymodel.history.init";
    public final static String PROP_CONTACT_REMOVE = "historymodel.contact.remove";

    // A list of all history added to Tnt (including errors)
    private static volatile ArrayList<History> historyArr = new ArrayList<History>();

    /**
     * No instantiation allowed!
     */
    private HistoryModel() {
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

    public static History getHistoryAt(int index) {
        // log.trace("getHistoryAt({})", index);
        return historyArr.get(index);
    }

    public static int getHistorySize() {
        // log.trace("getHistorySize()");
        return historyArr.size();
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
        historyArr = new ArrayList<History>();
        pcs.firePropertyChange(PROP_HISTORY_INIT, false, true); // Newly-initialized history array!
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
