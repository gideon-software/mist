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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.tntapi.entities.History;
import com.github.tomhallman.mist.util.ui.Images;

public class MessageDetailsView extends Composite implements PropertyChangeListener {
    private static Logger log = LogManager.getLogger();

    public final static String PROP_SUBJECT = "MessageDetailsView.subject";
    // Property change values
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private History history;

    private Composite infoComp;
    private Label infoTextLabel;
    private Label infoImageLabel;
    private Label subjectLabel;
    private Text subjectText;
    private Text msgText;
    private Button challengeCheckBox;
    private Button thankCheckBox;
    private Button massMailingCheckBox;

    public MessageDetailsView(Composite parent) {
        super(parent, SWT.NONE);
        log.trace("MessageDetailsView({})", parent);
        MIST.getView().getContactsView().addPropertyChangeListener(this);
        MIST.getView().getMessagesView().addPropertyChangeListener(this);

        applyGridLayout(this).numColumns(1);

        // Group
        Group group = new Group(this, SWT.NONE);
        applyGridLayout(group).numColumns(1);
        applyGridData(group).withFill();
        group.setText("Email Details");

        // Info image & text
        infoComp = new Composite(group, SWT.NONE);
        applyGridLayout(infoComp).zeroMarginAndSpacing().numColumns(2);
        applyGridData(infoComp).withHorizontalFill();
        infoImageLabel = new Label(infoComp, SWT.NONE);
        infoTextLabel = new Label(infoComp, SWT.NONE);
        applyGridData(infoTextLabel).withHorizontalFill();

        // Subject text
        Composite subjectComp = new Composite(group, SWT.NONE);
        applyGridLayout(subjectComp).zeroMarginAndSpacing().numColumns(2);
        applyGridData(subjectComp).withHorizontalFill();
        subjectLabel = new Label(subjectComp, SWT.NONE);
        subjectLabel.setText("Subject:  ");
        subjectText = new Text(subjectComp, SWT.BORDER);
        applyGridData(subjectText).withHorizontalFill();
        subjectText.setTextLimit(100);
        subjectText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                pcs.firePropertyChange(PROP_SUBJECT, null, subjectText.getText());
            }
        });

        // Message text
        msgText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        applyGridData(msgText).withFill();

        // Checkboxes
        Composite checkBoxComp = new Composite(group, SWT.NONE);
        applyGridLayout(checkBoxComp).numColumns(3);
        applyGridData(checkBoxComp).withHorizontalFill();
        challengeCheckBox = new Button(checkBoxComp, SWT.CHECK);
        challengeCheckBox.setText("Partnership Challenge");
        thankCheckBox = new Button(checkBoxComp, SWT.CHECK);
        thankCheckBox.setText("Thank");
        massMailingCheckBox = new Button(checkBoxComp, SWT.CHECK);
        massMailingCheckBox.setText("Mass Mailing");

        setEnabled(false);
        clearControls();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void clearControls() {
        log.trace("clearControls({})");
        infoImageLabel.setImage(null);
        infoTextLabel.setText("Select an email to see details");
        infoComp.pack(true);
        subjectText.setText("");
        msgText.setText("");
        challengeCheckBox.setSelection(false);
        thankCheckBox.setSelection(false);
        massMailingCheckBox.setSelection(false);
    }

    public Button getChallengeCheckBox() {
        return challengeCheckBox;
    }

    public History getHistory() {
        return history;
    }

    public Button getMassMailingCheckBox() {
        return massMailingCheckBox;
    }

    public Text getMessageText() {
        return msgText;
    }

    public Text getSubjectText() {
        return subjectText;
    }

    public Button getThankCheckBox() {
        return thankCheckBox;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        log.trace("propertyChange({})", event);

        if (ContactsView.PROP_CONTACT_SELECTED.equals(event.getPropertyName())) {
            // A new contact has been selected; clear this view
            setEnabled(false);
            clearControls();
            history = null;

        } else if (MessagesView.PROP_MESSAGE.equals(event.getPropertyName())) {
            // A new message has been selected; load the details
            history = (History) event.getNewValue();

            String infoTextStr = "";
            switch (history.getStatus()) {
                case History.STATUS_EXISTS:
                case History.STATUS_ADDED:
                    infoTextStr = "Edits below are automatically saved in TntConnect";
                    setEditable(true);
                    break;
                case History.STATUS_CONTACT_NOT_FOUND:
                    infoTextStr = String.format(
                        "No contact is associated with '%s'. Please match the contact.",
                        history.getContactInfo().getInfo());
                    setEditable(false);
                    break;
                case History.STATUS_MULTIPLE_CONTACTS_FOUND:
                    infoTextStr = String.format(
                        "Multiple contacts are associated with '%s'. Please fix this in TntConnect and then run the import again",
                        history.getContactInfo().getInfo());
                    setEditable(false);
                    break;
                case History.STATUS_ERROR:
                    infoTextStr = "There was an unknown problem importing this message. The error message is below.";
                    setEditable(false);
                    break;
            }

            // Set infoLabel stuff
            infoTextLabel.setText(" " + infoTextStr);
            Image infoImage = null;
            if (history.getStatus() != History.STATUS_ADDED && history.getStatus() != History.STATUS_EXISTS)
                infoImage = Images.getStatusImage(history.getStatus());
            infoImageLabel.setImage(infoImage);
            infoComp.pack(true);

            // Set message subject & body text
            if (history.getStatus() != History.STATUS_ERROR) {
                subjectText.setText(history.getDescription());
                msgText.setText(history.getNotes());
            } else {
                subjectText.setText("");
                msgText.setText(history.getStatusException().toString());
            }

            // Set checkbox stuff
            challengeCheckBox.setSelection(history.isChallenge());
            thankCheckBox.setSelection(history.isThank());
            massMailingCheckBox.setSelection(history.isMassMailing());
        } // MessagesView.PROP_MESSAGE
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void setEditable(boolean editable) {
        log.trace("setEditable({})", editable);
        infoTextLabel.setEnabled(true);
        subjectLabel.setEnabled(true);
        subjectText.setEditable(editable);
        msgText.setEditable(editable);
        if (editable) {
            subjectText.setEnabled(true);
            msgText.setEnabled(true);
        }
        challengeCheckBox.setEnabled(editable);
        thankCheckBox.setEnabled(editable);
        massMailingCheckBox.setEnabled(editable);
    }

    @Override
    public void setEnabled(boolean enabled) {
        log.trace("setEnabled({})", enabled);
        infoTextLabel.setEnabled(enabled);
        subjectLabel.setEnabled(enabled);
        subjectText.setEnabled(enabled);
        msgText.setEnabled(enabled);
        challengeCheckBox.setEnabled(enabled);
        thankCheckBox.setEnabled(enabled);
        massMailingCheckBox.setEnabled(enabled);
    }
}
