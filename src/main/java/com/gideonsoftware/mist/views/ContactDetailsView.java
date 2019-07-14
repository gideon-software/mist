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
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.exceptions.TntDbException;
import com.gideonsoftware.mist.model.HistoryModel;
import com.gideonsoftware.mist.tntapi.ContactManager;
import com.gideonsoftware.mist.tntapi.TntDb;
import com.gideonsoftware.mist.tntapi.entities.Contact;
import com.gideonsoftware.mist.tntapi.entities.ContactInfo;
import com.gideonsoftware.mist.util.Util;
import com.gideonsoftware.mist.util.ui.Images;
import com.gideonsoftware.mist.util.ui.SimpleEmailLink;

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
    private Label phaseLabel;
    private Label pledgeLabel;
    private Label lastGiftLabel;

    public ContactDetailsView(Composite parent) {
        super(parent, SWT.NONE);
        log.trace("ContactDetailsView({})", parent);
        MIST.getView().getContactsView().addPropertyChangeListener(this);
        HistoryModel.addPropertyChangeListener(this);

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
        matchContactButton.setImage(Images.getImage(Images.ICON_MATCH_CONTACT));
        applyGridData(matchContactButton).horizontalSpan(2);

        // Ignore Contact button
        ignoreContactButton = new Button(unknownContactView, SWT.PUSH);
        ignoreContactButton.setText("Ignore Contact");
        ignoreContactButton.setImage(Images.getImage(Images.ICON_IGNORE_CONTACT));
        applyGridData(ignoreContactButton).horizontalSpan(2);

        //
        // Known contact view
        //

        knownContactView = new Composite(contactDetailsGroup, SWT.NONE);
        applyGridData(knownContactView).withFill();
        applyGridLayout(knownContactView).numColumns(2);

        // Phase
        new Label(knownContactView, SWT.NONE).setText("Phase: ");
        phaseLabel = new Label(knownContactView, SWT.NONE);

        // Pledge
        new Label(knownContactView, SWT.NONE).setText("Pledge: ");
        pledgeLabel = new Label(knownContactView, SWT.NONE);

        // Last Gift
        new Label(knownContactView, SWT.NONE).setText("Last gift: ");
        lastGiftLabel = new Label(knownContactView, SWT.NONE);

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
                // Fill in data
                try {
                    Contact contact = ContactManager.get(contactInfo.getId());
                    phaseLabel.setText(TntDb.getMpdPhaseDescription(contact.getMpdPhaseId()));
                    pledgeLabel.setText(contact.getPledgeStr());
                    lastGiftLabel.setText(contact.getLastGiftStr());
                    phaseLabel.requestLayout();
                    lastGiftLabel.requestLayout();
                    pledgeLabel.requestLayout();
                    contactDetailsGroup.layout();
                } catch (TntDbException | SQLException e) {
                    Util.reportError("Database connection error", "Could not load contact", e);
                    return;
                }
            }

        } else if (HistoryModel.PROP_CONTACT_REMOVE.equals(event.getPropertyName())
            || HistoryModel.PROP_HISTORY_INIT.equals(event.getPropertyName())) {
            // The selected contact has been removed; clear the view
            contactDetailsLayout.topControl = knownContactView;
            contactDetailsGroup.layout();
            // Clear data
            phaseLabel.setText("");
            pledgeLabel.setText("");
            lastGiftLabel.setText("");
            phaseLabel.requestLayout();
            lastGiftLabel.requestLayout();
            pledgeLabel.requestLayout();
            contactDetailsGroup.layout();
        }

    }
}
