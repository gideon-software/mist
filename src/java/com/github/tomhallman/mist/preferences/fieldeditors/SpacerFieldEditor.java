package com.github.tomhallman.mist.preferences.fieldeditors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.widgets.Composite;

/**
 * A field editor for adding space to a preference page.
 * 
 * @see https://www.eclipse.org/articles/Article-Field-Editors/field_editors.html
 */
public class SpacerFieldEditor extends LabelFieldEditor {
    private static Logger log = LogManager.getLogger();

    // Implemented as an empty label field editor.
    public SpacerFieldEditor(Composite parent) {
        super("", parent);
        log.trace("SpacerFieldEditor({})", parent);
    }
}
