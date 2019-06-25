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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDateTime;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.exceptions.TntDbException;
import com.github.tomhallman.mist.tntapi.ContactManager;
import com.github.tomhallman.mist.tntapi.TntDb;

@RunWith(JUnit4.class)
public class TntDbTest {

    private static Logger log;

    private static String dbName;
    private static String dbPath;

    private static Integer BAMBIDEER_HISTORYID;
    private static String BAMBIDEER_HISTORY_DATE_STR;
    private static String BAMBIDEER_HISTORY_SUBJECT;

    public static void createTestData() {

        //
        // Bambi Deer
        // An existing history item
        //

        BAMBIDEER_HISTORYID = Integer.valueOf(537624034); // An email
        BAMBIDEER_HISTORY_DATE_STR = "#2007-11-06 15:27:21#";
        BAMBIDEER_HISTORY_SUBJECT = "Email tasks this color (BLACK)";
    }

    @BeforeClass
    public static void setup() {
        MIST.configureLogging(TntDbTest.class);
        setupTntDb();
        createTestData();
    }

    public static void setupTntDb() {
        dbName = "Toontown.mpddb";

        try {
            Path source = Paths.get(new TntDbTest().getClass().getResource(dbName).toURI());
            Path dest = Paths.get(System.getProperty("java.io.tmpdir") + "/" + dbName);
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            dbPath = dest.toString();
        } catch (URISyntaxException | IOException e) {
            log.error(e);
            fail("Could not set up the tests properly!");
        }
        String[] opts = { "--profile=tntdbtest" };
        MIST.parseOptions(opts);
        TntDb.setTntDatabasePath(dbPath);
        TntDb.setUseCommit(false);

        try {
            TntDb.connect(true);
        } catch (TntDbException e) {
            fail("Unable to connect to TntConnect database!");
        }
    }

    @AfterClass
    public static void teardown() {
        teardownTntDb();
    }

    public static void teardownTntDb() {
        try {
            TntDb.disconnect();
            Files.deleteIfExists(Paths.get(dbPath));
        } catch (IOException e) {
            log.error(e);
            fail("Database shouldn't have trouble being deleted!");
        }
    }

    /**
     * Tests formatting DB booleans
     * TODO
     */
    @Test
    public void formatDbBoolean() {

    }

    /**
     * Tests formatting DB currency
     * TODO
     */
    @Test
    public void formatDbCurrency() {

    }

    /**
     * Tests formatting DB dates with and without time
     */
    @Test
    public void formatDbDate() throws ParseException {
        LocalDateTime date = LocalDateTime.of(2017, 9, 30, 13, 45, 24);
        assertEquals("#2017-09-30 13:45:24#", TntDb.formatDbDate(date));
        assertEquals("#2017-09-30#", TntDb.formatDbDateNoTime(date));
        date = LocalDateTime.of(2007, 11, 6, 15, 27, 21);
        assertEquals("#2007-11-06 15:27:21#", TntDb.formatDbDate(date));
        assertEquals("#2007-11-06#", TntDb.formatDbDateNoTime(date));
    }

    /**
     * Tests formatting DB ints
     * TODO
     */
    @Test
    public void formatDbInt() {

    }

    /**
     * Tests formatting DB strings
     */
    @Test
    public void formatDbString() {
        String[][] tests = {
            { "", "''" },
            { "test", "'test'" },
            { "'test'", "'''test'''" },
            { "What''s up?", "'What''''s up?'" } };

        for (int i = 0; i < tests.length; i++)
            assertEquals(TntDb.formatDbString(tests[i][0]), tests[i][1]);
    }

    /**
     * Tests getting available ids from History and HistoryContact tables
     */
    @Test
    public void getAvailableId() throws SQLException {
        // Try this multiple times
        // Doesn't guarantee that it works, but gives some assurance!
        for (int i = 0; i < 100; i++) {
            TntDb.getAvailableId(TntDb.TABLE_HISTORYCONTACT);
            TntDb.getAvailableId(TntDb.TABLE_HISTORY);
        }
    }

    /**
     * Tests getting one when there are multiple values
     */
    @Test(expected = TntDbException.class)
    public void getOneX_MultipleValues() throws TntDbException, SQLException {
        assertEquals(null, ContactManager.getFileAs(0)); // Verify initial state
        TntDb.getOneX("SELECT [ContactID] FROM [Contact] WHERE [ContactID] > 0", TntDb.TYPE_INT);
    }

    /**
     * Tests getting one when there is no value
     */
    @Test
    public void getOneX_NoValues() throws TntDbException, SQLException {
        assertEquals(null, ContactManager.getFileAs(0)); // Verify initial state
        Object obj = TntDb.getOneX("SELECT [ContactID] FROM [Contact] WHERE [ContactID] = 0", TntDb.TYPE_INT);
        assertEquals(null, obj);
    }

    /**
     * Tests getting one of various types
     */
    @Test
    public void getOneX_OneValue() throws TntDbException, SQLException {
        Object obj = null;

        obj = TntDb.getOneX(
            String.format("SELECT [HistoryID] FROM [History] WHERE [HistoryID] = %s", BAMBIDEER_HISTORYID),
            TntDb.TYPE_INT);
        assertEquals(true, obj instanceof Integer);
        assertEquals(obj, BAMBIDEER_HISTORYID);

        obj = TntDb.getOneX(
            String.format("SELECT [HistoryDate] FROM [History] WHERE [HistoryID] = %s", BAMBIDEER_HISTORYID),
            TntDb.TYPE_DATE);
        assertEquals(true, obj instanceof LocalDateTime);
        assertEquals(BAMBIDEER_HISTORY_DATE_STR, TntDb.formatDbDate((LocalDateTime) obj));

        obj = TntDb.getOneX(
            String.format("SELECT [Description] FROM [History] WHERE [HistoryID] = %s", BAMBIDEER_HISTORYID),
            TntDb.TYPE_STRING);
        assertEquals(true, obj instanceof String);
        assertEquals(BAMBIDEER_HISTORY_SUBJECT, obj);

        // Multiple columns is also allowed, but only first column's value is returned
        obj = TntDb.getOneX(
            String.format(
                "SELECT [Description], [LastEdit] FROM [History] WHERE [HistoryID] = %s",
                BAMBIDEER_HISTORYID),
            TntDb.TYPE_STRING);
        assertEquals(true, obj instanceof String);
        assertEquals(BAMBIDEER_HISTORY_SUBJECT, obj);
    }

    /**
     * Tests getRowCount using pre-defined Contact table
     */
    @Test
    public void getRowCount() throws SQLException, TntDbException {
        ResultSet rs = null;
        rs = TntDb.runQuery("SELECT * FROM [Contact] WHERE [FileAs] LIKE 'Nobody'");
        assertEquals(0, TntDb.getRowCount(rs));
        rs = TntDb.runQuery("SELECT * FROM [Contact] WHERE [FileAs] LIKE 'Kent%'");
        assertEquals(1, TntDb.getRowCount(rs)); // Clark & Lois
        rs = TntDb.runQuery("SELECT * FROM [Contact] WHERE [FileAs] LIKE 'Bear%'");
        assertEquals(2, TntDb.getRowCount(rs)); // Yogi, Baloo & Balinda
    }

    /**
     * Rolls back the database after each test
     */
    @After
    public void rollback() throws SQLException, TntDbException {
        TntDb.rollback();
        assertEquals(TntDb.getConnection().getAutoCommit(), false);
    }
}
