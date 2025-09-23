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
import org.apache.sis.util.Classes;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.internal.shared.PropertyFormat;


/**
 * Creates string representation of property values of unknown type.
 * Tabulations are replaced by spaces and line feeds are replaced by the Pilcrow character.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class PropertyValueFormatter extends PropertyFormat {
    /**
     * The formats to use for objects. Its locale is usually {@link Locale#getDefault()}.
     * The locale is also given to {@link InternationalString#toString(Locale)} calls.
     */
    final PropertyValueFormats formats;

    /**
     * Creates a formatter for the specified locale.
     *
     * @param  locale  the locale to use for texts, numbers and dates.
     * @param  buffer  where to format the objects.
     */
    public PropertyValueFormatter(final Appendable buffer, final Locale locale) {
        super(buffer);
        setLineSeparator(" ¶ ");
        formats = new PropertyValueFormats(locale);
    }

    /**
     * The locale to use for formatting textual content.
     */
    @Override
    public final Locale getLocale() {
        return formats.getLocale();
    }

    /**
     * Invoked by {@link PropertyFormat} for formatting a value which has not been recognized as one of
     * the types to be handled in a special way. In particular numbers and dates should be handled here.
     */
    @Override
    protected String toString(final Object value) {
        String text = formats.formatValue(value, false);
        if (text != null) {
            return text;
        }
        return Classes.getShortClassName(value) + "(…)";
    }

    /**
     * Invoked by {@link PropertyFormat} when the property value is any kind of {@link CharSequence}.
     * This method applies an arbitrary limit for avoiding too long texts.
     *
     * @param  text  the text.
     * @return the text potentially truncated if too long.
     */
    @Override
    protected String freeText(final String text) {
        return CharSequences.shortSentence(super.freeText(text), 100).toString();
    }
}
