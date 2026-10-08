/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.util.internal.shared;

import java.util.AbstractList;
import java.text.Format;
import java.util.Locale;
import org.apache.sis.io.TableAppender;


/**
 * A list of row items (for example records) which can be separated in finer items (field values).
 * This class is not used directly by the utility module, but is defined for allowing transfer of
 * information between (for example) GeoTIFF and <abbr>GUI</abbr> modules.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <E>  type of row items in the list.
 * @param  <V>  type of value in columns.
 */
public abstract class TableRowList<E, V> extends AbstractList<E> {
    /**
     * Creates a new list.
     */
    protected TableRowList() {
    }

    /**
     * Returns the title of each column, in order.
     * The length of this array is the number of columns.
     *
     * @return the column titles.
     */
    public abstract String[] columns();

    /**
     * Returns the value in the given column of the given row.
     *
     * @param  row     the row, from 0 inclusive to {@link #size()} exclusive.
     * @param  column  the column, from 0 inclusive to {@code columns().length} exclusive.
     * @return value in the given column of the given row.
     * @throws IndexOutOfBoundsException if the row of column index is invalid.
     */
    public abstract V get(int row, int column);

    /**
     * Returns the format to use for the given column (optional).
     *
     * @param  locale  the locale of the format to create.
     * @param  column  the column for which to get a format.
     * @return format configured for the given column, or {@code null} if none.
     */
    public Format createFormat(Locale locale, int column) {
        return null;
    }

    /**
     * Formats this list as a table.
     *
     * @return a string representation of this list formatted as a table.
     */
    @Override
    public String toString() {
        final var locale = Locale.getDefault();
        final var table = new TableAppender();
        final String[] columns = columns();
        final Format[] formats = new Format[columns.length];
        table.setCellAlignment(TableAppender.ALIGN_RIGHT);
        table.setMultiLinesCells(true);
        table.nextLine('═');
        for (int column = 0; column < columns.length; column++) {
            formats[column] = createFormat(locale, column);
            table.append(columns[column]);
            table.nextColumn();
        }
        table.nextLine();
        table.nextLine('─');
        final int size = size();
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < columns.length; column++) {
                table.append(formats[column].format(get(row, column)));
                table.nextColumn();
            }
            table.nextLine();
        }
        table.nextLine('═');
        return table.toString();
    }
}
