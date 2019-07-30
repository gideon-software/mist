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
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.time.LocalDateTime;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.exceptions.TntDbException;
import com.gideonsoftware.mist.tntapi.entities.History;
import com.gideonsoftware.mist.tntapi.entities.TaskType;

@RunWith(JUnit4.class)
public class ContactManagerTest {
    // private static Logger log;

    private static LocalDateTime OLDDATE;
    private static LocalDateTime NEWDATE;

    private static History MRINCREDIBLE_HISTORY;
    private static Integer MRINCREDIBLE_CONTACTID;
    private static LocalDateTime MRINCREDIBLE_LASTLETTER;
    private static LocalDateTime MRINCREDIBLE_LASTACTIVITY;
    private static Integer MRINCREDIBLE_CHALLENGESSINCELASTGIFT;

    private static History GEORGEJETSON_HISTORY;
    private static Integer GEORGEJETSON_CONTACTID;
    private static LocalDateTime GEORGEJETSON_LASTACTIVITY;
    private static LocalDateTime GEORGEJETSON_LASTTHANK;
    private static LocalDateTime GEORGEJETSON_LASTCHALLENGE;

    private static Integer DONALDDUCK_CONTACTID;

    private static Integer USERID_TOM;

    public static void createTestData() {

        // User data
        USERID_TOM = Integer.valueOf(1481326147);

        // Date values
        OLDDATE = LocalDateTime.of(2000, 1, 1, 0, 0);
        NEWDATE = LocalDateTime.of(2017, 7, 23, 0, 0);

        //
        // Mr. Incredible
        // A non-existent history item with new date
        //

        MRINCREDIBLE_CONTACTID = Integer.valueOf(1);
        MRINCREDIBLE_LASTLETTER = LocalDateTime.of(2008, 2, 23, 0, 0);
        MRINCREDIBLE_LASTACTIVITY = MRINCREDIBLE_LASTLETTER; // They're the same
        MRINCREDIBLE_CHALLENGESSINCELASTGIFT = Integer.valueOf(2);

        MRINCREDIBLE_HISTORY = new History();
        MRINCREDIBLE_HISTORY.setHistoryDate(NEWDATE);
        MRINCREDIBLE_HISTORY.setTaskTypeId(TaskType.EMAIL);
        MRINCREDIBLE_HISTORY.setDescription("This is my description.");
        MRINCREDIBLE_HISTORY.setNotes("These are my notes.");
        MRINCREDIBLE_HISTORY.setHistoryResultId(History.RESULT_DONE);
        MRINCREDIBLE_HISTORY.getContactInfo().setId(MRINCREDIBLE_CONTACTID);
        MRINCREDIBLE_HISTORY.setLoggedByUserId(USERID_TOM);

        //
        // George Jetson
        // A non-existent history item with new date that is useful for challenge and thank testing
        //

        GEORGEJETSON_CONTACTID = Integer.valueOf(1799108674);
        GEORGEJETSON_LASTACTIVITY = LocalDateTime.of(2008, 2, 23, 0, 0);
        GEORGEJETSON_LASTTHANK = LocalDateTime.of(2006, 9, 28, 0, 0);
        GEORGEJETSON_LASTCHALLENGE = LocalDateTime.of(2007, 6, 15, 0, 0);

        GEORGEJETSON_HISTORY = new History();
        GEORGEJETSON_HISTORY.setHistoryDate(NEWDATE);
        MRINCREDIBLE_HISTORY.setTaskTypeId(TaskType.EMAIL);
        GEORGEJETSON_HISTORY.setDescription("This is my description.");
        GEORGEJETSON_HISTORY.setNotes("These are my notes.");
        GEORGEJETSON_HISTORY.setHistoryResultId(History.RESULT_DONE);
        GEORGEJETSON_HISTORY.getContactInfo().setId(GEORGEJETSON_CONTACTID);
        GEORGEJETSON_HISTORY.setLoggedByUserId(USERID_TOM);
        GEORGEJETSON_HISTORY.setChallenge(true);
        GEORGEJETSON_HISTORY.setThank(true);

        //
        // Other contact data
        //

        DONALDDUCK_CONTACTID = Integer.valueOf(608241759);
    }

    @BeforeClass
    public static void setup() {
        MIST.configureLogging(ContactManager.class);
        TntDbTest.setupTntDb();
        createTestData();
    }

    @AfterClass
    public static void teardown() {
        TntDbTest.teardownTntDb();
    }

    /**
     * Tests getting the challenges since a contact's last gift.
     */
    public void getContactChallengesSinceLastGift() throws TntDbException, SQLException {
        Integer challenges = ContactManager.getChallengesSinceLastGift(MRINCREDIBLE_CONTACTID);
        assertEquals(MRINCREDIBLE_CHALLENGESSINCELASTGIFT, challenges);
    }

    /**
     * Tests counting and returning contacts given an existing, unique email address
     */
    @Test
    public void getContactIdByEmailFound() throws TntDbException, SQLException {
        int count = ContactManager.getContactsByEmailCount("dduck@disney.org");
        assertEquals(1, count);
        Integer id = ContactManager.getContactIdByEmail("dduck@disney.org");
        assertEquals(DONALDDUCK_CONTACTID, id);
    }

    /**
     * Tests counting and returning contacts given an existing, unique email address using angle brackets.
     */
    @Test
    public void getContactIdByEmailFoundWithBrackets() throws TntDbException, SQLException {
        TntDb.runQuery(
            "UPDATE [Contact] SET [Email3] = '\"Bob Parr\" <parrb@metroinsurance.com>' WHERE [ContactID] = 1");
        int count = ContactManager.getContactsByEmailCount("parrb@metroinsurance.com");
        assertEquals(1, count);
        Integer id = ContactManager.getContactIdByEmail("parrb@metroinsurance.com");
        assertEquals(MRINCREDIBLE_CONTACTID, id);
    }

    /**
     * Tests counting and returning contacts given an existing, unique email address using underscores.
     */
    @Test
    public void getContactIdByEmailFoundWithUnderscores() throws TntDbException, SQLException {
        TntDb.runQuery(
            "UPDATE [Contact] SET [Email3] = 'parr_b@metroinsurance.com' WHERE [ContactID] = "
                + MRINCREDIBLE_CONTACTID);
        int count = ContactManager.getContactsByEmailCount("parr_b@metroinsurance.com");
        assertEquals(1, count);
        Integer id = ContactManager.getContactIdByEmail("parr_b@metroinsurance.com");
        assertEquals(MRINCREDIBLE_CONTACTID, id);
    }

    /**
     * Tests counting and returning contacts given an existing, non-unique email address
     */
    @Test(expected = TntDbException.class)
    public void getContactIdByEmailMultipleFound() throws TntDbException, SQLException {
        // Add duplicate email address to primary contact
        TntDb.runQuery(
            "UPDATE [Contact] SET [Email3] = 'dduck@disney.org' WHERE [ContactID] = " + MRINCREDIBLE_CONTACTID);
        int count = ContactManager.getContactsByEmailCount("dduck@disney.org");
        assertEquals(2, count);
        ContactManager.getContactIdByEmail("dduck@disney.org");
        fail("Multiple contacts found; this should have failed.");
    }

    /**
     * Tests counting and returning contacts given a non-existent email address
     */
    @Test
    public void getContactIdByEmailNotFound() throws TntDbException, SQLException {
        assertEquals(0, ContactManager.getContactsByEmailCount("nobody@nowhere.nope"));
        assertEquals(null, ContactManager.getContactIdByEmail("nobody@nowhere.nope"));
    }

    /**
     * Tests retrieval of Contact's "Last Activity" date
     */
    @Test
    public void getContactLastActivityDate() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(MRINCREDIBLE_LASTACTIVITY, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));

        // Insert old history; "last activity" shouldn't update
        History oldHistory = new History(MRINCREDIBLE_HISTORY);
        oldHistory.setHistoryDate(OLDDATE);
        HistoryManager.create(oldHistory);
        assertEquals(MRINCREDIBLE_LASTACTIVITY, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));

        // Insert new history; "last activity" should update
        History newHistory = new History(MRINCREDIBLE_HISTORY);
        oldHistory.setHistoryDate(NEWDATE);
        HistoryManager.create(newHistory);
        assertEquals(NEWDATE, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));
    }

    /**
     * Tests retrieval of Contact's "Last Challenge" date
     */
    @Test
    public void getContactLastChallengeDate() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(GEORGEJETSON_LASTCHALLENGE, ContactManager.getLastChallengeDate(GEORGEJETSON_CONTACTID));
        assertEquals(true, GEORGEJETSON_HISTORY.isChallenge());

        // Insert old history; "last challenge" shouldn't update
        History oldHistory = new History(GEORGEJETSON_HISTORY);
        oldHistory.setHistoryDate(OLDDATE);
        HistoryManager.create(oldHistory);
        assertEquals(GEORGEJETSON_LASTCHALLENGE, ContactManager.getLastChallengeDate(GEORGEJETSON_CONTACTID));

        // Insert new history; "last challenge" should update
        History newHistory = new History(GEORGEJETSON_HISTORY);
        HistoryManager.create(newHistory);
        assertEquals(NEWDATE, ContactManager.getLastChallengeDate(GEORGEJETSON_CONTACTID));
    }

    /**
     * Tests retrieval of Contact's "Last Letter" date (and "Last Activity", which may be redundant)
     */
    @Test
    public void getContactLastLetterDate() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(MRINCREDIBLE_LASTLETTER, ContactManager.getLastLetterDate(MRINCREDIBLE_CONTACTID));

        // Insert old history; "last letter" shouldn't update, nor should "last activity"
        History oldHistory = new History(MRINCREDIBLE_HISTORY);
        oldHistory.setHistoryDate(OLDDATE);
        HistoryManager.create(oldHistory);
        assertEquals(MRINCREDIBLE_LASTLETTER, ContactManager.getLastLetterDate(MRINCREDIBLE_CONTACTID));
        assertEquals(MRINCREDIBLE_LASTLETTER, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));

        // Insert new history; "last letter" should update, as should "last activity"
        History newHistory = new History(MRINCREDIBLE_HISTORY);
        HistoryManager.create(newHistory);
        assertEquals(NEWDATE, ContactManager.getLastLetterDate(MRINCREDIBLE_CONTACTID));
        assertEquals(NEWDATE, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));
    }

    /**
     * Tests retrieval of Contact's "Last Thank" date
     */
    @Test
    public void getContactLastThankDate() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(GEORGEJETSON_LASTTHANK, ContactManager.getLastThankDate(GEORGEJETSON_CONTACTID));
        assertEquals(true, GEORGEJETSON_HISTORY.isThank());

        // Insert old history; "last thank" shouldn't update
        History oldHistory = new History(GEORGEJETSON_HISTORY);
        oldHistory.setHistoryDate(OLDDATE);
        HistoryManager.create(oldHistory);
        assertEquals(GEORGEJETSON_LASTTHANK, ContactManager.getLastThankDate(GEORGEJETSON_CONTACTID));

        // Insert new history; "last thank" should update
        History newHistory = new History(GEORGEJETSON_HISTORY);
        HistoryManager.create(newHistory);
        assertEquals(NEWDATE, ContactManager.getLastThankDate(GEORGEJETSON_CONTACTID));
    }

    /**
     * Tests getting contact names for contacts that exist
     */
    @Test
    public void getContactNameFound() throws TntDbException, SQLException {
        assertEquals("Parr, Bob and Helen", ContactManager.getFileAs(MRINCREDIBLE_CONTACTID));
        assertEquals("Duck, Donald and Daisy", ContactManager.getFileAs(DONALDDUCK_CONTACTID));
    }

    /**
     * Tests getting a contact name for a contact that doesn't exist
     */
    @Test
    public void getContactNameNotFound() throws TntDbException, SQLException {
        assertEquals(null, ContactManager.getFileAs(0));
    }

    /**
     * Tests recalculation of a contact's challenges since the last gift
     */
    @Test
    public void recalculateContactChallengesSinceLastGift() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(
            MRINCREDIBLE_CHALLENGESSINCELASTGIFT.intValue(),
            ContactManager.getChallengesSinceLastGift(MRINCREDIBLE_CONTACTID));

        // Set number of challenges to 0
        TntDb.runQuery(
            String.format(
                "UPDATE [Contact] SET [ChallengesSinceLastGift] = 0 WHERE [ContactID] = %s",
                MRINCREDIBLE_CONTACTID));
        assertEquals(0, ContactManager.getChallengesSinceLastGift(MRINCREDIBLE_CONTACTID));

        // Recalculate
        ContactManager.recalculateChallengesSinceLastGift(MRINCREDIBLE_CONTACTID);

        // Verify that the number of challenges is correct
        assertEquals(
            MRINCREDIBLE_CHALLENGESSINCELASTGIFT.intValue(),
            ContactManager.getChallengesSinceLastGift(MRINCREDIBLE_CONTACTID));
    }

    /**
     * Tests recalculation of a contact's last activity
     */
    @Test
    public void recalculateContactLastActivity() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(MRINCREDIBLE_LASTACTIVITY, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));

        // Remove last activity
        TntDb.runQuery(
            String.format("UPDATE [Contact] SET [LastActivity] = null WHERE [ContactID] = %s", MRINCREDIBLE_CONTACTID));
        assertEquals(null, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));

        // Recalculate
        ContactManager.recalculateLastActivityDate(MRINCREDIBLE_CONTACTID);

        // Verify that last activity is correct
        assertEquals(MRINCREDIBLE_LASTACTIVITY, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));
    }

    /**
     * Tests recalculation of a contact's last challenge
     */
    @Test
    public void recalculateContactLastChallenge() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(GEORGEJETSON_LASTCHALLENGE, ContactManager.getLastChallengeDate(GEORGEJETSON_CONTACTID));

        // Remove last challenge
        TntDb.runQuery(
            String.format(
                "UPDATE [Contact] SET [LastChallenge] = null WHERE [ContactID] = %s",
                GEORGEJETSON_CONTACTID));
        assertEquals(null, ContactManager.getLastChallengeDate(GEORGEJETSON_CONTACTID));

        // Recalculate
        ContactManager.recalculateLastChallengeDate(GEORGEJETSON_CONTACTID);

        // Verify that last challenge is correct
        assertEquals(GEORGEJETSON_LASTCHALLENGE, ContactManager.getLastChallengeDate(GEORGEJETSON_CONTACTID));
    }

    /**
     * Tests recalculation of a contact's last letter
     */
    @Test
    public void recalculateContactLastLetter() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(MRINCREDIBLE_LASTLETTER, ContactManager.getLastLetterDate(MRINCREDIBLE_CONTACTID));

        // Remove last letter
        TntDb.runQuery(
            String.format("UPDATE [Contact] SET [LastLetter] = null WHERE [ContactID] = %s", MRINCREDIBLE_CONTACTID));
        assertEquals(null, ContactManager.getLastLetterDate(MRINCREDIBLE_CONTACTID));

        // Recalculate
        ContactManager.recalculateLastLetterDate(MRINCREDIBLE_CONTACTID);

        // Verify that last letter is correct
        assertEquals(MRINCREDIBLE_LASTLETTER, ContactManager.getLastLetterDate(MRINCREDIBLE_CONTACTID));
    }

    /**
     * Tests recalculation of a contact's last thank
     */
    @Test
    public void recalculateContactLastThank() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(GEORGEJETSON_LASTTHANK, ContactManager.getLastThankDate(GEORGEJETSON_CONTACTID));

        // Remove last challenge
        TntDb.runQuery(
            String.format("UPDATE [Contact] SET [LastThank] = null WHERE [ContactID] = %s", GEORGEJETSON_CONTACTID));
        assertEquals(null, ContactManager.getLastThankDate(GEORGEJETSON_CONTACTID));

        // Recalculate
        ContactManager.recalculateLastThankDate(GEORGEJETSON_CONTACTID);

        // Verify that last challenge is correct
        assertEquals(GEORGEJETSON_LASTTHANK, ContactManager.getLastThankDate(GEORGEJETSON_CONTACTID));
    }

    @After
    public void rollback() throws TntDbException {
        TntDb.rollback();
    }

    /**
     * Tests updating of a contact's last activity
     */
    @Test
    public void updateContactLastActivityDate() throws SQLException {
        // Verify initial state
        assertEquals(MRINCREDIBLE_LASTACTIVITY, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));

        // Update last activity
        ContactManager.updateLastActivityDate(MRINCREDIBLE_CONTACTID, NEWDATE);

        // Verify that last activity is correct
        assertEquals(NEWDATE, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));

        // Remove last activity; should force a recalculation
        ContactManager.updateLastActivityDate(MRINCREDIBLE_CONTACTID, null);
        assertEquals(MRINCREDIBLE_LASTACTIVITY, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));
    }

    /**
     * Tests updating of a contact's last challenge
     */
    @Test
    public void updateContactLastChallengeDate() throws SQLException {
        // Verify initial state
        assertEquals(GEORGEJETSON_LASTCHALLENGE, ContactManager.getLastChallengeDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTACTIVITY, ContactManager.getLastActivityDate(GEORGEJETSON_CONTACTID));

        // Update last challenge
        ContactManager.updateLastChallengeDate(GEORGEJETSON_CONTACTID, NEWDATE);

        // Verify that last challenge and last activity are correct
        assertEquals(NEWDATE, ContactManager.getLastChallengeDate(GEORGEJETSON_CONTACTID));
        assertEquals(NEWDATE, ContactManager.getLastActivityDate(GEORGEJETSON_CONTACTID));

        // Remove last challenge; should force a recalculation
        ContactManager.updateLastChallengeDate(GEORGEJETSON_CONTACTID, null);
        assertEquals(GEORGEJETSON_LASTCHALLENGE, ContactManager.getLastChallengeDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTACTIVITY, ContactManager.getLastActivityDate(GEORGEJETSON_CONTACTID));
    }

    /**
     * Tests updating of a contact's last edit
     */
    @Test
    public void updateContactLastEdit() throws SQLException {
        // Get current LastEdit
        LocalDateTime curLastEdit = ContactManager.getLastEditDate(MRINCREDIBLE_CONTACTID);

        // Update last edit
        ContactManager.updateLastEdit(MRINCREDIBLE_CONTACTID);

        // Verify that last edit is updated (precise value check is not possible)
        LocalDateTime newLastEdit = ContactManager.getLastEditDate(MRINCREDIBLE_CONTACTID);
        assertEquals(true, curLastEdit.isBefore(newLastEdit));
    }

    /**
     * Tests updating of a contact's last letter
     */
    @Test
    public void updateContactLastLetterDate() throws SQLException {
        // Verify initial state
        assertEquals(MRINCREDIBLE_LASTLETTER, ContactManager.getLastLetterDate(MRINCREDIBLE_CONTACTID));
        assertEquals(MRINCREDIBLE_LASTACTIVITY, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));

        // Update last letter
        ContactManager.updateLastLetterDate(MRINCREDIBLE_CONTACTID, NEWDATE);

        // Verify that last letter and last activity are correct
        assertEquals(NEWDATE, ContactManager.getLastLetterDate(MRINCREDIBLE_CONTACTID));
        assertEquals(NEWDATE, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));

        // Remove last letter; should force a recalculation
        ContactManager.updateLastLetterDate(MRINCREDIBLE_CONTACTID, null);
        assertEquals(MRINCREDIBLE_LASTLETTER, ContactManager.getLastLetterDate(MRINCREDIBLE_CONTACTID));
        assertEquals(MRINCREDIBLE_LASTACTIVITY, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));
    }

    /**
     * Tests updating of a contact's last thank
     */
    @Test
    public void updateContactLastThankDate() throws SQLException {
        // Verify initial state
        assertEquals(GEORGEJETSON_LASTTHANK, ContactManager.getLastThankDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTACTIVITY, ContactManager.getLastActivityDate(GEORGEJETSON_CONTACTID));

        // Update last thank
        ContactManager.updateLastThankDate(GEORGEJETSON_CONTACTID, NEWDATE);

        // Verify that last thank and last activity are correct
        assertEquals(NEWDATE, ContactManager.getLastThankDate(GEORGEJETSON_CONTACTID));
        assertEquals(NEWDATE, ContactManager.getLastActivityDate(GEORGEJETSON_CONTACTID));

        // Remove last thank; should force a recalculation
        ContactManager.updateLastThankDate(GEORGEJETSON_CONTACTID, null);
        assertEquals(GEORGEJETSON_LASTTHANK, ContactManager.getLastThankDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTACTIVITY, ContactManager.getLastActivityDate(GEORGEJETSON_CONTACTID));
    }
}
