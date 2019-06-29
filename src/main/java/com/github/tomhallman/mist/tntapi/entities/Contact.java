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

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javamoney.moneta.FastMoney;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.tntapi.CurrencyManager;
import com.github.tomhallman.mist.tntapi.PledgeFrequencyManager;
import com.github.tomhallman.mist.util.Util;

/**
 * Object representing a row in the Tnt Contact table.
 */
public class Contact {
    private static Logger log = LogManager.getLogger();

    private Integer contactId = null;
    private LocalDateTime lastEdit = null;
    private LocalDateTime createdDate = null;
    private String rejectedDuplicateContactIDs = "";
    private String fileAs = "";
    private boolean fileAsIsCustom = false;
    private String fullName = "";
    private boolean fullNameIsCustom = false;
    private String greeting = "";
    private boolean greetingIsCustom = false;
    private String salutation = "";
    private boolean salutationIsCustom = false;
    private String shortName = "";
    private boolean shortNameIsCustom = false;
    private String mailingAddressBlock = "";
    private boolean mailingAddressIsDeliverable = false;
    private String phone = "";
    private boolean phoneIsValid = false;
    private String email = "";
    private boolean emailIsValid = false;
    private boolean isOrganization = false;
    private String organizationName = "";
    private String orgContactPerson = "";
    private String title = "";
    private String firstName = "";
    private String middleName = "";
    private String lastName = "";
    private String suffix = "";
    private String spouseTitle = "";
    private String spouseFirstName = "";
    private String spouseMiddleName = "";
    private String spouseLastName = "";
    private boolean deceased = false;
    private int mailingAddressType = 1;
    private String mailingStreetAddress = "";
    private String mailingCity = "";
    private String mailingState = "";
    private String mailingPostalCode = "";
    private String mailingCountry = "";
    private String homeStreetAddress = "";
    private String homeCity = "";
    private String homeState = "";
    private String homePostalCode = "";
    private int homeCountryId = 840;
    private String homeCountry = "United States of America";
    private boolean homeAddressIsDeliverable = false;
    private String homeAddressBlock = "";
    private boolean homeAddressBlockIsCustom = false;
    private String otherStreetAddress = "";
    private String otherCity = "";
    private String otherState = "";
    private String otherPostalCode = "";
    private int otherCountryId = 840;
    private String otherCountry = "United States of America";
    private boolean otherAddressIsDeliverable = false;
    private String otherAddressBlock = "";
    private boolean otherAddressBlockIsCustom = false;
    private String businessName = "";
    private String businessStreetAddress = "";
    private String businessCity = "";
    private String businessState = "";
    private String businessPostalCode = "";
    private int businessCountryId = 840;
    private String businessCountry = "United States of America";
    private boolean businessAddressIsDeliverable = false;
    private String businessAddressBlock = "";
    private boolean businessAddressBlockIsCustom = false;
    private String spouseBusinessName = "";
    private String spouseBusinessStreetAddress = "";
    private String spouseBusinessCity = "";
    private String spouseBusinessState = "";
    private String spouseBusinessPostalCode = "";
    private int spouseBusinessCountryId = 840;
    private String spouseBusinessCountry = "United States of America";
    private boolean spouseBusinessAddressIsDeliverable = false;
    private String spouseBusinessAddressBlock = "";
    private boolean spouseBusinessAddressBlockIsCustom = false;
    private int preferredPhoneType = 0;
    private int phoneIsValidMask = 133693440; // Default in Access = 134217727
    private String phoneCountryIds = "";
    private String homePhone = "";
    private String homePhone2 = "";
    private String homeFax = "";
    private String otherPhone = "";
    private String otherFax = "";
    private String businessPhone = "";
    private String businessPhone2 = "";
    private String businessFax = "";
    private String companyMainPhone = "";
    private String mobilePhone = "";
    private String mobilePhone2 = "";
    private String pagerNumber = "";
    private String spouseBusinessPhone = "";
    private String spouseBusinessPhone2 = "";
    private String spouseBusinessFax = "";
    private String spouseCompanyMainPhone = "";
    private String spouseMobilePhone = "";
    private String spouseMobilePhone2 = "";
    private String spousePagerNumber = "";
    private int preferredEmailTypes = 2;
    private String emailLabels = "";
    private String email1 = "";
    private String email2 = "";
    private String email3 = "";
    private boolean email1IsValid = false;
    private boolean email2IsValid = false;
    private boolean email3IsValid = false;
    private String emailCustomGreeting = "";
    private String emailCustomSalutation = "";
    private String spouseEmail1 = "";
    private String spouseEmail2 = "";
    private String spouseEmail3 = "";
    private boolean spouseEmail1IsValid = false;
    private boolean spouseEmail2IsValid = false;
    private boolean spouseEmail3IsValid = false;
    private String spouseEmailCustomGreeting = "";
    private String spouseEmailCustomSalutation = "";
    private String webPage1 = "";
    private String webPage2 = "";
    private String voiceSkype = "";
    private String imAddress = "";
    private String socialWeb1 = "";
    private String socialWeb2 = "";
    private String socialWeb3 = "";
    private String socialWeb4 = "";
    private String spouseWebPage1 = "";
    private String spouseWebPage2 = "";
    private String spouseVoiceSkype = "";
    private String spouseImAddress = "";
    private String spouseSocialWeb1 = "";
    private String spouseSocialWeb2 = "";
    private String spouseSocialWeb3 = "";
    private String spouseSocialWeb4 = "";
    private String notesAsRtf = "";
    private String notes = "";
    private int familySideId = 0;
    private int familyLevelId = 0;
    private String children = "";
    private String interests = "";
    private String nickname = "";
    private String profession = "";
    private String spouseInterests = "";
    private String spouseNickname = "";
    private String spouseProfession = "";
    private Integer anniversaryMonth = null;
    private Integer anniversaryDay = null;
    private Integer anniversaryYear = null;
    private Integer birthdayMonth = null;
    private Integer birthdayDay = null;
    private Integer birthdayYear = null;
    private Integer spouseBirthdayMonth = null;
    private Integer spouseBirthdayDay = null;
    private Integer spouseBirthdayYear = null;
    private String categories = "";
    private String user1 = "";
    private String user2 = "";
    private String user3 = "";
    private String user4 = "";
    private String user5 = "";
    private String user6 = "";
    private String user7 = "";
    private String user8 = "";
    private String userStatus = "";
    private int mapAddressType = 0;
    private Integer mapLat = null;
    private Integer mapLng = null;
    private String mapStatus = "CRC=0";
    private FastMoney pledgeAmount = null;
    private int pledgeFrequencyId = 0;
    private boolean pledgeReceived = false;
    private LocalDateTime pledgeStartDate = null;
    private int pledgeCurrencyId = 0;
    private String referredBy = "";
    private String referredByList = "";
    private int mpdPhaseId = 0;
    private Integer fundRepId = null;
    private LocalDateTime nextAsk = null;
    private FastMoney nextAskAmount = null;
    private FastMoney estimatedAnnualCapacity = null;
    private boolean neverAsk = false;
    private String region = "";
    private int likelyToGiveId = 0;
    private String churchName = "";
    private boolean sendNewsletter = false;
    private String newsletterMediaPref = "+E";
    private int newsletterLangId = 0;
    private boolean directDeposit = false;
    private boolean magazine = false;
    private FastMoney monthlyPledge = FastMoney.of(0, "USD");
    private LocalDateTime firstGiftDate = null;
    private LocalDateTime lastGiftDate = null;
    private FastMoney lastGiftAmount = FastMoney.of(0, "USD");
    private int lastGiftCurrencyId = 0;
    private int lastGiftOrganizationId = 0;
    private String lastGiftOrgDonorCode = "";
    private String lastGiftPaymentMethod = "";
    private FastMoney prevYearTotal = FastMoney.of(0, "USD");
    private FastMoney yearTotal = FastMoney.of(0, "USD");
    private FastMoney lifetimeTotal = FastMoney.of(0, "USD");
    private int lifetimeNumberOfGifts = 0;
    private FastMoney largestGift = FastMoney.of(0, "USD");
    private LocalDateTime goodUntil = null;
    private FastMoney aveMonthlyGift = FastMoney.of(0, "USD");
    private LocalDateTime lastDateInAve = null;
    private FastMoney twelveMonthTotal = FastMoney.of(0, "USD");
    private int baseCurrencyId = CurrencyManager.getBaseCurrencyId();
    private FastMoney baseMonthlyPledge = FastMoney.of(0, "USD");
    private FastMoney baseLastGiftAmount = FastMoney.of(0, "USD");
    private FastMoney basePrevYearTotal = FastMoney.of(0, "USD");
    private FastMoney baseYearTotal = FastMoney.of(0, "USD");
    private FastMoney baseLifetimeTotal = FastMoney.of(0, "USD");
    private FastMoney baseLargestGift = FastMoney.of(0, "USD");
    private FastMoney baseAveMonthlyGift = FastMoney.of(0, "USD");
    private FastMoney baseTwelveMonthTotal = FastMoney.of(0, "USD");
    private LocalDateTime lastActivity = null;
    private LocalDateTime lastAppointment = null;
    private LocalDateTime lastCall = null;
    private LocalDateTime lastPreCall = null;
    private LocalDateTime lastLetter = null;
    private LocalDateTime lastVisit = null;
    private LocalDateTime lastThank = null;
    private LocalDateTime lastChallenge = null;
    private int campaignsSinceLastGift = 0;
    private int challengesSinceLastGift = 0;
    private String orgDonorCodes = "";

    public Contact() {
    }

    public Integer getAnniversaryDay() {
        return anniversaryDay;
    }

    public Integer getAnniversaryMonth() {
        return anniversaryMonth;
    }

    public Integer getAnniversaryYear() {
        return anniversaryYear;
    }

    public FastMoney getAveMonthlyGift() {
        return aveMonthlyGift;
    }

    public FastMoney getBaseAveMonthlyGift() {
        return baseAveMonthlyGift;
    }

    public int getBaseCurrencyId() {
        return baseCurrencyId;
    }

    public FastMoney getBaseLargestGift() {
        return baseLargestGift;
    }

    public FastMoney getBaseLastGiftAmount() {
        return baseLastGiftAmount;
    }

    public FastMoney getBaseLifetimeTotal() {
        return baseLifetimeTotal;
    }

    public FastMoney getBaseMonthlyPledge() {
        return baseMonthlyPledge;
    }

    public FastMoney getBasePrevYearTotal() {
        return basePrevYearTotal;
    }

    public FastMoney getBaseTwelveMonthTotal() {
        return baseTwelveMonthTotal;
    }

    public FastMoney getBaseYearTotal() {
        return baseYearTotal;
    }

    public Integer getBirthdayDay() {
        return birthdayDay;
    }

    public Integer getBirthdayMonth() {
        return birthdayMonth;
    }

    public Integer getBirthdayYear() {
        return birthdayYear;
    }

    public String getBusinessAddressBlock() {
        return businessAddressBlock;
    }

    public String getBusinessCity() {
        return businessCity;
    }

    public String getBusinessCountry() {
        return businessCountry;
    }

    public int getBusinessCountryId() {
        return businessCountryId;
    }

    public String getBusinessFax() {
        return businessFax;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getBusinessPhone() {
        return businessPhone;
    }

    public String getBusinessPhone2() {
        return businessPhone2;
    }

    public String getBusinessPostalCode() {
        return businessPostalCode;
    }

    public String getBusinessState() {
        return businessState;
    }

    public String getBusinessStreetAddress() {
        return businessStreetAddress;
    }

    public int getCampaignsSinceLastGift() {
        return campaignsSinceLastGift;
    }

    public String getCategories() {
        return categories;
    }

    public int getChallengesSinceLastGift() {
        return challengesSinceLastGift;
    }

    public String getChildren() {
        return children;
    }

    public String getChurchName() {
        return churchName;
    }

    public String getCompanyMainPhone() {
        return companyMainPhone;
    }

    public Integer getContactId() {
        return contactId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public String getEmail() {
        return email;
    }

    public String getEmail1() {
        return email1;
    }

    public String getEmail2() {
        return email2;
    }

    public String getEmail3() {
        return email3;
    }

    public String getEmailCustomGreeting() {
        return emailCustomGreeting;
    }

    public String getEmailCustomSalutation() {
        return emailCustomSalutation;
    }

    public String getEmailLabels() {
        return emailLabels;
    }

    /**
     * Returns a list of email addresses associated with this contact.
     * 
     * @param includeContactEmails
     *            if true, include emails associated with the primary contact
     * @param includeSpouseEmails
     *            if true, include emails associated with the contact's spouse
     * @return a list of email addresses associated with this contact
     */
    public String[] getEmails(boolean includeContactEmails, boolean includeSpouseEmails) {
        log.trace("getEmails({},{})", includeContactEmails, includeSpouseEmails);
        ArrayList<String> emails = new ArrayList<String>();

        // We need to do some regular expression matching in case a field contains multiple addresses
        Pattern pattern = Pattern.compile(MIST.REGEX_EMAILADDRESS);

        if (includeContactEmails) {
            Util.addMatchesToList(emails, pattern, email1);
            Util.addMatchesToList(emails, pattern, email2);
            Util.addMatchesToList(emails, pattern, email3);
        }

        if (includeSpouseEmails) {
            Util.addMatchesToList(emails, pattern, spouseEmail1);
            Util.addMatchesToList(emails, pattern, spouseEmail2);
            Util.addMatchesToList(emails, pattern, spouseEmail3);
        }

        return emails.toArray(new String[0]);
    }

    public FastMoney getEstimatedAnnualCapacity() {
        return estimatedAnnualCapacity;
    }

    public int getFamilyLevelID() {
        return familyLevelId;
    }

    public int getFamilySideID() {
        return familySideId;
    }

    public String getFileAs() {
        return fileAs;
    }

    public LocalDateTime getFirstGiftDate() {
        return firstGiftDate;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getFullName() {
        return fullName;
    }

    public Integer getFundRepId() {
        return fundRepId;
    }

    public LocalDateTime getGoodUntil() {
        return goodUntil;
    }

    public String getGreeting() {
        return greeting;
    }

    public String getHomeAddressBlock() {
        return homeAddressBlock;
    }

    public String getHomeCity() {
        return homeCity;
    }

    public String getHomeCountry() {
        return homeCountry;
    }

    public int getHomeCountryId() {
        return homeCountryId;
    }

    public String getHomeFax() {
        return homeFax;
    }

    public String getHomePhone() {
        return homePhone;
    }

    public String getHomePhone2() {
        return homePhone2;
    }

    public String getHomePostalCode() {
        return homePostalCode;
    }

    public String getHomeState() {
        return homeState;
    }

    public String getHomeStreetAddress() {
        return homeStreetAddress;
    }

    public String getImAddress() {
        return imAddress;
    }

    public String getInterests() {
        return interests;
    }

    public FastMoney getLargestGift() {
        return largestGift;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public LocalDateTime getLastAppointment() {
        return lastAppointment;
    }

    public LocalDateTime getLastCall() {
        return lastCall;
    }

    public LocalDateTime getLastChallenge() {
        return lastChallenge;
    }

    public LocalDateTime getLastDateInAve() {
        return lastDateInAve;
    }

    public LocalDateTime getLastEdit() {
        return lastEdit;
    }

    public FastMoney getLastGiftAmount() {
        return lastGiftAmount;
    }

    public int getLastGiftCurrencyId() {
        return lastGiftCurrencyId;
    }

    public LocalDateTime getLastGiftDate() {
        return lastGiftDate;
    }

    public int getLastGiftOrganizationId() {
        return lastGiftOrganizationId;
    }

    public String getLastGiftOrgDonorCode() {
        return lastGiftOrgDonorCode;
    }

    public String getLastGiftPaymentMethod() {
        return lastGiftPaymentMethod;
    }

    /**
     * Return a friendly string representation of the last gift date.
     * 
     * @return a friendly string representation of the last gift date
     */
    public String getLastGiftStr() {
        if (lastGiftDate == null)
            return "---";
        return lastGiftDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
    }

    public LocalDateTime getLastLetter() {
        return lastLetter;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDateTime getLastPreCall() {
        return lastPreCall;
    }

    public LocalDateTime getLastThank() {
        return lastThank;
    }

    public LocalDateTime getLastVisit() {
        return lastVisit;
    }

    public int getLifetimeNumberOfGifts() {
        return lifetimeNumberOfGifts;
    }

    public FastMoney getLifetimeTotal() {
        return lifetimeTotal;
    }

    public int getLikelyToGiveId() {
        return likelyToGiveId;
    }

    public String getMailingAddressBlock() {
        return mailingAddressBlock;
    }

    public int getMailingAddressType() {
        return mailingAddressType;
    }

    public String getMailingCity() {
        return mailingCity;
    }

    public String getMailingCountry() {
        return mailingCountry;
    }

    public String getMailingPostalCode() {
        return mailingPostalCode;
    }

    public String getMailingState() {
        return mailingState;
    }

    public String getMailingStreetAddress() {
        return mailingStreetAddress;
    }

    public int getMapAddressType() {
        return mapAddressType;
    }

    public Integer getMapLat() {
        return mapLat;
    }

    public Integer getMapLng() {
        return mapLng;
    }

    public String getMapStatus() {
        return mapStatus;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public String getMobilePhone2() {
        return mobilePhone2;
    }

    public FastMoney getMonthlyPledge() {
        return monthlyPledge;
    }

    public int getMpdPhaseId() {
        return mpdPhaseId;
    }

    public int getNewsletterLangId() {
        return newsletterLangId;
    }

    public String getNewsletterMediaPref() {
        return newsletterMediaPref;
    }

    public LocalDateTime getNextAsk() {
        return nextAsk;
    }

    public FastMoney getNextAskAmount() {
        return nextAskAmount;
    }

    public String getNickname() {
        return nickname;
    }

    public String getNotes() {
        return notes;
    }

    public String getNotesAsRtf() {
        return notesAsRtf;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getOrgContactPerson() {
        return orgContactPerson;
    }

    public String getOrgDonorCodes() {
        return orgDonorCodes;
    }

    public String getOtherAddressBlock() {
        return otherAddressBlock;
    }

    public String getOtherCity() {
        return otherCity;
    }

    public String getOtherCountry() {
        return otherCountry;
    }

    public int getOtherCountryId() {
        return otherCountryId;
    }

    public String getOtherFax() {
        return otherFax;
    }

    public String getOtherPhone() {
        return otherPhone;
    }

    public String getOtherPostalCode() {
        return otherPostalCode;
    }

    public String getOtherState() {
        return otherState;
    }

    public String getOtherStreetAddress() {
        return otherStreetAddress;
    }

    public String getPagerNumber() {
        return pagerNumber;
    }

    public String getPhone() {
        return phone;
    }

    public String getPhoneCountryIds() {
        return phoneCountryIds;
    }

    public int getPhoneIsValidMask() {
        return phoneIsValidMask;
    }

    public FastMoney getPledgeAmount() {
        return pledgeAmount;
    }

    public int getPledgeCurrencyId() {
        return pledgeCurrencyId;
    }

    public int getPledgeFrequencyId() {
        return pledgeFrequencyId;
    }

    public LocalDateTime getPledgeStartDate() {
        return pledgeStartDate;
    }

    /**
     * Returns a string representation of the contact's pledge
     * 
     * @return a string representation of the contact's pledge
     */
    public String getPledgeStr() {
        if (pledgeFrequencyId == 0) // "<none>"
            return PledgeFrequencyManager.get(pledgeFrequencyId).getDescription();

        Currency currency = CurrencyManager.get(pledgeCurrencyId);
        return String.format(
            "%s%s %s",
            currency.getSymbol(),
            new DecimalFormat("#." + StringUtils.repeat("0", currency.getDecimalPlaces())).format(
                pledgeAmount.getNumber()),
            PledgeFrequencyManager.get(pledgeFrequencyId).getDescription());
    }

    public int getPreferredEmailTypes() {
        return preferredEmailTypes;
    }

    public int getPreferredPhoneType() {
        return preferredPhoneType;
    }

    public FastMoney getPrevYearTotal() {
        return prevYearTotal;
    }

    public String getProfession() {
        return profession;
    }

    public String getReferredBy() {
        return referredBy;
    }

    public String getReferredByList() {
        return referredByList;
    }

    public String getRegion() {
        return region;
    }

    public String getRejectedDuplicateContactIDs() {
        return rejectedDuplicateContactIDs;
    }

    public String getSalutation() {
        return salutation;
    }

    public String getShortName() {
        return shortName;
    }

    public String getSocialWeb1() {
        return socialWeb1;
    }

    public String getSocialWeb2() {
        return socialWeb2;
    }

    public String getSocialWeb3() {
        return socialWeb3;
    }

    public String getSocialWeb4() {
        return socialWeb4;
    }

    public Integer getSpouseBirthdayDay() {
        return spouseBirthdayDay;
    }

    public Integer getSpouseBirthdayMonth() {
        return spouseBirthdayMonth;
    }

    public Integer getSpouseBirthdayYear() {
        return spouseBirthdayYear;
    }

    public String getSpouseBusinessAddressBlock() {
        return spouseBusinessAddressBlock;
    }

    public String getSpouseBusinessCity() {
        return spouseBusinessCity;
    }

    public String getSpouseBusinessCountry() {
        return spouseBusinessCountry;
    }

    public int getSpouseBusinessCountryId() {
        return spouseBusinessCountryId;
    }

    public String getSpouseBusinessFax() {
        return spouseBusinessFax;
    }

    public String getSpouseBusinessName() {
        return spouseBusinessName;
    }

    public String getSpouseBusinessPhone() {
        return spouseBusinessPhone;
    }

    public String getSpouseBusinessPhone2() {
        return spouseBusinessPhone2;
    }

    public String getSpouseBusinessPostalCode() {
        return spouseBusinessPostalCode;
    }

    public String getSpouseBusinessState() {
        return spouseBusinessState;
    }

    public String getSpouseBusinessStreetAddress() {
        return spouseBusinessStreetAddress;
    }

    public String getSpouseCompanyMainPhone() {
        return spouseCompanyMainPhone;
    }

    public String getSpouseEmail1() {
        return spouseEmail1;
    }

    public String getSpouseEmail2() {
        return spouseEmail2;
    }

    public String getSpouseEmail3() {
        return spouseEmail3;
    }

    public String getSpouseEmailCustomGreeting() {
        return spouseEmailCustomGreeting;
    }

    public String getSpouseEmailCustomSalutation() {
        return spouseEmailCustomSalutation;
    }

    public String getSpouseFirstName() {
        return spouseFirstName;
    }

    public String getSpouseImAddress() {
        return spouseImAddress;
    }

    public String getSpouseInterests() {
        return spouseInterests;
    }

    public String getSpouseLastName() {
        return spouseLastName;
    }

    public String getSpouseMiddleName() {
        return spouseMiddleName;
    }

    public String getSpouseMobilePhone() {
        return spouseMobilePhone;
    }

    public String getSpouseMobilePhone2() {
        return spouseMobilePhone2;
    }

    public String getSpouseNickname() {
        return spouseNickname;
    }

    public String getSpousePagerNumber() {
        return spousePagerNumber;
    }

    public String getSpouseProfession() {
        return spouseProfession;
    }

    public String getSpouseSocialWeb1() {
        return spouseSocialWeb1;
    }

    public String getSpouseSocialWeb2() {
        return spouseSocialWeb2;
    }

    public String getSpouseSocialWeb3() {
        return spouseSocialWeb3;
    }

    public String getSpouseSocialWeb4() {
        return spouseSocialWeb4;
    }

    public String getSpouseTitle() {
        return spouseTitle;
    }

    public String getSpouseVoiceSkype() {
        return spouseVoiceSkype;
    }

    public String getSpouseWebPage1() {
        return spouseWebPage1;
    }

    public String getSpouseWebPage2() {
        return spouseWebPage2;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getTitle() {
        return title;
    }

    public FastMoney getTwelveMonthTotal() {
        return twelveMonthTotal;
    }

    public String getUser1() {
        return user1;
    }

    public String getUser2() {
        return user2;
    }

    public String getUser3() {
        return user3;
    }

    public String getUser4() {
        return user4;
    }

    public String getUser5() {
        return user5;
    }

    public String getUser6() {
        return user6;
    }

    public String getUser7() {
        return user7;
    }

    public String getUser8() {
        return user8;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public String getVoiceSkype() {
        return voiceSkype;
    }

    public String getWebPage1() {
        return webPage1;
    }

    public String getWebPage2() {
        return webPage2;
    }

    public FastMoney getYearTotal() {
        return yearTotal;
    }

    /**
     * Return whether this contact has an associated spouse.
     * This is currently determined based on whether the spouse has a non-blank first name.
     * 
     * @return whether this contact has an associated spouse
     */
    public boolean hasSpouse() {
        log.trace("hasSpouse()");
        return !spouseFirstName.isBlank();
    }

    public boolean isBusinessAddressBlockCustom() {
        return businessAddressBlockIsCustom;
    }

    public boolean isBusinessAddressDeliverable() {
        return businessAddressIsDeliverable;
    }

    public boolean isDeceased() {
        return deceased;
    }

    public boolean isDirectDeposit() {
        return directDeposit;
    }

    public boolean isEmail1Valid() {
        return email1IsValid;
    }

    public boolean isEmail2Valid() {
        return email2IsValid;
    }

    public boolean isEmail3Valid() {
        return email3IsValid;
    }

    public boolean isEmailValid() {
        return emailIsValid;
    }

    public boolean isFileAsCustom() {
        return fileAsIsCustom;
    }

    public boolean isFullNameCustom() {
        return fullNameIsCustom;
    }

    public boolean isGreetingCustom() {
        return greetingIsCustom;
    }

    public boolean isHomeAddressBlockCustom() {
        return homeAddressBlockIsCustom;
    }

    public boolean isHomeAddressDeliverable() {
        return homeAddressIsDeliverable;
    }

    public boolean isMagazine() {
        return magazine;
    }

    public boolean isMailingAddressDeliverable() {
        return mailingAddressIsDeliverable;
    }

    public boolean isNeverAsk() {
        return neverAsk;
    }

    public boolean isOrganization() {
        return isOrganization;
    }

    public boolean isOtherAddressBlockCustom() {
        return otherAddressBlockIsCustom;
    }

    public boolean isOtherAddressDeliverable() {
        return otherAddressIsDeliverable;
    }

    public boolean isPhoneValid() {
        return phoneIsValid;
    }

    public boolean isPledgeReceived() {
        return pledgeReceived;
    }

    public boolean isSalutationCustom() {
        return salutationIsCustom;
    }

    public boolean isSendNewsletter() {
        return sendNewsletter;
    }

    public boolean isShortNameCustom() {
        return shortNameIsCustom;
    }

    public boolean isSpouseBusinessAddressBlockCustom() {
        return spouseBusinessAddressBlockIsCustom;
    }

    public boolean isSpouseBusinessAddressDeliverable() {
        return spouseBusinessAddressIsDeliverable;
    }

    public boolean isSpouseEmail1Valid() {
        return spouseEmail1IsValid;
    }

    public boolean isSpouseEmail2Valid() {
        return spouseEmail2IsValid;
    }

    public boolean isSpouseEmail3Valid() {
        return spouseEmail3IsValid;
    }

    public void setAnniversaryDay(int anniversaryDay) {
        this.anniversaryDay = anniversaryDay;
    }

    public void setAnniversaryMonth(int anniversaryMonth) {
        this.anniversaryMonth = anniversaryMonth;
    }

    public void setAnniversaryYear(int anniversaryYear) {
        this.anniversaryYear = anniversaryYear;
    }

    public void setAveMonthlyGift(FastMoney aveMonthlyGift) {
        this.aveMonthlyGift = aveMonthlyGift;
    }

    public void setBaseAveMonthlyGift(FastMoney baseAveMonthlyGift) {
        this.baseAveMonthlyGift = baseAveMonthlyGift;
    }

    public void setBaseCurrencyId(int baseCurrencyId) {
        this.baseCurrencyId = baseCurrencyId;
    }

    public void setBaseLargestGift(FastMoney baseLargestGift) {
        this.baseLargestGift = baseLargestGift;
    }

    public void setBaseLastGiftAmount(FastMoney baseLastGiftAmount) {
        this.baseLastGiftAmount = baseLastGiftAmount;
    }

    public void setBaseLifetimeTotal(FastMoney baseLifetimeTotal) {
        this.baseLifetimeTotal = baseLifetimeTotal;
    }

    public void setBaseMonthlyPledge(FastMoney baseMonthlyPledge) {
        this.baseMonthlyPledge = baseMonthlyPledge;
    }

    public void setBasePrevYearTotal(FastMoney basePrevYearTotal) {
        this.basePrevYearTotal = basePrevYearTotal;
    }

    public void setBaseTwelveMonthTotal(FastMoney baseTwelveMonthTotal) {
        this.baseTwelveMonthTotal = baseTwelveMonthTotal;
    }

    public void setBaseYearTotal(FastMoney baseYearTotal) {
        this.baseYearTotal = baseYearTotal;
    }

    public void setBirthdayDay(Integer birthdayDay) {
        this.birthdayDay = birthdayDay;
    }

    public void setBirthdayMonth(Integer birthdayMonth) {
        this.birthdayMonth = birthdayMonth;
    }

    public void setBirthdayYear(Integer birthdayYear) {
        this.birthdayYear = birthdayYear;
    }

    public void setBusinessAddressBlock(String businessAddressBlock) {
        this.businessAddressBlock = businessAddressBlock;
    }

    public void setBusinessAddressBlockIsCustom(boolean businessAddressBlockIsCustom) {
        this.businessAddressBlockIsCustom = businessAddressBlockIsCustom;
    }

    public void setBusinessAddressIsDeliverable(boolean businessAddressIsDeliverable) {
        this.businessAddressIsDeliverable = businessAddressIsDeliverable;
    }

    public void setBusinessCity(String businessCity) {
        this.businessCity = businessCity;
    }

    public void setBusinessCountry(String businessCountry) {
        this.businessCountry = businessCountry;
    }

    public void setBusinessCountryId(int businessCountryId) {
        this.businessCountryId = businessCountryId;
    }

    public void setBusinessFax(String businessFax) {
        this.businessFax = businessFax;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public void setBusinessPhone(String businessPhone) {
        this.businessPhone = businessPhone;
    }

    public void setBusinessPhone2(String businessPhone2) {
        this.businessPhone2 = businessPhone2;
    }

    public void setBusinessPostalCode(String businessPostalCode) {
        this.businessPostalCode = businessPostalCode;
    }

    public void setBusinessState(String businessState) {
        this.businessState = businessState;
    }

    public void setBusinessStreetAddress(String businessStreetAddress) {
        this.businessStreetAddress = businessStreetAddress;
    }

    public void setCampaignsSinceLastGift(int campaignsSinceLastGift) {
        this.campaignsSinceLastGift = campaignsSinceLastGift;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public void setChallengesSinceLastGift(int challengesSinceLastGift) {
        this.challengesSinceLastGift = challengesSinceLastGift;
    }

    public void setChildren(String children) {
        this.children = children;
    }

    public void setChurchName(String churchName) {
        this.churchName = churchName;
    }

    public void setCompanyMainPhone(String companyMainPhone) {
        this.companyMainPhone = companyMainPhone;
    }

    public void setContactId(Integer contactId) {
        this.contactId = contactId;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setDeceased(boolean deceased) {
        this.deceased = deceased;
    }

    public void setDirectDeposit(boolean directDeposit) {
        this.directDeposit = directDeposit;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setEmail1(String email1) {
        this.email1 = email1;
    }

    public void setEmail1IsValid(boolean email1IsValid) {
        this.email1IsValid = email1IsValid;
    }

    public void setEmail2(String email2) {
        this.email2 = email2;
    }

    public void setEmail2IsValid(boolean email2IsValid) {
        this.email2IsValid = email2IsValid;
    }

    public void setEmail3(String email3) {
        this.email3 = email3;
    }

    public void setEmail3IsValid(boolean email3IsValid) {
        this.email3IsValid = email3IsValid;
    }

    public void setEmailCustomGreeting(String emailCustomGreeting) {
        this.emailCustomGreeting = emailCustomGreeting;
    }

    public void setEmailCustomSalutation(String emailCustomSalutation) {
        this.emailCustomSalutation = emailCustomSalutation;
    }

    public void setEmailIsValid(boolean emailIsValid) {
        this.emailIsValid = emailIsValid;
    }

    public void setEmailLabels(String emailLabels) {
        this.emailLabels = emailLabels;
    }

    public void setEstimatedAnnualCapacity(FastMoney estimatedAnnualCapacity) {
        this.estimatedAnnualCapacity = estimatedAnnualCapacity;
    }

    public void setFamilyLevelID(int familyLevelID) {
        this.familyLevelId = familyLevelID;
    }

    public void setFamilySideID(int familySideID) {
        this.familySideId = familySideID;
    }

    public void setFileAs(String fileAs) {
        this.fileAs = fileAs;
    }

    public void setFileAsIsCustom(boolean fileAsIsCustom) {
        this.fileAsIsCustom = fileAsIsCustom;
    }

    public void setFirstGiftDate(LocalDateTime firstGiftDate) {
        this.firstGiftDate = firstGiftDate;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setFullNameIsCustom(boolean fullNameIsCustom) {
        this.fullNameIsCustom = fullNameIsCustom;
    }

    public void setFundRepId(Integer fundRepId) {
        this.fundRepId = fundRepId;
    }

    public void setGoodUntil(LocalDateTime goodUntil) {
        this.goodUntil = goodUntil;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public void setGreetingIsCustom(boolean greetingIsCustom) {
        this.greetingIsCustom = greetingIsCustom;
    }

    public void setHomeAddressBlock(String homeAddressBlock) {
        this.homeAddressBlock = homeAddressBlock;
    }

    public void setHomeAddressBlockIsCustom(boolean homeAddressBlockIsCustom) {
        this.homeAddressBlockIsCustom = homeAddressBlockIsCustom;
    }

    public void setHomeAddressIsDeliverable(boolean homeAddressIsDeliverable) {
        this.homeAddressIsDeliverable = homeAddressIsDeliverable;
    }

    public void setHomeCity(String homeCity) {
        this.homeCity = homeCity;
    }

    public void setHomeCountry(String homeCountry) {
        this.homeCountry = homeCountry;
    }

    public void setHomeCountryID(int homeCountryID) {
        this.homeCountryId = homeCountryID;
    }

    public void setHomeFax(String homeFax) {
        this.homeFax = homeFax;
    }

    public void setHomePhone(String homePhone) {
        this.homePhone = homePhone;
    }

    public void setHomePhone2(String homePhone2) {
        this.homePhone2 = homePhone2;
    }

    public void setHomePostalCode(String homePostalCode) {
        this.homePostalCode = homePostalCode;
    }

    public void setHomeState(String homeState) {
        this.homeState = homeState;
    }

    public void setHomeStreetAddress(String homeStreetAddress) {
        this.homeStreetAddress = homeStreetAddress;
    }

    public void setImAddress(String imAddress) {
        this.imAddress = imAddress;
    }

    /**
     * Sets initial "email fields" for this contact using the specified email address.
     * <ul>
     * <li>Email</li>
     * <li>EmailIsValid: true</li>
     * <li>Email1</li>
     * <li>Email1IsValid: true</li>
     * </ul>
     * 
     * @param email
     *            the contact's email address
     */
    public void setInitialEmailFields(String email) {
        this.email = email;
        email1 = email;
        emailIsValid = true;
        email1IsValid = true;
    }

    /**
     * Sets initial "name fields" for this contact using the specified first and last names as follows:
     * <ul>
     * <li>FileAs: Last, First</li>
     * <li>FirstName: First</li>
     * <li>FullName: First Last</li>
     * <li>Greeting: First</li>
     * <li>LastName: Last</li>
     * <li>Salutation: Dear First</li>
     * <li>ShortName: First Last</li>
     * </ul>
     * 
     * @param firstName
     *            the contact's first name
     * @param lastName
     *            the contact's last name
     */
    public void setInitialNameFields(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        greeting = firstName;
        fileAs = String.format("%s, %s", lastName, firstName);
        fullName = String.format("%s %s", firstName, lastName);
        shortName = fullName;
        salutation = String.format("Dear %s", firstName);
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public void setLargestGift(FastMoney largestGift) {
        this.largestGift = largestGift;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public void setLastAppointment(LocalDateTime lastAppointment) {
        this.lastAppointment = lastAppointment;
    }

    public void setLastCall(LocalDateTime lastCall) {
        this.lastCall = lastCall;
    }

    public void setLastChallenge(LocalDateTime lastChallenge) {
        this.lastChallenge = lastChallenge;
    }

    public void setLastDateInAve(LocalDateTime lastDateInAve) {
        this.lastDateInAve = lastDateInAve;
    }

    public void setLastEdit(LocalDateTime lastEdit) {
        this.lastEdit = lastEdit;
    }

    public void setLastGiftAmount(FastMoney lastGiftAmount) {
        this.lastGiftAmount = lastGiftAmount;
    }

    public void setLastGiftCurrencyId(int lastGiftCurrencyId) {
        this.lastGiftCurrencyId = lastGiftCurrencyId;
    }

    public void setLastGiftDate(LocalDateTime lastGiftDate) {
        this.lastGiftDate = lastGiftDate;
    }

    public void setLastGiftOrganizationId(int lastGiftOrganizationId) {
        this.lastGiftOrganizationId = lastGiftOrganizationId;
    }

    public void setLastGiftOrgDonorCode(String lastGiftOrgDonorCode) {
        this.lastGiftOrgDonorCode = lastGiftOrgDonorCode;
    }

    public void setLastGiftPaymentMethod(String lastGiftPaymentMethod) {
        this.lastGiftPaymentMethod = lastGiftPaymentMethod;
    }

    public void setLastLetter(LocalDateTime lastLetter) {
        this.lastLetter = lastLetter;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setLastPreCall(LocalDateTime lastPreCall) {
        this.lastPreCall = lastPreCall;
    }

    public void setLastThank(LocalDateTime lastThank) {
        this.lastThank = lastThank;
    }

    public void setLastVisit(LocalDateTime lastVisit) {
        this.lastVisit = lastVisit;
    }

    public void setLifetimeNumberOfGifts(int lifetimeNumberOfGifts) {
        this.lifetimeNumberOfGifts = lifetimeNumberOfGifts;
    }

    public void setLifetimeTotal(FastMoney lifetimeTotal) {
        this.lifetimeTotal = lifetimeTotal;
    }

    public void setLikelyToGiveId(int likelyToGiveId) {
        this.likelyToGiveId = likelyToGiveId;
    }

    public void setMagazine(boolean magazine) {
        this.magazine = magazine;
    }

    public void setMailingAddressBlock(String mailingAddressBlock) {
        this.mailingAddressBlock = mailingAddressBlock;
    }

    public void setMailingAddressIsDeliverable(boolean mailingAddressIsDeliverable) {
        this.mailingAddressIsDeliverable = mailingAddressIsDeliverable;
    }

    public void setMailingAddressType(int mailingAddressType) {
        this.mailingAddressType = mailingAddressType;
    }

    public void setMailingCity(String mailingCity) {
        this.mailingCity = mailingCity;
    }

    public void setMailingCountry(String mailingCountry) {
        this.mailingCountry = mailingCountry;
    }

    public void setMailingPostalCode(String mailingPostalCode) {
        this.mailingPostalCode = mailingPostalCode;
    }

    public void setMailingState(String mailingState) {
        this.mailingState = mailingState;
    }

    public void setMailingStreetAddress(String mailingStreetAddress) {
        this.mailingStreetAddress = mailingStreetAddress;
    }

    public void setMapAddressType(int mapAddressType) {
        this.mapAddressType = mapAddressType;
    }

    public void setMapLat(Integer mapLat) {
        this.mapLat = mapLat;
    }

    public void setMapLng(Integer mapLng) {
        this.mapLng = mapLng;
    }

    public void setMapStatus(String mapStatus) {
        this.mapStatus = mapStatus;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public void setMobilePhone2(String mobilePhone2) {
        this.mobilePhone2 = mobilePhone2;
    }

    public void setMonthlyPledge(FastMoney monthlyPledge) {
        this.monthlyPledge = monthlyPledge;
    }

    public void setMpdPhaseId(int mpdPhaseId) {
        this.mpdPhaseId = mpdPhaseId;
    }

    public void setNeverAsk(boolean neverAsk) {
        this.neverAsk = neverAsk;
    }

    public void setNewsletterLangId(int newsletterLangId) {
        this.newsletterLangId = newsletterLangId;
    }

    public void setNewsletterMediaPref(String newsletterMediaPref) {
        this.newsletterMediaPref = newsletterMediaPref;
    }

    public void setNextAsk(LocalDateTime nextAsk) {
        this.nextAsk = nextAsk;
    }

    public void setNextAskAmount(FastMoney nextAskAmount) {
        this.nextAskAmount = nextAskAmount;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setNotesAsRtf(String notesAsRtf) {
        this.notesAsRtf = notesAsRtf;
    }

    public void setOrganization(boolean isOrganization) {
        this.isOrganization = isOrganization;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public void setOrgContactPerson(String orgContactPerson) {
        this.orgContactPerson = orgContactPerson;
    }

    public void setOrgDonorCodes(String orgDonorCodes) {
        this.orgDonorCodes = orgDonorCodes;
    }

    public void setOtherAddressBlock(String otherAddressBlock) {
        this.otherAddressBlock = otherAddressBlock;
    }

    public void setOtherAddressBlockIsCustom(boolean otherAddressBlockIsCustom) {
        this.otherAddressBlockIsCustom = otherAddressBlockIsCustom;
    }

    public void setOtherAddressIsDeliverable(boolean otherAddressIsDeliverable) {
        this.otherAddressIsDeliverable = otherAddressIsDeliverable;
    }

    public void setOtherCity(String otherCity) {
        this.otherCity = otherCity;
    }

    public void setOtherCountry(String otherCountry) {
        this.otherCountry = otherCountry;
    }

    public void setOtherCountryID(int otherCountryID) {
        this.otherCountryId = otherCountryID;
    }

    public void setOtherFax(String otherFax) {
        this.otherFax = otherFax;
    }

    public void setOtherPhone(String otherPhone) {
        this.otherPhone = otherPhone;
    }

    public void setOtherPostalCode(String otherPostalCode) {
        this.otherPostalCode = otherPostalCode;
    }

    public void setOtherState(String otherState) {
        this.otherState = otherState;
    }

    public void setOtherStreetAddress(String otherStreetAddress) {
        this.otherStreetAddress = otherStreetAddress;
    }

    public void setPagerNumber(String pagerNumber) {
        this.pagerNumber = pagerNumber;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPhoneCountryIds(String phoneCountryIds) {
        this.phoneCountryIds = phoneCountryIds;
    }

    public void setPhoneIsValid(boolean phoneIsValid) {
        this.phoneIsValid = phoneIsValid;
    }

    public void setPhoneIsValidMask(int phoneIsValidMask) {
        this.phoneIsValidMask = phoneIsValidMask;
    }

    public void setPledgeAmount(FastMoney pledgeAmount) {
        this.pledgeAmount = pledgeAmount;
    }

    public void setPledgeCurrencyId(int pledgeCurrencyId) {
        this.pledgeCurrencyId = pledgeCurrencyId;
    }

    public void setPledgeFrequencyId(int pledgeFrequencyId) {
        this.pledgeFrequencyId = pledgeFrequencyId;
    }

    public void setPledgeReceived(boolean pledgeReceived) {
        this.pledgeReceived = pledgeReceived;
    }

    public void setPledgeStartDate(LocalDateTime pledgeStartDate) {
        this.pledgeStartDate = pledgeStartDate;
    }

    public void setPreferredEmailTypes(int preferredEmailTypes) {
        this.preferredEmailTypes = preferredEmailTypes;
    }

    public void setPreferredPhoneType(int preferredPhoneType) {
        this.preferredPhoneType = preferredPhoneType;
    }

    public void setPrevYearTotal(FastMoney prevYearTotal) {
        this.prevYearTotal = prevYearTotal;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public void setReferredBy(String referredBy) {
        this.referredBy = referredBy;
    }

    public void setReferredByList(String referredByList) {
        this.referredByList = referredByList;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setRejectedDuplicateContactIDs(String rejectedDuplicateContactIDs) {
        this.rejectedDuplicateContactIDs = rejectedDuplicateContactIDs;
    }

    public void setSalutation(String salutation) {
        this.salutation = salutation;
    }

    public void setSalutationIsCustom(boolean salutationIsCustom) {
        this.salutationIsCustom = salutationIsCustom;
    }

    public void setSendNewsletter(boolean sendNewsletter) {
        this.sendNewsletter = sendNewsletter;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public void setShortNameIsCustom(boolean shortNameIsCustom) {
        this.shortNameIsCustom = shortNameIsCustom;
    }

    public void setSocialWeb1(String socialWeb1) {
        this.socialWeb1 = socialWeb1;
    }

    public void setSocialWeb2(String socialWeb2) {
        this.socialWeb2 = socialWeb2;
    }

    public void setSocialWeb3(String socialWeb3) {
        this.socialWeb3 = socialWeb3;
    }

    public void setSocialWeb4(String socialWeb4) {
        this.socialWeb4 = socialWeb4;
    }

    public void setSpouseBirthdayDay(Integer spouseBirthdayDay) {
        this.spouseBirthdayDay = spouseBirthdayDay;
    }

    public void setSpouseBirthdayMonth(Integer spouseBirthdayMonth) {
        this.spouseBirthdayMonth = spouseBirthdayMonth;
    }

    public void setSpouseBirthdayYear(Integer spouseBirthdayYear) {
        this.spouseBirthdayYear = spouseBirthdayYear;
    }

    public void setSpouseBusinessAddressBlock(String spouseBusinessAddressBlock) {
        this.spouseBusinessAddressBlock = spouseBusinessAddressBlock;
    }

    public void setSpouseBusinessAddressBlockIsCustom(boolean spouseBusinessAddressBlockIsCustom) {
        this.spouseBusinessAddressBlockIsCustom = spouseBusinessAddressBlockIsCustom;
    }

    public void setSpouseBusinessAddressIsDeliverable(boolean spouseBusinessAddressIsDeliverable) {
        this.spouseBusinessAddressIsDeliverable = spouseBusinessAddressIsDeliverable;
    }

    public void setSpouseBusinessCity(String spouseBusinessCity) {
        this.spouseBusinessCity = spouseBusinessCity;
    }

    public void setSpouseBusinessCountry(String spouseBusinessCountry) {
        this.spouseBusinessCountry = spouseBusinessCountry;
    }

    public void setSpouseBusinessCountryId(int spouseBusinessCountryId) {
        this.spouseBusinessCountryId = spouseBusinessCountryId;
    }

    public void setSpouseBusinessFax(String spouseBusinessFax) {
        this.spouseBusinessFax = spouseBusinessFax;
    }

    public void setSpouseBusinessName(String spouseBusinessName) {
        this.spouseBusinessName = spouseBusinessName;
    }

    public void setSpouseBusinessPhone(String spouseBusinessPhone) {
        this.spouseBusinessPhone = spouseBusinessPhone;
    }

    public void setSpouseBusinessPhone2(String spouseBusinessPhone2) {
        this.spouseBusinessPhone2 = spouseBusinessPhone2;
    }

    public void setSpouseBusinessPostalCode(String spouseBusinessPostalCode) {
        this.spouseBusinessPostalCode = spouseBusinessPostalCode;
    }

    public void setSpouseBusinessState(String spouseBusinessState) {
        this.spouseBusinessState = spouseBusinessState;
    }

    public void setSpouseBusinessStreetAddress(String spouseBusinessStreetAddress) {
        this.spouseBusinessStreetAddress = spouseBusinessStreetAddress;
    }

    public void setSpouseCompanyMainPhone(String spouseCompanyMainPhone) {
        this.spouseCompanyMainPhone = spouseCompanyMainPhone;
    }

    public void setSpouseEmail1(String spouseEmail1) {
        this.spouseEmail1 = spouseEmail1;
    }

    public void setSpouseEmail1IsValid(boolean spouseEmail1IsValid) {
        this.spouseEmail1IsValid = spouseEmail1IsValid;
    }

    public void setSpouseEmail2(String spouseEmail2) {
        this.spouseEmail2 = spouseEmail2;
    }

    public void setSpouseEmail2IsValid(boolean spouseEmail2IsValid) {
        this.spouseEmail2IsValid = spouseEmail2IsValid;
    }

    public void setSpouseEmail3(String spouseEmail3) {
        this.spouseEmail3 = spouseEmail3;
    }

    public void setSpouseEmail3IsValid(boolean spouseEmail3IsValid) {
        this.spouseEmail3IsValid = spouseEmail3IsValid;
    }

    public void setSpouseEmailCustomGreeting(String spouseEmailCustomGreeting) {
        this.spouseEmailCustomGreeting = spouseEmailCustomGreeting;
    }

    public void setSpouseEmailCustomSalutation(String spouseEmailCustomSalutation) {
        this.spouseEmailCustomSalutation = spouseEmailCustomSalutation;
    }

    public void setSpouseFirstName(String spouseFirstName) {
        this.spouseFirstName = spouseFirstName;
    }

    public void setSpouseImAddress(String spouseImAddress) {
        this.spouseImAddress = spouseImAddress;
    }

    public void setSpouseInterests(String spouseInterests) {
        this.spouseInterests = spouseInterests;
    }

    public void setSpouseLastName(String spouseLastName) {
        this.spouseLastName = spouseLastName;
    }

    public void setSpouseMiddleName(String spouseMiddleName) {
        this.spouseMiddleName = spouseMiddleName;
    }

    public void setSpouseMobilePhone(String spouseMobilePhone) {
        this.spouseMobilePhone = spouseMobilePhone;
    }

    public void setSpouseMobilePhone2(String spouseMobilePhone2) {
        this.spouseMobilePhone2 = spouseMobilePhone2;
    }

    public void setSpouseNickname(String spouseNickname) {
        this.spouseNickname = spouseNickname;
    }

    public void setSpousePagerNumber(String spousePagerNumber) {
        this.spousePagerNumber = spousePagerNumber;
    }

    public void setSpouseProfession(String spouseProfession) {
        this.spouseProfession = spouseProfession;
    }

    public void setSpouseSocialWeb1(String spouseSocialWeb1) {
        this.spouseSocialWeb1 = spouseSocialWeb1;
    }

    public void setSpouseSocialWeb2(String spouseSocialWeb2) {
        this.spouseSocialWeb2 = spouseSocialWeb2;
    }

    public void setSpouseSocialWeb3(String spouseSocialWeb3) {
        this.spouseSocialWeb3 = spouseSocialWeb3;
    }

    public void setSpouseSocialWeb4(String spouseSocialWeb4) {
        this.spouseSocialWeb4 = spouseSocialWeb4;
    }

    public void setSpouseTitle(String spouseTitle) {
        this.spouseTitle = spouseTitle;
    }

    public void setSpouseVoiceSkype(String spouseVoiceSkype) {
        this.spouseVoiceSkype = spouseVoiceSkype;
    }

    public void setSpouseWebPage1(String spouseWebPage1) {
        this.spouseWebPage1 = spouseWebPage1;
    }

    public void setSpouseWebPage2(String spouseWebPage2) {
        this.spouseWebPage2 = spouseWebPage2;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTwelveMonthTotal(FastMoney twelveMonthTotal) {
        this.twelveMonthTotal = twelveMonthTotal;
    }

    public void setUser1(String user1) {
        this.user1 = user1;
    }

    public void setUser2(String user2) {
        this.user2 = user2;
    }

    public void setUser3(String user3) {
        this.user3 = user3;
    }

    public void setUser4(String user4) {
        this.user4 = user4;
    }

    public void setUser5(String user5) {
        this.user5 = user5;
    }

    public void setUser6(String user6) {
        this.user6 = user6;
    }

    public void setUser7(String user7) {
        this.user7 = user7;
    }

    public void setUser8(String user8) {
        this.user8 = user8;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    public void setVoiceSkype(String voiceSkype) {
        this.voiceSkype = voiceSkype;
    }

    public void setWebPage1(String webPage1) {
        this.webPage1 = webPage1;
    }

    public void setWebPage2(String webPage2) {
        this.webPage2 = webPage2;
    }

    public void setYearTotal(FastMoney yearTotal) {
        this.yearTotal = yearTotal;
    }

    @Override
    public String toString() {
        return String.format("%s:'%s'", contactId, fileAs);
    }
}
