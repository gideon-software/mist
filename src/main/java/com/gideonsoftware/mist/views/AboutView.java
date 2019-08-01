/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2010 Gideon Software
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

package com.gideonsoftware.mist.views;

import static com.gideonsoftware.mist.util.ui.GridDataUtil.applyGridData;
import static com.gideonsoftware.mist.util.ui.GridLayoutUtil.applyGridLayout;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.util.ui.Images;

public class AboutView extends Dialog {
    private static Logger log = LogManager.getLogger();

    private Link homepageLink = null;
    private Link mailingListLink = null;
    private Button facebookButton = null;

    public AboutView(Shell parentShell) {
        super(parentShell);
        log.trace("AboutView({})", parentShell);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("About MIST");
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button okButton = createButton(parent, IDialogConstants.OK_ID, "OK", true);
        okButton.setFocus();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        // Create main composite
        Composite comp = new Composite(parent, SWT.NONE);
        applyGridLayout(comp).numColumns(2);
        applyGridData(comp).withFill();

        // Left side includes the logo & facebook button
        Composite leftComp = new Composite(comp, SWT.NONE);
        applyGridLayout(leftComp).numColumns(2);
        applyGridData(leftComp).withFill();

        Label imageLabel = new Label(leftComp, SWT.NONE);
        imageLabel.setImage(Images.getImage(Images.LOGO_MIST));
        applyGridData(imageLabel).withFill().horizontalSpan(2);

        facebookButton = new Button(leftComp, SWT.PUSH);
        facebookButton.setImage(Images.getImage(Images.LOGO_FACEBOOK));
        if (Util.isMac()) {
            applyGridData(facebookButton).horizontalAlignment(SWT.CENTER).verticalAlignment(SWT.CENTER)
                .grabExcessHorizontalSpace(true).grabExcessVerticalSpace(true).heightHint(50);
        }

        // Right side includes the text
        Composite rightComp = new Composite(comp, SWT.NONE);
        applyGridLayout(rightComp);
        applyGridData(rightComp).withFill();

        (new Label(rightComp, SWT.NONE)).setText(
            String.format("MIST: eMail Import System for TntConnect %s", MIST.getAppVersion()));
        new Label(rightComp, SWT.NONE);
        homepageLink = new Link(rightComp, SWT.NONE);
        homepageLink.setText(String.format("Homepage: <a>%s</a>", MIST.HOMEPAGE));
        mailingListLink = new Link(rightComp, SWT.NONE);
        // TODO: Reformat into two controls to use SimpleEmailLink (and remove EmailLinkListener?)
        mailingListLink.setText(String.format("Email support: <a>%s</a>", MIST.EMAIL_SUPPORT));
        new Label(rightComp, SWT.NONE);
        (new Label(rightComp, SWT.NONE)).setText(
            String.format(
                ""
                    + "MIST is not directly affiliated with TntConnect or TntWare,%n"
                    + "but we hope it will be useful to the TntConnect community!%n"
                    + "Please do not seek help for MIST on the TntConnect forums.%n"
                    + "Use the MIST links above instead."));
        return comp;
    }

    public Button getFacebookButton() {
        return facebookButton;
    }

    public Link getHomepageLink() {
        return homepageLink;
    }

    public Link getMailingListLink() {
        return mailingListLink;
    }
}
