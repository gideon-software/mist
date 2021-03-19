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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.exceptions.TntDbException;
import com.gideonsoftware.mist.tntapi.entities.Contact;
import com.gideonsoftware.mist.tntapi.entities.ContactInfo;
import com.gideonsoftware.mist.tntapi.entities.History;
import com.gideonsoftware.mist.tntapi.entities.TaskType;
import com.gideonsoftware.mist.util.Util;

/**
 *
 */
public class ContactManager {
    private static Logger log = LogManager.getLogger();

    // Contact's "Last" types
    public final static String LASTTYPE_ACTIVITY = "Activity";
    public final static String LASTTYPE_APPOINTMENT = "Appointment";
    public final static String LASTTYPE_CALL = "Call";
    public final static String LASTTYPE_CHALLENGE = "Challenge";
    public final static String LASTTYPE_EDIT = "Edit";
    public final static String LASTTYPE_LETTER = "Letter";
    public final static String LASTTYPE_PRECALL = "PreCall";
    public final static String LASTTYPE_THANK = "Thank";
    public final static String LASTTYPE_VISIT = "Visit";
    public final static String LASTTYPE_GIFT = "GiftDate"; // The "Date" part is in the Tnt column name

    private ContactManager() {
    }

    /**
     * Adds a new email address to the specified contact.
     * <p>
     * Commits changes to the Tnt database if {@code useCommit} is true.
     *
     * @param email
     *            the email address to add
     * @param contactId
     *            the contact's ID
     * @param usePrimaryContact
     *            true to add the email address to the primary contact; false to add it to the spouse
     * @throws SQLException
     *             if there is a database access problem
     * @throws TntDbException
     *             if email or contactId was null
     *             if the email already exists in the Tnt database
     *             if a rollback was required but failed
     */
    public static void addNewEmailAddress(
        String email,
        Integer contactId,
        boolean usePrimaryContact) throws SQLException, TntDbException {
        log.trace("addNewEmailAddress({},{},{})", email, contactId, usePrimaryContact);

        if (email == null)
            throw new TntDbException("Email address not supplied");

        if (contactId == null)
            throw new TntDbException("Contact not supplied");

        if (getContactsByEmailCount(email) != 0)
            throw new TntDbException("Email address already exists in the Tnt database");

        Contact contact = get(contactId);
        String emailField = ""; //
        boolean addToExisting = false;

        if (usePrimaryContact) {
            if (contact.getEmail1().isBlank())
                emailField = "Email1";
            else if (contact.getEmail2().isBlank())
                emailField = "Email2";
            else if (contact.getEmail3().isBlank())
                emailField = "Email3";
            else
                addToExisting = true;
        } else {
            if (contact.getSpouseEmail1().isBlank())
                emailField = "SpouseEmail1";
            else if (contact.getSpouseEmail2().isBlank())
                emailField = "SpouseEmail2";
            else if (contact.getSpouseEmail3().isBlank())
                emailField = "SpouseEmail3";
            else
                addToExisting = true;
        }

        if (addToExisting) {
            // All our email slots are full; append this one to the end of an existing one
            if (usePrimaryContact) {
                emailField = "Email3";
                email = String.format("%s,%s", contact.getEmail3(), email);
            } else {
                emailField = "SpouseEmail3";
                email = String.format("%s,%s", contact.getSpouseEmail3(), email);
            }
        }

        try {
            String query = String.format(
                "UPDATE [Contact] SET [%1$s] = ?, [%1$sIsValid] = -1 WHERE [ContactId] = ?",
                emailField);
            PreparedStatement stmt = TntDb.getConnection().prepareStatement(query);
            stmt.setString(1, email);
            stmt.setInt(2, contactId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            TntDb.rollback();
            throw e;
        }

        // Also update "Email" field if needed
        if (contact.getEmail().isBlank()) {
            try {
                String query = "UPDATE [Contact] SET [Email] = ?, [EmailIsValid] = -1 WHERE [ContactId] = ?";
                PreparedStatement stmt = TntDb.getConnection().prepareStatement(query);
                stmt.setString(1, email);
                stmt.setInt(2, contactId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                TntDb.rollback();
                throw e;
            }
        }

        TntDb.commit();
    }

    /**
     * Creates a new contact in the Tnt database.
     * <p>
     * Commits changes to the Tnt database if {@code useCommit} is true.
     *
     * @param contact
     *            The contact to create
     * @throws TntDbException
     *             if contact is null,
     *             if contact's contactId is not null (since we shouldn't be adding an existing contact),
     *             if contact's name fields are null or empty (i.e. FileAs, FullName, Greeting, Salutation, ShortName,
     *             FirstName -or- LastName)
     * @throws SQLException
     *             if there is a database access problem
     * @return the created contact's ID
     */
    public static Integer create(Contact contact) throws TntDbException, SQLException {
        log.trace("create({})", contact);

        if (contact == null)
            throw new TntDbException("Contact not supplied");

        if (contact.getContactId() != null)
            throw new TntDbException("ContactId must be null");

        if (contact.getFileAs() == null || contact.getFileAs().isBlank())
            throw new TntDbException("FileAs is not supplied for contact");
        if (contact.getFullName() == null || contact.getFullName().isBlank())
            throw new TntDbException("FullName is not supplied for contact");
        if (contact.getGreeting() == null || contact.getGreeting().isBlank())
            throw new TntDbException("Greeting is not supplied for contact");
        if (contact.getSalutation() == null || contact.getSalutation().isBlank())
            throw new TntDbException("Salutation is not supplied for contact");
        if (contact.getShortName() == null || contact.getShortName().isBlank())
            throw new TntDbException("ShortName is not supplied for contact");
        if ((contact.getFirstName() == null || contact.getFirstName().isBlank())
            && (contact.getLastName() == null || contact.getLastName().isBlank()))
            throw new TntDbException("Either FirstName or LastName must be supplied for contact");

        contact.setContactId(TntDb.getAvailableId(TntDb.TABLE_CONTACT, true));
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Object[][] colValuePairs = {
            { "ContactID", contact.getContactId(), java.sql.Types.INTEGER },
            { "LastEdit", now, java.sql.Types.TIMESTAMP },
            { "CreatedDate", now, java.sql.Types.DATE },
            { "RejectedDuplicateContactIDs", contact.getRejectedDuplicateContactIDs(), java.sql.Types.LONGVARCHAR },
            { "FileAs", contact.getFileAs(), java.sql.Types.VARCHAR, 75 },
            { "FileAsIsCustom", contact.isFileAsCustom(), java.sql.Types.BOOLEAN },
            { "FullName", contact.getFullName(), java.sql.Types.LONGVARCHAR },
            { "FullNameIsCustom", contact.isFullNameCustom(), java.sql.Types.BOOLEAN },
            { "Greeting", contact.getGreeting(), java.sql.Types.LONGVARCHAR },
            { "GreetingIsCustom", contact.isGreetingCustom(), java.sql.Types.BOOLEAN },
            { "Salutation", contact.getSalutation(), java.sql.Types.LONGVARCHAR },
            { "SalutationIsCustom", contact.isSalutationCustom(), java.sql.Types.BOOLEAN },
            { "ShortName", contact.getShortName(), java.sql.Types.VARCHAR },
            { "ShortNameIsCustom", contact.isShortNameCustom(), java.sql.Types.BOOLEAN },
            { "MailingAddressBlock", contact.getMailingAddressBlock(), java.sql.Types.VARCHAR },
            { "MailingAddressIsDeliverable", contact.isMailingAddressDeliverable(), java.sql.Types.BOOLEAN },
            { "Phone", contact.getPhone(), java.sql.Types.VARCHAR },
            { "PhoneIsValid", contact.isPhoneValid(), java.sql.Types.BOOLEAN },
            { "Email", contact.getEmail(), java.sql.Types.VARCHAR },
            { "EmailIsValid", contact.isEmailValid(), java.sql.Types.BOOLEAN },
            { "IsOrganization", contact.isOrganization(), java.sql.Types.BOOLEAN },
            { "OrganizationName", contact.getOrganizationName(), java.sql.Types.VARCHAR, 50 },
            { "OrgContactPerson", contact.getOrgContactPerson(), java.sql.Types.VARCHAR, 50 },
            { "Title", contact.getTitle(), java.sql.Types.VARCHAR, 25 },
            { "FirstName", contact.getFirstName(), java.sql.Types.VARCHAR, 25 },
            { "MiddleName", contact.getMiddleName(), java.sql.Types.VARCHAR, 25 },
            { "LastName", contact.getLastName(), java.sql.Types.VARCHAR, 50 },
            { "Suffix", contact.getSuffix(), java.sql.Types.VARCHAR, 25 },
            { "SpouseTitle", contact.getSpouseTitle(), java.sql.Types.VARCHAR, 25 },
            { "SpouseFirstName", contact.getSpouseFirstName(), java.sql.Types.VARCHAR, 25 },
            { "SpouseMiddleName", contact.getSpouseMiddleName(), java.sql.Types.VARCHAR, 25 },
            { "SpouseLastName", contact.getSpouseLastName(), java.sql.Types.VARCHAR, 50 },
            { "Deceased", contact.isDeceased(), java.sql.Types.BOOLEAN },
            { "MailingAddressType", contact.getMailingAddressType(), java.sql.Types.INTEGER },
            { "MailingStreetAddress", contact.getMailingStreetAddress(), java.sql.Types.VARCHAR },
            { "MailingCity", contact.getMailingCity(), java.sql.Types.VARCHAR, 50 },
            { "MailingState", contact.getMailingState(), java.sql.Types.VARCHAR, 50 },
            { "MailingPostalCode", contact.getMailingPostalCode(), java.sql.Types.VARCHAR, 25 },
            { "MailingCountry", contact.getMailingCountry(), java.sql.Types.VARCHAR, 50 },
            { "HomeStreetAddress", contact.getHomeStreetAddress(), java.sql.Types.VARCHAR },
            { "HomeCity", contact.getHomeCity(), java.sql.Types.VARCHAR, 50 },
            { "HomeState", contact.getHomeState(), java.sql.Types.VARCHAR, 50 },
            { "HomePostalCode", contact.getHomePostalCode(), java.sql.Types.VARCHAR, 25 },
            { "HomeCountryID", contact.getHomeCountryId(), java.sql.Types.INTEGER },
            { "HomeCountry", contact.getHomeCountry(), java.sql.Types.VARCHAR, 50 },
            { "HomeAddressIsDeliverable", contact.isHomeAddressDeliverable(), java.sql.Types.BOOLEAN },
            { "HomeAddressBlock", contact.getHomeAddressBlock(), java.sql.Types.VARCHAR },
            { "HomeAddressBlockIsCustom", contact.isHomeAddressBlockCustom(), java.sql.Types.BOOLEAN },
            { "OtherStreetAddress", contact.getOtherStreetAddress(), java.sql.Types.VARCHAR },
            { "OtherCity", contact.getOtherCity(), java.sql.Types.VARCHAR, 50 },
            { "OtherState", contact.getOtherState(), java.sql.Types.VARCHAR, 50 },
            { "OtherPostalCode", contact.getOtherPostalCode(), java.sql.Types.VARCHAR, 25 },
            { "OtherCountryID", contact.getOtherCountryId(), java.sql.Types.INTEGER },
            { "OtherCountry", contact.getOtherCountry(), java.sql.Types.VARCHAR, 50 },
            { "OtherAddressIsDeliverable", contact.isOtherAddressDeliverable(), java.sql.Types.BOOLEAN },
            { "OtherAddressBlock", contact.getOtherAddressBlock(), java.sql.Types.VARCHAR },
            { "OtherAddressBlockIsCustom", contact.isOtherAddressBlockCustom(), java.sql.Types.BOOLEAN },
            { "BusinessName", contact.getBusinessName(), java.sql.Types.VARCHAR },
            { "BusinessStreetAddress", contact.getBusinessStreetAddress(), java.sql.Types.VARCHAR },
            { "BusinessCity", contact.getBusinessCity(), java.sql.Types.VARCHAR, 50 },
            { "BusinessState", contact.getBusinessState(), java.sql.Types.VARCHAR, 50 },
            { "BusinessPostalCode", contact.getBusinessPostalCode(), java.sql.Types.VARCHAR, 25 },
            { "BusinessCountryID", contact.getBusinessCountryId(), java.sql.Types.INTEGER },
            { "BusinessCountry", contact.getBusinessCountry(), java.sql.Types.VARCHAR, 50 },
            { "BusinessAddressIsDeliverable", contact.isBusinessAddressDeliverable(), java.sql.Types.BOOLEAN },
            { "BusinessAddressBlock", contact.getBusinessAddressBlock(), java.sql.Types.VARCHAR },
            { "BusinessAddressBlockIsCustom", contact.isBusinessAddressBlockCustom(), java.sql.Types.BOOLEAN },
            { "SpouseBusinessName", contact.getSpouseBusinessName(), java.sql.Types.VARCHAR },
            { "SpouseBusinessStreetAddress", contact.getSpouseBusinessStreetAddress(), java.sql.Types.VARCHAR },
            { "SpouseBusinessCity", contact.getSpouseBusinessCity(), java.sql.Types.VARCHAR, 50 },
            { "SpouseBusinessState", contact.getSpouseBusinessState(), java.sql.Types.VARCHAR, 50 },
            { "SpouseBusinessPostalCode", contact.getSpouseBusinessPostalCode(), java.sql.Types.VARCHAR, 25 },
            { "SpouseBusinessCountryID", contact.getSpouseBusinessCountryId(), java.sql.Types.INTEGER },
            { "SpouseBusinessCountry", contact.getSpouseBusinessCountry(), java.sql.Types.VARCHAR, 50 },
            {
                "SpouseBusinessAddressIsDeliverable",
                contact.isSpouseBusinessAddressDeliverable(),
                java.sql.Types.BOOLEAN },
            { "SpouseBusinessAddressBlock", contact.getSpouseBusinessAddressBlock(), java.sql.Types.VARCHAR },
            {
                "SpouseBusinessAddressBlockIsCustom",
                contact.isSpouseBusinessAddressBlockCustom(),
                java.sql.Types.BOOLEAN },
            { "PreferredPhoneType", contact.getPreferredPhoneType(), java.sql.Types.INTEGER },
            { "PhoneIsValidMask", contact.getPhoneIsValidMask(), java.sql.Types.INTEGER },
            { "PhoneCountryIDs", contact.getPhoneCountryIds(), java.sql.Types.VARCHAR },
            { "HomePhone", contact.getHomePhone(), java.sql.Types.VARCHAR },
            { "HomePhone2", contact.getHomePhone2(), java.sql.Types.VARCHAR },
            { "HomeFax", contact.getHomeFax(), java.sql.Types.VARCHAR },
            { "OtherPhone", contact.getOtherPhone(), java.sql.Types.VARCHAR },
            { "OtherFax", contact.getOtherFax(), java.sql.Types.VARCHAR },
            { "BusinessPhone", contact.getBusinessPhone(), java.sql.Types.VARCHAR },
            { "BusinessPhone2", contact.getBusinessPhone2(), java.sql.Types.VARCHAR },
            { "BusinessFax", contact.getBusinessFax(), java.sql.Types.VARCHAR },
            { "CompanyMainPhone", contact.getCompanyMainPhone(), java.sql.Types.VARCHAR },
            { "MobilePhone", contact.getMobilePhone(), java.sql.Types.VARCHAR },
            { "MobilePhone2", contact.getMobilePhone2(), java.sql.Types.VARCHAR },
            { "PagerNumber", contact.getPagerNumber(), java.sql.Types.VARCHAR },
            { "SpouseBusinessPhone", contact.getSpouseBusinessPhone(), java.sql.Types.VARCHAR },
            { "SpouseBusinessPhone2", contact.getSpouseBusinessPhone2(), java.sql.Types.VARCHAR },
            { "SpouseBusinessFax", contact.getSpouseBusinessFax(), java.sql.Types.VARCHAR },
            { "SpouseCompanyMainPhone", contact.getSpouseCompanyMainPhone(), java.sql.Types.VARCHAR },
            { "SpouseMobilePhone", contact.getSpouseMobilePhone(), java.sql.Types.VARCHAR },
            { "SpouseMobilePhone2", contact.getSpouseMobilePhone2(), java.sql.Types.VARCHAR },
            { "SpousePagerNumber", contact.getSpousePagerNumber(), java.sql.Types.VARCHAR },
            { "PreferredEmailTypes", contact.getPreferredEmailTypes(), java.sql.Types.INTEGER },
            { "EmailLabels", contact.getEmailLabels(), java.sql.Types.VARCHAR },
            { "Email1", contact.getEmail1(), java.sql.Types.VARCHAR },
            { "Email2", contact.getEmail2(), java.sql.Types.VARCHAR },
            { "Email3", contact.getEmail3(), java.sql.Types.VARCHAR },
            { "Email1IsValid", contact.isEmail1Valid(), java.sql.Types.BOOLEAN },
            { "Email2IsValid", contact.isEmail2Valid(), java.sql.Types.BOOLEAN },
            { "Email3IsValid", contact.isEmail3Valid(), java.sql.Types.BOOLEAN },
            { "EmailCustomGreeting", contact.getEmailCustomGreeting(), java.sql.Types.VARCHAR },
            { "EmailCustomSalutation", contact.getEmailCustomSalutation(), java.sql.Types.VARCHAR },
            { "SpouseEmail1", contact.getSpouseEmail1(), java.sql.Types.VARCHAR },
            { "SpouseEmail2", contact.getSpouseEmail2(), java.sql.Types.VARCHAR },
            { "SpouseEmail3", contact.getSpouseEmail3(), java.sql.Types.VARCHAR },
            { "SpouseEmail1IsValid", contact.isSpouseEmail1Valid(), java.sql.Types.BOOLEAN },
            { "SpouseEmail2IsValid", contact.isSpouseEmail2Valid(), java.sql.Types.BOOLEAN },
            { "SpouseEmail3IsValid", contact.isSpouseEmail3Valid(), java.sql.Types.BOOLEAN },
            { "SpouseEmailCustomGreeting", contact.getSpouseEmailCustomGreeting(), java.sql.Types.VARCHAR },
            { "SpouseEmailCustomSalutation", contact.getSpouseEmailCustomSalutation(), java.sql.Types.VARCHAR },
            { "WebPage1", contact.getWebPage1(), java.sql.Types.VARCHAR },
            { "WebPage2", contact.getWebPage2(), java.sql.Types.VARCHAR },
            { "VoiceSkype", contact.getVoiceSkype(), java.sql.Types.VARCHAR },
            { "IMAddress", contact.getImAddress(), java.sql.Types.VARCHAR },
            { "SocialWeb1", contact.getSocialWeb1(), java.sql.Types.VARCHAR },
            { "SocialWeb2", contact.getSocialWeb2(), java.sql.Types.VARCHAR },
            { "SocialWeb3", contact.getSocialWeb3(), java.sql.Types.VARCHAR },
            { "SocialWeb4", contact.getSocialWeb4(), java.sql.Types.VARCHAR },
            { "SpouseWebPage1", contact.getSpouseWebPage1(), java.sql.Types.VARCHAR },
            { "SpouseWebPage2", contact.getSpouseWebPage2(), java.sql.Types.VARCHAR },
            { "SpouseVoiceSkype", contact.getSpouseVoiceSkype(), java.sql.Types.VARCHAR },
            { "SpouseIMAddress", contact.getSpouseImAddress(), java.sql.Types.VARCHAR },
            { "SpouseSocialWeb1", contact.getSpouseSocialWeb1(), java.sql.Types.VARCHAR },
            { "SpouseSocialWeb2", contact.getSpouseSocialWeb2(), java.sql.Types.VARCHAR },
            { "SpouseSocialWeb3", contact.getSpouseSocialWeb3(), java.sql.Types.VARCHAR },
            { "SpouseSocialWeb4", contact.getSpouseSocialWeb4(), java.sql.Types.VARCHAR },
            { "NotesAsRTF", contact.getNotesAsRtf(), java.sql.Types.VARCHAR },
            { "Notes", contact.getNotes(), java.sql.Types.VARCHAR },
            { "FamilySideID", contact.getFamilySideID(), java.sql.Types.INTEGER },
            { "FamilyLevelID", contact.getFamilyLevelID(), java.sql.Types.INTEGER },
            { "Children", contact.getChildren(), java.sql.Types.VARCHAR },
            { "Interests", contact.getInterests(), java.sql.Types.VARCHAR },
            { "Nickname", contact.getNickname(), java.sql.Types.VARCHAR },
            { "Profession", contact.getProfession(), java.sql.Types.VARCHAR },
            { "SpouseInterests", contact.getSpouseInterests(), java.sql.Types.VARCHAR },
            { "SpouseNickname", contact.getSpouseNickname(), java.sql.Types.VARCHAR },
            { "SpouseProfession", contact.getSpouseProfession(), java.sql.Types.VARCHAR },
            { "AnniversaryMonth", contact.getAnniversaryMonth(), java.sql.Types.INTEGER },
            { "AnniversaryDay", contact.getAnniversaryDay(), java.sql.Types.INTEGER },
            { "AnniversaryYear", contact.getAnniversaryYear(), java.sql.Types.INTEGER },
            { "BirthdayMonth", contact.getBirthdayMonth(), java.sql.Types.INTEGER },
            { "BirthdayDay", contact.getBirthdayDay(), java.sql.Types.INTEGER },
            { "BirthdayYear", contact.getBirthdayYear(), java.sql.Types.INTEGER },
            { "SpouseBirthdayMonth", contact.getSpouseBirthdayMonth(), java.sql.Types.INTEGER },
            { "SpouseBirthdayDay", contact.getSpouseBirthdayDay(), java.sql.Types.INTEGER },
            { "SpouseBirthdayYear", contact.getSpouseBirthdayYear(), java.sql.Types.INTEGER },
            { "Categories", contact.getCategories(), java.sql.Types.VARCHAR },
            { "User1", contact.getUser1(), java.sql.Types.VARCHAR },
            { "User2", contact.getUser2(), java.sql.Types.VARCHAR },
            { "User3", contact.getUser3(), java.sql.Types.VARCHAR },
            { "User4", contact.getUser4(), java.sql.Types.VARCHAR },
            { "User5", contact.getUser5(), java.sql.Types.VARCHAR },
            { "User6", contact.getUser6(), java.sql.Types.VARCHAR },
            { "User7", contact.getUser7(), java.sql.Types.VARCHAR },
            { "User8", contact.getUser8(), java.sql.Types.VARCHAR },
            { "UserStatus", contact.getUserStatus(), java.sql.Types.VARCHAR },
            { "MapAddressType", contact.getMapAddressType(), java.sql.Types.INTEGER },
            { "MapLat", contact.getMapLat(), java.sql.Types.INTEGER },
            { "MapLng", contact.getMapLng(), java.sql.Types.INTEGER },
            { "MapStatus", contact.getMapStatus(), java.sql.Types.VARCHAR },
            { "PledgeAmount", TntDb.formatDbCurrency(contact.getPledgeAmount()), java.sql.Types.VARCHAR },
            { "PledgeFrequencyID", contact.getPledgeFrequencyId(), java.sql.Types.INTEGER },
            { "PledgeReceived", contact.isPledgeReceived(), java.sql.Types.BOOLEAN },
            { "PledgeStartDate", contact.getPledgeStartDate(), java.sql.Types.DATE },
            { "PledgeCurrencyID", contact.getPledgeCurrencyId(), java.sql.Types.INTEGER },
            { "ReferredBy", contact.getReferredBy(), java.sql.Types.VARCHAR },
            { "ReferredByList", contact.getReferredByList(), java.sql.Types.VARCHAR },
            { "MPDPhaseID", contact.getMpdPhaseId(), java.sql.Types.INTEGER },
            { "FundRepID", contact.getFundRepId(), java.sql.Types.INTEGER },
            { "NextAsk", contact.getNextAsk(), java.sql.Types.DATE },
            { "NextAskAmount", TntDb.formatDbCurrency(contact.getNextAskAmount()), java.sql.Types.VARCHAR },
            {
                "EstimatedAnnualCapacity",
                TntDb.formatDbCurrency(contact.getEstimatedAnnualCapacity()),
                java.sql.Types.VARCHAR },
            { "NeverAsk", contact.isNeverAsk(), java.sql.Types.BOOLEAN },
            { "Region", contact.getRegion(), java.sql.Types.VARCHAR },
            { "LikelyToGiveID", contact.getLikelyToGiveId(), java.sql.Types.INTEGER },
            { "ChurchName", contact.getChurchName(), java.sql.Types.VARCHAR },
            { "SendNewsletter", contact.isSendNewsletter(), java.sql.Types.BOOLEAN },
            { "NewsletterMediaPref", contact.getNewsletterMediaPref(), java.sql.Types.VARCHAR, 4 },
            { "NewsletterLangID", contact.getNewsletterLangId(), java.sql.Types.INTEGER },
            { "DirectDeposit", contact.isDirectDeposit(), java.sql.Types.BOOLEAN },
            { "Magazine", contact.isMagazine(), java.sql.Types.BOOLEAN },
            { "MonthlyPledge", TntDb.formatDbCurrency(contact.getMonthlyPledge()), java.sql.Types.VARCHAR },
            { "FirstGiftDate", contact.getFirstGiftDate(), java.sql.Types.DATE },
            { "LastGiftDate", contact.getLastGiftDate(), java.sql.Types.DATE },
            { "LastGiftAmount", TntDb.formatDbCurrency(contact.getLastGiftAmount()), java.sql.Types.VARCHAR },
            { "LastGiftCurrencyID", contact.getLastGiftCurrencyId(), java.sql.Types.INTEGER },
            { "LastGiftOrganizationID", contact.getLastGiftOrganizationId(), java.sql.Types.INTEGER },
            { "LastGiftOrgDonorCode", contact.getLastGiftOrgDonorCode(), java.sql.Types.VARCHAR },
            { "LastGiftPaymentMethod", contact.getLastGiftPaymentMethod(), java.sql.Types.VARCHAR },
            { "PrevYearTotal", TntDb.formatDbCurrency(contact.getPrevYearTotal()), java.sql.Types.VARCHAR },
            { "YearTotal", TntDb.formatDbCurrency(contact.getYearTotal()), java.sql.Types.VARCHAR },
            { "LifetimeTotal", TntDb.formatDbCurrency(contact.getLifetimeTotal()), java.sql.Types.VARCHAR },
            { "LifetimeNumberOfGifts", contact.getLifetimeNumberOfGifts(), java.sql.Types.INTEGER },
            { "LargestGift", TntDb.formatDbCurrency(contact.getLargestGift()), java.sql.Types.VARCHAR },
            { "GoodUntil", contact.getGoodUntil(), java.sql.Types.DATE },
            { "AveMonthlyGift", TntDb.formatDbCurrency(contact.getAveMonthlyGift()), java.sql.Types.VARCHAR },
            { "LastDateInAve", contact.getLastDateInAve(), java.sql.Types.DATE },
            { "TwelveMonthTotal", TntDb.formatDbCurrency(contact.getTwelveMonthTotal()), java.sql.Types.VARCHAR },
            { "BaseCurrencyID", contact.getBaseCurrencyId(), java.sql.Types.INTEGER },
            { "BaseMonthlyPledge", TntDb.formatDbCurrency(contact.getBaseMonthlyPledge()), java.sql.Types.VARCHAR },
            { "BaseLastGiftAmount", TntDb.formatDbCurrency(contact.getBaseLastGiftAmount()), java.sql.Types.VARCHAR },
            { "BasePrevYearTotal", TntDb.formatDbCurrency(contact.getBasePrevYearTotal()), java.sql.Types.VARCHAR },
            { "BaseYearTotal", TntDb.formatDbCurrency(contact.getBaseYearTotal()), java.sql.Types.VARCHAR },
            { "BaseLifetimeTotal", TntDb.formatDbCurrency(contact.getBaseLifetimeTotal()), java.sql.Types.VARCHAR },
            { "BaseLargestGift", TntDb.formatDbCurrency(contact.getBaseLargestGift()), java.sql.Types.VARCHAR },
            { "BaseAveMonthlyGift", TntDb.formatDbCurrency(contact.getBaseAveMonthlyGift()), java.sql.Types.VARCHAR },
            {
                "BaseTwelveMonthTotal",
                TntDb.formatDbCurrency(contact.getBaseTwelveMonthTotal()),
                java.sql.Types.VARCHAR },
            { "LastActivity", contact.getLastActivity(), java.sql.Types.DATE },
            { "LastAppointment", contact.getLastAppointment(), java.sql.Types.DATE },
            { "LastCall", contact.getLastCall(), java.sql.Types.DATE },
            { "LastPreCall", contact.getLastPreCall(), java.sql.Types.DATE },
            { "LastLetter", contact.getLastLetter(), java.sql.Types.DATE },
            { "LastVisit", contact.getLastVisit(), java.sql.Types.DATE },
            { "LastThank", contact.getLastThank(), java.sql.Types.DATE },
            { "LastChallenge", contact.getLastChallenge(), java.sql.Types.DATE },
            { "CampaignsSinceLastGift", contact.getCampaignsSinceLastGift(), java.sql.Types.INTEGER },
            { "ChallengesSinceLastGift", contact.getChallengesSinceLastGift(), java.sql.Types.INTEGER },
            { "OrgDonorCodes", contact.getOrgDonorCodes(), java.sql.Types.VARCHAR } };

        try {
            TntDb.insert(TntDb.TABLE_CONTACT, colValuePairs);
            TntDb.commit();
        } catch (SQLException e) {
            TntDb.rollback();
            throw e;
        }

        // Create initial history
        History history = new History();
        history.setTaskTypeId(TaskType.DATA_CHANGE);
        history.setHistoryDate(now);
        history.setLastEdit(now);
        history.setHistoryResultId(History.RESULT_DONE);
        history.setDescription(String.format("Contact added via %s %s", MIST.APP_NAME, MIST.getAppVersion()));
        history.getContactInfo().setId(contact.getContactId());
        HistoryManager.create(history);

        return contact.getContactId();
    }

    /**
     * Returns the contact associated with the specified contact ID or null if none exists.
     *
     * @param contactId
     *            the contact ID; null returns null
     * @return the contact associated with the specified contact ID or null if none exists
     * @throws SQLException
     *             if there is a database access problem
     */
    public static Contact get(Integer contactId) throws SQLException {
        log.trace("get({})", contactId);

        if (contactId == null)
            return null;

        String query = "SELECT * FROM [Contact] WHERE [ContactID] = ?";
        PreparedStatement stmt = TntDb.getConnection().prepareStatement(
            query,
            ResultSet.TYPE_SCROLL_INSENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
        stmt.setInt(1, contactId);
        ResultSet rs = stmt.executeQuery();
        if (!rs.first())
            return null;

        Contact contact = new Contact();
        contact.setContactId(TntDb.getRSInteger(rs, "ContactID"));
        contact.setLastEdit(TntDb.timestampToDate(rs.getTimestamp("LastEdit")));
        contact.setCreatedDate(TntDb.timestampToDate(rs.getTimestamp("CreatedDate")));
        contact.setRejectedDuplicateContactIDs(rs.getString("RejectedDuplicateContactIDs"));
        contact.setFileAs(rs.getString("FileAs"));
        contact.setFileAsIsCustom(rs.getBoolean("FileAsIsCustom"));
        contact.setFullName(rs.getString("FullName"));
        contact.setFullNameIsCustom(rs.getBoolean("FullNameIsCustom"));
        contact.setGreeting(rs.getString("Greeting"));
        contact.setGreetingIsCustom(rs.getBoolean("GreetingIsCustom"));
        contact.setSalutation(rs.getString("Salutation"));
        contact.setSalutationIsCustom(rs.getBoolean("SalutationIsCustom"));
        contact.setShortName(rs.getString("ShortName"));
        contact.setShortNameIsCustom(rs.getBoolean("ShortNameIsCustom"));
        contact.setMailingAddressBlock(rs.getString("MailingAddressBlock"));
        contact.setMailingAddressIsDeliverable(rs.getBoolean("MailingAddressIsDeliverable"));
        contact.setPhone(rs.getString("Phone"));
        contact.setPhoneIsValid(rs.getBoolean("PhoneIsValid"));
        contact.setEmail(rs.getString("Email"));
        contact.setEmailIsValid(rs.getBoolean("EmailIsValid"));
        contact.setOrganization(rs.getBoolean("IsOrganization"));
        contact.setOrganizationName(rs.getString("OrganizationName"));
        contact.setOrgContactPerson(rs.getString("OrgContactPerson"));
        contact.setTitle(rs.getString("Title"));
        contact.setFirstName(rs.getString("FirstName"));
        contact.setMiddleName(rs.getString("MiddleName"));
        contact.setLastName(rs.getString("LastName"));
        contact.setSuffix(rs.getString("Suffix"));
        contact.setSpouseTitle(rs.getString("SpouseTitle"));
        contact.setSpouseFirstName(rs.getString("SpouseFirstName"));
        contact.setSpouseMiddleName(rs.getString("SpouseMiddleName"));
        contact.setSpouseLastName(rs.getString("SpouseLastName"));
        contact.setDeceased(rs.getBoolean("Deceased"));
        contact.setMailingAddressType(TntDb.getRSInteger(rs, "MailingAddressType"));
        contact.setMailingStreetAddress(rs.getString("MailingStreetAddress"));
        contact.setMailingCity(rs.getString("MailingCity"));
        contact.setMailingState(rs.getString("MailingState"));
        contact.setMailingPostalCode(rs.getString("MailingPostalCode"));
        contact.setMailingCountry(rs.getString("MailingCountry"));
        contact.setHomeStreetAddress(rs.getString("HomeStreetAddress"));
        contact.setHomeCity(rs.getString("HomeCity"));
        contact.setHomeState(rs.getString("HomeState"));
        contact.setHomePostalCode(rs.getString("HomePostalCode"));
        contact.setHomeCountryID(TntDb.getRSInteger(rs, "HomeCountryID"));
        contact.setHomeCountry(rs.getString("HomeCountry"));
        contact.setHomeAddressIsDeliverable(rs.getBoolean("HomeAddressIsDeliverable"));
        contact.setHomeAddressBlock(rs.getString("HomeAddressBlock"));
        contact.setHomeAddressBlockIsCustom(rs.getBoolean("HomeAddressBlockIsCustom"));
        contact.setOtherStreetAddress(rs.getString("OtherStreetAddress"));
        contact.setOtherCity(rs.getString("OtherCity"));
        contact.setOtherState(rs.getString("OtherState"));
        contact.setOtherPostalCode(rs.getString("OtherPostalCode"));
        contact.setOtherCountryID(TntDb.getRSInteger(rs, "OtherCountryID"));
        contact.setOtherCountry(rs.getString("OtherCountry"));
        contact.setOtherAddressIsDeliverable(rs.getBoolean("OtherAddressIsDeliverable"));
        contact.setOtherAddressBlock(rs.getString("OtherAddressBlock"));
        contact.setOtherAddressBlockIsCustom(rs.getBoolean("OtherAddressBlockIsCustom"));
        contact.setBusinessName(rs.getString("BusinessName"));
        contact.setBusinessStreetAddress(rs.getString("BusinessStreetAddress"));
        contact.setBusinessCity(rs.getString("BusinessCity"));
        contact.setBusinessState(rs.getString("BusinessState"));
        contact.setBusinessPostalCode(rs.getString("BusinessPostalCode"));
        contact.setBusinessCountryId(TntDb.getRSInteger(rs, "BusinessCountryID"));
        contact.setBusinessCountry(rs.getString("BusinessCountry"));
        contact.setBusinessAddressIsDeliverable(rs.getBoolean("BusinessAddressIsDeliverable"));
        contact.setBusinessAddressBlock(rs.getString("BusinessAddressBlock"));
        contact.setBusinessAddressBlockIsCustom(rs.getBoolean("BusinessAddressBlockIsCustom"));
        contact.setSpouseBusinessName(rs.getString("SpouseBusinessName"));
        contact.setSpouseBusinessStreetAddress(rs.getString("SpouseBusinessStreetAddress"));
        contact.setSpouseBusinessCity(rs.getString("SpouseBusinessCity"));
        contact.setSpouseBusinessState(rs.getString("SpouseBusinessState"));
        contact.setSpouseBusinessPostalCode(rs.getString("SpouseBusinessPostalCode"));
        contact.setSpouseBusinessCountryId(TntDb.getRSInteger(rs, "SpouseBusinessCountryID"));
        contact.setSpouseBusinessCountry(rs.getString("SpouseBusinessCountry"));
        contact.setSpouseBusinessAddressIsDeliverable(rs.getBoolean("SpouseBusinessAddressIsDeliverable"));
        contact.setSpouseBusinessAddressBlock(rs.getString("SpouseBusinessAddressBlock"));
        contact.setSpouseBusinessAddressBlockIsCustom(rs.getBoolean("SpouseBusinessAddressBlockIsCustom"));
        contact.setPreferredPhoneType(TntDb.getRSInteger(rs, "PreferredPhoneType"));
        contact.setPhoneIsValidMask(TntDb.getRSInteger(rs, "PhoneIsValidMask"));
        contact.setPhoneCountryIds(rs.getString("PhoneCountryIDs"));
        contact.setHomePhone(rs.getString("HomePhone"));
        contact.setHomePhone2(rs.getString("HomePhone2"));
        contact.setHomeFax(rs.getString("HomeFax"));
        contact.setOtherPhone(rs.getString("OtherPhone"));
        contact.setOtherFax(rs.getString("OtherFax"));
        contact.setBusinessPhone(rs.getString("BusinessPhone"));
        contact.setBusinessPhone2(rs.getString("BusinessPhone2"));
        contact.setBusinessFax(rs.getString("BusinessFax"));
        contact.setCompanyMainPhone(rs.getString("CompanyMainPhone"));
        contact.setMobilePhone(rs.getString("MobilePhone"));
        contact.setMobilePhone2(rs.getString("MobilePhone2"));
        contact.setPagerNumber(rs.getString("PagerNumber"));
        contact.setSpouseBusinessPhone(rs.getString("SpouseBusinessPhone"));
        contact.setSpouseBusinessPhone2(rs.getString("SpouseBusinessPhone2"));
        contact.setSpouseBusinessFax(rs.getString("SpouseBusinessFax"));
        contact.setSpouseCompanyMainPhone(rs.getString("SpouseCompanyMainPhone"));
        contact.setSpouseMobilePhone(rs.getString("SpouseMobilePhone"));
        contact.setSpouseMobilePhone2(rs.getString("SpouseMobilePhone2"));
        contact.setSpousePagerNumber(rs.getString("SpousePagerNumber"));
        contact.setPreferredEmailTypes(TntDb.getRSInteger(rs, "PreferredEmailTypes"));
        contact.setEmailLabels(rs.getString("EmailLabels"));
        contact.setEmail1(rs.getString("Email1"));
        contact.setEmail2(rs.getString("Email2"));
        contact.setEmail3(rs.getString("Email3"));
        contact.setEmail1IsValid(rs.getBoolean("Email1IsValid"));
        contact.setEmail2IsValid(rs.getBoolean("Email2IsValid"));
        contact.setEmail3IsValid(rs.getBoolean("Email3IsValid"));
        contact.setEmailCustomGreeting(rs.getString("EmailCustomGreeting"));
        contact.setEmailCustomSalutation(rs.getString("EmailCustomSalutation"));
        contact.setSpouseEmail1(rs.getString("SpouseEmail1"));
        contact.setSpouseEmail2(rs.getString("SpouseEmail2"));
        contact.setSpouseEmail3(rs.getString("SpouseEmail3"));
        contact.setSpouseEmail1IsValid(rs.getBoolean("SpouseEmail1IsValid"));
        contact.setSpouseEmail2IsValid(rs.getBoolean("SpouseEmail2IsValid"));
        contact.setSpouseEmail3IsValid(rs.getBoolean("SpouseEmail3IsValid"));
        contact.setSpouseEmailCustomGreeting(rs.getString("SpouseEmailCustomGreeting"));
        contact.setSpouseEmailCustomSalutation(rs.getString("SpouseEmailCustomSalutation"));
        contact.setWebPage1(rs.getString("WebPage1"));
        contact.setWebPage2(rs.getString("WebPage2"));
        contact.setVoiceSkype(rs.getString("VoiceSkype"));
        contact.setImAddress(rs.getString("IMAddress"));
        contact.setSocialWeb1(rs.getString("SocialWeb1"));
        contact.setSocialWeb2(rs.getString("SocialWeb2"));
        contact.setSocialWeb3(rs.getString("SocialWeb3"));
        contact.setSocialWeb4(rs.getString("SocialWeb4"));
        contact.setSpouseWebPage1(rs.getString("SpouseWebPage1"));
        contact.setSpouseWebPage2(rs.getString("SpouseWebPage2"));
        contact.setSpouseVoiceSkype(rs.getString("SpouseVoiceSkype"));
        contact.setSpouseImAddress(rs.getString("SpouseIMAddress"));
        contact.setSpouseSocialWeb1(rs.getString("SpouseSocialWeb1"));
        contact.setSpouseSocialWeb2(rs.getString("SpouseSocialWeb2"));
        contact.setSpouseSocialWeb3(rs.getString("SpouseSocialWeb3"));
        contact.setSpouseSocialWeb4(rs.getString("SpouseSocialWeb4"));
        contact.setNotesAsRtf(rs.getString("NotesAsRTF"));
        contact.setNotes(rs.getString("Notes"));
        contact.setFamilySideID(TntDb.getRSInteger(rs, "FamilySideID"));
        contact.setFamilyLevelID(TntDb.getRSInteger(rs, "FamilyLevelID"));
        contact.setChildren(rs.getString("Children"));
        contact.setInterests(rs.getString("Interests"));
        contact.setNickname(rs.getString("Nickname"));
        contact.setProfession(rs.getString("Profession"));
        contact.setSpouseInterests(rs.getString("SpouseInterests"));
        contact.setSpouseNickname(rs.getString("SpouseNickname"));
        contact.setSpouseProfession(rs.getString("SpouseProfession"));
        contact.setAnniversaryMonth(TntDb.getRSInteger(rs, "AnniversaryMonth"));
        contact.setAnniversaryDay(TntDb.getRSInteger(rs, "AnniversaryDay"));
        contact.setAnniversaryYear(TntDb.getRSInteger(rs, "AnniversaryYear"));
        contact.setBirthdayMonth(TntDb.getRSInteger(rs, "BirthdayMonth"));
        contact.setBirthdayDay(TntDb.getRSInteger(rs, "BirthdayDay"));
        contact.setBirthdayYear(TntDb.getRSInteger(rs, "BirthdayYear"));
        contact.setSpouseBirthdayMonth(TntDb.getRSInteger(rs, "SpouseBirthdayMonth"));
        contact.setSpouseBirthdayDay(TntDb.getRSInteger(rs, "SpouseBirthdayDay"));
        contact.setSpouseBirthdayYear(TntDb.getRSInteger(rs, "SpouseBirthdayYear"));
        contact.setCategories(rs.getString("Categories"));
        contact.setUser1(rs.getString("User1"));
        contact.setUser2(rs.getString("User2"));
        contact.setUser3(rs.getString("User3"));
        contact.setUser4(rs.getString("User4"));
        contact.setUser5(rs.getString("User5"));
        contact.setUser6(rs.getString("User6"));
        contact.setUser7(rs.getString("User7"));
        contact.setUser8(rs.getString("User8"));
        contact.setUserStatus(rs.getString("UserStatus"));
        contact.setMapAddressType(TntDb.getRSInteger(rs, "MapAddressType"));
        contact.setMapLat(TntDb.getRSInteger(rs, "MapLat"));
        contact.setMapLng(TntDb.getRSInteger(rs, "MapLng"));
        contact.setMapStatus(rs.getString("MapStatus"));
        contact.setPledgeAmount(TntDb.floatToMoney(rs.getFloat("PledgeAmount")));
        contact.setPledgeFrequencyId(TntDb.getRSInteger(rs, "PledgeFrequencyID"));
        contact.setPledgeReceived(rs.getBoolean("PledgeReceived"));
        contact.setPledgeStartDate(TntDb.timestampToDate(rs.getTimestamp("PledgeStartDate")));
        contact.setPledgeCurrencyId(TntDb.getRSInteger(rs, "PledgeCurrencyID"));
        contact.setReferredBy(rs.getString("ReferredBy"));
        contact.setReferredByList(rs.getString("ReferredByList"));
        contact.setMpdPhaseId(TntDb.getRSInteger(rs, "MPDPhaseID"));
        contact.setFundRepId(TntDb.getRSInteger(rs, "FundRepID"));
        contact.setNextAsk(TntDb.timestampToDate(rs.getTimestamp("NextAsk")));
        contact.setNextAskAmount(TntDb.floatToMoney(rs.getFloat("NextAskAmount")));
        contact.setEstimatedAnnualCapacity(TntDb.floatToMoney(rs.getFloat("EstimatedAnnualCapacity")));
        contact.setNeverAsk(rs.getBoolean("NeverAsk"));
        contact.setRegion(rs.getString("Region"));
        contact.setLikelyToGiveId(TntDb.getRSInteger(rs, "LikelyToGiveID"));
        contact.setChurchName(rs.getString("ChurchName"));
        contact.setSendNewsletter(rs.getBoolean("SendNewsletter"));
        contact.setNewsletterMediaPref(rs.getString("NewsletterMediaPref"));
        contact.setNewsletterLangId(TntDb.getRSInteger(rs, "NewsletterLangID"));
        contact.setDirectDeposit(rs.getBoolean("DirectDeposit"));
        contact.setMagazine(rs.getBoolean("Magazine"));
        contact.setMonthlyPledge(TntDb.floatToMoney(rs.getFloat("MonthlyPledge")));
        contact.setFirstGiftDate(TntDb.timestampToDate(rs.getTimestamp("FirstGiftDate")));
        contact.setLastGiftDate(TntDb.timestampToDate(rs.getTimestamp("LastGiftDate")));
        contact.setLastGiftAmount(TntDb.floatToMoney(rs.getFloat("LastGiftAmount")));
        contact.setLastGiftCurrencyId(TntDb.getRSInteger(rs, "LastGiftCurrencyID"));
        contact.setLastGiftOrganizationId(TntDb.getRSInteger(rs, "LastGiftOrganizationID"));
        contact.setLastGiftOrgDonorCode(rs.getString("LastGiftOrgDonorCode"));
        contact.setLastGiftPaymentMethod(rs.getString("LastGiftPaymentMethod"));
        contact.setPrevYearTotal(TntDb.floatToMoney(rs.getFloat("PrevYearTotal")));
        contact.setYearTotal(TntDb.floatToMoney(rs.getFloat("YearTotal")));
        contact.setLifetimeTotal(TntDb.floatToMoney(rs.getFloat("LifetimeTotal")));
        contact.setLifetimeNumberOfGifts(TntDb.getRSInteger(rs, "LifetimeNumberOfGifts"));
        contact.setLargestGift(TntDb.floatToMoney(rs.getFloat("LargestGift")));
        contact.setGoodUntil(TntDb.timestampToDate(rs.getTimestamp("GoodUntil")));
        contact.setAveMonthlyGift(TntDb.floatToMoney(rs.getFloat("AveMonthlyGift")));
        contact.setLastDateInAve(TntDb.timestampToDate(rs.getTimestamp("LastDateInAve")));
        contact.setTwelveMonthTotal(TntDb.floatToMoney(rs.getFloat("TwelveMonthTotal")));
        contact.setBaseCurrencyId(TntDb.getRSInteger(rs, "BaseCurrencyID"));
        contact.setBaseMonthlyPledge(TntDb.floatToMoney(rs.getFloat("BaseMonthlyPledge")));
        contact.setBaseLastGiftAmount(TntDb.floatToMoney(rs.getFloat("BaseLastGiftAmount")));
        contact.setBasePrevYearTotal(TntDb.floatToMoney(rs.getFloat("BasePrevYearTotal")));
        contact.setBaseYearTotal(TntDb.floatToMoney(rs.getFloat("BaseYearTotal")));
        contact.setBaseLifetimeTotal(TntDb.floatToMoney(rs.getFloat("BaseLifetimeTotal")));
        contact.setBaseLargestGift(TntDb.floatToMoney(rs.getFloat("BaseLargestGift")));
        contact.setBaseAveMonthlyGift(TntDb.floatToMoney(rs.getFloat("BaseAveMonthlyGift")));
        contact.setBaseTwelveMonthTotal(TntDb.floatToMoney(rs.getFloat("BaseTwelveMonthTotal")));
        contact.setLastActivity(TntDb.timestampToDate(rs.getTimestamp("LastActivity")));
        contact.setLastAppointment(TntDb.timestampToDate(rs.getTimestamp("LastAppointment")));
        contact.setLastCall(TntDb.timestampToDate(rs.getTimestamp("LastCall")));
        contact.setLastPreCall(TntDb.timestampToDate(rs.getTimestamp("LastPreCall")));
        contact.setLastLetter(TntDb.timestampToDate(rs.getTimestamp("LastLetter")));
        contact.setLastVisit(TntDb.timestampToDate(rs.getTimestamp("LastVisit")));
        contact.setLastThank(TntDb.timestampToDate(rs.getTimestamp("LastThank")));
        contact.setLastChallenge(TntDb.timestampToDate(rs.getTimestamp("LastChallenge")));
        contact.setCampaignsSinceLastGift(TntDb.getRSInteger(rs, "CampaignsSinceLastGift"));
        contact.setChallengesSinceLastGift(TntDb.getRSInteger(rs, "ChallengesSinceLastGift"));
        contact.setOrgDonorCodes(rs.getString("OrgDonorCodes"));
        return contact;
    }

    /**
     * Gets the number of challenges issued to this contact since their last gift.
     *
     * @param contactId
     *            the contact ID
     * @return
     *         the number of challenges issued to this contact since their last gift.
     * @throws SQLException
     *             if there is a database access problem
     */
    public static int getChallengesSinceLastGift(int contactId) throws SQLException {
        log.trace("getChallengesSinceLastGift({})", contactId);
        String query = "SELECT [ChallengesSinceLastGift] FROM [Contact] WHERE [ContactId] = ?";
        try {
            return TntDb.getOneInt(query, contactId);
        } catch (TntDbException e) {
            log.error(e); // Nothing useful can be done in this case
            return 0;
        }
    }

    /**
     * Return the contact ID associated with the specified email, or null if none exists.
     *
     * @param email
     *            the email for which to find an associated contact ID
     * @return the contact ID associated with the specified email, or null if none exists
     * @throws TntDbException
     *             if multiple contacts exist with this email address
     * @throws SQLException
     *             if there is a database access problem
     */
    public static Integer getContactIdByEmail(String email) throws TntDbException, SQLException {
        log.trace("getContactIdByEmail({})", email);
        Integer[] contactIds = getContactIdByEmailHelper(email);
        switch (contactIds.length) {
            case 0:
                return null;
            case 1:
                return contactIds[0];
            default:
                throw new TntDbException("Multiple contacts exist with this email address");
        }
    }

    /**
     * Return an array of contact ids found when searching by email.
     *
     * @param email
     *            the email for which to find associated contacts
     * @return an array of contact ids found when searching by email.
     * @throws SQLException
     *             if there is a database access problem
     */
    private static Integer[] getContactIdByEmailHelper(String email) throws SQLException {
        log.trace("getContactIdByEmailQuery({})", email);

        if (email == null)
            return new Integer[0];

        //
        // First we search for all partial string matches in the DB
        //
        HashSet<Integer> contactIds = new HashSet<Integer>();

        // Concatenate the email addresses together with commas
        String query = "SELECT [ContactID],"
            + "[Email1] & ',' & [Email2] & ',' & [Email3] & ',' & "
            + "[SpouseEmail1] & ',' & [SpouseEmail2] & ',' & [SpouseEmail3]"
            + "FROM [Contact] WHERE "
            + "[Email1] LIKE ? OR "
            + "[Email2] LIKE ? OR "
            + "[Email3] LIKE ? OR "
            + "[SpouseEmail1] LIKE ? OR "
            + "[SpouseEmail2] LIKE ? OR "
            + "[SpouseEmail3] LIKE ?";
        PreparedStatement stmt = TntDb.getConnection().prepareStatement(
            query,
            ResultSet.TYPE_SCROLL_INSENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
        for (int i = 0; i < 6; i++) {
            stmt.setString(i + 1, "%" + email + "%");
        }
        ResultSet rs = stmt.executeQuery();
        log.warn(TntDb.getResultSetString(rs));

        Pattern pattern = Pattern.compile(MIST.REGEX_EMAILADDRESS);
        while (rs.next()) {
            Integer contactId = rs.getInt("ContactID");
            String emailStr = rs.getString(2);

            // Parse out individual email addresses
            ArrayList<String> emails = new ArrayList<String>();
            Util.addMatchesToList(emails, pattern, emailStr);

            // Now look for exact matches
            for (Iterator<String> it = emails.iterator(); it.hasNext();) {
                String em = it.next();
                if (email.equals(em))
                    contactIds.add(contactId);
            }
        }

        return contactIds.toArray(new Integer[0]);
    }

    /**
     * Returns a list of all contacts in the TntConnect database, sorted by name.
     * <p>
     * Note: only the id and name fields are populated
     * 
     * @return all contacts in the TntConnect database
     * @throws SQLException
     *             if there is a database access problem
     */
    public static ContactInfo[] getContactList() throws SQLException {
        log.trace("getContactList()");
        List<ContactInfo> contacts = new ArrayList<ContactInfo>();
        ResultSet rs = TntDb.getConnection().createStatement().executeQuery(
            "SELECT [ContactID], [FileAs] FROM [Contact] ORDER BY [FileAs]");
        while (rs.next())
            contacts.add(new ContactInfo(rs.getInt("ContactID"), rs.getString("FileAs")));
        return contacts.toArray(new ContactInfo[0]);
    }

    /**
     * Return the number of contacts associated with the specified email.
     *
     * @param email
     *            the email for which to find associated contacts
     * @return the contact ID associated with the specified email
     * @throws SQLException
     *             if there is a database access problem
     */
    public static int getContactsByEmailCount(String email) throws SQLException {
        log.trace("getContactsByEmailCount({})", email);
        return getContactIdByEmailHelper(email).length;
    }

    /**
     * Return the contact "File As" name associated with the specified contact ID or null if no contact exists.
     *
     * @param contactId
     *            the contact ID
     * @return The contact name associated with the specified contact ID or null if no contact exists.
     * @throws TntDbException
     *             if multiple contacts exist with this ID
     * @throws SQLException
     *             if there is a database access problem
     */
    public static String getFileAs(int contactId) throws TntDbException, SQLException {
        log.trace("getFileAs({})", contactId);
        return TntDb.getOneString("SELECT [FileAs] FROM [Contact] WHERE [ContactId] = ?", contactId);
    }

    /**
     * Gets the last activity date for the specified contact.
     *
     * @param contactId
     *            the contact ID for which to get the last activity date
     * @return
     *         the last activity date or null if one doesn't exist
     * @throws SQLException
     *             if there is a database access problem
     */
    public static LocalDateTime getLastActivityDate(int contactId) throws SQLException {
        log.trace("getLastActivityDate({})", contactId);
        return getLastXDate(contactId, LASTTYPE_ACTIVITY);
    }

    /**
     * Gets the last appointment date for the specified contact.
     *
     * @param contactId
     *            the contact ID for which to get the last appointment date
     * @return
     *         the last appointment date or null if one doesn't exist
     * @throws SQLException
     *             if there is a database access problem
     */
    public static LocalDateTime getLastAppointmentDate(int contactId) throws SQLException {
        log.trace("getLastAppointmentDate({})", contactId);
        return getLastXDate(contactId, LASTTYPE_APPOINTMENT);
    }

    /**
     * Gets the last call date for the specified contact.
     *
     * @param contactId
     *            the contact ID for which to get the last call date
     * @return
     *         the last call date or null if one doesn't exist
     * @throws SQLException
     *             if there is a database access problem
     */
    public static LocalDateTime getLastCallDate(int contactId) throws SQLException {
        log.trace("getLastCallDate({})", contactId);
        return getLastXDate(contactId, LASTTYPE_CALL);
    }

    /**
     * Gets the last challenge date for the specified contact.
     *
     * @param contactId
     *            the contact ID for which to get the last challenge date
     * @return
     *         the last challenge date or null if one doesn't exist
     * @throws SQLException
     *             if there is a database access problem
     */
    public static LocalDateTime getLastChallengeDate(int contactId) throws SQLException {
        log.trace("getLastChallengeDate({})", contactId);
        return getLastXDate(contactId, LASTTYPE_CHALLENGE);
    }

    /**
     * Gets the last edit date for the specified contact.
     *
     * @param contactId
     *            the contact ID for which to get the last edit date
     * @return
     *         the last edit date or null if one doesn't exist (which shouldn't happen)
     * @throws SQLException
     *             if there is a database access problem
     */
    public static LocalDateTime getLastEditDate(int contactId) throws SQLException {
        log.trace("getLastEditDate({})", contactId);
        return getLastXDate(contactId, LASTTYPE_EDIT);
    }

    /**
     * Gets the last letter date for the specified contact.
     *
     * @param contactId
     *            the contact ID for which to get the last letter date
     * @return
     *         the last letter date or null if one doesn't exist
     * @throws SQLException
     *             if there is a database access problem
     */
    public static LocalDateTime getLastLetterDate(int contactId) throws SQLException {
        log.trace("getLastLetterDate({})", contactId);
        return getLastXDate(contactId, LASTTYPE_LETTER);
    }

    /**
     * Gets the last pre-call date for the specified contact.
     *
     * @param contactId
     *            the contact ID for which to get the last pre-call date
     * @return
     *         the last activity date or null if one doesn't exist
     * @throws SQLException
     *             if there is a database access problem
     */
    public static LocalDateTime getLastPreCallDate(int contactId) throws SQLException {
        log.trace("getLastPreCallDate({})", contactId);
        return getLastXDate(contactId, LASTTYPE_PRECALL);
    }

    /**
     * Gets the last thank date for the specified contact.
     *
     * @param contactId
     *            the contact ID for which to get the last thank date
     * @return
     *         the last thank date or null if one doesn't exist
     * @throws SQLException
     *             if there is a database access problem
     */
    public static LocalDateTime getLastThankDate(int contactId) throws SQLException {
        log.trace("getLastThankDate({})", contactId);
        return getLastXDate(contactId, LASTTYPE_THANK);
    }

    /**
     * Gets the last visit date for the specified contact.
     *
     * @param contactId
     *            the contact ID for which to get the last visit date
     * @return
     *         the last visit date or null if one doesn't exist
     * @throws SQLException
     *             if there is a database access problem
     */
    public static LocalDateTime getLastVisitDate(int contactId) throws SQLException {
        log.trace("getLastVisitDate({})", contactId);
        return getLastXDate(contactId, LASTTYPE_VISIT);
    }

    /**
     * Gets the last X date (where X is specified by {@code lastType}) for the specified contact.
     *
     * @param contactId
     *            the contact ID for which to get the last date
     * @param lastType
     *            the type of last date to return
     * @return
     *         the last X date; null if the field was empty
     * @throws SQLException
     *             if there is a database access problem
     */
    protected static LocalDateTime getLastXDate(int contactId, String lastType) throws SQLException {
        log.trace("getLastXDate({},{})", contactId, lastType);
        String query = String.format("SELECT [Last%s] FROM [Contact] WHERE [ContactId] = ?", lastType);
        try {
            return TntDb.getOneDate(query, contactId);
        } catch (TntDbException e) {
            log.error(e); // Nothing useful can be done in this case
            return null;
        }
    }

    /**
     * Recalculates the specified contact's "Challenges Since Last Gift" value.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact's ID
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void recalculateChallengesSinceLastGift(int contactId) throws SQLException {
        log.trace("recalculateChallengesSinceLastGift({})", contactId);

        LocalDateTime lastGiftDate = getLastXDate(contactId, LASTTYPE_GIFT);
        if (lastGiftDate == null)
            lastGiftDate = LocalDateTime.of(1900, 1, 1, 0, 0);

        String countQuery = String.format(
            // MassMailing doesn't matter here
            "SELECT COUNT([History].[HistoryID]) "
                + "FROM [History] INNER JOIN [HistoryContact] ON [History].[HistoryID] = [HistoryContact].[HistoryID] "
                + "WHERE [HistoryContact].[ContactID] = ? AND "
                + "[History].[HistoryResultID] <> %s AND "
                + "[History].[IsChallenge] = -1 AND "
                + "[History].[HistoryDate] >= %s",
            History.RESULT_ATTEMPTED,
            TntDb.formatDbDateNoTime(lastGiftDate));
        Integer challengesSinceLastGift = 0;
        try {
            challengesSinceLastGift = TntDb.getOneInt(countQuery, contactId);
        } catch (TntDbException e) {
            log.error(e); // Nothing useful can be done in this case
        }

        String query = "UPDATE [Contact] SET [ChallengesSinceLastGift] = ? WHERE [ContactId] = ?";
        PreparedStatement stmt = TntDb.getConnection().prepareStatement(query);
        stmt.setInt(1, challengesSinceLastGift);
        stmt.setInt(2, contactId);
        stmt.executeUpdate();
    }

    /**
     * Recalculates all of the specified contact's "History Data" (i.e. "LastX" fields)
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact's ID
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void recalculateHistoryData(int contactId) throws SQLException {
        log.trace("recalculateHistoryData({})", contactId);
        recalculateLastAppointmentDate(contactId);
        recalculateLastCallDate(contactId);
        recalculateLastChallengeDate(contactId);
        recalculateLastPreCallDate(contactId);
        recalculateLastLetterDate(contactId);
        recalculateLastThankDate(contactId);
        recalculateLastVisitDate(contactId);
        recalculateLastActivityDate(contactId);
    }

    /**
     * Recalculates the specified contact's "Last Activity" date.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact's ID
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void recalculateLastActivityDate(int contactId) throws SQLException {
        log.trace("recalculateLastActivityDate({})", contactId);

        LocalDateTime[] dates = {
            getLastCallDate(contactId),
            getLastChallengeDate(contactId),
            getLastLetterDate(contactId),
            getLastPreCallDate(contactId),
            getLastThankDate(contactId),
            getLastVisitDate(contactId) };

        LocalDateTime lastActivity = getLastAppointmentDate(contactId); // Start with LastAppointment
        for (LocalDateTime date : dates) {
            if (lastActivity == null)
                lastActivity = date;
            else if (date != null) {
                if (lastActivity.isBefore(date))
                    lastActivity = date;
            }
        }
        if (lastActivity != null)
            updateLastActivityDate(contactId, lastActivity, true); // Force an overwrite of new date
    }

    /**
     * Recalculates the specified contact's "Last Appointment" date.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact's ID
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void recalculateLastAppointmentDate(int contactId) throws SQLException {
        log.trace("recalculateLastAppointmentDate({})", contactId);
        recalculateLastXDate(contactId, LASTTYPE_APPOINTMENT, 1); // 1 = appointment
    }

    /**
     * Recalculates the specified contact's "Last Call" date.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact's ID
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void recalculateLastCallDate(int contactId) throws SQLException {
        log.trace("recalculateLastCallDate({})", contactId);
        recalculateLastXDate(contactId, LASTTYPE_CALL, 20); // 20 = call
    }

    /**
     * Recalculates the specified contact's "Last Challenge" date.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact's ID
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void recalculateLastChallengeDate(int contactId) throws SQLException {
        log.trace("recalculateLastChallengeDate({})", contactId);
        // Property of history; not a task type
        String query = String.format(
            // MassMailing doesn't matter here
            "SELECT MAX([History].[HistoryDate]) "
                + "FROM [History] INNER JOIN [HistoryContact] ON [History].[HistoryID] = [HistoryContact].[HistoryID] "
                + "WHERE [HistoryContact].[ContactID] = ? AND "
                + "[History].[HistoryResultID] <> %s AND "
                + "[History].[IsChallenge] = -1",
            History.RESULT_ATTEMPTED);
        LocalDateTime maxDate = null;
        try {
            maxDate = TntDb.getOneDate(query, contactId);
        } catch (TntDbException e) {
            log.error(e); // Nothing useful can be done in this case
            return;
        }
        updateLastChallengeDate(contactId, maxDate);
    }

    /**
     * Recalculates the specified contact's "Last Letter" date.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact's ID
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void recalculateLastLetterDate(int contactId) throws SQLException {
        log.trace("recalculateLastLetterDate({})", contactId);
        recalculateLastXDateWithAffectsLastCol(contactId, LASTTYPE_LETTER);
    }

    /**
     * Recalculates the specified contact's "Last PreCall" date.
     * <p>
     * Does NOT automatically recalculate the last letter date.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact's ID
     * @throws SQLException
     *             if there is a database access problem
     * @see #recalculateLastLetterDate(int)
     */
    public static void recalculateLastPreCallDate(int contactId) throws SQLException {
        log.trace("recalculateLastPreCallDate({})", contactId);
        recalculateLastXDate(contactId, LASTTYPE_PRECALL, 70); // 70 = PreCall
    }

    /**
     * Recalculates the specified contact's "Last Thank" date.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact's ID
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void recalculateLastThankDate(int contactId) throws SQLException {
        log.trace("recalculateLastThankDate({})", contactId);
        // Property of history despite also being a task type
        String query = String.format(
            "SELECT MAX([History].[HistoryDate]) AS maxdate "
                + "FROM [History] INNER JOIN [HistoryContact] ON [History].[HistoryID] = [HistoryContact].[HistoryID] "
                + "WHERE [HistoryContact].[ContactID] = ? AND "
                + "[History].[HistoryResultID] <> %s AND "
                + "[History].[IsMassMailing] = 0 AND "
                + "[History].[IsThank] = -1",
            History.RESULT_ATTEMPTED);
        LocalDateTime maxDate = null;
        try {
            maxDate = TntDb.getOneDate(query, contactId);
        } catch (TntDbException e) {
            log.error(e); // Nothing useful can be done in this case
            return;
        }
        updateLastThankDate(contactId, maxDate);
    }

    /**
     * Recalculates the specified contact's "Last Visit" date.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact's ID
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void recalculateLastVisitDate(int contactId) throws SQLException {
        log.trace("recalculateLastVisitDate({})", contactId);
        recalculateLastXDateWithAffectsLastCol(contactId, LASTTYPE_VISIT);
    }

    /**
     * Recalculates the specified contact's "Last X" date.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact's ID
     * @param lastType
     *            the type of last date to recalculate
     * @throws SQLException
     *             if there is a database access problem
     */
    protected static void recalculateLastXDate(int contactId, String lastType) throws SQLException {
        log.trace("recalculateLastXDate({},{})", contactId, lastType);
        if (LASTTYPE_ACTIVITY.equals(lastType))
            recalculateLastActivityDate(contactId);
        else if (LASTTYPE_APPOINTMENT.equals(lastType))
            recalculateLastAppointmentDate(contactId);
        else if (LASTTYPE_CALL.equals(lastType))
            recalculateLastCallDate(contactId);
        else if (LASTTYPE_CHALLENGE.equals(lastType))
            recalculateLastChallengeDate(contactId);
        else if (LASTTYPE_GIFT.equals(lastType))
            ; // Not implemented
        else if (LASTTYPE_LETTER.equals(lastType))
            recalculateLastLetterDate(contactId);
        else if (LASTTYPE_PRECALL.equals(lastType))
            recalculateLastPreCallDate(contactId);
        else if (LASTTYPE_THANK.equals(lastType))
            recalculateLastThankDate(contactId);
        else if (LASTTYPE_VISIT.equals(lastType))
            recalculateLastVisitDate(contactId);
    }

    /**
     * Recalculates the specified contact's "Last X" date based on the specified Task Type ID.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact's ID
     * @param lastType
     *            the type of last date to recalculate
     * @param taskTypeId
     *            the task type ID for determining when the Last X was
     * @throws SQLException
     *             if there is a database access problem
     */
    protected static void recalculateLastXDate(int contactId, String lastType, int taskTypeId) throws SQLException {
        log.trace("recalculateLastXDate({},{},{})", contactId, lastType, taskTypeId);

        String query = String.format(
            "SELECT MAX([History].[HistoryDate]) "
                + "FROM [History] INNER JOIN [HistoryContact] ON [History].[HistoryID] = [HistoryContact].[HistoryID] "
                + "WHERE [HistoryContact].[ContactID] = ? AND "
                + "[History].[HistoryResultID] <> %s AND "
                + "[History].[IsMassMailing] = 0 AND "
                + "[History].[TaskTypeID] = %s",
            History.RESULT_ATTEMPTED,
            taskTypeId);
        LocalDateTime maxDate = null;
        try {
            maxDate = TntDb.getOneDate(query, contactId);
        } catch (TntDbException e) {
            log.error(e); // Nothing useful can be done in this case
            return;
        }
        updateLastXDate(contactId, maxDate, lastType);
    }

    /**
     * Recalculates the specified contact's "Last X" date when a "Affects Last X" column exists for this type.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact's ID
     * @param lastType
     *            the type of last date to recalculate
     * @throws SQLException
     *             if there is a database access problem
     */
    protected static void recalculateLastXDateWithAffectsLastCol(int contactId, String lastType) throws SQLException {
        log.trace("recalculateLastXDateWithAffectsLastCol({},{})", contactId, lastType);

        // First determine which task types to look for based on TaskType table
        String taskTypeQuery = String.format(
            "SELECT [TaskTypeID] FROM [TaskType] WHERE [AffectsLast%s] = -1",
            lastType);
        ResultSet rs = TntDb.getConnection().createStatement().executeQuery(taskTypeQuery);
        String taskTypeStr = "(";
        while (rs.next()) {
            Integer taskTypeId = rs.getInt("TaskTypeID");
            taskTypeStr += "[History].[TaskTypeID] = " + taskTypeId;
            taskTypeStr += rs.isLast() ? " " : " OR ";
        }
        taskTypeStr += ") ";

        // Now find the max date
        String query = String.format(
            "SELECT MAX([History].[HistoryDate]) "
                + "FROM [History] INNER JOIN [HistoryContact] ON [History].[HistoryID] = [HistoryContact].[HistoryID] "
                + "WHERE [HistoryContact].[ContactID] = ? AND "
                + "[History].[HistoryResultID] <> %s AND "
                + "[History].[IsMassMailing] = 0 AND "
                + taskTypeStr,
            History.RESULT_ATTEMPTED);
        LocalDateTime maxDate = null;
        try {
            maxDate = TntDb.getOneDate(query, contactId);
        } catch (TntDbException e) {
            log.error(e); // Nothing useful can be done in this case
            return;
        }
        updateLastXDate(contactId, maxDate, lastType);
    }

    /**
     * Updates the last activity date for the specified contact.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact ID for which to set the last date
     * @param date
     *            the date to set; null forces a recalculation of last activity date (unless last activity date was
     *            already null prior to calling this function)
     * @throws SQLException
     *             if there is a database access problem
     * @see #recalculateLastActivityDate(int)
     */
    public static void updateLastActivityDate(int contactId, LocalDateTime date) throws SQLException {
        log.trace("updateLastActivityDate({},{})", contactId, date);
        updateLastActivityDate(contactId, date, false);
    }

    /**
     * Updates the last activity date for the specified contact.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact ID for which to set the last date
     * @param lastActivity
     *            the date to set; null forces a recalculation of last activity date (unless last activity date was
     *            already null prior to calling this function)
     * @param force
     *            true forces this update even if {@code date} is older than the date in the Tnt database; false
     *            will not override a newer date in the Tnt database
     * @throws SQLException
     *             if there is a database access problem
     * @see #recalculateLastActivityDate(int)
     */
    public static void updateLastActivityDate(
        int contactId,
        LocalDateTime lastActivity,
        boolean force) throws SQLException {
        log.trace("updateLastActivityDate({},{},{})", contactId, lastActivity, force);
        updateLastXDate(contactId, lastActivity, LASTTYPE_ACTIVITY, force);
    }

    /**
     * Updates the last appointment date for the specified contact.
     * <p>
     * Updates the contact's last activity date if appropriate.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact ID for which to set the last date
     * @param date
     *            the date to set; null forces a recalculation of last appointment date and last activity date (unless
     *            last appointment date was already null prior to calling this function)
     * @throws SQLException
     *             if there is a database access problem
     * @see #recalculateLastAppointmentDate(int)
     * @see #recalculateLastActivityDate(int)
     */
    public static void updateLastAppointmentDate(int contactId, LocalDateTime date) throws SQLException {
        log.trace("updateLastAppointmentDate({},{})", contactId, date);
        updateLastXDate(contactId, date, LASTTYPE_APPOINTMENT);
    }

    /**
     * Updates the last call date for the specified contact.
     * <p>
     * Updates the contact's last activity date if appropriate.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact ID for which to set the last date
     * @param date
     *            the date to set; null forces a recalculation of last call date and last activity date (unless last
     *            call date was already null prior to calling this function)
     * @throws SQLException
     *             if there is a database access problem
     * @see #recalculateLastCallDate(int)
     * @see #recalculateLastActivityDate(int)
     */
    public static void updateLastCallDate(int contactId, LocalDateTime date) throws SQLException {
        log.trace("updateLastCallDate({},{})", contactId, date);
        updateLastXDate(contactId, date, LASTTYPE_CALL);
    }

    /**
     * Updates the last challenge date for the specified contact.
     * <p>
     * Updates the contact's last activity date if appropriate.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact ID for which to set the last date
     * @param date
     *            the date to set; null forces a recalculation of last challenge date and last activity date (unless
     *            last challenge date was already null prior to calling this function)
     * @throws SQLException
     *             if there is a database access problem
     * @see #recalculateLastChallengeDate(int)
     * @see #recalculateLastActivityDate(int)
     */
    public static void updateLastChallengeDate(int contactId, LocalDateTime date) throws SQLException {
        log.trace("updateLastChallengeDate({},{})", contactId, date);
        updateLastXDate(contactId, date, LASTTYPE_CHALLENGE);
    }

    /**
     * Updates the specified contact's "LastEdit" value to be "now".
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param historyId
     *            the ID of the history to update
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void updateLastEdit(int contactId) throws SQLException {
        log.trace("updateLastEdit({})", contactId);
        TntDb.updateTableLastEdit(TntDb.TABLE_CONTACT, contactId);
    }

    /**
     * Updates the last letter date for the specified contact.
     * <p>
     * Updates the contact's last activity date if appropriate.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact ID for which to set the last date
     * @param date
     *            the date to set; null forces a recalculation of last letter date and last activity date (unless last
     *            letter date was already null prior to calling this function)
     * @throws SQLException
     *             if there is a database access problem
     * @see #recalculateLastLetterDate(int)
     * @see #recalculateLastActivityDate(int)
     */
    public static void updateLastLetterDate(int contactId, LocalDateTime date) throws SQLException {
        log.trace("updateLastLetterDate({},{})", contactId, date);
        updateLastXDate(contactId, date, LASTTYPE_LETTER);
    }

    /**
     * Updates the last pre-call date for the specified contact.
     * <p>
     * Updates the contact's last activity date if appropriate.
     * <p>
     * Does NOT automatically update the last letter date.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact ID for which to set the last date
     * @param date
     *            the date to set; null forces a recalculation of last pre-call date and last activity date (unless last
     *            pre-call date was already null prior to calling this function)
     * @throws SQLException
     *             if there is a database access problem
     * @see #updateLastLetterDate(int, Date)
     * @see #recalculateLastPreCallDate(int)
     * @see #recalculateLastLetterDate(int)
     * @see #recalculateLastActivityDate(int)
     */
    public static void updateLastPreCallDate(int contactId, LocalDateTime date) throws SQLException {
        log.trace("updateLastPreCallDate({},{})", contactId, date);
        updateLastXDate(contactId, date, LASTTYPE_PRECALL);
    }

    /**
     * Updates the last thank date for the specified contact.
     * <p>
     * Updates the contact's last thank date if appropriate.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact ID for which to set the last date
     * @param date
     *            the date to set; null forces a recalculation of last thank date and last activity date (unless last
     *            thank date was already null prior to calling this function)
     * @throws SQLException
     *             if there is a database access problem
     * @see #recalculateLastThankDate(int)
     * @see #recalculateLastActivityDate(int)
     */
    public static void updateLastThankDate(int contactId, LocalDateTime date) throws SQLException {
        log.trace("updateLastThankDate({},{})", contactId, date);
        updateLastXDate(contactId, date, LASTTYPE_THANK);
    }

    /**
     * Updates the last visit date for the specified contact.
     * <p>
     * Updates the contact's last activity date if appropriate.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact ID for which to set the last date
     * @param date
     *            the date to set; null forces a recalculation of last visit date and last activity date (unless last
     *            visit date was already null prior to calling this function)
     * @throws SQLException
     *             if there is a database access problem
     * @see #recalculateLastVisitDate(int)
     * @see #recalculateLastActivityDate(int)
     */
    public static void updateLastVisitDate(int contactId, LocalDateTime date) throws SQLException {
        log.trace("updateLastVisitDate({},{})", contactId, date);
        updateLastXDate(contactId, date, LASTTYPE_VISIT);
    }

    /**
     * Updates the last X date (where X is specified by {@code lastType}) for the specified contact.
     * <p>
     * Updates the contact's last activity date if appropriate.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact ID for which to set the last date
     * @param date
     *            the date to set; null forces a recalculation of last X date and last activity date (unless last X date
     *            was already null prior to calling this function)
     * @param lastType
     *            the type of last date to set
     * @throws SQLException
     *             if there is a database access problem
     * @see #recalculateLastXDate(int,String)
     * @see #recalculateLastActivityDate(int)
     */
    protected static void updateLastXDate(int contactId, LocalDateTime date, String lastType) throws SQLException {
        log.trace("updateLastXDate({},{},{})", contactId, date, lastType);
        updateLastXDate(contactId, date, lastType, false);
    }

    /**
     * Updates the last X date (where X is specified by {@code lastType}) for the specified contact.
     * <p>
     * Updates the contact's last activity date if appropriate.
     * <p>
     * Does NOT automatically commit changes to the Tnt database.
     *
     * @param contactId
     *            the contact ID for which to set the last date
     * @param date
     *            the date to set; null forces a recalculation of last X date and last activity date (unless last X date
     *            was already null prior to calling this function)
     * @param lastType
     *            the type of last date to set
     * @param force
     *            true forces this update even if {@code date} is older than the date in the Tnt database; false
     *            will not override a newer date in the Tnt database. Force is ignored if {@code date} is null and the
     *            date was already null prior to calling this function)
     * @throws SQLException
     *             if there is a database access problem
     * @see #recalculateLastXDate(int,String)
     * @see #recalculateLastActivityDate(int)
     */
    protected static void updateLastXDate(
        int contactId,
        LocalDateTime date,
        String lastType,
        boolean force) throws SQLException {
        log.trace("updateLastXDate({},{},{},{})", contactId, date, lastType, force);

        // Update the given field
        LocalDateTime lastDate = getLastXDate(contactId, lastType);
        if (date == null && lastDate == null)
            return;

        if (date == null || lastDate == null || force || lastDate.isBefore(date)) {
            String query = String.format("UPDATE [Contact] SET [Last%s] = ? WHERE [ContactId] = ?", lastType);
            PreparedStatement stmt = TntDb.getConnection().prepareStatement(query);
            if (date == null) {
                stmt.setNull(1, java.sql.Types.DATE);
            } else {
                stmt.setObject(1, date.toLocalDate());
            }
            stmt.setInt(2, contactId);
            stmt.executeUpdate();
            // LastEdit is not updated for calculated fields
        }

        // If date was set to null, we need to recalculate the last date (and LastActivity, just in case)
        // This must happen AFTER the set to null because if it *should* be null, this method won't be called again
        if (date == null) {
            recalculateLastXDate(contactId, lastType);
            if (!lastType.equals(LASTTYPE_ACTIVITY)) {
                recalculateLastActivityDate(contactId);
            }
        } else if (!lastType.equals(LASTTYPE_ACTIVITY)) {
            // Also update Contact's LastActivity
            updateLastActivityDate(contactId, date);
        }
    }

}
