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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.gideonsoftware.mist.tntapi.entities.Contact;

/**
 * 
 */
public class ConfirmExistingContactPage extends WizardPage {
    private static Logger log = LogManager.getLogger();

    private Label infoLabel;

    public ConfirmExistingContactPage() {
        super("Confirm Existing Contact Page");
        log.trace("ConfirmExistingContactPage()");
        setTitle("Confirm Contact Match");
        setDescription("");
    }

    @Override
    public void createControl(Composite parent) {
        log.trace("createControl({})", parent);
        setPageComplete(false);
        Composite comp = new Composite(parent, SWT.NONE);
        applyGridLayout(comp).numColumns(1);

        infoLabel = new Label(comp, SWT.NONE);

        setControl(comp); // Needed for page to work properly
    }

    @Override
    public IWizardPage getNextPage() {
        log.trace("getNextPage()");
        return null;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        log.trace("setVisible({})", visible);
        if (visible) {

            MatchContactWizard wiz = ((MatchContactWizard) getWizard());

            // Get email
            String email = wiz.getContactInfo().getInfo();
            Contact contact = wiz.getSelectContactPage().getSelectedContact();

            // Get name
            String name = contact.getFullName();
            if (contact.hasSpouse())
                name = wiz.getSelectContactSpousePage().getSelectedSpouseName();

            infoLabel.setText(String.format("'%s' will become associated with %s.", email, name));
            infoLabel.requestLayout();

            setPageComplete(true);
        } else
            setPageComplete(false);
    }

}
