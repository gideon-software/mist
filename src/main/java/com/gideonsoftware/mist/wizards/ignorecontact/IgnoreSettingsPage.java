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

package com.gideonsoftware.mist.wizards.ignorecontact;

import static com.gideonsoftware.mist.util.ui.GridDataUtil.applyGridData;
import static com.gideonsoftware.mist.util.ui.GridLayoutUtil.applyGridLayout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.gideonsoftware.mist.model.EmailModel;

/**
 * 
 */
public class IgnoreSettingsPage extends WizardPage {
    private static Logger log = LogManager.getLogger();

    private Text emailText;
    private Button globalCheck;
    private List<Button> serverChecks = new ArrayList<Button>();

    public IgnoreSettingsPage() {
        super("Ignore Settings Page");
        log.trace("IgnoreSettingsPage()");
        setTitle("Ignore Settings");
        setDescription("Confirm email address and select which ignore list(s) to add it to.");
    }

    private void checkPageComplete() {
        if (emailText.getText().isBlank())
            setPageComplete(false);
        else {
            if (globalCheck.getSelection())
                setPageComplete(true);
            else {
                // If there's at least one server checked
                boolean isOneSelected = false;
                for (Iterator<Button> it = serverChecks.iterator(); it.hasNext();)
                    if (it.next().getSelection())
                        isOneSelected = true;
                setPageComplete(isOneSelected);
            }
        }
    }

    @Override
    public void createControl(Composite parent) {
        log.trace("createControl({})", parent);
        IgnoreContactWizard wizard = ((IgnoreContactWizard) getWizard());
        Composite comp = new Composite(parent, SWT.NONE);
        applyGridLayout(comp);

        emailText = new Text(comp, SWT.BORDER);
        emailText.setText(wizard.getContactInfo().getInfo());
        applyGridData(emailText).horizontalAlignment(SWT.FILL);
        emailText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                log.trace("emailText.modifyText()");
                checkPageComplete();
            }
        });
        new Label(comp, SWT.NONE).setText("(Use * for any string and ? for any character)");

        new Label(comp, SWT.NONE).setText("");

        globalCheck = new Button(comp, SWT.CHECK);
        globalCheck.setText("Add to ignore list for all email accounts");
        globalCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                log.trace("globalCheck.widgetSelected()");
                enableServerChecks(!globalCheck.getSelection());
                checkPageComplete();
            }
        });

        applyGridData(new Label(comp, SWT.HORIZONTAL | SWT.SEPARATOR)).horizontalAlignment(SWT.FILL);

        // For every server, add a checkbox
        for (int i = 0; i < EmailModel.getEmailServerCount(); i++) {
            Button serverCheck = new Button(comp, SWT.CHECK);
            String serverName = EmailModel.getEmailServer(i).getNickname();
            serverCheck.setText(String.format("Add to ignore list for '%s'", serverName));
            serverCheck.setData("serverId", i);
            serverCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    log.trace("serverCheck.widgetSelected()");
                    checkPageComplete();
                }
            });
            serverChecks.add(serverCheck);
        }

        globalCheck.setSelection(true);
        enableServerChecks(false);

        setControl(comp); // Needed for page to work properly
    }

    protected void enableServerChecks(boolean enable) {
        log.trace("enableServerChecks({})", enable);
        for (Iterator<Button> it = serverChecks.iterator(); it.hasNext();)
            it.next().setEnabled(enable);
    }

    public String getEmail() {
        return emailText.getText();
    }

    public int[] getSelectedServerIds() {
        ArrayList<Integer> selectedIdList = new ArrayList<Integer>();
        for (Iterator<Button> it = serverChecks.iterator(); it.hasNext();) {
            Button serverCheck = it.next();
            if (serverCheck.getSelection())
                selectedIdList.add((Integer) serverCheck.getData("serverId"));
        }
        int[] selectedIds = new int[selectedIdList.size()];
        Iterator<Integer> it = selectedIdList.iterator();
        for (int i = 0; i < selectedIds.length; i++)
            selectedIds[i] = it.next().intValue();
        return selectedIds;
    }

    public boolean isGlobalCheckSelected() {
        return globalCheck.getSelection();
    }
}
