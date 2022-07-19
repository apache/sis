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
package org.apache.sis.measure;

import java.util.Locale;
import java.text.Format;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.format.ParserException;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.util.FinalFieldSetter;

import static java.util.logging.Logger.getLogger;


/**
 * Parses and formats numbers with units of measurement.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see NumberFormat
 * @see UnitFormat
 *
 * @since 1.1
 * @module
 */
public class QuantityFormat extends Format {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1014042719969477503L;

    /**
     * The default separator used between numerical value and its unit of measurement.
     * Current value is narrow no-break space (U+202F).
     */
    public static final char SEPARATOR = '\u202F';

    /**
     * The format for parsing and formatting the number part.
     */
    protected final NumberFormat numberFormat;

    /**
     * The format for parsing and formatting the unit of measurement part.
     */
    protected final UnitFormat unitFormat;

    /**
     * Creates a new instance for the given locale.
     *
     * @param  locale  the locale for the quantity format.
     */
    public QuantityFormat(final Locale locale) {
        ArgumentChecks.ensureNonNull("locale", locale);
        numberFormat = NumberFormat.getNumberInstance(locale);
        unitFormat   = new UnitFormat(locale);
    }

    /**
     * Creates a new instance using the given number and unit formats.
     *
     * @param  numberFormat  the format for parsing and formatting the number part.
     * @param  unitFormat    the format for parsing and formatting the unit of measurement part.
     */
    public QuantityFormat(final NumberFormat numberFormat, final UnitFormat unitFormat) {
        ArgumentChecks.ensureNonNull("numberFormat", numberFormat);
        ArgumentChecks.ensureNonNull("unitFormat",   unitFormat);
        this.numberFormat = numberFormat;
        this.unitFormat   = unitFormat;
    }

    /**
     * Formats the specified quantity in the given buffer.
     * The given object shall be a {@link Quantity} instance.
     *
     * @param  quantity    the quantity to format.
     * @param  toAppendTo  where to format the quantity.
     * @param  pos         where to store the position of a formatted field, or {@code null} if none.
     * @return the given {@code toAppendTo} argument, for method calls chaining.
     */
    @Override
    public StringBuffer format(final Object quantity, StringBuffer toAppendTo, FieldPosition pos) {
        final Quantity<?> q = (Quantity<?>) quantity;
        if (pos == null) pos = new FieldPosition(0);
        toAppendTo = numberFormat.format(q.getValue(), toAppendTo, pos).append(SEPARATOR);   // Narrow no-break space.
        toAppendTo = unitFormat.format(q.getUnit(), toAppendTo, pos);
        return toAppendTo;
    }

    /**
     * Parses text from a string to produce a quantity, or returns {@code null} if the parsing failed.
     *
     * @param  source  the text, part of which should be parsed.
     * @param  pos     index and error index information.
     * @return a quantity parsed from the string, or {@code null} in case of error.
     */
    @Override
    public Object parseObject(final String source, final ParsePosition pos) {
        final int start = pos.getIndex();
        final Number value = numberFormat.parse(source, pos);
        if (value != null) {
            try {
                final Unit<?> unit = unitFormat.parse(source, pos);
                if (unit != null) {
                    return Quantities.create(value.doubleValue(), unit);
                }
            } catch (ParserException e) {
                Logging.ignorableException(getLogger(Loggers.MEASURE), QuantityFormat.class, "parseObject", e);
            }
            pos.setIndex(start);        // By `Format.parseObject(…)` method contract.
        }
        return null;
    }

    /**
     * Returns a clone of this format.
     *
     * @return a clone of this format.
     */
    @Override
    public QuantityFormat clone() {
        final QuantityFormat clone = (QuantityFormat) super.clone();
        try {
            FinalFieldSetter.set(QuantityFormat.class, "numberFormat", "unitFormat",
                                 clone, numberFormat.clone(), unitFormat.clone());
        } catch (ReflectiveOperationException e) {
            throw FinalFieldSetter.cloneFailure(e);
        }
        return clone;
    }
}
