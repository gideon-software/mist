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

package com.github.tomhallman.mist.preferences;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.util.Util;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.model.data.EmailServer;
import com.github.tomhallman.mist.tntapi.TntDb;

public class Preferences extends PreferenceStore {
    private static Logger log = LogManager.getLogger();

    // We use JFace's Preferences functionality here, which somewhat breaks our normal MVC
    // architecture, but it's worthwhile for simplicity.
    private static Preferences prefs;

    private static final String DEFAULT_SEPARATOR = ";";
    private static String separator = DEFAULT_SEPARATOR;

    /**
     * Preferences singleton
     */
    private Preferences() {
        log.trace("Preferences()");
        initDefaults();
        loadPreferences();
    }

    /**
     * Thread-safe, lazily-initialized singleton creator.
     * 
     * @see https://en.wikipedia.org/wiki/Singleton_pattern
     * @return a handle to the Preferences object for this user
     */
    public static synchronized Preferences getInstance() {
        if (prefs == null) {
            prefs = new Preferences();
        }
        return prefs;
    }

    public static String getSeparator() {
        return separator;
    }

    public static void setSeparator(String sep) {
        separator = sep;
    }

    /**
     * Get the path for the preferences file
     * 
     * @return the path for the preferences file
     */
    protected String getPreferencesPath() {
        log.trace("getPreferencesPath()");
        String appName = MIST.APP_NAME.toLowerCase();

        String node = appName;
        String profileName = MIST.getOption(MIST.OPTION_PROFILE);
        if (profileName != null)
            node += "-" + profileName;

        String path = "";
        if (Util.isWindows())
            path = String.format("%s\\%s\\%s.properties", System.getenv("APPDATA"), appName, node);
        else // Mac or Linux
            path = String.format("%s/.%s/%s.properties", System.getProperty("user.home"), appName, node);
        return path;
    }

    public String[] getStrings(String name) {
        return StringUtils.split(getString(name), Preferences.getSeparator());
    }

    /**
     * Initialize default values
     */
    private void initDefaults() {
        log.trace("initDefaults()");
        // EmailServerModel initializes defaults for email servers, which have unique preference names
    }

    public boolean isConfigured() {
        log.trace("isConfigured()");

        // We need a Tnt DB
        String dbPath = getString(TntDb.PREF_TNT_DBPATH);
        if (dbPath.isEmpty())
            return false;

        // We need at least one email server set up
        if (MIST.getPreferenceManager().getEmailServerPrefCount() == 0)
            return false;

        return true;
    }

    /**
     * Load preferences
     */
    protected void loadPreferences() {
        log.trace("loadPreferences()");

        String path = getPreferencesPath();
        log.debug(String.format("Loading preferences from [%s]...", path));

        this.setFilename(path);
        JFacePreferences.setPreferenceStore(this);

        File file = new File(path);
        if (file.exists()) {
            try {
                load();
                log.debug("Preferences successfully loaded.");
            } catch (IOException e) {
                log.error("Couldn't load preferences file from {{}}!", path, e);
            }
        } else {
            // See if there are MIST 4.x settings that can be loaded
            loadPreferencesFromMIST4();
            savePreferences();
        }
    }

    private boolean loadPreferencesFromMIST4() {
        log.trace("loadPreferencesFromMIST4()");
        // Try to update from 4.x settings to 5.0 settings
        try {
            java.util.prefs.Preferences oldPrefs = java.util.prefs.Preferences.userRoot().node(
                "MIST" + (MIST.isDevel() ? "-devel" : ""));
            if (oldPrefs.nodeExists("TntMPD")) {
                log.info("Loading preferences from 4.x...");

                setValue(TntDb.PREF_TNT_DBPATH, oldPrefs.node("TntMPD").get("DbPath", null));

                // Pre-5.0 stored its one email server under "Mail" node

                // Folder
                String oldVal = oldPrefs.node("Mail").get("Folder", "");
                setValue(EmailServer.getPrefName(0, EmailServer.PREF_FOLDER), oldVal);

                // Host
                oldVal = oldPrefs.node("Mail").get("Host", "");
                setValue(EmailServer.getPrefName(0, EmailServer.PREF_HOST), oldVal);

                // My name
                oldVal = oldPrefs.node("Mail").get("MyName", "");
                setValue(EmailServer.getPrefName(0, EmailServer.PREF_MYNAME), oldVal);

                // Password
                oldVal = oldPrefs.node("Mail").get("Password", "");
                setValue(EmailServer.getPrefName(0, EmailServer.PREF_PASSWORD), oldVal);

                // Password prompt
                Boolean oldValBool = oldPrefs.node("Mail").getBoolean("PasswordPrompt", true);
                // Set the default here because we'll soon save our preferences
                setDefault(EmailServer.getPrefName(0, EmailServer.PREF_PASSWORD_PROMPT), true);
                setValue(EmailServer.getPrefName(0, EmailServer.PREF_PASSWORD_PROMPT), oldValBool);

                // Port
                oldVal = oldPrefs.node("Mail").get("Port", "");
                setValue(EmailServer.getPrefName(0, EmailServer.PREF_PORT), Integer.parseInt(oldVal));

                // Username
                oldVal = oldPrefs.node("Mail").get("User", "");
                setValue(EmailServer.getPrefName(0, EmailServer.PREF_USERNAME), oldVal);

                // There was no nickname; use host name
                oldVal = oldPrefs.node("Mail").get("Host", "");
                setValue(EmailServer.getPrefName(0, EmailServer.PREF_NICKNAME), oldVal);

                // Addresses to ignore for this email server
                List<String> ignoreAddresses = new ArrayList<String>();
                int i = 0;
                String val = null;
                do {
                    val = oldPrefs.node("Mail").node("IgnoreAddresses").get("Value" + i++, null);
                    if (val != null)
                        ignoreAddresses.add(val);
                } while (val != null);
                setValues(
                    EmailServer.getPrefName(0, EmailServer.PREF_ADDRESSES_IGNORE),
                    ignoreAddresses.toArray(new String[0]));

                // "My" Addresses for this email server
                List<String> myAddresses = new ArrayList<String>();
                i = 0;
                val = null;
                do {
                    val = oldPrefs.node("Mail").node("MyAddresses").get("Value" + i++, null);
                    if (val != null)
                        myAddresses.add(val);
                } while (val != null);
                setValues(
                    EmailServer.getPrefName(0, EmailServer.PREF_ADDRESSES_MY),
                    myAddresses.toArray(new String[0]));

                // Tnt User ID: Pre-5.0 associated the Tnt User Id with "TntMPD" rather than "Mail"
                Integer oldValInt = oldPrefs.node("TntMPD").getInt("UserId", 0);
                setValue(EmailServer.getPrefName(0, EmailServer.PREF_TNT_USERID), oldValInt);
                // Ignore former TntMPD/Username pref; no longer needed

                log.info("4.x preferences successfully loaded.");
                return true;
            }
        } catch (BackingStoreException e) {
            log.warn("Could not load preferences backing store.", e);
        }
        return false;
    }

    /**
     * Resets preference store to last saved state
     */
    public void resetPreferences() {
        log.trace("resetPreferences()");
        prefs = new Preferences();
    }

    /**
     * Save preferences
     */
    public void savePreferences() {
        log.trace("savePreferences()");

        if (needsSaving()) {
            String path = getPreferencesPath();
            log.debug(String.format("Saving preferences to [%s]...", path));
            try {
                // Create preference store (including parent folders) if it doesn't exist
                File file = new File(path);
                file.getParentFile().mkdirs();
                file.createNewFile();
                save();
                log.debug("Preferences successfully saved.");
            } catch (IOException e) {
                log.error("Couldn't save preferences file at {{}}!", path, e);
            }
        }
    }

    /**
     * Sets all preferences to their default values if the preference name contains the specified string
     * 
     * @param name
     *            The string to look for
     */
    public void setToDefaultIfContains(String name) {
        log.trace("setToDefaultIfContains({})", name);
        String[] prefNames = preferenceNames();
        for (int i = 0; i < prefNames.length; i++)
            if (prefNames[i].contains(name))
                setToDefault(prefNames[i]);
    }

////////////////////////////////////////////////////////////////////

    public void setValues(String name, String[] values) {
        setValue(name, StringUtils.join(values, Preferences.getSeparator()));
    }

}
