package com.github.tomhallman.mist.preferences.fieldeditors;

import static com.github.tomhallman.mist.util.ui.GridDataUtil.applyGridData;
import static com.github.tomhallman.mist.util.ui.GridDataUtil.onGridData;
import static com.github.tomhallman.mist.util.ui.GridLayoutUtil.applyGridLayout;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;

import com.github.tomhallman.mist.preferences.Preferences;

/**
 * A field editor for displaying and storing a list of strings.
 * Buttons are provided for adding items to the list, editing items in the list, and removing
 * items from the list.
 * 
 * Inspired by https://www.eclipse.org/articles/Article-Field-Editors/field_editors.html
 */
public class AddEditRemoveListFieldEditor extends FieldEditor {
    private static Logger log = LogManager.getLogger();

    private static final String DEFAULT_ADD_LABEL = "&Add...";
    private static final String DEFAULT_EDIT_LABEL = "&Edit...";
    private static final String DEFAULT_REMOVE_LABEL = "&Remove";
    private static final String DEFAULT_ADD_DLG_TITLE = "Add";
    private static final String DEFAULT_ADD_DLG_MESSAGE = "Add item";
    private static final String DEFAULT_ADD_DLG_DESCRIPTION = "";
    private static final String DEFAULT_EDIT_DLG_TITLE = "Edit";
    private static final String DEFAULT_EDIT_DLG_MESSAGE = "Update item";
    private static final String DEFAULT_EDIT_DLG_DESCRIPTION = "";
    private static final int DEFAULT_MIN_LIST_SIZE = 0;

    private static final int VERTICAL_DIALOG_UNITS_PER_CHAR = 8;
    private static final int LIST_HEIGHT_IN_CHARS = 6;
    private static final int LIST_HEIGHT_IN_DLUS = LIST_HEIGHT_IN_CHARS * VERTICAL_DIALOG_UNITS_PER_CHAR;

    private List stringList;
    private int minItemCount;
    private Button addButton;
    private Button editButton;
    private Button removeButton;
    private String addDlgTitleStr;
    private String addDlgMessageStr;
    private String addDlgDescriptionStr;
    private String editDlgTitleStr;
    private String editDlgMessageStr;
    private String editDlgDescriptionStr;

    /**
     * Current list in string format
     */
    private String curListString = "";

    /**
     * Cached valid state
     */
    private boolean isValid = false;

    /**
     * The error message, or <code>null</code> if none.
     */
    private String errorMessage = "List contains too few items";

    public AddEditRemoveListFieldEditor(String name, String labelText, Composite parent) {
        super(name, labelText, parent);
        log.trace("AddEditRemoveListFieldEditor({},{},{})", name, labelText, parent);
        addDlgTitleStr = DEFAULT_ADD_DLG_TITLE;
        addDlgMessageStr = DEFAULT_ADD_DLG_MESSAGE;
        addDlgDescriptionStr = DEFAULT_ADD_DLG_DESCRIPTION;
        editDlgTitleStr = DEFAULT_EDIT_DLG_TITLE;
        editDlgMessageStr = DEFAULT_EDIT_DLG_MESSAGE;
        editDlgDescriptionStr = DEFAULT_EDIT_DLG_DESCRIPTION;
        minItemCount = DEFAULT_MIN_LIST_SIZE;
    }

    /**
     * Adds the string in the text field to the list.
     */
    protected void addButtonSelected() {
        log.trace("{{}} addButtonSelected()", stringList);
        InputDialog dlg = new InputDialog(getPage().getShell(), addDlgTitleStr, addDlgMessageStr, "", null);
        // TODO: dlg.setDescription(addDlgDescriptionStr);
        int result = dlg.open();
        if (result == Window.OK && !dlg.getValue().isEmpty()) {
            curListString = getListString();
            stringList.add(dlg.getValue());
            valueChanged();
        }
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditor#adjustForNumColumns(int)
     */
    @Override
    protected void adjustForNumColumns(int numColumns) {
        log.trace("{{}} adjustForNumColumns({})", stringList, numColumns);
        // We only grab excess space if we have to.
        // If another field editor has more columns then we assume it is setting the width.
        onGridData(stringList).horizontalSpan(numColumns - 2).grabExcessHorizontalSpace(numColumns - 2 == 1);
    }

    /**
     * Set the enablement of the edit and remove buttons depending on the selection in the list.
     */
    private void checkEditRemoveButtonsEnabled() {
        log.trace("{{}} checkEditRemoveButtonsEnabled()", stringList);
        int index = stringList.getSelectionIndex();
        editButton.setEnabled(index >= 0);
        removeButton.setEnabled(index >= 0);
    }

    /**
     * Checks whether the list is long enough
     *
     * @return <code>true</code> if the list is long enough
     *         and <code>false</code> if not
     */
    protected boolean checkState() {
        log.trace("{{}} checkState()", stringList);
        boolean result;
        if (stringList == null)
            result = false;
        else
            result = stringList.getItemCount() >= minItemCount;

        log.trace("  result is {}", result);
        if (result)
            clearErrorMessage();
        else
            showErrorMessage();
        return result;
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditor#doFillIntoGrid(Composite, int)
     */
    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        log.trace("{{}} doFillIntoGrid({},{})", stringList, parent, numColumns);
        Label label = getLabelControl(parent);
        applyGridData(label).withVerticalFill();

        stringList = new List(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        int heightHint = convertVerticalDLUsToPixels(stringList, LIST_HEIGHT_IN_DLUS);
        applyGridData(stringList).withHorizontalFill().verticalAlignment(SWT.TOP).heightHint(heightHint).horizontalSpan(
            numColumns - 2);
        stringList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                checkEditRemoveButtonsEnabled();
                refreshValidState();
            }
        });

        // Create a composite for the add and remove buttons
        Composite buttonComp = new Composite(parent, SWT.NONE);
        applyGridData(buttonComp).horizontalAlignment(SWT.FILL).verticalAlignment(SWT.TOP);
        applyGridLayout(buttonComp).marginHeight(0).marginWidth(0);

        // Create the add button
        addButton = new Button(buttonComp, SWT.NONE);
        applyGridData(addButton).withHorizontalFill();
        addButton.setText(DEFAULT_ADD_LABEL);
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addButtonSelected();
            }
        });

        // Create the edit button
        editButton = new Button(buttonComp, SWT.NONE);
        applyGridData(editButton).withHorizontalFill();
        editButton.setText(DEFAULT_EDIT_LABEL);
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                editButtonSelected();
            }
        });

        // Create the remove button
        removeButton = new Button(buttonComp, SWT.NONE);
        applyGridData(removeButton).withHorizontalFill();
        removeButton.setEnabled(false);
        removeButton.setText(DEFAULT_REMOVE_LABEL);
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                removeButtonSelected();
            }
        });
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditor#doLoad()
     */
    @Override
    protected void doLoad() {
        log.trace("{{}} doLoad()", stringList);
        if (stringList != null) {
            String items = getPreferenceStore().getString(getPreferenceName());
            setList(items);
            curListString = getListString();
        }
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditor#doLoadDefault()
     */
    @Override
    protected void doLoadDefault() {
        log.trace("{{}} doLoadDefault()", stringList);
        if (stringList != null) {
            String items = getPreferenceStore().getDefaultString(getPreferenceName());
            setList(items);
            curListString = getListString();
        }
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditor#doStore()
     */
    @Override
    protected void doStore() {
        log.trace("{{}} doStore()", stringList);
        if (curListString != null)
            getPreferenceStore().setValue(getPreferenceName(), curListString);
    }

    /**
     * @param title
     * @param msg
     * @param description
     */
    protected void editButtonSelected() {
        log.trace("{{}} editButtonSelected({},{},{})", stringList);
        InputDialog dlg = new InputDialog(
            getPage().getShell(),
            editDlgTitleStr,
            editDlgMessageStr,
            stringList.getItem(stringList.getSelectionIndex()),
            null);
        // TODO: dlg.setDescription(editDlgDescriptionStr);
        int result = dlg.open();
        if (result == Window.OK && !dlg.getValue().isEmpty()) {
            curListString = getListString();
            stringList.setItem(stringList.getSelectionIndex(), dlg.getValue());
            valueChanged();
        }
    }

    public String getAddDialogDescription() {
        return addDlgDescriptionStr;
    }

    public String getAddDialogMessage() {
        return addDlgMessageStr;
    }

    public String getAddDialogTitle() {
        return addDlgTitleStr;
    }

    public String getEditDialogDescription() {
        return editDlgDescriptionStr;
    }

    public String getEditDialogMessage() {
        return editDlgMessageStr;
    }

    public String getEditDialogTitle() {
        return editDlgTitleStr;
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    private String getListString() {
        if (stringList == null)
            return "";
        else
            return StringUtils.join(stringList.getItems(), Preferences.getSeparator());
    }

    /**
     * @return the minItemCount
     */
    public int getMinItemCount() {
        return minItemCount;
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditor#getNumberOfControls()
     */
    @Override
    public int getNumberOfControls() {
        return 3; // Label, list, buttons
    }

    /*
     * (non-Javadoc)
     * Method declared on FieldEditor.
     * Returns whether this field editor contains a valid value.
     */
    @Override
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
    @Override
    protected void refreshValidState() {
        log.trace("{{}} refreshValidState()", stringList);
        boolean oldState = isValid;
        isValid = checkState();
        if (isValid != oldState) {
            log.trace("  calling fireStateChanged({},{},{})", IS_VALID, oldState, isValid);
            fireStateChanged(IS_VALID, oldState, isValid);
        }
    }

    /**
     * 
     */
    protected void removeButtonSelected() {
        log.trace("{{}} removeButtonSelected()", stringList);
        int index = stringList.getSelectionIndex();
        stringList.remove(index);
        if (stringList.getItemCount() > 0) {
            if (index < stringList.getItemCount())
                stringList.select(index);
            else
                stringList.select(index - 1);
            stringList.setFocus();
            curListString = getListString();
        }
        checkEditRemoveButtonsEnabled();
        valueChanged();
    }

    /**
     * Sets the label for the button that adds the contents of the text field to the list.
     */
    public void setAddButtonText(String text) {
        addButton.setText(text);
    }

    public void setAddDialogDescription(String description) {
        this.addDlgDescriptionStr = description;
    }

    public void setAddDialogMessage(String msg) {
        this.addDlgMessageStr = msg;
    }

    public void setAddDialogTitle(String title) {
        this.addDlgTitleStr = title;
    }

    /**
     * Sets the label for the button that edits the selected item in the list.
     */
    public void setEditButtonText(String text) {
        editButton.setText(text);
    }

    public void setEditDialogDescription(String description) {
        this.editDlgDescriptionStr = description;
    }

    public void setEditDialogMessage(String msg) {
        this.editDlgMessageStr = msg;
    }

    public void setEditDialogTitle(String title) {
        this.editDlgTitleStr = title;
    }

    @Override
    public void setEnabled(boolean enabled, Composite parent) {
        log.trace("{{}} setEnabled({},{})", stringList, enabled, parent);
        super.setEnabled(enabled, parent);
        if (stringList != null) {
            stringList.setEnabled(enabled);
            addButton.setEnabled(enabled);
            checkEditRemoveButtonsEnabled();
        }
    }

    /**
     * @param errorMessage
     *            the errorMessage to set
     */
    public void setErrorMessage(String errorMessage) {
        log.trace("{{}} setErrorMessage({})", stringList, errorMessage);
        this.errorMessage = errorMessage;
    }

    @Override
    public void setFocus() {
        log.trace("{{}} setFocus()", stringList);
        if (stringList != null)
            stringList.setFocus();
    }

    // Parses the string into separate list items and adds them to the list.
    private void setList(String items) {
        log.trace("{{}} setList({})", stringList, items);
        String[] itemArray = StringUtils.split(items, Preferences.getSeparator());
        stringList.setItems(itemArray);
    }

    /**
     * @param minListSize
     *            the minListSize to set
     */
    public void setMinListSize(int minListSize) {
        log.trace("{{}} setMinListSize({})", stringList, minListSize);
        this.minItemCount = minListSize;
    }

    /**
     * Sets the label for the button that removes the selected item from the list.
     */
    public void setRemoveButtonText(String text) {
        removeButton.setText(text);
    }

    /**
     * Shows the error message set via <code>setErrorMessage</code>.
     */
    public void showErrorMessage() {
        log.trace("{{}} showErrorMessage()", stringList);
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
        log.trace("{{}} valueChanged()", stringList);
        setPresentsDefaultValue(false);

        refreshValidState();

        String newListString = getListString();
        if (!newListString.equals(curListString)) {
            log.trace("  calling fireValueChanged({},{},{})", VALUE, curListString, newListString);
            fireValueChanged(VALUE, curListString, newListString);
            curListString = newListString;
        }
    }
}
