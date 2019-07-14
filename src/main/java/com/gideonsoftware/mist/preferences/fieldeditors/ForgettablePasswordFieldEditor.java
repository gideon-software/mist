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

package com.gideonsoftware.mist.preferences.fieldeditors;

import static com.gideonsoftware.mist.util.ui.GridDataUtil.applyGridData;
import static com.gideonsoftware.mist.util.ui.GridDataUtil.onGridData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 *
 */
public class ForgettablePasswordFieldEditor extends FieldEditor {
    private static Logger log = LogManager.getLogger();

    private static final String DEFAULT_SAVED_TEXT = "<saved>";
    private static final String DEFAULT_PROMPT_TEXT = "<prompt>";
    private static final String DEFAULT_FORGET_TEXT = "&Forget";

    private String promptPrefName;
    private boolean isPrompt;
    private String password;

    private Label passwordLabel;
    private Button forgetButton;

    private String savedStr;
    private String promptStr;
    private String forgetStr;

    public ForgettablePasswordFieldEditor(String name, String promptPrefName, String labelText, Composite parent) {
        super(name, labelText, parent);
        log.trace("ForgettablePasswordFieldEditor({},{},{},{})", name, promptPrefName, labelText, parent);
        this.promptPrefName = promptPrefName;
        savedStr = DEFAULT_SAVED_TEXT;
        promptStr = DEFAULT_PROMPT_TEXT;
        forgetStr = DEFAULT_FORGET_TEXT;
    }

    @Override
    protected void adjustForNumColumns(int numColumns) {
        onGridData(passwordLabel).horizontalSpan(numColumns - 2);
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        getLabelControl(parent);

        passwordLabel = new Label(parent, SWT.NONE);
        applyGridData(passwordLabel).horizontalSpan(numColumns - 1).withHorizontalFill();

        forgetButton = new Button(parent, SWT.NONE);
        applyGridData(forgetButton).horizontalAlignment(SWT.FILL);
        forgetButton.setText(DEFAULT_FORGET_TEXT);
        forgetButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                setPrompt(true);
            }
        });
    }

    @Override
    protected void doLoad() {
        password = getPreferenceStore().getString(getPreferenceName());
        // If the value is not found, it defaults to false. We want true, though. So let's check!
        if (getPreferenceStore().contains(promptPrefName))
            isPrompt = getPreferenceStore().getBoolean(promptPrefName);
        else
            isPrompt = true;
        setPrompt(isPrompt);
    }

    @Override
    protected void doLoadDefault() {
        password = getPreferenceStore().getDefaultString(getPreferenceName());
        isPrompt = getPreferenceStore().getDefaultBoolean(promptPrefName);
        setPrompt(isPrompt);
    }

    @Override
    protected void doStore() {
        getPreferenceStore().setValue(promptPrefName, isPrompt);
        getPreferenceStore().setValue(getPreferenceName(), password);
    }

    @Override
    public int getNumberOfControls() {
        return 3; // Label, password label and "Forget" button
    }

    public boolean isPrompt() {
        return isPrompt;
    }

    /**
     * @return the forget button text
     */
    public String getForgetText() {
        return forgetStr;
    }

    public String getPassword() {
        return password;
    }

    /**
     * @param forgetStr
     *            the forget button text to set
     */
    public void setForgetText(String forgetText) {
        this.forgetStr = forgetText;
    }

    public void setPrompt(boolean isPrompt) {
        this.isPrompt = isPrompt;
        if (isPrompt) {
            passwordLabel.setText(promptStr);
            forgetButton.setEnabled(false);
            password = getPreferenceStore().getDefaultString(getPreferenceName());
            getPreferenceStore().setToDefault(getPreferenceName());
        } else {
            passwordLabel.setText(savedStr);
            forgetButton.setEnabled(true);
        }
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
