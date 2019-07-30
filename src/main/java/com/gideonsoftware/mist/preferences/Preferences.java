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

package com.gideonsoftware.mist.preferences;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.graphics.Rectangle;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.model.EmailModel;
import com.gideonsoftware.mist.model.data.ImapServer;
import com.gideonsoftware.mist.tntapi.TntDb;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Preferences extends PreferenceStore {
    private static Logger log = LogManager.getLogger();

    private static Preferences prefs;

    private static final String DEFAULT_SEPARATOR = ";";
    private static String separator = DEFAULT_SEPARATOR;

    /**
     * Preferences singleton
     */
    private Preferences() {
        log.trace("Preferences()");
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
            prefs.loadPreferences();
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
     * Clears all current preferences
     */
    private void clearPreferences() {
        log.trace("clearPreferences()");
        for (String pref : preferenceNames())
            setToDefault(pref);
    }

    public int[] getInts(String name) {
        String[] strings = getStrings(name);
        int[] ints = new int[strings.length];
        for (int i = 0; i < strings.length; i++)
            ints[i] = Integer.parseInt(strings[i]);
        return ints;
    }

    /**
     * Get the path for the preferences file
     * 
     * @return the path for the preferences file
     */
    protected String getPreferencesPath() {
        log.trace("getPreferencesPath()");
        return MIST.getAppConfDir() + MIST.APP_NAME.toLowerCase() + MIST.getProfileExt() + ".properties";
    }

    public Rectangle getRectangle(String name) {
        int[] rectVals = getInts(name);
        if (rectVals.length == 4)
            return new Rectangle(rectVals[0], rectVals[1], rectVals[2], rectVals[3]);
        else
            return new Rectangle(0, 0, 0, 0);
    }

    public String[] getStrings(String name) {
        return StringUtils.split(getString(name), Preferences.getSeparator());
    }

    public boolean isConfigured() {
        log.trace("isConfigured()");

        // We need a Tnt DB
        String dbPath = getString(TntDb.PREF_TNT_DBPATH);
        if (dbPath.isBlank())
            return false;

        // We need at least one email server set up and enabled
        if (EmailModel.getEnabledEmailServerCount() == 0)
            return false;

        return true;
    }

    /**
     * Load preferences
     */
    protected void loadPreferences() {
        log.trace("loadPreferences()");

        String path = getPreferencesPath();
        log.debug(String.format("Loading preferences from '%s'...", path));

        this.setFilename(path);
        JFacePreferences.setPreferenceStore(this);

        File file = new File(path);
        if (file.exists()) {
            try {
                load();
                log.debug("Preferences successfully loaded.");
            } catch (IOException e) {
                log.error("Couldn't load preferences file from '{}'!", path, e);
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
                log.debug("Loading preferences from 4.x...");

                setValue(TntDb.PREF_TNT_DBPATH, oldPrefs.node("TntMPD").get("DbPath", null));

                // Pre-5.0 stored its one IMAP server under "Mail" node
                ImapServer server = new ImapServer(0);
                server.setEnabled(true);
                server.setFolderName(oldPrefs.node("Mail").get("Folder", ""));
                server.setHost(oldPrefs.node("Mail").get("Host", ""));
                server.setNickname(oldPrefs.node("Mail").get("Host", "")); // There was no nickname; use host name
                server.setPassword(oldPrefs.node("Mail").get("Password", ""));
                server.setPasswordPrompt(oldPrefs.node("Mail").getBoolean("PasswordPrompt", true));
                server.setPort(oldPrefs.node("Mail").get("Port", ""));
                server.setUsername(oldPrefs.node("Mail").get("User", ""));
                // We no longer use the "MyName" feature

                // Tnt User ID: Pre-5.0 associated the Tnt User Id with "TntMPD" rather than "Mail"
                server.setTntUserId(oldPrefs.node("TntMPD").getInt("UserId", 0));
                server.setTntUsername(oldPrefs.node("TntMPD").get("Username", ""));

                // Addresses to ignore for this email server
                List<String> ignoreAddresses = new ArrayList<String>();
                int i = 0;
                String val = null;
                do {
                    val = oldPrefs.node("Mail").node("IgnoreAddresses").get("Value" + i++, null);
                    if (val != null)
                        ignoreAddresses.add(val);
                } while (val != null);
                server.setIgnoreAddresses(ignoreAddresses.toArray(new String[0]));

                // "My" Addresses for this email server
                List<String> myAddresses = new ArrayList<String>();
                i = 0;
                val = null;
                do {
                    val = oldPrefs.node("Mail").node("MyAddresses").get("Value" + i++, null);
                    if (val != null)
                        myAddresses.add(val);
                } while (val != null);
                server.setMyAddresses(myAddresses.toArray(new String[0]));
                EmailModel.addEmailServer(server);

                log.debug("4.x preferences successfully loaded.");
                return true;
            }
        } catch (BackingStoreException e) {
            log.warn("Could not load preferences backing store.", e);
        }
        return false;
    }

    /**
     * Replaces all preference names containing {@code oldPrefName} with {@code newPrefName}. Partial matches okay.
     * 
     * @param oldPrefName
     *            the old preference name
     * @param newPrefName
     *            the new preference name
     */
    public void replacePrefNames(String oldPrefName, String newPrefName) {
        log.trace("replacePrefNames({},{})", oldPrefName, newPrefName);

        // Create renamed preferences
        Map<String, String> newPrefs = new HashMap<String, String>();
        for (String prefName : preferenceNames())
            if (prefName.contains(oldPrefName))
                newPrefs.put(prefName.replace(oldPrefName, newPrefName), getString(oldPrefName));

        // Remove old preferences
        setToDefaultIfContains(oldPrefName);

        // Add new preferences
        for (String key : newPrefs.keySet())
            setValue(key, newPrefs.get(key));
    }

    /**
     * Resets preference store to last saved state
     */
    public void resetPreferences() {
        log.trace("resetPreferences()");
        clearPreferences();
        loadPreferences();
    }

    /**
     * Save preferences
     */
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", justification = "createNewFile return value is irrelevant")
    public void savePreferences() {
        log.trace("savePreferences()");

        if (needsSaving()) {
            String path = getPreferencesPath();
            log.debug(String.format("Saving preferences to '%s'...", path));
            try {
                // Create preference store (including parent folders) if it doesn't exist
                File file = new File(path);
                file.getParentFile().mkdirs();
                file.createNewFile();
                save();
                log.debug("Preferences successfully saved.");
            } catch (IOException e) {
                log.error("Couldn't save preferences file at '{}'!", path, e);
            }
        }
    }

    public void setDefault(String name, int[] values) {
        String[] strValues = new String[values.length];
        for (int i = 0; i < values.length; i++)
            strValues[i] = String.valueOf(values[i]);
        setDefault(name, strValues);
    }

    public void setDefault(String name, String[] values) {
        setDefault(name, StringUtils.join(values, Preferences.getSeparator()));
    }

    /**
     * Sets all preferences to their default values if the preference name contains the specified string.
     * 
     * @param name
     *            The string to look for
     */
    public void setToDefaultIfContains(String name) {
        log.trace("setToDefaultIfContains({})", name);
        for (String prefName : preferenceNames())
            if (prefName.contains(name))
                setToDefault(prefName);
    }

    public void setValue(String name, Rectangle rect) {
        int[] rectVals = { rect.x, rect.y, rect.width, rect.height };
        setValues(name, rectVals);
    }

    public void setValues(String name, int[] values) {
        String[] strValues = new String[values.length];
        for (int i = 0; i < values.length; i++)
            strValues[i] = String.valueOf(values[i]);
        setValues(name, strValues);
    }

    public void setValues(String name, String[] values) {
        setValue(name, StringUtils.join(values, Preferences.getSeparator()));
    }

}
