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

package com.github.tomhallman.mist.preferences.preferencepages;

import static com.github.tomhallman.mist.util.ui.GridDataUtil.applyGridData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Text;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.model.EmailModel;
import com.github.tomhallman.mist.tntapi.TntDb;
import com.github.tomhallman.mist.util.Util;
import com.github.tomhallman.mist.util.ui.ViewUtil;

/**
 *
 */
public class TntDbPreferencePage extends FieldEditorPreferencePage {
    private static Logger log = LogManager.getLogger();

    /**
     * The "original" path once this page is loaded. If the dialog is canceled, this is restored.
     */
    private String originalDbPath;

    private FileFieldEditor tntDbPath;

    public TntDbPreferencePage() {
        super(FieldEditorPreferencePage.GRID);
        log.trace("TntDbPreferencePage()");
        setTitle("TntConnect");
        // setDescription("description here");
        noDefaultAndApplyButton();
        originalDbPath = MIST.getPrefs().getString(TntDb.PREF_TNT_DBPATH);
    }

    @Override
    protected void createFieldEditors() {
        log.trace("createFieldEditors()");
        tntDbPath = new FileFieldEditor(TntDb.PREF_TNT_DBPATH, "&TntConnect Database:", true, getFieldEditorParent());
        tntDbPath.setEmptyStringAllowed(false);
        tntDbPath.setFileExtensions(new String[] { "*.mpddb", "*.*" });
        Text control = tntDbPath.getTextControl(getFieldEditorParent());
        control.setEditable(false);
        // Limit text size so the path doesn't make a huge preference dialog
        applyGridData(control).widthHint(ViewUtil.getTextWidth(control) * 20).withHorizontalFill();
        // Set selection to end, so we see the actual DB name in a long path
        // TODO: Trim name to include ellipses instead (use PrefDlg.ShortenText)
        control.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                Text text = (Text) event.widget;
                text.setSelection(text.getText().length());
            }
        });
        // Make an effort to show the whole path name if there's enough space
        control.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                control.setSelection(control.getText().length());
            }
        });
        addField(tntDbPath);

    }

    @Override
    public boolean performCancel() {
        TntDb.setTntDatabasePath(originalDbPath);
        return true;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        log.trace("propertyChange({})", event);
        super.propertyChange(event);
        if (event.getProperty().equals(FieldEditor.VALUE)) {

            if (tntDbPath.getStringValue().isEmpty())
                return;

            TntDb.setTntDatabasePath(tntDbPath.getStringValue());

            Util.connectToTntDatabase(getShell());
            if (TntDb.isConnected()) {
                setErrorMessage(null);
                setValid(true);
                if (!event.getNewValue().equals(event.getOldValue())) {
                    // Clear the Tnt User ID for all associated email servers
                    for (int i = 0; i < EmailModel.getEmailServerCount(); i++)
                        MIST.getPrefs().setToDefault(EmailModel.getPrefName(i, EmailModel.TNT_USERID));
                    // TODO: Somehow clear the value in the EmailServerPreferencePages?
                }
            } else {
                setErrorMessage("Error accessing TntConnect database");
                setValid(false);
            }
        }
    }

}
