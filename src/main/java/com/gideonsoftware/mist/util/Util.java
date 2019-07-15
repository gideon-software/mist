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

package com.gideonsoftware.mist.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.exceptions.EmailServerException;
import com.gideonsoftware.mist.exceptions.TntDbException;
import com.gideonsoftware.mist.model.data.EmailServer;
import com.gideonsoftware.mist.model.data.PasswordData;
import com.gideonsoftware.mist.tntapi.TntDb;
import com.gideonsoftware.mist.util.ui.MistProgressMonitorDialog;
import com.gideonsoftware.mist.util.ui.PasswordInputDialog;

class EmailConnectionRunnable implements IRunnableWithProgress {

    private EmailServer server;
    private boolean openFolder;
    private boolean loadMessageList;

    public EmailConnectionRunnable(EmailServer server, boolean openFolder, boolean loadMessageList) {
        this.server = server;
        this.openFolder = openFolder;
        this.loadMessageList = loadMessageList;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        monitor.beginTask(
            String.format("%s: Connecting to email server...", server.getNickname()),
            IProgressMonitor.UNKNOWN);
        try {
            server.connect();
            if (server.isConnected() && openFolder) {
                monitor.setTaskName(String.format("%s: Opening folder...", server.getNickname()));
                server.openFolder();
                if (loadMessageList) {
                    monitor.setTaskName(String.format("%s: Loading message list...", server.getNickname()));
                    server.loadMessageList();
                }
            }
        } catch (EmailServerException e) {
            throw new InvocationTargetException(e);
        }
        monitor.done();
    }
}

public class Util {
    private static Logger log = LogManager.getLogger();

    /**
     * Add pattern matches in string to the specified list.
     * TODO: write test
     * 
     * @param list
     *            the list to add matches to
     * @param pattern
     *            the pattern to match on
     * @param string
     *            the string to find match in
     */
    public static void addMatchesToList(ArrayList<String> list, Pattern pattern, String string) {
        // log.trace("addMatchesToArr({},{},{})", list, pattern, string);
        if (!string.isBlank()) {
            Matcher matcher = pattern.matcher(string);
            while (matcher.find())
                list.add(string.substring(matcher.start(), matcher.end()));
        }
    }

    /**
     * Attempts to establish a connection to the given email server.
     * 
     * @param emailServer
     *            the email server to connect to
     * @param openFolder
     *            true if the email server's folder should be opened; false if not
     * @param loadMessageList
     *            true if we should load the message list; false if not
     */
    public static void connectToEmailServer(EmailServer emailServer, boolean openFolder, boolean loadMessageList) {
        log.trace("connectToEmailServer({},{},{})", emailServer, openFolder, loadMessageList);

        if (emailServer == null || emailServer.isConnected())
            return;

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    MistProgressMonitorDialog dialog = new MistProgressMonitorDialog(
                        Display.getDefault().getActiveShell());
                    dialog.setTitle("Connecting");
                    dialog.run(true, false, new EmailConnectionRunnable(emailServer, openFolder, loadMessageList));
                } catch (InvocationTargetException e) {
                    String msg = String.format(
                        "Unable to connect to email server '%s'.\nPlease check your settings and try again.",
                        emailServer.getNickname());
                    reportError("Email connection failed", msg, e.getCause()); // We want the cause, not the ITE
                } catch (InterruptedException e) {
                    // Not currently enabled...
                    log.debug("{{}} Connection to email server canceled.", emailServer.getNickname());
                    emailServer.disconnect();
                }
            }
        });
    }

    /**
     * Establishes a connection to the TntConnect database
     * 
     * @param tntDb
     *            The TntConnect database
     */
    public static void connectToTntDatabase() {
        log.trace("connectToTntDatabase()");

        if (TntDb.isConnected())
            return;

        try {
            MistProgressMonitorDialog dialog = new MistProgressMonitorDialog(Display.getDefault().getActiveShell());
            dialog.setTitle("Connecting");
            dialog.run(true, false, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Connecting to TntConnect database...", IProgressMonitor.UNKNOWN);
                    try {
                        TntDb.connect(true);
                    } catch (TntDbException e) {
                        throw new InvocationTargetException(e, e.getMessage());
                    }
                    monitor.done();
                }
            });
        } catch (InvocationTargetException e) {
            String msg = "Unable to connect to TntConnect database.\nPlease check your settings and try again.";
            reportError("TntConnect database connection failure", msg, e.getCause()); // We want the cause, not the ITE
        } catch (InterruptedException e) {
            // Not currently enabled...
            log.debug("Connection to TntConnect database canceled.");
            TntDb.disconnect();
        }
    }

    /**
     * Prompts the user for their email password
     * 
     * @param serverNickname
     *            Nickname of the email server to connect to
     * 
     * @return The user-supplied password data or null if the user canceled the operation.
     */
    public static PasswordData promptForEmailPassword(String serverNickname) {
        log.trace("promptForEmailPassword({})", serverNickname);

        // We need to use syncExec to get at the shell to prompt the user.
        // Thus this reference to passwordData needs to be thread-safe.
        // syncExec should block, regardless, but this works.
        final AtomicReference<PasswordData> passwordData = new AtomicReference<PasswordData>();

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                PasswordInputDialog dlg = new PasswordInputDialog(
                    Display.getDefault().getActiveShell(),
                    "Email Server Password Required",
                    String.format("Enter your password for '%s'", serverNickname),
                    "",
                    null);
                int result = dlg.open();
                if (result == Window.OK)
                    passwordData.set(dlg.getPasswordData());
                else
                    passwordData.set(null);
            }
        });
        return passwordData.get();
    }

    /**
     * Shows JFace ErrorDialog but improved by constructing full stack trace in detail area.
     * 
     * @see https://stackoverflow.com/a/9404081
     */
    public static void reportError(String title, String msg) {
        reportError(title, msg, null);
    }

    /**
     * Shows JFace ErrorDialog but improved by constructing full stack trace in detail area.
     * 
     * @see https://stackoverflow.com/a/9404081
     */
    public static void reportError(String title, String msg, Throwable e) {

        if (e == null) {
            // First log the error, if possible
            if (log != null)
                log.error(String.format("%s: %s", title, msg));
            // No throwable, so use simple MessageBox rather than ErrorDialog
            MessageBox msgBox = new MessageBox(Display.getDefault().getActiveShell(), SWT.ICON_ERROR | SWT.OK);
            msgBox.setText(title);
            msgBox.setMessage(msg);
            msgBox.open();
            return;
        }

        // First log the error, if possible
        if (log != null)
            log.error(String.format("%s: %s", title, msg), e);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);

        final String trace = stringWriter.toString(); // stack trace as a string

        List<Status> childStatuses = new ArrayList<>();

        // Split output by OS-independent new-line
        for (String line : trace.split(System.lineSeparator())) {
            // build & add status
            childStatuses.add(new Status(IStatus.ERROR, MIST.APP_NAME, line));
        }

        // convert to array of statuses
        String localizedMessage = e.getLocalizedMessage() == null ? e.toString() : e.getLocalizedMessage();
        MultiStatus ms = new MultiStatus(
            MIST.APP_NAME,
            IStatus.ERROR,
            childStatuses.toArray(new Status[] {}),
            localizedMessage,
            e);

        ErrorDialog.openError(Display.getDefault().getActiveShell(), title, msg, ms);
    }

}
