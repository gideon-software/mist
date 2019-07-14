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

package com.gideonsoftware.mist.model.data;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.model.data.EmailMessage;
import com.gideonsoftware.mist.tntapi.TntDbTest;

public class EmailMessageTest {

    @BeforeClass
    public static void globalSetUp() {
        MIST.configureLogging(TntDbTest.class);
    }

    @Test
    public void testGuessFromName() {
        String[][] tests = {
            { null, "Contact" },
            { "", "Contact" },
            { "Luke Skywalker", "Luke" },
            { " Han  Solo ", "Han" },
            { "M. Night Shyamalan", "M. Night" },
            { "Bueller, Ferris", "Ferris" },
            { "=?UTF-8?B?3zSQ5y63IFRvZGQgD2hxaXN0ZW5zb24=?= John Doe", "John" } };

        for (int i = 0; i < tests.length; i++) {
            assertEquals(tests[i][1], EmailMessage.guessFromName(tests[i][0]));
        }
    }
}
