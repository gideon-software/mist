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

package com.gideonsoftware.mist.tntapi;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.exceptions.TntDbException;
import com.gideonsoftware.mist.tntapi.TntDb;
import com.gideonsoftware.mist.tntapi.UserManager;
import com.gideonsoftware.mist.tntapi.entities.User;

@RunWith(JUnit4.class)
public class UserManagerTest {
    // private static Logger log;
    private static Integer USERID_TOM;
    private static Integer USERID_DAVID;

    public static void createTestData() {
        USERID_TOM = Integer.valueOf(1481326147);
        USERID_DAVID = Integer.valueOf(1404378063);
    }

    @BeforeClass
    public static void setup() {
        MIST.configureLogging(UserManager.class);
        TntDbTest.setupTntDb();
        createTestData();
    }

    @AfterClass
    public static void teardown() {
        TntDbTest.teardownTntDb();
    }

    /**
     * Tests getting user name
     */
    @Test
    public void getUserName() throws TntDbException, SQLException {
        assertEquals("Tom", UserManager.getUserName(USERID_TOM));
        assertEquals("David", UserManager.getUserName(USERID_DAVID));
    }

    /**
     * Tests getting user list
     */
    @Test
    public void getUsers() throws SQLException {
        User[] users = UserManager.getUserList();
        assertEquals(2, users.length);
        assertEquals(USERID_DAVID.intValue(), users[0].getId());
        assertEquals("David", users[0].getUsername());
        assertEquals(USERID_TOM.intValue(), users[1].getId());
        assertEquals("Tom", users[1].getUsername());
    }

    @After
    public void rollback() throws TntDbException {
        TntDb.rollback();
    }
}
