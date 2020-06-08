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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.exceptions.EmailServerException;
import com.gideonsoftware.mist.exceptions.TntDbException;
import com.gideonsoftware.mist.model.data.EmailServer;
import com.gideonsoftware.mist.tntapi.TntDb;
import com.gideonsoftware.mist.util.ui.MistProgressMonitorDialog;

class EmailConnectionRunnable implements IRunnableWithProgress {

    private EmailServer server;

    public EmailConnectionRunnable(EmailServer server) {
        this.server = server;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        monitor.beginTask(
            String.format("%s: Connecting to email server...", server.getNickname()),
            IProgressMonitor.UNKNOWN);
        try {
            server.connect();
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
     */
    public static void connectToEmailServer(EmailServer emailServer) {
        log.trace("connectToEmailServer({})", emailServer);

        if (emailServer == null)
            return;

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    MistProgressMonitorDialog dialog = new MistProgressMonitorDialog(
                        Display.getDefault().getActiveShell());
                    dialog.setTitle("Connecting");
                    dialog.run(true, false, new EmailConnectionRunnable(emailServer));
                } catch (InvocationTargetException e) {
                    String msg = String.format(
                        "Unable to connect to email server '%s'.%nPlease check your settings and try again.",
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
     * Increment the integer at the specified preference by the specified amount
     * 
     * @param prefName
     *            The name of the preference to increment
     * @param incBy
     *            The amount to increment by
     */
    public static void incPrefCounter(String prefName, int incBy) {
        MIST.getPrefs().setValue(prefName, MIST.getPrefs().getInt(prefName) + incBy);
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

        Display.getDefault().syncExec(() -> ErrorDialog.openError(null, title, msg, ms));
    }

}
