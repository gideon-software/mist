/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2020 Gideon Software
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

package com.gideonsoftware.mist.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.gideonsoftware.mist.MIST;

public class UpdateModelTest {

    @BeforeAll
    public static void globalSetUp() {
        MIST.configureLogging(UpdateModel.class);
    }

    @Test
    public void isVersionNewer() {
        String[][] tests = {
            // { curVer, testVer, isTestVerNewer }
            { null, null, "false" },
            { null, "", "false" },
            { "1.0", null, "false" },

            { "1.0", "1.0", "false" },
            { "1.0", "1.0-beta", "false" },
            { "1.0", "1.0.0", "false" },
            { "1.0", "1.0.0-beta", "false" },
            { "1.0", "1.0.0-beta.1", "false" },
            { "1.0", "1.0.1", "true" },
            { "1.0", "1.1", "true" },
            { "1.0", "1.1-beta", "true" },

            { "1.0.0", "1.0", "false" },
            { "1.0.0", "1.0-beta", "false" },
            { "1.0.0", "1.0.0", "false" },
            { "1.0.0", "1.0.0-beta", "false" },
            { "1.0.0", "1.0.0-beta.1", "false" },
            { "1.0.0", "1.0.1", "true" },
            { "1.0.0", "1.1", "true" },
            { "1.0.0", "1.1-beta", "true" },

            { "1.1", "1.0", "false" },
            { "1.1", "1.0-beta", "false" },
            { "1.1", "1.0.0", "false" },
            { "1.1", "1.0.0-beta", "false" },
            { "1.1", "1.0.0-beta.1", "false" },
            { "1.1", "1.0.1", "false" },
            { "1.1", "1.1", "false" },
            { "1.1", "1.1-beta", "false" },

            { "1.1", "1.1", "false" },
            { "1.1", "1.1-beta", "false" },
            { "1.1", "1.1.0", "false" },
            { "1.1", "1.1.0-beta", "false" },
            { "1.1", "1.1.0-beta.1", "false" },
            { "1.1", "1.1.1", "true" },
            { "1.1", "1.1.1-beta", "true" },
            { "1.1", "1.1.1-beta.1", "true" },

            { "1.0-beta", "1.0", "true" },
            { "1.0-beta", "1.0-beta", "false" },
            { "1.0-beta", "1.0.0-beta", "false" },
            { "1.0-beta", "1.0.0-beta.1", "false" },
            { "1.0-beta", "1.0.0-beta.2", "true" },
            { "1.0-beta", "1.0.1", "true" },
            { "1.0-beta", "1.1", "true" },
            { "1.0-beta", "1.1-beta", "true" } };

        for (int i = 0; i < tests.length; i++) {
            boolean expectedResult = Boolean.parseBoolean(tests[i][2]);
            assertEquals(UpdateModel.isVersionNewer(tests[i][0], tests[i][1]), expectedResult);
        }
    }
}
