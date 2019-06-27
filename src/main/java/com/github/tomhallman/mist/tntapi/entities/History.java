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

package com.github.tomhallman.mist.tntapi.entities;

import java.time.LocalDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javamoney.moneta.FastMoney;

import com.github.tomhallman.mist.model.data.MessageSource;
import com.github.tomhallman.mist.tntapi.TntDb;

/**
 * Object representing a row in the Tnt History table.
 */
public class History {
    private static Logger log = LogManager.getLogger();

    // Result types (These values are taken from the Tnt "HistoryResult" table)
    public final static int RESULT_NONE = 0;
    public final static int RESULT_DONE = 1;
    public final static int RESULT_RECEIVED = 2;
    public final static int RESULT_ATTEMPTED = 3;

    // Status types
    public final static int STATUS_NONE = 0;
    public final static int STATUS_CONTACT_NOT_FOUND = 1;
    public final static int STATUS_MULTIPLE_CONTACTS_FOUND = 2;
    public final static int STATUS_ERROR = 3;
    public final static int STATUS_ADDED = 4;
    public final static int STATUS_EXISTS = 5;

    // Database fields
    private Integer historyId = null;
    private LocalDateTime lastEdit = LocalDateTime.now();
    private Integer campaignId = null;
    private int taskTypeId = TaskType.TODO; // default from TntDb
    private String description = ""; // max length 150
    private LocalDateTime historyDate = LocalDateTime.now();
    private String notes = "";
    private Integer loggedByUserId = null;
    private boolean inMpdWeeklyUpdate = false;
    private boolean isChallenge = false;
    private boolean isThank = false;
    private boolean isMassMailing = false;
    private String autoGenCode = ""; // max length 50
    private int historyResultId = RESULT_NONE; // default from TntDb
    private String dataChangeLogAsCsv = "";
    private FastMoney pledgeChangeAmount = FastMoney.of(0, "USD");
    private int pledgeChangeCurrencyId = 0; // default from TntDb
    private FastMoney basePledgeChangeAmount = FastMoney.of(0, "USD");
    private int baseCurrencyId = TntDb.getBaseCurrencyId();

    // Additional fields
    private MessageSource messageSource = new MessageSource();
    private ContactInfo contactInfo = new ContactInfo();
    private int status = STATUS_NONE;
    private Exception statusException = null;

    public History() {
    }

    public History(History tntHistory) {
        log.trace("{{}} TntHistory({})", this, tntHistory);

        // Database fields
        this.historyId = tntHistory.historyId;
        this.lastEdit = tntHistory.lastEdit;
        this.campaignId = tntHistory.campaignId;
        this.taskTypeId = tntHistory.taskTypeId;
        this.description = tntHistory.description;
        this.historyDate = tntHistory.historyDate;
        this.notes = tntHistory.notes;
        this.loggedByUserId = tntHistory.loggedByUserId;
        this.inMpdWeeklyUpdate = tntHistory.inMpdWeeklyUpdate;
        this.isChallenge = tntHistory.isChallenge;
        this.isThank = tntHistory.isThank;
        this.isMassMailing = tntHistory.isMassMailing;
        this.autoGenCode = tntHistory.autoGenCode;
        this.historyResultId = tntHistory.historyResultId;
        this.dataChangeLogAsCsv = tntHistory.dataChangeLogAsCsv;
        this.pledgeChangeAmount = tntHistory.pledgeChangeAmount;
        this.pledgeChangeCurrencyId = tntHistory.pledgeChangeCurrencyId;
        this.basePledgeChangeAmount = tntHistory.basePledgeChangeAmount;
        this.baseCurrencyId = tntHistory.baseCurrencyId;

        // Additional fields
        this.messageSource = tntHistory.messageSource.cloneObject();
        this.contactInfo = new ContactInfo(tntHistory.contactInfo);
        this.status = tntHistory.status;
        this.statusException = new Exception(tntHistory.statusException);
    }

    public String getAutoGenCode() {
        return autoGenCode;
    }

    public int getBaseCurrencyId() {
        return baseCurrencyId;
    }

    public FastMoney getBasePledgeChangeAmount() {
        return basePledgeChangeAmount;
    }

    public Integer getCampaignId() {
        return campaignId;
    }

    public ContactInfo getContactInfo() {
        return contactInfo;
    }

    public String getDataChangeLogAsCsv() {
        return dataChangeLogAsCsv;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getHistoryDate() {
        return historyDate;
    }

    public Integer getHistoryId() {
        return historyId;
    }

    public int getHistoryResultId() {
        return historyResultId;
    }

    public String getHistoryResultString() {
        if (historyResultId == RESULT_NONE)
            return "---";
        switch (historyResultId) {
            case RESULT_DONE:
                return "Done";
            case RESULT_RECEIVED:
                return "Received";
            default:
                return "Unknown";
        }
    }

    public LocalDateTime getLastEdit() {
        return lastEdit;
    }

    public Integer getLoggedByUserId() {
        return loggedByUserId;
    }

    public MessageSource getMessageSource() {
        return messageSource;
    }

    public String getNotes() {
        return notes;
    }

    public FastMoney getPledgeChangeAmount() {
        return pledgeChangeAmount;
    }

    public int getPledgeChangeCurrencyId() {
        return pledgeChangeCurrencyId;
    }

    public int getStatus() {
        return status;
    }

    public Exception getStatusException() {
        return statusException;
    }

    public String getStatusString() {
        if (status == STATUS_NONE)
            return "---";
        switch (status) {
            case STATUS_ADDED:
                return "Added";
            case STATUS_ERROR:
                return "Error";
            case STATUS_EXISTS:
                return "Exists";
            case STATUS_CONTACT_NOT_FOUND:
                return "Contact not found";
            case STATUS_MULTIPLE_CONTACTS_FOUND:
                return "Multiple contacts found";
            default:
                return "Unknown";
        }
    }

    public int getTaskTypeId() {
        return taskTypeId;
    }

    public boolean isChallenge() {
        return isChallenge;
    }

    public boolean isInMpdWeeklyUpdate() {
        return inMpdWeeklyUpdate;
    }

    public boolean isMassMailing() {
        return isMassMailing;
    }

    public boolean isThank() {
        return isThank;
    }

    public void setAutoGenCode(String autoGenCode) {
        this.autoGenCode = autoGenCode;
    }

    public void setBaseCurrencyId(int baseCurrencyId) {
        this.baseCurrencyId = baseCurrencyId;
    }

    public void setBasePledgeChangeAmount(FastMoney basePledgeChangeAmount) {
        this.basePledgeChangeAmount = basePledgeChangeAmount;
    }

    public void setCampaignId(Integer campaignId) {
        this.campaignId = campaignId;
    }

    public void setChallenge(boolean isChallenge) {
        this.isChallenge = isChallenge;
    }

    public void setContactInfo(ContactInfo contactInfo) {
        this.contactInfo = new ContactInfo(contactInfo);
    }

    public void setDataChangeLogAsCsv(String dataChangeLogAsCsv) {
        this.dataChangeLogAsCsv = dataChangeLogAsCsv;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setHistoryDate(LocalDateTime historyDate) {
        this.historyDate = historyDate;
    }

    public void setHistoryId(Integer historyId) {
        this.historyId = historyId;
    }

    public void setHistoryResultId(int historyResultId) {
        this.historyResultId = historyResultId;
    }

    public void setInMpdWeeklyUpdate(boolean inMpdWeeklyUpdate) {
        this.inMpdWeeklyUpdate = inMpdWeeklyUpdate;
    }

    public void setLastEdit(LocalDateTime lastEdit) {
        this.lastEdit = lastEdit;
    }

    public void setLoggedByUserId(Integer loggedByUserId) {
        this.loggedByUserId = loggedByUserId;
    }

    public void setMassMailing(boolean isMassMailing) {
        this.isMassMailing = isMassMailing;
    }

    public void setMessageSource(MessageSource source) {
        this.messageSource = source;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setPledgeChangeAmount(FastMoney pledgeChangeAmount) {
        this.pledgeChangeAmount = pledgeChangeAmount;
    }

    public void setPledgeChangeCurrencyId(int pledgeChangeCurrencyId) {
        this.pledgeChangeCurrencyId = pledgeChangeCurrencyId;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setStatusException(Exception statusException) {
        this.statusException = statusException;
    }

    public void setTaskTypeId(int taskTypeId) {
        this.taskTypeId = taskTypeId;
    }

    public void setThank(boolean isThank) {
        this.isThank = isThank;
    }

    @Override
    public String toString() {
        return String.format("%s:'%s':%s", historyId, description, historyDate);
    }

}
