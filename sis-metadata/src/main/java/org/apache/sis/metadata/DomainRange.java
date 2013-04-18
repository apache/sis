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
package org.apache.sis.metadata;

import java.util.Locale;
import org.opengis.util.InternationalString;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.RangeFormat;
import org.apache.sis.measure.ValueRange;


/**
 * The range of values that a metadata property can take, representable as an {@link InternationalString}
 * in order to make possible to return this range from the {@link PropertyInformation#getDomainValue()}
 * method.
 *
 * @param <E> The type of range elements as a subclass of {@link Number}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see PropertyInformation#getDomainValue()
 */
final class DomainRange<E extends Number & Comparable<? super E>> extends NumberRange<E>
        implements InternationalString
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 702771264296112914L;

    /**
     * The locale-independent string representation,
     * built by {@link #toString()} when first needed.
     */
    private transient volatile String text;

    /**
     * Constructs a range of the given type with values from the given annotation.
     * This constructor does not verify if the given type is wide enough for the values of
     * the given annotation, because those information are usually static. If nevertheless
     * the given type is not wide enough, then the values are truncated in the same way
     * than the Java language casts primitive types.
     *
     * @param  type  The element type, restricted to one of {@link Byte}, {@link Short},
     *               {@link Integer}, {@link Long}, {@link Float} or {@link Double}.
     * @param  range The range of values.
     * @throws IllegalArgumentException If the given type is not one of the primitive
     *         wrappers for numeric types.
     */
    DomainRange(final Class<E> type, final ValueRange range) throws IllegalArgumentException {
        super(type, range);
    }

    /**
     * Returns the string representation in the given locale.
     */
    @Override
    public String toString(final Locale locale) {
        if (locale == null || locale == Locale.ROOT) {
            return toString();
        }
        final RangeFormat format = new RangeFormat(locale, getElementType());
        return format.format(this);
    }

    /**
     * Builds, caches and returns the unlocalized string representation of this range.
     */
    @Override
    public String toString() {
        String s = text;
        if (s == null) {
            text = s = super.toString();
        }
        return s;
    }

    /**
     * Returns the length of the unlocalized string.
     */
    @Override
    public int length() {
        return toString().length();
    }

    /**
     * Returns the character at the given index in the unlocalized string.
     */
    @Override
    public char charAt(final int index) {
        return toString().charAt(index);
    }

    /**
     * Returns a subsequence of the unlocalized string representation.
     */
    @Override
    public CharSequence subSequence(final int start, final int end) {
        return toString().subSequence(start, end);
    }

    /**
     * Compares the unlocalized string representations. In the special case where the other instance
     * is a {@code DomainRange}, actually compares the numerical values for better ordering.
     */
    @Override
    public int compareTo(final InternationalString o) {
        if (o instanceof DomainRange) {
            return compareTo((DomainRange<?>) o);
        }
        return toString().compareTo(o.toString());
    }

    /**
     * Compares this range with the given range object for ordering of minimal values, or maximal values
     * if two ranges have the same minimum value.
     *
     * <p>Notes:</p>
     * <ul>
     *   <li>This method requires {@code DomainRange} instance rather than more generic {@code NumberRange}
     *       in order to ensure reciprocity: {@code A.compareTo(B) == -B.compareTo(A)}.</li>
     *   <li>This ordering is appropriate for {@code DomainRange} because it is close to the ordering
     *       of their string representations, but is otherwise not provided for general {@code Range}
     *       objects because the ordering criterion would be arbitrary for them (what would be the
     *       best ordering of overlapping ranges?).</li>
     * </ul>
     */
    private int compareTo(final DomainRange<?> range) {
        int c = Double.compare(getMinDouble(), range.getMinDouble());
        if (c == 0) {
            boolean b = isMinIncluded();
            if (b != range.isMinIncluded()) {
                c = b ? -1 : +1;
            } else {
                c = Double.compare(getMaxDouble(), range.getMaxDouble());
                if (c == 0) {
                    b = isMaxIncluded();
                    if (b != range.isMaxIncluded()) {
                        c = b ? +1 : -1;
                    }
                }
            }
        }
        return c;
    }
}
