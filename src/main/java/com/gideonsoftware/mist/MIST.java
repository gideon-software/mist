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

package com.gideonsoftware.mist;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.widgets.Display;

import com.gideonsoftware.mist.controllers.MainWindowController;
import com.gideonsoftware.mist.model.EmailModel;
import com.gideonsoftware.mist.model.HistoryModel;
import com.gideonsoftware.mist.model.MessageModel;
import com.gideonsoftware.mist.preferences.MistPreferenceManager;
import com.gideonsoftware.mist.preferences.Preferences;
import com.gideonsoftware.mist.tntapi.TntDb;
import com.gideonsoftware.mist.util.ui.Images;
import com.gideonsoftware.mist.views.MainWindowView;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.htmlparser.jericho.LoggerProvider;

/**
 * Main MIST class.
 */
public class MIST {
    private static Logger log = null;

    private static MainWindowView view = null;
    private static OptionSet options = null;

    public final static String APP_NAME = "MIST";
    public final static String COMPANY_NAME = "Gideon Software";
    public final static String COMPANY_NAME_NO_SPACES = "GideonSoftware";
    public final static String HOMEPAGE = "https://www.gideonsoftware.com";
    public final static String EMAIL_SUPPORT = "mist-support@gideonsoftware.com";
    public final static String FACEBOOK = "http://www.facebook.com/MIST4Tnt";

    public final static String REGEX_EMAILADDRESS = "([a-zA-Z0-9+._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z0-9_-]+)";

    public final static String OPTION_PROFILE = "profile";

    public final static String PREF_LOGFILE_LOGLEVEL = "mist.logfile.loglevel";

    private static String profileExt = "";
    private static String appDataDir = "";
    private static String appConfDir = "";
    private static String logfilePath = "";

    public static String configureLogging(@SuppressWarnings("rawtypes") Class clazz) {
        String logConfPath = null;
        String logPath = null;

        String logConfFileName = "log4j2.xml";
        String logFilename = String.format("mist%s.log", getProfileExt());

        if (isDevel()) {
            logConfPath = String.format("devel/%s", logConfFileName);
            logPath = String.format("devel/logs/%s", logFilename);
        } else if (Util.isMac()) {
            // Mac OS X: From inside an application bundle
            // TODO: Text Mac installation to verify if this works
//            URL mistJarURL = MIST.class.getProtectionDomain().getCodeSource().getLocation();
//            File mistJarFile = new File(mistJarURL.getPath());
//            String logXMLPath = String.format(
//                "%s/conf/%s",
//                mistJarFile.getParentFile().getParentFile().getPath(),
//                logConfFileName);
//            File logXMLFile = new File(logXMLPath);
//            logConfPath = logXMLFile.getAbsolutePath();
            logConfPath = logConfFileName;
            logPath = String.format("%s/Library/Logs/%s/%s", System.getProperty("user.home"), APP_NAME, logFilename);
        } else if (Util.isLinux()) {
            // Linux
            logConfPath = logConfFileName;
            logPath = getAppDataDir() + "logs/" + logFilename;
        } else {
            // Windows
            logConfPath = logConfFileName;
            logPath = getAppDataDir() + "logs\\" + logFilename;
        }
        System.setProperty("log.path", logPath);
        System.setProperty("log4j.configurationFile", logConfPath);
        log = LogManager.getRootLogger();
        try {
            String logConfFilePath = new File(logConfPath).getAbsolutePath();
            logfilePath = new File(logPath).getAbsolutePath();
            log.debug("Log configuration path: {}", logConfFilePath);
            log.debug("Log path: {}", logfilePath);
        } catch (NullPointerException e) {
            System.out.println("Incorrect configuration settings; confPath: " + logConfPath + "; logPath: " + logPath);
        }

        // Disable Jericho logging
        net.htmlparser.jericho.Config.LoggerProvider = LoggerProvider.DISABLED;

        return logPath;
    }

    private static void configureUserSettings() {
        // Set global profile extension, which can be configured via options or on account of being in Devel mode
        String profileOptionStr = getOption(OPTION_PROFILE);
        if (profileOptionStr != null)
            profileExt = "-" + profileOptionStr;
        else if (isDevel())
            profileExt += "-devel";
        else
            profileExt = "";

        // Set app data dir
        appDataDir = "";
        if (Util.isWindows())
            appDataDir = String.format("%s\\%s\\", System.getenv("APPDATA"), APP_NAME.toLowerCase());
        else if (Util.isLinux())
            appDataDir = String.format("%s/.%s/", System.getProperty("user.home"), APP_NAME.toLowerCase());
        else { // Mac
            appDataDir = ""; // TODO! Also use in configureLogging()
        }

        // Set conf data dir
        appConfDir = appDataDir + "conf" + File.separator;

        try {
            // Verify that the directory exists
            Path dataDirPath = Paths.get(appConfDir);

            if (Files.notExists(dataDirPath))
                Files.createDirectory(dataDirPath);

            // Verify that the directory is secure; set permissions so that only the current user can read the file
            if (Util.isMac() || Util.isLinux()) {
                Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwx------");
                Files.setPosixFilePermissions(dataDirPath, permissions);
            } else { // Windows; see https://stackoverflow.com/a/13892920/1307022
                AclFileAttributeView aclAttr = Files.getFileAttributeView(dataDirPath, AclFileAttributeView.class);
                UserPrincipalLookupService upls = dataDirPath.getFileSystem().getUserPrincipalLookupService();
                UserPrincipal user = upls.lookupPrincipalByName(System.getProperty("user.name"));
                AclEntry.Builder builder = AclEntry.newBuilder();
                builder.setPermissions(
                    EnumSet.of(
                        AclEntryPermission.APPEND_DATA,
                        AclEntryPermission.DELETE,
                        AclEntryPermission.DELETE_CHILD,
                        AclEntryPermission.EXECUTE,
                        AclEntryPermission.READ_ACL,
                        AclEntryPermission.READ_ATTRIBUTES,
                        AclEntryPermission.READ_DATA,
                        AclEntryPermission.READ_NAMED_ATTRS,
                        AclEntryPermission.SYNCHRONIZE,
                        AclEntryPermission.WRITE_ACL,
                        AclEntryPermission.WRITE_ATTRIBUTES,
                        AclEntryPermission.WRITE_DATA,
                        AclEntryPermission.WRITE_NAMED_ATTRS,
                        AclEntryPermission.WRITE_OWNER));
                builder.setPrincipal(user);
                builder.setType(AclEntryType.ALLOW);
                aclAttr.setAcl(Collections.singletonList(builder.build()));
            }
        } catch (IOException e) {
            log.error(e);
        }
    }

    public static String getAppConfDir() {
        return appConfDir;
    }

    public static String getAppDataDir() {
        return appDataDir;
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

    public static String getProfileExt() {
        return profileExt;
    }

    public static MainWindowView getView() {
        return view;
    }

    public static void initModel() {
        log.trace("initModel()");
        TntDb.init();
        HistoryModel.init();
        MessageModel.init();
        EmailModel.init();
    }

    public static boolean isDevel() {
        // Check for existence of devel folder
        return Files.exists(Path.of("devel"));
    }

    public static void main(String[] args) {

        //
        // Initialization
        //

        // Command-line arguments
        parseOptions(args);

        // User settings
        configureUserSettings();

        // Logging
        configureLogging(MIST.class);

        // Load preferences
        MIST.getPrefs();

        // Set user-specified logging level now that preferences are loaded
        MIST.getPrefs().setDefault(PREF_LOGFILE_LOGLEVEL, Level.WARN.name()); // If changed, see LoggingPreferencePage
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

        // Shut down
        shutdown();
    }

    public static void parseOptions(String[] opts) {
        OptionParser parser = new OptionParser();
        parser.accepts(OPTION_PROFILE).withRequiredArg();
        options = parser.parse(opts);
    }

    public static void setLogfileLogLevel(String levelName) {
        log.debug("Setting logfile log level to '{}'", levelName);
        Level level = Level.toLevel(levelName, Level.OFF);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        LoggerConfig rootLoggerConfig = config.getLoggers().get("");
        rootLoggerConfig.removeAppender("logfile");
        rootLoggerConfig.addAppender(config.getAppender("logfile"), level, null);
        ctx.updateLoggers();
    }

    /**
     * 
     */
    private static void shutdown() {
        log.trace("shutdown()");

        // Save preferences
        log.trace("shutdown: Saving preferences...");
        try {
            MIST.getPrefs().save();
        } catch (IOException e) {
            log.error("Could not save preferences.", e);
        }

        if (Display.getDefault() != null && !Display.getDefault().isDisposed()) {
            log.trace("shutdown: Disposing display...");
            Display.getDefault().dispose();
        }

        Thread shutdownThread = new Thread() {
            @Override
            public void run() {
                // Shut down Email import server
                if (EmailModel.isImporting()) {
                    log.trace("shutdown: Shutting down email import service...");
                    EmailModel.stopImportService();
                    while (EmailModel.isImporting()) {
                        try {
                            sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                }

                // Shut down Tnt import service
                if (TntDb.isImporting()) {
                    log.trace("shutdown: Shutting down TntConnect import service...");
                    TntDb.stopImportService();
                    while (TntDb.isImporting()) {
                        try {
                            sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                    TntDb.disconnect();
                }

                // Disconnect all email servers
                // This must happen last because the TntImport may need to execute commands on the server
                // (e.g. removing labels, etc.)
                log.trace("shutdown: Disconnecting email servers...");
                EmailModel.disconnectServers();

                log.trace("=== shutdown complete ===");
            }
        };
        shutdownThread.setName("Shutdown");
        shutdownThread.start();
    }
}
