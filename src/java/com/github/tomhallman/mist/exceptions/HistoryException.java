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

package com.github.tomhallman.mist.exceptions;

public class HistoryException extends Exception {
    private static final long serialVersionUID = -8201607043120603363L;

    public HistoryException() {
    }

    public HistoryException(String message) {
        super(message);
    }

    public HistoryException(Throwable cause) {
        super(cause);
    }

    public HistoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
