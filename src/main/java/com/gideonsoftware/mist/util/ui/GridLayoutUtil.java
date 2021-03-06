/**
 * @see https://eclipsesource.com/blogs/2013/07/25/efficiently-dealing-with-swt-gridlayout-and-griddata/
 * @see https://gist.github.com/mpost/6077907
 */

package com.gideonsoftware.mist.util.ui;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;

public class GridLayoutUtil {

    private final GridLayout gridLayout;

    private GridLayoutUtil(GridLayout gridLayout) {
        this.gridLayout = gridLayout;
    }

    public static GridLayoutUtil applyGridLayout(Composite composite) {
        GridLayout gridLayout = new GridLayout();
        composite.setLayout(gridLayout);
        return new GridLayoutUtil(gridLayout);
    }

    public static GridLayoutUtil onGridLayout(Composite composite) {
        Layout layout = composite.getLayout();
        if (layout instanceof GridLayout) {
            return new GridLayoutUtil((GridLayout) layout);
        }
        throw new IllegalStateException("Composite has to have a GridLayout. Has " + layout);
    }

    public GridLayoutUtil numColumns(int numColumns) {
        gridLayout.numColumns = numColumns;
        return this;
    }

    public GridLayoutUtil columnsEqualWidth(boolean columnsEqualWidth) {
        gridLayout.makeColumnsEqualWidth = columnsEqualWidth;
        return this;
    }

    public GridLayoutUtil horizontalSpacing(int horizontalSpacing) {
        gridLayout.horizontalSpacing = horizontalSpacing;
        return this;
    }

    public GridLayoutUtil verticalSpacing(int verticalSpacing) {
        gridLayout.verticalSpacing = verticalSpacing;
        return this;
    }

    public GridLayoutUtil marginWidth(int marginWidth) {
        gridLayout.marginWidth = marginWidth;
        return this;
    }

    public GridLayoutUtil marginHeight(int marginHeight) {
        gridLayout.marginHeight = marginHeight;
        return this;
    }

    public GridLayoutUtil marginTop(int marginTop) {
        gridLayout.marginTop = marginTop;
        return this;
    }

    public GridLayoutUtil marginBottom(int marginBottom) {
        gridLayout.marginBottom = marginBottom;
        return this;
    }

    public GridLayoutUtil marginLeft(int marginLeft) {
        gridLayout.marginLeft = marginLeft;
        return this;
    }

    public GridLayoutUtil marginRight(int marginRight) {
        gridLayout.marginRight = marginRight;
        return this;
    }

    // Added by TJH
    public GridLayoutUtil zeroMarginAndSpacing() {
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.horizontalSpacing = 0;
        return this;
    }
}
