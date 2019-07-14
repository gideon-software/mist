/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2019 Gideon Software
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
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gideonsoftware.mist.tntapi.entities.PledgeFrequency;

/**
 * 
 */
public class PledgeFrequencyManager {
    private static Logger log = LogManager.getLogger();
    private static ArrayList<PledgeFrequency> values = new ArrayList<PledgeFrequency>();

    private PledgeFrequencyManager() {
    }

    /**
     * Returns the pledge frequency associated with the specified pledge frequency ID or null if none exists.
     *
     * @param pledgeFrequencyId
     *            the pledge frequency ID; null returns null
     * @return the pledge frequency associated with the specified pledge frequency ID or null if none exists
     */
    public static PledgeFrequency get(Integer pledgeFrequencyId) {
        log.trace("get({})", pledgeFrequencyId);

        for (Iterator<PledgeFrequency> it = values.iterator(); it.hasNext();) {
            PledgeFrequency pledgeFrequency = it.next();
            if (pledgeFrequency.getPledgeFrequencyId().equals(pledgeFrequencyId))
                return pledgeFrequency;
        }
        return null;
    }

    /**
     * Loads the PledgeFrequency table from the TntConnect database.
     *
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void load() throws SQLException {
        log.trace("load()");

        ResultSet rs = TntDb.runQuery("SELECT * FROM [PledgeFrequency]", false);

        while (rs.next()) {
            PledgeFrequency pf = new PledgeFrequency();
            pf.setPledgeFrequencyId(rs.getInt("PledgeFrequencyID"));
            pf.setLastEdit(TntDb.timestampToDate(rs.getTimestamp("LastEdit")));
            pf.setDescription(rs.getString("Description"));
            pf.setNumberOfMonths(rs.getInt("NumberOfMonths"));
            values.add(pf);
        }
    }

}
