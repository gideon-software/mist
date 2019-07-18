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
import java.util.LinkedList;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gideonsoftware.mist.model.data.MessageSource;

public class MessageModel {
    private static Logger log = LogManager.getLogger();

    // Property change values
    private final static PropertyChangeSupport pcs = new PropertyChangeSupport(MessageModel.class);
    public final static String PROP_MESSAGE_ADD = "messagemodel.message.add";
    public final static String PROP_MESSAGE_INIT = "messagemodel.message.init";

    private static volatile Queue<MessageSource> messageQueue = new LinkedList<MessageSource>();

    /**
     * No instantiation allowed!
     */
    private MessageModel() {
    }

    public static synchronized void addMessage(MessageSource message) {
        log.trace("addMessage({})", message);
        if (!messageQueue.offer(message)) {
            log.error("Couldn't add message to queue!");
            return;
        }
        pcs.firePropertyChange(PROP_MESSAGE_ADD, null, message);
    }

    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        log.trace("addPropertyChangeListener({})", listener);
        pcs.addPropertyChangeListener(listener);
    }

    public static synchronized int getMessageCount() {
        return messageQueue.size();
    }

    public static synchronized MessageSource getNextMessage() {
        return messageQueue.poll();
    }

    public static synchronized boolean hasMessages() {
        return !messageQueue.isEmpty();
    }

    public static void init() {
        log.trace("init()");
        messageQueue.clear();
        pcs.firePropertyChange(PROP_MESSAGE_INIT, false, true); // Newly-initialized message list!
    }

    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        log.trace("removePropertyChangeListener({})", listener);
        pcs.removePropertyChangeListener(listener);
    }
}
