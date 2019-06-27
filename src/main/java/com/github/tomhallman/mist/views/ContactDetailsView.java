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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.tntapi.entities.ContactInfo;
import com.github.tomhallman.mist.util.ui.SimpleEmailLink;

public class ContactDetailsView extends Composite implements PropertyChangeListener {
    private static Logger log = LogManager.getLogger();

    private Group contactDetailsGroup;
    private StackLayout contactDetailsLayout;
    private ContactInfo contactInfo = null;

    // Unknown Contact View controls
    private Composite unknownContactView;
    private Button matchContactButton;
    private Button ignoreContactButton;
    private SimpleEmailLink emailLink;

    // Known Contact View controls
    private Composite knownContactView;
    private Label nameLabel;
    private Label phaseLabel;
    private Label pledgeLabel;

    public ContactDetailsView(Composite parent) {
        super(parent, SWT.NONE);
        log.trace("ContactDetailsView({})", parent);
        MIST.getView().getContactsView().addPropertyChangeListener(this);

        applyGridLayout(this).numColumns(1);

        // Contact Details Group
        contactDetailsGroup = new Group(this, SWT.NONE);
        applyGridData(contactDetailsGroup).withFill();
        contactDetailsLayout = new StackLayout();
        contactDetailsGroup.setLayout(contactDetailsLayout);
        contactDetailsGroup.setText("Contact Details");

        //
        // Unknown contact view
        //

        unknownContactView = new Composite(contactDetailsGroup, SWT.NONE);
        applyGridData(unknownContactView).withFill();
        applyGridLayout(unknownContactView).numColumns(2);

        // Email
        new Label(unknownContactView, SWT.NONE).setText("Email: ");
        emailLink = new SimpleEmailLink(unknownContactView, SWT.NONE);
        applyGridData(emailLink);

        // Match Contact button
        matchContactButton = new Button(unknownContactView, SWT.PUSH);
        matchContactButton.setText("Match Contact");
        applyGridData(matchContactButton).horizontalSpan(2);

        // Ignore Contact button
        ignoreContactButton = new Button(unknownContactView, SWT.PUSH);
        ignoreContactButton.setText("Ignore Contact");
        applyGridData(ignoreContactButton).horizontalSpan(2);

        //
        // Known contact view
        //

        knownContactView = new Composite(contactDetailsGroup, SWT.NONE);
        applyGridData(knownContactView).withFill();
        applyGridLayout(knownContactView).numColumns(2);

        // Name
        new Label(knownContactView, SWT.NONE).setText("Name: ");
        nameLabel = new Label(knownContactView, SWT.NONE);

        // Phase
        new Label(knownContactView, SWT.NONE).setText("Phase: ");
        phaseLabel = new Label(knownContactView, SWT.NONE);

        // Pledge
        new Label(knownContactView, SWT.NONE).setText("Pledge: ");
        pledgeLabel = new Label(knownContactView, SWT.NONE);

        // Start with known contact view
        contactDetailsLayout.topControl = knownContactView;
        contactDetailsGroup.layout();
    }

    public ContactInfo getContactInfo() {
        return contactInfo;
    }

    public Button getIgnoreContactButton() {
        return ignoreContactButton;
    }

    public Button getMatchContactButton() {
        return matchContactButton;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {

        if (ContactsView.PROP_CONTACT_SELECTED.equals(event.getPropertyName())) {
            // A new contact has been selected
            contactInfo = (ContactInfo) event.getNewValue();

            if (contactInfo.getId() == null) {
                // Unknown contact; show unknown contact view
                contactDetailsLayout.topControl = unknownContactView;
                emailLink.setEmail(contactInfo.getInfo());
                emailLink.requestLayout();
                contactDetailsGroup.layout();
            } else {
                // Known contact; show info from Tnt database
                contactDetailsLayout.topControl = knownContactView;
                contactDetailsGroup.layout();
            }
        }

    }
}
