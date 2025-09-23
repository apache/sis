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
package org.apache.sis.gui.internal;

import java.util.Locale;
import java.util.TimeZone;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.io.CompoundFormat;


/**
 * A provider for {@link java.text.NumberFormat}, {@link java.text.DateFormat}, <i>etc</i>.
 * Used for formatting values in {@link PropertyValueFormatter} or in metadata summary.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings({"serial","CloneableImplementsClone"})            // Not intended to be serialized.
public class PropertyValueFormats extends CompoundFormat<Object> {
    /**
     * Creates a new format for the given locale.
     *
     * @param  locale  the locale to use for formatting objects.
     */
    protected PropertyValueFormats(final Locale locale) {
        super(locale, TimeZone.getDefault());
    }

    /**
     * Required by {@link CompoundFormat} but not used.
     *
     * @return the base type of values formatted by this {@code PropertyView} instance.
     */
    @Override
    public final Class<? extends Object> getValueType() {
        return Object.class;
    }

    /**
     * Unsupported operation.
     *
     * @param  text ignored.
     * @param  pos  ignored.
     * @return never return.
     * @throws ParseException always thrown.
     */
    @Override
    public final Object parse(CharSequence text, ParsePosition pos) throws ParseException {
        throw new ParseException(null, 0);
    }

    /**
     * Formats the given property value. Current implementation requires {@code toAppendTo}
     * to be an instance of {@link StringBuffer}. This method is not intended to be invoked
     * outside internal usage.
     *
     * @param  value       the property value to format.
     * @param  toAppendTo  where to append the property value.
     */
    @Override
    public final void format(final Object value, final Appendable toAppendTo) {
        final StringBuffer buffer = (StringBuffer) toAppendTo;
        final Format f = getFormat(value.getClass());
        if (f != null) {
            f.format(value, buffer, new FieldPosition(0));
        } else {
            buffer.append(value);
        }
    }

    /**
     * Formats a single value. This method does the same work as the inherited
     * {@link #format(Object)} final method but in a more efficient way.
     *
     * @param  value            the value to format.
     * @param  toStringAllowed  whether to fallback on {@link Object#toString()} if there is no format
     *         for the given object. If {@code false}, then {@code null} will be returned instead.
     * @return formatted string representation of the given value.
     */
    public final String formatValue(final Object value, final boolean toStringAllowed) {
        final Format f = getFormat(value.getClass());
        if (f == null) {
            return value.toString();
        } else if (value instanceof Number) {
            return Numerics.useScientificNotationIfNeeded(f, value, Format::format);
        } else {
            return f.format(value);
        }
    }

    /**
     * Formats a pair of values in the given buffer. If can be used for formatting a range
     * like "minimum … maximum" or for formatting a value with uncertainty like "mean ± std".
     *
     * @param  first       the first value of the range.
     * @param  separator   string to insert between the two values.
     * @param  second      the maximum value of the range.
     * @param  toAppendTo  where to append the property value.
     */
    public final void formatPair(final double first, final String separator, final double second, final StringBuffer toAppendTo) {
        final FieldPosition pos = new FieldPosition(0);
        final Format f = getFormat(Number.class);
        format(f, first,  toAppendTo, pos); toAppendTo.append(separator);
        format(f, second, toAppendTo, pos);
    }

    /**
     * Formats the given value, using scientific notation if needed.
     */
    private static void format(final Format f, final double value, final StringBuffer buffer, final FieldPosition pos) {
        Numerics.useScientificNotationIfNeeded(f, value, (nf,v) -> {nf.format(v, buffer, pos); return null;});
    }
}
