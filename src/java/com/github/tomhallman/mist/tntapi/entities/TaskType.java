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
 * Constants taken from the TntConnect "TaskType" table
 */
public class TaskType {

    public final static int APPOINTMENT = 1;
    public final static int THANK = 2;
    public final static int TODO = 3;
    public final static int CALL = 20;
    public final static int REMINDER_LETTER = 30;
    public final static int SUPPORT_LETTER = 40;
    public final static int LETTER = 50;
    public final static int NEWSLETTER = 60;
    public final static int E_NEWSLETTER = 65;
    public final static int PRE_CALL_LETTER = 70;
    public final static int EMAIL = 100;
    public final static int UNSCHEDULED_VISIT = 120;
    public final static int NOTE = 130;
    public final static int FACEBOOK = 140;
    public final static int TEXT_SMS = 150;
    public final static int PRESENT = 160;
    public final static int MAILCHIMP = 170;
    public final static int WHATSAPP = 180;
    public final static int DATA_CHANGE = 190;

    private TaskType() {
    }
}
