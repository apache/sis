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
package org.apache.sis.gui.metadata;

import java.util.Date;
import java.util.Locale;
import java.text.Format;
import java.text.DateFormat;
import javax.measure.Unit;
import org.apache.sis.measure.UnitFormat;
import org.apache.sis.gui.internal.PropertyValueFormats;


/**
 * A provider for {@link java.text.NumberFormat}, {@link java.text.DateFormat}, <i>etc</i>.
 * This format uses verbose patterns, for example full dates with day of weeks.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 */
@SuppressWarnings({"serial","CloneableImplementsClone"})            // Not intended to be serialized.
final class VerboseFormats extends PropertyValueFormats {
    /**
     * Creates a new format for the given locale.
     *
     * @param  locale  the locale to use for formatting objects.
     */
    VerboseFormats(final Locale locale) {
        super(locale);
    }

    /**
     * Creates a new format to use for formatting values of the given type.
     * This method is invoked by {@link #getFormat(Class)} the first time
     * that a format is needed for the given type.
     *
     * @param  valueType  the base type of values to parse or format.
     * @return the format to use for parsing of formatting values of the given type.
     */
    @Override
    protected Format createFormat(final Class<?> valueType) {
        if (valueType == Date.class) {
            return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, getLocale());
        }
        if (valueType == Unit.class) {
            UnitFormat f = new UnitFormat(getLocale());
            f.setStyle(UnitFormat.Style.NAME);
            return f;
        }
        return super.createFormat(valueType);
    }
}
