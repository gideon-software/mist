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

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.gideonsoftware.mist.MIST;

/**
 * 
 */
public class TipMessageBox extends Dialog {
    private static Logger log = LogManager.getLogger();

    public static int DONOTSHOWCHECKBOX_ID = IDialogConstants.CLIENT_ID + 1;
    public static String DONOTSHOWSTR = "Do not show this tip again";

    /**
     * Only allow one tip to pop up at a time
     */
    private static BlockingQueue<Integer> tipQueue = new LinkedBlockingQueue<Integer>();
    private static int tipCounter = 0;

    /**
     * Collection of buttons created by the <code>createButton</code> method.
     * (Taken from Dialog)
     */
    private HashMap<Integer, Button> buttons = new HashMap<>();

    protected Button doNotShowAgainCheckBox = null;
    protected String preferenceName = null;
    protected boolean defaultDoNotShowAgain = false;
    protected String title = "Tip";
    protected String message = "";
    protected boolean skipTip = false;
    private int myTipId = 0;

    public TipMessageBox(Shell parentShell, String preferenceName, String title, String message) {
        this(parentShell, preferenceName, title, message, true);
    }

    public TipMessageBox(
        Shell parentShell,
        String preferenceName,
        String title,
        String message,
        boolean defaultDoNotShowAgain) {
        super(parentShell);
        log.trace("TipMessageBox({},{},{},{},{})", parentShell, preferenceName, title, message, defaultDoNotShowAgain);
        this.preferenceName = preferenceName;
        if (title != null && !title.isBlank())
            this.title += ": " + title;
        if (preferenceName == null) {
            log.error("Skipping tip: Preference name not supplied");
            skipTip = true;
        }
        MIST.getPrefs().setDefault(preferenceName, false);
        if (MIST.getPrefs().getBoolean(preferenceName)) {
            log.error("Skipping tip: Preference is 'skip' (true)");
            skipTip = true;
        }
        this.message = message;
        this.defaultDoNotShowAgain = defaultDoNotShowAgain;
    }

    @Override
    protected void configureShell(Shell newShell) {
        log.trace("configureShell({})", newShell);
        newShell.setText(title);
        super.configureShell(newShell);
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        GridLayout layout = new GridLayout();
        layout.numColumns = 0; // this is incremented by createButton
        layout.makeColumnsEqualWidth = false;
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        GridData data = new GridData(
            GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER | GridData.FILL_HORIZONTAL);
        Composite composite = WidgetFactory.composite(SWT.NONE).layout(layout).layoutData(data).font(parent.getFont())
            .create(parent);
        // Add the buttons to the button bar.
        createButtonsForButtonBar(composite);
        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        log.trace("createButtonsForButtonBar({})", parent);
        createDoNotShowAgainCheckBox(parent);
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        log.trace("createDialogArea({})", parent);
        Composite dialogAreaComp = (Composite) super.createDialogArea(parent);

        Composite comp = new Composite(dialogAreaComp, SWT.NONE);
        GridLayoutUtil.applyGridLayout(comp).marginWidth(0).marginHeight(0).verticalSpacing(0).numColumns(2);
        GridDataUtil.applyGridData(comp).withFill();

        Label imgLabel = new Label(comp, SWT.NONE);
        imgLabel.setImage(getShell().getDisplay().getSystemImage(SWT.ICON_INFORMATION));
        GridDataUtil.applyGridData(imgLabel).verticalAlignment(SWT.BEGINNING);

        Label msgLabel = new Label(comp, SWT.NONE);
        msgLabel.setText(message);
        GridDataUtil.applyGridData(msgLabel).withFill().horizontalIndent(8);

        return comp;
    }

    protected Button createDoNotShowAgainCheckBox(Composite parent) {
        // increment the number of columns in the button bar
        ((GridLayout) parent.getLayout()).numColumns++;
        doNotShowAgainCheckBox = WidgetFactory.button(SWT.CHECK).text(DONOTSHOWSTR).font(JFaceResources.getDialogFont())
            .data(Integer.valueOf(DONOTSHOWCHECKBOX_ID)).onSelect(
                event -> buttonPressed(((Integer) event.widget.getData()).intValue())).create(parent);
        buttons.put(Integer.valueOf(DONOTSHOWCHECKBOX_ID), doNotShowAgainCheckBox);
        // setButtonLayoutData(doNotShowAgainCheckBox);
        GridDataUtil.applyGridData(doNotShowAgainCheckBox).grabExcessHorizontalSpace(true).horizontalAlignment(
            GridData.HORIZONTAL_ALIGN_BEGINNING);
        doNotShowAgainCheckBox.setSelection(defaultDoNotShowAgain);
        return doNotShowAgainCheckBox;
    }

    @Override
    protected void okPressed() {
        log.trace("okPressed()");
        MIST.getPrefs().setValue(preferenceName, doNotShowAgainCheckBox.getSelection());
        super.okPressed();
    }

    @Override
    public int open() {
        log.trace("open(){}", skipTip ? " -- skipping" : "");
        if (skipTip)
            return CANCEL;

        // Only allow one tip to be open at a time
//        myTipId = ++tipCounter;
//        tipQueue.offer(myTipId);

        int ret;
        // Wait for user to close any previous tip(s)
//        while (tipQueue.peek() != myTipId) {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//            }
//        }

        ret = super.open();

        // Remove this tip from the tip queue
//        tipQueue.poll();
        return ret;
    }
}
