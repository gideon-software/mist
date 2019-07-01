/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2010 Tom Hallman
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

package com.github.tomhallman.mist.wizards.matchcontact;

import java.sql.SQLException;
import java.time.LocalDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.wizard.Wizard;

import com.github.tomhallman.mist.exceptions.TntDbException;
import com.github.tomhallman.mist.tntapi.ContactManager;
import com.github.tomhallman.mist.tntapi.entities.Contact;
import com.github.tomhallman.mist.tntapi.entities.ContactInfo;
import com.github.tomhallman.mist.util.Util;

/**
 * 
 */
public class MatchContactWizard extends Wizard {
    private static Logger log = LogManager.getLogger();

    private ContactInfo contactInfo = null;

    private SelectContactPage selectContactPage;
    private SelectContactSpousePage selectContactSpousePage;
    private ConfirmNewContactPage confirmNewContactPage;
    private ConfirmExistingContactPage confirmExistingContactPage;

    public MatchContactWizard(ContactInfo contactInfo) {
        log.trace("MatchContactWizard({})", contactInfo);
        this.contactInfo = contactInfo;
        setWindowTitle("Match Contact Wizard");
    }

    @Override
    public void addPages() {
        log.trace("addPages()");
        selectContactPage = new SelectContactPage();
        selectContactSpousePage = new SelectContactSpousePage();
        confirmNewContactPage = new ConfirmNewContactPage();
        confirmExistingContactPage = new ConfirmExistingContactPage();

        addPage(selectContactPage);
        addPage(selectContactSpousePage);
        addPage(confirmNewContactPage);
        addPage(confirmExistingContactPage);
    }

    @Override
    public boolean canFinish() {
        log.trace("canFinish()");
        return confirmNewContactPage.isPageComplete() || confirmExistingContactPage.isPageComplete();
    }

    public ConfirmExistingContactPage getConfirmExistingContactPage() {
        return confirmExistingContactPage;
    }

    public ConfirmNewContactPage getConfirmNewContactPage() {
        return confirmNewContactPage;
    }

    public ContactInfo getContactInfo() {
        return contactInfo;
    }

    public SelectContactPage getSelectContactPage() {
        return selectContactPage;
    }

    public SelectContactSpousePage getSelectContactSpousePage() {
        return selectContactSpousePage;
    }

    @Override
    public boolean performFinish() {
        log.trace("performFinish()");
        Integer contactId = selectContactPage.getSelectedContactId();
        if (contactId == null) {
            //
            // Match with a new contact
            //

            // TODO: Confirm before creating new contact that this is what they want
            // Show all matching last names in a list; if one exists & is selected, match on that one

            Contact contact = new Contact();
            // Fill in fields for initial contact
            LocalDateTime now = LocalDateTime.now();
            contact.setLastEdit(now);
            contact.setCreatedDate(now);
            contact.setInitialNameFields(confirmNewContactPage.getFirstName(), confirmNewContactPage.getLastName());
            contact.setInitialEmailFields(contactInfo.getInfo());
            try {
                contactId = ContactManager.create(contact);
            } catch (TntDbException | SQLException e) {
                Util.reportError(getShell(), "Database error", "Could not create contact", e);
                return false;
            }

        } else {
            //
            // Match with existing contact
            //

            String email = contactInfo.getInfo();
            Contact contact = selectContactPage.getSelectedContact();
            boolean usePrimaryContact = !contact.hasSpouse() || selectContactSpousePage.isPrimaryContactSelected();

            try {
                ContactManager.addNewEmailAddress(email, contactId, usePrimaryContact);
            } catch (SQLException | TntDbException e) {
                Util.reportError(getShell(), "Database error", "Could not create contact", e);
                return false;
            }
        }
        contactInfo.setId(contactId); // So the calling function can reference the matched contactId
        return true;
    }
}
