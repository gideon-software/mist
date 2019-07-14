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

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDateTime;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.exceptions.TntDbException;
import com.gideonsoftware.mist.tntapi.HistoryManager;
import com.gideonsoftware.mist.tntapi.TntDb;
import com.gideonsoftware.mist.tntapi.entities.History;
import com.gideonsoftware.mist.tntapi.entities.TaskType;

@RunWith(JUnit4.class)
public class HistoryManagerTest {
    // private static Logger log;

    private static Integer MRINCREDIBLE_CONTACTID;
    private static History MRINCREDIBLE_HISTORY;

    private static Integer BAMBIDEER_CONTACTID;
    private static Integer BAMBIDEER_HISTORYID;
    private static String BAMBIDEER_HISTORY_DESCRIPTION;
    private static String BAMBIDEER_HISTORY_NOTES;

    private static Integer DONALDDUCK_CONTACTID;

    private static Integer USERID_TOM;

    public static void createTestData() {

        USERID_TOM = Integer.valueOf(1481326147);
        LocalDateTime date = LocalDateTime.of(2017, 7, 23, 0, 0);

        //
        // Mr. Incredible
        // A non-existent history item with new date
        //

        MRINCREDIBLE_CONTACTID = Integer.valueOf(1);
        MRINCREDIBLE_HISTORY = new History();
        MRINCREDIBLE_HISTORY.setHistoryDate(date);
        MRINCREDIBLE_HISTORY.setTaskTypeId(TaskType.EMAIL);
        MRINCREDIBLE_HISTORY.setDescription("This is my description.");
        MRINCREDIBLE_HISTORY.setNotes("These are my notes.");
        MRINCREDIBLE_HISTORY.setHistoryResultId(History.RESULT_DONE);
        MRINCREDIBLE_HISTORY.getContactInfo().setId(MRINCREDIBLE_CONTACTID);
        MRINCREDIBLE_HISTORY.setLoggedByUserId(USERID_TOM);

        //
        // Bambi Deer
        // An existing history item
        //

        BAMBIDEER_CONTACTID = Integer.valueOf(272072119);
        BAMBIDEER_HISTORYID = Integer.valueOf(537624034); // An email
        BAMBIDEER_HISTORY_DESCRIPTION = "Email tasks this color (BLACK)";
        BAMBIDEER_HISTORY_NOTES = "";

        //
        // Other contact data
        //

        DONALDDUCK_CONTACTID = Integer.valueOf(608241759);
    }

    @BeforeClass
    public static void setup() {
        MIST.configureLogging(HistoryManager.class);
        TntDbTest.setupTntDb();
        createTestData();
    }

    @AfterClass
    public static void teardown() {
        TntDbTest.teardownTntDb();
    }

    /**
     * Tests adding history and then retrieving it again
     */
    @Test
    public void addHistory() throws TntDbException, SQLException {
        History history = new History(MRINCREDIBLE_HISTORY);
        HistoryManager.create(history);
        History retrievedHistory = HistoryManager.get(history.getHistoryId());
        // Use toString to avoid milliseconds; MIST ignores them
        assertEquals(MRINCREDIBLE_HISTORY.getHistoryDate(), retrievedHistory.getHistoryDate());
        assertEquals(MRINCREDIBLE_HISTORY.getDescription(), retrievedHistory.getDescription());
        assertEquals(MRINCREDIBLE_HISTORY.getNotes(), retrievedHistory.getNotes());
        assertEquals(MRINCREDIBLE_HISTORY.getHistoryResultId(), retrievedHistory.getHistoryResultId());
        assertEquals(MRINCREDIBLE_HISTORY.getContactInfo().getId(), retrievedHistory.getContactInfo().getId());
        assertEquals(MRINCREDIBLE_HISTORY.getLoggedByUserId(), retrievedHistory.getLoggedByUserId());
    }

    /**
     * Tests adding history twice and verify that it's not added the second time
     */
    @Test
    public void addHistoryDuplicates() throws TntDbException, SQLException {
        History history = new History(MRINCREDIBLE_HISTORY);
        HistoryManager.create(history);
        assertEquals(History.STATUS_ADDED, history.getStatus());

        // Exact duplicate
        History historyCopy = new History(MRINCREDIBLE_HISTORY);
        HistoryManager.create(historyCopy);
        assertEquals(History.STATUS_EXISTS, historyCopy.getStatus());

        // Different subject
        History historyNewSubject = new History(MRINCREDIBLE_HISTORY);
        historyNewSubject.setDescription("This is a NEW subject. Should still be same history.");
        HistoryManager.create(historyNewSubject);
        assertEquals(History.STATUS_EXISTS, historyNewSubject.getStatus());

        // Different body
        History historyNewBody = new History(MRINCREDIBLE_HISTORY);
        historyNewBody.setNotes("This is a NEW body. Should still be same history.");
        HistoryManager.create(historyNewBody);
        assertEquals(History.STATUS_EXISTS, historyNewBody.getStatus());

        // Different date
        History historyNewDate = new History(MRINCREDIBLE_HISTORY);
        LocalDateTime date = historyNewDate.getHistoryDate();
        historyNewDate.setHistoryDate(date.plusYears(1));
        HistoryManager.create(historyNewDate);
        assertEquals(History.STATUS_ADDED, historyNewDate.getStatus());

        // Different contact
        History historyNewContact = new History(MRINCREDIBLE_HISTORY);
        historyNewContact.getContactInfo().setId(DONALDDUCK_CONTACTID); // Donald & Daisy Duck
        HistoryManager.create(historyNewContact);
        assertEquals(History.STATUS_ADDED, historyNewContact.getStatus());

        // Different result
        History historyNewResult = new History(MRINCREDIBLE_HISTORY);
        historyNewResult.setHistoryResultId(History.RESULT_RECEIVED);
        // If we change the test history status, this test won't help =)
        assertEquals(MRINCREDIBLE_HISTORY.getHistoryResultId(), History.RESULT_DONE);
        HistoryManager.create(historyNewResult);
        assertEquals(History.STATUS_ADDED, historyNewResult.getStatus());
    }

    /**
     * Tests adding history with a long (100+ char) description
     */
    @Test
    public void addHistoryLongDescription() throws TntDbException, SQLException {
        History history = new History(MRINCREDIBLE_HISTORY);
        String longStr1 = StringUtils.repeat("x", 151);
        history.setDescription(longStr1);
        HistoryManager.create(history);
        assertEquals(History.STATUS_ADDED, history.getStatus());
        History addedHistoryLongStr1 = HistoryManager.get(history.getHistoryId());
        assertEquals(history.getDescription().substring(0, 150), addedHistoryLongStr1.getDescription());
        TntDb.rollback();

        history = new History(MRINCREDIBLE_HISTORY);
        String longStr2 = StringUtils.repeat("x", 1000);
        history.setDescription(longStr2);
        HistoryManager.create(history);
        assertEquals(History.STATUS_ADDED, history.getStatus());
        History addedHistoryLongStr2 = HistoryManager.get(history.getHistoryId());
        assertEquals(history.getDescription().substring(0, 150), addedHistoryLongStr2.getDescription());
        TntDb.rollback();
    }

    /**
     * Tests adding history with long notes
     */
    @Test
    public void addHistoryLongNotes() throws TntDbException, SQLException {
        History history = new History(MRINCREDIBLE_HISTORY);
        String longStr1 = StringUtils.repeat("x", 1025);
        history.setNotes(longStr1);
        HistoryManager.create(history);
        assertEquals(History.STATUS_ADDED, history.getStatus());
        History addedHistoryLongStr1 = HistoryManager.get(history.getHistoryId());
        assertEquals(history.getNotes(), addedHistoryLongStr1.getNotes());
        TntDb.rollback();

        history = new History(MRINCREDIBLE_HISTORY);
        String longStr2 = StringUtils.repeat("x", 4097);
        history.setNotes(longStr2);
        HistoryManager.create(history);
        assertEquals(History.STATUS_ADDED, history.getStatus());
        History addedHistoryLongStr2 = HistoryManager.get(history.getHistoryId());
        assertEquals(history.getNotes(), addedHistoryLongStr2.getNotes());
        TntDb.rollback();
    }

    /**
     * Tests adding history with newline characters
     */
    @Test
    public void addHistoryNewLines() throws TntDbException, SQLException {
        History history = new History(MRINCREDIBLE_HISTORY);
        String nl = System.lineSeparator();
        String nlStr = "Let's see if" + nl + "newline characters" + nl + nl + "work correctly.";
        history.setNotes(nlStr);
        HistoryManager.create(history);
        History addedHistoryNLs = HistoryManager.get(history.getHistoryId());
        assertEquals(history.getNotes(), addedHistoryNLs.getNotes());
    }

    /**
     * Tests adding history with one double quote character, which has previously
     * caused very slow responses when creating PreparedStatement
     */
    @Test
    public void addHistoryOneDoubleQuote() throws TntDbException, SQLException {
        History history = new History(MRINCREDIBLE_HISTORY);
        String str = "\"12345678901234567890123456"; // Should happen quickly
        history.setNotes(str);
        HistoryManager.create(history);
        History addedHistoryOneDoubleQuote = HistoryManager.get(history.getHistoryId());
        assertEquals(history.getNotes(), addedHistoryOneDoubleQuote.getNotes());
    }

    /**
     * Tests adding history with special characters
     */
    @Test
    public void addHistorySpecialCharacters() throws TntDbException, SQLException {
        History history = new History(MRINCREDIBLE_HISTORY);
        String specialChars = "!@#$%^&*()'\";:,.<>/?\\[]{}|.,-_=+";
        history.setNotes(specialChars);
        HistoryManager.create(history);
        History addedHistorySpecialChars = HistoryManager.get(history.getHistoryId());
        assertEquals(history.getNotes(), addedHistorySpecialChars.getNotes());
    }

    /**
     * Tests getting existing email in sample Tnt DB
     */
    @Test
    public void getHistorySample() throws TntDbException, SQLException, ParseException {
        History history = HistoryManager.get(BAMBIDEER_HISTORYID); // Only email reference in sample DB!
        assertEquals(BAMBIDEER_HISTORYID, history.getHistoryId());
        assertEquals(BAMBIDEER_HISTORY_DESCRIPTION, history.getDescription());
        assertEquals(LocalDateTime.of(2007, 11, 06, 15, 27, 21), history.getHistoryDate());
        assertEquals("", history.getNotes());
        assertEquals(false, history.isChallenge());
        assertEquals(false, history.isThank());
        assertEquals(false, history.isMassMailing());
        assertEquals(History.RESULT_DONE, history.getHistoryResultId());
        assertEquals(BAMBIDEER_CONTACTID, history.getContactInfo().getId());
    }

    @After
    public void rollback() throws TntDbException {
        TntDb.rollback();
    }

    /**
     * Tests updating of a history's description
     */
    @Test
    public void updateHistoryDescription() throws SQLException {
        // Verify initial state
        History history = HistoryManager.get(BAMBIDEER_HISTORYID);
        assertEquals(BAMBIDEER_HISTORY_DESCRIPTION, history.getDescription());
        LocalDateTime lastEdit = HistoryManager.getLastEditDate(BAMBIDEER_HISTORYID);

        // Update subject
        String newSubject = "Whatcha doing? Hibernating?";
        HistoryManager.updateDescription(BAMBIDEER_HISTORYID, newSubject);

        // Verify new state
        History newHistory = HistoryManager.get(BAMBIDEER_HISTORYID);
        assertEquals(newSubject, newHistory.getDescription());
        LocalDateTime newLastEdit = HistoryManager.getLastEditDate(BAMBIDEER_HISTORYID);
        assertEquals(true, lastEdit.isBefore(newLastEdit));
    }

    /**
     * Tests updating of a history's IsChallenge field
     */
    @Test
    public void updateHistoryIsChallenge() throws SQLException {
        // Get the current value
        boolean isChallenge = HistoryManager.get(BAMBIDEER_HISTORYID).isChallenge();

        // Change it
        HistoryManager.updateIsChallenge(BAMBIDEER_HISTORYID, !isChallenge);
        assertEquals(!isChallenge, HistoryManager.get(BAMBIDEER_HISTORYID).isChallenge());

        // Change it back
        HistoryManager.updateIsChallenge(BAMBIDEER_HISTORYID, isChallenge);
        assertEquals(isChallenge, HistoryManager.get(BAMBIDEER_HISTORYID).isChallenge());
    }

    /**
     * Tests updating of a history's IsMassMailing field
     */
    @Test
    public void updateHistoryIsMassMailing() throws SQLException {
        // Get the current value
        boolean isMassMailing = HistoryManager.get(BAMBIDEER_HISTORYID).isMassMailing();

        // Change it
        HistoryManager.updateIsMassMailing(BAMBIDEER_HISTORYID, !isMassMailing);
        assertEquals(!isMassMailing, HistoryManager.get(BAMBIDEER_HISTORYID).isMassMailing());

        // Change it back
        HistoryManager.updateIsMassMailing(BAMBIDEER_HISTORYID, isMassMailing);
        assertEquals(isMassMailing, HistoryManager.get(BAMBIDEER_HISTORYID).isMassMailing());
    }

    /**
     * Tests updating of a history's IsThank field
     */
    @Test
    public void updateHistoryIsThank() throws SQLException {
        // Get the current value
        boolean isThank = HistoryManager.get(BAMBIDEER_HISTORYID).isThank();

        // Change it
        HistoryManager.updateIsThank(BAMBIDEER_HISTORYID, !isThank);
        assertEquals(!isThank, HistoryManager.get(BAMBIDEER_HISTORYID).isThank());

        // Change it back
        HistoryManager.updateIsThank(BAMBIDEER_HISTORYID, isThank);
        assertEquals(isThank, HistoryManager.get(BAMBIDEER_HISTORYID).isThank());
    }

    /**
     * Tests updating of a history's last edit
     */
    @Test
    public void updateHistoryLastEdit() throws SQLException {
        // Get current LastEdit
        LocalDateTime curLastEdit = HistoryManager.getLastEditDate(BAMBIDEER_HISTORYID);

        // Update last edit
        HistoryManager.updateLastEdit(BAMBIDEER_HISTORYID);

        // Verify that last edit is updated (precise value check is not possible)
        LocalDateTime newLastEdit = HistoryManager.getLastEditDate(BAMBIDEER_HISTORYID);
        assertEquals(true, curLastEdit.isBefore(newLastEdit));
    }

    /**
     * Tests updating of a history's notes
     */
    @Test
    public void updateHistoryNotes() throws SQLException {
        // Verify initial state
        History history = HistoryManager.get(BAMBIDEER_HISTORYID);
        assertEquals(BAMBIDEER_HISTORY_NOTES, history.getNotes());
        LocalDateTime lastEdit = HistoryManager.getLastEditDate(BAMBIDEER_HISTORYID);

        // Update body
        String newBody = "Whatcha doing? Hibernating?";
        HistoryManager.updateNotes(BAMBIDEER_HISTORYID, newBody);

        // Verify new state
        History newHistory = HistoryManager.get(BAMBIDEER_HISTORYID);
        assertEquals(newBody, newHistory.getNotes());
        LocalDateTime newLastEdit = HistoryManager.getLastEditDate(BAMBIDEER_HISTORYID);
        assertEquals(true, lastEdit.isBefore(newLastEdit));
    }
}
