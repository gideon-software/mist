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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

/**
 * A smart combo box with these features:
 * <ul>
 * <li>auto-selecting of items as the user types in the text box</li>
 * <li>can return a templated item ({@link getSelectedItem()} ) instead of only indexes</i>
 * </ul>
 */
public class SmartCombo<T> extends Composite {
    private static Logger log = LogManager.getLogger();

    private List<T> items = new ArrayList<T>();

    /**
     * The underlying combo box
     */
    protected Combo combo;

    /**
     * Create a new SmartCombo.
     * 
     * @param parent
     *            Parent composite for this control to go into.
     * @param style
     *            Style for this control.
     */
    public SmartCombo(Composite parent, int style) {
        super(parent, SWT.NULL);
        log.trace("SmartCombo({},{})", parent, style);

        setLayout(new FillLayout());

        combo = new Combo(this, style);

        // Add the key listener to this adapter to give the functionality
        // of hopping to the first entry that matches the character typed
        combo.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent event) {
                smartComboKeyReleased(event);
            }
        });
    }

    /**
     * Adds a single entry to the combo. Note: items must be unique.
     * 
     * @param item
     *            the item to add
     * @param value
     *            the value to add (which shows up in the combo)
     * @return true if the value was added successfully; false otherwise
     */
    public boolean add(T item, String value) {
        // log.trace("add({},{})", item, value);
        // items must be unique
        if (items.contains(item))
            return false;

        if (items.add(item)) {
            combo.add(value);
            return true;
        }
        return false;
    }

    @Override
    public void addFocusListener(FocusListener listener) {
        combo.addFocusListener(listener);
    }

    public void addModifyListener(ModifyListener listener) {
        combo.addModifyListener(listener);
    }

    public void addSelectionListener(SelectionListener listener) {
        combo.addSelectionListener(listener);
    }

    /**
     * Try to autocomplete the text selection based on the available list items
     * 
     * @return true if autocomplete was successful (a match was found), false otherwise
     */
    public boolean autoComplete() {
        // log.trace("autoComplete()");
        // Get the lowercase version of the text
        String lowerText = combo.getText().toLowerCase();

        // Search the list of items for the typed text
        for (int i = 0; i < items.size(); i++) {
            if (combo.getItem(i).toLowerCase().startsWith(lowerText)) {
                // If found, select it from the list
                combo.select(i);
                // And highlight the rest of the text
                combo.setSelection(new Point(lowerText.length(), combo.getItem(i).length()));
                // Then break from the for loop
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean getEnabled() {
        return combo.getEnabled();
    }

    public Map<T, String> getItemsMap() {
        // log.trace("getItemsMap()");
        Map<T, String> map = new HashMap<T, String>();
        for (int i = 0; i < items.size(); i++)
            map.put(items.get(i), combo.getItem(i));
        return map;
    }

    public int getSelectionIndex() {
        return combo.getSelectionIndex();
    }

    /**
     * Gets the selected item from the combo.
     * 
     * @return The selected item
     */
    public T getSelectionItem() {
        // log.trace("getSelectionItem()");
        int selectionIndex = getSelectionIndex();
        if (selectionIndex != -1)
            return items.get(selectionIndex);
        return null;
    }

    public String getSelectionValue() {
        // log.trace("getSelectionValue()");
        int selectionIndex = getSelectionIndex();
        if (selectionIndex != -1)
            return combo.getItem(selectionIndex);
        return null;
    }

    public String getText() {
        return combo.getText();
    }

    public void removeAll() {
        combo.removeAll();
        items = new ArrayList<T>();
    }

    /**
     * Selects an item in the combo.
     * 
     * @param item
     *            the item to select
     * @return whether an item was selected
     */
    public boolean select(T item) {
        log.trace("select([{}|{}])", getSelectionItem(), getSelectionValue());
        for (int i = 0; i < items.size(); i++) {
            // Special case of null item
            if (item == null && items.get(i) == null) {
                combo.select(i);
                return true;
            }
            if (item != null && item.equals(items.get(i))) {
                combo.select(i);
                return true;
            }
        }
        return false;

    }

    @Override
    public void setEnabled(boolean enabled) {
        combo.setEnabled(enabled);
    }

    public void setSelection(Point point) {
        combo.setSelection(point);
    }

    public void setText(String text) {
        combo.setText(text);
        autoComplete();
    }

    /**
     * Handles the event that a key is pressed in the combo.
     * Performs a search as the user is typing, highlighting as it matches.
     */
    protected void smartComboKeyReleased(KeyEvent e) {
        // log.trace("smartComboKeyReleased({})", e);

        if ((e.character >= 'a' && e.character <= 'z')
            || (e.character >= 'A' && e.character <= 'Z')
            || (e.character >= '0' && e.character <= '9')
            || (e.character == ' ')
            || (e.character == '\b') // Backspace
        ) {

            // If it's a backspace, just return (it shouldn't change the selection)
            if (e.character == '\b')
                return;

            autoComplete();
        }
    }

    @Override
    public String toString() {
        return String.format("SmartCombo [%s|%s]", items.size(), getSelectionValue());
    }
}
