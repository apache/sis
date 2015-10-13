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
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import java.io.Console;
import java.util.concurrent.atomic.AtomicReference;
import javax.measure.unit.Unit;

import org.opengis.parameter.*;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.operation.OperationMethod;

import org.apache.sis.measure.Range;
import org.apache.sis.io.wkt.Colors;
import org.apache.sis.io.TableAppender;
import org.apache.sis.io.TabularFormat;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.internal.metadata.NameToIdentifier;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.X364;

import static org.apache.sis.util.collection.Containers.hashMapCapacity;


/**
 * Formats {@linkplain DefaultParameterDescriptorGroup parameter descriptors} or
 * {@linkplain DefaultParameterValueGroup parameter values} in a tabular format.
 * This format assumes a monospaced font and an encoding supporting drawing box
 * characters (e.g. UTF-8).
 *
 * <p>This class can format parameters with different levels of verbosity, specified by the {@link ContentLevel}
 * property. The content level controls whether the formatter should write all names and aliases (at the cost of
 * multi-line rows), or to pickup one name per parameter for a more compact table. See {@link ContentLevel}
 * javadoc for output examples.</p>
 *
 * <div class="note"><b>Example:</b>
 * The <cite>Mercator (variant A)</cite> example given in {@link DefaultParameterDescriptorGroup} javadoc
 * will be formatted by default as below:
 *
 * {@preformat text
 *   EPSG: Mercator (variant A)
 *   ┌────────────────────────────────┬────────┬────────────┬───────────────┬───────────────┐
 *   │ Name (EPSG)                    │ Type   │ Obligation │ Value domain  │ Default value │
 *   ├────────────────────────────────┼────────┼────────────┼───────────────┼───────────────┤
 *   │ Latitude of natural origin     │ Double │ Mandatory  │  [-80 … 84]°  │         0.0°  │
 *   │ Longitude of natural origin    │ Double │ Mandatory  │ [-180 … 180]° │         0.0°  │
 *   │ Scale factor at natural origin │ Double │ Mandatory  │    (0 … ∞)    │         1.0   │
 *   │ False easting                  │ Double │ Mandatory  │   (-∞ … ∞) m  │         0.0 m │
 *   │ False northing                 │ Double │ Mandatory  │   (-∞ … ∞) m  │         0.0 m │
 *   └────────────────────────────────┴────────┴────────────┴───────────────┴───────────────┘
 * }
 * </div>
 *
 * The kind of objects accepted by this formatter are:
 * <table class="sis">
 *   <caption>Formattable object types</caption>
 *   <tr><th>Class</th> <th>Remarks</th></tr>
 *   <tr><td>{@link ParameterValueGroup}</td><td><cite>Default values</cite> column is replaced by a column of the actual values.</td></tr>
 *   <tr><td>{@link ParameterDescriptorGroup}</td><td>Table caption is the parameter group name.</td></tr>
 *   <tr><td>{@link OperationMethod}</td><td>Table caption is the method name (not necessarily the same than parameter group name).</td></tr>
 *   <tr><td><code>{@linkplain IdentifiedObject}[]</code></td><td>Accepted only for {@link ContentLevel#NAME_SUMMARY}.</td></tr>
 * </table>
 *
 * <div class="warning"><b>Limitation:</b>
 * Current implementation supports only formatting, not parsing.
 * </div>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
public class ParameterFormat extends TabularFormat<Object> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1345231739800152411L;

    /**
     * An instance created when first needed and potentially shared.
     */
    private static final AtomicReference<ParameterFormat> INSTANCE = new AtomicReference<ParameterFormat>();

    /**
     * The default column separator. User can change the separator
     * by a call to {@link #setColumnSeparatorPattern(String)}.
     */
    private static final String SEPARATOR = " │ ";

    /**
     * The amount of information to include in the table formatted by {@link ParameterFormat}.
     * The content level controls whether the formatter should write all names and aliases
     * (at the cost of multi-line rows), or to pickup one name per parameter for a more compact table.
     *
     * <p>The enumeration value javadoc provide examples of formatting output.</p>
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
         *
         * <div class="note"><b>Example:</b>
         * The <cite>Mercator (variant A)</cite> example given in {@link DefaultParameterDescriptorGroup} javadoc,
         * (augmented with parameter aliases) formatted at this level produces a text like below:
         *
         * {@preformat text
         *   EPSG: Mercator (variant A) (9804)
         *   EPSG: Mercator (1SP)
         *   OGC:  Mercator_1SP
         *   ╔══════════════════════════════════════╤════════╤════════════╤═══════════════╤═══════════════╗
         *   ║ Name                                 │ Type   │ Obligation │ Value domain  │ Default value ║
         *   ╟──────────────────────────────────────┼────────┼────────────┼───────────────┼───────────────╢
         *   ║ EPSG: Latitude of natural origin     │ Double │ Mandatory  │  [-80 … 84]°  │         0.0°  ║
         *   ║ OGC:  latitude_of_origin             │        │            │               │               ║
         *   ╟──────────────────────────────────────┼────────┼────────────┼───────────────┼───────────────╢
         *   ║ EPSG: Longitude of natural origin    │ Double │ Mandatory  │ [-180 … 180]° │         0.0°  ║
         *   ║ OGC:  central_meridian               │        │            │               │               ║
         *   ╟──────────────────────────────────────┼────────┼────────────┼───────────────┼───────────────╢
         *   ║ EPSG: Scale factor at natural origin │ Double │ Mandatory  │    (0 … ∞)    │         1.0   ║
         *   ║ OGC:  scale_factor                   │        │            │               │               ║
         *   ╟──────────────────────────────────────┼────────┼────────────┼───────────────┼───────────────╢
         *   ║ EPSG: False easting                  │ Double │ Mandatory  │   (-∞ … ∞) m  │         0.0 m ║
         *   ║ OGC:  false_easting                  │        │            │               │               ║
         *   ╟──────────────────────────────────────┼────────┼────────────┼───────────────┼───────────────╢
         *   ║ EPSG: False northing                 │ Double │ Mandatory  │   (-∞ … ∞) m  │         0.0 m ║
         *   ║ OGC:  false_northing                 │        │            │               │               ║
         *   ╚══════════════════════════════════════╧════════╧════════════╧═══════════════╧═══════════════╝
         * }
         * </div>
         */
        DETAILED,

        /**
         * A medium level of content which formats each parameter on a single line. For each parameter only the
         * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getName() name} is formatted —
         * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getAlias() aliases} and
         * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getIdentifiers() identifiers} are omitted.
         *
         * <div class="note"><b>Example:</b>
         * The <cite>Mercator (variant A)</cite> example given in {@link DefaultParameterDescriptorGroup} javadoc
         * formatted at this level produces a text like below:
         *
         * {@preformat text
         *   EPSG: Mercator (variant A)
         *   ┌────────────────────────────────┬────────┬────────────┬───────────────┬───────────────┐
         *   │ Name (EPSG)                    │ Type   │ Obligation │ Value domain  │ Default value │
         *   ├────────────────────────────────┼────────┼────────────┼───────────────┼───────────────┤
         *   │ Latitude of natural origin     │ Double │ Mandatory  │  [-80 … 84]°  │         0.0°  │
         *   │ Longitude of natural origin    │ Double │ Mandatory  │ [-180 … 180]° │         0.0°  │
         *   │ Scale factor at natural origin │ Double │ Mandatory  │    (0 … ∞)    │         1.0   │
         *   │ False easting                  │ Double │ Mandatory  │   (-∞ … ∞) m  │         0.0 m │
         *   │ False northing                 │ Double │ Mandatory  │   (-∞ … ∞) m  │         0.0 m │
         *   └────────────────────────────────┴────────┴────────────┴───────────────┴───────────────┘
         * }
         * </div>
         */
        BRIEF,

        /**
         * Limits the content to names and aliases in a tabular format. In addition to parameters,
         * this level can also format array of operation method, coordinate reference system, <i>etc.</i>
         * The summary contains the identifier names and aliases aligned in a table.
         *
         * <div class="note"><b>Example:</b>
         * The <cite>Mercator (variant A)</cite> example given in {@link ParameterBuilder} javadoc
         * formatted at this level produces a text like below:
         *
         * {@preformat text
         *   EPSG: Mercator (variant A)
         *   ┌────────────────────────────────┬────────────────────┐
         *   │ EPSG                           │ OGC                │
         *   ├────────────────────────────────┼────────────────────┤
         *   │ Latitude of natural origin     │ latitude_of_origin │
         *   │ Longitude of natural origin    │ central_meridian   │
         *   │ Scale factor at natural origin │ scale_factor       │
         *   │ False easting                  │ false_easting      │
         *   │ False northing                 │ false_northing     │
         *   └────────────────────────────────┴────────────────────┘
         * }
         * </div>
         *
         * <p><b>Tip:</b> the table formatted by default may be quite large. It is recommended to invoke
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
        super(Locale.getDefault(), TimeZone.getDefault());
        displayLocale = super.getLocale(); // Implemented as Locale.getDefault(Locale.Category.DISPLAY) on the JDK7 branch.
        columnSeparator = SEPARATOR;
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
        columnSeparator = SEPARATOR;
    }

    /**
     * Returns the type of objects formatted by this class. This method has to return {@code Object.class}
     * since it is the only common parent to all object types accepted by this formatter.
     *
     * @return {@code Object.class}
     */
    @Override
    public final Class<Object> getValueType() {
        return Object.class;
    }

    /**
     * Returns the amount of information to put in the table.
     * The default value is {@link ContentLevel#BRIEF}.
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
     * Returns the code spaces of names, aliases and identifiers to show, or {@code null} if there is no restriction.
     * This method returns the sequence specified by the last call to {@link #setPreferredCodespaces(String[])},
     * without duplicated values.
     *
     * <p>The default value is {@code null}.</p>
     *
     * @return The code spaces of names and identifiers to show, or {@code null} if no restriction.
     */
    public String[] getPreferredCodespaces() {
        return (preferredCodespaces != null) ? preferredCodespaces.toArray(new String[preferredCodespaces.size()]) : null;
    }

    /**
     * Filters names, aliases and identifiers by their code spaces. If the given array is non-null, then the only names,
     * aliases and identifiers to be formatted are those having a {@link ReferenceIdentifier#getCodeSpace()},
     * {@link ScopedName#head()} or {@link GenericName#scope()} value in the given list, unless no name or alias
     * matches this criterion.
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
     * Returns {@code true} if a name, alias or identifier in the given codespace should be formatted.
     */
    private boolean isPreferredCodespace(final String codespace) {
        return (preferredCodespaces == null) || preferredCodespaces.contains(codespace);
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
     *
     * @param colors The colors for an output on X3.64 compatible terminal, or {@code null} if none.
     */
    public void setColors(final Colors colors) {
        this.colors = colors;
    }

    /**
     * Invoked when the formatter needs to move to the next column.
     */
    private void nextColumn(final TableAppender table) {
        table.append(beforeFill);
        table.nextColumn(fillCharacter);
    }

    /**
     * Formats the given object to the given stream of buffer.
     * The object may be an instance of any of the following types:
     *
     * <ul>
     *   <li>{@link ParameterValueGroup}</li>
     *   <li>{@link ParameterDescriptorGroup}</li>
     *   <li>{@link OperationMethod}</li>
     *   <li><code>{@linkplain IdentifiedObject}[]</code> — accepted only for {@link ContentLevel#NAME_SUMMARY}.</li>
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
        final Identifier               name;
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
        final boolean             isBrief        = (contentLevel == ContentLevel.BRIEF);
        final boolean             showObligation = !isBrief || (values == null);
        final boolean             hasColors      = (colors != null);
        final String              lineSeparator  = this.lineSeparator;
        final Map<String,Integer> remarks        = new LinkedHashMap<String,Integer>();
        final ParameterTableRow   header         = new ParameterTableRow(group, displayLocale, preferredCodespaces, remarks, isBrief);
        final String              groupCodespace = header.getCodeSpace();
        /*
         * Prepares the informations to be printed later as table rows. We scan all rows before to print them
         * in order to compute the width of codespaces. During this process, we split the objects to be printed
         * later in two collections: simple parameters are stored as (descriptor,value) pairs, while groups are
         * stored in an other collection for deferred formatting after the simple parameters.
         */
        int codespaceWidth = 0;
        final Collection<?> elements = (values != null) ? values.values() : group.descriptors();
        final Map<GeneralParameterDescriptor, ParameterTableRow> descriptorValues =
                new LinkedHashMap<GeneralParameterDescriptor, ParameterTableRow>(hashMapCapacity(elements.size()));
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
                    deferredGroups = new ArrayList<Object>(4);
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
                row = new ParameterTableRow(descriptor, displayLocale, preferredCodespaces, remarks, isBrief);
                descriptorValues.put(descriptor, row);
                if (row.codespaceWidth > codespaceWidth) {
                    codespaceWidth = row.codespaceWidth;
                }
            }
            row.addValue(value, unit);
        }
        /*
         * Finished to collect the values. Now transform the values:
         *
         *   - Singleton value of array types (either primitive or not) are wrapped into a list.
         *   - Values are formatted.
         *   - Value domains are formatted.
         *   - Position of the character on which to do the alignment are remembered.
         */
        int     unitWidth             = 0;
        int     valueDomainAlignment  = 0;
        boolean writeCodespaces       = (groupCodespace == null);
        final   StringBuffer  buffer  = new StringBuffer();
        final   FieldPosition dummyFP = new FieldPosition(-1);
        for (final Map.Entry<GeneralParameterDescriptor,ParameterTableRow> entry : descriptorValues.entrySet()) {
            final GeneralParameterDescriptor descriptor = entry.getKey();
            if (descriptor instanceof ParameterDescriptor<?>) {
                final ParameterTableRow row = entry.getValue();
                /*
                 * Verify if all rows use the same codespace than the header, in which case we can omit
                 * row codespace formatting.
                 */
                if (!writeCodespaces && !groupCodespace.equals(entry.getValue().getCodeSpace())) {
                    writeCodespaces = true;
                }
                /*
                 * Format the value domain, so we can compute the character position on which to perform alignment.
                 */
                final Range<?> valueDomain = Parameters.getValueDomain((ParameterDescriptor<?>) descriptor);
                if (valueDomain != null) {
                    final int p = row.setValueDomain(valueDomain, getFormat(Range.class), buffer);
                    if (p > valueDomainAlignment) {
                        valueDomainAlignment = p;
                    }
                }
                /*
                 * Singleton array conversion. Because it may be an array of primitive types, we can not just
                 * cast to Object[]. Then formats the units, with a space before the unit if the symbol is a
                 * letter or digit (i.e. we do not put a space in front of ° symbol for instance).
                 */
                row.expandSingleton();
                final int length = row.units.size();
                for (int i=0; i<length; i++) {
                    final Object unit = row.units.get(i);
                    if (unit != null) {
                        if (getFormat(Unit.class).format(unit, buffer, dummyFP).length() != 0) {
                            if (Character.isLetterOrDigit(buffer.codePointAt(0))) {
                                buffer.insert(0, ' ');
                            }
                        }
                        final String symbol = buffer.toString();
                        row.units.set(i, symbol);
                        buffer.setLength(0);
                        final int p = symbol.length();
                        if (p > unitWidth) {
                            unitWidth = p;
                        }
                    }
                }
            }
        }
        /*
         * Finished to prepare information. Now begin the actual writing.
         * First, formats the table header (i.e. the column names).
         */
        final Vocabulary resources = Vocabulary.getResources(displayLocale);
        header.writeIdentifiers(out, true, colors, false, lineSeparator);
        out.append(lineSeparator);
        final char horizontalBorder = isBrief ? '─' : '═';
        final TableAppender table = (isBrief || !columnSeparator.equals(SEPARATOR)) ?
                new TableAppender(out, columnSeparator) : new TableAppender(out);
        table.setMultiLinesCells(true);
        table.nextLine(horizontalBorder);
        int numColumnsBeforeValue = 0;
        for (int i=0; ; i++) {
            boolean end = false;
            final short key;
            switch (i) {
                case 0: {
                    key = Vocabulary.Keys.Name;
                    break;
                }
                case 1: {
                    key = Vocabulary.Keys.Type;
                    break;
                }
                case 2: {
                    if (!showObligation) {
                       continue;
                    }
                    key = Vocabulary.Keys.Obligation;
                    break;
                }
                case 3: {
                    key = Vocabulary.Keys.ValueDomain;
                    break;
                }
                case 4: {
                    key = (values == null) ? Vocabulary.Keys.DefaultValue : Vocabulary.Keys.Value;
                    end = true;
                    break;
                }
                default: throw new AssertionError(i);
            }
            if (hasColors) table.append(X364.BOLD.sequence());
            table.append(resources.getString(key));
            if (hasColors) table.append(X364.NORMAL.sequence());
            if (!writeCodespaces && i == 0) {
                table.append(" (").append(groupCodespace).append(')');
            }
            if (end) break;
            nextColumn(table);
            numColumnsBeforeValue++;
        }
        table.nextLine();
        /*
         * Now process to the formatting of (descriptor,value) pairs. Each descriptor's alias
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
            row.writeIdentifiers(table, writeCodespaces, null, hasColors, lineSeparator);
            nextColumn(table);
            final GeneralParameterDescriptor generalDescriptor = entry.getKey();
            if (generalDescriptor instanceof ParameterDescriptor<?>) {
                /*
                 * Writes value type.
                 */
                final ParameterDescriptor<?> descriptor = (ParameterDescriptor<?>) generalDescriptor;
                final Class<?> valueClass = descriptor.getValueClass();
                if (valueClass != null) {  // Should never be null, but let be safe.
                    table.append(getFormat(Class.class).format(valueClass, buffer, dummyFP).toString());
                }
                nextColumn(table);
                buffer.setLength(0);
                /*
                 * Writes the obligation (mandatory or optional).
                 */
                if (showObligation) {
                    final int minimumOccurs = descriptor.getMinimumOccurs();
                    final int maximumOccurs = descriptor.getMaximumOccurs();
                    if (maximumOccurs == 1) {
                        table.append(resources.getString(minimumOccurs == 0 ?
                                Vocabulary.Keys.Optional : Vocabulary.Keys.Mandatory));
                    } else {
                        final Format f = getFormat(Integer.class);
                        table.append(f.format(minimumOccurs, buffer, dummyFP).toString()).append(" … ");
                        buffer.setLength(0);
                        if (maximumOccurs == Integer.MAX_VALUE) {
                            table.append('∞');
                        } else {
                            table.append(f.format(maximumOccurs, buffer, dummyFP).toString());
                            buffer.setLength(0);
                        }
                    }
                    nextColumn(table);
                }
                /*
                 * Writes minimum and maximum values, together with the unit of measurement (if any).
                 */
                final String valueDomain = row.valueDomain;
                if (valueDomain != null) {
                    table.append(CharSequences.spaces(valueDomainAlignment - row.valueDomainAlignment)).append(valueDomain);
                }
                nextColumn(table);
                /*
                 * Writes the values, each on its own line, together with their unit of measurement.
                 */
                table.setCellAlignment(TableAppender.ALIGN_RIGHT);
                final int length = row.values.size();
                for (int i=0; i<length; i++) {
                    Object value = row.values.get(i);
                    if (value != null) {
                        if (i != 0) {
                            /*
                             * If the same parameter is repeated more than once (not allowed by ISO 19111,
                             * but this extra flexibility is allowed by Apache SIS), write the ditto mark
                             * in all previous columns (name, type, etc.) on a new row.
                             */
                            final String ditto = resources.getString(Vocabulary.Keys.DittoMark);
                            table.nextLine();
                            table.setCellAlignment(TableAppender.ALIGN_CENTER);
                            for (int j=0; j<numColumnsBeforeValue; j++) {
                                table.append(ditto);
                                nextColumn(table);
                            }
                            table.setCellAlignment(TableAppender.ALIGN_RIGHT);
                        }
                        /*
                         * Format the value followed by the unit of measure, or followed by spaces if there is no unit
                         * for this value. The intend is the right align the numerical value rather than the numerical
                         * + unit tupple.
                         */
                        final Format format = getFormat(value.getClass());
                        if (format != null) {
                            if (format instanceof NumberFormat && value instanceof Number) {
                                configure((NumberFormat) format, Math.abs(((Number) value).doubleValue()));
                            }
                            value = format.format(value, buffer, dummyFP);
                        }
                        table.append(value.toString());
                        buffer.setLength(0);
                        int pad = unitWidth;
                        final String unit = (String) row.units.get(i);
                        if (unit != null) {
                            table.append(unit);
                            pad -= unit.length();
                        }
                        table.append(CharSequences.spaces(pad));
                    }
                }
            }
            table.nextLine();
            table.setCellAlignment(TableAppender.ALIGN_LEFT);
        }
        table.nextLine(horizontalBorder);
        table.flush();
        /*
         * Write remarks, if any.
         */
        for (final Map.Entry<String,Integer> remark : remarks.entrySet()) {
            ParameterTableRow.writeFootnoteNumber(out, remark.getValue());
            out.append(' ').append(remark.getKey()).append(lineSeparator);
        }
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
     * Configures the number pattern to use for the given value. The main intend of this method is to ensure that
     * the map projection scale factor (a value close to 1) is formatted with a sufficient number of fraction digits.
     * A common default NumberFormat precision is 3 digits, which is not sufficient. For example the scale factor of
     * Transverse Mercator projections is 0.9996 (4 digits), and the scale factor of "NTF (Paris) / Lambert zone II"
     * projection is 0.99987742 (8 digits).
     *
     * @param format The format to configure.
     * @param m The absolute value (magnitude) of the value to write.
     */
    private static void configure(final NumberFormat format, final double m) {
        if (format.getMaximumFractionDigits() <= 9) {
            /*
             * If the maximum fraction digits is higher than 9, then that value has not been set by this class.
             * Maybe the user overrides the createFormat(Class<?>) method in his own subclass, in which case we
             * will respect his wish and not set a lower value here.
             */
            final int n;
            if (m < 10) {
                n = 9;
            } else if (m < 1000) {  // No real use case for this threshold yet, but added for more progressive behavior.
                n = 6;
            } else {
                n = 3;
            }
            /*
             * The minimum fraction digits is usually 0. But if we find a higher value (for example because the
             * user overrides the createFormat(Class<?>) method), then we will respect user's wish and not set
             * a lower value.
             */
            if (n >= format.getMinimumFractionDigits()) {
                format.setMaximumFractionDigits(n);
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
        final Vocabulary resources = Vocabulary.getResources(displayLocale);
        /*
         * Prepares all rows before we write them to the output stream, because not all
         * identified objects may have names with the same scopes in the same order. We
         * also need to iterate over all rows in order to know the number of columns.
         *
         * The first column is reserved for the identifier. We put null as a sentinal key for
         * that column name, to be replaced later by "Identifier" in user locale. We can not
         * put the localized strings in the map right now because they could conflict with
         * the scope of some alias to be processed below.
         */
        boolean hasIdentifiers = false;
        final List<String[]> rows = new ArrayList<String[]>();
        final Map<String,Integer> columnIndices = new LinkedHashMap<String,Integer>();
        columnIndices.put(null, 0); // See above comment for the meaning of "null" here.
        if (preferredCodespaces != null) {
            for (final String codespace : preferredCodespaces) {
                columnIndices.put(codespace, columnIndices.size());
            }
        }
        for (final IdentifiedObject object : objects) {
            String[] row = new String[columnIndices.size()]; // Will growth later if needed.
            /*
             * Put the first identifier in the first column. If no identifier has a codespace in the list
             * supplied by the user, then we will use the first identifier (any codespace) as a fallback.
             */
            final Set<ReferenceIdentifier> identifiers = object.getIdentifiers();
            if (identifiers != null) { // Paranoiac check.
                Identifier identifier = null;
                for (final ReferenceIdentifier candidate : identifiers) {
                    if (candidate != null) { // Paranoiac check.
                        if (isPreferredCodespace(candidate.getCodeSpace())) {
                            identifier = candidate;
                            break; // Format now.
                        }
                        if (identifier == null) {
                            identifier = candidate; // To be used as a fallback if we find nothing better.
                        }
                    }
                }
                if (identifier != null) {
                    row[0] = IdentifiedObjects.toString(identifier);
                    hasIdentifiers = true;
                }
            }
            /*
             * If the name's codespace is in the list of codespaces asked by the user, add that name
             * in the current row and clear the 'name' locale variable. Otherwise, keep the 'name'
             * locale variable in case we found no alias to format.
             */
            ReferenceIdentifier name = object.getName();
            if (name != null) { // Paranoiac check.
                final String codespace = name.getCodeSpace();
                if (isPreferredCodespace(codespace)) {
                    row = putIfAbsent(resources, row, columnIndices, codespace, name.getCode());
                    name = null;
                }
            }
            /*
             * Put all aliases having a codespace in the list asked by the user.
             */
            final Collection<GenericName> aliases = object.getAlias();
            if (aliases != null) { // Paranoiac check.
                for (final GenericName alias : aliases) {
                    if (alias != null) { // Paranoiac check.
                        final String codespace = NameToIdentifier.getCodeSpace(alias, displayLocale);
                        if (isPreferredCodespace(codespace)) {
                            row = putIfAbsent(resources, row, columnIndices, codespace,
                                    alias.tip().toInternationalString().toString(displayLocale));
                            name = null;
                        }
                    }
                }
            }
            /*
             * If no name and no alias have a codespace in the list of codespaces asked by the user,
             * force the addition of primary name regardless its codespace.
             */
            if (name != null) {
                row = putIfAbsent(resources, row, columnIndices, name.getCodeSpace(), name.getCode());
            }
            rows.add(row);
        }
        /*
         * Writes the table. The header will contain one column for each codespace in the order declared
         * by the user. If the user did not specified any codespace, or if we had to write codespace not
         * on the user list, then those codespaces will be written in the order we found them.
         */
        final boolean hasColors = (colors != null);
        final TableAppender table = new TableAppender(out, columnSeparator);
        table.setMultiLinesCells(true);
        table.appendHorizontalSeparator();
        for (String codespace : columnIndices.keySet()) {
            if (codespace == null) {
                if (!hasIdentifiers) continue; // Skip empty column.
                codespace = resources.getString(Vocabulary.Keys.Identifier);
            }
            if (hasColors) {
                codespace = X364.BOLD.sequence() + codespace + X364.NORMAL.sequence();
            }
            table.append(codespace);
            nextColumn(table);
        }
        table.appendHorizontalSeparator();
        /*
         * Writes row content.
         */
        final int numColumns = columnIndices.size();
        for (final String[] row : rows) {
            for (int i=hasIdentifiers ? 0 : 1; i<numColumns; i++) {
                if (i < row.length) {
                    final String name = row[i];
                    if (name != null) {
                        table.append(name);
                    }
                }
                nextColumn(table);
            }
            table.nextLine();
        }
        table.appendHorizontalSeparator();
        table.flush();
    }

    /**
     * Stores a value in the given position of the given row, expanding the array if needed.
     * This operation is performed only if no value already exists in the cell.
     *
     * @param  row           All columns in a single row.
     * @param  columnIndices Indices of columns for each codespace.
     * @param  codespace     The codespace of the name or alias to add.
     * @param  name          The code of the name or alias to add.
     * @return {@code row}, or a new array if it was necessary to expand the row.
     */
    private static String[] putIfAbsent(final Vocabulary resources, String[] row,
            final Map<String,Integer> columnIndices, String codespace, final String name)
    {
        if (codespace == null) {
            codespace = resources.getString(Vocabulary.Keys.Unnamed);
        }
        final Integer columnIndex = columnIndices.get(codespace);
        final int i;
        if (columnIndex != null) {
            i = columnIndex;
        } else {
            i = columnIndices.size();
            columnIndices.put(codespace, i);
        }
        if (i >= row.length) {
            row = Arrays.copyOf(row, i + 1);
        }
        if (row[i] == null) {
            row[i] = name;
        }
        return row;
    }

    /**
     * Returns a shared instance of {@code ParameterFormat} if possible, or a new one otherwise.
     */
    private static ParameterFormat getSharedInstance(final Colors colors) {
        ParameterFormat f = INSTANCE.getAndSet(null);
        if (f == null) {
            f = new ParameterFormat();
        }
        f.setColors(colors);
        return f;
    }

    /**
     * Formats the given object using a shared instance of {@code ParameterFormat}.
     * This is used for {@link DefaultParameterDescriptorGroup#toString()} implementation.
     */
    static String sharedFormat(final Object object) {
        final ParameterFormat f = getSharedInstance(null);
        final String s = f.format(object);
        INSTANCE.set(f);
        return s;
    }

    /**
     * Writes the given object to the console using a shared instance of {@code ParameterFormat}.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    static void print(final Object object) {
        final Console console = System.console();
        final Appendable out = (console != null) ? console.writer() : System.out;
        final ParameterFormat f = getSharedInstance(Colors.NAMING);
        try {
            f.format(object, out);
        } catch (IOException e) {
            throw new AssertionError(e); // Should never happen, since we are writing to stdout.
        }
        INSTANCE.set(f);
    }

    /**
     * Not yet supported.
     *
     * @return Currently never return.
     * @throws ParseException Currently always thrown.
     */
    @Override
    public Object parse(final CharSequence text, final ParsePosition pos) throws ParseException {
        throw new ParseException(Errors.getResources(displayLocale)
                .getString(Errors.Keys.UnsupportedOperation_1, "parse"), 0);
    }
}
