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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;

import com.github.tomhallman.mist.util.ui.SmartCombo;

/**
 *
 */
public class SmartComboFieldEditor<T> extends FieldEditor {
    private static Logger log = LogManager.getLogger();

    /**
     * The smart combo field, or <code>null</code> if none.
     */
    private SmartCombo<T> combo;

    /**
     * Cached valid state
     */
    private boolean isValid = false;

    /**
     * The currently-selected key in the smart combo widget
     */
    private T curKey;

    /**
     * The error message, or <code>null</code> if none.
     */
    private String errorMessage = "Selection cannot be empty";

    /**
     * Indicates whether an empty selection is legal; <code>true</code> by default
     */
    private boolean emptySelectionAllowed = true;

    public SmartComboFieldEditor(String name, String labelText, Composite parent) {
        super(name, labelText, parent);
        log.trace("SmartComboFieldEditor({},{},{})", name, labelText, parent);
    }

    /**
     * Adds a single key-value pair to the combo.
     */
    public void add(T key, String value) {
        if (combo != null)
            combo.add(key, value);
    }

    @Override
    protected void adjustForNumColumns(int numColumns) {
        log.trace("{{}} adjustForNumColumns({})", combo, numColumns);
        // We only grab excess space if we have to.
        // If another field editor has more columns then we assume it is setting the width.
        onGridData(combo).horizontalSpan(numColumns - 1).grabExcessHorizontalSpace(numColumns - 1 == 1);
    }

    /**
     * Checks whether the text input field contains a valid value or not.
     *
     * @return <code>true</code> if the field selection is valid,
     *         and <code>false</code> if invalid
     */
    protected boolean checkState() {
        log.trace("{{}} checkState()", combo);
        boolean result;
        if (emptySelectionAllowed)
            result = true;
        else if (combo == null)
            result = false;
        else {
            // Note: This code may need to be rewritten if combo isn't set to READ_ONLY
            String str = combo.getSelectionValue();
            result = str != null;
        }

        log.trace("  result is {}", result);
        if (result)
            clearErrorMessage();
        else
            showErrorMessage();

        return result;
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        log.trace("{{}} doFillIntoGrid({},{})", combo, parent, numColumns);
        getLabelControl(parent);

        combo = getComboControl(parent);
        applyGridData(combo).withHorizontalFill();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doLoad() {
        log.trace("{{}} doLoad()", combo);
        if (combo != null) {
            curKey = (T) getPreferenceStore().getString(getPreferenceName());
            // This might cause problems if malformed; we're probably safe for our purposes
            try {
                combo.select(curKey);
            } catch (ClassCastException e) {
                log.error("Couldn't load property into SmartCombo", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doLoadDefault() {
        log.trace("{{}} doLoadDefault()", combo);
        if (combo != null) {
            // This might cause problems if malformed; we're probably safe for our purposes
            curKey = (T) getPreferenceStore().getDefaultString(getPreferenceName());
            combo.select(curKey);
        }
    }

    @Override
    protected void doStore() {
        log.trace("{{}} doStore()", combo);
        if (curKey == null) {
            getPreferenceStore().setToDefault(getPreferenceName());
            return;
        }
        getPreferenceStore().setValue(getPreferenceName(), curKey.toString());
    }

    /**
     * Returns this field editor's combo control.
     * 
     * @return the combo control or <code>null</code> if no combo field is created yet
     */
    protected SmartCombo<T> getComboControl() {
        log.trace("{{}} getComboControl()", combo);
        return combo;
    }

    /**
     * Returns this field editor's combo control.
     * <p>
     * The control is created if it does not yet exist
     * </p>
     *
     * @param parent
     *            the parent
     * @return the combo control
     */
    public SmartCombo<T> getComboControl(Composite parent) {
        // TODO: This method probably shouldn't be exposed publicly
        log.trace("{{}} getComboControl({})", combo, parent);
        if (combo == null) {
            combo = new SmartCombo<T>(parent, SWT.BORDER | SWT.READ_ONLY);
            combo.addSelectionListener(new SelectionListener() {
                public void widgetSelected(SelectionEvent event) {
                    log.trace("{{}} widgetSelected({})", combo, event);
                    selected(event);
                }

                public void widgetDefaultSelected(SelectionEvent event) {
                    log.trace("{{}} widgetDefaultSelected({})", combo, event);
                    selected(event);
                }

                private void selected(SelectionEvent event) {
                    valueChanged();
                }
            });
            combo.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) {
                    refreshValidState();
                }
            });
            combo.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    log.trace("{{}} widgetDisposed({})", combo, event);
                    combo = null;
                }
            });
        } else {
            checkParent(combo, parent);
        }
        return combo;
    }

    @Override
    public int getNumberOfControls() {
        return 2; // Label + combo
    }

    /**
     * Gets the selected item's key, or null if nothing is selected.
     * 
     * @return The selected key
     */
    public T getSelectionKey() {
        return combo == null ? null : combo.getSelectionKey();
    }

    /**
     * Returns whether an empty selection is valid or not.
     *
     * @return true if an empty selection is a valid value, and
     *         false if an empty selection is invalid
     */
    public boolean isEmptySelectionAllowed() {
        return emptySelectionAllowed;
    }

    /*
     * (non-Javadoc)
     * Method declared on FieldEditor.
     * Returns whether this field editor contains a valid value.
     */
    public boolean isValid() {
        return isValid;
    }

    /*
     * (non-Javadoc)
     * Method declared on FieldEditor.
     * Refreshes this field editor's valid state after a value change
     * and fires an <code>IS_VALID</code> property change event if
     * warranted.
     */
    protected void refreshValidState() {
        log.trace("{{}} refreshValidState()", combo);
        boolean oldState = isValid;
        isValid = checkState();
        if (isValid != oldState) {
            log.trace("  calling fireStateChanged({},{},{})", IS_VALID, oldState, isValid);
            fireStateChanged(IS_VALID, oldState, isValid);
        }
    }

    /**
     * Removes all items from the combo.
     */
    public void removeAll() {
        if (combo != null)
            combo.removeAll();
    }

    /**
     * Sets whether the empty selection is a valid value or not.
     *
     * @param allowed
     *            <code>true</code> if the empty selection is allowed,
     *            and <code>false</code> if it is considered invalid
     */
    public void setEmptySelectionAllowed(boolean allowed) {
        emptySelectionAllowed = allowed;
    }

    @Override
    public void setEnabled(boolean enabled, Composite parent) {
        log.trace("{{}} setEnabled({},{})", combo, enabled, parent);
        super.setEnabled(enabled, parent);
        getComboControl(parent).setEnabled(enabled);
    }

    /**
     * Sets the error message that will be displayed when and if an error occurs.
     *
     * @param errorMessage
     *            the error message
     */
    public void setErrorMessage(String errorMessage) {
        log.trace("{{}} setErrorMessage({})", combo, errorMessage);
        this.errorMessage = errorMessage;
    }

    @Override
    public void setFocus() {
        log.trace("{{}} setFocus()", combo);
        if (combo != null)
            combo.setFocus();
    }

    /**
     * Set this field editor's key
     * 
     * @param key
     *            the key to select; if key is not found, no selection change is made
     */
    public void setSelection(T key) {
        log.trace("{{}} setSelection({})", combo, key);
        if (combo != null) {
            curKey = combo.getSelectionKey();
            combo.select(key);
            valueChanged();
        }
    }

    /**
     * Shows the error message set via <code>setErrorMessage</code>.
     */
    public void showErrorMessage() {
        log.trace("{{}} showErrorMessage()", combo);
        showErrorMessage(errorMessage);
    }

    /**
     * Informs this field editor's listener, if it has one, about a change
     * to the value (<code>VALUE</code> property) provided that the old and
     * new values are different.
     * <p>
     * This hook is <em>not</em> called when the text is initialized
     * (or reset to the default value) from the preference store.
     * </p>
     */
    protected void valueChanged() {
        log.trace("{{}} valueChanged()", combo);
        setPresentsDefaultValue(false);
        refreshValidState();

        T newKey = combo.getSelectionKey();
        if (newKey != null && !newKey.equals(curKey)) {
            log.trace("  calling fireValueChanged({},{},{})", VALUE, curKey, newKey);
            fireValueChanged(VALUE, curKey, newKey);
            curKey = newKey;
        }
    }
}
