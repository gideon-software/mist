/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2019 Tom Hallman
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

package com.github.tomhallman.mist.wizards.ignorecontact;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.wizard.Wizard;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.model.EmailModel;
import com.github.tomhallman.mist.preferences.Preferences;
import com.github.tomhallman.mist.tntapi.entities.ContactInfo;

/**
 * 
 */
public class IgnoreContactWizard extends Wizard {
    private static Logger log = LogManager.getLogger();

    private ContactInfo contactInfo = null;
    private IgnoreSettingsPage ignoreSettingsPage;

    public IgnoreContactWizard(ContactInfo contactInfo) {
        log.trace("IgnoreContactWizard({})", contactInfo);
        this.contactInfo = contactInfo;
        setWindowTitle("Ignore Contact Wizard");
    }

    @Override
    public void addPages() {
        log.trace("addPages()");

        ignoreSettingsPage = new IgnoreSettingsPage();
        addPage(ignoreSettingsPage);
    }

    @Override
    public boolean canFinish() {
        log.trace("canFinish()");
        return ignoreSettingsPage.isPageComplete();
    }

    public ContactInfo getContactInfo() {
        return contactInfo;
    }

    @Override
    public boolean performFinish() {
        log.trace("performFinish()");

        String email = ignoreSettingsPage.getEmail().trim();
        if (ignoreSettingsPage.isGlobalCheckSelected()) {
            // Get preference (char-separated list of strings)
            String ignoreEmailPref = MIST.getPrefs().getString(EmailModel.PREF_ADDRESSES_IGNORE);
            // Add email
            MIST.getPrefs().setValue(
                EmailModel.PREF_ADDRESSES_IGNORE,
                ignoreEmailPref + Preferences.getSeparator() + email);
        } else {
            for (int id : ignoreSettingsPage.getSelectedServerIds())
                EmailModel.getEmailServer(id).addIgnoreAddress(email);
        }

        return true;
    }
}
