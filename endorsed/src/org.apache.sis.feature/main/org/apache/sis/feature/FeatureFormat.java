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

import java.util.ArrayList;
import java.util.Set;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Collection;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.util.InternationalString;
import org.opengis.util.GenericName;
import org.apache.sis.io.TableAppender;
import org.apache.sis.io.TabularFormat;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.Characters;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.math.MathFunctions;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Property;
import org.opengis.feature.PropertyType;
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.FeatureAssociation;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.Operation;


/**
 * Formats {@linkplain AbstractFeature features} or {@linkplain DefaultFeatureType feature types} in a tabular format.
 * This format assumes a monospaced font and an encoding supporting drawing box characters (e.g. UTF-8).
 *
 * <h2>Example</h2>
 * A feature named “City” and containing 3 properties (“name”, “population” and “twin town”)
 * may be formatted like below. The two first properties are {@linkplain AbstractAttribute attributes}
 * while the last property is an {@linkplain AbstractAssociation association} to another feature.
 *
 * <pre class="text">
 *   City
 *   ┌────────────┬─────────┬──────────────┬───────────┐
 *   │ Name       │ Type    │ Multiplicity │ Value     │
 *   ├────────────┼─────────┼──────────────┼───────────┤
 *   │ name       │ String  │ [1 … 1]      │ Paderborn │
 *   │ population │ Integer │ [1 … 1]      │ 143,174   │
 *   │ twin town  │ City    │ [0 … ∞]      │ Le Mans   │
 *   └────────────┴─────────┴──────────────┴───────────┘</pre>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>The current implementation can only format features — parsing is not yet implemented.</li>
 *   <li>{@code FeatureFormat}, like most {@code java.text.Format} subclasses, is not thread-safe.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.5
 */
public class FeatureFormat extends TabularFormat<Object> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5792086817264884947L;

    /**
     * The separator to use in comma-separated lists.
     */
    private static final String SEPARATOR = ", ";

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
     * <p>This is defined as a static final variable for now because its value is approximate:
     * it is a number of characters instead of a number of code points, and that length may be
     * exceeded by a few characters if the overflow happen while appending the list separator.</p>
     */
    private static final int MAXIMAL_VALUE_LENGTH = 40;

    /**
     * The bit patterns of the last {@link Float#NaN} value for which {@link MathFunctions#toNanOrdinal(float)} could
     * not get the ordinal value. We use this information for avoiding flooding the logger with the same message.
     */
    private transient int illegalNaN;

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
     * since it is the only common parent to {@link Feature} and {@link FeatureType}.
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
     * However, columns <em>not</em> included in the set are guaranteed to be omitted.
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
         * This is the character sequence returned by {@link PropertyType#getDesignation()}.
         * This column is omitted if no property has a designation.
         */
        DESIGNATION(Vocabulary.Keys.Designation),

        /**
         * Name of the property.
         * This is the character sequence returned by {@link PropertyType#getName()}.
         */
        NAME(Vocabulary.Keys.Name),

        /**
         * Type of property values. This is the type returned by {@link AttributeType#getValueClass()} or
         * {@link FeatureAssociationRole#getValueType()}.
         */
        TYPE(Vocabulary.Keys.Type),

        /**
         * Cardinality (for attributes) or multiplicity (for attribute types).
         * The cardinality is the actual number of attribute values.
         * The multiplicity is the minimum and maximum occurrences of attribute values.
         * The multiplicity is made from the numbers returned by {@link AttributeType#getMinimumOccurs()}
         * and {@link AttributeType#getMaximumOccurs()}.
         */
        CARDINALITY(Vocabulary.Keys.Cardinality),

        /**
         * Property value (for properties) or default value (for property types).
         * This is the value returned by {@link Attribute#getValue()}, {@link FeatureAssociation#getValue()}
         * or {@link AttributeType#getDefaultValue()}.
         */
        VALUE(Vocabulary.Keys.Value),

        /**
         * Other attributes that describes the attribute.
         * This is made from the map returned by {@link Attribute#characteristics()}.
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
     *   <li>{@link Feature}</li>
     *   <li>{@link FeatureType}</li>
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
        final FeatureType featureType;
        final Feature feature;
        if (object instanceof Feature) {
            feature     = (Feature) object;
            featureType = feature.getType();
        } else if (object instanceof FeatureType) {
            featureType = (FeatureType) object;
            feature     = null;
        } else {
            throw new IllegalArgumentException(Errors.forLocale(displayLocale)
                    .getString(Errors.Keys.UnsupportedType_1, object.getClass()));
        }
        /*
         * Computes the columns to show. We start with the set of columns specified by setAllowedColumns(Set),
         * then we check if some of those columns are empty. For example, in many cases there are no attributes
         * with characteritic, in which case we will ommit the whole "characteristics" column. We perform such
         * check only for optional information, not for mandatory information like property names.
         */
        final EnumSet<Column> visibleColumns = columns.clone();
        {
            boolean hasDesignation     = false;
            boolean hasCharacteristics = false;
            boolean hasDeprecatedTypes = false;
            for (final PropertyType propertyType : featureType.getProperties(true)) {
                if (!hasDesignation) {
                    hasDesignation = propertyType.getDesignation().isPresent();
                }
                if (!hasCharacteristics && propertyType instanceof AttributeType<?>) {
                    hasCharacteristics = !((AttributeType<?>) propertyType).characteristics().isEmpty();
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
                separator = SEPARATOR;
            }
            final InternationalString definition = featureType.getDefinition();
            if (definition != null) {
                final String text = Strings.trimOrNull(definition.toString(displayLocale));
                if (text != null) {
                    toAppendTo.append(getLineSeparator()).append(text);
                }
            }
        }
        toAppendTo.append(getLineSeparator());
        /*
         * Create a table and format the header. Columns will be shown in Column enumeration order.
         */
        final Vocabulary resources = Vocabulary.forLocale(displayLocale);
        final var table = new TableAppender(toAppendTo, columnSeparator);
        table.setMultiLinesCells(true);
        table.nextLine('─');
        boolean isFirstColumn = true;
        for (final Column column : visibleColumns) {
            short key = column.resourceKey;
            if (feature == null) {
                if (key == Vocabulary.Keys.Cardinality) key = Vocabulary.Keys.Multiplicity;
                if (key == Vocabulary.Keys.Value)       key = Vocabulary.Keys.DefaultValue;
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
        final var buffer  = new StringBuffer();
        final var dummyFP = new FieldPosition(-1);
        final var remarks = new ArrayList<String>();
        for (final PropertyType propertyType : featureType.getProperties(true)) {
            Object value = null;
            int cardinality = -1;
            if (feature != null) {
                if (!(propertyType instanceof AttributeType<?>) &&
                    !(propertyType instanceof FeatureAssociationRole) &&
                    !DefaultFeatureType.isParameterlessOperation(propertyType))
                {
                    continue;
                }
                value = feature.getPropertyValue(propertyType.getName().toString());
                if (value == null) {
                    if (propertyType instanceof AttributeType<?>
                            && ((AttributeType<?>) propertyType).getMinimumOccurs() == 0
                            && ((AttributeType<?>) propertyType).characteristics().isEmpty())
                    {
                        continue;                           // If optional, no value and no characteristics, skip the full row.
                    }
                    if (propertyType instanceof FeatureAssociationRole
                            && ((FeatureAssociationRole) propertyType).getMinimumOccurs() == 0)
                    {
                        continue;                           // If optional and no value, skip the full row.
                    }
                    cardinality = 0;
                } else if (value instanceof Collection<?>) {
                    cardinality = ((Collection<?>) value).size();
                } else {
                    cardinality = 1;
                }
            } else if (propertyType instanceof AttributeType<?>) {
                value = ((AttributeType<?>) propertyType).getDefaultValue();
            } else if (propertyType instanceof Operation) {
                buffer.append(" = ");
                try {
                    if (propertyType instanceof AbstractOperation) {
                        ((AbstractOperation) propertyType).formatResultFormula(buffer);
                    } else {
                        AbstractOperation.defaultFormula(((Operation) propertyType).getParameters(), buffer);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);      // Should never happen since we write in a StringBuffer.
                }
                value = CharSequences.trimWhitespaces(buffer).toString();
                buffer.setLength(0);
            }
            final String   valueType;                       // The value to write in the type column.
            final Class<?> valueClass;                      // AttributeType.getValueClass() if applicable.
            final int minimumOccurs, maximumOccurs;         // Negative values mean no cardinality.
            final IdentifiedType resultType;                // Result of operation if applicable.
            if (propertyType instanceof Operation) {
                resultType = ((Operation) propertyType).getResult();                // May be null
            } else {
                resultType = propertyType;
            }
            if (resultType instanceof AttributeType<?>) {
                final AttributeType<?> pt = (AttributeType<?>) resultType;
                minimumOccurs = pt.getMinimumOccurs();
                maximumOccurs = pt.getMaximumOccurs();
                valueClass    = pt.getValueClass();
                valueType     = getFormat(Class.class).format(valueClass, buffer, dummyFP).toString();
                buffer.setLength(0);
            } else if (resultType instanceof FeatureAssociationRole) {
                final FeatureAssociationRole pt = (FeatureAssociationRole) resultType;
                minimumOccurs = pt.getMinimumOccurs();
                maximumOccurs = pt.getMaximumOccurs();
                valueType     = toString(DefaultAssociationRole.getValueTypeName(pt));
                valueClass    = Feature.class;
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
                    /*
                     * Human-readable name of the property. May contains any characters (spaces, ideographs, etc).
                     * In many cases, this information is not provided and the whole column is skipped.
                     */
                    case DESIGNATION: {
                        propertyType.getDesignation().ifPresent((d) -> {
                            table.append(d.toString(displayLocale));
                        });
                        break;
                    }
                    /*
                     * Machine-readable name of the property (identifier). This information is mandatory.
                     * This name is usually shorter than the designation and should contain only valid
                     * Unicode identifier characters (e.g. no spaces).
                     */
                    case NAME: {
                        table.append(toString(propertyType.getName()));
                        break;
                    }
                    /*
                     * The base class or interface for all values in properties of the same type.
                     * This is typically String, Number, Integer, Geometry or URL.
                     */
                    case TYPE: {
                        table.append(valueType);
                        break;
                    }
                    /*
                     * Minimum and maximum number of occurrences allowed for this property.
                     * If we are formatting a Feature instead of a FeatureType, then the
                     * actual number of values is also formatted. Example: 42 ∈ [0 … ∞]
                     */
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
                    /*
                     * If formatting a FeatureType, the default value. If formatting a Feature, the actual value.
                     * A java.text.Format instance dedicated to the value class is used if possible. In addition
                     * to types for which a java.text.Format may be available, we also have to check for other
                     * special cases. If there is more than one value, they are formatted as a coma-separated list.
                     */
                    case VALUE: {
                        table.setCellAlignment(TableAppender.ALIGN_LEFT);
                        final Format format = getFormat(valueClass);                            // Null if valueClass is null.
                        final Iterator<?> it = Containers.toCollection(value).iterator();
                        String separator = "";
                        int length = 0;
                        while (it.hasNext()) {
                            value = it.next();
                            if (value != null) {
                                if (propertyType instanceof FeatureAssociationRole) {
                                    final String p = DefaultAssociationRole.getTitleProperty((FeatureAssociationRole) propertyType);
                                    if (p != null) {
                                        value = ((Feature) value).getPropertyValue(p);
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
                                            try {
                                                final int n = MathFunctions.toNanOrdinal(f);
                                                if (n > 0) t.append(" #").append(n);
                                            } catch (IllegalArgumentException e) {
                                                // May happen if the NaN is a signaling NaN instead of a quiet NaN.
                                                final int bits = Float.floatToRawIntBits(f);
                                                if (bits != illegalNaN) {
                                                    illegalNaN = bits;
                                                    Logging.recoverableException(AbstractIdentifiedType.LOGGER, FeatureFormat.class, "format", e);
                                                }
                                            }
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
                                separator = SEPARATOR;
                                length += SEPARATOR.length();
                            }
                        }
                        break;
                    }
                    /*
                     * Characteristics are optional information attached to some values. For example if a property
                     * value is a temperature measurement, a characteritic of that value may be the unit of measure.
                     * Characteristics are handled as "attributes of attributes".
                     */
                    case CHARACTERISTICS: {
                        if (propertyType instanceof AttributeType<?>) {
                            int length = 0;
                            String separator = "";
format:                     for (final AttributeType<?> ct : ((AttributeType<?>) propertyType).characteristics().values()) {
                                /*
                                 * Format the characteristic name. We will append the value(s) later.
                                 * We keep trace of the text length in order to stop formatting if the
                                 * text become too long.
                                 */
                                final GenericName cn = ct.getName();
                                final String cs = toString(cn);
                                table.append(separator).append(cs);
                                length += separator.length() + cs.length();
                                Collection<?> cv = Containers.singletonOrEmpty(ct.getDefaultValue());
                                if (feature != null) {
                                    /*
                                     * Usually, the property `cp` below is null because all features use the same
                                     * characteristic value (for example the same unit of measurement),  which is
                                     * given by the default value `cv`.  Nevertheless we have to check if current
                                     * feature overrides this characteristic.
                                     */
                                    final Property cp = feature.getProperty(propertyType.getName().toString());
                                    if (cp instanceof Attribute<?>) {            // Should always be true, but we are paranoiac.
                                        Attribute<?> ca = ((Attribute<?>) cp).characteristics().get(cn.toString());
                                        if (ca != null) cv = ca.getValues();
                                    }
                                }
                                /*
                                 * Now format the value, separated from the name with " = ". Example: unit = m/s
                                 * If the value accepts multi-occurrences, we will format the value between {…}.
                                 * We use {…} because we may have more than one characteristic in the same cell,
                                 * so we need a way to distinguish multi-values from multi-characteristics.
                                 */
                                final boolean multi = ct.getMaximumOccurs() > 1;
                                String sep = multi ? " = {" : " = ";
                                for (Object c : cv) {
                                    length = formatValue(c, table.append(sep), length += sep.length());
                                    if (length < 0) break format;   // Value is too long, abandon remaining iterations.
                                    sep = SEPARATOR;
                                }
                                separator = SEPARATOR;
                                if (multi && sep == SEPARATOR) {
                                    table.append('}');
                                }
                            }
                        }
                        break;
                    }
                    case REMARKS: {
                        if (org.apache.sis.feature.Field.isDeprecated(propertyType)) {
                            table.append(resources.getString(Vocabulary.Keys.Deprecated));
                            final InternationalString r = ((Deprecable) propertyType).getRemarks().orElse(null);
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
        String text;
        if (value instanceof InternationalString) {
            text = ((InternationalString) value).toString(displayLocale);
        } else if (value instanceof GenericName) {
            text = toString((GenericName) value);
        } else if (value instanceof IdentifiedType) {
            text = toString(((IdentifiedType) value).getName());
        } else if (value instanceof IdentifiedObject) {
            text = IdentifiedObjects.getIdentifierOrName((IdentifiedObject) value);
        } else {
            text = Geometries.wrap(value).map(GeometryWrapper::toString).orElseGet(value::toString);
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
        throw new ParseException(Errors.forLocale(displayLocale)
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
