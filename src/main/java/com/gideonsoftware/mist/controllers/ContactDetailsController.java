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

package com.gideonsoftware.mist.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import com.gideonsoftware.mist.model.EmailModel;
import com.gideonsoftware.mist.model.HistoryModel;
import com.gideonsoftware.mist.model.MessageModel;
import com.gideonsoftware.mist.model.data.MessageSource;
import com.gideonsoftware.mist.tntapi.entities.ContactInfo;
import com.gideonsoftware.mist.tntapi.entities.History;
import com.gideonsoftware.mist.views.ContactDetailsView;
import com.gideonsoftware.mist.wizards.ignorecontact.IgnoreContactWizard;
import com.gideonsoftware.mist.wizards.matchcontact.MatchContactWizard;

/**
 * 
 */
public class ContactDetailsController {
    private static Logger log = LogManager.getLogger();

    /**
     * 
     */
    public ContactDetailsController(ContactDetailsView view) {
        log.trace("ContactDetailsController({})", view);

        view.getMatchContactButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                log.trace("matchContactButton.widgetSelected({})", event);
                ContactInfo matchedContactInfo = new ContactInfo(view.getContactInfo());
                WizardDialog dlg = new WizardDialog(view.getShell(), new MatchContactWizard(matchedContactInfo));
                if (dlg.open() == Window.OK) {
                    // The view still contains old info; get associated history now
                    History[] historyArr = HistoryModel.getAllHistoryWithContactInfo(view.getContactInfo());

                    // Remove the history
                    HistoryModel.removeAllHistoryWithContactInfo(view.getContactInfo());

                    for (History history : historyArr) {
                        MessageSource msgSource = history.getMessageSource();
                        // Don't put existing history into our model a second time. Otherwise, a message to multiple
                        // contacts could show duplicates in the model (though not in the DB)
                        msgSource.setAddExistingHistory(false);

                        // Add the message back into the queue for reprocessing
                        MessageModel.addMessage(msgSource);
                    }
                }
            }
        });

        view.getIgnoreContactButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                log.trace("ignoreContactButton.widgetSelected({})", event);
                ContactInfo contactInfo = new ContactInfo(view.getContactInfo());
                WizardDialog dlg = new WizardDialog(view.getShell(), new IgnoreContactWizard(contactInfo));
                if (dlg.open() == Window.OK) {
                    // Check all unknown history to see if anything there should be ignored now
                    // This must be done because of wildcards
                    for (History history : HistoryModel.getUnknownHistory()) {
                        ContactInfo ci = history.getContactInfo();
                        String email = ci.getInfo();
                        int serverId = history.getMessageSource().getSourceId();
                        if (EmailModel.isEmailInIgnoreList(email)) {
                            HistoryModel.removeAllHistoryWithContactInfo(ci);
                        } else if (EmailModel.getEmailServer(serverId).isEmailInIgnoreList(email)) {
                            HistoryModel.removeAllHistoryWithContactInfo(ci, serverId);
                        }
                    }
                }
            }
        });
    }
}
