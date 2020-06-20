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

package com.gideonsoftware.mist.util.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;

/**
 * 
 */
public abstract class SmartWizardPage extends WizardPage {
    // private static Logger log = LogManager.getLogger();

    /**
     * Creates a new wizard page with the given name, and
     * with no title or image.
     *
     * @param pageName
     *            the name of the page
     */
    protected SmartWizardPage(String pageName) {
        super(pageName);
    }

    /**
     * Creates a new wizard page with the given name, title, and image.
     *
     * @param pageName
     *            the name of the page
     * @param title
     *            the title for this wizard page,
     *            or <code>null</code> if none
     * @param titleImage
     *            the image descriptor for the title of this wizard page,
     *            or <code>null</code> if none
     */
    protected SmartWizardPage(String pageName, String title, ImageDescriptor titleImage) {
        super(pageName, title, titleImage);
    }

    /**
     * 
     * @return
     */
    protected boolean nextPressed() {
        return true;
    }
}
