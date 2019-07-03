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

package com.github.tomhallman.mist;

import java.io.File;
import java.net.URL;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.widgets.Display;

import com.github.tomhallman.mist.controllers.MainWindowController;
import com.github.tomhallman.mist.model.EmailModel;
import com.github.tomhallman.mist.model.HistoryModel;
import com.github.tomhallman.mist.model.MessageModel;
import com.github.tomhallman.mist.preferences.MistPreferenceManager;
import com.github.tomhallman.mist.preferences.Preferences;
import com.github.tomhallman.mist.tntapi.TntDb;
import com.github.tomhallman.mist.util.ui.Images;
import com.github.tomhallman.mist.views.MainWindowView;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Main MIST class.
 */
public class MIST {
    private static Logger log = null;

    private static MainWindowView view = null;
    private static OptionSet options = null;

    public final static String APP_NAME = "MIST";
    public final static String FACEBOOK = "http://www.facebook.com/MIST4Tnt";
    public final static String HOMEPAGE = "https://github.com/tomhallman/mist";
    public final static String MANUAL = "https://github.com/tomhallman/mist/wiki/manual";
    public final static String USERLIST = "mist-users@googlegroups.com"; // TODO

    public final static String REGEX_EMAILADDRESS = "([a-zA-Z0-9+._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z0-9_-]+)";

    public final static String OPTION_PROFILE = "profile";

    public final static String PREF_LOGFILE_LOGLEVEL = "mist.logfile.loglevel";

    private static String logfilePath = "";

    public static String configureLogging(@SuppressWarnings("rawtypes") Class clazz) {
        String confPath = null;
        String logPath = null;

        String profileName = MIST.getOption(MIST.OPTION_PROFILE);
        profileName = (profileName == null) ? "" : "-" + profileName;

        String logConfFileName = "log4j2.xml";
        String logFilename = String.format("mist%s.log", profileName);

        if (isDevel()) {
            confPath = String.format("devel/conf/%s", logConfFileName);
            logPath = String.format("devel/logs/%s", logFilename);
        } else if (Util.isMac()) {
            // Mac OS X: From inside an application bundle
            URL mistJarURL = MIST.class.getProtectionDomain().getCodeSource().getLocation();
            File mistJarFile = new File(mistJarURL.getPath());
            String logXMLPath = String.format(
                "%s/conf/%s",
                mistJarFile.getParentFile().getParentFile().getPath(),
                logConfFileName);
            File logXMLFile = new File(logXMLPath);
            confPath = logXMLFile.getAbsolutePath();
            logPath = String.format("%s/Library/Logs/%s/%s", System.getProperty("user.home"), APP_NAME, logFilename);
        } else if (Util.isLinux()) {
            // Linux
            // TODO: Test Linux installation to verify if this works
            confPath = String.format("conf/%s", logConfFileName);
            logPath = String.format("%s/.%s/%s", System.getProperty("user.home"), APP_NAME.toLowerCase(), logFilename);
        } else {
            // Windows
            confPath = String.format("conf\\%s", logConfFileName);
            logPath = String.format("%s\\%s\\logs\\%s", System.getenv("APPDATA"), APP_NAME, logFilename);
        }
        System.setProperty("log.path", logPath);
        System.setProperty("log4j.configurationFile", confPath);
        log = LogManager.getRootLogger();
        try {
            String logConfFilePath = new File(confPath).getAbsolutePath();
            logfilePath = new File(logPath).getAbsolutePath();
            log.debug("Log configuration path: {}", logConfFilePath);
            log.debug("Log path: {}", logfilePath);
        } catch (NullPointerException e) {
            System.out.println("Incorrect configuration settings; confPath: " + confPath + "; logPath: " + logPath);
        }
        return logPath;
    }

    public static String getAppVersion() {
        String ver = MIST.class.getPackage().getImplementationVersion();
        if (ver == null)
            return "(Devel build)";
        return ver;
    }

    public static String getLogfilePath() {
        return logfilePath;
    }

    public static String getOption(String option) {
        return (String) options.valueOf(option);
    }

    public static MistPreferenceManager getPreferenceManager() {
        return MistPreferenceManager.getInstance();
    }

    public static Preferences getPrefs() {
        return Preferences.getInstance();
    }

    public static MainWindowView getView() {
        return view;
    }

    public static void initModel() {
        log.trace("initModel()");
        EmailModel.init();
        HistoryModel.init();
        MessageModel.init();
        TntDb.init();
    }

    public static boolean isDevel() {
        String ver = MIST.class.getPackage().getImplementationVersion();
        return ver == null;
    }

    public static void main(String[] args) {

        //
        // Initialization
        //

        // Command-line arguments
        parseOptions(args);

        // Logging
        configureLogging(MIST.class);

        // Load preferences
        MIST.getPrefs();

        // Set user-specified logging level now that preferences are loaded
        MIST.getPrefs().setDefault(PREF_LOGFILE_LOGLEVEL, Level.WARN.name());
        setLogfileLogLevel(MIST.getPrefs().getString(PREF_LOGFILE_LOGLEVEL));

        // Display & images
        Display.setAppName(APP_NAME);
        new Display(); // Needed for ImageManager::init()
        Images.init();

        //
        // Fire up MVC framework
        //

        initModel();
        view = new MainWindowView();
        MainWindowController controller = new MainWindowController(view);
        controller.openView();

        //
        // Shut down
        //

        if (TntDb.isConnected())
            TntDb.disconnect();

        if (Display.getCurrent() != null && Display.getCurrent().isDisposed())
            Display.getCurrent().dispose();
    }

    public static void parseOptions(String[] opts) {
        OptionParser parser = new OptionParser();
        parser.accepts(OPTION_PROFILE).withRequiredArg();
        options = parser.parse(opts);
    }

    public static void setLogfileLogLevel(String levelName) {
        log.info("Setting logfile log level to '{}'", levelName);
        Level level = Level.toLevel(levelName, Level.OFF);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        LoggerConfig rootLoggerConfig = config.getLoggers().get("");
        rootLoggerConfig.removeAppender("logfile");
        rootLoggerConfig.addAppender(config.getAppender("logfile"), level, null);
        ctx.updateLoggers();
    }
}
