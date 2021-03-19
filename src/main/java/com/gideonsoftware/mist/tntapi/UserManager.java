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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gideonsoftware.mist.exceptions.TntDbException;
import com.gideonsoftware.mist.tntapi.entities.User;

/**
 * 
 */
public class UserManager {
    private static Logger log = LogManager.getLogger();

    private UserManager() {
    }

    /**
     * Returns all users in the TntConnect database, sorted by username.
     * 
     * @return all users in the TntConnect database
     * @throws SQLException
     *             if there is a database access problem
     */
    public static User[] getUserList() throws SQLException {
        log.trace("getUserList()");
        List<User> users = new ArrayList<User>();
        Statement stmt = TntDb.getConnection().createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
        ResultSet rs = stmt.executeQuery("SELECT [UserID], [UserName] FROM [User] ORDER BY [UserName]");

        // Log result if tracing
        if (log.isTraceEnabled())
            log.trace(TntDb.getResultSetString(rs));

        while (rs.next())
            users.add(new User(rs.getInt("UserID"), rs.getString("UserName")));
        return users.toArray(new User[0]);
    }

    /**
     * Returns the username associated with the specified user ID.
     * 
     * @param userId
     *            the user ID; null returns null
     * @return the username associated with the specified user ID or null if none exists.
     * @throws TntDbException
     *             if there is more than one username associated with this user ID
     * @throws SQLException
     *             if there is a database access problem
     */
    public static String getUserName(Integer userId) throws TntDbException, SQLException {
        log.trace("getUserName({})", userId);
        if (userId == null)
            return null;
        return TntDb.getOneString("SELECT [UserName] FROM [User] WHERE [UserID] = ?", userId);
    }

}
