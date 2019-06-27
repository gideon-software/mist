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
 * Buttons are provided for adding items to the list and removing
 * items from the list.
 * 
 * Inspired by https://www.eclipse.org/articles/Article-Field-Editors/field_editors.html
 */
public class AddRemoveListFieldEditor extends FieldEditor {
    private static Logger log = LogManager.getLogger();

    private static final String DEFAULT_ADD_LABEL = "&Add...";
    private static final String DEFAULT_REMOVE_LABEL = "&Remove";
    private static final String DEFAULT_ADD_DLG_TITLE = "Add";
    private static final String DEFAULT_ADD_DLG_MESSAGE = "Add item";
    private static final String DEFAULT_ADD_DLG_DESCRIPTION = "";
    private static final int DEFAULT_MIN_LIST_SIZE = 0;

    private static final int VERTICAL_DIALOG_UNITS_PER_CHAR = 8;
    private static final int LIST_HEIGHT_IN_CHARS = 4;
    private static final int LIST_HEIGHT_IN_DLUS = LIST_HEIGHT_IN_CHARS * VERTICAL_DIALOG_UNITS_PER_CHAR;

    private List stringList;
    private int minItemCount;
    private Button addButton;
    private Button removeButton;
    private String addDlgTitleStr;
    private String addDlgMessageStr;
    private String addDlgDescriptionStr;

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

    public AddRemoveListFieldEditor(String name, String labelText, Composite parent) {
        super(name, labelText, parent);
        log.trace("AddRemoveListFieldEditor({},{},{})", name, labelText, parent);
        addDlgTitleStr = DEFAULT_ADD_DLG_TITLE;
        addDlgMessageStr = DEFAULT_ADD_DLG_MESSAGE;
        addDlgDescriptionStr = DEFAULT_ADD_DLG_DESCRIPTION;
        minItemCount = DEFAULT_MIN_LIST_SIZE;
    }

    // Adds the string in the text field to the list.
    protected void addButtonSelected(String title, String msg, String description) {
        log.trace("{{}} addButtonSelected({},{},{})", stringList, title, msg, description);
        InputDialog dlg = new InputDialog(getPage().getShell(), title, msg, "", null);
        // dlg.setDescription(description);
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
    protected void adjustForNumColumns(int numColumns) {
        log.trace("{{}} adjustForNumColumns({})", stringList, numColumns);
        // We only grab excess space if we have to.
        // If another field editor has more columns then we assume it is setting the width.
        onGridData(stringList).horizontalSpan(numColumns - 2).grabExcessHorizontalSpace(numColumns - 2 == 1);
    }

    /**
     * Set the enablement of the remove button depending on the selection in the list.
     */
    private void checkRemoveButtonEnabled() {
        log.trace("{{}} checkRemoveButtonEnabled()", stringList);
        int index = stringList.getSelectionIndex();
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
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        log.trace("{{}} doFillIntoGrid({},{})", stringList, parent, numColumns);
        Label label = getLabelControl(parent);
        applyGridData(label).withVerticalFill();

        stringList = new List(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        int heightHint = convertVerticalDLUsToPixels(stringList, LIST_HEIGHT_IN_DLUS);
        applyGridData(stringList)
            .withHorizontalFill()
            .verticalAlignment(SWT.TOP)
            .heightHint(heightHint)
            .horizontalSpan(numColumns - 2);
        stringList.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                checkRemoveButtonEnabled();
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
            public void widgetSelected(SelectionEvent e) {
                addButtonSelected(addDlgTitleStr, addDlgMessageStr, addDlgDescriptionStr);
            }
        });

        // Create the remove button
        removeButton = new Button(buttonComp, SWT.NONE);
        applyGridData(removeButton).withHorizontalFill();
        removeButton.setEnabled(false);
        removeButton.setText(DEFAULT_REMOVE_LABEL);
        removeButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                removeButtonSelected();
            }
        });
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditor#doLoad()
     */
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
    protected void doStore() {
        log.trace("{{}} doStore()", stringList);
        if (curListString != null)
            getPreferenceStore().setValue(getPreferenceName(), curListString);
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
    public int getNumberOfControls() {
        return 3; // Label, list, buttons
    }

    /**
     * Sets the label for the button that adds the contents of the text field to the list.
     */
    public void setAddButtonText(String text) {
        addButton.setText(text);
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
        log.trace("{{}} refreshValidState()", stringList);
        boolean oldState = isValid;
        isValid = checkState();
        if (isValid != oldState) {
            log.trace("  calling fireStateChanged({},{},{})", IS_VALID, oldState, isValid);
            fireStateChanged(IS_VALID, oldState, isValid);
        }
    }

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
        checkRemoveButtonEnabled();
        valueChanged();
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

    @Override
    public void setEnabled(boolean enabled, Composite parent) {
        log.trace("{{}} setEnabled({},{})", stringList, enabled, parent);
        super.setEnabled(enabled, parent);
        if (stringList != null) {
            stringList.setEnabled(enabled);
            addButton.setEnabled(enabled);
            checkRemoveButtonEnabled();
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
