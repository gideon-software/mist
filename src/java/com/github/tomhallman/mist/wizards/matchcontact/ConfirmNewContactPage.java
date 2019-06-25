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

import static com.github.tomhallman.mist.util.ui.GridDataUtil.applyGridData;
import static com.github.tomhallman.mist.util.ui.GridLayoutUtil.applyGridLayout;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.github.tomhallman.mist.tntapi.entities.ContactInfo;
import com.github.tomhallman.mist.util.ui.SimpleEmailLink;

/**
 * 
 */
public class ConfirmNewContactPage extends WizardPage {

    private class NameTextModifyListener implements ModifyListener {
        @Override
        public void modifyText(ModifyEvent e) {
            log.trace("NameTextModifyListener.modifyText()");
            setPageComplete(!firstNameText.getText().isBlank() || !lastNameText.getText().isBlank());
            getWizard().getContainer().updateButtons();
        }
    }

    private static Logger log = LogManager.getLogger();
    private Text firstNameText;
    private Text lastNameText;
    private SimpleEmailLink emailLink;

    public ConfirmNewContactPage() {
        super("Confirm New Contact Page");
        log.trace("ConfirmNewContactPage()");
        setTitle("Confirm New Contact");
        setDescription("A new contact will be created with this information.");
    }

    @Override
    public void createControl(Composite parent) {
        log.trace("createControl({})", parent);
        setPageComplete(false);
        Composite comp = new Composite(parent, SWT.NONE);
        applyGridLayout(comp).numColumns(2);

        new Label(comp, SWT.NONE).setText("First name: ");
        firstNameText = new Text(comp, SWT.BORDER);
        applyGridData(firstNameText).widthHint(200);
        firstNameText.addModifyListener(new NameTextModifyListener());

        new Label(comp, SWT.NONE).setText("Last name: ");
        lastNameText = new Text(comp, SWT.BORDER);
        applyGridData(lastNameText).widthHint(200);
        lastNameText.addModifyListener(new NameTextModifyListener());

        new Label(comp, SWT.NONE).setText("Email: ");
        emailLink = new SimpleEmailLink(comp, SWT.NONE);

        setControl(comp); // Needed for page to work properly
    }

    public String getFirstName() {
        return firstNameText.getText();
    }

    public String getLastName() {
        return lastNameText.getText();
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
            ContactInfo ci = ((MatchContactWizard) getWizard()).getContactInfo();
            String[] names = ContactInfo.guessFirstAndLastNames(ci.getName());
            firstNameText.setText(names[0]);
            lastNameText.setText(names[1]);
            emailLink.setEmail(ci.getInfo());
            emailLink.requestLayout();
        }
        setPageComplete(visible);
    }

}
