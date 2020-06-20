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

package com.gideonsoftware.mist.wizards.firsttimesetup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.wizard.WizardPage;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.tntapi.TntDb;
import com.gideonsoftware.mist.wizards.newemailaccount.NewEmailAccountWizard;

/**
 * First-time setup for configuring MIST
 */
public class FirstTimeSetupWizard extends NewEmailAccountWizard {
    private static Logger log = LogManager.getLogger();

    private FirstTimeSetupWelcomePage firstTimeSetupWelcomePage;
    private SelectTntDbPage selectTntDbPage;
    private FirstTimeSetupCompletePage firstTimeSetupCompletePage;

    public FirstTimeSetupCompletePage getFirstTimeSetupCompletePage() {
        return firstTimeSetupCompletePage;
    }

    public FirstTimeSetupWelcomePage getFirstTimeSetupWelcomePage() {
        return firstTimeSetupWelcomePage;
    }

    @Override
    public WizardPage getPostEmailSetupPage() {
        return firstTimeSetupCompletePage;
    }

    public SelectTntDbPage getSelectTntDbPage() {
        return selectTntDbPage;
    }

    @Override
    public void insertPagesAfter() {
        log.trace("insertPagesAfter()");
        firstTimeSetupCompletePage = new FirstTimeSetupCompletePage();
        addPage(firstTimeSetupCompletePage);
    }

    @Override
    public void insertPagesBefore() {
        log.trace("insertPagesBefore()");
        firstTimeSetupWelcomePage = new FirstTimeSetupWelcomePage();
        selectTntDbPage = new SelectTntDbPage();
        addPage(firstTimeSetupWelcomePage);
        addPage(selectTntDbPage);
    }

    @Override
    public boolean performFinish() {
        log.trace("performFinish()");
        TntDb.disconnect(); // Should already be done
        MIST.getPrefs().setValue(TntDb.PREF_TNT_DBPATH, getSelectTntDbPage().getTntDbPath());
        return super.performFinish();
    }

}
