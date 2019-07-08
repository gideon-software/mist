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

package com.github.tomhallman.mist.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
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
import org.eclipse.swt.widgets.Shell;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.exceptions.EmailServerException;
import com.github.tomhallman.mist.exceptions.TntDbException;
import com.github.tomhallman.mist.model.data.EmailServer;
import com.github.tomhallman.mist.model.data.PasswordData;
import com.github.tomhallman.mist.tntapi.TntDb;
import com.github.tomhallman.mist.util.ui.MistProgressMonitorDialog;
import com.github.tomhallman.mist.util.ui.PasswordInputDialog;

class EmailConnectionRunnable implements IRunnableWithProgress {

    private EmailServer server;
    private boolean selectFolder;
    private PasswordData passwordData;

    public EmailConnectionRunnable(EmailServer server, boolean selectFolder, PasswordData passwordData) {
        this.server = server;
        this.selectFolder = selectFolder;
        this.passwordData = passwordData;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

        String msg = String.format("Connecting to email server '%s'", server.getNickname());
        monitor.beginTask(msg, IProgressMonitor.UNKNOWN);

        try {
            server.connect(selectFolder);

            // If save password had been requested, store in preferences now that we know it worked
            if (passwordData != null && server.isConnected() && passwordData.isSavePassword()) {
                server.setPasswordPrompt(false);
                server.setPassword(passwordData.getPassword());
            }

        } catch (EmailServerException e) {
            // If we'd asked for the password, clear it again
            if (passwordData != null)
                server.setPassword("");

            throw new InvocationTargetException(e);
        }

        monitor.done();
    }

}

class TntConnectionRunnable implements IRunnableWithProgress {

    public TntConnectionRunnable() {
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

        monitor.beginTask("Connecting to TntConnect database", IProgressMonitor.UNKNOWN);

        try {
            TntDb.connect(true);
        } catch (TntDbException e) {
            throw new InvocationTargetException(e, e.getMessage());
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
     * @param shell
     *            the shell for notifying the user of the connection taking place; if null, no notification will take
     *            place
     * @param emailServer
     *            the email server to connect to
     * @param selectFolder
     *            true if the email server's folder should be selected; false if not
     */
    public static void connectToEmailServer(Shell shell, EmailServer emailServer, boolean selectFolder) {
        log.trace("connectToEmailServer({},{},{})", shell, emailServer, selectFolder);

        if (emailServer == null || emailServer.isConnected())
            return;

        // TODO: Handle null shell

        // Get password if prompting is required
        PasswordData passwordData = null;
        if (emailServer.isPasswordNeeded()) {
            passwordData = Util.promptForEmailPassword(shell, emailServer.getNickname());
            if (passwordData == null)
                return;
            emailServer.setPassword(passwordData.getPassword());
        }

        try {
            MistProgressMonitorDialog dialog = new MistProgressMonitorDialog(shell);
            dialog.setTitle("Connecting");
            dialog.run(true, false, new EmailConnectionRunnable(emailServer, selectFolder, passwordData));
        } catch (InvocationTargetException e) {
            String msg = String.format(
                "Unable to connect to email server '%s'.\nPlease check your settings and try again.",
                emailServer.getNickname());
            reportError("Email connection failed", msg, e.getCause()); // We want the cause, not the ITE
        } catch (InterruptedException e) {
            // Only needed if run is cancelable
        }
    }

    /**
     * Establishes a connection to the TntConnect database
     * 
     * @param shell
     *            The window's shell
     * @param tntDb
     *            The TntConnect database
     */
    public static void connectToTntDatabase(Shell shell) {
        log.trace("connectToTntDatabase({})", shell);

        if (TntDb.isConnected())
            return;

        // TODO: Handle null shell

        try {
            MistProgressMonitorDialog dialog = new MistProgressMonitorDialog(shell);
            dialog.setTitle("Connecting");
            dialog.run(true, false, new TntConnectionRunnable());
        } catch (InvocationTargetException e) {
            String msg = "Unable to connect to Tnt database.\nPlease check your settings and try again.";
            reportError("Tnt database connection failure", msg, e.getCause()); // We want the cause, not the ITE
        } catch (InterruptedException e) {
            // Only needed if run is cancelable
        }
    }

    /**
     * Prompts the user for their email password
     * 
     * @param shell
     *            shell from which to open the prompt; null returns null
     * @param serverNickname
     *            Nickname of the email server to connect to
     * 
     * @return The user-supplied password data or null if the user canceled the operation.
     */
    private static PasswordData promptForEmailPassword(Shell shell, String serverNickname) {
        log.trace("promptForEmailPassword({},{})", shell, serverNickname);

        if (shell == null)
            return null;

        PasswordInputDialog dlg = new PasswordInputDialog(
            shell,
            "Email Server Password Required",
            String.format("Enter your password for '%s'", serverNickname),
            "",
            null);
        int result = dlg.open();
        if (result == Window.OK)
            return dlg.getPasswordData();
        return null;
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
