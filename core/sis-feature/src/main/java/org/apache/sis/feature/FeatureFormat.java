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
package org.apache.sis.feature;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Collection;
import java.util.Locale;
import java.util.TimeZone;
import java.io.IOException;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import java.util.concurrent.atomic.AtomicReference;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.util.InternationalString;
import org.opengis.util.GenericName;
import org.apache.sis.io.TableAppender;
import org.apache.sis.io.TabularFormat;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.Characters;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.math.MathFunctions;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.UncheckedIOException;


/**
 * Formats {@linkplain AbstractFeature features} or {@linkplain DefaultFeatureType feature types} in a tabular format.
 * This format assumes a monospaced font and an encoding supporting drawing box characters (e.g. UTF-8).
 *
 * <div class="note"><b>Example:</b> a feature named “City” and containing 3 properties (“name”, “population” and
 * “twin town”) may be formatted like below. The two first properties are {@linkplain AbstractAttribute attributes}
 * while the last property is an {@linkplain AbstractAssociation association} to an other feature.
 *
 * {@preformat text
 *   City
 *   ┌────────────┬─────────┬─────────────┬───────────┐
 *   │ Name       │ Type    │ Cardinality │ Value     │
 *   ├────────────┼─────────┼─────────────┼───────────┤
 *   │ name       │ String  │ [1 … 1]     │ Paderborn │
 *   │ population │ Integer │ [1 … 1]     │ 143,174   │
 *   │ twin town  │ City    │ [0 … ∞]     │ Le Mans   │
 *   └────────────┴─────────┴─────────────┴───────────┘
 * }</div>
 *
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>The current implementation can only format features — parsing is not yet implemented.</li>
 *   <li>{@code FeatureFormat}, like most {@code java.text.Format} subclasses, is not thread-safe.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.5
 * @module
 */
public class FeatureFormat extends TabularFormat<Object> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5792086817264884947L;

    /**
     * An instance created when first needed and potentially shared.
     */
    private static final AtomicReference<FeatureFormat> INSTANCE = new AtomicReference<>();

    /**
     * The locale for international strings.
     */
    private final Locale displayLocale;

    /**
     * The columns to include in the table formatted by this {@code FeatureFormat}.
     * By default, all columns having at least one value are included.
     */
    private final EnumSet<Column> columns = EnumSet.allOf(Column.class);

    /**
     * Maximal length of attribute values, in number of characters.
     * If a value is longer than this length, it will be truncated.
     *
     * <p>This is defined as a static final variable for now because its value is approximative:
     * it is a number of characters instead than a number of code points, and that length may be
     * exceeded by a few characters if the overflow happen while appending the list separator.</p>
     */
    private static final int MAXIMAL_VALUE_LENGTH = 40;

    /**
     * Creates a new formatter for the default locale and timezone.
     */
    public FeatureFormat() {
        super(Locale.getDefault(Locale.Category.FORMAT), TimeZone.getDefault());
        displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
        columnSeparator = " │ ";
    }

    /**
     * Creates a new formatter for the given locale and timezone.
     *
     * @param  locale    the locale, or {@code null} for {@code Locale.ROOT}.
     * @param  timezone  the timezone, or {@code null} for UTC.
     */
    public FeatureFormat(final Locale locale, final TimeZone timezone) {
        super(locale, timezone);
        displayLocale = (locale != null) ? locale : Locale.ROOT;
        columnSeparator = " │ ";
    }

    /**
     * Returns the type of objects formatted by this class. This method has to return {@code Object.class}
     * since it is the only common parent to {@code Feature} and {@link FeatureType}.
     *
     * @return {@code Object.class}
     */
    @Override
    public final Class<Object> getValueType() {
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
     * @param  category  the category for which a locale is desired.
     * @return the locale for the given category (never {@code null}).
     */
    @Override
    public Locale getLocale(final Locale.Category category) {
        return (category == Locale.Category.DISPLAY) ? displayLocale : super.getLocale(category);
    }

    /**
     * Returns all columns that may be shown in the tables to format.
     * The columns included in the set may be shown, but not necessarily;
     * some columns will still be omitted if they are completely empty.
     * However columns <em>not</em> included in the set are guaranteed to be omitted.
     *
     * @return all columns that may be shown in the tables to format.
     *
     * @since 0.8
     */
    public Set<Column> getAllowedColumns() {
        return columns.clone();
    }

    /**
     * Sets all columns that may be shown in the tables to format.
     * Note that the columns specified to this method are not guaranteed to be shown;
     * some columns will still be omitted if they are completely empty.
     *
     * @param inclusion  all columns that may be shown in the tables to format.
     *
     * @since 0.8
     */
    public void setAllowedColumns(final Set<Column> inclusion) {
        ArgumentChecks.ensureNonNull("inclusion", inclusion);
        columns.clear();
        columns.addAll(inclusion);
    }

    /**
     * Identifies the columns to include in the table formatted by {@code FeatureFormat}.
     * By default, all columns having at least one non-null value are shown. But a smaller
     * set of columns can be specified to the {@link FeatureFormat#setAllowedColumns(Set)}
     * method for formatting narrower tables.
     *
     * @see FeatureFormat#setAllowedColumns(Set)
     *
     * @since 0.8
     */
    public enum Column {
        /**
         * Natural language designator for the property.
         * This is the character sequence returned by {@link AbstractIdentifiedType#getDesignation()}.
         * This column is omitted if no property has a designation.
         */
        DESIGNATION(Vocabulary.Keys.Designation),

        /**
         * Name of the property.
         * This is the character sequence returned by {@link AbstractIdentifiedType#getName()}.
         */
        NAME(Vocabulary.Keys.Name),

        /**
         * Type of property values. This is the type returned by {@link DefaultAttributeType#getValueClass()} or
         * {@link DefaultAssociationRole#getValueType()}.
         */
        TYPE(Vocabulary.Keys.Type),

        /**
         * The minimum and maximum occurrences of attribute values. This is made from the numbers returned
         * by {@link DefaultAttributeType#getMinimumOccurs()} and {@link DefaultAttributeType#getMaximumOccurs()}.
         */
        CARDINALITY(Vocabulary.Keys.Cardinality),

        /**
         * Property value (for properties) or default value (for property types).
         * This is the value returned by {@link AbstractAttribute#getValue()}, {@link AbstractAssociation#getValue()}
         * or {@link DefaultAttributeType#getDefaultValue()}.
         */
        VALUE(Vocabulary.Keys.Value),

        /**
         * Other attributes that describes the attribute.
         * This is made from the map returned by {@link AbstractAttribute#characteristics()}.
         * This column is omitted if no property has characteristics.
         */
        CHARACTERISTICS(Vocabulary.Keys.Characteristics),

        /**
         * Whether a property is deprecated, or other remarks.
         * This column is omitted if no property has remarks.
         */
        REMARKS(Vocabulary.Keys.Remarks);

        /**
         * The {@link Vocabulary} key to use for formatting the header of this column.
         */
        final short resourceKey;

        /**
         * Creates a new column enumeration constant.
         */
        private Column(final short key) {
            resourceKey = key;
        }
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
     *   <li>{@code Feature}</li>
     *   <li>{@code FeatureType}</li>
     * </ul>
     *
     * @throws IOException if an error occurred while writing to the given appendable.
     */
    @Override
    public void format(final Object object, final Appendable toAppendTo) throws IOException {
        ArgumentChecks.ensureNonNull("object",     object);
        ArgumentChecks.ensureNonNull("toAppendTo", toAppendTo);
        /*
         * Separate the Feature (optional) and the FeatureType (mandatory) instances.
         */
        final DefaultFeatureType featureType;
        final AbstractFeature feature;
        if (object instanceof AbstractFeature) {
            feature     = (AbstractFeature) object;
            featureType = feature.getType();
        } else if (object instanceof DefaultFeatureType) {
            featureType = (DefaultFeatureType) object;
            feature     = null;
        } else {
            throw new IllegalArgumentException(Errors.getResources(displayLocale)
                    .getString(Errors.Keys.UnsupportedType_1, object.getClass()));
        }
        /*
         * Computes the columns to show. We start with the set of columns specified by setAllowedColumns(Set),
         * then we check if some of those columns are empty. For example in many cases there is no attribute
         * with characteritic, in which case we will ommit the whole "characteristics" column. We perform such
         * check only for optional information, not for mandatory information like property names.
         */
        final EnumSet<Column> visibleColumns = columns.clone();
        {
            boolean hasDesignation     = false;
            boolean hasCharacteristics = false;
            boolean hasDeprecatedTypes = false;
            for (final AbstractIdentifiedType propertyType : featureType.getProperties(true)) {
                if (!hasDesignation) {
                    hasDesignation = propertyType.getDesignation() != null;
                }
                if (!hasCharacteristics && propertyType instanceof DefaultAttributeType<?>) {
                    hasCharacteristics = !((DefaultAttributeType<?>) propertyType).characteristics().isEmpty();
                }
                if (!hasDeprecatedTypes && propertyType instanceof Deprecable) {
                    hasDeprecatedTypes = ((Deprecable) propertyType).isDeprecated();
                }
            }
            if (!hasDesignation)     visibleColumns.remove(Column.DESIGNATION);
            if (!hasCharacteristics) visibleColumns.remove(Column.CHARACTERISTICS);
            if (!hasDeprecatedTypes) visibleColumns.remove(Column.REMARKS);
        }
        /*
         * Format the feature type name. In the case of feature type, format also the names of super-type
         * after the UML symbol for inheritance (an arrow with white head). We do not use the " : " ASCII
         * character for avoiding confusion with the ":" separator in namespaces. After the feature (type)
         * name, format the column header: property name, type, cardinality and (default) value.
         */
        toAppendTo.append(toString(featureType.getName()));
        if (feature == null) {
            String separator = " ⇾ ";                                       // UML symbol for inheritance.
            for (final FeatureType parent : featureType.getSuperTypes()) {
                toAppendTo.append(separator).append(toString(parent.getName()));
                separator = ", ";
            }
        }
        toAppendTo.append(getLineSeparator());
        /*
         * Create a table and format the header. Columns will be shown in Column enumeration order.
         */
        final Vocabulary resources = Vocabulary.getResources(displayLocale);
        final TableAppender table = new TableAppender(toAppendTo, columnSeparator);
        table.setMultiLinesCells(true);
        table.nextLine('─');
        boolean isFirstColumn = true;
        for (final Column column : visibleColumns) {
            short key = column.resourceKey;
            if (key == Vocabulary.Keys.Value && feature == null) {
                key = Vocabulary.Keys.DefaultValue;
            }
            if (!isFirstColumn) nextColumn(table);
            table.append(resources.getString(key));
            isFirstColumn = false;
        }
        table.nextLine();
        table.nextLine('─');
        /*
         * Done writing the header. Now write all property rows.  For each row, the first part in the loop
         * extracts all information needed without formatting anything yet. If we detect in that part that
         * a row has no value, it will be skipped if and only if that row is optional (minimum occurrence
         * of zero).
         */
        final StringBuffer  buffer  = new StringBuffer();
        final FieldPosition dummyFP = new FieldPosition(-1);
        final List<String>  remarks = new ArrayList<>();
        for (final AbstractIdentifiedType propertyType : featureType.getProperties(true)) {
            Object value = null;
            int cardinality = -1;
            if (feature != null) {
                if (!(propertyType instanceof DefaultAttributeType<?>) &&
                    !(propertyType instanceof DefaultAssociationRole) &&
                    !DefaultFeatureType.isParameterlessOperation(propertyType))
                {
                    continue;
                }
                value = feature.getPropertyValue(propertyType.getName().toString());
                if (value == null) {
                    if (propertyType instanceof FieldType && ((FieldType) propertyType).getMinimumOccurs() == 0) {
                        continue;                           // If optional and no value, skip the full row.
                    }
                    cardinality = 0;
                } else if (value instanceof Collection<?>) {
                    cardinality = ((Collection<?>) value).size();
                } else {
                    cardinality = 1;
                }
            } else if (propertyType instanceof DefaultAttributeType<?>) {
                value = ((DefaultAttributeType<?>) propertyType).getDefaultValue();
            } else if (propertyType instanceof AbstractOperation) {
                buffer.append(" = ");
                try {
                    ((AbstractOperation) propertyType).formatResultFormula(buffer);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);      // Should never happen since we write in a StringBuffer.
                }
                value = CharSequences.trimWhitespaces(buffer).toString();
                buffer.setLength(0);
            }
            final String   valueType;                       // The value to write in the type column.
            final Class<?> valueClass;                      // AttributeType.getValueClass() if applicable.
            final int minimumOccurs, maximumOccurs;         // Negative values mean no cardinality.
            final AbstractIdentifiedType resultType;        // Result of operation if applicable.
            if (propertyType instanceof AbstractOperation) {
                resultType = ((AbstractOperation) propertyType).getResult();        // May be null
            } else {
                resultType = propertyType;
            }
            if (resultType instanceof DefaultAttributeType<?>) {
                final DefaultAttributeType<?> pt = (DefaultAttributeType<?>) resultType;
                minimumOccurs = pt.getMinimumOccurs();
                maximumOccurs = pt.getMaximumOccurs();
                valueClass    = pt.getValueClass();
                valueType     = getFormat(Class.class).format(valueClass, buffer, dummyFP).toString();
                buffer.setLength(0);
            } else if (resultType instanceof DefaultAssociationRole) {
                final DefaultAssociationRole pt = (DefaultAssociationRole) resultType;
                minimumOccurs = pt.getMinimumOccurs();
                maximumOccurs = pt.getMaximumOccurs();
                valueType     = toString(DefaultAssociationRole.getValueTypeName(pt));
                valueClass    = AbstractFeature.class;
            } else {
                valueType  = (resultType != null) ? toString(resultType.getName()) : "";
                valueClass = null;
                minimumOccurs = -1;
                maximumOccurs = -1;
            }
            /*
             * At this point we determined that the row should not be skipped
             * and we got all information to format.
             */
            isFirstColumn = true;
            for (final Column column : visibleColumns) {
                if (!isFirstColumn) nextColumn(table);
                isFirstColumn = false;
                switch (column) {
                    case DESIGNATION: {
                        final InternationalString d = propertyType.getDesignation();
                        if (d != null) table.append(d.toString(displayLocale));
                        break;
                    }
                    case NAME: {
                        table.append(toString(propertyType.getName()));
                        break;
                    }
                    case TYPE: {
                        table.append(valueType);
                        break;
                    }
                    case CARDINALITY: {
                        table.setCellAlignment(TableAppender.ALIGN_RIGHT);
                        if (cardinality >= 0) {
                            table.append(getFormat(Integer.class).format(cardinality, buffer, dummyFP));
                            buffer.setLength(0);
                        }
                        if (maximumOccurs >= 0) {
                            if (cardinality >= 0) {
                                table.append(' ')
                                     .append((cardinality >= minimumOccurs && cardinality <= maximumOccurs) ? '∈' : '∉')
                                     .append(' ');
                            }
                            final Format format = getFormat(Integer.class);
                            table.append('[').append(format.format(minimumOccurs, buffer, dummyFP)).append(" … ");
                            buffer.setLength(0);
                            if (maximumOccurs != Integer.MAX_VALUE) {
                                table.append(format.format(maximumOccurs, buffer, dummyFP));
                            } else {
                                table.append('∞');
                            }
                            buffer.setLength(0);
                            table.append(']');
                        }
                        break;
                    }
                    case VALUE: {
                        table.setCellAlignment(TableAppender.ALIGN_LEFT);
                        final Format format = getFormat(valueClass);                            // Null if valueClass is null.
                        final Iterator<?> it = CollectionsExt.toCollection(value).iterator();
                        String separator = "";
                        int length = 0;
                        while (it.hasNext()) {
                            value = it.next();
                            if (value != null) {
                                if (propertyType instanceof DefaultAssociationRole) {
                                    final String p = DefaultAssociationRole.getTitleProperty((DefaultAssociationRole) propertyType);
                                    if (p != null) {
                                        value = ((AbstractFeature) value).getPropertyValue(p);
                                        if (value == null) continue;
                                    }
                                } else if (format != null && valueClass.isInstance(value)) {    // Null safe because of getFormat(valueClass) contract.
                                    /*
                                     * Convert numbers, dates, angles, etc. to character sequences before to append them in the table.
                                     * Note that DecimalFormat writes Not-a-Number as "NaN" in some locales and as "�" in other locales
                                     * (U+FFFD - Unicode replacement character). The "�" seems to be used mostly for historical reasons;
                                     * as of 2017 the Unicode Common Locale Data Repository (CLDR) seems to define "NaN" for all locales.
                                     * We could configure DecimalFormatSymbols for using "NaN", but (for now) we rather substitute "�" by
                                     * "NaN" here for avoiding to change the DecimalFormat configuration and for distinguishing the NaNs.
                                     */
                                    final StringBuffer t = format.format(value, buffer, dummyFP);
                                    if (value instanceof Number) {
                                        final float f = ((Number) value).floatValue();
                                        if (Float.isNaN(f)) {
                                            if ("�".contentEquals(t)) {
                                                t.setLength(0);
                                                t.append("NaN");
                                            }
                                            final int n = MathFunctions.toNanOrdinal(f);
                                            if (n > 0) buffer.append(" #").append(n);
                                        }
                                    }
                                    value = t;
                                }
                                /*
                                 * All values: the numbers, dates, angles, etc. formatted above, any other character sequences
                                 * (e.g. InternationalString), or other kind of values - some of them handled in a special way.
                                 */
                                length = formatValue(value, table.append(separator), length);
                                buffer.setLength(0);
                                if (length < 0) break;      // Value is too long, abandon remaining iterations.
                                separator = ", ";
                                length += 2;
                            }
                        }
                        break;
                    }
                    case CHARACTERISTICS: {
                        if (propertyType instanceof DefaultAttributeType<?>) {
                            String separator = "";
                            for (final DefaultAttributeType<?> attribute : ((DefaultAttributeType<?>) propertyType).characteristics().values()) {
                                table.append(separator).append(toString(attribute.getName()));
                                Object c = attribute.getDefaultValue();
                                if (feature != null) {
                                    final Object p = feature.getProperty(propertyType.getName().toString());
                                    if (p instanceof AbstractAttribute<?>) {    // Should always be true, but we are paranoiac.
                                        c = ((AbstractAttribute<?>) p).characteristics().get(attribute.getName().toString());
                                    }
                                }
                                if (c != null) {
                                    formatValue(c, table.append(" = "), 0);
                                }
                                separator = ", ";
                            }
                        }
                        break;
                    }
                    case REMARKS: {
                        if (org.apache.sis.feature.Field.isDeprecated(propertyType)) {
                            table.append(resources.getString(Vocabulary.Keys.Deprecated));
                            final InternationalString r = ((Deprecable) propertyType).getRemarks();
                            if (r != null) {
                                remarks.add(r.toString(displayLocale));
                                appendSuperscript(remarks.size(), table);
                            }
                        }
                        break;
                    }
                }
            }
            table.nextLine();
        }
        table.nextLine('─');
        table.flush();
        /*
         * If there is any remarks, write them below the table.
         */
        final int n = remarks.size();
        for (int i=0; i<n; i++) {
            appendSuperscript(i+1, toAppendTo);
            toAppendTo.append(' ').append(remarks.get(i)).append(lineSeparator);
        }
    }

    /**
     * Returns the display name for the given {@code GenericName}.
     */
    private String toString(final GenericName name) {
        if (name == null) {                                             // Should not be null, but let be safe.
            return "";
        }
        final InternationalString i18n = name.toInternationalString();
        if (i18n != null) {                                             // Should not be null, but let be safe.
            final String s = i18n.toString(displayLocale);
            if (s != null) {
                return s;
            }
        }
        return name.toString();
    }

    /**
     * Appends the given attribute value, in a truncated form if it exceed the maximal value length.
     *
     * @param  value   the value to append.
     * @param  table   where to append the value.
     * @param  length  number of characters appended before this method call in the current table cell.
     * @return number of characters appended after this method call in the current table cell, or -1 if
     *         the length exceed the maximal length (in which case the caller should break iteration).
     */
    private int formatValue(final Object value, final TableAppender table, final int length) {
        final String text;
        if (value instanceof InternationalString) {
            text = ((InternationalString) value).toString(displayLocale);
        } else if (value instanceof GenericName) {
            text = toString((GenericName) value);
        } else if (value instanceof AbstractIdentifiedType) {
            text = toString(((AbstractIdentifiedType) value).getName());
        } else if (value instanceof IdentifiedObject) {
            text = IdentifiedObjects.getIdentifierOrName((IdentifiedObject) value);
        } else {
            text = value.toString();
        }
        final int remaining = MAXIMAL_VALUE_LENGTH - length;
        if (remaining >= text.length()) {
            table.append(text);
            return length + text.length();
        } else {
            table.append(text, 0, Math.max(0, remaining - 1)).append('…');
            return -1;
        }
    }

    /**
     * Appends the given number as an superscript if possible, or as an ordinary number otherwise.
     */
    private static void appendSuperscript(final int n, final Appendable toAppendTo) throws IOException {
        if (n >= 0 && n < 10) {
            toAppendTo.append(Characters.toSuperScript((char) ('0' + n)));
        } else {
            toAppendTo.append('(').append(String.valueOf(n)).append(')');
        }
    }

    /**
     * Formats the given object using a shared instance of {@code ParameterFormat}.
     * This is used for {@link DefaultFeatureType#toString()} implementation.
     */
    static String sharedFormat(final Object object) {
        FeatureFormat f = INSTANCE.getAndSet(null);
        if (f == null) {
            f = new FeatureFormat();
        }
        final String s = f.format(object);
        INSTANCE.set(f);
        return s;
    }

    /**
     * Not yet supported.
     *
     * @return currently never return.
     * @throws ParseException currently always thrown.
     */
    @Override
    public Object parse(final CharSequence text, final ParsePosition pos) throws ParseException {
        throw new ParseException(Errors.getResources(displayLocale)
                .getString(Errors.Keys.UnsupportedOperation_1, "parse"), pos.getIndex());
    }

    /**
     * Returns a clone of this format.
     *
     * @return a clone of this format.
     */
    @Override
    public FeatureFormat clone() {
        return (FeatureFormat) super.clone();
    }
}
