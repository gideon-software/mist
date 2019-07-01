/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2017 Tom Hallman
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.model.HistoryModel;
import com.github.tomhallman.mist.tntapi.entities.ContactInfo;
import com.github.tomhallman.mist.tntapi.entities.History;
import com.github.tomhallman.mist.util.ui.Images;

public class MessagesView extends Composite implements PropertyChangeListener {
    private static Logger log = LogManager.getLogger();

    // Property change values
    public final static String PROP_MESSAGE = "MessagesView.message";

    // Table columns
    public final static int COL_STATUS = 0;
    public final static int COL_SOURCE = 1;
    public final static int COL_DATE = 2;
    public final static int COL_RESULT = 3;
    public final static int COL_SUBJECT = 4;

    public final static String DATA_DATE = "date";
    public final static String DATA_HISTORY = "history";
    public final static String DATA_COMPARATOR = "comparator";

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private int oldSelectionIndex;

    private ContactInfo contactInfo = null;
    private Table messagesTable = null;

    public MessagesView(Composite parent) {
        super(parent, SWT.NONE);
        log.trace("MessagesView({})", parent);
        MIST.getView().getContactsView().addPropertyChangeListener(this);
        HistoryModel.addPropertyChangeListener(this);
        // This must also listen for MessageDetailsView, but that's not instantiated yet
        // It is added later in MainWindowView:createContents(Composite)

        applyGridLayout(this).numColumns(1);

        // Create messages group
        Group detailsGroup = new Group(this, SWT.NONE);
        applyGridLayout(detailsGroup).numColumns(1);
        applyGridData(detailsGroup).withFill();
        detailsGroup.setText("Emails");

        // Create messages table
        messagesTable = new Table(detailsGroup, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE | SWT.FLAT);
        applyGridData(messagesTable).withFill();
        messagesTable.setHeaderVisible(true);
        messagesTable.setLinesVisible(true);
        messagesTable.setItemCount(0);
        oldSelectionIndex = -1;
        messagesTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                log.trace("messagesTable.widgetSelected({})", event);
                super.widgetSelected(event);
                int newSelectionIndex = messagesTable.getSelectionIndex();
                History history = (History) messagesTable.getSelection()[0].getData(DATA_HISTORY);
                if (newSelectionIndex != oldSelectionIndex) {
                    oldSelectionIndex = newSelectionIndex;
                    pcs.firePropertyChange(PROP_MESSAGE, null, history);
                }
            }
        });

        TableColumn actionColumn = new TableColumn(messagesTable, SWT.NONE);
        actionColumn.setText("MIST Action");
        actionColumn.setWidth(28);
        // TODO: Sortable columns?
        // actionColumn.setData("comparator", new Comparator<TableItem>() {});

        final TableColumn sourceColumn = new TableColumn(messagesTable, SWT.NONE);
        sourceColumn.setText("Account");
        sourceColumn.setWidth(70);

        final TableColumn dateColumn = new TableColumn(messagesTable, SWT.NONE);
        dateColumn.setText("Date");
        dateColumn.setWidth(130);

        final TableColumn resultColumn = new TableColumn(messagesTable, SWT.NONE);
        resultColumn.setText("Email Result");
        resultColumn.setWidth(28);

        final TableColumn subjectColumn = new TableColumn(messagesTable, SWT.NONE);
        subjectColumn.setText("Subject");
        subjectColumn.setWidth(300);

        // Resize subject column as the table/window resizes
        // TODO: Adjust this to allow for a minimum, but no maximum
        addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle area = getClientArea();
                Point size = messagesTable.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                ScrollBar vBar = messagesTable.getVerticalBar();
                int width = area.width - messagesTable.computeTrim(0, 0, 0, 0).width - vBar.getSize().x - 10;
                if (size.y > area.height + messagesTable.getHeaderHeight()) {
                    // Subtract the scrollbar width from the total column width
                    // if a vertical scrollbar will be required
                    Point vBarSize = vBar.getSize();
                    width -= vBarSize.x;
                }
                Point oldSize = messagesTable.getSize();
                if (oldSize.x < area.width) {
                    // table is getting bigger so make the table
                    // bigger first and then make the columns wider
                    // to match the client area width
                    messagesTable.setSize(area.width, area.height);
                }
                subjectColumn.setWidth(
                    width
                        - actionColumn.getWidth()
                        - resultColumn.getWidth()
                        - sourceColumn.getWidth()
                        - dateColumn.getWidth());
                if (oldSize.x > area.width) {
                    // table is getting smaller so make the columns
                    // smaller first and then resize the table to
                    // match the client area width
                    messagesTable.setSize(area.width, area.height);
                }
            }
        });

        messagesTable.addListener(SWT.PaintItem, new Listener() {

            @Override
            public void handleEvent(Event event) {
                if (event.index == COL_STATUS || event.index == COL_RESULT) {
                    TableItem tableItem = (TableItem) event.item;
                    Image image = (Image) tableItem.getData(String.valueOf(event.index));
                    int colWidth = messagesTable.getColumn(event.index).getWidth();
                    int imgWidth = image.getBounds().width;
                    int xPadding = colWidth / 2 - imgWidth / 2;
                    int newX = 0;
                    if (xPadding <= 0)
                        newX = event.x;
                    else
                        newX = event.x + xPadding;
                    if (event.index == 0) // First column is unique...
                        newX -= event.x;
                    event.gc.drawImage(image, newX, event.y + 1);
                }
            }
        });
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void addTableItem(History history) {
        log.trace("addTableItem({})", history);

        Image statusImage = Images.getStatusImage(history.getStatus());
        Image toFromImage = Images.getImage(
            history.getHistoryResultId() == History.RESULT_RECEIVED ? Images.ICON_MESSAGE_TO_ME
                : Images.ICON_MESSAGE_FROM_ME);

        String dateStr;
        LocalDateTime date = history.getHistoryDate();
        LocalDateTime jan1 = LocalDateTime.of(
            LocalDate.of(LocalDate.now().getYear(), Month.JANUARY, 1),
            LocalTime.MIDNIGHT);
        if (date.isBefore(jan1))
            dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd @ h:mma"));
        else
            dateStr = date.format(DateTimeFormatter.ofPattern("MMM d @ h:mma"));

        // Add to the message list in sorted order (newest first)
        int index = 0;
        while (index < messagesTable.getItemCount()
            && date.isAfter((LocalDateTime) messagesTable.getItem(index).getData(DATA_DATE)))
            index++;

        // Create table item
        TableItem item = new TableItem(messagesTable, SWT.NONE, index);
        item.setText(COL_STATUS, "");
        item.setData(String.valueOf(COL_STATUS), statusImage);
        item.setText(COL_RESULT, "");
        item.setData(String.valueOf(COL_RESULT), toFromImage);
        item.setText(COL_SOURCE, history.getMessageSource().getSourceName());
        item.setText(COL_DATE, dateStr);
        item.setText(COL_SUBJECT, history.getDescription());
        item.setData(DATA_DATE, date); // Store original date for sorting
        item.setData(DATA_HISTORY, history); // Store history for lookup later
    }

    public Table getMessagesTable() {
        return messagesTable;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        log.trace("propertyChange({})", event);

        if (ContactsView.PROP_CONTACT_SELECTED.equals(event.getPropertyName())) {
            // A new contact has been selected; load all their messages into the table
            messagesTable.removeAll();
            oldSelectionIndex = -1;
            contactInfo = (ContactInfo) event.getNewValue();
            History[] hisArr = HistoryModel.getAllHistoryWithContactInfo(contactInfo);
            for (History history : hisArr)
                addTableItem(history);

        } else if (HistoryModel.PROP_HISTORY_ADD.equals(event.getPropertyName()) && contactInfo != null) {
            History his = (History) event.getNewValue();
            if (contactInfo.equals(his.getContactInfo())) {
                // New history is being added for our contact; add it to the table
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        addTableItem(his);
                        // Scroll to the bottom of the table as items are entered
                        messagesTable.setTopIndex(messagesTable.getItemCount() - 1);
                    }
                });
            }

        } else if (HistoryModel.PROP_CONTACT_REMOVE.equals(event.getPropertyName())) {
            // The selected contact has been removed; clear the table
            messagesTable.removeAll();
            oldSelectionIndex = -1;
            contactInfo = null;

        } else if (MessageDetailsView.PROP_SUBJECT.equals(event.getPropertyName())) {
            // The subject has been altered; update it here
            String subject = (String) event.getNewValue();
            if (messagesTable.getSelectionCount() == 1)
                messagesTable.getSelection()[0].setText(COL_SUBJECT, subject);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

}
