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

package com.gideonsoftware.mist.util.ui;

import static com.gideonsoftware.mist.util.ui.GridDataUtil.applyGridData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.gideonsoftware.mist.model.data.PasswordData;

public class PasswordInputDialog extends InputDialog {
    private static Logger log = LogManager.getLogger();

    private Button savePasswordBox;
    private PasswordData passwordData = null;

    public PasswordInputDialog(
        Shell parentShell,
        String dialogTitle,
        String dialogMessage,
        String initialValue,
        IInputValidator validator) {
        super(parentShell, dialogTitle, dialogMessage, initialValue, validator);
        log.trace(
            "PasswordInputDialog({},{},{},{},{})",
            parentShell,
            dialogTitle,
            dialogMessage,
            initialValue,
            validator);
        passwordData = new PasswordData();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite comp = (Composite) super.createDialogArea(parent);

        savePasswordBox = new Button(comp, SWT.CHECK);
        savePasswordBox.setText("&Remember this password");
        applyGridData(savePasswordBox);

        return null;
    }

    @Override
    protected int getInputTextStyle() {
        return super.getInputTextStyle() | SWT.PASSWORD;
    }

    public PasswordData getPasswordData() {
        return passwordData; // Set when ok is pressed
    }

    @Override
    protected void okPressed() {
        // Store values
        passwordData.setPassword(getValue());
        passwordData.setSavePassword(savePasswordBox == null ? false : savePasswordBox.getSelection());
        super.okPressed();
    }
}
