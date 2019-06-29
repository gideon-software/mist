/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2019 Tom Hallman
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

import java.time.LocalDateTime;

/**
 * Values loaded from the TntConnect "Currency" table
 */
public class PledgeFrequency {
    // private static Logger log = LogManager.getLogger();

    private Integer pledgeFrequencyId = null;
    private LocalDateTime lastEdit = null;
    private String description = "";
    private int numberOfMonths = 0;

    public PledgeFrequency() {
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getLastEdit() {
        return lastEdit;
    }

    public int getNumberOfMonths() {
        return numberOfMonths;
    }

    public Integer getPledgeFrequencyId() {
        return pledgeFrequencyId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLastEdit(LocalDateTime lastEdit) {
        this.lastEdit = lastEdit;
    }

    public void setNumberOfMonths(int numberOfMonths) {
        this.numberOfMonths = numberOfMonths;
    }

    public void setPledgeFrequencyId(Integer pledgeFrequencyId) {
        this.pledgeFrequencyId = pledgeFrequencyId;
    }
}
