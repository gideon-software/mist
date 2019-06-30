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

package com.github.tomhallman.mist.tntapi;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import javax.money.MonetaryAmount;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.javamoney.moneta.FastMoney;

import com.github.tomhallman.mist.EmailMessageToHistoryConverter;
import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.exceptions.TntDbException;
import com.github.tomhallman.mist.model.HistoryModel;
import com.github.tomhallman.mist.model.MessageModel;
import com.github.tomhallman.mist.model.data.EmailMessage;
import com.github.tomhallman.mist.model.data.MessageSource;
import com.github.tomhallman.mist.tntapi.entities.History;
import com.github.tomhallman.mist.util.DBTablePrinter;
import com.github.tomhallman.mist.util.Util;

/**
 * The TntConnect database.
 */
public class TntDb {
    private static Logger log = LogManager.getLogger();

    // Generic types
    public final static String TYPE_DATE = "Date";
    public final static String TYPE_INT = "int";
    public final static String TYPE_STRING = "String";

    // Table names
    public final static String TABLE_CONTACT = "Contact";
    public final static String TABLE_CURRENCY = "Currency";
    public final static String TABLE_HISTORY = "History";
    public final static String TABLE_HISTORYCONTACT = "HistoryContact";
    public final static String TABLE_MPDPHASE = "MPDPhase";
    public final static String TABLE_PLEDGEFREQUENCY = "PledgeFrequency";
    public final static String TABLE_TASKTYPE = "TaskType";

    // Preferences
    public final static String PREF_TNT_DBPATH = "tnt.db.path";

    // Property change values
    private final static PropertyChangeSupport pcs = new PropertyChangeSupport(TntDb.class);
    public final static String PROP_IMPORTSTATUS_IMPORTING = "tntdb.importstatus.importing";
    public final static String PROP_IMPORTSTATUS_STOPPED = "tntdb.importstatus.stopped";
    public final static String PROP_IMPORTSTATUS_MESSAGE_PROCESSED = "tntdb.importstatus.message.processed";

    // Import controls
    private static boolean stopImporting = false;

    // Other objects
    private static String dbPath = MIST.getPrefs().getString(PREF_TNT_DBPATH);
    private static Connection conn = null;
    private static boolean useCommit = true;

    /**
     * No instantiation allowed!
     */
    private TntDb() {
    }

    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        log.trace("addPropertyChangeListener({})", listener);
        pcs.addPropertyChangeListener(listener);
    }

    /**
     * Commits changes to the Tnt database if useCommit is true.
     *
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void commit() throws SQLException {
        log.trace("commit() -- useCommit is {}", useCommit);
        if (isUseCommit())
            conn.commit();
    }

    /**
     * Connects to the TntConnect database at a previously-specified path.
     * <p>
     * If a connection already exists, that connection is used.
     *
     * @throws TntDbException
     *             if the database path has not been specified previously,
     *             if the TntConnect database cannot be found,
     *             if the UCanAccess JDBC driver cannot be loaded,
     *             if there is a database access problem
     */
    public static void connect() throws TntDbException {
        log.trace("connect()");
        connect(false);
    }

    /**
     * Connects to the TntConnect database at a previously-specified path.
     *
     * @param force
     *            true if a reconnection should be forced; false if an existing connection will do
     * @throws TntDbException
     *             if the database path has not been specified previously,
     *             if the TntConnect database cannot be found,
     *             if the UCanAccess JDBC driver cannot be loaded,
     *             if there is a database access problem
     */
    public static void connect(boolean force) throws TntDbException {
        log.trace("connect({})", force);
        connect(force, dbPath);
    }

    /**
     * Connects to the TntConnect database the specified path.
     *
     * @param force
     *            true if a reconnection should be forced; false if an existing connection will do;
     *            If {@code dbPath} is different from the current connection, a reconnection is always forced
     * @param databasePath
     *            path to the TntConnect database to connect to; if null or blank, the previous connection is used.
     * @throws TntDbException
     *             if {@code databasePath} is null or blank and there is no previous connection,
     *             if the TntConnect database cannot be found at {@code databasePath},
     *             if the UCanAccess JDBC driver cannot be loaded,
     *             if there is a database access problem
     */
    public static void connect(boolean force, String databasePath) throws TntDbException {
        log.trace("connect({},{})", force, databasePath);

        if (databasePath == null || databasePath.isEmpty())
            throw new TntDbException("Tnt database path not specified.");

        // If we're already connected AND we're not changing DB paths, we're done (unless forced)
        if (!force && isConnected() && databasePath.equals(dbPath)) {
            log.trace("  already connected");
            return;
        }

        try {
            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
        } catch (ClassNotFoundException e) {
            log.fatal("Unable to load JDBC driver", e);
            throw new TntDbException("Unable to load JDBC driver", e);
        }

        log.debug("Verifying that Tnt database exists at '{}'", databasePath);
        if (!new File(databasePath).exists()) {
            String errStr = String.format("Could not find Tnt database at '%s'", databasePath);
            throw new TntDbException(errStr);
        }

        log.debug("Connecting to Tnt database at '{}'", databasePath);
        String url = String.format(
            "jdbc:ucanaccess://%s;immediatelyReleaseResources=true;openExclusive=true;",
            databasePath);

        try {
            conn = DriverManager.getConnection(url, null, "tntMPD");
            conn.setAutoCommit(false); // We'll commit our own transactions, thank you =)
            dbPath = databasePath;
        } catch (SQLException e) {
            disconnect();
            throw new TntDbException(String.format("Could not connect to Tnt database at '%s'", databasePath), e);
        }

        try {
            // Note: we load thse into memory rather than making DB calls simply for efficiency
            CurrencyManager.load();
            PledgeFrequencyManager.load();
        } catch (SQLException e) {
            throw new TntDbException("Could not load initialization data from Tnt database", e);
        }
    }

    /**
     * TODO
     *
     * @param table
     * @param colValuePairs
     */
    public static String createInsertQuery(String table, String[][] colValuePairs) {
        log.trace("createInsertQuery({},{})", table, "<" + colValuePairs.length + " pairs>");
        StringBuilder colNames = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < colValuePairs.length; i++) {
            colNames.append((i > 0 ? "," : "") + "[" + colValuePairs[i][0] + "]");
            values.append((i > 0 ? "," : "") + colValuePairs[i][1]);
        }
        return String.format("INSERT INTO [%s] (%s) VALUES (%s)", table, colNames, values);
    }

    /**
     * Disconnects from the TntConnect database if a connection exists.
     */
    public static void disconnect() {
        log.trace("disconnect()");
        try {
            if (conn != null) {
                log.debug("Disconnecting from Tnt database...");
                conn.close();
                conn = null;
            }
        } catch (SQLException e) {
            log.warn(e);
        }
    }

    /**
     * Converts {@link float} to {@link org.javamoney.moneta.FastMoney}
     *
     * @param f
     *            the float to convert
     * @return the float in FastMoney format
     */
    public static FastMoney floatToMoney(float f) {
        return FastMoney.of(f, "USD");
    }

    /**
     * Return the specified boolean in the format that TntConnect uses (-1 = true, 0 = false).
     *
     * @param bool
     *            the boolean value to convert
     * @return the specified boolean in the format that TntConnect uses (-1 = true, 0 = false)
     */
    public static String formatDbBoolean(boolean bool) {
        return bool ? "-1" : "0";
    }

    /**
     * Return the specified money in the format that TntConnect uses.
     *
     * @param money
     *            the money value to convert
     * @return the specified money in the format that TntConnect uses
     */
    public static String formatDbCurrency(MonetaryAmount money) {
        if (money == null)
            return "NULL";
        return money.getNumber().toString();
    }

    /**
     * Return the specified date in the format that TntConnect uses, including the time.
     *
     * @param date
     *            the date value to convert
     * @return the specified date in the format that TntConnect uses, including the time
     */
    public static String formatDbDate(LocalDateTime date) {
        // Tnt DB date format: #2010-08-18 21:58:14#
        if (date == null)
            return "NULL";
        LocalDateTime dateNoNano = date.withNano(0);
        return String.format(
            "#%s#",
            dateNoNano.format(DateTimeFormatter.ISO_LOCAL_DATE)
                + " "
                + dateNoNano.format(DateTimeFormatter.ISO_LOCAL_TIME));
    }

    /**
     * Return the specified date in the format that TntConnect uses, without the time.
     *
     * @param date
     *            the date value to convert
     * @return the specified date in the format that TntConnect uses, without the time
     */
    public static String formatDbDateNoTime(LocalDateTime date) {
        if (date == null)
            return "NULL";
        return String.format("#%s#", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    /**
     * TODO
     * 
     * @param integer
     * @return
     */
    public static String formatDbInt(Integer integer) {
        if (integer == null)
            return "NULL";
        return String.valueOf(integer);
    }

    /**
     * Return the specified string in the format that TntConnect uses.
     *
     * @param str
     *            the string value to convert
     * @return the specified string in the format that TntConnect uses
     */
    public static String formatDbString(String string) {
        if (string == null)
            return "NULL";
        return String.format("'%s'", string.replace("'", "''"));
    }

    /**
     * Return the specified string in the format that TntConnect uses, trimming it down to {@code maxLen} if necessary.
     *
     * @param string
     *            the string value to convert
     * @param maxLen
     *            the maximum string length
     * @return the specified string in the format that TntConnect uses, trimmed down to {@code maxLen} if necessary
     */
    public static String formatDbString(String string, int maxLen) {
        if (string == null)
            return "NULL";
        String newStr = string.replace("'", "''");
        if (newStr.length() > maxLen)
            newStr = newStr.substring(0, maxLen);
        return String.format("'%s'", newStr);
    }

    /**
     * Gets an available ID for the specified table. Useful for creating foreign key IDs.
     * <p>
     * IDs can be negative or positive.
     *
     * @param tableName
     *            the name of the table
     * @return
     *         an available ID for the specified table
     * @throws SQLException
     *             if there is a database access problem
     */
    public static int getAvailableId(String tableName) throws SQLException {
        return getAvailableId(tableName, false);
    }

    /**
     * Gets an available ID for the specified table. Useful for creating foreign key IDs.
     * <p>
     * IDs can be specified as only positive or either positive or negative.
     *
     * @param tableName
     *            the name of the table
     * @param onlyPositive
     *            true if only positive IDs should be considered; false if IDs can be negative too
     * @return
     *         an available ID for the specified table
     * @throws SQLException
     *             if there is a database access problem
     */
    public static int getAvailableId(String tableName, boolean onlyPositive) throws SQLException {
        log.trace("getAvailableId({}.{})", tableName, onlyPositive);

        // Tnt IDs are generated randomly; find an available one
        Random generator = new Random();
        int id;
        ResultSet rs;
        do {
            // Get a random long number
            id = onlyPositive ? generator.nextInt(Integer.MAX_VALUE) : generator.nextInt();
            // Try to find it in the DB table
            rs = runQuery(String.format("SELECT * FROM [%s] WHERE [%sID] = %s", tableName, tableName, id));
        } while (rs.next()); // If there are any results, try again (cause it's a duplicate)
        return id;
    }

    /**
     * Returns the current connection (or null if none exists)
     *
     * @return the current connection (or null if none exists)
     */
    protected static Connection getConnection() {
        return conn;
    }

    /**
     * Returns the Description field of the specified table with the specified ID.
     *
     * @param table
     *            the table from which to get the description
     * @param id
     *            the id from which to get the description
     * @return the Description field of the specified table with the specified ID
     * @throws TntDbException
     *             if there is more than one value returned
     * @throws SQLException
     *             if there is a database access problem
     */
    protected static String getDescription(String table, int id) throws TntDbException, SQLException {
        log.trace("getDescription({},{})", table, id);
        String query = String.format("SELECT [Description] FROM [%1$s] WHERE [%1$sId] = %2$s", table, id);
        return getOneString(query);
    }

    /**
     * Returns the description for the specified ID in the MPDPhase table.
     *
     * @param id
     *            the id from which to get the description
     * @return the description for the specified ID in the MPDPhase table
     * @throws TntDbException
     *             if there is more than one value returned
     * @throws SQLException
     *             if there is a database access problem
     */
    public static String getMpdPhaseDescription(int id) throws TntDbException, SQLException {
        return getDescription(TABLE_MPDPHASE, id);
    }

    /**
     * Returns one {@code Date} as a result of running the specified query.
     *
     * @param query
     *            the query to run
     * @return one {@code Date} as a result of running the specified query; null if no value exists
     * @throws TntDbException
     *             if there is more than one value returned
     * @throws SQLException
     *             if there is a database access problem
     */
    public static LocalDateTime getOneDate(String query) throws TntDbException, SQLException {
        log.trace("getOneDate({})", query);
        return (LocalDateTime) getOneX(query, TYPE_DATE);
    }

    /**
     * Returns one {@code Integer} as a result of running the specified query.
     *
     * @param query
     *            the query to run
     * @return one {@code Integer} as a result of running the specified query; null if no value exists.
     * @throws TntDbException
     *             if there is more than one value returned
     * @throws SQLException
     *             if there is a database access problem
     */
    public static Integer getOneInt(String query) throws TntDbException, SQLException {
        log.trace("getOneInt({})", query);
        return (Integer) getOneX(query, TYPE_INT);
    }

    /**
     * Returns one {@code String} as a result of running the specified query.
     *
     * @param query
     *            the query to run
     * @return one {@code String} as a result of running the specified query; null if no value exists.
     * @throws TntDbException
     *             if there is more than one value returned
     * @throws SQLException
     *             if there is a database access problem
     */
    public static String getOneString(String query) throws TntDbException, SQLException {
        log.trace("getOneString({})", query);
        return (String) getOneX(query, TYPE_STRING);
    }

    /**
     * Returns one value of type {@code type} as a result of running the specified query, or null if no value exists.
     *
     * @param query
     *            the query to run
     * @param type
     *            the type to return (which must be cast from {@code Object})
     * @return one value of type {@code type} as a result of running the specified; null if no value exists
     * @throws TntDbException
     *             if there is more than one value returned,
     *             if the specified type is invalid
     * @throws SQLException
     *             if there is a database access problem
     */
    protected static Object getOneX(String query, String type) throws TntDbException, SQLException {
        log.trace("getOneX({},{})", query, type);

        ResultSet rs = runQuery(query);
        // Make sure we only have one result
        int count = getRowCount(rs);
        if (count == 0)
            return null;
        else if (count == 1) {
            rs.next();
            switch (type) {
                case TYPE_DATE:
                    return timestampToDate(rs.getTimestamp(1));
                case TYPE_STRING:
                    return rs.getString(1);
                case TYPE_INT:
                    return rs.getInt(1);
                default:
                    throw new TntDbException(String.format("Unknown type '%s'"));
            }
        } else
            throw new TntDbException(String.format("Expected to find exactly 1 result but found %s", count));
    }

    /**
     * Returns a {@code String} representing the specified {@link ResultSet}.
     *
     * @param rs
     *            the result set
     * @return a {@code String} representing the specified {@link ResultSet}, or {@literal "ERROR"} if there is a
     *         problem
     */
    public static String getResultSetString(ResultSet rs) {
        // Purposefully no trace statement here

        try {
            // Save ResultSet state
            int prev = rs.getRow();

            String rsStr = DBTablePrinter.getFormattedResultSet(rs);

            // Restore ResultSet state
            if (prev == 0)
                rs.beforeFirst();
            else
                rs.absolute(prev);

            return rsStr;
        } catch (SQLException e) {
            log.error(e);
            return "ERROR";
        }
    }

    /**
     * Returns the number of rows in the specified {@link ResultSet}.
     *
     * @param rs
     *            the result set
     * @return the number of rows in the specified {@link ResultSet}
     * @throws SQLException
     *             if there is a database access problem
     */
    public static int getRowCount(ResultSet rs) throws SQLException {
        // Purposefully no trace statement here

        int size = 0;
        if (rs != null) {
            // Store previous location
            int prev = rs.getRow();
            // Get size of result set
            rs.last();
            size = rs.getRow();
            // Keep in valid range
            if (size == -1)
                size = 0;
            // Restore previous location
            if (prev == 0)
                rs.beforeFirst();
            else
                rs.absolute(prev);
        }
        return size;
    }

    /**
     * Returns the description for the specified ID in the TaskType table.
     *
     * @param id
     *            the id from which to get the description
     * @return the description for the specified ID in the TaskType table
     * @throws TntDbException
     *             if there is more than one value returned
     * @throws SQLException
     *             if there is a database access problem
     */
    public static String getTaskTypeDescription(int id) throws TntDbException, SQLException {
        return getDescription(TABLE_TASKTYPE, id);
    }

    /**
     * Returns the current TntConnect database path.
     *
     * @return the current TntConnect database path
     */
    public static String getTntDatabasePath() {
        return dbPath;
    }

    public static void init() {
        log.trace("init()");
        disconnect();
        dbPath = MIST.getPrefs().getString(PREF_TNT_DBPATH);
    }

    /**
     * Returns whether a connection currently exists to the TntConnect database.
     *
     * @return true if a connection exists; false otherwise
     */
    public static boolean isConnected() {
        return conn != null;
    }

    /**
     * Returns whether to commit transactions after add/update/delete methods.
     *
     * @return whether to commit transactions after add/update/delete methods
     */
    public static boolean isUseCommit() {
        return useCommit;
    }

    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        log.trace("removePropertyChangeListener({})", listener);
        pcs.removePropertyChangeListener(listener);
    }

    /**
     * Rolls back uncommitted changes in the TntConnect database
     *
     * @throws TntDbException
     *             if a connection to the database does not exist
     *             if there is a database access problem
     */
    public static void rollback() throws TntDbException {
        log.trace("rollback()");

        if (conn == null)
            throw new TntDbException("No database connection available.");

        try {
            log.debug("Rolling back Tnt database...");
            conn.rollback();
        } catch (SQLException e) {
            throw new TntDbException("Unable to roll back failed transaction. Data corruption may have occured.", e);
        }
    }

    /**
     * Runs the specified query on the TntConnect database.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param query
     *            the query to run
     * @return a {@link ResultSet} in the query was a {@code SELECT} statement, otherwise null
     * @throws SQLException
     *             if there is a database access problem
     */
    public static ResultSet runQuery(String query) throws SQLException {
        log.trace("runQuery({})", query);

        // Create statement that we can scroll over (important for getting size of resultsets)
        PreparedStatement stmt = conn.prepareStatement(
            query,
            ResultSet.TYPE_SCROLL_INSENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
        ResultSet rs = null;
        if (query.startsWith("SELECT")) {
            rs = stmt.executeQuery();
            log.trace(getResultSetString(rs));
        } else {
            stmt.executeUpdate();
        }
        return rs;
    }

    /**
     * Set the Tnt database path.
     *
     * @param databasePath
     *            the Tnt database path
     */
    public static void setTntDatabasePath(String databasePath) {
        log.trace("setTntDatabasePath({})", databasePath);
        dbPath = databasePath;
    }

    /**
     * Set whether to commit transactions after add/update/delete methods.
     *
     * @param useCommit
     *            whether to commit transactions after add/update/delete methods
     */
    public static void setUseCommit(boolean commit) {
        log.trace("setUseCommit({})", commit);
        useCommit = commit;
    }

    /**
     * Starts the TntConnect import service.
     * 
     * @param shell
     *            the shell for notifying the user of the connection taking place; if null, no notification will take
     *            place
     */
    public static void startImportService(Shell shell) {
        log.trace("startImportService({})", shell);

        Util.connectToTntDatabase(shell);

        Thread importThread = new Thread() {

            public void importMessage(MessageSource messageSource) {
                log.trace("importMessage({})", messageSource);

                // Converts message into one or more history objects
                History[] historyArr = EmailMessageToHistoryConverter.getHistory((EmailMessage) messageSource);

                if (historyArr == null) // No history to add
                    return;

                // Add the history into Tnt
                for (History history : historyArr) {

                    // If status is still unset
                    if (history.getStatus() == History.STATUS_NONE) {
                        try {
                            HistoryManager.create(history);
                        } catch (TntDbException | SQLException e) {
                            history.setStatus(History.STATUS_ERROR);
                            history.setStatusException(e);
                        }
                    }

                    if (history.getStatus() != History.STATUS_EXISTS
                        || history.getMessageSource().isAddExistingHistory()) {
                        // Add the resulting history into our model
                        HistoryModel.addHistory(history);
                    }
                }
                pcs.firePropertyChange(PROP_IMPORTSTATUS_MESSAGE_PROCESSED, null, messageSource);
            }

            @Override
            public void run() {
                log.trace("=== TntDb Import Service Started ===");
                pcs.firePropertyChange(PROP_IMPORTSTATUS_IMPORTING, null, stopImporting);
                while (!stopImporting) {
                    while (MessageModel.hasMessages() && !stopImporting) {
                        try {
                            MessageSource message = MessageModel.getNextMessage();
                            if (message != null) // Thread paranoia that's actually happened!
                                importMessage(message);
                        } catch (Exception e) {
                            Display.getDefault().syncExec(new Runnable() {
                                @Override
                                public void run() {
                                    String msg = "Error while importing email into TntConnect.";
                                    Util.reportError(MIST.getView().getShell(), "Import error", msg, e);
                                }
                            });
                        }
                    }
                }
                pcs.firePropertyChange(PROP_IMPORTSTATUS_STOPPED, null, stopImporting);
                log.trace("=== TntDb Import Service Stopped ===");
            }
        };
        importThread.start();
    }

    public static void stopImportService() {
        stopImporting = true;
    }

    /**
     * Converts {@link java.sql.TimeStamp} to {@link java.time.LocalDateTime}.
     *
     * @param ts
     *            the timestamp to convert
     * @return the timestamp in LocalDateTime format
     */
    public static LocalDateTime timestampToDate(Timestamp ts) {
        if (ts == null)
            return null;
        return LocalDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault()).withNano(0);
    }

    /**
     * Updates the specified table's "LastEdit" value to be "now".
     * <p>
     * LastEdit should be updated on any record in any table, anytime it changes - with the exception of calculated
     * fields (e.g. LastThank, LastActivity, etc.)
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param table
     *            the table to update
     * @param id
     *            the id of the table's primary key to update
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void updateTableLastEdit(String table, int id) throws SQLException {
        log.trace("updateTableLastEdit({},{})", table, id);

        String query = String.format(
            "UPDATE [%s] SET [LastEdit] = %s WHERE [%sId] = %s",
            table,
            formatDbDate(LocalDateTime.now().withNano(0)),
            table,
            id);
        runQuery(query);
    }

}
