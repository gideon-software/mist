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

package com.github.tomhallman.mist.views;

import static com.github.tomhallman.mist.util.ui.GridDataUtil.applyGridData;
import static com.github.tomhallman.mist.util.ui.GridLayoutUtil.applyGridLayout;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.util.ui.ImageManager;

public class MainWindowView extends ApplicationWindow {
    private static Logger log = LogManager.getLogger();

    private ContactsView contactsView = null;
    private ContactDetailsView contactDetailsView = null;

    private MessagesView messagesView = null;
    private ImportButtonView importButtonView = null;
    private MainMenuView mainMenuView = null;
    private MessageDetailsView messageDetailsView = null;
    private ProgressBarView progressBarView = null;
    private TaskItemView taskItemView = null;

    public MainWindowView() {
        super(null);
        log.trace("MainWindowView()");
    }

    @Override
    protected void configureShell(Shell shell) {
        log.trace("configureShell({})", shell);
        super.configureShell(shell);
        shell.setText(MIST.APP_NAME);
        setShellImage(shell);

        // Disable full-screen mode on Mac to get around nasty "freeze" bug
        // See https://gist.github.com/azhawkes/5009567
        // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=389486
        // See http://stackoverflow.com/questions/14834207/how-to-disable-fullscreen-button-in-mac-os-in-swt-java-app
        if (Util.isMac()) {
            try {
                java.lang.reflect.Field field = Control.class.getDeclaredField("view");
                Object /* NSView */ view = field.get(shell);
                if (view != null) {
                    Class<?> c = Class.forName("org.eclipse.swt.internal.cocoa.NSView");
                    Object /* NSWindow */ window = c.getDeclaredMethod("window").invoke(view);

                    c = Class.forName("org.eclipse.swt.internal.cocoa.NSWindow");
                    java.lang.reflect.Method setCollectionBehavior = c.getDeclaredMethod(
                        "setCollectionBehavior",
                        /* JVM.is64bit() ? */long.class /* : int.class */);
                    setCollectionBehavior.invoke(window, 0);
                }
            } catch (Exception e) {
                log.warn("Unable to disable full-screen mode", e);
            }
        }
    }

    @Override
    protected Control createContents(Composite parent) {
        log.trace("createContents({})", parent);

        // Create task item
        taskItemView = new TaskItemView(getShell());

        // Create menu bar
        mainMenuView = new MainMenuView(getShell());

        // Create main composite
        Composite mainComposite = new Composite(parent, SWT.NONE);
        applyGridLayout(mainComposite).numColumns(1);
        applyGridData(mainComposite).withFill();

        // Create import controls composite
        Composite importControlsComp = new Composite(mainComposite, SWT.NONE);
        applyGridLayout(importControlsComp).numColumns(2).zeroMarginAndSpacing();
        applyGridData(importControlsComp).withHorizontalFill();

        importButtonView = new ImportButtonView(importControlsComp);
        progressBarView = new ProgressBarView(importControlsComp);

        // Create left/right sash
        // TODO: Set minimum size for contact list
        // See https://forums.pentaho.com/showthread.php?61793-Creating-a-Minimum-Width-Constraint-on-an-SWT-SashForm
        SashForm leftRightSash = new SashForm(mainComposite, SWT.HORIZONTAL | SWT.SMOOTH);
        applyGridData(leftRightSash).withFill();

        // Create composite on left
        Composite leftComp = new Composite(leftRightSash, SWT.NONE);
        applyGridLayout(leftComp).numColumns(1).zeroMarginAndSpacing();
        applyGridData(leftComp).withFill();
        contactsView = new ContactsView(leftComp);
        applyGridData(contactsView).withFill().heightHint(300);
        contactDetailsView = new ContactDetailsView(leftComp);
        applyGridData(contactDetailsView).withHorizontalFill();

        // Create vertical sash on right
        SashForm rightVerticalSash = new SashForm(leftRightSash, SWT.VERTICAL | SWT.SMOOTH);
        messagesView = new MessagesView(rightVerticalSash);
        applyGridData(messagesView).withFill();
        messageDetailsView = new MessageDetailsView(rightVerticalSash);
        applyGridData(messageDetailsView).withFill();
        messageDetailsView.addPropertyChangeListener(messagesView); // Needs to be added after instantiation

        leftRightSash.setWeights(new int[] { 1, 3 });
        rightVerticalSash.setWeights(new int[] { 2, 3 });
        getShell().pack();

        return mainComposite;
    }

    public ContactsView getContactsView() {
        return contactsView;
    }

    public ContactDetailsView getContactDetailsView() {
        return contactDetailsView;
    }

    public ImportButtonView getImportButtonView() {
        return importButtonView;
    }

    public MessageDetailsView getMessageDetailsView() {
        return messageDetailsView;
    }

    public MessagesView getMessagesView() {
        return messagesView;
    }

    public MainMenuView getMainMenuView() {
        return mainMenuView;
    }

    public ProgressBarView getProgressBarView() {
        return progressBarView;
    }

    protected void setShellImage(Shell shell) {
        log.trace("setShellImage({})", shell);
        Image[] images = new Image[5];
        images[0] = ImageManager.getImage("appicon-16x16");
        images[1] = ImageManager.getImage("appicon-32x32");
        images[2] = ImageManager.getImage("appicon-48x48");
        images[3] = ImageManager.getImage("appicon-64x64");
        images[4] = ImageManager.getImage("appicon-128x128");
        shell.setImages(images);
    }

    public TaskItemView getTaskItemView() {
        return taskItemView;
    }
}
