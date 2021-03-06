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

import static com.gideonsoftware.mist.util.ui.GridLayoutUtil.applyGridLayout;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.model.EmailModel;
import com.gideonsoftware.mist.model.MessageModel;
import com.gideonsoftware.mist.util.ui.Images;

public class ImportButtonView extends Composite implements PropertyChangeListener {
    private static Logger log = LogManager.getLogger();

    private Button importButton;

    public ImportButtonView(Composite parent) {
        super(parent, SWT.NONE);
        log.trace("ImportButtonView({})", parent);

        MessageModel.addPropertyChangeListener(this);
        EmailModel.addPropertyChangeListener(this);
        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                log.trace("ImportButtonView.widgetDisposed()");
                MessageModel.removePropertyChangeListener(ImportButtonView.this);
                EmailModel.removePropertyChangeListener(ImportButtonView.this);
            }
        });

        applyGridLayout(this).numColumns(1);

        // Create import button
        importButton = new Button(this, SWT.PUSH | SWT.CENTER);
        configureImportButton(false);
    }

    public void configureImportButton(boolean importing) {
        if (importButton.isDisposed())
            return;

        // Initial enabling depends on whether MIST is configured
        importButton.setEnabled(MIST.getPrefs().isConfigured());

        if (importing) {
            importButton.setText("&Stop Import");
            importButton.setImage(Images.getImage(Images.ICON_IMPORT_STOP));
        } else {
            importButton.setText("&Import Email");
            importButton.setImage(Images.getImage(Images.ICON_IMPORT_START));
        }

        // Force UI update (needed on Mac)
        importButton.update();
    }

    public Button getImportButton() {
        return importButton;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        log.trace("propertyChange({})", event);

        if (EmailModel.PROP_IMPORTING.equals(event.getPropertyName())
            || EmailModel.PROP_EMAILSERVERS_INIT.equals(event.getPropertyName())
            || MessageModel.PROP_MESSAGE_INIT.equals(event.getPropertyName())
            || MessageModel.PROP_MESSAGE_NEXT.equals(event.getPropertyName())) {
            if (Display.getDefault().isDisposed())
                return;
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    configureImportButton(EmailModel.isImporting() || MessageModel.getMessageCount() > 0);
                }
            });
        }
    }
}
