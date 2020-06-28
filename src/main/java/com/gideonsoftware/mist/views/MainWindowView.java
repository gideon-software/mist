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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.gideonsoftware.mist.MIST;
import com.gideonsoftware.mist.model.UpdateModel;
import com.gideonsoftware.mist.util.Util;
import com.gideonsoftware.mist.util.ui.Images;
import com.gideonsoftware.mist.util.ui.TipManager;

public class MainWindowView extends ApplicationWindow implements PropertyChangeListener {
    private static Logger log = LogManager.getLogger();

    public static final String PREF_WINDOW_BOUNDS = "mainwindow.window.bounds";
    public static final String PREF_WINDOW_MAXIMIZED = "mainwindow.window.maximized";
    public static final String PREF_SASH_LEFTRIGHT_WEIGHTS = "mainwindow.sash.leftright.weights";
    public static final String PREF_SASH_RIGHTVERT_WEIGHTS = "mainwindow.sash.rightvert.weights";

    private SashForm leftRightSash = null;
    private SashForm rightVerticalSash = null;

    private ContactsView contactsView = null;
    private ContactDetailsView contactDetailsView = null;
    private MessagesView messagesView = null;
    private ImportButtonView importButtonView = null;
    private MainMenuView mainMenuView = null;
    private MessageDetailsView messageDetailsView = null;
    private ProgressBarView progressBarView = null;
    private TaskItemView taskItemView = null;

    private TipManager tipManager = null;

    public MainWindowView() {
        super(null);
        log.trace("MainWindowView()");
        UpdateModel.addPropertyChangeListener(this); // Removed in handleShellCloseEvent
        tipManager = new TipManager();
    }

    @Override
    protected void configureShell(Shell shell) {
        log.trace("configureShell({})", shell);
        super.configureShell(shell);
        shell.setText(MIST.APP_NAME);
        setShellImage(shell);

        // Remember window size and location
        shell.addShellListener(new ShellAdapter() {
            @Override
            public void shellActivated(ShellEvent e) {
                Rectangle rect = MIST.getPrefs().getRectangle(PREF_WINDOW_BOUNDS);
                if (rect.width != 0)
                    shell.setBounds(rect);
                shell.setMaximized(MIST.getPrefs().getBoolean(PREF_WINDOW_MAXIMIZED));
            }
        });
        Listener saveShellStateListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (getShell().isVisible()) {
                    MIST.getPrefs().setValue(MainWindowView.PREF_WINDOW_MAXIMIZED, getShell().getMaximized());
                    if (!getShell().getMaximized())
                        MIST.getPrefs().setValue(MainWindowView.PREF_WINDOW_BOUNDS, getShell().getBounds());
                }
            }
        };
        shell.addListener(SWT.Resize, saveShellStateListener);
        shell.addListener(SWT.Move, saveShellStateListener);

        // Disable full-screen mode on Mac to get around nasty "freeze" bug
        // TODO: Is this still the case?
        // See https://gist.github.com/azhawkes/5009567
        // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=389486
        // See http://stackoverflow.com/questions/14834207/how-to-disable-fullscreen-button-in-mac-os-in-swt-java-app
        if (org.eclipse.jface.util.Util.isMac()) {
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

        tipManager.enableTips(true);
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
        leftRightSash = new SashForm(mainComposite, SWT.HORIZONTAL | SWT.SMOOTH);
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
        rightVerticalSash = new SashForm(leftRightSash, SWT.VERTICAL | SWT.SMOOTH);
        messagesView = new MessagesView(rightVerticalSash);
        applyGridData(messagesView).withFill();
        messageDetailsView = new MessageDetailsView(rightVerticalSash);
        applyGridData(messageDetailsView).withFill();

        messageDetailsView.addPropertyChangeListener(messagesView); // Needs to be added after instantiation
        messagesView.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                log.trace("MessagesView.widgetDisposed()");
                messageDetailsView.removePropertyChangeListener(messagesView);
            }
        });

        setSashFormWeightPrefData(leftRightSash, PREF_SASH_LEFTRIGHT_WEIGHTS, new int[] { 1, 3 });
        setSashFormWeightPrefData(rightVerticalSash, PREF_SASH_RIGHTVERT_WEIGHTS, new int[] { 2, 3 });

        getShell().pack();
        return mainComposite;
    }

    /**
     * Called by MainWindowController.closeView()
     */
    public void doClose() {
        log.trace("doClose()");
        tipManager.enableTips(false);
        UpdateModel.removePropertyChangeListener(this);
    }

    public ContactDetailsView getContactDetailsView() {
        return contactDetailsView;
    }

    public ContactsView getContactsView() {
        return contactsView;
    }

    public ImportButtonView getImportButtonView() {
        return importButtonView;
    }

    public SashForm getLeftRightSash() {
        return leftRightSash;
    }

    public MainMenuView getMainMenuView() {
        return mainMenuView;
    }

    public MessageDetailsView getMessageDetailsView() {
        return messageDetailsView;
    }

    public MessagesView getMessagesView() {
        return messagesView;
    }

    public ProgressBarView getProgressBarView() {
        return progressBarView;
    }

    public SashForm getRightVerticalSash() {
        return rightVerticalSash;
    }

    public TaskItemView getTaskItemView() {
        return taskItemView;
    }

    public TipManager getTipManager() {
        return tipManager;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        log.trace("propertyChange({})", event);

        if (UpdateModel.PROP_STATUS_CHECKED.equals(event.getPropertyName()) && UpdateModel.isUpdateAvailable()) {
            if (Display.getDefault().isDisposed())
                return;
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    log.info("A new version of MIST is available: {}", UpdateModel.getNewVersion());
                    MessageBox mBox = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.YES | SWT.NO);
                    mBox.setText("MIST");
                    mBox.setMessage(
                        String.format(
                            "A new version of MIST is available!\n"
                                + "Current version:\t%s\n"
                                + "New version:\t%s\n"
                                + "\n"
                                + "Would you like to download it now?",
                            MIST.getAppVersion(),
                            UpdateModel.getNewVersion()));
                    if (mBox.open() == SWT.YES) {
                        String link = UpdateModel.getNewVersionDownloadURL();
                        if (link.isBlank())
                            link = MIST.HOMEPAGE;
                        if (!Program.launch(link)) {
                            String msg = String.format(
                                "There was a problem loading your default web browser. You can find download MIST from: %s",
                                link);
                            Util.reportError("Unable to load URL in default browser", msg);
                        }
                    }
                }
            });
        }
    }

    /**
     * @param sashForm
     * @param prefName
     * @param defaultWeights
     */
    public void setSashFormWeightPrefData(SashForm sashForm, String prefName, int[] defaultWeights) {
        log.trace("setSashFormWeightPrefData({},{},{})", sashForm, prefName, defaultWeights);
        MIST.getPrefs().setDefault(prefName, defaultWeights);
        int[] weights = MIST.getPrefs().getInts(prefName);
        sashForm.setWeights(weights);
        // Note: this listener is never called; gave up tying! 2019-07-02 TJH
        sashForm.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                log.trace("SashForm.mouseUp({})", e);
                MIST.getPrefs().setValues(prefName, sashForm.getWeights());
            }
        });

    }

    protected void setShellImage(Shell shell) {
        log.trace("setShellImage({})", shell);
        Image[] images = new Image[5];
        images[0] = Images.getImage(Images.ICON_MIST_16);
        images[1] = Images.getImage(Images.ICON_MIST_32);
        images[2] = Images.getImage(Images.ICON_MIST_48);
        images[3] = Images.getImage(Images.ICON_MIST_64);
        images[4] = Images.getImage(Images.ICON_MIST_128);
        shell.setImages(images);
        Window.setDefaultImage(images[0]);
    }
}
