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

package com.gideonsoftware.mist.util.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolTip;

public class ViewUtil {

    final static int TOOLTIP_HIDE_DELAY = 0;
    final static int TOOLTIP_SHOW_DELAY = 0;

    public static int getTextHeight(Text text) {
        GC gc = new GC(text);
        FontMetrics fm = gc.getFontMetrics();
        int lineHeight = fm.getHeight();
        gc.dispose();
        return lineHeight;
    }

    public static int getTextWidth(Text text) {
        GC gc = new GC(text);
        FontMetrics fm = gc.getFontMetrics();
        double charWidth = fm.getAverageCharacterWidth();
        gc.dispose();
        return (int) Math.round(charWidth);
    }

    /**
     * Found at http://stackoverflow.com/a/11898253
     * 
     * @param control
     * @param tooltipText
     * @param tooltipMessage
     */
    public static void setTooltip(final Control control, String tooltipText, String tooltipMessage) {

        final ToolTip tip = new ToolTip(control.getShell(), SWT.BALLOON);
        tip.setText(tooltipText);
        tip.setMessage(tooltipMessage);
        tip.setAutoHide(false);

        control.addListener(SWT.MouseHover, new Listener() {
            public void handleEvent(Event event) {
                tip.getDisplay().timerExec(TOOLTIP_SHOW_DELAY, new Runnable() {
                    public void run() {
                        tip.setVisible(true);
                    }
                });
            }
        });

        control.addListener(SWT.MouseExit, new Listener() {
            public void handleEvent(Event event) {
                tip.getDisplay().timerExec(TOOLTIP_HIDE_DELAY, new Runnable() {
                    public void run() {
                        tip.setVisible(false);
                    }
                });
            }
        });
    }
}
