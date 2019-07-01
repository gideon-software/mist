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

import static com.github.tomhallman.mist.util.ui.GridLayoutUtil.applyGridLayout;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.model.EmailModel;
import com.github.tomhallman.mist.util.ui.Images;

public class ImportButtonView extends Composite implements PropertyChangeListener {
    private static Logger log = LogManager.getLogger();

    private Button importButton;

    public ImportButtonView(Composite parent) {
        super(parent, SWT.NONE);
        log.trace("ImportButtonView({})", parent);
        EmailModel.addPropertyChangeListener(this);

        applyGridLayout(this).numColumns(1);

        // Create import button
        importButton = new Button(this, SWT.PUSH | SWT.CENTER);
        importButton.setText("&Import Email");
        importButton.setImage(Images.getImage(Images.ICON_IMPORT_START));

        // Initial state depends on whether MIST is configured
        importButton.setEnabled(MIST.getPrefs().isConfigured());
    }

    public Button getImportButton() {
        return importButton;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        log.trace("propertyChange({})", event);

        if (EmailModel.PROP_IMPORTING.equals(event.getPropertyName())) {
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    boolean importing = (Boolean) event.getNewValue();
                    importButton.setEnabled(!importing);
                }
            });
        }

    }
}
