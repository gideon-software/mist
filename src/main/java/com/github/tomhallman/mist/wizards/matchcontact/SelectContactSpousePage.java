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

import static com.github.tomhallman.mist.util.ui.GridLayoutUtil.applyGridLayout;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.github.tomhallman.mist.tntapi.entities.Contact;

/**
 * 
 */
public class SelectContactSpousePage extends WizardPage {
    private class SpouseSelectionListener extends SelectionAdapter {
        @Override
        public void widgetSelected(SelectionEvent e) {
            log.trace("SpouseSelectionListener.widgetSelected()");
            setPageComplete(spouse1Radio.getSelection() || spouse2Radio.getSelection());
        }
    }

    private static Logger log = LogManager.getLogger();
    private Button spouse1Radio;
    private Button spouse2Radio;
    private Integer oldContactId = null;

    public SelectContactSpousePage() {
        super("Select Contact Spouse Page");
        log.trace("SelectContactSpousePage()");
        setTitle("Select Contact Spouse");
        setDescription("Select which spouse to match this email address with.");
    }

    @Override
    public void createControl(Composite parent) {
        log.trace("createControl({})", parent);
        setPageComplete(false);
        Composite comp = new Composite(parent, SWT.NONE);
        applyGridLayout(comp);

        spouse1Radio = new Button(comp, SWT.RADIO);
        spouse1Radio.addSelectionListener(new SpouseSelectionListener());
        spouse2Radio = new Button(comp, SWT.RADIO);
        spouse2Radio.addSelectionListener(new SpouseSelectionListener());

        setControl(comp); // Needed for page to work properly
    }

    @Override
    public IWizardPage getNextPage() {
        log.trace("getNextPage()");
        MatchContactWizard wizard = ((MatchContactWizard) getWizard());
        return wizard.getConfirmExistingContactPage();
    }

    public String getSelectedSpouseName() {
        Contact contact = ((MatchContactWizard) getWizard()).getSelectContactPage().getSelectedContact();
        if (spouse1Radio.getSelection())
            return String.format("%s %s", contact.getFirstName(), contact.getLastName());
        if (spouse2Radio.getSelection())
            return String.format(
                "%s %s",
                contact.getSpouseFirstName(),
                contact.getSpouseLastName().isBlank() ? contact.getLastName() : contact.getSpouseLastName());
        return "";
    }

    public boolean isPrimaryContactSelected() {
        return spouse1Radio.getSelection();
    }

    @Override
    public void setVisible(boolean visible) {
        log.trace("setVisible({})", visible);
        if (visible) {
            Contact contact = ((MatchContactWizard) getWizard()).getSelectContactPage().getSelectedContact();
            spouse1Radio.setText(contact.getFirstName());
            spouse2Radio.setText(contact.getSpouseFirstName());
            if (!contact.getContactId().equals(oldContactId)) {
                // Only change selection if the contact changed
                spouse1Radio.setSelection(false);
                spouse2Radio.setSelection(false);
                oldContactId = contact.getContactId();
            }
            spouse1Radio.requestLayout();
            spouse2Radio.requestLayout();
        }
        super.setVisible(visible);
    }

}
