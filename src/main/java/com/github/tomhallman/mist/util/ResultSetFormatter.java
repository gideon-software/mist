/*
 * Database Table Printer
 * Copyright (C) 2014 Hami Galip Torun
 * 
 * Email: hamitorun@e-fabrika.net
 * Project Home: https://github.com/htorun/dbtableprinter
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
 */

/*
 * This is my first Java program that does something more or less
 * useful. It is part of my effort to learn Java, how to use
 * an IDE (IntelliJ IDEA 13.1.15 in this case), how to apply an
 * open source license and how to use Git and GitHub (https://github.com)
 * for version control and publishing an open source software.
 * 
 * Hami
 */

package com.github.tomhallman.mist.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility for converting a ResultSet into a human-readable string format.
 * <p>
 * Derived from {@link https://github.com/htorun/dbtableprinter}
 */
public class ResultSetFormatter {
    /**
     * Represents a database table column.
     */
    private static class Column {

        /**
         * Column label.
         */
        private String label;

        /**
         * Generic SQL type of the column as defined in {@link java.sql.Types}.
         */
        private int type;

        /**
         * Generic SQL type of the column as defined in {@link java.sql.Types}.
         */
        private String typeName;

        /**
         * Width of the column that will be adjusted according to column label
         * and values.
         */
        private int width = 0;

        /**
         * Column values from each row of a <code>ResultSet</code>.
         */
        private List<String> values = new ArrayList<>();

        /**
         * Flag for text justification using <code>String.format</code>.
         * Empty string (<code>""</code>) to justify right,
         * dash (<code>-</code>) to justify left.
         *
         * @see #justifyLeft()
         */
        private String justifyFlag = "";

        /**
         * Column type category. The columns will be categorized according
         * to their column types and specific needs to format them correctly.
         */
        private int typeCategory = 0;

        /**
         * Constructs a new <code>Column</code> with a column label,
         * generic SQL type and type name (as defined in {@link java.sql.Types})
         *
         * @param label
         *            Column label or name
         * @param type
         *            Generic SQL type
         * @param typeName
         *            Generic SQL type name
         */
        public Column(String label, int type, String typeName) {
            this.label = label;
            this.type = type;
            this.typeName = typeName;
        }

        /**
         * Adds a <code>String</code> representation
         * of a value to this column object's {@link #values} list.
         * These values will come from each row of a {@link ResultSet} of a database query.
         *
         * @param value
         *            The column value to add to {@link #values}
         */
        public void addValue(String value) {
            values.add(value);
        }

        /**
         * Returns the value of the {@link #justifyFlag}. The column
         * values will be formatted using <code>String.format</code> and
         * this flag will be used to right or left justify the text.
         *
         * @return The {@link #justifyFlag} of this column
         * @see #justifyLeft()
         */
        public String getJustifyFlag() {
            return justifyFlag;
        }

        /**
         * Returns the column label
         *
         * @return Column label
         */
        public String getLabel() {
            return label;
        }

        /**
         * Returns the generic SQL type of the column
         *
         * @return Generic SQL type
         */
        public int getType() {
            return type;
        }

        /**
         * Returns the generic SQL type category of the column
         *
         * @return The {@link #typeCategory} of the column
         */
        public int getTypeCategory() {
            return typeCategory;
        }

        /**
         * Returns the generic SQL type name of the column
         *
         * @return Generic SQL type name
         */
        public String getTypeName() {
            return typeName;
        }

        /**
         * Returns the column value at row index <code>i</code>.
         *
         * @param i
         *            The index of the column value to get
         * @return The String representation of the value
         */
        public String getValue(int i) {
            return values.get(i);
        }

        /**
         * Returns the width of the column
         *
         * @return Column width
         */
        public int getWidth() {
            return width;
        }

        /**
         * Sets {@link #justifyFlag} to <code>"-"</code> so that
         * the column value will be left justified when printed with
         * <code>String.format</code>. Typically numbers will be right
         * justified and text will be left justified.
         */
        public void justifyLeft() {
            this.justifyFlag = "-";
        }

        /**
         * Sets the {@link #typeCategory} of the column
         *
         * @param typeCategory
         *            The type category
         */
        public void setTypeCategory(int typeCategory) {
            this.typeCategory = typeCategory;
        }

        /**
         * Sets the width of the column to <code>width</code>
         *
         * @param width
         *            Width of the column
         */
        public void setWidth(int width) {
            this.width = width;
        }
    }

    private static Logger log = LogManager.getLogger();

    /**
     * Default maximum width for text columns
     * (like a <code>VARCHAR</code>) column.
     */
    private static final int DEFAULT_MAX_TEXT_COL_WIDTH = 50;

    /**
     * Column type category for <code>CHAR</code>, <code>VARCHAR</code>
     * and similar text columns.
     */
    public static final int CATEGORY_STRING = 1;

    /**
     * Column type category for <code>TINYINT</code>, <code>SMALLINT</code>,
     * <code>INT</code> and <code>BIGINT</code> columns.
     */
    public static final int CATEGORY_INTEGER = 2;

    /**
     * Column type category for <code>REAL</code>, <code>DOUBLE</code>,
     * and <code>DECIMAL</code> columns.
     */
    public static final int CATEGORY_DOUBLE = 3;

    /**
     * Column type category for date and time related columns like
     * <code>DATE</code>, <code>TIME</code>, <code>TIMESTAMP</code> etc.
     */
    public static final int CATEGORY_DATETIME = 4;

    /**
     * Column type category for <code>BOOLEAN</code> columns.
     */
    public static final int CATEGORY_BOOLEAN = 5;

    /**
     * Column type category for types for which the type name
     * will be printed instead of the content, like <code>BLOB</code>,
     * <code>BINARY</code>, <code>ARRAY</code> etc.
     */
    public static final int CATEGORY_OTHER = 0;

    /**
     * Returns a {@link ResultSet} in string form.
     *
     * @param rs
     *            the <code>ResultSet</code>
     * @return the <code>ResultSet</code> in string form
     */
    public static String getFormattedResultSet(ResultSet rs) {
        return getFormattedResultSet(rs, DEFAULT_MAX_TEXT_COL_WIDTH);
    }

    /**
     * Returns a {@link ResultSet} in string form.
     *
     * @param rs
     *            the <code>ResultSet</code>
     * @param maxStringColWidth
     *            the maximum column width for string fields
     * @return the <code>ResultSet</code> in string form
     */
    public static String getFormattedResultSet(ResultSet rs, int maxStringColWidth) {
        String retStr = "";
        String lineSeparator = System.getProperty("line.separator");

        try {

            if (rs == null) {
                retStr = "Result set is null";
                log.error(retStr);
                return retStr;
            }

            if (rs.isClosed()) {
                retStr = "Result set is closed";
                log.error(retStr);
                return retStr;
            }

            if (maxStringColWidth < 1) {
                log.warn("Invalid max string column width; using default");
                maxStringColWidth = DEFAULT_MAX_TEXT_COL_WIDTH;
            }

            // Get the meta data object of this ResultSet.
            ResultSetMetaData rsmd;
            rsmd = rs.getMetaData();

            // Total number of columns in this ResultSet
            int columnCount = rsmd.getColumnCount();

            // List of Column objects to store each column of the ResultSet
            // and the String representation of their values.
            List<Column> columns = new ArrayList<>(columnCount);

            // List of table names. Can be more than one if it is a joined
            // table query
            List<String> tableNames = new ArrayList<>(columnCount);

            // Get the columns and their meta data.
            // NOTE: columnIndex for rsmd.getXXX methods STARTS AT 1 NOT 0
            for (int i = 1; i <= columnCount; i++) {
                Column c = new Column(rsmd.getColumnLabel(i), rsmd.getColumnType(i), rsmd.getColumnTypeName(i));
                c.setWidth(c.getLabel().length());
                c.setTypeCategory(whichCategory(c.getType()));
                columns.add(c);

                if (!tableNames.contains(rsmd.getTableName(i))) {
                    tableNames.add(rsmd.getTableName(i));
                }
            }

            // Go through each row, get values of each column and adjust column widths.
            int rowCount = 0;
            while (rs.next()) {

                // NOTE: columnIndex for rs.getXXX methods STARTS AT 1 NOT 0
                for (int i = 0; i < columnCount; i++) {
                    Column c = columns.get(i);
                    String value;
                    int category = c.getTypeCategory();

                    if (category == CATEGORY_OTHER) {
                        // Use generic SQL type name instead of the actual value for column types BLOB, BINARY etc.
                        value = "(" + c.getTypeName() + ")";
                    } else {
                        value = rs.getString(i + 1) == null ? "NULL" : rs.getString(i + 1);
                    }

                    switch (category) {
                        case CATEGORY_DOUBLE:

                            // For real numbers, format the string value to have 3 digits
                            // after the point. THIS IS TOTALLY ARBITRARY and can be
                            // improved to be CONFIGURABLE.
                            if (!value.equals("NULL")) {
                                Double dValue = rs.getDouble(i + 1);
                                value = String.format("%.3f", dValue);
                            }
                            break;

                        case CATEGORY_STRING:

                            // Left justify the text columns
                            c.justifyLeft();

                            // Replace newlines with spaces
                            value = value.replace(lineSeparator, " ");

                            // and apply the width limit
                            if (value.length() > maxStringColWidth) {
                                value = value.substring(0, maxStringColWidth - 3) + "...";
                            }
                            break;
                    }

                    // Adjust the column width
                    c.setWidth(value.length() > c.getWidth() ? value.length() : c.getWidth());
                    c.addValue(value);
                } // END of for loop columnCount
                rowCount++;

            } // END of while (rs.next)

            /*
             * At this point we have gone through meta data, gotten the
             * columns and created all Column objects, iterated over the
             * ResultSet rows, populated the column values and adjusted
             * the column widths.
             * 
             * We cannot start formatting just yet because we have to prepare
             * a row separator String.
             */

            StringBuilder formattedStr = new StringBuilder();
            StringBuilder horizontalLine = new StringBuilder();

            /*
             * Prepare column labels to print as well as the row separator.
             * It should look something like this:
             * +--------+------------+------------+-----------+ (row separator)
             * | EMP_NO | BIRTH_DATE | FIRST_NAME | LAST_NAME | (labels row)
             * +--------+------------+------------+-----------+ (row separator)
             */

            // Iterate over columns
            for (Column c : columns) {
                int width = c.getWidth();

                // Center the column label
                String toPrint;
                String name = c.getLabel();
                int diff = width - name.length();

                if ((diff % 2) == 1) {
                    // diff is not divisible by 2, add 1 to width (and diff)
                    // so that we can have equal padding to the left and right
                    // of the column label.
                    width++;
                    diff++;
                    c.setWidth(width);
                }

                int paddingSize = diff / 2;

                // Cool String repeater code thanks to user102008 at stackoverflow.com
                // (http://tinyurl.com/7x9qtyg) "Simple way to repeat a string in java"
                String padding = new String(new char[paddingSize]).replace("\0", " ");

                toPrint = "| " + padding + name + padding + " ";
                // END centering the column label

                formattedStr.append(toPrint);

                horizontalLine.append("+");
                horizontalLine.append(new String(new char[width + 2]).replace("\0", "-"));
            }

            horizontalLine.append("+").append(lineSeparator);

            formattedStr.append("|").append(lineSeparator);
            formattedStr.insert(0, horizontalLine);
            formattedStr.append(horizontalLine);

            StringJoiner sj = new StringJoiner(", ");
            for (String name : tableNames) {
                sj.add(name);
            }

            String info = "Printing " + rowCount;
            info += String.format(
                " row%s from table%s ",
                rowCount > 1 || rowCount == 0 ? "s" : "",
                tableNames.size() > 1 ? "s" : "");
            info += sj.toString();

            retStr += info + lineSeparator;

            // Add the formatted column labels
            retStr += formattedStr.toString();

            String format;

            // Format the rows
            for (int i = 0; i < rowCount; i++) {
                for (Column c : columns) {

                    // This should form a format string like: "%-60s"
                    format = String.format("| %%%s%ds ", c.getJustifyFlag(), c.getWidth());
                    retStr += String.format(format, c.getValue(i));
                }

                retStr += "|" + lineSeparator;
            }

            retStr += horizontalLine;

            // @formatter:off
            /* 
             * Hopefully this should have printed something like this:
             * +--------+------------+------------+-----------+--------+------------+
             * | EMP_NO | BIRTH_DATE | FIRST_NAME | LAST_NAME | GENDER | HIRE_DATE  |
             * +--------+------------+------------+-----------+--------+------------+
             * | 10001  | 1953-09-02 | Georgi     | Facello   | M      | 1986-06-26 |
             * | 10002  | 1964-06-02 | Bezalel    | Simmel    | F      | 1985-11-21 |
             * +--------+------------+------------+-----------+--------+------------+
             */
            // @formatter:on

        } catch (SQLException e) {
            retStr = String.format("Error processing ResultSet: %s", e);
            log.error(retStr);
            return retStr;
        }
        // On the last time through, remove the last lineSeparator
        return retStr.substring(0, retStr.length() - lineSeparator.length());
    }

    /**
     * Takes a generic SQL type and returns the category this type
     * belongs to. Types are categorized according to print formatting
     * needs:
     * <p>
     * Integers should not be truncated so column widths should
     * be adjusted without a column width limit. Text columns should be
     * left justified and can be truncated to a max. column width etc...
     * </p>
     *
     * See also: <a target="_blank"
     * href="http://docs.oracle.com/javase/8/docs/api/java/sql/Types.html">
     * java.sql.Types</a>
     *
     * @param type
     *            Generic SQL type
     * @return The category this type belongs to
     */
    private static int whichCategory(int type) {
        switch (type) {
            case Types.BIGINT:
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                return CATEGORY_INTEGER;

            case Types.REAL:
            case Types.DOUBLE:
            case Types.DECIMAL:
                return CATEGORY_DOUBLE;

            case Types.DATE:
            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return CATEGORY_DATETIME;

            case Types.BOOLEAN:
                return CATEGORY_BOOLEAN;

            case Types.VARCHAR:
            case Types.NVARCHAR:
            case Types.LONGVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.CHAR:
            case Types.NCHAR:
                return CATEGORY_STRING;

            default:
                return CATEGORY_OTHER;
        }
    }
}
