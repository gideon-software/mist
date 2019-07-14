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

package com.gideonsoftware.mist.wizards.matchcontact;

import static com.gideonsoftware.mist.util.ui.GridLayoutUtil.applyGridLayout;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.gideonsoftware.mist.tntapi.ContactManager;
import com.gideonsoftware.mist.tntapi.entities.Contact;
import com.gideonsoftware.mist.tntapi.entities.ContactInfo;
import com.gideonsoftware.mist.util.Util;
import com.gideonsoftware.mist.util.ui.SmartCombo;

/**
 * 
 */
public class SelectContactPage extends WizardPage {
    private static Logger log = LogManager.getLogger();

    private Contact contact = null;

    private SmartCombo<Integer> contactCombo;
    private Button newContactCheck;
    ContactInfo[] contactList = new ContactInfo[0];

    public SelectContactPage() {
        super("Select Contact Page");
        log.trace("SelectContactPage()");
        setTitle("Select Contact");
        setDescription("Select an existing contact or create a new one.");
    }

    @Override
    public void createControl(Composite parent) {
        log.trace("createControl({})", parent);
        setPageComplete(false);
        Composite comp = new Composite(parent, SWT.NONE);
        applyGridLayout(comp).numColumns(2);

        // Get contact list
        try {
            contactList = ContactManager.getContactList();
        } catch (SQLException e) {
            Util.reportError("Database connection error", "Could not load contact list", e);
        }

        // Get contact names
        String[] contactNames = new String[contactList.length];
        for (int i = 0; i < contactList.length; i++)
            contactNames[i] = contactList[i].getName();

        contactCombo = new SmartCombo<Integer>(comp, SWT.DROP_DOWN);
        for (ContactInfo ci : contactList)
            contactCombo.add(ci.getId(), ci.getName());
        contactCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                log.trace("contactCombo.modifyText()");
                setPageComplete(contactCombo.getSelectionItem() != null);
            }
        });
        contactCombo.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                log.trace("contactCombo:widgetDefaultSelected()");
                setPageComplete(true);
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                log.trace("contactCombo:widgetSelected()");
                setPageComplete(true);
            }
        });
        // Fill in last name by default
        MatchContactWizard wizard = ((MatchContactWizard) getWizard());
        contactCombo.setText(wizard.getContactInfo().guessLastName());

        newContactCheck = new Button(comp, SWT.CHECK);
        newContactCheck.setText("Create a new contact");
        newContactCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                log.trace("newContactCheck.widgetSelected()");
                contactCombo.setEnabled(!newContactCheck.getSelection());
                if (contactCombo.isEnabled())
                    contactCombo.autoComplete();
                setPageComplete(newContactCheck.getSelection());
            }
        });

        setControl(comp); // Needed for page to work properly
    }

    @Override
    public IWizardPage getNextPage() {
        log.trace("getNextPage()");
        MatchContactWizard wizard = ((MatchContactWizard) getWizard());
        Integer contactId = contactCombo.getSelectionItem();
        if (newContactCheck.getSelection() || contactId == null) {
            return wizard.getConfirmNewContactPage();
        } else {
            Contact contact = null;
            try {
                contact = ContactManager.get(contactId);
            } catch (SQLException e) {
                Util.reportError("Database connection error", "Could not load contact", e);
                return null;
            }
            if (contact.hasSpouse())
                return wizard.getSelectContactSpousePage();
            else
                return wizard.getConfirmExistingContactPage();
        }
    }

    public Contact getSelectedContact() {
        log.trace("getSelectedContact()");
        if (getSelectedContactId() == null)
            return null;

        // We haven't yet loaded the contact; get it now
        try {
            contact = ContactManager.get(getSelectedContactId());
        } catch (SQLException e) {
            Util.reportError("Database connection error", "Could not load contact", e);
            return null;
        }

        return contact;
    }

    public Integer getSelectedContactId() {
        log.trace("getSelectedContactId()");
        if (newContactCheck.getSelection() || contactCombo.getSelectionItem() == null)
            return null;
        return contactCombo.getSelectionItem();
    }
}
