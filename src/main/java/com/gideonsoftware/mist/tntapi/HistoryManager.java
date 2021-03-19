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

package com.gideonsoftware.mist.tntapi;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gideonsoftware.mist.exceptions.TntDbException;
import com.gideonsoftware.mist.tntapi.entities.History;
import com.gideonsoftware.mist.tntapi.entities.TaskType;

/**
 *
 */
public class HistoryManager {
    private static Logger log = LogManager.getLogger();

    private HistoryManager() {
    }

    /**
     * Adds existing Tnt history data (specified by {@code historyId} to the specified {@code history}.
     * 
     * @param history
     *            The history to add existing Tnt history data to; null returns false
     * @param historyId
     *            The history ID; null returns false
     * @return True if the history data was successfully added, false if not
     */
    private static boolean addTntDataToHistory(History history, Integer historyId) throws SQLException {
        log.trace("addTntDataToHistory({},{})", history, historyId);

        if (history == null || historyId == null)
            return false;

        String query = "SELECT * FROM [History] JOIN [HistoryContact] "
            + "ON ([History].[HistoryID] = [HistoryContact].[HistoryID]) "
            + "WHERE [HistoryID] = ?";
        PreparedStatement stmt = TntDb.getConnection().prepareStatement(
            query,
            ResultSet.TYPE_SCROLL_INSENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
        stmt.setInt(1, historyId);
        ResultSet rs = stmt.executeQuery();

        if (!rs.first())
            return false;

        history.setHistoryId(rs.getInt("HistoryID"));
        history.setLastEdit(TntDb.timestampToDate(rs.getTimestamp("LastEdit")));
        history.setCampaignId(rs.getInt("CampaignID"));
        history.setTaskTypeId(rs.getInt("TaskTypeID"));
        history.setDescription(rs.getString("Description")); // X = UCanAccess fix for reserved word (TODO: still?)
        history.setHistoryDate(TntDb.timestampToDate(rs.getTimestamp("HistoryDate")));
        history.setNotes(rs.getString("Notes"));
        history.setHistoryResultId(rs.getInt("HistoryResultID"));
        history.setLoggedByUserId(rs.getInt("LoggedByUserID"));
        history.setInMpdWeeklyUpdate(rs.getBoolean("InMPDWeeklyUpdate"));
        history.setChallenge(rs.getBoolean("IsChallenge"));
        history.setThank(rs.getBoolean("IsThank"));
        history.setMassMailing(rs.getBoolean("IsMassMailing"));
        history.setAutoGenCode(rs.getString("AutoGenCode"));
        history.setDataChangeLogAsCsv(rs.getString("DataChangeLogAsCsv"));
        history.setPledgeChangeAmount(TntDb.floatToMoney(rs.getFloat("PledgeChangeAmount")));
        history.setPledgeChangeCurrencyId(rs.getInt("PledgeChangeCurrencyID"));
        history.setBasePledgeChangeAmount(TntDb.floatToMoney(rs.getFloat("BasePledgeChangeAmount")));
        history.setBaseCurrencyId(rs.getInt("BaseCurrencyID"));
        history.getContactInfo().setId(rs.getInt("ContactID")); // Contact name and email not set
        return true;
    }

    /**
     * Creates a new history in the Tnt database.
     * <p>
     * If the history was previously created (same contact ID, date and history result), it will not be created again.
     * <p>
     * Commits changes to the Tnt database if {@code useCommit} is true.
     *
     * @param history
     *            The history to create
     * @throws TntDbException
     *             if history is null,
     *             if the history's date is null,
     *             if the history's contact ID is null
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void create(History history) throws TntDbException, SQLException {
        log.trace("create({})", history);
        log.debug("Preparing to add history to TntConnect: {}", history);

        if (history == null)
            throw new TntDbException("History not supplied");
        if (history.getHistoryDate() == null)
            throw new TntDbException("Date is not supplied for history");
        if (history.getContactInfo().getId() == null)
            throw new TntDbException("Contact ID is not supplied for history");
        if (history.getDescription() == null)
            history.setDescription("");
        if (history.getHistoryResultId() == History.RESULT_NONE) {
            log.warn("History result was not supplied; assuming RESULT_DONE");
            history.setHistoryResultId(History.RESULT_DONE);
        } else if (History.RESULT_DONE != history.getHistoryResultId()
            && History.RESULT_RECEIVED != history.getHistoryResultId())
            log.warn("Unexpected history result: {}", history.getHistoryResultId());

        LocalDateTime historyDate = history.getHistoryDate();

        // Check for duplicate data (don't insert this twice!)
        // Two history records with the same contact, date and result are considered identical.
        // (That way you can update description, notes, etc. without creating duplicates)
        History existingHistory = get(
            history.getContactInfo().getId(),
            history.getTaskTypeId(),
            historyDate,
            history.getHistoryResultId());
        if (existingHistory != null) {
            log.debug("History already exists in TntConnect.");
            history.setStatus(History.STATUS_EXISTS);
            // Replace values in history with values from TntConnect (for editing in MIST)
            addTntDataToHistory(history, existingHistory.getHistoryId());
            return;
        }

        history.setHistoryId(TntDb.getAvailableId(TntDb.TABLE_HISTORY));
        LocalDateTime now = LocalDateTime.now().withNano(0);

        // History
        Object[][] historyColValuePairs = {
            { "HistoryID", history.getHistoryId(), java.sql.Types.INTEGER },
            { "LastEdit", history.getLastEdit(), java.sql.Types.TIMESTAMP },
            { "CampaignID", history.getCampaignId(), java.sql.Types.INTEGER },
            { "TaskTypeID", history.getTaskTypeId(), java.sql.Types.INTEGER },
            { "Description", history.getDescription(), java.sql.Types.VARCHAR, 150 },
            { "HistoryDate", historyDate, java.sql.Types.TIMESTAMP },
            { "Notes", history.getNotes(), java.sql.Types.VARCHAR },
            { "LoggedByUserID", history.getLoggedByUserId(), java.sql.Types.INTEGER },
            { "InMPDWeeklyUpdate", history.isInMpdWeeklyUpdate(), java.sql.Types.BOOLEAN },
            { "IsChallenge", history.isChallenge(), java.sql.Types.BOOLEAN },
            { "IsThank", history.isThank(), java.sql.Types.BOOLEAN },
            { "IsMassMailing", history.isMassMailing(), java.sql.Types.BOOLEAN },
            { "AutoGenCode", history.getAutoGenCode(), java.sql.Types.VARCHAR, 50 },
            { "HistoryResultID", history.getHistoryResultId(), java.sql.Types.INTEGER },
            { "DataChangeLogAsCsv", history.getDataChangeLogAsCsv(), java.sql.Types.VARCHAR },
            { "PledgeChangeAmount", TntDb.formatDbCurrency(history.getPledgeChangeAmount()), java.sql.Types.VARCHAR },
            { "PledgeChangeCurrencyID", history.getPledgeChangeCurrencyId(), java.sql.Types.INTEGER },
            {
                "BasePledgeChangeAmount",
                TntDb.formatDbCurrency(history.getBasePledgeChangeAmount()),
                java.sql.Types.VARCHAR },
            { "BaseCurrencyID", history.getBaseCurrencyId(), java.sql.Types.INTEGER } };

        // HistoryContact
        Object[][] historyContactColValuePairs = {
            { "HistoryContactID", TntDb.getAvailableId(TntDb.TABLE_HISTORYCONTACT), java.sql.Types.INTEGER },
            { "LastEdit", now, java.sql.Types.TIMESTAMP },
            { "HistoryID", history.getHistoryId(), java.sql.Types.INTEGER },
            { "ContactID", history.getContactInfo().getId(), java.sql.Types.INTEGER } };

        // Run queries
        try {
            TntDb.insert(TntDb.TABLE_HISTORY, historyColValuePairs);
            TntDb.insert(TntDb.TABLE_HISTORYCONTACT, historyContactColValuePairs);

            switch (history.getTaskTypeId()) {
                case TaskType.APPOINTMENT:
                case TaskType.UNSCHEDULED_VISIT:
                    ContactManager.updateLastVisitDate(history.getContactInfo().getId(), historyDate);
                    break;
                case TaskType.REMINDER_LETTER:
                case TaskType.SUPPORT_LETTER:
                case TaskType.LETTER:
                case TaskType.NEWSLETTER:
                case TaskType.E_NEWSLETTER:
                case TaskType.PRE_CALL_LETTER:
                case TaskType.EMAIL:
                case TaskType.FACEBOOK:
                case TaskType.TEXT_SMS:
                    ContactManager.updateLastLetterDate(history.getContactInfo().getId(), historyDate);
                    break;
                default:
                    log.error("Unknown task type: " + history.getTaskTypeId());
            }

            if (history.isChallenge())
                ContactManager.updateLastChallengeDate(history.getContactInfo().getId(), historyDate);
            if (history.isThank())
                ContactManager.updateLastThankDate(history.getContactInfo().getId(), historyDate);
            TntDb.commit();
        } catch (SQLException e) {
            TntDb.rollback();
            throw e;
        }

        history.setStatus(History.STATUS_ADDED);
    }

    /**
     * Returns the history associated with the specified contact ID, task type, date and history result - or null if
     * none exists.
     * <p>
     * Note that the contact name and email will not be returned.
     *
     * @param contactId
     *            the history's contact ID
     * @param taskType
     *            the history's task type
     * @param date
     *            the history's date
     * @param result
     *            the history's result
     * @return the history associated with the specified information or null if none exists
     * @throws SQLException
     *             if there is a database access problem
     */
    public static History get(int contactId, int taskType, LocalDateTime date, int result) throws SQLException {
        log.trace("get({},{},{},{})", contactId, taskType, date, result);

        String query = "SELECT [History].[HistoryID] FROM [History], [HistoryContact] WHERE "
            + "[HistoryContact].[ContactID] = ? AND "
            + "[History].[TaskTypeID] = ? AND "
            + "[History].[HistoryDate] = ? AND "
            + "[History].[HistoryResultID] = ? AND "
            + "[HistoryContact].[HistoryID] = [History].[HistoryID]";

        PreparedStatement stmt = TntDb.getConnection().prepareStatement(
            query,
            ResultSet.TYPE_SCROLL_INSENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
        stmt.setInt(1, contactId);
        stmt.setInt(2, taskType);
        stmt.setObject(3, date);
        stmt.setInt(4, result);
        ResultSet rs = stmt.executeQuery();

        // Log result if tracing
        if (log.isTraceEnabled())
            log.trace(TntDb.getResultSetString(rs));

        if (rs.first())
            return get(rs.getInt("HistoryID"));
        else
            return null;
    }

    /**
     * Returns the history associated with the specified history ID or null if none exists.
     *
     * @param historyId
     *            the history ID; null returns null
     * @return the history associated with the specified history ID or null if none exists
     * @throws SQLException
     *             if there is a database access problem
     */
    public static History get(Integer historyId) throws SQLException {
        log.trace("get({})", historyId);

        History history = new History();
        if (addTntDataToHistory(history, historyId))
            return history;
        else
            return null;
    }

    /**
     * Gets the last edit date for the specified history item.
     *
     * @param historyId
     *            the history ID for which to get the last edit date
     * @return
     *         the last edit date or null if one doesn't exist (which shouldn't happen)
     * @throws SQLException
     *             if there is a database access problem
     */
    public static LocalDateTime getLastEditDate(int historyId) throws SQLException {
        log.trace("getLastEditDate({})", historyId);

        try {
            return TntDb.getOneDate("SELECT [LastEdit] FROM [History] WHERE [HistoryId] = ?", historyId);
        } catch (TntDbException e) {
            log.error(e); // Nothing useful can be done in this case
            return null;
        }
    }

    /**
     * Updates the specified history's description.
     * <p>
     * Commits changes to the Tnt database if {@code useCommit} is true.
     *
     * @param historyId
     *            the ID of the history to update
     * @param description
     *            the new description
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void updateDescription(int historyId, String description) throws SQLException {
        log.trace("updateDescription({},{})", historyId, description);

        String query = "UPDATE [History] SET [Description] = ? WHERE [HistoryId] = ?";
        PreparedStatement stmt = TntDb.getConnection().prepareStatement(query);
        stmt.setString(1, description);
        stmt.setInt(2, historyId);
        stmt.executeUpdate();

        updateLastEdit(historyId);

        TntDb.commit();
    }

    /**
     * Updates the specified history's "IsChallenge" value.
     * <p>
     * Commits changes to the Tnt database if {@code useCommit} is true.
     *
     * @param historyId
     *            the ID of the history to update
     * @param isChallenge
     *            the isChallenge value
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void updateIsChallenge(int historyId, boolean isChallenge) throws SQLException {
        log.trace("updateIsChallenge({},{})", historyId, isChallenge);

        String query = "UPDATE [History] SET [IsChallenge] = ? WHERE [HistoryId] = ?";
        PreparedStatement stmt = TntDb.getConnection().prepareStatement(query);
        stmt.setBoolean(1, isChallenge);
        stmt.setInt(2, historyId);
        stmt.executeUpdate();

        updateLastEdit(historyId);

        // Also update Contact's LastChallenge if needed
        History history = get(historyId);
        int contactId = history.getContactInfo().getId();
        ContactManager.updateLastChallengeDate(contactId, isChallenge ? history.getHistoryDate() : null);

        // And recalculate challenges since last gift
        ContactManager.recalculateChallengesSinceLastGift(contactId);

        TntDb.commit();
    }

    /**
     * Updates the specified history's "IsMassMailing" value.
     * <p>
     * Commits changes to the Tnt database if {@code useCommit} is true.
     *
     * @param historyId
     *            the ID of the history to update
     * @param isMassMailing
     *            the isMassMailing value
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void updateIsMassMailing(int historyId, boolean isMassMailing) throws SQLException {
        log.trace("updateIsMassMailing({},{})", historyId, isMassMailing);

        String query = "UPDATE [History] SET [IsMassMailing] = ? WHERE [HistoryId] = ?";
        PreparedStatement stmt = TntDb.getConnection().prepareStatement(query);
        stmt.setBoolean(1, isMassMailing);
        stmt.setInt(2, historyId);
        stmt.executeUpdate();

        updateLastEdit(historyId);

        // Mass Mailing affects a lot of history data, so recalculate
        History history = get(historyId);
        int contactId = history.getContactInfo().getId();
        ContactManager.recalculateHistoryData(contactId);

        TntDb.commit();
    }

    /**
     * Updates the specified history's "IsThank" value.
     * <p>
     * Commits changes to the Tnt database if {@code useCommit} is true.
     *
     * @param historyId
     *            the ID of the history to update
     * @param isThank
     *            the isThank value
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void updateIsThank(int historyId, boolean isThank) throws SQLException {
        log.trace("updateIsThank({},{})", historyId, isThank);

        String query = "UPDATE [History] SET [IsThank] = ? WHERE [HistoryId] = ?";
        PreparedStatement stmt = TntDb.getConnection().prepareStatement(query);
        stmt.setBoolean(1, isThank);
        stmt.setInt(2, historyId);
        stmt.executeUpdate();

        updateLastEdit(historyId);

        // Also update Contact's LastThank if needed
        History history = get(historyId);
        int contactId = history.getContactInfo().getId();
        ContactManager.updateLastThankDate(contactId, isThank ? history.getHistoryDate() : null);

        TntDb.commit();
    }

    /**
     * Updates the specified history's "LastEdit" value to be "now".
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param historyId
     *            the ID of the contact to update
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void updateLastEdit(int historyId) throws SQLException {
        log.trace("updateLastEdit({})", historyId);
        TntDb.updateTableLastEdit(TntDb.TABLE_HISTORY, historyId);
    }

    /**
     * Updates the specified history's notes.
     * <p>
     * Commits changes to the Tnt database if {@code useCommit} is true.
     *
     * @param historyId
     *            the ID of the history to update
     * @param notes
     *            the new notes
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void updateNotes(int historyId, String notes) throws SQLException {
        log.trace("updateNotes({},{})", historyId, notes);

        String query = "UPDATE [History] SET [Notes] = ? WHERE [HistoryId] = ?";
        PreparedStatement stmt = TntDb.getConnection().prepareStatement(query);
        stmt.setString(1, notes);
        stmt.setInt(2, historyId);
        stmt.executeUpdate();

        updateLastEdit(historyId);

        TntDb.commit();
    }

}
