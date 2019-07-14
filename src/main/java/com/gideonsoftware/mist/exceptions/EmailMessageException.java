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

package com.gideonsoftware.mist.exceptions;

public class EmailMessageException extends Exception {
    private static final long serialVersionUID = -8231607043120603363L;

    public EmailMessageException() {
    }

    public EmailMessageException(String message) {
        super(message);
    }

    public EmailMessageException(Throwable cause) {
        super(cause);
    }

    public EmailMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
