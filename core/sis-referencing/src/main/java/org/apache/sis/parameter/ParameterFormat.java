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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.io.IOException;
import java.text.Format;
import java.text.ParsePosition;
import java.text.ParseException;
import javax.measure.unit.Unit;

import org.opengis.parameter.*;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.operation.OperationMethod;

import org.apache.sis.measure.Range;
import org.apache.sis.io.wkt.Colors;
import org.apache.sis.io.TableAppender;
import org.apache.sis.io.CompoundFormat;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.X364;

import static org.apache.sis.util.collection.Containers.hashMapCapacity;


/**
 * Formats {@linkplain DefaultParameterDescriptorGroup parameter descriptors} or
 * {@linkplain DefaultParameterValueGroup parameter values} in a tabular format.
 * This format assumes a monospaced font and an encoding supporting drawing box
 * characters (e.g. UTF-8).
 *
 * <div class="note"><b>Example:</b>
 * The <cite>Mercator (variant A)</cite> example given in {@link DefaultParameterDescriptorGroup} javadoc
 * can be formatted as below:
 *
 * {@preformat text
 *   EPSG: Mercator (variant A)
 *   ┌────────────────────────────────┬────────┬───────────────┬───────────────┐
 *   │ Name                           │ Type   │ Value domain  │ Default value │
 *   ├────────────────────────────────┼────────┼───────────────┼───────────────┤
 *   │ Latitude of natural origin     │ Double │ [-80 … 84]°   │ 40.0°         │
 *   │ Longitude of natural origin    │ Double │ [-180 … 180]° │ -60.0°        │
 *   │ Scale factor at natural origin │ Double │ (0 … ∞)       │ 1.0           │
 *   │ False easting                  │ Double │ (-∞ … ∞) m    │ 5000.0 m      │
 *   │ False northing                 │ Double │ (-∞ … ∞) m    │ 10000.0 m     │
 *   └────────────────────────────────┴────────┴───────────────┴───────────────┘
 * }
 * </div>
 *
 * <div class="warning"><b>Limitation:</b>
 * Current implementation supports only formatting, not parsing.
 * </div>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.1)
 * @version 0.4
 * @module
 */
public class ParameterFormat extends CompoundFormat<Object> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1345231739800152411L;

    /**
     * Special codespace for requesting the display of EPSG codes.
     */
    private static final String SHOW_EPSG_CODES = "EPSG:#";

    /**
     * The amount of information to put in the table to be formatted by {@link ParameterFormat}.
     *
     * @since   0.4
     * @version 0.4
     * @module
     */
    public static enum ContentLevel {
        /**
         * The most detailed content, which includes
         * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getName() name} and
         * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getAlias() aliases}.
         * Each parameter may be formatted on many lines if they have aliases.
         */
        DETAILED,

        /**
         * A medium level of content which formats each parameter on a single line. For each parameter only the
         * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getName() name} is formatted —
         * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getAlias() aliases} and
         * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getIdentifiers() identifiers} are omitted.
         */
        BRIEF,

        /**
         * Limits the content to names and aliases in a tabular format. In addition to parameters,
         * this level can also format array of operation method, coordinate reference system, <i>etc.</i>
         * The summary contains the identifier names and aliases aligned in a table.
         *
         * <p><b>Tip:</b> The table formatted by default may be quite large. It is recommended to invoke
         * {@link ParameterFormat#setPreferredCodespaces(String[])} before to format in order to reduce the
         * amount of columns to display.</p>
         */
        NAME_SUMMARY
    }

    /**
     * The locale for international strings.
     */
    private final Locale displayLocale;

    /**
     * The amount of information to put in the table.
     *
     * @see #getContentLevel()
     */
    private ContentLevel contentLevel = ContentLevel.BRIEF;

    /**
     * If the identifier should be written only for some code spaces, those code spaces.
     * Otherwise {@code null}.
     *
     * @see #getPreferredCodespaces()
     */
    private Set<String> preferredCodespaces;

    /**
     * The colors for an output on X3.64 compatible terminal, or {@code null} if none.
     *
     * @see #getColors()
     */
    private Colors colors;

    /**
     * Creates a new formatter for the default locale and timezone.
     */
    public ParameterFormat() {
        super(Locale.getDefault(Locale.Category.FORMAT), TimeZone.getDefault());
        displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
    }

    /**
     * Creates a new formatter for the given locale and timezone.
     *
     * @param locale   The locale, or {@code null} for {@code Locale.ROOT}.
     * @param timezone The timezone, or {@code null} for UTC.
     */
    public ParameterFormat(final Locale locale, final TimeZone timezone) {
        super(locale, timezone);
        displayLocale = (locale != null) ? locale : Locale.ROOT;
    }

    /**
     * Returns the base type of values parsed and formatted by this {@code Format} instance.
     * The default implementation returns {@code Object.class} since it is the only common parent
     * of classes formatted by this object.
     *
     * @return The common parent of object classes formatted by this {@code ParameterFormat}.
     */
    @Override
    public Class<? extends Object> getValueType() {
        return Object.class;
    }

    /**
     * Returns the locale for the given category.
     *
     * <ul>
     *   <li>{@link java.util.Locale.Category#FORMAT} specifies the locale to use for values.</li>
     *   <li>{@link java.util.Locale.Category#DISPLAY} specifies the locale to use for labels.</li>
     * </ul>
     *
     * @param  category The category for which a locale is desired.
     * @return The locale for the given category (never {@code null}).
     */
    @Override
    public Locale getLocale(final Locale.Category category) {
        return (category == Locale.Category.DISPLAY) ? displayLocale : super.getLocale(category);
    }

    /**
     * Returns the amount of information to put in the table.
     * The default value is {@link org.apache.sis.parameter.ParameterFormat.ContentLevel#BRIEF}.
     *
     * @return The table content.
     */
    public ContentLevel getContentLevel() {
        return contentLevel;
    }

    /**
     * Sets the amount of information to put in the table.
     *
     * @param level The amount of information to put in the table.
     */
    public void setContentLevel(final ContentLevel level) {
        ArgumentChecks.ensureNonNull("level", level);
        this.contentLevel = level;
    }

    /**
     * Returns the code spaces of names and identifiers to show, or {@code null} if there is no restriction.
     * This method returns the sequence specified by the last call to {@link #setPreferredCodespaces(String[])}.
     * The default value is {@code null}.
     *
     * @return The code spaces of names and identifiers to show, or {@code null} if no restriction.
     */
    public String[] getPreferredCodespaces() {
        return (preferredCodespaces != null) ? preferredCodespaces.toArray(new String[preferredCodespaces.size()]) : null;
    }

    /**
     * Filters names and identifiers by their code spaces. If the given array is non-null, then the only names,
     * aliases and identifiers to be formatted are those having a {@link ReferenceIdentifier#getCodeSpace()},
     * {@link GenericName#scope()} or {@link ScopedName#head()} value in the given list, unless no name or alias
     * matches this criterion.
     *
     * <p>Additional effects:</p>
     * <ul>
     *   <li>With {@link org.apache.sis.parameter.ParameterFormat.ContentLevel#BRIEF}, the given list determines
     *       the preference order to choosing the name or identifier to format.</li>
     *   <li>With {@link org.apache.sis.parameter.ParameterFormat.ContentLevel#NAME_SUMMARY}, the given list
     *       sets the column order.</li>
     * </ul>
     *
     * @param codespaces The preferred code spaces of names, aliases and identifiers to format, or {@code null}
     *        for accepting all of them. Some typical values are {@code "EPSG"}, {@code "OGC"} or {@code "GeoTIFF"}.
     */
    public void setPreferredCodespaces(final String... codespaces) {
        Set<String> copy = null;
        if (codespaces != null) {
            copy = CollectionsExt.immutableSet(true, codespaces);
        }
        this.preferredCodespaces = copy;
    }

    /**
     * Returns the colors for an output on X3.64 compatible terminal, or {@code null} if none.
     * The default value is {@code null}.
     *
     * @return The colors for an output on X3.64 compatible terminal, or {@code null} if none.
     */
    public Colors getColors() {
        return colors;
    }

    /**
     * Sets the colors for an output on X3.64 compatible terminal.
     * This is used for example in order to emphases the identifier in a list of alias.
     *
     * @param colors The colors for an output on X3.64 compatible terminal, or {@code null} if none.
     */
    public void setColors(final Colors colors) {
        this.colors = colors;
    }

    /**
     * Formats the given object to the given stream of buffer.
     * The object may be an instance of any of the following types:
     *
     * <ul>
     *   <li>{@link ParameterValueGroup}</li>
     *   <li>{@link ParameterDescriptorGroup}</li>
     *   <li>{@link OperationMethod}</li>
     *   <li><code>{@linkplain IdentifiedObject}[]</code> — accepted only for
     *       {@link org.apache.sis.parameter.ParameterFormat.ContentLevel#NAME_SUMMARY}.</li>
     * </ul>
     *
     * @throws IOException If an error occurred while writing to the given appendable.
     */
    @Override
    public void format(final Object object, final Appendable toAppendTo) throws IOException {
        ArgumentChecks.ensureNonNull("object",     object);
        ArgumentChecks.ensureNonNull("toAppendTo", toAppendTo);
        final boolean isSummary = contentLevel == ContentLevel.NAME_SUMMARY;
        final ParameterDescriptorGroup descriptor;
        final ParameterValueGroup      values;
        final ReferenceIdentifier      name;
        if (object instanceof ParameterValueGroup) {
            values     = (ParameterValueGroup) object;
            descriptor = values.getDescriptor();
            name       = descriptor.getName();
        } else if (object instanceof ParameterDescriptorGroup) {
            descriptor = (ParameterDescriptorGroup) object;
            values     = null;
            name       = descriptor.getName();
        } else if (object instanceof OperationMethod) {
            final OperationMethod operation = (OperationMethod) object;
            descriptor = operation.getParameters();
            values     = null;
            name       = operation.getName();
        } else if (isSummary && object instanceof IdentifiedObject[]) {
            formatSummary((IdentifiedObject[]) object, toAppendTo);
            return;
        } else {
            throw new IllegalArgumentException(Errors.getResources(displayLocale)
                    .getString(Errors.Keys.UnsupportedType_1, object.getClass()));
        }
        if (isSummary) {
            final List<GeneralParameterDescriptor> parameters = descriptor.descriptors();
            formatSummary(parameters.toArray(new IdentifiedObject[parameters.size()]), toAppendTo);
        } else {
            format(name.getCode(), descriptor, values, toAppendTo);
        }
    }

    /**
     * Implementation of public {@code format(…)} methods for all content levels except {@code NAME_SUMMARY}.
     *
     * @param  name       The group name, usually {@code descriptor.getName().getCode()}.
     * @param  descriptor The parameter descriptor, usually {@code values.getDescriptor()}.
     * @param  values     The parameter values, or {@code null} if none.
     * @throws IOException If an error occurred while writing to the given appendable.
     */
    private void format(final String name, final ParameterDescriptorGroup group,
            final ParameterValueGroup values, final Appendable out) throws IOException
    {
        final boolean isBrief       = (contentLevel == ContentLevel.BRIEF);
        final boolean hasColors     = (colors != null);
        final String  lineSeparator = System.lineSeparator();
        final Vocabulary resources  = Vocabulary.getResources(displayLocale);
        new ParameterTableRow(group, displayLocale, isBrief).appendIdentifiers(out, hasColors, false, lineSeparator);
        out.append(lineSeparator);
        /*
         * Formats the table header (i.e. the column names).
         */
        final char horizontalBorder = isBrief ? '─' : '═';
        final TableAppender table = isBrief ? new TableAppender(out, " │ ") : new TableAppender(out);
        table.setMultiLinesCells(true);
        table.nextLine(horizontalBorder);
        for (int i=0; ; i++) {
            boolean end = false;
            final short key;
            switch (i) {
                case 0: key = Vocabulary.Keys.Name; break;
                case 1: key = Vocabulary.Keys.Type; break;
                case 2: key = Vocabulary.Keys.ValueDomain; break;
                case 3: key = (values == null) ? Vocabulary.Keys.DefaultValue : Vocabulary.Keys.Value; end = true; break;
                default: throw new AssertionError(i);
            }
            if (hasColors) table.append(X364.BOLD.sequence());
            table.append(resources.getString(key));
            if (hasColors) table.append(X364.NORMAL.sequence());
            if (end) break;
            table.nextColumn();
        }
        table.nextLine();
        /*
         * Prepares the informations to be printed later as table rows. We scan all rows before to print them
         * in order to compute the width of codespaces. During this process, we split the objects to be printed
         * later in two collections: simple parameters are stored as (descriptor,value) pairs, while groups are
         * stored in an other collection for deferred printing after the simple parameters.
         */
        int codespaceWidth = 0;
        final Collection<?> elements = (values != null) ? values.values() : group.descriptors();
        final Map<GeneralParameterDescriptor, ParameterTableRow> descriptorValues =
                new LinkedHashMap<>(hashMapCapacity(elements.size()));
        List<Object> deferredGroups = null; // To be created only if needed (it is usually not).
        for (final Object element : elements) {
            final GeneralParameterValue parameter;
            final GeneralParameterDescriptor descriptor;
            if (values != null) {
                parameter  = (GeneralParameterValue) element;
                descriptor = parameter.getDescriptor();
            } else {
                parameter  = null;
                descriptor = (GeneralParameterDescriptor) element;
            }
            if (descriptor instanceof ParameterDescriptorGroup) {
                if (deferredGroups == null) {
                    deferredGroups = new ArrayList<>(4);
                }
                deferredGroups.add(element);
                continue;
            }
            /*
             * In the vast majority of cases, there is only one value for each parameter. However
             * if we find more than one value, we will append all extra occurrences in a "multiple
             * values" list to be formatted in the same row.
             */
            Object value = null;
            Unit<?> unit = null;
            if (parameter instanceof ParameterValue<?>) {
                final ParameterValue<?> p = (ParameterValue<?>) parameter;
                value = p.getValue();
                unit  = p.getUnit();
            } else if (descriptor instanceof ParameterDescriptor<?>) {
                final ParameterDescriptor<?> p = (ParameterDescriptor<?>) descriptor;
                value = p.getDefaultValue();
                unit  = p.getUnit();
            }
            ParameterTableRow row = descriptorValues.get(descriptor);
            if (row == null) {
                row = new ParameterTableRow(descriptor, displayLocale, isBrief);
                descriptorValues.put(descriptor, row);
            }
            row.values.add(value);
            row.units .add(unit);
            if (row.codespaceWidth > codespaceWidth) {
                codespaceWidth = row.codespaceWidth;
            }
        }
        /*
         * Now process to the formatting of (descriptor,value) pairs. Each descriptor alias
         * will be formatted on its own line in a table row. If there is more than one value,
         * then each value will be formatted on its own line as well. Note that the values may
         * be null if there is none.
         */
        char horizontalLine = horizontalBorder;
        for (final Map.Entry<GeneralParameterDescriptor,ParameterTableRow> entry : descriptorValues.entrySet()) {
            if (horizontalLine != 0) {
                table.nextLine('─');
            }
            horizontalLine = isBrief ? 0 : '─';
            final ParameterTableRow row = entry.getValue();
            row.codespaceWidth = codespaceWidth;
            row.appendIdentifiers(table, false, hasColors, lineSeparator);
            table.nextColumn();
            final GeneralParameterDescriptor generalDescriptor = entry.getKey();
            if (generalDescriptor instanceof ParameterDescriptor<?>) {
                /*
                 * Writes value type.
                 */
                final ParameterDescriptor<?> descriptor = (ParameterDescriptor<?>) generalDescriptor;
                final Class<?> valueClass = descriptor.getValueClass();
                table.append(getFormat(Class.class).format(valueClass));
                table.nextColumn();
                /*
                 * Writes minimum and maximum values, together with the unit of measurement (if any).
                 */
                final Range<?> valueDomain = Parameters.getValueDomain(descriptor);
                if (valueDomain != null) {
                    table.append(getFormat(Range.class).format(valueDomain));
                }
                table.nextColumn();
                /*
                 * Wraps the value in an array. Because it may be an array of primitive type,
                 * we can't cast to Object[]. Then, each array's element will be formatted on
                 * its own line.
                 */
                row.expandSingleton();
                final int length = row.values.size();
                for (int i=0; i<length; i++) {
                    final Object value = row.values.get(i);
                    if (value != null) {
                        if (i != 0) {
                            table.append(lineSeparator);
                        }
                        final Format format = getFormat(value.getClass());
                        table.append(format != null ? format.format(value) : value.toString());
                        final Unit<?> unit = row.units.get(i);
                        if (unit != null) {
                            final String symbol = getFormat(Unit.class).format(unit);
                            if (!symbol.isEmpty()) {
                                if (Character.isLetterOrDigit(symbol.codePointAt(0))) {
                                    table.append(' ');
                                }
                                table.append(symbol);
                            }
                        }
                    }
                }
            }
            table.nextLine();
        }
        table.nextLine(horizontalBorder);
        table.flush();
        /*
         * Now formats all groups deferred to the end of this table, with recursive calls to
         * this method (recursive calls use their own TableWriter instance, so they may result
         * in a different cell layout). Most of the time, there is no such additional group.
         */
        if (deferredGroups != null) {
            for (final Object element : deferredGroups) {
                final ParameterValueGroup value;
                final ParameterDescriptorGroup descriptor;
                if (element instanceof ParameterValueGroup) {
                    value = (ParameterValueGroup) element;
                    descriptor = value.getDescriptor();
                } else {
                    value = null;
                    descriptor = (ParameterDescriptorGroup) element;
                }
                out.append(lineSeparator);
                format(name + '/' + descriptor.getName().getCode(), descriptor, value, out);
            }
        }
    }

    /**
     * Implementation of public {@code format(…)} methods for {@code NAME_SUMMARY} content level.
     *
     * @param  objects The collection of objects to format.
     * @param  out The stream or buffer where to write the summary.
     * @throws IOException if an error occurred will writing to the given appendable.
     */
    private void formatSummary(final IdentifiedObject[] objects, final Appendable out) throws IOException {
        /*
         * Prepares all rows before we write them to the output stream, because not all
         * identified objects may have names with the same scopes in the same order. We
         * also need to iterate over all rows in order to know the number of columns.
         *
         * The two first columns are treated especially.  The first one is the optional
         * EPSG code. The second one is the main identifier (usually the EPSG name). We
         * put SHOW_EPSG_CODE and null as special values for their column names,  to be
         * replaced later by "EPSG" and "Identifier" in user locale. We can not put the
         * localized strings in the map right now because they could conflict with the
         * scope of some alias to be processed below.
         */
        final Map<Object,Integer> header = new LinkedHashMap<>();
        final List<String[]>        rows = new ArrayList<>();
        final List<String>     epsgNames = new ArrayList<>();
        final Set<String>     codespaces = this.preferredCodespaces;
        final Vocabulary       resources = Vocabulary.getResources(displayLocale);
        final int          showEpsgCodes = ((codespaces == null) || codespaces.contains(SHOW_EPSG_CODES)) ? 1 : 0;
        if (showEpsgCodes != 0) {
            header.put(SHOW_EPSG_CODES, 0);
        }
        header.put(null, showEpsgCodes); // See above comment for the meaning of "null" here.
        for (final IdentifiedObject element : objects) {
            /*
             * Prepares a row: puts the name in the "identifier" column, which is the
             * first or the second one depending if we display EPSG codes or not.
             */
            String epsgName = null;
            String[] row = new String[header.size()];
            row[showEpsgCodes] = element.getName().getCode();
            int numUnscoped = 0;
            final Collection<GenericName> aliases = element.getAlias();
            if (aliases != null) {
                /*
                 * Adds alias (without scope) to the row. Each alias will be put in the column
                 * appropriate for its scope. If a name has no scope, we will create one using
                 * sequential number ("numUnscoped" is the count of such names without scope).
                 */
                for (final GenericName alias : aliases) {
                    final GenericName scope = alias.scope().name();
                    final String name = alias.tip().toInternationalString().toString(displayLocale);
                    final Object columnName;
                    if (scope != null) {
                        columnName = scope.toInternationalString().toString(displayLocale);
                    } else {
                        columnName = ++numUnscoped;
                    }
                    if (columnName.equals("EPSG")) {
                        epsgName = name;
                    }
                    if (codespaces != null && !codespaces.contains(scope.toString())) {
                        /*
                         * The user requested only for a few authorities and the current alias
                         * is not a member of this subset. Continue the search to other alias.
                         */
                        continue;
                    }
                    /*
                     * Now stores the alias name at the position we just determined above. If
                     * more than one value are assigned to the same column, keep the first one.
                     */
                    row = putIfAbsent(row, getColumnIndex(header, columnName), name);
                }
            }
            /*
             * After the aliases, search for the identifiers. The code in this block is similar
             * to the one we just did for aliases. By doing this operation after the aliases we
             * ensure that if both an identifier and a name is defined for the same column, the
             * name is given precedence.
             */
            final Collection<ReferenceIdentifier> identifiers = element.getIdentifiers();
            if (identifiers != null) {
                for (final ReferenceIdentifier identifier : identifiers) {
                    final String scope = identifier.getCodeSpace();
                    final String name = identifier.getCode();
                    final Object columnName = (scope != null) ? scope : ++numUnscoped;
                    int columnIndex;
                    if (showEpsgCodes != 0 && columnName.equals("EPSG")) {
                        columnIndex = 0;
                    } else {
                        if (codespaces!=null && !codespaces.contains(scope)) {
                            continue;
                        }
                        columnIndex = getColumnIndex(header, columnName);
                    }
                    row = putIfAbsent(row, columnIndex, name);
                }
            }
            rows.add(row);
            epsgNames.add(epsgName);
        }
        /*
         * Writes the table. The header will contains one column for each alias's scope
         * (or authority) declared in 'titles', in the same order. The column for Geotk
         * names will treated especially, because cit ontains ambiguous names.
         */
        final boolean hasColors = (colors != null);
        final TableAppender table = new TableAppender(out, " │ ");
        table.setMultiLinesCells(true);
        table.appendHorizontalSeparator();
        /*
         * Writes all column headers.
         */
        int column = 0;
        int geotoolkitColumn = -1;
        for (final Object element : header.keySet()) {
            String title;
            if (element == null) {
                title = resources.getString(Vocabulary.Keys.Identifier);
            } else if (element == SHOW_EPSG_CODES) {
                title = "EPSG";
            } else if (element instanceof String) {
                title = (String) element;
                if (title.equalsIgnoreCase("geotk") ||
                    title.equalsIgnoreCase("Geotoolkit.org") ||
                    title.equalsIgnoreCase("Geotoolkit")) // Legacy
                {
                    geotoolkitColumn = column;
                    title = resources.getString(Vocabulary.Keys.Description);
                }
            } else { // Should be a Number
                title = resources.getString(Vocabulary.Keys.Aliases) + ' ' + element;
            }
            if (hasColors) {
                title = X364.BOLD.sequence() + title + X364.NORMAL.sequence();
            }
            table.append(title);
            table.nextColumn();
            column++;
        }
        table.appendHorizontalSeparator();
        /*
         * Writes all rows.
         */
        final int numRows    = rows.size();
        final int numColumns = header.size();
        for (int rowIndex=0; rowIndex<numRows; rowIndex++) {
            final String[] aliases = rows.get(rowIndex);
            for (column=0; column<numColumns; column++) {
                if (column < aliases.length) {
                    String alias = aliases[column];
                    if (column == geotoolkitColumn) {
                        if (alias == null) {
                            alias = epsgNames.get(rowIndex);
                        } else if (hasColors) {
                            if (!alias.equals(aliases[showEpsgCodes])) {
                                alias = X364.FAINT.sequence() + alias + X364.NORMAL.sequence();
                            }
                        }
                    }
                    if (alias != null) {
                        table.append(alias);
                    }
                }
                table.nextColumn();
            }
            table.nextLine();
        }
        table.appendHorizontalSeparator();
        table.flush();
    }

    /**
     * Returns the index of the column of the given name. If no such column
     * exists, then a new column is appended at the right of the table.
     */
    private static int getColumnIndex(final Map<Object,Integer> header, final Object columnName) {
        Integer position = header.get(columnName);
        if (position == null) {
            position = header.size();
            header.put(columnName, position);
        }
        return position;
    }

    /**
     * Stores a value at the given position in the given row, expanding the array if needed.
     * This operation is performed only if no value already exists at the given index.
     */
    private static String[] putIfAbsent(String[] row, final int columnIndex, final String name) {
        if (columnIndex >= row.length) {
            row = Arrays.copyOf(row, columnIndex+1);
        }
        if (row[columnIndex] == null) {
            row[columnIndex] = name;
        }
        return row;
    }

    /**
     * Not yet supported.
     *
     * @return Currently never return.
     * @throws ParseException Currently always thrown.
     */
    @Override
    public Object parse(final CharSequence text, final ParsePosition pos) throws ParseException {
        throw new ParseException("Not supported yet.", 0);
    }
}
