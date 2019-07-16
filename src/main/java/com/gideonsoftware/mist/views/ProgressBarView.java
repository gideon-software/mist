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

import static com.gideonsoftware.mist.util.ui.GridDataUtil.applyGridData;
import static com.gideonsoftware.mist.util.ui.GridLayoutUtil.applyGridLayout;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;

import com.gideonsoftware.mist.model.EmailModel;
import com.gideonsoftware.mist.model.MessageModel;

public class ProgressBarView extends Composite implements PropertyChangeListener {
    private static Logger log = LogManager.getLogger();

    private ProgressBar progressBar;

    public ProgressBarView(Composite parent) {
        super(parent, SWT.NONE);
        log.trace("ProgressBarView({})", parent);

        MessageModel.addPropertyChangeListener(this);
        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                log.trace("ProgressBarView.widgetDisposed()");
                MessageModel.removePropertyChangeListener(ProgressBarView.this);
            }
        });

        applyGridLayout(this).numColumns(1);
        applyGridData(this).withHorizontalFill();

        // Create progress bar that spans length of the space
        progressBar = new ProgressBar(this, SWT.SMOOTH);
        applyGridData(progressBar).withHorizontalFill();
        progressBar.setMinimum(0);
        progressBar.setMaximum(1); // The minimum maximum
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        log.trace("propertyChange({})", event);

        if (MessageModel.PROP_MESSAGE_ADD.equals(event.getPropertyName())
            || MessageModel.PROP_MESSAGE_INIT.equals(event.getPropertyName())) {
            if (Display.getDefault().isDisposed())
                return;
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    int total = EmailModel.getMessageCountTotal();
                    int current = EmailModel.getCurrentMessageNumberTotal();
                    if (!progressBar.isDisposed()) {
                        progressBar.setMaximum(total);
                        progressBar.setSelection(current);
                    }
                }
            });
        }
    }
}
