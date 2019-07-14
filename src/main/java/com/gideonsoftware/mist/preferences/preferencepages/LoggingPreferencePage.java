/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2019 Gideon Software
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

package com.gideonsoftware.mist.preferences.preferencepages;

import java.io.File;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.program.Program;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.preferences.fieldeditors.ButtonFieldEditor;
import com.gideonsoftware.mist.preferences.fieldeditors.SmartComboFieldEditor;
import com.gideonsoftware.mist.preferences.fieldeditors.SpacerFieldEditor;
import com.gideonsoftware.mist.util.Util;
import com.gideonsoftware.mist.util.ui.Images;

/**
 *
 */
public class LoggingPreferencePage extends FieldEditorPreferencePage {
    private static Logger log = LogManager.getLogger();

    private SmartComboFieldEditor<String> levelEditor;
    private ButtonFieldEditor openFolderButton;

    public LoggingPreferencePage() {
        super(FieldEditorPreferencePage.GRID);
        log.trace("LoggingPreferencePage()");
        setTitle("Logging");
        setImageDescriptor(ImageDescriptor.createFromImage(Images.getImage(Images.ICON_LOG)));
        // setDescription("description here");
        noDefaultAndApplyButton();
    }

    @Override
    protected void createFieldEditors() {
        log.trace("createFieldEditors()");

        // Level
        MIST.getPrefs().setDefault(MIST.PREF_LOGFILE_LOGLEVEL, Level.WARN.name()); // Default
        levelEditor = new SmartComboFieldEditor<String>(
            MIST.PREF_LOGFILE_LOGLEVEL,
            "&Logging level:",
            getFieldEditorParent());
        levelEditor.setEmptySelectionAllowed(false);
        levelEditor.add(Level.OFF.name(), "OFF");
        levelEditor.add(Level.FATAL.name(), "FATAL");
        levelEditor.add(Level.ERROR.name(), "ERROR");
        levelEditor.add(Level.WARN.name(), "WARN");
        levelEditor.add(Level.INFO.name(), "INFO");
        levelEditor.add(Level.DEBUG.name(), "DEBUG");
        levelEditor.add(Level.TRACE.name(), "TRACE (may log sensitive data; slower)");
        addField(levelEditor);

        // Spacer
        addField(new SpacerFieldEditor(getFieldEditorParent()));

        // Open Folder button
        openFolderButton = new ButtonFieldEditor("Open Logfile Folder", getFieldEditorParent());
        openFolderButton.getButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                log.trace("openFolderButton.widgetSelected({})", e);
                String folderPath = new File(MIST.getLogfilePath()).getParent();
                if (!Program.launch(folderPath)) {
                    String msg = String.format(
                        "There was a problem loading your default file explorer. You can find the folder at '%s'",
                        folderPath);
                    Util.reportError("Unable to load folder in default file explorer", msg);
                }
            }
        });
        addField(openFolderButton);
    }

}
