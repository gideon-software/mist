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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gideonsoftware.mist.MIST;

/**
 * 
 */
public class UpdateModel {
    private static Logger log = LogManager.getLogger();

    // Constants
    public final static String CHANNEL_STABLE = "stable";
    public final static String CHANNEL_BETA = "beta";

    // Preferences
    public final static String PREF_UPDATE_CHANNEL = "update.channel";

    // Property change values
    private final static PropertyChangeSupport pcs = new PropertyChangeSupport(UpdateModel.class);
    public final static String PROP_STATUS_INIT = "updatemodel.status.init";
    public final static String PROP_STATUS_CHECKING = "updatemodel.status.checking";
    public final static String PROP_STATUS_CHECKED = "updatemodel.status.checked";

    private static String errorMsg = "";
    private static String channel = "";
    private static String newVersion = "";
    private static String newVersionChannel = "";
    private static String newVersionDownloadURL = "";
    private static String newVersionInfoURL = "";

    /**
     * No instantiation allowed!
     */
    private UpdateModel() {
    }

    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        log.trace("addPropertyChangeListener({})", listener);
        pcs.addPropertyChangeListener(listener);
    }

    public static void checkForUpdate() {
        log.trace("checkForUpdate()");

        // Don't need to check during development!
//        if (MIST.isDevel()) {
//            log.debug("Skipping check for new version because MIST is in devel mode");
//            return;
//        }

        pcs.firePropertyChange(PROP_STATUS_CHECKING, false, true);

        // Thread
        Thread updateThread = new Thread() {
            @Override
            public void run() {
                String jsonStr = "";
                // Connect to update URL
                try {
                    CloseableHttpClient httpClient = HttpClients.createDefault();
                    URIBuilder builder = new URIBuilder(MIST.UPDATE_URL);
                    builder.setParameter("channel", channel).setParameter("curVersion", MIST.getAppVersion());
                    HttpGet get = new HttpGet(builder.build());
                    CloseableHttpResponse response = httpClient.execute(get);

                    // Get the response code
                    int status = response.getStatusLine().getStatusCode();
                    if (status != 200) {
                        log.error("Http Response {}: {}", status, response.getStatusLine().getReasonPhrase());
                        setErrorMsg(response.getStatusLine().getReasonPhrase());
                        setChecked();
                        return;
                    }

                    // Read the response
                    jsonStr = EntityUtils.toString(response.getEntity());
                    response.close();
                    httpClient.close();
                } catch (URISyntaxException | ParseException | IOException e) {
                    String errorStr = "Error connecting to update server";
                    log.error(errorStr, e);
                    setErrorMsg(errorStr);
                    setChecked();
                    return;
                }

                // Parse the resulting JSON
                ObjectMapper mapper = new ObjectMapper();
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> jsonMap = mapper.readValue(jsonStr, Map.class);
                    setNewVersion(jsonMap.get("newVersion"));
                    setNewVersionChannel(jsonMap.get("channel"));
                    setNewVersionDownloadURL(jsonMap.get("downloadURL"));
                    setNewVersionInfoURL(jsonMap.get("infoURL"));
                } catch (JsonProcessingException e) {
                    String errorStr = "Error parsing server response";
                    log.error(errorStr, e);
                    setErrorMsg(errorStr);
                    setChecked();
                    return;
                }

                setChecked();
            } // updateThread.run()
        };
        updateThread.setName("Update");
        updateThread.start();
    }

    public static String getChannel() {
        return channel;
    }

    public static String getErrorMsg() {
        return errorMsg;
    }

    public static String getNewVersion() {
        return newVersion;
    }

    public static String getNewVersionChannel() {
        return newVersionChannel;
    }

    public static String getNewVersionDownloadURL() {
        return newVersionDownloadURL;
    }

    public static String getNewVersionInfoURL() {
        return newVersionInfoURL;
    }

    /**
     * Helper function for building URL
     * 
     * @param params
     *            Map of parameters of form &lt;name, value&gt;
     * @return URL-encoded parameter string
     * @throws UnsupportedEncodingException
     *             if encoding of parameters fails
     * @see https://www.baeldung.com/java-http-request
     */
    public static String getParamsString(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0 ? resultString.substring(0, resultString.length() - 1) : resultString;
    }

    public static void init() {
        log.trace("init()");
        // Load channel from preferences
        channel = MIST.getPrefs().getString(PREF_UPDATE_CHANNEL);

        // If channel didn't previously exist, calculate it based on current version
        if (channel.isBlank()) {
            channel = CHANNEL_STABLE;
            if (MIST.isDevel() || MIST.getAppVersion().contains("beta"))
                channel = CHANNEL_BETA;
            MIST.getPrefs().setValue(PREF_UPDATE_CHANNEL, channel);
        }

        log.info("MIST update channel is '{}'", channel);
        pcs.firePropertyChange(PROP_STATUS_INIT, false, true);
    }

    public static boolean isUpdateAvailable() {
        if (newVersion.isBlank())
            return false;

        boolean updateAvailable = isVersionNewer(MIST.getAppVersion(), newVersion);
        if (updateAvailable)
            log.info(
                "MIST update is available! Current version: {}; New version: {}",
                MIST.getAppVersion(),
                newVersion);
        return updateAvailable;
    }

    public static boolean isVersionNewer(String currentVersion, String potentiallyNewerVersion) {
        log.trace("isVersionNewer({},{})", currentVersion, potentiallyNewerVersion);

        if (currentVersion == null
            || currentVersion.isBlank()
            || potentiallyNewerVersion == null
            || potentiallyNewerVersion.isBlank())
            return false;

        int[] currVer = null;
        int[] testVer = null;
        try {
            currVer = parseVersionNumber(currentVersion);
            testVer = parseVersionNumber(potentiallyNewerVersion);
        } catch (IllegalArgumentException e) {
            return false;
        }

        for (int i = 0; i < testVer.length; i++)
            if (testVer[i] != currVer[i])
                return testVer[i] > currVer[i];

        return false; // Both versions are the same
    }

    /**
     * Inspiration: https://stackoverflow.com/a/11501749/1307022
     * 
     * @param ver
     * @return
     */
    private static int[] parseVersionNumber(String ver) {
        log.trace("partVersionNumber({})", ver);
        Matcher m = Pattern.compile("(\\d+)\\.(\\d+)(\\.\\d+)?(-beta(\\.\\d+)?)?").matcher(ver);
        if (!m.matches())
            throw new IllegalArgumentException("Malformed version");

        try {
            return new int[] {
                Integer.parseInt(m.group(1)), // major
                Integer.parseInt(m.group(2)), // minor
                m.group(3) == null ? 0 : Integer.parseInt(m.group(3).substring(1)), // revision (remove initial period)
                m.group(4) == null ? Integer.MAX_VALUE // no beta suffix
                    : m.group(5) == null || m.group(5).isEmpty() ? 1 // "beta" ==> beta.1
                        : Integer.parseInt(m.group(5).substring(1)) // "beta 2", etc. (remove initial period)
            };
        } catch (NumberFormatException e) {
            log.error("Could not parse version number", e);
            return new int[] { 0, 0, 0, 0 };
        }
    }

    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        log.trace("removePropertyChangeListener({})", listener);
        pcs.removePropertyChangeListener(listener);
    }

    public static void setChannel(String channel) {
        UpdateModel.channel = channel;
    }

    private static void setChecked() {
        log.trace("setChecked()");
        pcs.firePropertyChange(PROP_STATUS_CHECKED, false, true);
    }

    public static void setErrorMsg(String errorMsg) {
        UpdateModel.errorMsg = errorMsg;
    }

    public static void setNewVersion(String newVersion) {
        UpdateModel.newVersion = newVersion;
    }

    public static void setNewVersionChannel(String newVersionChannel) {
        UpdateModel.newVersionChannel = newVersionChannel;
    }

    public static void setNewVersionDownloadURL(String newVersionDownloadURL) {
        UpdateModel.newVersionDownloadURL = newVersionDownloadURL;
    }

    public static void setNewVersionInfoURL(String newVersionInfoURL) {
        UpdateModel.newVersionInfoURL = newVersionInfoURL;
    }

}
