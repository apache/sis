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
package org.apache.sis.storage.geotiff.reader;

import java.util.Objects;
import java.util.Locale;
import java.text.Format;
import java.text.NumberFormat;
import org.apache.sis.math.Vector;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.internal.shared.TableRowList;


/**
 * The color map represented as a table. Used only for native metadata representation.
 * In a TIFF color map, all the Red values come first, followed by all Green values, then all Blue values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Jonatas Fischer
 */
public final class ColorMap extends TableRowList<Vector, Integer> {
    /**
     * Number of values per color.
     */
    private static final int RECORD_LENGTH = 3;
    /**
     * Values of colors read from GeoTIFF file.
     */
    private final Vector values;

    /**
     * Number of colors, saved because frequently used.
     */
    private final int size;

    /**
     * Creates a new color map.
     *
     * @param  values  values of colors read from GeoTIFF file.
     */
    public ColorMap(final Vector values) {
        this.values = values;
        size = values.size() / RECORD_LENGTH;
    }

    /**
     * Returns the number of colors.
     *
     * @return number of colors.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Returns the column headers.
     * The returned array length is {@link #RECORD_LENGTH}.
     */
    @Override
    public String[] columns(final Locale locale) {
        final Vocabulary vocabulary = Vocabulary.forLocale(locale);
        return new String[] {
            vocabulary.getString(Vocabulary.Keys.Red),
            vocabulary.getString(Vocabulary.Keys.Green),
            vocabulary.getString(Vocabulary.Keys.Blue)
        };
    }

    /**
     * Returns the value in the specified row and column.
     */
    @Override
    public Integer get(final int row, final int column) {
        return values.intValue(size * Objects.checkIndex(column, RECORD_LENGTH) + Objects.checkIndex(row, size));
    }

    /**
     * Returns the colors in the given row.
     */
    @Override
    public Vector get(final int row) {
        return values.subSampling(Objects.checkIndex(row, size), size, RECORD_LENGTH);
    }

    /**
     * Returns the format to use for the given column.
     *
     * @param  locale  the locale of the format to create.
     * @param  column  the column for which to get a format.
     */
    @Override
    public Format createFormat(final Locale locale, final int column) {
        return NumberFormat.getIntegerInstance(locale);
    }
}
