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
package org.apache.sis.parameter;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.lang.reflect.Array;
import java.io.IOException;
import javax.measure.unit.Unit;
import org.opengis.util.GenericName;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.util.InternationalString;
import org.opengis.util.NameSpace;
import org.apache.sis.internal.referencing.NameToIdentifier;
import org.apache.sis.internal.util.X364;

import static org.apache.sis.internal.util.X364.*;
import static org.apache.sis.util.CharSequences.spaces;


/**
 * A row in the table to be formatted by {@link ParameterFormat}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.00)
 * @version 0.4
 * @module
 */
final class ParameterTableRow {
    /**
     * The (<var>codespace(s)</var>, <var>name(s)</var>) entries for the identifier and all aliases
     * declared in the constructor. The codespace key may be null, but the name values shall never be null.
     *
     * <p>Values can be of two kinds:</p>
     * <ul>
     *   <li>{@link String} for names or aliases.</li>
     *   <li>{@link ReferenceIdentifier} for identifiers.</li>
     * </ul>
     */
    private final Map<String,Set<Object>> identifiers;

    /**
     * The values. Some elements in this list may be null.
     */
    final List<Object> values;

    /**
     * The units of measurement. The size of this list shall be the same than {@link #values}.
     * The list may contain null elements.
     */
    final List<Unit<?>> units;

    /**
     * The largest codespace width, in number of Unicode code points.
     */
    int codespaceWidth;

    /**
     * Creates a new row in a table to be formatted by {@link ParameterFormat}.
     *
     * @param object The object for which to get the (<var>codespace(s)</var>, <var>name(s)</var>).
     * @param locale The locale for formatting the names.
     */
    ParameterTableRow(final IdentifiedObject object, final Locale locale, final boolean brief) {
        values = new ArrayList<>(2); // In the vast majority of cases, we will have only one value.
        units  = new ArrayList<>(2);
        /*
         * Creates a collection which will contain the identifier and all aliases
         * found for the given IdentifiedObject. We begin with the primary name.
         */
        identifiers = new LinkedHashMap<>();
        final ReferenceIdentifier identifier = object.getName();
        addIdentifier(identifier.getCodeSpace(), identifier.getCode()); // Value needs to be a String here.
        if (!brief) {
            final Collection<GenericName> aliases = object.getAlias();
            if (aliases != null) { // Paranoiac check.
                for (GenericName alias : aliases) {
                    String codespace = NameToIdentifier.getCodeSpace(alias);
                    if (codespace != null) {
                        alias = alias.tip();
                    } else {
                        final NameSpace scope = alias.scope();
                        if (scope != null && !scope.isGlobal()) {
                            codespace = toString(scope.name().tip(), locale);
                        }
                    }
                    addIdentifier(codespace, toString(alias, locale));
                }
            }
            final Collection<? extends ReferenceIdentifier> ids = object.getIdentifiers();
            if (ids != null) { // Paranoiac check.
                for (final ReferenceIdentifier id : ids) {
                    addIdentifier(id.getCodeSpace(), id); // No .getCode() here.
                }
            }
        }
    }

    /**
     * Adds an identifier for the given code space.
     * As a side effect, this method remembers the length of the widest code space.
     */
    private void addIdentifier(final String codespace, final Object identifier) {
        if (codespace != null) {
            final int width = codespace.codePointCount(0, codespace.length());
            if (width > codespaceWidth) {
                codespaceWidth = width;
            }
        }
        Set<Object> ids = identifiers.get(codespace);
        if (ids == null) {
            ids = new LinkedHashSet<>(8);
            identifiers.put(codespace, ids);
        }
        ids.add(identifier);
    }

    /**
     * If the list has only one element and this element is an array or a collection, expands it.
     * This method shall be invoked only after the caller finished to add all elements in the
     * {@link #values} and {@link #units} lists.
     */
    final void expandSingleton() {
        assert values.size() == units.size();
        if (values.size() == 1) {
            Object value = values.get(0);
            if (value != null) {
                if (value instanceof Collection<?>) {
                    value = ((Collection<?>) value).toArray();
                }
                if (value.getClass().isArray()) {
                    final int length = Array.getLength(value);
                    final Unit<?> unit = units.get(0);
                    values.clear();
                    units.clear();
                    for (int i=0; i<length; i++) {
                        values.add(Array.get(value, i));
                        units.add(unit);
                    }
                }
            }
        }
    }

    /**
     * Writes the given color if {@code colorEnabled} is {@code true}.
     */
    private static void appendColor(final Appendable out, final X364 color, final boolean colorEnabled)
            throws IOException
    {
        if (colorEnabled) {
            out.append(color.sequence());
        }
    }

    /**
     * Writes the identifiers. At most one of {@code colorsForTitle} and {@code colorsForRows}
     * can be set to {@code true}.
     *
     * @param  out             Where to write.
     * @param  colorsForTitle  {@code true} if syntax coloring should be applied for table title.
     * @param  colorsForRows   {@code true} if syntax coloring should be applied for table rows.
     * @param  lineSeparator   The system-dependent line separator.
     * @throws IOException     If an exception occurred while writing.
     */
    final void appendIdentifiers(final Appendable out, final boolean colorsForTitle,
            final boolean colorsForRows, final String lineSeparator) throws IOException
    {
        boolean continuing = false;
        for (final Map.Entry<String,Set<Object>> entry : identifiers.entrySet()) {
            if (continuing) {
                out.append(lineSeparator);
            }
            continuing = true;
            int length = codespaceWidth + 1;
            final String authority  = entry.getKey();
            appendColor(out, FOREGROUND_GREEN, colorsForTitle);
            if (authority != null) {
                appendColor(out, FAINT, colorsForRows);
                out.append(authority);
                out.append(':');
                appendColor(out, NORMAL, colorsForRows);
                length -= authority.length();
            }
            out.append(spaces(length));
            appendColor(out, BOLD, colorsForTitle);
            final Iterator<Object> it = entry.getValue().iterator();
            out.append(toString(it.next()));
            appendColor(out, RESET, colorsForTitle);
            boolean hasMore = false;
            while (it.hasNext()) {
                out.append(hasMore ? ", " : " (");
                final Object id = it.next();
                final X364 color, normal;
                if (id instanceof ReferenceIdentifier) {
                    color  = FOREGROUND_YELLOW;
                    normal = FOREGROUND_DEFAULT;
                } else {
                    color  = FAINT;
                    normal = NORMAL;
                }
                appendColor(out, color, colorsForTitle);
                out.append(toString(id));
                appendColor(out, normal, colorsForTitle);
                hasMore = true;
            }
            if (hasMore) {
                out.append(')');
            }
            appendColor(out, RESET, colorsForTitle);
        }
    }

    /**
     * Returns a string representation of the given name in the given locale, with paranoiac checks against null value.
     * Such null values should never happen since the properties used here are mandatory, but we try to make this class
     * robust to broken implementations.
     */
    private static String toString(final GenericName name, final Locale locale) {
        if (name != null) {
            final InternationalString i18n = name.toInternationalString();
            return (i18n != null) ? i18n.toString(locale) : name.toString();
        }
        return null;
    }

    /**
     * Returns the string representation of the given parameter name.
     */
    private static String toString(Object parameter) {
        if (parameter instanceof ReferenceIdentifier) {
            parameter = ((ReferenceIdentifier) parameter).getCode();
        }
        return parameter.toString();
    }
}
