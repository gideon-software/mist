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

package com.github.tomhallman.mist.util.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

class SmartComboData<T> {
    public T key;
    public String value;

    public SmartComboData(T key, String value) {
        this.key = key;
        this.value = value;
    }
}

/**
 * A combo box with easy reference to keys, values and indexes.
 */
public class SmartCombo<T> extends Combo {
    private static Logger log = LogManager.getLogger();

    // The data stored in this combo
    private List<SmartComboData<T>> data;

    @Override
    protected void checkSubclass() {
        // Override the disallowing of subclassing
    }

    /**
     * Default constructor.
     * 
     * @param parent
     *            Parent composite for this control to go into.
     * @param style
     *            Style for this control.
     */
    public SmartCombo(Composite parent, int style) {
        super(parent, style);
        log.trace("SmartCombo({},{})", parent, style);
        data = new ArrayList<SmartComboData<T>>();
    }

    /**
     * Adds a single key-value pair to the combo
     */
    public void add(T key, String value) {
        // TODO: Verify uniqueness of key
        data.add(new SmartComboData<T>(key, value));
        add(value);
    }

    /**
     * Gets the selected item's key, or null if nothing is selected.
     * 
     * @return The selected key
     */
    public T getSelectionKey() {
        if (getSelectionIndex() == -1)
            return null;
        return data.get(getSelectionIndex()).key;
    }

    /**
     * Gets the selected item's value, or null if nothing is selected.
     * 
     * @return The selected value
     */
    public String getSelectionValue() {
        if (getSelectionIndex() == -1)
            return null;
        return data.get(getSelectionIndex()).value;
    }

    /**
     * Removes all items from the combo.
     */
    public void removeAll() {
        super.removeAll();
        data.clear();
    }

    /**
     * Selects an item based on the specified key.
     * 
     * @param key
     *            The key to select the item by.
     */
    public void select(T key) {
        for (int i = 0; i < data.size(); i++) {
            if (key == null && data.get(i).key == null) { // null could be a key!
                super.select(i);
                return;
            } else if (data.get(i).key != null && data.get(i).key.equals(key)) {
                super.select(i);
                return;
            }
        }
    }

}
