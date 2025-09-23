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
package org.apache.sis.io;

import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import java.io.ObjectStreamException;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.internal.shared.LocalizedParseException;


/**
 * Created by {@link CompoundFormat} for parsing and formatting unlocalized numbers.
 * This implementation use {@code toString()} and {@code valueOf(…)} methods instead
 * than the {@link java.text} package because the former provide the best guarantees
 * to format all significant digits.
 *
 * <h2>Thread safety</h2>
 * The same {@linkplain #getInstance instance} can be safely used by many threads without synchronization
 * on the part of the caller. Note that this is specific to {@code DefaultFormat} and generally not true
 * for arbitrary {@code Format} classes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class DefaultFormat extends Format {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2309270763519060316L;

    /**
     * The array for storing singleton instances for types {@code byte} to {@code double}.
     * The value at index 0 is reserved for the generic {@link Number} type.
     */
    private static final Format[] INSTANCES = new Format[Numbers.DOUBLE - Numbers.BYTE + 2];

    /**
     * The type of the objects to parse.
     */
    private final Class<?> type;

    /**
     * Gets an instance of the given type, or {@code null} if the given type is not supported.
     */
    static Format getInstance(final Class<?> type) {
        final int index;
        if (type == Number.class) {
            index = 0;
        } else {
            index = Numbers.getEnumConstant(type) - (Numbers.BYTE - 1);
            if (index < 0 || index >= INSTANCES.length) {
                return null;
            }
        }
        synchronized (INSTANCES) {
            Format format = INSTANCES[index];
            if (format == null) {
                INSTANCES[index] = format = new DefaultFormat(type);
            }
            return format;
        }
    }

    /**
     * Creates a new instance for parsing and formatting objects of the given type.
     */
    private DefaultFormat(final Class<?> type) {
        this.type = type;
    }

    /**
     * Formats the given number using its {@link Object#toString()} method.
     */
    @Override
    public StringBuffer format(final Object obj, final StringBuffer toAppendTo, final FieldPosition pos) {
        return toAppendTo.append(obj);
    }

    /**
     * Parses the given string. Callers shall catch the {@link NumberFormatException}
     * and handle the error according the caller's method contract.
     *
     * @throws NumberFormatException if the parsing failed.
     */
    private Object valueOf(final String source) throws NumberFormatException {
        return (type != Number.class) ? Numbers.valueOf(source, type) : Numbers.narrowestNumber(source);
    }

    /**
     * Parses the given string as a number of the type given at construction time.
     */
    @Override
    public Object parseObject(String source) throws ParseException {
        source = source.strip();
        try {
            return valueOf(source);
        } catch (NumberFormatException cause) {
            throw new LocalizedParseException(null, type, source, null).initCause(cause);
        }
    }

    /**
     * Parses the given string as a number of the type given at construction time.
     */
    @Override
    public Object parseObject(String source, final ParsePosition pos) {
        boolean exponent = false;
        final int index = CharSequences.skipLeadingWhitespaces(source, pos.getIndex(), source.length());
        int end;
        for (end = index; end < source.length(); end++) {
            final char c = source.charAt(end);
            switch (c) {
                default: {
                    if (c >= '+' && c <= '9') continue;
                    break;
                    /*
                     * ASCII characters in above range are +,-./0123456789
                     * But the , and / characters are excluded by the case below.
                     */
                }
                case ',': case '/': break;
                case 'E': case 'e': {
                    if (exponent) break;
                    exponent = true;
                    continue;
                }
            }
            break;
        }
        source = source.substring(index, end);
        final Object value;
        try {
            value = valueOf(source);
        } catch (NumberFormatException cause) {
            pos.setErrorIndex(index);
            return null;
        }
        pos.setIndex(end);
        return value;
    }

    /**
     * Unconditionally returns {@code this} since this format does not contain any modifiable field.
     * The same {@code DefaultFormat} instances can be shared.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Object clone() {
        return this;
    }

    /**
     * Resolves to the singleton instance on deserialization.
     */
    private Object readResolve() throws ObjectStreamException {
        final Format format = getInstance(type);
        return (format != null) ? format : this;
    }
}
