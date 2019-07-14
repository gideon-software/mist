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

package com.gideonsoftware.mist.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TaskBar;
import org.eclipse.swt.widgets.TaskItem;

import com.gideonsoftware.mist.model.EmailModel;
import com.gideonsoftware.mist.model.MessageModel;

public class TaskItemView implements PropertyChangeListener {
    private static Logger log = LogManager.getLogger();

    private TaskItem taskItem = null;

    public TaskItemView(Shell shell) {
        log.trace("TaskItemView({})", shell);

        // TODO: Still a problem?
        // There's a problem with TaskItem on MacOS that I've been unable to fix.
        // See http://eclipsesource.com/blogs/2012/11/06/eclipse-swt-throws-an-npe-in-taskitem-on-macos/
        if (Util.isMac())
            return;

        MessageModel.addPropertyChangeListener(this);

        TaskBar bar = shell.getDisplay().getSystemTaskBar();
        if (bar == null)
            return;
        taskItem = bar.getItem(shell);
        if (taskItem == null)
            taskItem = bar.getItem(null);

        if (taskItem != null) {
            // Configure it
            taskItem.setProgressState(SWT.DEFAULT);
        }
    }

    public TaskItem getTaskItem() {
        return taskItem;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        log.trace("propertyChange({})", event);

        if (MessageModel.PROP_MESSAGE_ADD.equals(event.getPropertyName())
            || MessageModel.PROP_MESSAGE_INIT.equals(event.getPropertyName())) {
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    if (taskItem != null) {
                        int total = EmailModel.getMessageCountTotal();
                        int current = EmailModel.getCurrentMessageNumberTotal();
                        if (current < total) {
                            taskItem.setProgress(current * 100 / total);
                            taskItem.setProgressState(SWT.NORMAL);
                        } else {
                            // Reset
                            taskItem.setProgressState(SWT.DEFAULT);
                        }
                    }
                }
            });
        }
    }
}
