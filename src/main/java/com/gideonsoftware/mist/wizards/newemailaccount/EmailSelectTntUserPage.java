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

package com.gideonsoftware.mist.wizards.newemailaccount;

import static com.gideonsoftware.mist.util.ui.GridDataUtil.applyGridData;
import static com.gideonsoftware.mist.util.ui.GridLayoutUtil.applyGridLayout;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;

import com.gideonsoftware.mist.model.data.EmailServer;
import com.gideonsoftware.mist.tntapi.TntDb;
import com.gideonsoftware.mist.tntapi.UserManager;
import com.gideonsoftware.mist.tntapi.entities.User;
import com.gideonsoftware.mist.util.Util;
import com.gideonsoftware.mist.util.ui.Images;
import com.gideonsoftware.mist.util.ui.SmartCombo;
import com.gideonsoftware.mist.util.ui.ViewUtil;

public class EmailSelectTntUserPage extends WizardPage {
    private static Logger log = LogManager.getLogger();

    private SmartCombo<Integer> userCombo = null;
    private Button usersRefreshButton = null;

    public EmailSelectTntUserPage() {
        super("Add Email Account: Select TntConnect User");
        log.trace("EmailSelectTntUserPage()");
        // Title set later based on account type
        setDescription("Which TntConnect user should MIST associate with this email account?");
    }

    @Override
    public void createControl(Composite parent) {
        log.trace("createControl({})", parent);

        Composite comp = new Composite(parent, SWT.NONE);
        applyGridLayout(comp).numColumns(2);

        userCombo = new SmartCombo<Integer>(comp, SWT.BORDER | SWT.READ_ONLY);
        applyGridData(userCombo).widthHint(ViewUtil.getTextWidth(userCombo) * 15);
        userCombo.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            log.trace("userCombo.widgetSelected({})", e);
            setPageComplete(!userCombo.getText().isBlank());
        }));

        usersRefreshButton = new Button(comp, SWT.PUSH);
        usersRefreshButton.setImage(Images.getImage(Images.ICON_RELOAD));
        usersRefreshButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            log.trace("usersRefreshButton.widgetSelected({})", e);
            if (loadUsers()) {
                MessageBox msgBox = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
                msgBox.setMessage("Users reloaded from TntConnect.");
                msgBox.open();
            }
        }));

        setControl(comp); // Needed for page to work properly

        setPageComplete(false);
    }

    @Override
    public IWizardPage getNextPage() {
        log.trace("getNextPage()");
        return ((NewEmailAccountWizard) getWizard()).getPostEmailSetupPage();
    }

    public Integer getTntUserId() {
        if (userCombo.getItemsMap().size() == 1)
            return userCombo.getItemsMap().keySet().toArray(new Integer[0])[0];
        else
            return userCombo.getSelectionItem();
    }

    public String getTntUsername() {
        return userCombo.getItemsMap().get(getTntUserId());
    }

    public SmartCombo<Integer> getUserCombo() {
        return userCombo;
    }

    public boolean loadUsers() {
        // Try to populate the control from the Tnt DB
        Util.connectToTntDatabase();

        if (TntDb.isConnected()) {
            // If there was a previously selected key, use that
            Integer oldKey = userCombo.getSelectionItem();

            // Populate user list
            userCombo.removeAll();
            try {
                for (User user : UserManager.getUserList())
                    userCombo.add(user.getId(), user.getUsername());
            } catch (SQLException e) {
                Util.reportError("Unable to load user list", "Unable to load user list", e);
            }
            if (oldKey != null)
                userCombo.select(oldKey);
        }
        return TntDb.isConnected();
    }

    @Override
    public void setVisible(boolean visible) {
        log.trace("setVisible({})", visible);

        if (visible) {
            String type = ((NewEmailAccountWizard) getWizard()).getEmailSelectAccountTypePage().getType();
            setTitle(String.format("Add %s Account: Select TntConnect User", EmailServer.getFormattedTypeName(type)));
            if (userCombo.getItemsMap().isEmpty())
                loadUsers();
        }

        super.setVisible(visible);
    }

}
