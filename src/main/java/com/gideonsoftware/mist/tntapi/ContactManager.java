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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.exceptions.TntDbException;
import com.gideonsoftware.mist.tntapi.entities.Contact;
import com.gideonsoftware.mist.tntapi.entities.ContactInfo;
import com.gideonsoftware.mist.tntapi.entities.History;
import com.gideonsoftware.mist.tntapi.entities.TaskType;

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
     *             TODO: Rollback shouldn't send this
     */
    public static void addNewEmailAddress(
        String email,
        Integer contactId,
        boolean usePrimaryContact) throws SQLException, TntDbException {
        log.trace("addNewEmailAddress({},{},{})", email, contactId, usePrimaryContact);

        Contact contact = get(contactId);
        String emailField = "";
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
            if (usePrimaryContact)
                email = String.format("%s,%s", contact.getEmail3(), email);
            else
                email = String.format("%s,%s", contact.getSpouseEmail3(), email);
        }

        String query = String.format(
            "UPDATE [Contact] SET [%s] = %s, [%s] = -1 WHERE [ContactId] = %s",
            emailField,
            TntDb.formatDbString(email),
            emailField + "IsValid",
            TntDb.formatDbInt(contactId));
        try {
            TntDb.runQuery(query);
        } catch (SQLException e) {
            TntDb.rollback();
            throw e;
        }

        // Also update "Email" field if needed
        if (contact.getEmail().isBlank()) {
            query = String.format(
                "UPDATE [Contact] SET [Email] = %s, [EmailIsValid] = -1 WHERE [ContactId] = %s",
                TntDb.formatDbString(email),
                TntDb.formatDbInt(contactId));
            try {
                TntDb.runQuery(query);
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
        String[][] colValuePairs = {
            { "ContactID", contact.getContactId().toString() },
            { "LastEdit", TntDb.formatDbDate(now) },
            { "CreatedDate", TntDb.formatDbDateNoTime(now) },
            { "RejectedDuplicateContactIDs", TntDb.formatDbString(contact.getRejectedDuplicateContactIDs()) },
            { "FileAs", TntDb.formatDbString(contact.getFileAs(), 75) },
            { "FileAsIsCustom", TntDb.formatDbBoolean(contact.isFileAsCustom()) },
            { "FullName", TntDb.formatDbString(contact.getFullName()) },
            { "FullNameIsCustom", TntDb.formatDbBoolean(contact.isFullNameCustom()) },
            { "Greeting", TntDb.formatDbString(contact.getGreeting()) },
            { "GreetingIsCustom", TntDb.formatDbBoolean(contact.isGreetingCustom()) },
            { "Salutation", TntDb.formatDbString(contact.getSalutation()) },
            { "SalutationIsCustom", TntDb.formatDbBoolean(contact.isSalutationCustom()) },
            { "ShortName", TntDb.formatDbString(contact.getShortName()) },
            { "ShortNameIsCustom", TntDb.formatDbBoolean(contact.isShortNameCustom()) },
            { "MailingAddressBlock", TntDb.formatDbString(contact.getMailingAddressBlock()) },
            { "MailingAddressIsDeliverable", TntDb.formatDbBoolean(contact.isMailingAddressDeliverable()) },
            { "Phone", TntDb.formatDbString(contact.getPhone()) },
            { "PhoneIsValid", TntDb.formatDbBoolean(contact.isPhoneValid()) },
            { "Email", TntDb.formatDbString(contact.getEmail()) },
            { "EmailIsValid", TntDb.formatDbBoolean(contact.isEmailValid()) },
            { "IsOrganization", TntDb.formatDbBoolean(contact.isOrganization()) },
            { "OrganizationName", TntDb.formatDbString(contact.getOrganizationName(), 50) },
            { "OrgContactPerson", TntDb.formatDbString(contact.getOrgContactPerson(), 50) },
            { "Title", TntDb.formatDbString(contact.getTitle(), 25) },
            { "FirstName", TntDb.formatDbString(contact.getFirstName(), 25) },
            { "MiddleName", TntDb.formatDbString(contact.getMiddleName(), 25) },
            { "LastName", TntDb.formatDbString(contact.getLastName(), 50) },
            { "Suffix", TntDb.formatDbString(contact.getSuffix(), 25) },
            { "SpouseTitle", TntDb.formatDbString(contact.getSpouseTitle(), 25) },
            { "SpouseFirstName", TntDb.formatDbString(contact.getSpouseFirstName(), 25) },
            { "SpouseMiddleName", TntDb.formatDbString(contact.getSpouseMiddleName(), 25) },
            { "SpouseLastName", TntDb.formatDbString(contact.getSpouseLastName(), 50) },
            { "Deceased", TntDb.formatDbBoolean(contact.isDeceased()) },
            { "MailingAddressType", TntDb.formatDbInt(contact.getMailingAddressType()) },
            { "MailingStreetAddress", TntDb.formatDbString(contact.getMailingStreetAddress()) },
            { "MailingCity", TntDb.formatDbString(contact.getMailingCity(), 50) },
            { "MailingState", TntDb.formatDbString(contact.getMailingState(), 50) },
            { "MailingPostalCode", TntDb.formatDbString(contact.getMailingPostalCode(), 25) },
            { "MailingCountry", TntDb.formatDbString(contact.getMailingCountry(), 50) },
            { "HomeStreetAddress", TntDb.formatDbString(contact.getHomeStreetAddress()) },
            { "HomeCity", TntDb.formatDbString(contact.getHomeCity(), 50) },
            { "HomeState", TntDb.formatDbString(contact.getHomeState(), 50) },
            { "HomePostalCode", TntDb.formatDbString(contact.getHomePostalCode(), 25) },
            { "HomeCountryID", TntDb.formatDbInt(contact.getHomeCountryId()) },
            { "HomeCountry", TntDb.formatDbString(contact.getHomeCountry(), 50) },
            { "HomeAddressIsDeliverable", TntDb.formatDbBoolean(contact.isHomeAddressDeliverable()) },
            { "HomeAddressBlock", TntDb.formatDbString(contact.getHomeAddressBlock()) },
            { "HomeAddressBlockIsCustom", TntDb.formatDbBoolean(contact.isHomeAddressBlockCustom()) },
            { "OtherStreetAddress", TntDb.formatDbString(contact.getOtherStreetAddress()) },
            { "OtherCity", TntDb.formatDbString(contact.getOtherCity(), 50) },
            { "OtherState", TntDb.formatDbString(contact.getOtherState(), 50) },
            { "OtherPostalCode", TntDb.formatDbString(contact.getOtherPostalCode(), 25) },
            { "OtherCountryID", TntDb.formatDbInt(contact.getOtherCountryId()) },
            { "OtherCountry", TntDb.formatDbString(contact.getOtherCountry(), 50) },
            { "OtherAddressIsDeliverable", TntDb.formatDbBoolean(contact.isOtherAddressDeliverable()) },
            { "OtherAddressBlock", TntDb.formatDbString(contact.getOtherAddressBlock()) },
            { "OtherAddressBlockIsCustom", TntDb.formatDbBoolean(contact.isOtherAddressBlockCustom()) },
            { "BusinessName", TntDb.formatDbString(contact.getBusinessName()) },
            { "BusinessStreetAddress", TntDb.formatDbString(contact.getBusinessStreetAddress()) },
            { "BusinessCity", TntDb.formatDbString(contact.getBusinessCity(), 50) },
            { "BusinessState", TntDb.formatDbString(contact.getBusinessState(), 50) },
            { "BusinessPostalCode", TntDb.formatDbString(contact.getBusinessPostalCode(), 25) },
            { "BusinessCountryID", TntDb.formatDbInt(contact.getBusinessCountryId()) },
            { "BusinessCountry", TntDb.formatDbString(contact.getBusinessCountry(), 50) },
            { "BusinessAddressIsDeliverable", TntDb.formatDbBoolean(contact.isBusinessAddressDeliverable()) },
            { "BusinessAddressBlock", TntDb.formatDbString(contact.getBusinessAddressBlock()) },
            { "BusinessAddressBlockIsCustom", TntDb.formatDbBoolean(contact.isBusinessAddressBlockCustom()) },
            { "SpouseBusinessName", TntDb.formatDbString(contact.getSpouseBusinessName()) },
            { "SpouseBusinessStreetAddress", TntDb.formatDbString(contact.getSpouseBusinessStreetAddress()) },
            { "SpouseBusinessCity", TntDb.formatDbString(contact.getSpouseBusinessCity(), 50) },
            { "SpouseBusinessState", TntDb.formatDbString(contact.getSpouseBusinessState(), 50) },
            { "SpouseBusinessPostalCode", TntDb.formatDbString(contact.getSpouseBusinessPostalCode(), 25) },
            { "SpouseBusinessCountryID", TntDb.formatDbInt(contact.getSpouseBusinessCountryId()) },
            { "SpouseBusinessCountry", TntDb.formatDbString(contact.getSpouseBusinessCountry(), 50) },
            {
                "SpouseBusinessAddressIsDeliverable",
                TntDb.formatDbBoolean(contact.isSpouseBusinessAddressDeliverable()) },
            { "SpouseBusinessAddressBlock", TntDb.formatDbString(contact.getSpouseBusinessAddressBlock()) },
            {
                "SpouseBusinessAddressBlockIsCustom",
                TntDb.formatDbBoolean(contact.isSpouseBusinessAddressBlockCustom()) },
            { "PreferredPhoneType", TntDb.formatDbInt(contact.getPreferredPhoneType()) },
            { "PhoneIsValidMask", TntDb.formatDbInt(contact.getPhoneIsValidMask()) },
            { "PhoneCountryIDs", TntDb.formatDbString(contact.getPhoneCountryIds()) },
            { "HomePhone", TntDb.formatDbString(contact.getHomePhone()) },
            { "HomePhone2", TntDb.formatDbString(contact.getHomePhone2()) },
            { "HomeFax", TntDb.formatDbString(contact.getHomeFax()) },
            { "OtherPhone", TntDb.formatDbString(contact.getOtherPhone()) },
            { "OtherFax", TntDb.formatDbString(contact.getOtherFax()) },
            { "BusinessPhone", TntDb.formatDbString(contact.getBusinessPhone()) },
            { "BusinessPhone2", TntDb.formatDbString(contact.getBusinessPhone2()) },
            { "BusinessFax", TntDb.formatDbString(contact.getBusinessFax()) },
            { "CompanyMainPhone", TntDb.formatDbString(contact.getCompanyMainPhone()) },
            { "MobilePhone", TntDb.formatDbString(contact.getMobilePhone()) },
            { "MobilePhone2", TntDb.formatDbString(contact.getMobilePhone2()) },
            { "PagerNumber", TntDb.formatDbString(contact.getPagerNumber()) },
            { "SpouseBusinessPhone", TntDb.formatDbString(contact.getSpouseBusinessPhone()) },
            { "SpouseBusinessPhone2", TntDb.formatDbString(contact.getSpouseBusinessPhone2()) },
            { "SpouseBusinessFax", TntDb.formatDbString(contact.getSpouseBusinessFax()) },
            { "SpouseCompanyMainPhone", TntDb.formatDbString(contact.getSpouseCompanyMainPhone()) },
            { "SpouseMobilePhone", TntDb.formatDbString(contact.getSpouseMobilePhone()) },
            { "SpouseMobilePhone2", TntDb.formatDbString(contact.getSpouseMobilePhone2()) },
            { "SpousePagerNumber", TntDb.formatDbString(contact.getSpousePagerNumber()) },
            { "PreferredEmailTypes", TntDb.formatDbInt(contact.getPreferredEmailTypes()) },
            { "EmailLabels", TntDb.formatDbString(contact.getEmailLabels()) },
            { "Email1", TntDb.formatDbString(contact.getEmail1()) },
            { "Email2", TntDb.formatDbString(contact.getEmail2()) },
            { "Email3", TntDb.formatDbString(contact.getEmail3()) },
            { "Email1IsValid", TntDb.formatDbBoolean(contact.isEmail1Valid()) },
            { "Email2IsValid", TntDb.formatDbBoolean(contact.isEmail2Valid()) },
            { "Email3IsValid", TntDb.formatDbBoolean(contact.isEmail3Valid()) },
            { "EmailCustomGreeting", TntDb.formatDbString(contact.getEmailCustomGreeting()) },
            { "EmailCustomSalutation", TntDb.formatDbString(contact.getEmailCustomSalutation()) },
            { "SpouseEmail1", TntDb.formatDbString(contact.getSpouseEmail1()) },
            { "SpouseEmail2", TntDb.formatDbString(contact.getSpouseEmail2()) },
            { "SpouseEmail3", TntDb.formatDbString(contact.getSpouseEmail3()) },
            { "SpouseEmail1IsValid", TntDb.formatDbBoolean(contact.isSpouseEmail1Valid()) },
            { "SpouseEmail2IsValid", TntDb.formatDbBoolean(contact.isSpouseEmail2Valid()) },
            { "SpouseEmail3IsValid", TntDb.formatDbBoolean(contact.isSpouseEmail3Valid()) },
            { "SpouseEmailCustomGreeting", TntDb.formatDbString(contact.getSpouseEmailCustomGreeting()) },
            { "SpouseEmailCustomSalutation", TntDb.formatDbString(contact.getSpouseEmailCustomSalutation()) },
            { "WebPage1", TntDb.formatDbString(contact.getWebPage1()) },
            { "WebPage2", TntDb.formatDbString(contact.getWebPage2()) },
            { "VoiceSkype", TntDb.formatDbString(contact.getVoiceSkype()) },
            { "IMAddress", TntDb.formatDbString(contact.getImAddress()) },
            { "SocialWeb1", TntDb.formatDbString(contact.getSocialWeb1()) },
            { "SocialWeb2", TntDb.formatDbString(contact.getSocialWeb2()) },
            { "SocialWeb3", TntDb.formatDbString(contact.getSocialWeb3()) },
            { "SocialWeb4", TntDb.formatDbString(contact.getSocialWeb4()) },
            { "SpouseWebPage1", TntDb.formatDbString(contact.getSpouseWebPage1()) },
            { "SpouseWebPage2", TntDb.formatDbString(contact.getSpouseWebPage2()) },
            { "SpouseVoiceSkype", TntDb.formatDbString(contact.getSpouseVoiceSkype()) },
            { "SpouseIMAddress", TntDb.formatDbString(contact.getSpouseImAddress()) },
            { "SpouseSocialWeb1", TntDb.formatDbString(contact.getSpouseSocialWeb1()) },
            { "SpouseSocialWeb2", TntDb.formatDbString(contact.getSpouseSocialWeb2()) },
            { "SpouseSocialWeb3", TntDb.formatDbString(contact.getSpouseSocialWeb3()) },
            { "SpouseSocialWeb4", TntDb.formatDbString(contact.getSpouseSocialWeb4()) },
            { "NotesAsRTF", TntDb.formatDbString(contact.getNotesAsRtf()) },
            { "Notes", TntDb.formatDbString(contact.getNotes()) },
            { "FamilySideID", TntDb.formatDbInt(contact.getFamilySideID()) },
            { "FamilyLevelID", TntDb.formatDbInt(contact.getFamilyLevelID()) },
            { "Children", TntDb.formatDbString(contact.getChildren()) },
            { "Interests", TntDb.formatDbString(contact.getInterests()) },
            { "Nickname", TntDb.formatDbString(contact.getNickname()) },
            { "Profession", TntDb.formatDbString(contact.getProfession()) },
            { "SpouseInterests", TntDb.formatDbString(contact.getSpouseInterests()) },
            { "SpouseNickname", TntDb.formatDbString(contact.getSpouseNickname()) },
            { "SpouseProfession", TntDb.formatDbString(contact.getSpouseProfession()) },
            { "AnniversaryMonth", TntDb.formatDbInt(contact.getAnniversaryMonth()) },
            { "AnniversaryDay", TntDb.formatDbInt(contact.getAnniversaryDay()) },
            { "AnniversaryYear", TntDb.formatDbInt(contact.getAnniversaryYear()) },
            { "BirthdayMonth", TntDb.formatDbInt(contact.getBirthdayMonth()) },
            { "BirthdayDay", TntDb.formatDbInt(contact.getBirthdayDay()) },
            { "BirthdayYear", TntDb.formatDbInt(contact.getBirthdayYear()) },
            { "SpouseBirthdayMonth", TntDb.formatDbInt(contact.getSpouseBirthdayMonth()) },
            { "SpouseBirthdayDay", TntDb.formatDbInt(contact.getSpouseBirthdayDay()) },
            { "SpouseBirthdayYear", TntDb.formatDbInt(contact.getSpouseBirthdayYear()) },
            { "Categories", TntDb.formatDbString(contact.getCategories()) },
            { "User1", TntDb.formatDbString(contact.getUser1()) },
            { "User2", TntDb.formatDbString(contact.getUser2()) },
            { "User3", TntDb.formatDbString(contact.getUser3()) },
            { "User4", TntDb.formatDbString(contact.getUser4()) },
            { "User5", TntDb.formatDbString(contact.getUser5()) },
            { "User6", TntDb.formatDbString(contact.getUser6()) },
            { "User7", TntDb.formatDbString(contact.getUser7()) },
            { "User8", TntDb.formatDbString(contact.getUser8()) },
            { "UserStatus", TntDb.formatDbString(contact.getUserStatus()) },
            { "MapAddressType", TntDb.formatDbInt(contact.getMapAddressType()) },
            { "MapLat", TntDb.formatDbInt(contact.getMapLat()) },
            { "MapLng", TntDb.formatDbInt(contact.getMapLng()) },
            { "MapStatus", TntDb.formatDbString(contact.getMapStatus()) },
            { "PledgeAmount", TntDb.formatDbCurrency(contact.getPledgeAmount()) },
            { "PledgeFrequencyID", TntDb.formatDbInt(contact.getPledgeFrequencyId()) },
            { "PledgeReceived", TntDb.formatDbBoolean(contact.isPledgeReceived()) },
            { "PledgeStartDate", TntDb.formatDbDateNoTime(contact.getPledgeStartDate()) },
            { "PledgeCurrencyID", TntDb.formatDbInt(contact.getPledgeCurrencyId()) },
            { "ReferredBy", TntDb.formatDbString(contact.getReferredBy()) },
            { "ReferredByList", TntDb.formatDbString(contact.getReferredByList()) },
            { "MPDPhaseID", TntDb.formatDbInt(contact.getMpdPhaseId()) },
            { "FundRepID", TntDb.formatDbInt(contact.getFundRepId()) },
            { "NextAsk", TntDb.formatDbDateNoTime(contact.getNextAsk()) },
            { "NextAskAmount", TntDb.formatDbCurrency(contact.getNextAskAmount()) },
            { "EstimatedAnnualCapacity", TntDb.formatDbCurrency(contact.getEstimatedAnnualCapacity()) },
            { "NeverAsk", TntDb.formatDbBoolean(contact.isNeverAsk()) },
            { "Region", TntDb.formatDbString(contact.getRegion()) },
            { "LikelyToGiveID", TntDb.formatDbInt(contact.getLikelyToGiveId()) },
            { "ChurchName", TntDb.formatDbString(contact.getChurchName()) },
            { "SendNewsletter", TntDb.formatDbBoolean(contact.isSendNewsletter()) },
            { "NewsletterMediaPref", TntDb.formatDbString(contact.getNewsletterMediaPref(), 6) }, // 4 in TntDb
            { "NewsletterLangID", TntDb.formatDbInt(contact.getNewsletterLangId()) },
            { "DirectDeposit", TntDb.formatDbBoolean(contact.isDirectDeposit()) },
            { "Magazine", TntDb.formatDbBoolean(contact.isMagazine()) },
            { "MonthlyPledge", TntDb.formatDbCurrency(contact.getMonthlyPledge()) },
            { "FirstGiftDate", TntDb.formatDbDateNoTime(contact.getFirstGiftDate()) },
            { "LastGiftDate", TntDb.formatDbDateNoTime(contact.getLastGiftDate()) },
            { "LastGiftAmount", TntDb.formatDbCurrency(contact.getLastGiftAmount()) },
            { "LastGiftCurrencyID", TntDb.formatDbInt(contact.getLastGiftCurrencyId()) },
            { "LastGiftOrganizationID", TntDb.formatDbInt(contact.getLastGiftOrganizationId()) },
            { "LastGiftOrgDonorCode", TntDb.formatDbString(contact.getLastGiftOrgDonorCode()) },
            { "LastGiftPaymentMethod", TntDb.formatDbString(contact.getLastGiftPaymentMethod()) },
            { "PrevYearTotal", TntDb.formatDbCurrency(contact.getPrevYearTotal()) },
            { "YearTotal", TntDb.formatDbCurrency(contact.getYearTotal()) },
            { "LifetimeTotal", TntDb.formatDbCurrency(contact.getLifetimeTotal()) },
            { "LifetimeNumberOfGifts", TntDb.formatDbInt(contact.getLifetimeNumberOfGifts()) },
            { "LargestGift", TntDb.formatDbCurrency(contact.getLargestGift()) },
            { "GoodUntil", TntDb.formatDbDateNoTime(contact.getGoodUntil()) },
            { "AveMonthlyGift", TntDb.formatDbCurrency(contact.getAveMonthlyGift()) },
            { "LastDateInAve", TntDb.formatDbDateNoTime(contact.getLastDateInAve()) },
            { "TwelveMonthTotal", TntDb.formatDbCurrency(contact.getTwelveMonthTotal()) },
            { "BaseCurrencyID", TntDb.formatDbInt(contact.getBaseCurrencyId()) },
            { "BaseMonthlyPledge", TntDb.formatDbCurrency(contact.getBaseMonthlyPledge()) },
            { "BaseLastGiftAmount", TntDb.formatDbCurrency(contact.getBaseLastGiftAmount()) },
            { "BasePrevYearTotal", TntDb.formatDbCurrency(contact.getBasePrevYearTotal()) },
            { "BaseYearTotal", TntDb.formatDbCurrency(contact.getBaseYearTotal()) },
            { "BaseLifetimeTotal", TntDb.formatDbCurrency(contact.getBaseLifetimeTotal()) },
            { "BaseLargestGift", TntDb.formatDbCurrency(contact.getBaseLargestGift()) },
            { "BaseAveMonthlyGift", TntDb.formatDbCurrency(contact.getBaseAveMonthlyGift()) },
            { "BaseTwelveMonthTotal", TntDb.formatDbCurrency(contact.getBaseTwelveMonthTotal()) },
            { "LastActivity", TntDb.formatDbDateNoTime(contact.getLastActivity()) },
            { "LastAppointment", TntDb.formatDbDateNoTime(contact.getLastAppointment()) },
            { "LastCall", TntDb.formatDbDateNoTime(contact.getLastCall()) },
            { "LastPreCall", TntDb.formatDbDateNoTime(contact.getLastPreCall()) },
            { "LastLetter", TntDb.formatDbDateNoTime(contact.getLastLetter()) },
            { "LastVisit", TntDb.formatDbDateNoTime(contact.getLastVisit()) },
            { "LastThank", TntDb.formatDbDateNoTime(contact.getLastThank()) },
            { "LastChallenge", TntDb.formatDbDateNoTime(contact.getLastChallenge()) },
            { "CampaignsSinceLastGift", TntDb.formatDbInt(contact.getCampaignsSinceLastGift()) },
            { "ChallengesSinceLastGift", TntDb.formatDbInt(contact.getChallengesSinceLastGift()) },
            { "OrgDonorCodes", TntDb.formatDbString(contact.getOrgDonorCodes()) } };

        try {
            TntDb.runQuery(TntDb.createInsertQuery(TntDb.TABLE_CONTACT, colValuePairs));
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

        String query = String.format("SELECT * FROM [Contact] WHERE [ContactID] = %s", contactId);
        ResultSet rs = TntDb.runQuery(query);
        if (!rs.first())
            return null;

        Contact contact = new Contact();
        contact.setContactId(rs.getInt("ContactID"));
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
        contact.setMailingAddressType(rs.getInt("MailingAddressType"));
        contact.setMailingStreetAddress(rs.getString("MailingStreetAddress"));
        contact.setMailingCity(rs.getString("MailingCity"));
        contact.setMailingState(rs.getString("MailingState"));
        contact.setMailingPostalCode(rs.getString("MailingPostalCode"));
        contact.setMailingCountry(rs.getString("MailingCountry"));
        contact.setHomeStreetAddress(rs.getString("HomeStreetAddress"));
        contact.setHomeCity(rs.getString("HomeCity"));
        contact.setHomeState(rs.getString("HomeState"));
        contact.setHomePostalCode(rs.getString("HomePostalCode"));
        contact.setHomeCountryID(rs.getInt("HomeCountryID"));
        contact.setHomeCountry(rs.getString("HomeCountry"));
        contact.setHomeAddressIsDeliverable(rs.getBoolean("HomeAddressIsDeliverable"));
        contact.setHomeAddressBlock(rs.getString("HomeAddressBlock"));
        contact.setHomeAddressBlockIsCustom(rs.getBoolean("HomeAddressBlockIsCustom"));
        contact.setOtherStreetAddress(rs.getString("OtherStreetAddress"));
        contact.setOtherCity(rs.getString("OtherCity"));
        contact.setOtherState(rs.getString("OtherState"));
        contact.setOtherPostalCode(rs.getString("OtherPostalCode"));
        contact.setOtherCountryID(rs.getInt("OtherCountryID"));
        contact.setOtherCountry(rs.getString("OtherCountry"));
        contact.setOtherAddressIsDeliverable(rs.getBoolean("OtherAddressIsDeliverable"));
        contact.setOtherAddressBlock(rs.getString("OtherAddressBlock"));
        contact.setOtherAddressBlockIsCustom(rs.getBoolean("OtherAddressBlockIsCustom"));
        contact.setBusinessName(rs.getString("BusinessName"));
        contact.setBusinessStreetAddress(rs.getString("BusinessStreetAddress"));
        contact.setBusinessCity(rs.getString("BusinessCity"));
        contact.setBusinessState(rs.getString("BusinessState"));
        contact.setBusinessPostalCode(rs.getString("BusinessPostalCode"));
        contact.setBusinessCountryId(rs.getInt("BusinessCountryID"));
        contact.setBusinessCountry(rs.getString("BusinessCountry"));
        contact.setBusinessAddressIsDeliverable(rs.getBoolean("BusinessAddressIsDeliverable"));
        contact.setBusinessAddressBlock(rs.getString("BusinessAddressBlock"));
        contact.setBusinessAddressBlockIsCustom(rs.getBoolean("BusinessAddressBlockIsCustom"));
        contact.setSpouseBusinessName(rs.getString("SpouseBusinessName"));
        contact.setSpouseBusinessStreetAddress(rs.getString("SpouseBusinessStreetAddress"));
        contact.setSpouseBusinessCity(rs.getString("SpouseBusinessCity"));
        contact.setSpouseBusinessState(rs.getString("SpouseBusinessState"));
        contact.setSpouseBusinessPostalCode(rs.getString("SpouseBusinessPostalCode"));
        contact.setSpouseBusinessCountryId(rs.getInt("SpouseBusinessCountryID"));
        contact.setSpouseBusinessCountry(rs.getString("SpouseBusinessCountry"));
        contact.setSpouseBusinessAddressIsDeliverable(rs.getBoolean("SpouseBusinessAddressIsDeliverable"));
        contact.setSpouseBusinessAddressBlock(rs.getString("SpouseBusinessAddressBlock"));
        contact.setSpouseBusinessAddressBlockIsCustom(rs.getBoolean("SpouseBusinessAddressBlockIsCustom"));
        contact.setPreferredPhoneType(rs.getInt("PreferredPhoneType"));
        contact.setPhoneIsValidMask(rs.getInt("PhoneIsValidMask"));
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
        contact.setPreferredEmailTypes(rs.getInt("PreferredEmailTypes"));
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
        contact.setFamilySideID(rs.getInt("FamilySideID"));
        contact.setFamilyLevelID(rs.getInt("FamilyLevelID"));
        contact.setChildren(rs.getString("Children"));
        contact.setInterests(rs.getString("Interests"));
        contact.setNickname(rs.getString("Nickname"));
        contact.setProfession(rs.getString("Profession"));
        contact.setSpouseInterests(rs.getString("SpouseInterests"));
        contact.setSpouseNickname(rs.getString("SpouseNickname"));
        contact.setSpouseProfession(rs.getString("SpouseProfession"));
        contact.setAnniversaryMonth(rs.getInt("AnniversaryMonth"));
        contact.setAnniversaryDay(rs.getInt("AnniversaryDay"));
        contact.setAnniversaryYear(rs.getInt("AnniversaryYear"));
        contact.setBirthdayMonth(rs.getInt("BirthdayMonth"));
        contact.setBirthdayDay(rs.getInt("BirthdayDay"));
        contact.setBirthdayYear(rs.getInt("BirthdayYear"));
        contact.setSpouseBirthdayMonth(rs.getInt("SpouseBirthdayMonth"));
        contact.setSpouseBirthdayDay(rs.getInt("SpouseBirthdayDay"));
        contact.setSpouseBirthdayYear(rs.getInt("SpouseBirthdayYear"));
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
        contact.setMapAddressType(rs.getInt("MapAddressType"));
        contact.setMapLat(rs.getInt("MapLat"));
        contact.setMapLng(rs.getInt("MapLng"));
        contact.setMapStatus(rs.getString("MapStatus"));
        contact.setPledgeAmount(TntDb.floatToMoney(rs.getFloat("PledgeAmount")));
        contact.setPledgeFrequencyId(rs.getInt("PledgeFrequencyID"));
        contact.setPledgeReceived(rs.getBoolean("PledgeReceived"));
        contact.setPledgeStartDate(TntDb.timestampToDate(rs.getTimestamp("PledgeStartDate")));
        contact.setPledgeCurrencyId(rs.getInt("PledgeCurrencyID"));
        contact.setReferredBy(rs.getString("ReferredBy"));
        contact.setReferredByList(rs.getString("ReferredByList"));
        contact.setMpdPhaseId(rs.getInt("MPDPhaseID"));
        contact.setFundRepId(rs.getInt("FundRepID"));
        contact.setNextAsk(TntDb.timestampToDate(rs.getTimestamp("NextAsk")));
        contact.setNextAskAmount(TntDb.floatToMoney(rs.getFloat("NextAskAmount")));
        contact.setEstimatedAnnualCapacity(TntDb.floatToMoney(rs.getFloat("EstimatedAnnualCapacity")));
        contact.setNeverAsk(rs.getBoolean("NeverAsk"));
        contact.setRegion(rs.getString("Region"));
        contact.setLikelyToGiveId(rs.getInt("LikelyToGiveID"));
        contact.setChurchName(rs.getString("ChurchName"));
        contact.setSendNewsletter(rs.getBoolean("SendNewsletter"));
        contact.setNewsletterMediaPref(rs.getString("NewsletterMediaPref"));
        contact.setNewsletterLangId(rs.getInt("NewsletterLangID"));
        contact.setDirectDeposit(rs.getBoolean("DirectDeposit"));
        contact.setMagazine(rs.getBoolean("Magazine"));
        contact.setMonthlyPledge(TntDb.floatToMoney(rs.getFloat("MonthlyPledge")));
        contact.setFirstGiftDate(TntDb.timestampToDate(rs.getTimestamp("FirstGiftDate")));
        contact.setLastGiftDate(TntDb.timestampToDate(rs.getTimestamp("LastGiftDate")));
        contact.setLastGiftAmount(TntDb.floatToMoney(rs.getFloat("LastGiftAmount")));
        contact.setLastGiftCurrencyId(rs.getInt("LastGiftCurrencyID"));
        contact.setLastGiftOrganizationId(rs.getInt("LastGiftOrganizationID"));
        contact.setLastGiftOrgDonorCode(rs.getString("LastGiftOrgDonorCode"));
        contact.setLastGiftPaymentMethod(rs.getString("LastGiftPaymentMethod"));
        contact.setPrevYearTotal(TntDb.floatToMoney(rs.getFloat("PrevYearTotal")));
        contact.setYearTotal(TntDb.floatToMoney(rs.getFloat("YearTotal")));
        contact.setLifetimeTotal(TntDb.floatToMoney(rs.getFloat("LifetimeTotal")));
        contact.setLifetimeNumberOfGifts(rs.getInt("LifetimeNumberOfGifts"));
        contact.setLargestGift(TntDb.floatToMoney(rs.getFloat("LargestGift")));
        contact.setGoodUntil(TntDb.timestampToDate(rs.getTimestamp("GoodUntil")));
        contact.setAveMonthlyGift(TntDb.floatToMoney(rs.getFloat("AveMonthlyGift")));
        contact.setLastDateInAve(TntDb.timestampToDate(rs.getTimestamp("LastDateInAve")));
        contact.setTwelveMonthTotal(TntDb.floatToMoney(rs.getFloat("TwelveMonthTotal")));
        contact.setBaseCurrencyId(rs.getInt("BaseCurrencyID"));
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
        contact.setCampaignsSinceLastGift(rs.getInt("CampaignsSinceLastGift"));
        contact.setChallengesSinceLastGift(rs.getInt("ChallengesSinceLastGift"));
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
        String query = String.format(
            "SELECT [ChallengesSinceLastGift] FROM [Contact] WHERE [ContactId] = %s",
            contactId);
        try {
            return TntDb.getOneInt(query);
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
        return TntDb.getOneInt(getContactIdByEmailQuery(email));
    }

    /**
     * Return the query needed to search for contact IDs by email.
     *
     * @param email
     *            the email for which to find associated contacts
     * @return the query needed to search for contact IDs by email
     */
    private static String getContactIdByEmailQuery(String email) {
        log.trace("getContactIdByEmailQuery({})", email);

        if (email == null)
            return null;

        // Add escape clause if any underscores exist; otherwise ignore
        String emailFormatted = email.replace("_", "\\_");
        String escapeStr = "";
        if (!emailFormatted.equals(email))
            escapeStr = "ESCAPE '\\'";

        String query = String.format(
            "SELECT [ContactID] FROM [Contact] WHERE "
                + "[Email1] LIKE '%1$s' %2$s OR [Email1] LIKE '*<%1$s>' %2$s OR "
                + "[Email2] LIKE '%1$s' %2$s OR [Email2] LIKE '*<%1$s>' %2$s OR "
                + "[Email3] LIKE '%1$s' %2$s OR [Email3] LIKE '*<%1$s>' %2$s",
            emailFormatted,
            escapeStr);

        // Search spouse fields as well
        query += String.format(
            " OR [SpouseEmail1] LIKE '%1$s' %2$s OR [SpouseEmail1] LIKE '*<%1$s>' %2$s OR "
                + "[SpouseEmail2] LIKE '%1$s' %2$s OR [SpouseEmail2] LIKE '*<%1$s>' %2$s OR "
                + "[SpouseEmail3] LIKE '%1$s' %2$s OR [SpouseEmail3] LIKE '*<%1$s>' %2$s",
            emailFormatted,
            escapeStr);

        return query;
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
        ResultSet rs = TntDb.runQuery("SELECT [ContactID], [FileAs] FROM [Contact] ORDER BY [FileAs]", false);
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
        ResultSet rs = TntDb.runQuery(getContactIdByEmailQuery(email));
        return TntDb.getRowCount(rs);
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
        String query = String.format("SELECT [FileAs] FROM [Contact] WHERE [ContactId] = %s", contactId);
        return TntDb.getOneString(query);
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
        String query = String.format("SELECT [Last%s] FROM [Contact] WHERE [ContactId] = %s", lastType, contactId);
        try {
            return TntDb.getOneDate(query);
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
                + "WHERE [HistoryContact].[ContactID] = %s AND "
                + "[History].[HistoryResultID] <> %s AND "
                + "[History].[IsChallenge] = -1 AND "
                + "[History].[HistoryDate] >= %s",
            contactId,
            History.RESULT_ATTEMPTED,
            TntDb.formatDbDateNoTime(lastGiftDate));
        Integer challengesSinceLastGift = 0;
        try {
            challengesSinceLastGift = TntDb.getOneInt(countQuery);
        } catch (TntDbException e) {
            log.error(e); // Nothing useful can be done in this case
        }

        String query = String.format(
            "UPDATE [Contact] SET [ChallengesSinceLastGift] = %s WHERE [ContactId] = %s",
            challengesSinceLastGift,
            contactId);
        TntDb.runQuery(query);
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
                + "WHERE [HistoryContact].[ContactID] = %s AND "
                + "[History].[HistoryResultID] <> %s AND "
                + "[History].[IsChallenge] = -1",
            contactId,
            History.RESULT_ATTEMPTED);
        LocalDateTime maxDate = null;
        try {
            maxDate = TntDb.getOneDate(query);
        } catch (TntDbException e) {
            log.error(e); // Nothing useful can be done in this case
        }
        if (maxDate != null)
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
                + "WHERE [HistoryContact].[ContactID] = %s AND "
                + "[History].[HistoryResultID] <> %s AND "
                + "[History].[IsMassMailing] = 0 AND "
                + "[History].[IsThank] = -1",
            contactId,
            History.RESULT_ATTEMPTED);
        LocalDateTime maxDate = null;
        try {
            maxDate = TntDb.getOneDate(query);
        } catch (TntDbException e) {
            log.error(e); // Nothing useful can be done in this case
        }
        if (maxDate != null)
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
                + "WHERE [HistoryContact].[ContactID] = %s AND "
                + "[History].[HistoryResultID] <> %s AND "
                + "[History].[IsMassMailing] = 0 AND "
                + "[History].[TaskTypeID] = %s",
            contactId,
            History.RESULT_ATTEMPTED,
            taskTypeId);
        LocalDateTime maxDate = null;
        try {
            maxDate = TntDb.getOneDate(query);
        } catch (TntDbException e) {
            log.error(e); // Nothing useful can be done in this case
        }
        if (maxDate != null)
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
        ResultSet rs = TntDb.runQuery(taskTypeQuery);
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
                + "WHERE [HistoryContact].[ContactID] = %s AND "
                + "[History].[HistoryResultID] <> %s AND "
                + "[History].[IsMassMailing] = 0 AND "
                + taskTypeStr,
            contactId,
            History.RESULT_ATTEMPTED);
        LocalDateTime maxDate = null;
        try {
            maxDate = TntDb.getOneDate(query);
        } catch (TntDbException e) {
            log.error(e); // Nothing useful can be done in this case
        }
        if (maxDate != null)
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
     *            the date to set; null forces a recalculation of last activity date
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
     *            the date to set; null forces a recalculation of last activity date
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
     *            the date to set; null forces a recalculation of last appointment date and last activity date
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
     *            the date to set; null forces a recalculation of last call date and last activity date
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
     *            the date to set; null forces a recalculation of last challenge date and last activity date
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
     *            the date to set; null forces a recalculation of last letter date and last activity date
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
     *            the date to set; null forces a recalculation of last pre-call date and last activity date
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
     *            the date to set; null forces a recalculation of last thank date and last activity date
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
     *            the date to set; null forces a recalculation of last visit date and last activity date
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
     *            the date to set; null forces a recalculation of last X date and last activity date
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
     *            the date to set; null forces a recalculation of last X date and last activity date
     * @param lastType
     *            the type of last date to set
     * @param force
     *            true forces this update even if {@code date} is older than the date in the Tnt database; false
     *            will not override a newer date in the Tnt database
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
        if (date == null || lastDate == null || force || lastDate.isBefore(date)) {
            String query = String.format(
                "UPDATE [Contact] SET [Last%s] = %s WHERE [ContactId] = %s",
                lastType,
                TntDb.formatDbDateNoTime(date),
                contactId);
            TntDb.runQuery(query);
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
