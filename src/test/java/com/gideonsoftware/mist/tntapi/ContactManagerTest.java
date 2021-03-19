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

import java.lang.reflect.Field;
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
import com.gideonsoftware.mist.tntapi.entities.Contact;
import com.gideonsoftware.mist.tntapi.entities.ContactInfo;
import com.gideonsoftware.mist.tntapi.entities.History;
import com.gideonsoftware.mist.tntapi.entities.TaskType;

@RunWith(JUnit4.class)
public class ContactManagerTest {
    // private static Logger log;

    private static LocalDateTime OLDDATE;
    private static LocalDateTime NEWDATE;

    private static History MRINCREDIBLE_HISTORY;
    private static Integer MRINCREDIBLE_CONTACTID;
    private static String MRINCREDIBLE_FILEAS;
    private static LocalDateTime MRINCREDIBLE_LASTGIFT;
    private static LocalDateTime MRINCREDIBLE_LASTLETTER;
    private static LocalDateTime MRINCREDIBLE_LASTACTIVITY;
    private static Integer MRINCREDIBLE_CHALLENGESSINCELASTGIFT;

    private static History GEORGEJETSON_HISTORY;
    private static Integer GEORGEJETSON_CONTACTID;
    private static String GEORGEJETSON_FILEAS;
    private static LocalDateTime GEORGEJETSON_LASTACTIVITY;
    private static LocalDateTime GEORGEJETSON_LASTTHANK;
    private static LocalDateTime GEORGEJETSON_LASTCHALLENGE;
    private static LocalDateTime GEORGEJETSON_LASTVISIT;
    private static LocalDateTime GEORGEJETSON_LASTAPPOINTMENT;
    private static LocalDateTime GEORGEJETSON_LASTCALL;
    private static LocalDateTime GEORGEJETSON_LASTPRECALL;
    private static LocalDateTime GEORGEJETSON_LASTLETTER;

    private static Integer BAMBIDEER_CONTACTID;
    private static String BAMBIDEER_FILEAS;
    private static LocalDateTime BAMBIDEER_LASTACTIVITY;
    private static LocalDateTime BAMBIDEER_LASTPRECALL;
    private static Integer BAMBIDEER_CHALLENGESSINCELASTGIFT;

    private static Integer DONALDDUCK_CONTACTID;
    private static Integer RABBITRABBIT_CONTACTID;
    private static Integer NONEXISTENT_CONTACTID;

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
        MRINCREDIBLE_FILEAS = "Parr, Bob and Helen";
        MRINCREDIBLE_LASTGIFT = null;
        MRINCREDIBLE_LASTLETTER = LocalDateTime.of(2008, 2, 23, 0, 0);
        MRINCREDIBLE_LASTACTIVITY = MRINCREDIBLE_LASTLETTER;
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
        GEORGEJETSON_FILEAS = "Jetson, George and Jane";
        GEORGEJETSON_LASTACTIVITY = LocalDateTime.of(2008, 2, 23, 0, 0);
        GEORGEJETSON_LASTTHANK = LocalDateTime.of(2006, 9, 28, 0, 0);
        GEORGEJETSON_LASTCHALLENGE = LocalDateTime.of(2007, 6, 15, 0, 0);
        GEORGEJETSON_LASTVISIT = GEORGEJETSON_LASTCHALLENGE;
        GEORGEJETSON_LASTAPPOINTMENT = GEORGEJETSON_LASTCHALLENGE;
        GEORGEJETSON_LASTPRECALL = null;
        GEORGEJETSON_LASTCALL = LocalDateTime.of(2006, 4, 22, 0, 0);
        GEORGEJETSON_LASTLETTER = LocalDateTime.of(2008, 2, 23, 0, 0);

        GEORGEJETSON_HISTORY = new History();
        GEORGEJETSON_HISTORY.setHistoryDate(NEWDATE);
        GEORGEJETSON_HISTORY.setTaskTypeId(TaskType.EMAIL);
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

        BAMBIDEER_CONTACTID = Integer.valueOf(272072119);
        BAMBIDEER_FILEAS = "Deer, Bambi and Feline";
        BAMBIDEER_LASTACTIVITY = LocalDateTime.of(2008, 2, 23, 0, 0);
        BAMBIDEER_LASTPRECALL = LocalDateTime.of(2007, 11, 6, 0, 0);
        BAMBIDEER_CHALLENGESSINCELASTGIFT = Integer.valueOf(0);

        DONALDDUCK_CONTACTID = Integer.valueOf(608241759);
        RABBITRABBIT_CONTACTID = Integer.valueOf(408495102);
        NONEXISTENT_CONTACTID = Integer.valueOf(2);
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
     * Tests adding new email addresses
     */
    @Test
    public void addNewEmailAddress() throws TntDbException, SQLException {

        // Add unique email addresses to a contact
        for (int i = 0; i < 20; i++) {

            // Verify that testEmail doesn't already exist in the DB
            String testEmail = "testEmail" + i + "@gmail.com";
            assertEquals(null, ContactManager.getContactIdByEmail(testEmail));

            // Add email (adding every other one to spouse)
            ContactManager.addNewEmailAddress(testEmail, MRINCREDIBLE_CONTACTID, i % 2 == 0);

            // Verify that the email exists in the DB
            assertEquals(MRINCREDIBLE_CONTACTID, ContactManager.getContactIdByEmail(testEmail));
        }

        // Test adding where "Email" field is previously blank
        assertEquals(null, ContactManager.getContactIdByEmail("bambi@deer.com"));
        ContactManager.addNewEmailAddress("bambi@deer.com", BAMBIDEER_CONTACTID, true);
        Contact bambi = ContactManager.get(BAMBIDEER_CONTACTID);
        assertEquals("bambi@deer.com", bambi.getEmail());
        assertEquals(true, bambi.isEmailValid());

        // Test adding existing email
        Contact contact = ContactManager.get(MRINCREDIBLE_CONTACTID);
        String email = contact.getEmail();
        assertEquals(1, ContactManager.getContactsByEmailCount(email));
        try {
            ContactManager.addNewEmailAddress(email, MRINCREDIBLE_CONTACTID, true);
            fail("Shouldn't be able to add an email address to the same contact a second time");
        } catch (TntDbException e) {
        }
        try {
            ContactManager.addNewEmailAddress(email, MRINCREDIBLE_CONTACTID, false);
            fail("Shouldn't be able to add an email address to the same contact's spouse a second time");
        } catch (TntDbException e) {
        }
        try {
            ContactManager.addNewEmailAddress(email, DONALDDUCK_CONTACTID, true);
            fail("Shouldn't be able to add an email address to a different contact a second time");
        } catch (TntDbException e) {
        }

        // Test nulls
        try {
            ContactManager.addNewEmailAddress(null, MRINCREDIBLE_CONTACTID, true);
            fail("Shouldn't be able to add a null email address");
        } catch (TntDbException e) {
        }
        try {
            ContactManager.addNewEmailAddress(email, null, true);
            fail("Shouldn't be able to add an email address to a null contact");
        } catch (TntDbException e) {
        }
    }

    /**
     * Tests getting & creating contacts
     */
    @Test
    public void getAndCreateContact() throws TntDbException, SQLException {
        // Test getting null
        assertEquals(null, ContactManager.get(null));

        // Test creating null
        try {
            ContactManager.create(null);
            fail("Shouldn't be able to create null contact");
        } catch (TntDbException e) {
        }

        // Test getting non-existant
        assertEquals(null, ContactManager.get(NONEXISTENT_CONTACTID));

        // Test getting and creating normal
        Contact contact = ContactManager.get(MRINCREDIBLE_CONTACTID);
        try {
            ContactManager.create(contact);
            fail("Shouldn't be able to recreate same contact");
        } catch (TntDbException e) {
        }

        contact.setContactId(null); // Should create new!
        Integer newContactId = ContactManager.create(contact);
        Contact newContact = ContactManager.get(newContactId);

        newContact.setContactId(contact.getContactId());

        Field[] fields = Contact.class.getFields();
        for (Field field : fields) {
            try {
                if (!"log".equals(field.getName()))
                    assertEquals(true, field.get(contact).equals(field.get(newContact)));
            } catch (IllegalAccessException e) {
                fail("IllegalAccessException: " + e.getMessage());
            }
        }

    }

    /**
     * Tests getting the challenges since a contact's last gift.
     */
    @Test
    public void getChallengesSinceLastGift() throws TntDbException, SQLException {
        Integer challenges = ContactManager.getChallengesSinceLastGift(MRINCREDIBLE_CONTACTID);
        assertEquals(MRINCREDIBLE_CHALLENGESSINCELASTGIFT, challenges);
    }

    /**
     * Tests counting and returning contacts given an existing, unique email address
     */
    @Test
    public void getContactIdByEmailFound() throws TntDbException, SQLException {
        assertEquals(1, ContactManager.getContactsByEmailCount("dduck@disney.org"));
        assertEquals(DONALDDUCK_CONTACTID, ContactManager.getContactIdByEmail("dduck@disney.org"));
    }

    /**
     * Tests counting and returning contacts given an existing, unique email address using angle brackets.
     */
    @Test
    public void getContactIdByEmailFoundWithBrackets() throws TntDbException, SQLException {
        TntDb.getConnection().createStatement().executeUpdate(
            "UPDATE [Contact] SET [Email3] = '\"Bob Parr\" <parrb@metroinsurance.com>' WHERE [ContactID] = "
                + MRINCREDIBLE_CONTACTID);
        assertEquals(1, ContactManager.getContactsByEmailCount("parrb@metroinsurance.com"));
        assertEquals(MRINCREDIBLE_CONTACTID, ContactManager.getContactIdByEmail("parrb@metroinsurance.com"));
    }

    /**
     * Tests counting and returning contacts given an existing, unique email address using underscores.
     */
    @Test
    public void getContactIdByEmailFoundWithUnderscores() throws TntDbException, SQLException {
        TntDb.getConnection().createStatement().executeUpdate(
            "UPDATE [Contact] SET [Email3] = 'parr_b@metroinsurance.com' WHERE [ContactID] = "
                + MRINCREDIBLE_CONTACTID);
        TntDb.getConnection().createStatement().executeUpdate(
            "UPDATE [Contact] SET [Email3] = '______@metroinsurance.com' WHERE [ContactID] = "
                + GEORGEJETSON_CONTACTID);

        assertEquals(1, ContactManager.getContactsByEmailCount("parr_b@metroinsurance.com"));
        assertEquals(MRINCREDIBLE_CONTACTID, ContactManager.getContactIdByEmail("parr_b@metroinsurance.com"));
    }

    /**
     * Tests counting and returning contacts given an existing, non-unique email address
     */
    @Test
    public void getContactIdByEmailMultipleFound() throws TntDbException, SQLException {
        // Add duplicate email address to primary contact
        TntDb.getConnection().createStatement().executeUpdate(
            "UPDATE [Contact] SET [Email3] = 'dduck@disney.org' WHERE [ContactID] = " + MRINCREDIBLE_CONTACTID);

        assertEquals(2, ContactManager.getContactsByEmailCount("dduck@disney.org"));
        try {
            ContactManager.getContactIdByEmail("dduck@disney.org");
            fail("Multiple contacts should throw exception");
        } catch (TntDbException e) {
        }
    }

    /**
     * Tests counting and returning contacts given non-existent, null and partial email addresses
     */
    @Test
    public void getContactIdByEmailNotFound() throws TntDbException, SQLException {
        assertEquals(0, ContactManager.getContactsByEmailCount("nobody@nowhere.nope"));
        assertEquals(null, ContactManager.getContactIdByEmail("nobody@nowhere.nope"));
        assertEquals(null, ContactManager.getContactIdByEmail(null));
        assertEquals(0, ContactManager.getContactsByEmailCount("duck@disney.org")); // dduck@disney.org exists
        assertEquals(null, ContactManager.getContactIdByEmail("duck@disney.org"));
        assertEquals(0, ContactManager.getContactsByEmailCount("George Jetson")); // Not an email address but exists
        assertEquals(null, ContactManager.getContactIdByEmail("George Jetson"));
    }

    /**
     * Tests getting the contact list
     */
    @Test
    public void getContactList() throws TntDbException, SQLException {
        // Get the contact list
        ContactInfo[] contactList = ContactManager.getContactList();

        // Verify size
        assertEquals(59, contactList.length);

        // Spot check
        for (ContactInfo ci : contactList) {
            if (MRINCREDIBLE_CONTACTID == ci.getId())
                assertEquals(MRINCREDIBLE_FILEAS, ci.getName());
            else if (GEORGEJETSON_CONTACTID == ci.getId())
                assertEquals(GEORGEJETSON_FILEAS, ci.getName());
            else if (BAMBIDEER_CONTACTID == ci.getId())
                assertEquals(BAMBIDEER_FILEAS, ci.getName());
        }
    }

    /**
     * Tests getting contact names for contacts
     */
    @Test
    public void getFileAs() throws TntDbException, SQLException {
        assertEquals("Parr, Bob and Helen", ContactManager.getFileAs(MRINCREDIBLE_CONTACTID));
        assertEquals("Duck, Donald and Daisy", ContactManager.getFileAs(DONALDDUCK_CONTACTID));
        assertEquals(null, ContactManager.getFileAs(0));
    }

    /**
     * Tests retrieval of Contact's "Last Activity" date
     */
    @Test
    public void getLastActivityDate() throws TntDbException, SQLException {
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
    public void getLastChallengeDate() throws TntDbException, SQLException {
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
    public void getLastLetterDate() throws TntDbException, SQLException {
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
    public void getLastThankDate() throws TntDbException, SQLException {
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
     * Tests recalculation of a contact's challenges since the last gift
     */
    @Test
    public void recalculateChallengesSinceLastGift() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(
            BAMBIDEER_CHALLENGESSINCELASTGIFT.intValue(),
            ContactManager.getChallengesSinceLastGift(BAMBIDEER_CONTACTID));

        // Set number of challenges to 0
        TntDb.getConnection().createStatement().executeUpdate(
            String.format(
                "UPDATE [Contact] SET [ChallengesSinceLastGift] = 0 WHERE [ContactID] = %s",
                BAMBIDEER_CONTACTID));
        assertEquals(0, ContactManager.getChallengesSinceLastGift(BAMBIDEER_CONTACTID));

        // Recalculate
        ContactManager.recalculateChallengesSinceLastGift(BAMBIDEER_CONTACTID);

        // Verify that the number of challenges is correct
        assertEquals(
            BAMBIDEER_CHALLENGESSINCELASTGIFT.intValue(),
            ContactManager.getChallengesSinceLastGift(BAMBIDEER_CONTACTID));

        //
        // Test case of no last gift
        //
        assertEquals(null, MRINCREDIBLE_LASTGIFT);
        ContactManager.recalculateChallengesSinceLastGift(MRINCREDIBLE_CONTACTID);
        assertEquals(
            MRINCREDIBLE_CHALLENGESSINCELASTGIFT.intValue(),
            ContactManager.getChallengesSinceLastGift(MRINCREDIBLE_CONTACTID));
    }

    /**
     * Tests recalculating of all history data
     */
    @Test
    public void recalculateHistoryData() throws TntDbException, SQLException {
        // Set all the dates to null
        TntDb.getConnection().createStatement().executeUpdate(
            String.format(
                "UPDATE [Contact] SET [LastActivity] = NULL, [LastAppointment] = NULL, [LastCall] = NULL, "
                    + "[LastChallenge] = NULL, [LastLetter] = NULL, [LastPreCall] = NULL, [LastThank] = NULL, "
                    + "[LastVisit] = NULL WHERE [ContactId] = %s",
                GEORGEJETSON_CONTACTID));

        // Recalculate everything
        ContactManager.recalculateHistoryData(GEORGEJETSON_CONTACTID);

        // Verify the results
        assertEquals(GEORGEJETSON_LASTACTIVITY, ContactManager.getLastActivityDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTAPPOINTMENT, ContactManager.getLastAppointmentDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTCALL, ContactManager.getLastCallDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTCHALLENGE, ContactManager.getLastChallengeDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTLETTER, ContactManager.getLastLetterDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTPRECALL, ContactManager.getLastPreCallDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTTHANK, ContactManager.getLastThankDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTVISIT, ContactManager.getLastVisitDate(GEORGEJETSON_CONTACTID));
    }

    /**
     * Tests recalculation of a contact's last activity
     */
    @Test
    public void recalculateLastActivity() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(MRINCREDIBLE_LASTACTIVITY, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));

        // Remove last activity
        TntDb.getConnection().createStatement().executeUpdate(
            String.format("UPDATE [Contact] SET [LastActivity] = null WHERE [ContactID] = %s", MRINCREDIBLE_CONTACTID));
        assertEquals(null, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));

        // Recalculate
        ContactManager.recalculateLastActivityDate(MRINCREDIBLE_CONTACTID);

        // Verify that last activity is correct
        assertEquals(MRINCREDIBLE_LASTACTIVITY, ContactManager.getLastActivityDate(MRINCREDIBLE_CONTACTID));

        //
        // Test case of no last activity
        //

        // Verify initial state
        assertEquals(null, ContactManager.getLastActivityDate(RABBITRABBIT_CONTACTID));

        // Recalculate
        ContactManager.recalculateLastActivityDate(RABBITRABBIT_CONTACTID);

        // Verify that last activity is correct
        assertEquals(null, ContactManager.getLastActivityDate(RABBITRABBIT_CONTACTID));
    }

    /**
     * Tests recalculation of a contact's last appointment
     */
    @Test
    public void recalculateLastAppointment() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(GEORGEJETSON_LASTAPPOINTMENT, ContactManager.getLastAppointmentDate(GEORGEJETSON_CONTACTID));

        // Remove last appointment
        TntDb.getConnection().createStatement().executeUpdate(
            String.format(
                "UPDATE [Contact] SET [LastAppointment] = null WHERE [ContactID] = %s",
                GEORGEJETSON_CONTACTID));
        assertEquals(null, ContactManager.getLastAppointmentDate(GEORGEJETSON_CONTACTID));

        // Recalculate
        ContactManager.recalculateLastAppointmentDate(GEORGEJETSON_CONTACTID);

        // Verify that last appointment is correct
        assertEquals(GEORGEJETSON_LASTAPPOINTMENT, ContactManager.getLastAppointmentDate(GEORGEJETSON_CONTACTID));
    }

    /**
     * Tests recalculation of a contact's last call
     */
    @Test
    public void recalculateLastCall() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(GEORGEJETSON_LASTCALL, ContactManager.getLastCallDate(GEORGEJETSON_CONTACTID));

        // Remove last call
        TntDb.getConnection().createStatement().executeUpdate(
            String.format("UPDATE [Contact] SET [LastCall] = null WHERE [ContactID] = %s", GEORGEJETSON_CONTACTID));
        assertEquals(null, ContactManager.getLastCallDate(GEORGEJETSON_CONTACTID));

        // Recalculate
        ContactManager.recalculateLastCallDate(GEORGEJETSON_CONTACTID);

        // Verify that last call is correct
        assertEquals(GEORGEJETSON_LASTCALL, ContactManager.getLastCallDate(GEORGEJETSON_CONTACTID));
    }

    /**
     * Tests recalculation of a contact's last challenge
     */
    @Test
    public void recalculateLastChallenge() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(GEORGEJETSON_LASTCHALLENGE, ContactManager.getLastChallengeDate(GEORGEJETSON_CONTACTID));

        // Remove last challenge
        TntDb.getConnection().createStatement().executeUpdate(
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
    public void recalculateLastLetter() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(MRINCREDIBLE_LASTLETTER, ContactManager.getLastLetterDate(MRINCREDIBLE_CONTACTID));

        // Remove last letter
        TntDb.getConnection().createStatement().executeUpdate(
            String.format("UPDATE [Contact] SET [LastLetter] = null WHERE [ContactID] = %s", MRINCREDIBLE_CONTACTID));

        assertEquals(null, ContactManager.getLastLetterDate(MRINCREDIBLE_CONTACTID));

        // Recalculate
        ContactManager.recalculateLastLetterDate(MRINCREDIBLE_CONTACTID);

        // Verify that last letter is correct
        assertEquals(MRINCREDIBLE_LASTLETTER, ContactManager.getLastLetterDate(MRINCREDIBLE_CONTACTID));
    }

    /**
     * Tests recalculation of a contact's last pre-call
     */
    @Test
    public void recalculateLastPreCall() throws TntDbException, SQLException {
        // Note: we're using Bambi here, not George!

        // Verify initial state
        assertEquals(BAMBIDEER_LASTPRECALL, ContactManager.getLastPreCallDate(BAMBIDEER_CONTACTID));

        // Remove last pre-call
        TntDb.getConnection().createStatement().executeUpdate(
            String.format("UPDATE [Contact] SET [LastPreCall] = null WHERE [ContactID] = %s", BAMBIDEER_CONTACTID));
        assertEquals(null, ContactManager.getLastPreCallDate(BAMBIDEER_CONTACTID));

        // Recalculate
        ContactManager.recalculateLastPreCallDate(BAMBIDEER_CONTACTID);

        // Verify that last pre-call is correct
        assertEquals(BAMBIDEER_LASTPRECALL, ContactManager.getLastPreCallDate(BAMBIDEER_CONTACTID));
    }

    /**
     * Tests recalculation of a contact's last thank
     */
    @Test
    public void recalculateLastThank() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(GEORGEJETSON_LASTTHANK, ContactManager.getLastThankDate(GEORGEJETSON_CONTACTID));

        // Remove last thank
        TntDb.getConnection().createStatement().executeUpdate(
            String.format("UPDATE [Contact] SET [LastThank] = null WHERE [ContactID] = %s", GEORGEJETSON_CONTACTID));
        assertEquals(null, ContactManager.getLastThankDate(GEORGEJETSON_CONTACTID));

        // Recalculate
        ContactManager.recalculateLastThankDate(GEORGEJETSON_CONTACTID);

        // Verify that last challenge is correct
        assertEquals(GEORGEJETSON_LASTTHANK, ContactManager.getLastThankDate(GEORGEJETSON_CONTACTID));
    }

    /**
     * Tests recalculation of a contact's last visit
     */
    @Test
    public void recalculateLastVisit() throws TntDbException, SQLException {
        // Verify initial state
        assertEquals(GEORGEJETSON_LASTVISIT, ContactManager.getLastVisitDate(GEORGEJETSON_CONTACTID));

        // Remove last visit
        TntDb.getConnection().createStatement().executeUpdate(
            String.format("UPDATE [Contact] SET [LastVisit] = null WHERE [ContactID] = %s", GEORGEJETSON_CONTACTID));
        assertEquals(null, ContactManager.getLastVisitDate(GEORGEJETSON_CONTACTID));

        // Recalculate
        ContactManager.recalculateLastVisitDate(GEORGEJETSON_CONTACTID);

        // Verify that last challenge is correct
        assertEquals(GEORGEJETSON_LASTVISIT, ContactManager.getLastVisitDate(GEORGEJETSON_CONTACTID));
    }

    /**
     * Test making sure we can't end up in an infinite loop of updating LastX and then recalculating LastX
     */
    @Test
    public void recalculateWithoutInfiniteLoop() throws TntDbException, SQLException {
        // George Jetson has no precall.
        // If we try to recalculate the precall, we must not end up in an infinite loop!

        // Verify initial state
        assertEquals(null, ContactManager.getLastPreCallDate(GEORGEJETSON_CONTACTID));

        // Recalculate -- mustn't enter infinite loop!
        ContactManager.recalculateLastPreCallDate(GEORGEJETSON_CONTACTID);

        // Verify that last pre-call is still null
        assertEquals(null, ContactManager.getLastPreCallDate(GEORGEJETSON_CONTACTID));
    }

    @After
    public void rollback() throws TntDbException {
        TntDb.rollback();
    }

    /**
     * Tests updating of a contact's last activity
     */
    @Test
    public void updateLastActivityDate() throws SQLException {
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
     * Tests updating of a contact's last appointment
     */
    @Test
    public void updateLastAppointmentDate() throws SQLException {
        // Verify initial state
        assertEquals(GEORGEJETSON_LASTAPPOINTMENT, ContactManager.getLastAppointmentDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTACTIVITY, ContactManager.getLastActivityDate(GEORGEJETSON_CONTACTID));

        // Update last appointment
        ContactManager.updateLastAppointmentDate(GEORGEJETSON_CONTACTID, NEWDATE);

        // Verify that last appointment and last activity are correct
        assertEquals(NEWDATE, ContactManager.getLastAppointmentDate(GEORGEJETSON_CONTACTID));
        assertEquals(NEWDATE, ContactManager.getLastActivityDate(GEORGEJETSON_CONTACTID));

        // Remove last appointment; should force a recalculation
        ContactManager.updateLastAppointmentDate(GEORGEJETSON_CONTACTID, null);
        assertEquals(GEORGEJETSON_LASTAPPOINTMENT, ContactManager.getLastAppointmentDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTACTIVITY, ContactManager.getLastActivityDate(GEORGEJETSON_CONTACTID));
    }

    /**
     * Tests updating of a contact's last call
     */
    @Test
    public void updateLastCallDate() throws SQLException {
        // Verify initial state
        assertEquals(GEORGEJETSON_LASTCALL, ContactManager.getLastCallDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTACTIVITY, ContactManager.getLastActivityDate(GEORGEJETSON_CONTACTID));

        // Update last call
        ContactManager.updateLastCallDate(GEORGEJETSON_CONTACTID, NEWDATE);

        // Verify that last call and last activity are correct
        assertEquals(NEWDATE, ContactManager.getLastCallDate(GEORGEJETSON_CONTACTID));
        assertEquals(NEWDATE, ContactManager.getLastActivityDate(GEORGEJETSON_CONTACTID));

        // Remove last call; should force a recalculation
        ContactManager.updateLastCallDate(GEORGEJETSON_CONTACTID, null);
        assertEquals(GEORGEJETSON_LASTCALL, ContactManager.getLastCallDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTACTIVITY, ContactManager.getLastActivityDate(GEORGEJETSON_CONTACTID));
    }

    /**
     * Tests updating of a contact's last challenge
     */
    @Test
    public void updateLastChallengeDate() throws SQLException {
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
    public void updateLastEdit() throws SQLException {
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
    public void updateLastLetterDate() throws SQLException {
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
     * Tests updating of a contact's last pre-call
     */
    @Test
    public void updateLastPreCallDate() throws SQLException {
        // Note: we're using Bambi here, not George

        // Verify initial state
        assertEquals(BAMBIDEER_LASTPRECALL, ContactManager.getLastPreCallDate(BAMBIDEER_CONTACTID));
        assertEquals(BAMBIDEER_LASTACTIVITY, ContactManager.getLastActivityDate(BAMBIDEER_CONTACTID));

        // Update last pre-call
        ContactManager.updateLastPreCallDate(BAMBIDEER_CONTACTID, NEWDATE);

        // Verify that last pre-call and last activity are correct
        assertEquals(NEWDATE, ContactManager.getLastPreCallDate(BAMBIDEER_CONTACTID));
        assertEquals(NEWDATE, ContactManager.getLastActivityDate(BAMBIDEER_CONTACTID));

        // Remove last pre-call; should force a recalculation
        ContactManager.updateLastPreCallDate(BAMBIDEER_CONTACTID, null);
        assertEquals(BAMBIDEER_LASTPRECALL, ContactManager.getLastPreCallDate(BAMBIDEER_CONTACTID));
        assertEquals(BAMBIDEER_LASTACTIVITY, ContactManager.getLastActivityDate(BAMBIDEER_CONTACTID));
    }

    /**
     * Tests updating of a contact's last thank
     */
    @Test
    public void updateLastThankDate() throws SQLException {
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

    /**
     * Tests updating of a contact's last visit
     */
    @Test
    public void updateLastVisitDate() throws SQLException {
        // Verify initial state
        assertEquals(GEORGEJETSON_LASTVISIT, ContactManager.getLastVisitDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTACTIVITY, ContactManager.getLastActivityDate(GEORGEJETSON_CONTACTID));

        // Update last visit
        ContactManager.updateLastVisitDate(GEORGEJETSON_CONTACTID, NEWDATE);

        // Verify that last visit and last activity are correct
        assertEquals(NEWDATE, ContactManager.getLastVisitDate(GEORGEJETSON_CONTACTID));
        assertEquals(NEWDATE, ContactManager.getLastActivityDate(GEORGEJETSON_CONTACTID));

        // Remove last visit; should force a recalculation
        ContactManager.updateLastVisitDate(GEORGEJETSON_CONTACTID, null);
        assertEquals(GEORGEJETSON_LASTVISIT, ContactManager.getLastVisitDate(GEORGEJETSON_CONTACTID));
        assertEquals(GEORGEJETSON_LASTACTIVITY, ContactManager.getLastActivityDate(GEORGEJETSON_CONTACTID));
    }
}
