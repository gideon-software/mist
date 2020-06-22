/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2017 Gideon Software
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

package com.gideonsoftware.mist.views;

import static com.gideonsoftware.mist.util.ui.GridDataUtil.applyGridData;
import static com.gideonsoftware.mist.util.ui.GridLayoutUtil.applyGridLayout;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;

import com.gideonsoftware.mist.model.HistoryModel;
import com.gideonsoftware.mist.tntapi.entities.ContactInfo;
import com.gideonsoftware.mist.tntapi.entities.History;
import com.gideonsoftware.mist.util.ui.TipMessageBox;

public class ContactsView extends Composite implements PropertyChangeListener {
    private static Logger log = LogManager.getLogger();

    // Tips
    public final static String SHOWTIP_UNMATCHED_CONTACT = "showtip.contact.unmatched";

    // Property change values
    public final static String PROP_CONTACT_SELECTED = "ContactsView.contact.selected";

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private int oldSelectionIndex;
    private int unmatchedContactFound = 0;

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
        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                log.trace("ContactsView.widgetDisposed()");
                HistoryModel.removePropertyChangeListener(ContactsView.this);
            }
        });

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
        log.trace("addPropertyChangeListener({})", listener);
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        log.trace("propertyChange({})", event);

        if (Display.getDefault().isDisposed() || contactList.isDisposed())
            return;

        if (HistoryModel.PROP_HISTORY_INIT.equals(event.getPropertyName())) {
            Display.getDefault().asyncExec(() -> {
                // Initialize the contact list
                contacts = new ArrayList<ContactInfo>();
                contactList.removeAll();
                oldSelectionIndex = -1;

                // Force UI update (needed on Mac)
                contactList.update();
            });

        } // PROP_HISTORY_INIT

        else if (HistoryModel.PROP_HISTORY_ADD.equals(event.getPropertyName())) {
            Display.getDefault().asyncExec(() -> {
                // Get the new history item
                History history = (History) event.getNewValue();

                ContactInfo ci = history.getContactInfo();
                String originalName = ci.getName();
                if (ci.getId() == null) {
                    ci.setName(" [?] " + ci.getName());
                    // Show tip if need be (max once per session)
                    if (unmatchedContactFound++ == 0)
                        showUnmatchedContactTip();
                }

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

                    // Force UI update (needed on Mac)
                    contactList.update();
                }
            });
        } // PROP_HISTORY_ADD

        else if (HistoryModel.PROP_CONTACT_REMOVE.equals(event.getPropertyName())) {
            Display.getDefault().asyncExec(() -> {
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

                // Force UI update (needed on Mac)
                contactList.update();
            });

        } // PROP_CONTACT_REMOVE
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    private void showUnmatchedContactTip() {
        String title = "Unmatched Contacts";
        String message = """
            MIST didn't recognize one or more of your contacts because there was no associated
            email address in TntConnect. These are marked with '[?]' in MIST's contact list.

            For each of these contacts, you can do any of the following:
              * Match the contact in TntConnect:\tUse the "Match Contact" button
              * Ignore the contact (this time):\tDo nothing
              * Ignore the contact (always):\tUse the "Ignore Contact" button
              * If the contact is actually "you", add it to 'My Email Addresses' in Settings.
            """;
        (new TipMessageBox(getShell(), SHOWTIP_UNMATCHED_CONTACT, title, message)).open();
    }
}
