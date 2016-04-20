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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
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
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.referencing.IdentifiedObjects;


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
 * <div class="warning"><b>Limitation:</b>
 * Current implementation supports only formatting, not parsing.
 * </div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
public class FeatureFormat extends TabularFormat<Object> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8866440357566645070L;

    /**
     * An instance created when first needed and potentially shared.
     */
    private static final AtomicReference<FeatureFormat> INSTANCE = new AtomicReference<FeatureFormat>();

    /**
     * The locale for international strings.
     */
    private final Locale displayLocale;

    /**
     * Creates a new formatter for the default locale and timezone.
     */
    public FeatureFormat() {
        super(Locale.getDefault(), TimeZone.getDefault());
        displayLocale = super.getLocale(); // This is different on the JDK7 branch.
        columnSeparator = " │ ";
    }

    /**
     * Creates a new formatter for the given locale and timezone.
     *
     * @param locale   The locale, or {@code null} for {@code Locale.ROOT}.
     * @param timezone The timezone, or {@code null} for UTC.
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
     * @throws IOException If an error occurred while writing to the given appendable.
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
         * Check if at least one attribute has at least one characteritic. In many cases there is none.
         * In none we will ommit the "characteristics" column, which is the last column.
         */
        boolean hasCharacteristics = false;
        for (final AbstractIdentifiedType propertyType : featureType.getProperties(true)) {
            if (propertyType instanceof DefaultAttributeType<?>) {
                if (!((DefaultAttributeType<?>) propertyType).characteristics().isEmpty()) {
                    hasCharacteristics = true;
                    break;
                }
            }
        }
        /*
         * Format the column header.
         */
        toAppendTo.append(toString(featureType.getName())).append(getLineSeparator());
        final Vocabulary resources = Vocabulary.getResources(displayLocale);
        final TableAppender table = new TableAppender(toAppendTo, columnSeparator);
        table.setMultiLinesCells(true);
        table.nextLine('─');
header: for (int i=0; ; i++) {
            final short key;
            switch (i) {
                case 0:                     key = Vocabulary.Keys.Name; break;
                case 1:  nextColumn(table); key = Vocabulary.Keys.Type; break;
                case 2:  nextColumn(table); key = Vocabulary.Keys.Cardinality; break;
                case 3:  nextColumn(table); key = (feature != null) ? Vocabulary.Keys.Value : Vocabulary.Keys.DefaultValue; break;
                case 4: {
                    if (hasCharacteristics) {
                        nextColumn(table);
                        key = Vocabulary.Keys.Characteristics;
                        break;
                    } else {
                        break header;
                    }
                }
                default: break header;
            }
            table.append(resources.getString(key));
        }
        table.nextLine();
        table.nextLine('─');
        /*
         * Done writing the header. Now write all property rows.
         * Rows without value will be skipped only if optional.
         */
        final StringBuffer  buffer  = new StringBuffer();
        final FieldPosition dummyFP = new FieldPosition(-1);
        for (final AbstractIdentifiedType propertyType : featureType.getProperties(true)) {
            Object value;
            if (feature != null) {
                value = feature.getPropertyValue(propertyType.getName().toString());
                if (value == null) {
                    if (propertyType instanceof FieldType && ((FieldType) propertyType).getMinimumOccurs() == 0) {
                        continue;                                       // If no value, skip the full row.
                    }
                }
            } else if (propertyType instanceof DefaultAttributeType<?>) {
                value = ((DefaultAttributeType<?>) propertyType).getDefaultValue();
            } else {
                value = null;
            }
            /*
             * Column 0 - Name.
             */
            table.append(toString(propertyType.getName()));
            nextColumn(table);
            /*
             * Column 1 and 2 - Type and cardinality.
             */
            final String   valueType;
            final Class<?> valueClass;
            final int minimumOccurs, maximumOccurs;
            if (propertyType instanceof DefaultAttributeType<?>) {
                final DefaultAttributeType<?> pt = (DefaultAttributeType<?>) propertyType;
                minimumOccurs = pt.getMinimumOccurs();
                maximumOccurs = pt.getMaximumOccurs();
                valueClass    = pt.getValueClass();
                valueType     = getFormat(Class.class).format(valueClass, buffer, dummyFP).toString();
                buffer.setLength(0);
            } else if (propertyType instanceof DefaultAssociationRole) {
                final DefaultAssociationRole pt = (DefaultAssociationRole) propertyType;
                minimumOccurs = pt.getMinimumOccurs();
                maximumOccurs = pt.getMaximumOccurs();
                valueType     = toString(DefaultAssociationRole.getValueTypeName(pt));
                valueClass    = AbstractFeature.class;
            } else if (propertyType instanceof AbstractOperation) {
                final AbstractIdentifiedType resultType = ((AbstractOperation) propertyType).getResult();
                valueType   = toString(resultType.getName());
                valueClass  = null;
                minimumOccurs = -1;
                maximumOccurs = -1;
            } else {
                valueType   = "";
                valueClass  = null;
                minimumOccurs = -1;
                maximumOccurs = -1;
            }
            table.append(valueType);
            nextColumn(table);
            if (maximumOccurs >= 0) {
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
            nextColumn(table);
            /*
             * Column 3 - Value or default value.
             */
            if (value != null) {
                final boolean isInstance = valueClass != null && valueClass.isInstance(value);
                final Format format = isInstance ? getFormat(valueClass) : null;
                final Iterator<?> it = (!isInstance && (value instanceof Collection<?>)
                        ? (Collection<?>) value : Collections.singleton(value)).iterator();
                String separator = "";
                while (it.hasNext()) {
                    value = it.next();
                    if (value != null) {
                        if (format != null) {
                            value = format.format(value, buffer, dummyFP);
                        } else if (value instanceof AbstractFeature && propertyType instanceof DefaultAssociationRole) {
                            final String p = DefaultAssociationRole.getTitleProperty((DefaultAssociationRole) propertyType);
                            if (p != null) {
                                value = ((AbstractFeature) value).getPropertyValue(p);
                                if (value == null) continue;
                            }
                        }
                        table.append(separator).append(formatValue(value));
                        buffer.setLength(0);
                        separator = ", ";
                    }
                }
            }
            /*
             * Column 4 - Characteristics.
             */
            if (hasCharacteristics) {
                nextColumn(table);
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
                            table.append(" = ").append(formatValue(c));
                        }
                        separator = ", ";
                    }
                }
            }
            table.nextLine();
        }
        table.nextLine('─');
        table.flush();
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
     * Formats the given attribute value.
     */
    private String formatValue(final Object value) {
        if (value instanceof InternationalString) {
            return ((InternationalString) value).toString(displayLocale);
        } else if (value instanceof GenericName) {
            return toString((GenericName) value);
        } else if (value instanceof AbstractIdentifiedType) {
            return toString(((AbstractIdentifiedType) value).getName());
        } else if (value instanceof IdentifiedObject) {
            return IdentifiedObjects.getIdentifierOrName((IdentifiedObject) value);
        }
        return value.toString();
    }

    /**
     * Formats the given object using a shared instance of {@code ParameterFormat}.
     * This is used for {@link DefaultParameterDescriptorGroup#toString()} implementation.
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
     * @return Currently never return.
     * @throws ParseException Currently always thrown.
     */
    @Override
    public Object parse(final CharSequence text, final ParsePosition pos) throws ParseException {
        throw new ParseException(Errors.getResources(displayLocale)
                .getString(Errors.Keys.UnsupportedOperation_1, "parse"), 0);
    }
}
