/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2020 Gideon Software
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.model.UpdateModel;
import com.gideonsoftware.mist.preferences.fieldeditors.SmartComboFieldEditor;
import com.gideonsoftware.mist.util.ui.Images;

/**
 *
 */
public class UpdatesPreferencePage extends FieldEditorPreferencePage {
    private static Logger log = LogManager.getLogger();

    private SmartComboFieldEditor<String> channelEditor;

    public UpdatesPreferencePage() {
        super(FieldEditorPreferencePage.GRID);
        log.trace("UpdatesPreferencePage()");
        setTitle("Updates");
        setImageDescriptor(ImageDescriptor.createFromImage(Images.getImage(Images.ICON_UPDATES)));
        // setDescription("description here");
        noDefaultAndApplyButton();
    }

    @Override
    protected void createFieldEditors() {
        log.trace("createFieldEditors()");

        // Channel
        MIST.getPrefs().setDefault(UpdateModel.PREF_UPDATE_CHANNEL, UpdateModel.CHANNEL_STABLE); // Default
        channelEditor = new SmartComboFieldEditor<String>(
            UpdateModel.PREF_UPDATE_CHANNEL,
            "&Update Channel:",
            getFieldEditorParent());
        channelEditor.setEmptySelectionAllowed(false);
        channelEditor.add(UpdateModel.CHANNEL_STABLE, UpdateModel.CHANNEL_STABLE);
        channelEditor.add(UpdateModel.CHANNEL_BETA, UpdateModel.CHANNEL_BETA);
        addField(channelEditor);
    }

}
