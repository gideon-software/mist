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

package com.github.tomhallman.mist.preferences.fieldeditors;

import static com.github.tomhallman.mist.util.ui.GridDataUtil.applyGridData;
import static com.github.tomhallman.mist.util.ui.GridDataUtil.onGridData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 *
 */
public class ButtonFieldEditor extends FieldEditor {
    private static Logger log = LogManager.getLogger();

    private Button button;

    public ButtonFieldEditor(String labelText, Composite parent) {
        super("", labelText, parent);
        log.trace("ButtonFieldEditor({},{})", labelText, parent);
    }

    @Override
    protected void adjustForNumColumns(int numColumns) {
        onGridData(button).horizontalSpan(numColumns - 1);
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        new Label(parent, SWT.NONE);

        button = new Button(parent, SWT.NONE);
        button.setText(getLabelText());
        applyGridData(button).horizontalAlignment(SWT.LEFT).horizontalSpan(numColumns - 1);
    }

    @Override
    protected void doLoad() {
    }

    @Override
    protected void doLoadDefault() {
    }

    @Override
    protected void doStore() {
    }

    public Button getButton() {
        return button;
    }

    @Override
    public int getNumberOfControls() {
        return 1;
    }
}
