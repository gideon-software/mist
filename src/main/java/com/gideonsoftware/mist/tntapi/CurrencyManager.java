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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gideonsoftware.mist.tntapi.entities.Currency;

/**
 * 
 */
public class CurrencyManager {
    private static Logger log = LogManager.getLogger();
    private static ArrayList<Currency> values = new ArrayList<Currency>();

    private CurrencyManager() {
    }

    /**
     * Returns the currency associated with the specified currency ID or null if none exists.
     *
     * @param currencyId
     *            the currency ID; null returns null
     * @return the currency associated with the specified currency ID or null if none exists
     */
    public static Currency get(Integer currencyId) {
        log.trace("get({})", currencyId);

        for (Iterator<Currency> it = values.iterator(); it.hasNext();) {
            Currency currency = it.next();
            if (currency.getCurrencyId().equals(currencyId))
                return currency;
        }
        return null;
    }

    /**
     * Returns the base currency ID.
     * 
     * @return the base currency ID
     */
    public static Integer getBaseCurrencyId() {
        log.trace("getBaseCurrencyId()");

        for (Iterator<Currency> it = values.iterator(); it.hasNext();) {
            Currency currency = it.next();
            if (currency.isBase())
                return currency.getCurrencyId();
        }
        return null;
    }

    /**
     * Loads the Currency table from the TntConnect database.
     *
     * @throws SQLException
     *             if there is a database access problem
     */
    public static void load() throws SQLException {
        log.trace("load()");

        Statement stmt = TntDb.getConnection().createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
        ResultSet rs = stmt.executeQuery("SELECT * FROM [Currency]");

        while (rs.next()) {
            Currency currency = new Currency();
            currency.setCurrencyId(rs.getInt("CurrencyID"));
            currency.setLastEdit(TntDb.timestampToDate(rs.getTimestamp("LastEdit")));
            currency.setCode(rs.getString("Code"));
            currency.setSymbol(rs.getString("Symbol"));
            currency.setDescription(rs.getString("Description"));
            currency.setDecimalPlaces(rs.getInt("DecimalPlaces"));
            currency.setColor(rs.getInt("Color"));
            currency.setLocalExchangeRate(rs.getInt("LocalExchangeRate"));
            currency.setIsBase(rs.getBoolean("IsBase"));
            currency.setAutoUpdateRate(rs.getBoolean("AutoUpdateRate"));
            currency.setDaysBetweenAutoUpdateRate(rs.getInt("DaysBetweenAutoUpdateRate"));
            currency.setLastRateUpdate(TntDb.timestampToDate(rs.getTimestamp("LastRateUpdate")));
            values.add(currency);
        }

        if (log.isTraceEnabled())
            log.trace(TntDb.getResultSetString(rs));
    }
}
