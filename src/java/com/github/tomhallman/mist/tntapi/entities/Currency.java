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
public class Currency {
    // private static Logger log = LogManager.getLogger();

    private Integer currencyId = null;
    private LocalDateTime lastEdit = null;
    private String code = "";
    private String symbol = "";
    private String description = "";
    private int decimalPlaces = 2;
    private int color = 10708548; // from TntDb
    private int localExchangeRate = 1;
    private boolean isBase = false;
    private boolean autoUpdateRate = true;
    private int daysBetweenAutoUpdateRate = 30;
    private LocalDateTime lastRateUpdate = null;

    public Currency() {
    }

    public String getCode() {
        return code;
    }

    public int getColor() {
        return color;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public int getDaysBetweenAutoUpdateRate() {
        return daysBetweenAutoUpdateRate;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getLastEdit() {
        return lastEdit;
    }

    public LocalDateTime getLastRateUpdate() {
        return lastRateUpdate;
    }

    public int getLocalExchangeRate() {
        return localExchangeRate;
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean isAutoUpdateRate() {
        return autoUpdateRate;
    }

    public boolean isBase() {
        return isBase;
    }

    public void setAutoUpdateRate(boolean autoUpdateRate) {
        this.autoUpdateRate = autoUpdateRate;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    public void setDaysBetweenAutoUpdateRate(int daysBetweenAutoUpdateRate) {
        this.daysBetweenAutoUpdateRate = daysBetweenAutoUpdateRate;
    }

    public void setDecimalPlaces(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIsBase(boolean isBase) {
        this.isBase = isBase;
    }

    public void setLastEdit(LocalDateTime lastEdit) {
        this.lastEdit = lastEdit;
    }

    public void setLastRateUpdate(LocalDateTime lastRateUpdate) {
        this.lastRateUpdate = lastRateUpdate;
    }

    public void setLocalExchangeRate(int localExchangeRate) {
        this.localExchangeRate = localExchangeRate;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}
