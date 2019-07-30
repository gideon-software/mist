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

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import com.gideonsoftware.mist.tntapi.HistoryManager;
import com.gideonsoftware.mist.tntapi.entities.History;
import com.gideonsoftware.mist.util.Util;
import com.gideonsoftware.mist.views.MessageDetailsView;

public class MessageDetailsController {
    private static Logger log = LogManager.getLogger();

    private MessageDetailsView view;

    /**
     * Our cached copy of the view's selected History ID. Used in case the view gets disposed and we still need to
     * commit changes to the Tnt database.
     */
    private Integer historyId;
    private String msgStr;
    private String subjectStr;

    public MessageDetailsController(MessageDetailsView view) {
        log.trace("MessageDetailsController({})", view);
        this.view = view;
        historyId = null;
        subjectStr = null;
        msgStr = null;

        view.getSubjectText().addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (view != null && !view.isDisposed()) {
                    subjectStr = view.getSubjectText().getText();
                    if (view.getHistory() != null)
                        historyId = view.getHistory().getHistoryId(); // Keep our cached copy in case view gets disposed
                }
            }
        });

        view.getMessageText().addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (view != null && !view.isDisposed()) {
                    msgStr = view.getMessageText().getText();
                    if (view.getHistory() != null)
                        historyId = view.getHistory().getHistoryId(); // Keep our cached copy in case view gets disposed
                }
            }
        });

        view.getSubjectText().addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                log.trace("subjectText.focusLost({})", event);
                commitSubjectToTntDb();
            }
        });

        view.getMessageText().addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                log.trace("messageText.focusLost({})", event);
                commitMessageToTntDb();
            }
        });

        view.getChallengeCheckBox().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                log.trace("challengeCheckBox.widgetSelected({})", event);
                // Don't need our cached copy since a checkbox selection requires an enabled view
                History history = view.getHistory();
                boolean selection = view.getChallengeCheckBox().getSelection();
                history.setChallenge(selection);
                try {
                    HistoryManager.updateIsChallenge(history.getHistoryId(), selection);
                } catch (SQLException e) {
                    String msg = "Could not update Partnership Challenge status";
                    Util.reportError("Update error", msg, e);
                }
            }
        });

        view.getThankCheckBox().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                log.trace("thankCheckBox.widgetSelected({})", event);
                // Don't need our cached copy since a checkbox selection requires an enabled view
                History history = view.getHistory();
                boolean selection = view.getThankCheckBox().getSelection();
                history.setThank(selection);
                try {
                    HistoryManager.updateIsThank(history.getHistoryId(), selection);
                } catch (SQLException e) {
                    String msg = "Could not update Thank status";
                    Util.reportError("Update error", msg, e);
                }
            }
        });

        view.getMassMailingCheckBox().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                log.trace("massMailingCheckBox.widgetSelected({})", event);
                // Don't need our cached copy since a checkbox selection requires an enabled view
                History history = view.getHistory();
                boolean selection = view.getMassMailingCheckBox().getSelection();
                history.setMassMailing(selection);
                try {
                    HistoryManager.updateIsMassMailing(history.getHistoryId(), selection);
                } catch (SQLException e) {
                    String msg = "Could not update Mass Mailing status";
                    Util.reportError("Update error", msg, e);
                }
            }
        });

    }

    public void commitMessageToTntDb() {
        log.trace("commitMessageToTntDb");
        if (historyId == null || msgStr == null)
            return;

        if (view != null && !view.isDisposed())
            view.getHistory().setNotes(msgStr);

        try {
            HistoryManager.updateNotes(historyId, msgStr);
        } catch (SQLException e) {
            Util.reportError("Update error", "Could not update history body", e);
        }
    }

    public void commitSubjectToTntDb() {
        log.trace("commitSubjectToTntDb");
        if (historyId == null || subjectStr == null)
            return;

        if (view != null && !view.isDisposed())
            view.getHistory().setDescription(subjectStr);

        try {
            HistoryManager.updateDescription(historyId, subjectStr);
        } catch (SQLException e) {
            Util.reportError("Update error", "Could not update history subject", e);
        }
    }
}
