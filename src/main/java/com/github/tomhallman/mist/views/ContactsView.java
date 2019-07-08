/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2017 Tom Hallman
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

package com.github.tomhallman.mist.views;

import static com.github.tomhallman.mist.util.ui.GridDataUtil.applyGridData;
import static com.github.tomhallman.mist.util.ui.GridLayoutUtil.applyGridLayout;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;

import com.github.tomhallman.mist.model.HistoryModel;
import com.github.tomhallman.mist.tntapi.entities.ContactInfo;
import com.github.tomhallman.mist.tntapi.entities.History;

public class ContactsView extends Composite implements PropertyChangeListener {
    private static Logger log = LogManager.getLogger();

    // Property change values
    public final static String PROP_CONTACT_SELECTED = "ContactsView.contact.selected";

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private int oldSelectionIndex;

    /**
     * The list of contacts that have associated history.
     */
    protected org.eclipse.swt.widgets.List contactList = null;

    /**
     * A list of all contacts who had history added to Tnt.
     */
    private java.util.List<ContactInfo> contacts = new ArrayList<ContactInfo>();

    public ContactsView(Composite parent) {
        super(parent, SWT.NONE);
        log.trace("ContactsView({})", parent);
        HistoryModel.addPropertyChangeListener(this);

        applyGridLayout(this).numColumns(1);

        // Contacts Group
        Group contactsGroup = new Group(this, SWT.NONE);
        applyGridLayout(contactsGroup).numColumns(1);
        applyGridData(contactsGroup).withFill();
        contactsGroup.setText("Contacts");

        // Contact list
        contactList = new List(contactsGroup, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        applyGridData(contactList).withFill();
        oldSelectionIndex = -1;
        contactList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                log.trace("contactList.widgetSelected({})", event);
                super.widgetSelected(event);
                int newSelectionIndex = contactList.getSelectionIndex();
                ContactInfo newInfo = null;
                if (newSelectionIndex != -1) {
                    newInfo = contacts.get(newSelectionIndex);
                    if (newSelectionIndex != oldSelectionIndex) {
                        pcs.firePropertyChange(PROP_CONTACT_SELECTED, null, newInfo);
                        oldSelectionIndex = newSelectionIndex;
                    }
                }
            }
        });
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        log.trace("propertyChange({})", event);

        if (HistoryModel.PROP_HISTORY_INIT.equals(event.getPropertyName())) {
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    // Initialize the contact list
                    contacts = new ArrayList<ContactInfo>();
                    contactList.removeAll();
                    oldSelectionIndex = -1;
                }
            });

        } // PROP_HISTORY_INIT

        else if (HistoryModel.PROP_HISTORY_ADD.equals(event.getPropertyName())) {
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    // Get the new history item
                    History history = (History) event.getNewValue();

                    ContactInfo ci = history.getContactInfo();
                    String originalName = ci.getName();
                    if (ci.getId() == null)
                        ci.setName(" [?] " + ci.getName());

                    // If this history item contains a contact not already in the list...
                    if (!contacts.contains(ci)) {
                        // Add to our UI list in sorted order (natural string sort)
                        int pos = 0;
                        while (pos < contactList.getItemCount() && contactList.getItem(pos).compareTo(ci.getName()) < 0)
                            pos++;
                        contactList.add(ci.getName(), pos);

                        // Add it to the contacts list (but without any [?], etc.)
                        ci.setName(originalName);
                        contacts.add(pos, ci);
                    }
                }
            });
        } // PROP_HISTORY_ADD

        else if (HistoryModel.PROP_CONTACT_REMOVE.equals(event.getPropertyName())) {
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    // Get the removed contact
                    ContactInfo ci = (ContactInfo) event.getNewValue();
                    String name = ci.getName();
                    if (ci.getId() == null)
                        ci.setName(String.format(" [?] %s", name));

                    // Remove the associated contact
                    int index = contacts.indexOf(ci);
                    if (index != -1) { // And if not, they were already removed!
                        contacts.remove(index);
                        contactList.remove(index);
                    }

                    oldSelectionIndex = -1;
                }
            });

        } // PROP_CONTACT_REMOVE
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}
