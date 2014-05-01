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

import java.util.Map;
import org.opengis.util.GenericName;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.measure.NumberRange;

import static org.apache.sis.util.ArgumentChecks.*;

// Related to JDK7
import java.util.Objects;


/**
 * Definition of an attribute in a feature type.
 * The name of attribute type is mandatory. The name {@linkplain org.apache.sis.util.iso.AbstractName#scope() scope}
 * is typically the name of the {@linkplain DefaultFeatureType feature type} containing this attribute, but this is
 * not mandatory. The scope could also be defined by the ontology for example.
 *
 * <div class="note"><b>Note:</b>
 * Compared to the Java language, {@code AttributeType} is equivalent to {@link java.lang.reflect.Field}
 * while {@code FeatureType} is equivalent to {@link Class}.</div>
 *
 * <div class="warning"><b>Warning:</b>
 * This class is expected to implement a GeoAPI {@code AttributeType} interface in a future version.
 * When such interface will be available, most references to {@code DefaultAttributeType} in the API
 * will be replaced by references to the {@code AttributeType} interface.</div>
 *
 * @param <T> The type of attribute values.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public class DefaultAttributeType<T> extends AbstractIdentifiedType {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8215784957556648553L;

    /**
     * The class that describe the type of attribute values.
     *
     * @see #getValueClass()
     */
    private final Class<T> valueClass;

    /**
     * The default value for the attribute, or {@code null} if none.
     *
     * @see #getDefaultValue()
     */
    private final T defaultValue;

    /**
     * The minimum/maximum number of occurrences of the property within its containing entity.
     */
    private final NumberRange<Integer> cardinality;

    /**
     * Constructs an attribute type from the given properties. The properties map is given unchanged to
     * the {@linkplain AbstractIdentifiedType#AbstractIdentifiedType(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#NAME_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DEFINITION_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getDefinition()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DESIGNATION_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getDesignation()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DESCRIPTION_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getDescription()}</td>
     *   </tr>
     * </table>
     *
     * @param properties    The name and other properties to be given to this attribute type.
     * @param valueClass    The type of attribute values.
     * @param defaultValue  The default value for the attribute, or {@code null} if none.
     * @param cardinality   The minimum and maximum number of occurrences of the property within its containing entity,
     *                      or {@code null} if there is no restriction.
     */
    public DefaultAttributeType(final Map<String,?> properties, final Class<T> valueClass, final T defaultValue,
            NumberRange<Integer> cardinality)
    {
        super(properties);
        ensureNonNull("valueClass",   valueClass);
        ensureCanCast("defaultValue", valueClass, defaultValue);
        if (cardinality == null) {
            cardinality = NumberRange.createLeftBounded(0, true);
        } else {
            final Integer minValue = cardinality.getMinValue();
            if (minValue == null || minValue < 0) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.IllegalArgumentValue_2, "cardinality", cardinality));
            }
        }
        this.valueClass   = valueClass;
        this.defaultValue = Numerics.cached(defaultValue);
        this.cardinality  = cardinality;
    }

    /**
     * Returns the type of attribute values.
     *
     * @return The type of attribute values.
     */
    public final Class<T> getValueClass() {
        return valueClass;
    }

    /**
     * Returns the default value for the attribute.
     * This value is used when an attribute is created and no value for it is specified.
     *
     * @return The default value for the attribute, or {@code null} if none.
     */
    public T getDefaultValue() {
        return defaultValue;
    }

    /*
     * ISO 19109 properties omitted for now:
     *
     *   - valueDomain : CharacterString
     *
     * Rational: a CharacterString is hardly programmatically usable. A Range would be better but too specific.
     * We could follow the GeoAPI path and define a "restrictions : Filter" property. That would be more generic,
     * but we are probably better to wait for Filter to be implemented in SIS.
     *
     * Reference: https://issues.apache.org/jira/browse/SIS-175
     */

    /**
     * Returns the minimum and maximum number of occurrences of the property within its containing entity.
     * The bounds are always integer values greater than or equal to zero. The upper bounds may be {@code null}
     * if there is no maximum number of occurrences.
     *
     * @return The minimum and maximum number of occurrences of the property within its containing entity.
     */
    public NumberRange<Integer> getCardinality() {
        return cardinality;
    }

    /**
     * Returns a hash code value for this attribute type.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode() + valueClass.hashCode() + Objects.hashCode(defaultValue) + 31*cardinality.hashCode();
    }

    /**
     * Compares this attribute type with the given object for equality.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (super.equals(obj)) {
            final DefaultAttributeType<?> that = (DefaultAttributeType<?>) obj;
            return valueClass == that.valueClass &&
                   Objects.equals(defaultValue, that.defaultValue) &&
                   cardinality.equals(that.cardinality);
        }
        return false;
    }

    /**
     * Returns a string representation of this attribute type.
     * The returned string is for debugging purpose and may change in any future SIS version.
     *
     * @return A string representation of this attribute type for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        return toString("AttributeType").toString();
    }

    /**
     * Implementation of {@link #toString()} to be shared by {@link DefaultAttribute#toString()}.
     */
    final StringBuilder toString(final String typeName) {
        final StringBuilder buffer = new StringBuilder(40).append(typeName).append('[');
        final GenericName name = super.getName();
        if (name != null) {
            buffer.append('“');
        }
        buffer.append(name);
        if (name != null) {
            buffer.append("” : ");
        }
        return buffer.append(Classes.getShortName(valueClass)).append(']');
    }
}
