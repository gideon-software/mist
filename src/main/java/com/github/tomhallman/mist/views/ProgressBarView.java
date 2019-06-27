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

package com.github.tomhallman.mist.views;

import static com.github.tomhallman.mist.util.ui.GridDataUtil.applyGridData;
import static com.github.tomhallman.mist.util.ui.GridLayoutUtil.applyGridLayout;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;

import com.github.tomhallman.mist.model.EmailModel;
import com.github.tomhallman.mist.model.MessageModel;

public class ProgressBarView extends Composite implements PropertyChangeListener {
    private static Logger log = LogManager.getLogger();

    private ProgressBar progressBar;

    public ProgressBarView(Composite parent) {
        super(parent, SWT.NONE);
        log.trace("ProgressBarView({})", parent);
        MessageModel.addPropertyChangeListener(this);

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

        if (MessageModel.PROP_MESSAGE_ADD.equals(event.getPropertyName())) {
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    int total = EmailModel.getMessageCountTotal();
                    int current = EmailModel.getCurrentMessageNumberTotal();

                    if (progressBar.getSelection() == 0) {
                        progressBar.setMaximum(total);
                        log.debug("Setting progress bar maximum to {}", total);
                    }
                    progressBar.setSelection(current);
                }
            });
        }
    }
}
