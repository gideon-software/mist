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

package com.github.tomhallman.mist.controllers;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import com.github.tomhallman.mist.tntapi.HistoryManager;
import com.github.tomhallman.mist.tntapi.entities.History;
import com.github.tomhallman.mist.util.Util;
import com.github.tomhallman.mist.views.MessageDetailsView;

public class MessageDetailsController {
    private class ChallengeSelectionListener extends SelectionAdapter {
        @Override
        public void widgetSelected(SelectionEvent event) {
            log.trace("ChallengeSelectionListener.widgetSelected({})", event);
            // Don't need our cached copy since a checkbox selection requires an enabled view
            History history = view.getHistory();
            boolean selection = view.getChallengeCheckBox().getSelection();
            history.setChallenge(selection);
            try {
                HistoryManager.updateIsChallenge(history.getHistoryId(), selection);
            } catch (SQLException e) {
                String msg = "Could not update Partnership Challenge status";
                Util.reportError(view.getShell(), "Update error", msg, e);
            }
        }
    }

    private class MassMailingSelectionListener extends SelectionAdapter {
        @Override
        public void widgetSelected(SelectionEvent event) {
            log.trace("MassMailingSelectionListener.widgetSelected({})", event);
            // Don't need our cached copy since a checkbox selection requires an enabled view
            History history = view.getHistory();
            boolean selection = view.getMassMailingCheckBox().getSelection();
            history.setMassMailing(selection);
            try {
                HistoryManager.updateIsMassMailing(history.getHistoryId(), selection);
            } catch (SQLException e) {
                String msg = "Could not update Mass Mailing status";
                Util.reportError(view.getShell(), "Update error", msg, e);
            }
        }
    }

    private class MessageFocusListener extends FocusAdapter {
        @Override
        public void focusLost(FocusEvent event) {
            log.trace("MessageFocusListener.focusLost({})", event);
            commitMessageToTntDb();
        }
    }

    private class MessageModifyListener implements ModifyListener {
        @Override
        public void modifyText(ModifyEvent event) {
            msgStr = view.getMessageText().getText();
            if (view != null && !view.isDisposed() && view.getHistory() != null)
                historyId = view.getHistory().getHistoryId(); // Keep our cached copy in case view gets disposed
        }
    }

    private class SubjectFocusListener extends FocusAdapter {
        @Override
        public void focusLost(FocusEvent event) {
            log.trace("SubjectFocusListener.focusLost({})", event);
            commitSubjectToTntDb();
        }
    }

    private class SubjectModifyListener implements ModifyListener {
        @Override
        public void modifyText(ModifyEvent event) {
            subjectStr = view.getSubjectText().getText();
            if (view != null && !view.isDisposed() && view.getHistory() != null)
                historyId = view.getHistory().getHistoryId(); // Keep our cached copy in case view gets disposed
        }
    }

    private class ThankSelectionListener extends SelectionAdapter {
        @Override
        public void widgetSelected(SelectionEvent event) {
            log.trace("ThankSelectionListener.widgetSelected({})", event);
            // Don't need our cached copy since a checkbox selection requires an enabled view
            History history = view.getHistory();
            boolean selection = view.getThankCheckBox().getSelection();
            history.setThank(selection);
            try {
                HistoryManager.updateIsThank(history.getHistoryId(), selection);
            } catch (SQLException e) {
                String msg = "Could not update Thank status";
                Util.reportError(view.getShell(), "Update error", msg, e);
            }
        }
    }

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

        view.getSubjectText().addModifyListener(new SubjectModifyListener());
        view.getMessageText().addModifyListener(new MessageModifyListener());
        view.getSubjectText().addFocusListener(new SubjectFocusListener());
        view.getMessageText().addFocusListener(new MessageFocusListener());
        view.getChallengeCheckBox().addSelectionListener(new ChallengeSelectionListener());
        view.getThankCheckBox().addSelectionListener(new ThankSelectionListener());
        view.getMassMailingCheckBox().addSelectionListener(new MassMailingSelectionListener());
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
            Util.reportError(view.getShell(), "Update error", "Could not update history body", e);
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
            Util.reportError(view.getShell(), "Update error", "Could not update history subject", e);
        }
    }
}
