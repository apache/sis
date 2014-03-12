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
import org.apache.sis.measure.Range;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Numerics;

import static org.apache.sis.util.ArgumentChecks.*;

// Related to JDK7
import org.apache.sis.internal.jdk7.Objects;


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
 * @param <T> The value type.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
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
     * The minimum and maximum attribute value with their unit of measurement, or {@code null} if none.
     * If this field is non-null, then <code>valueDomain.{@linkplain Range#getElementType() getElementType()}</code>
     * shall be one of the following:
     *
     * <ul>
     *   <li>If {@link #valueClass} is not an array, then the range element type shall be the same class.</li>
     *   <li>If {@code valueClass} is an array, then the range element type shall be the wrapper of
     *       <code>valueClass.{@linkplain Class#getComponentType() getComponentType()}</code>.</li>
     * </ul>
     *
     * @see #getValueDomain()
     */
    private final Range<?> valueDomain;

    /**
     * The default value for the attribute, or {@code null} if none.
     *
     * @see #getDefaultValue()
     */
    private final T defaultValue;

    /**
     * The minimum/maximum number of occurrences of the property within its containing entity.
     *
     * @see #getMinimumOccurs()
     * @see #getMaximumOccurs()
     */
    private final int minimumOccurs, maximumOccurs;

    /**
     * Constructs an attribute type from the given properties. The properties map is given unchanged to
     * the {@linkplain AbstractIdentifiedType#AbstractIdentifiedType(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
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
     * {@section Domain of attribute values}
     * If {@code valueDomain} argument is non-null, then it shall comply to the following conditions:
     *
     * <ul>
     *   <li>The range shall be non-{@linkplain Range#isEmpty() empty}.</li>
     *   <li><code>valueDomain.{@linkplain Range#getElementType() getElementType()}</code> shall be equals
     *       to one of the following:
     *     <ul>
     *       <li>to {@code valueClass} if the later is not an array,</li>
     *       <li>or to <code>{@linkplain Numbers#primitiveToWrapper(Class)
     *           primitiveToWrapper}(valueClass.{@linkplain Class#getComponentType() getComponentType()})</code>
     *           if {@code valueClass} is an array.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param properties    The name and other properties to be given to this attribute type.
     * @param valueClass    The type of attribute values.
     * @param valueDomain   The minimum value, maximum value and unit of measurement, or {@code null} if none.
     * @param defaultValue  The default value for the attribute, or {@code null} if none.
     * @param minimumOccurs The minimum number of occurrences of the property within its containing entity.
     * @param maximumOccurs The maximum number of occurrences of the property within its containing entity,
     *                      or {@link Integer#MAX_VALUE} if none.
     */
    public DefaultAttributeType(final Map<String,?> properties, final Class<T> valueClass, final Range<?> valueDomain,
            final T defaultValue, final int minimumOccurs, final int maximumOccurs)
    {
        super(properties);
        ensureNonNull("valueClass",   valueClass);
        ensureCanCast("defaultValue", valueClass, defaultValue);
        if (minimumOccurs < 0 || minimumOccurs > maximumOccurs) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalRange_2, minimumOccurs, maximumOccurs));
        }
        if (valueDomain != null) {
            Class<?> componentType = valueClass.getComponentType();
            if (componentType != null) {
                componentType = Numbers.primitiveToWrapper(componentType);
            } else {
                componentType = valueClass;
            }
            final Class<?> elementType = valueDomain.getElementType();
            if (elementType != componentType) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentClass_2,
                        "valueDomain", "Range<" + Classes.getShortName(elementType) + '>'));
            }
            if (valueDomain.isEmpty()) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalRange_2,
                        valueDomain.getMinValue(), valueDomain.getMaxValue()));
            }
        }
        this.valueClass    = valueClass;
        this.valueDomain   = valueDomain;
        this.defaultValue  = Numerics.cached(defaultValue);
        this.minimumOccurs = minimumOccurs;
        this.maximumOccurs = maximumOccurs;
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
     * Returns the domain of values with their unit of measurement (if any), or {@code null} if none.
     *
     * <div class="note"><b>API note:</b> If this method returns a non-null value, then its type is either exactly
     * {@code Range<T>}, or {@code Range<E>} where {@code <E>} is the {@linkplain Class#getComponentType() component
     * type} of {@code <T>} (using wrapper classes for primitive types).</div>
     *
     * @return The domain of values, or {@code null}.
     */
    /* Implementation note: this method is final because the constructor performs various checks on range validity,
     * and we can not express those rules in the method signature. If the user was allowed to override this method,
     * there is no way we can ensure that the range element type still valid.
     */
    public final Range<?> getValueDomain() {
        return valueDomain;
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

    /**
     * Returns the minimum number of occurrences of the property within its containing entity.
     * This value is always an integer greater than or equal to zero.
     *
     * @return The minimum number of occurrences of the property within its containing entity.
     */
    public int getMinimumOccurs() {
        return minimumOccurs;
    }

    /**
     * The maximum number of occurrences of the property within its containing entity.
     * A value of {@link Integer#MAX_VALUE} means that the maximum number of occurrences is unbounded.
     *
     * @return The maximum number of occurrences of the property within its containing entity,
     *         or {@link Integer#MAX_VALUE} if none.
     */
    public int getMaximumOccurs() {
        return maximumOccurs;
    }

    /**
     * Returns a hash code value for this attribute type.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode() + valueClass.hashCode() + Objects.hashCode(valueDomain) +
               31*(Objects.hashCode(defaultValue) + 31*(minimumOccurs + 31*maximumOccurs));
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
            return valueClass    == that.valueClass    &&
                   minimumOccurs == that.minimumOccurs &&
                   maximumOccurs == that.maximumOccurs &&
                   Objects.equals(valueDomain,  that.valueDomain) &&
                   Objects.equals(defaultValue, that.defaultValue);
        }
        return false;
    }
}
