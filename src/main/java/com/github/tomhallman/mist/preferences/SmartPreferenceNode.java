/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2019 Tom Hallman
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

package com.github.tomhallman.mist.preferences;

import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.swt.graphics.Image;

/**
 * Extends PreferenceNode to support using images from non-lazily-loaded PreferencePages
 */
public class SmartPreferenceNode extends PreferenceNode {
    // private static Logger log = LogManager.getLogger();

    private IPreferencePage page = null;

    /**
     * Creates a preference node with the given id and preference page. The title
     * and image of the preference page are used for the node label and image.
     *
     * @param id
     *            the node id
     * @param preferencePage
     *            the preference page
     * @param image
     *            the image shown in the tree
     */
    public SmartPreferenceNode(String id, IPreferencePage preferencePage) {
        super(id, preferencePage);
        this.page = preferencePage;
    }

    @Override
    public Image getLabelImage() {
        return page.getImage();
    }

}
