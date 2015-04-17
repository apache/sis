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
import java.text.Format;
import java.text.FieldPosition;
import javax.measure.unit.Unit;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.io.wkt.Colors;
import org.apache.sis.io.wkt.ElementKind;
import org.apache.sis.util.Characters;
import org.apache.sis.util.Deprecable;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.RangeFormat;
import org.apache.sis.internal.metadata.NameToIdentifier;
import org.apache.sis.internal.util.X364;

import static org.apache.sis.internal.util.X364.*;
import static org.apache.sis.util.CharSequences.spaces;
import static org.apache.sis.util.iso.DefaultNameSpace.DEFAULT_SEPARATOR;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;


/**
 * A row in the table to be formatted by {@link ParameterFormat}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
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
     *   <li>{@link Identifier} for identifiers.</li>
     * </ul>
     *
     * @see #addIdentifier(String, Object)
     */
    private final Map<String,Set<Object>> identifiers;

    /**
     * The largest codespace width, in number of Unicode code points.
     *
     * @see #addIdentifier(String, Object)
     */
    int codespaceWidth;

    /**
     * The string representation of the domain of values, or {@code null} if none.
     *
     * @see #setValueDomain(Range, Format, StringBuffer)
     */
    String valueDomain;

    /**
     * The position to use for alignment of {@link #valueDomain}.
     * This is usually after the '…' separator.
     *
     * @see #setValueDomain(Range, Format, StringBuffer)
     */
    int valueDomainAlignment;

    /**
     * The values. Some elements in this list may be null.
     *
     * @see #addValue(Object, Unit)
     */
    final List<Object> values;

    /**
     * The units of measurement. The size of this list shall be the same than {@link #values}.
     * The list may contain null elements.
     *
     * <p>This list is initially filled with {@link Unit} instance. Later in the formatting process,
     * {@code Unit} instances will be replaced by their symbol.</p>
     *
     * @see #addValue(Object, Unit)
     */
    final List<Object> units;

    /**
     * Reference to a remark, or {@code 0} if none.
     */
    private int remarks;

    /**
     * Creates a new row in a table to be formatted by {@link ParameterFormat}.
     *
     * @param object  The object for which to get the (<var>codespace(s)</var>, <var>name(s)</var>).
     * @param locale  The locale for formatting the names and the remarks.
     * @param remarks An initially empty map, to be filled with any remarks we may found.
     */
    ParameterTableRow(final IdentifiedObject object, final Locale locale, final Set<String> preferredCodespaces,
            final Map<String,Integer> remarks, final boolean isBrief)
    {
        values = new ArrayList<Object>(2); // In the vast majority of cases, we will have only one value.
        units  = new ArrayList<Object>(2);
        identifiers = new LinkedHashMap<String,Set<Object>>();
        ReferenceIdentifier name = object.getName();
        if (name != null) { // Paranoiac check.
            final String codespace = name.getCodeSpace();
            if (preferredCodespaces == null || preferredCodespaces.contains(codespace)) {
                addIdentifier(codespace, name.getCode()); // Value needs to be a String here.
                name = null;
            }
        }
        /*
         * For detailed content, add aliases.
         * For brief content, add the first alias if we have not been able to add the name.
         */
        if (!isBrief || identifiers.isEmpty()) {
            final Collection<GenericName> aliases = object.getAlias();
            if (aliases != null) { // Paranoiac check.
                for (GenericName alias : aliases) {
                    if (!isDeprecated(alias)) {
                        final String codespace = NameToIdentifier.getCodeSpace(alias, locale);
                        if (codespace != null) {
                            alias = alias.tip();
                        }
                        if (preferredCodespaces == null || preferredCodespaces.contains(codespace)) {
                            addIdentifier(codespace, NameToIdentifier.toString(alias, locale));
                            name = null;
                            if (isBrief) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        /*
         * If we found no name and no alias in the codespaces requested by the user,
         * unconditionally add the name regardless its namespace.
         */
        if (name != null) {
            addIdentifier(name.getCodeSpace(), name.getCode()); // Value needs to be a String here.
        }
        /*
         * Add identifiers (detailed mode only).
         */
        if (!isBrief) {
            final Collection<? extends ReferenceIdentifier> ids = object.getIdentifiers();
            if (ids != null) { // Paranoiac check.
                for (final ReferenceIdentifier id : ids) {
                    if (!isDeprecated(id)) {
                        final String codespace = id.getCodeSpace();
                        if (preferredCodespaces == null || preferredCodespaces.contains(codespace)) {
                            addIdentifier(codespace, id); // No .getCode() here.
                        }
                    }
                }
            }
        }
        /*
         * Take the remarks, if any.
         */
        final InternationalString r = object.getRemarks();
        if (r != null) {
            final int n = remarks.size() + 1;
            final Integer p = JDK8.putIfAbsent(remarks, r.toString(locale), n);
            this.remarks = (p != null) ? p : n;
        }
    }

    /**
     * Returns {@code true} if the given name or identifier is deprecated.
     */
    private static boolean isDeprecated(final Object object) {
        return (object instanceof Deprecable) && ((Deprecable) object).isDeprecated();
    }

    /**
     * Helper method for the constructor only, adding an identifier for the given code space.
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
            ids = new LinkedHashSet<Object>(8);
            identifiers.put(codespace, ids);
        }
        ids.add(identifier);
    }

    /**
     * If this row has exactly one codespace, returns that codespace.
     * Otherwise returns {@code null}.
     */
    final String getCodeSpace() {
        final Iterator<Map.Entry<String,Set<Object>>> it = identifiers.entrySet().iterator();
        if (it.hasNext()) {
            final Map.Entry<String,Set<Object>> entry = it.next();
            if (!it.hasNext()) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Sets the value domain to the string representation of the given range.
     *
     * @param  range  The range to format.
     * @param  format The format to use for formatting the {@code range}.
     * @param  buffer A temporary buffer to use for formatting the range.
     * @return The position of a character on which to align the text in the cell.
     */
    final int setValueDomain(final Range<?> range, final Format format, final StringBuffer buffer) {
        final FieldPosition fieldPosition = new FieldPosition(RangeFormat.Field.MAX_VALUE);
        valueDomain = format.format(range, buffer, fieldPosition).toString();
        buffer.setLength(0);
        return valueDomainAlignment = fieldPosition.getBeginIndex();
    }

    /**
     * Adds a value and its unit of measurement.
     *
     * @param value The value, or {@code null}.
     * @param unit  The unit of measurement, or {@code null}.
     */
    final void addValue(final Object value, final Unit<?> unit) {
        values.add(value);
        units .add(unit);
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
                    final Object unit = units.get(0);
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
     * Writes the color for the given type if {@code colors} is non-null.
     */
    private static void writeColor(final Appendable out, final Colors colors, final ElementKind type)
            throws IOException
    {
        if (colors != null) {
            final String name = colors.getName(type);
            if (name != null) {
                out.append(X364.forColorName(name).sequence());
            }
        }
    }

    /**
     * Writes the given color if {@code colorEnabled} is {@code true}.
     */
    private static void writeColor(final Appendable out, final X364 color, final boolean colorEnabled)
            throws IOException
    {
        if (colorEnabled) {
            out.append(color.sequence());
        }
    }

    /**
     * Writes the identifiers. At most one of {@code colors != null} and {@code colorsForRows}
     * can be {@code true}.
     *
     * <p><b>This method can be invoked only once per {@code ParameterTableRow} instance</b>,
     * as its implementation destroys the internal list of identifiers.</p>
     *
     * @param  out             Where to write.
     * @param  writeCodespaces {@code true} for writing codespaces, or {@code false} for omitting them.
     * @param  colors          Non null if syntax coloring should be applied for table title.
     * @param  colorsForRows   {@code true} if syntax coloring should be applied for table rows.
     * @param  lineSeparator   The system-dependent line separator.
     * @throws IOException     If an exception occurred while writing.
     */
    final void writeIdentifiers(final Appendable out, final boolean writeCodespaces,
            final Colors colors, final boolean colorsForRows, final String lineSeparator) throws IOException
    {
        if (codespaceWidth != 0) {
            codespaceWidth += 2; // Add a colon and space between codespace and code in e.g. "OGC: Mercator".
        }
        boolean isNewLine = false;
        for (final Map.Entry<String,Set<Object>> entry : identifiers.entrySet()) {
            final String codespace = entry.getKey();
            final Set<Object> identifiers = entry.getValue();
            Iterator<Object> it = identifiers.iterator();
            while (it.hasNext()) {
                if (isNewLine) {
                    out.append(lineSeparator);
                }
                isNewLine = true;
                /*
                 * Write the codespace. More than one name may exist for the same codespace,
                 * in which case the code space will be repeated on a new line each time.
                 */
                writeColor(out, colors, ElementKind.NAME);
                if (writeCodespaces) {
                    int pad = codespaceWidth;
                    if (codespace != null) {
                        writeColor(out, FAINT, colorsForRows);
                        out.append(codespace).append(DEFAULT_SEPARATOR);
                        writeColor(out, NORMAL, colorsForRows);
                        pad -= (codespace.length() + 1);
                    }
                    out.append(spaces(pad));
                }
                /*
                 * Write the name or alias after the codespace. We remove what we wrote,
                 * because we may iterate over the 'identifiers' set more than once.
                 */
                writeColor(out, BOLD, colors != null);
                out.append(toString(it.next()));
                writeColor(out, RESET, colors != null);
                it.remove();
                /*
                 * Write the footnote number if there is a remark associated to this parameter.
                 * We write the remark only for the first name or identifier.
                 */
                if (remarks != 0) {
                    writeFootnoteNumber(out, remarks);
                    remarks = 0;
                }
                /*
                 * Write all identifiers between parenthesis after the firt name only.
                 * Aliases (to be written in a new iteration) will not have identifier.
                 */
                boolean hasAliases     = false;
                boolean hasIdentifiers = false;
                while (it.hasNext()) {
                    final Object id = it.next();
                    if (id instanceof Identifier) {
                        out.append(hasIdentifiers ? ", " : " (");
                        writeColor(out, colors, ElementKind.IDENTIFIER);
                        out.append(toString(id));
                        writeColor(out, FOREGROUND_DEFAULT, colors != null);
                        hasIdentifiers = true;
                        it.remove();
                    } else {
                        hasAliases = true;
                    }
                }
                if (hasIdentifiers) {
                    out.append(')');
                }
                if (hasAliases) {
                    it = identifiers.iterator();
                }
            }
        }
    }

    /**
     * Writes the footnote number to the given appendable.
     * The number is written in superscript if possible.
     */
    static void writeFootnoteNumber(final Appendable out, final int n) throws IOException {
        if (n >= 0 && n < 10) {
            out.append(Characters.toSuperScript((char) ('0' + n)));
        } else {
            out.append('(').append(Integer.toString(n)).append(')');
        }
    }

    /**
     * Returns the string representation of the given parameter name.
     */
    private static String toString(Object parameter) {
        if (parameter instanceof Identifier) {
            parameter = ((Identifier) parameter).getCode();
        }
        return parameter.toString();
    }
}
