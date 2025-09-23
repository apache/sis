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

import java.text.Format;
import java.util.Map;
import java.util.Arrays;
import java.util.Currency;
import java.util.Locale;
import java.util.TimeZone;
import java.io.IOException;
import java.nio.charset.Charset;
import org.opengis.util.Type;
import org.opengis.util.Record;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.io.CompoundFormat;
import org.apache.sis.io.LineAppender;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.Localized;
import org.apache.sis.util.resources.Vocabulary;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.util.ControlledVocabulary;


/**
 * Creates string representation of property values of unknown type.
 * Tabulations are replaced by spaces, and line feeds can optionally
 * be replaced by the Pilcrow character.
 *
 * Subclasses need to override {@link #getLocale()}, and should also override {@link #toString(Object)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class PropertyFormat extends LineAppender implements Localized {
    /**
     * The string to insert for missing values.
     */
    private static final String MISSING = " ";

    /**
     * The format for the column in process of being written. This is a format to use for the column as a whole.
     * This field is updated for every new column to write. May be {@code null} if the format is unspecified.
     */
    protected transient Format columnFormat;

    /**
     * Creates a new instance which will write to the given appendable.
     *
     * @param  out  where to format the objects.
     */
    protected PropertyFormat(final Appendable out) {
        super(out);
    }

    /**
     * Appends a textual representation of the given value.
     *
     * @param  value  the value to format (may be {@code null}).
     * @throws IOException if an error occurred while writing the value.
     */
    public final void appendValue(final Object value) throws IOException {
        appendValue(value, false);
    }

    /**
     * Appends a textual representation of the given value, with a check for nested collections.
     *
     * @param  value      the value to format (may be {@code null}).
     * @param  recursive  {@code true} if this method is invoking itself for writing collection values.
     */
    private void appendValue(final Object value, final boolean recursive) throws IOException {
        final CharSequence text;
        if (value == null) {
            text = MISSING;
        } else if (columnFormat != null) {
            if (columnFormat instanceof CompoundFormat<?>) {
                appendCompound((CompoundFormat<?>) columnFormat, value);
                return;
            }
            text = columnFormat.format(value);
        } else if (value instanceof InternationalString) {
            text = freeText(((InternationalString) value).toString(getLocale()));
        } else if (value instanceof CharSequence) {
            text = freeText(value.toString());
        } else if (value instanceof ControlledVocabulary) {
            text = MetadataServices.getInstance().getCodeTitle((ControlledVocabulary) value, getLocale());
        } else if (value instanceof Boolean) {
            text = Vocabulary.forLocale(getLocale()).getString((Boolean) value ? Vocabulary.Keys.True : Vocabulary.Keys.False);
        } else if (value instanceof Enum<?>) {
            text = CharSequences.upperCaseToSentence(((Enum<?>) value).name());
        } else if (value instanceof Type) {
            appendName(((Type) value).getTypeName());
            return;
        } else if (value instanceof Locale) {
            final Locale locale = getLocale();
            text = (locale != Locale.ROOT) ? ((Locale) value).getDisplayName(locale) : value.toString();
        } else if (value instanceof TimeZone) {
            final Locale locale = getLocale();
            text = (locale != Locale.ROOT) ? ((TimeZone) value).getDisplayName(locale) : ((TimeZone) value).getID();
        } else if (value instanceof Charset) {
            final Locale locale = getLocale();
            text = (locale != Locale.ROOT) ? ((Charset) value).displayName(locale) : ((Charset) value).name();
        } else if (value instanceof Currency) {
            final Locale locale = getLocale();
            text = (locale != Locale.ROOT) ? ((Currency) value).getDisplayName(locale) : value.toString();
        } else if (value instanceof Record) {
            appendCollection(((Record) value).getFields().values(), recursive);
            return;
        } else if (value instanceof Iterable<?>) {
            appendCollection((Iterable<?>) value, recursive);
            return;
        } else if (value instanceof Object[]) {
            appendCollection(Arrays.asList((Object[]) value), recursive);
            return;
        } else if (value instanceof Map.Entry<?,?>) {
            final Map.Entry<?,?> entry = (Map.Entry<?,?>) value;
            final Object k = entry.getKey();
            final Object v = entry.getValue();
            if (k == null) {
                append(null);
            } else {
                appendValue(k, recursive);
            }
            if (v != null) {
                append(" → ");
                appendValue(v, recursive);
            }
            return;
        } else {
            text = toString(value);
        }
        append(text);
    }

    /**
     * Invoked by {@link PropertyFormat} for formatting a value which has not been recognized as one of the types
     * to be handled in a special way. Some of the types handled in a special way are {@link InternationalString},
     * {@link ControlledVocabulary}, {@link Enum}, {@link Type}, {@link Locale}, {@link TimeZone}, {@link Charset},
     * {@link Currency}, {@link Record}, {@link Iterable} and arrays. Other types should be handled by this method.
     * In particular, {@link Number}, {@link java.util.Date} and {@link org.apache.sis.measure.Angle}
     * are <strong>not</strong> handled by default by this {@link PropertyFormat} class and should be handled here.
     *
     * @param  value  the value to format (never {@code null}).
     * @return the formatted value.
     */
    protected String toString(final Object value) {
        return freeText(value.toString());
    }

    /**
     * Invoked after formatting a text that could be anything. It current version, it includes all kinds of
     * {@link CharSequence} including {@link InternationalString}, together with {@link Object#toString()}
     * values computed by the default {@link #toString(Object)} implementation.
     *
     * The default {@code freeText(…)} implementation removes white space and control characters.
     * Subclasses can override for example for making a text shorter.
     *
     * @param  text  the free text, or {@code null}.
     * @return the text to append.
     */
    protected String freeText(final String text) {
        // Really want `trim()` because there is sometimes control characters to remove.
        return (text != null) ? text.trim() : MISSING;
    }

    /**
     * Writes the values of the given collection. A maximum of 10 values will be written.
     * If the collection contains other collections, the other collections will <strong>not</strong>
     * be written recursively.
     */
    private void appendCollection(final Iterable<?> values, final boolean recursive) throws IOException {
        if (values != null) {
            if (recursive) {
                append('…');                                // Do not format collections inside collections.
            } else {
                int count = 0;
                for (final Object value : values) {
                    if (value != null) {
                        if (count != 0) append(", ");
                        appendValue(value, true);
                        if (++count == 10) {                // Arbitrary limit.
                            append(", …");
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Workaround for the inability to define the variable {@code <V>} locally.
     */
    @Workaround(library="JDK", version="1.7")
    private <V> void appendCompound(final CompoundFormat<V> format, final Object value) throws IOException {
        format.format(format.getValueType().cast(value), this);
    }

    /**
     * Localizes the given name in the display locale, or formats "(Unnamed)" if no localized value is found.
     */
    private void appendName(final GenericName name) throws IOException {
        final Locale locale = getLocale();
        if (name != null) {
            final InternationalString i18n = name.toInternationalString();
            if (i18n != null) {
                final String localized = i18n.toString(locale);
                if (localized != null) {
                    append(localized);
                    return;
                }
            }
            final String localized = name.toString();
            if (localized != null) {
                append(localized);
                return;
            }
        }
        append('(').append(Vocabulary.forLocale(locale).getString(Vocabulary.Keys.Unnamed)).append(')');
    }
}
