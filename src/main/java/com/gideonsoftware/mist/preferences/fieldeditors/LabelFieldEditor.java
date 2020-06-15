package com.gideonsoftware.mist.preferences.fieldeditors;

import static com.gideonsoftware.mist.util.ui.GridDataUtil.applyGridData;
import static com.gideonsoftware.mist.util.ui.GridDataUtil.onGridData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * A field editor for displaying labels not associated with other widgets.
 * 
 * @see https://www.eclipse.org/articles/Article-Field-Editors/field_editors.html
 */
public class LabelFieldEditor extends FieldEditor {
    private static Logger log = LogManager.getLogger();

    private Label label;

    // All labels can use the same preference name since they don't
    // store any preference.
    public LabelFieldEditor(String value, Composite parent) {
        super("label", value, parent);
        log.trace("LabelFieldEditor({},{})", value, parent);
    }

    // Adjusts the field editor to be displayed correctly
    // for the given number of columns.
    @Override
    protected void adjustForNumColumns(int numColumns) {
        onGridData(label).horizontalSpan(numColumns);
    }

    // Fills the field editor's controls into the given parent.
    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        label = getLabelControl(parent);

        applyGridData(label).horizontalSpan(numColumns).horizontalAlignment(SWT.FILL).verticalAlignment(SWT.CENTER);
        // grabExcessHorizontalSpace = false;
        // grabExcessVerticalSpace = false;
    }

    // Labels do not persist any preferences, so these methods are empty.
    @Override
    protected void doLoad() {
    }

    @Override
    protected void doLoadDefault() {
    }

    @Override
    protected void doStore() {
    }

    // Returns the number of controls in the field editor.
    @Override
    public int getNumberOfControls() {
        return 1;
    }
}
