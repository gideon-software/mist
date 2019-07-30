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

package com.gideonsoftware.mist.wizards.newemailserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.wizard.Wizard;

/**
 * 
 */
public class NewEmailServerWizard extends Wizard {
    private static Logger log = LogManager.getLogger();

    public static final String TYPE_IMAP = "imap";
    public static final String TYPE_GMAIL = "gmail";

    private SelectTypePage selectTypePage;
    private String type = TYPE_IMAP;

    @Override
    public void addPages() {
        log.trace("addPages()");

        selectTypePage = new SelectTypePage();
        addPage(selectTypePage);
    }

    @Override
    public boolean canFinish() {
        log.trace("canFinish()");
        return selectTypePage.isPageComplete();
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean performFinish() {
        log.trace("performFinish()");
        return true;
    }

    public void setType(String type) {
        this.type = type;
    }

}
