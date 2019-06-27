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

/**
 *
 */
public class ContactInfo {
    // private static Logger log = LogManager.getLogger();
    private Integer id = null;
    private String name = null;
    private String info = null;

    public ContactInfo() {
    }

    public ContactInfo(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public ContactInfo(Integer id, String name, String info) {
        this.id = id;
        this.name = name;
        this.info = info;
    }

    public ContactInfo(ContactInfo info) {
        this.id = info.getId();
        this.name = info.getName();
        this.info = info.getInfo();
    }

    /**
     * Guesses at a first and last name from the specified full name.
     * 
     * @param fullName
     *            the full name for which to guess a first and last name; null returns empty names
     * @return a two-element string array consisting of the first and last names
     */
    public static String[] guessFirstAndLastNames(String fullName) {
        if (fullName == null || fullName.isBlank())
            return new String[] { "", "" };

        String[] names = fullName.trim().split(" ");

        String firstName = "";
        for (int i = 0; i < names.length - 1; i++)
            firstName += (i > 0 ? " " : "") + names[i];

        String lastName = names.length > 0 ? names[names.length - 1] : "";

        return new String[] { firstName, lastName };
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ContactInfo))
            return false;

        ContactInfo ci = (ContactInfo) obj;

        if (this.id == null && ci.id == null) {
            // Compare emails
            if (this.info == null && ci.info == null) {
                return true;
            } else if (this.info == null || ci.info == null) {
                return false;
            } else
                return this.info.equals(ci.info);
        } else if (this.id == null || ci.id == null) {
            return false;
        } else
            return this.id.equals(ci.id);
    }

    public Integer getId() {
        return id;
    }

    public String getInfo() {
        return info;
    }

    public String getName() {
        return name;
    }

    public String guessFirstName() {
        return guessFirstAndLastNames(name)[0];
    }

    public String guessLastName() {
        return guessFirstAndLastNames(name)[1];
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("TntContactInfo [id=%s, name=%s, info=%s]", id, name, info);
    }

}
