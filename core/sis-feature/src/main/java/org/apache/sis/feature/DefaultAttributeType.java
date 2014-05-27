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
import org.opengis.util.InternationalString;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.internal.util.Numerics;

import static org.apache.sis.util.ArgumentChecks.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;
import org.opengis.feature.AttributeType;


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
 * {@section Value type}
 * Attributes can be used for both spatial and non-spatial properties.
 * Some examples are:
 *
 * <table class="sis">
 *   <caption>Attribute value type examples</caption>
 *   <tr><th>Attribute name</th>      <th>Value type</th></tr>
 *   <tr><td>Building shape</td>      <td>{@link org.opengis.geometry.Geometry}</td></tr>
 *   <tr><td>Building owner</td>      <td>{@link org.opengis.metadata.citation.ResponsibleParty}</td></tr>
 *   <tr><td>Horizontal accuracy</td> <td>{@link org.opengis.metadata.quality.PositionalAccuracy}</td></tr>
 * </table>
 *
 * {@section Immutability and thread safety}
 * Instances of this class are immutable if all properties ({@link GenericName} and {@link InternationalString}
 * instances) and all arguments (e.g. {@code defaultValue}) given to the constructor are also immutable.
 * Such immutable instances can be shared by many objects and passed between threads without synchronization.
 *
 * <p>In particular, the {@link #getDefaultValue()} method does <strong>not</strong> clone the returned value.
 * This means that the same {@code defaultValue} instance may be shared by many {@link AbstractAttribute} instances.
 * Consequently the default value should be immutable for avoiding unexpected behavior.</p>
 *
 * @param <V> The type of attribute values.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see AbstractAttribute
 */
public class DefaultAttributeType<V> extends FieldType implements AttributeType<V> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8215784957556648553L;

    /**
     * The class that describe the type of attribute values.
     *
     * @see #getValueClass()
     */
    private final Class<V> valueClass;

    /**
     * The default value for the attribute, or {@code null} if none.
     *
     * @see #getDefaultValue()
     */
    private final V defaultValue;

    /**
     * Constructs an attribute type from the given properties. The identification map is given unchanged to
     * the {@linkplain AbstractIdentifiedType#AbstractIdentifiedType(Map) super-class constructor}.
     * The following table is a reminder of main (not all) recognized map entries:
     *
     * <table class="sis">
     *   <caption>Recognized map entries (non exhaustive list)</caption>
     *   <tr>
     *     <th>Map key</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#NAME_KEY}</td>
     *     <td>{@link GenericName} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DEFINITION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDefinition()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DESIGNATION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDesignation()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DESCRIPTION_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getDescription()}</td>
     *   </tr>
     * </table>
     *
     * @param identification The name and other information to be given to this attribute type.
     * @param valueClass     The type of attribute values.
     * @param minimumOccurs  The minimum number of occurrences of the attribute within its containing entity.
     * @param maximumOccurs  The maximum number of occurrences of the attribute within its containing entity,
     *                       or {@link Integer#MAX_VALUE} if there is no restriction.
     * @param defaultValue   The default value for the attribute, or {@code null} if none.
     */
    public DefaultAttributeType(final Map<String,?> identification, final Class<V> valueClass,
            final int minimumOccurs, final int maximumOccurs, final V defaultValue)
    {
        super(identification, minimumOccurs, maximumOccurs);
        ensureNonNull("valueClass",   valueClass);
        ensureCanCast("defaultValue", valueClass, defaultValue);
        this.valueClass   = valueClass;
        this.defaultValue = Numerics.cached(defaultValue);
    }

    /**
     * Returns the type of attribute values.
     *
     * @return The type of attribute values.
     */
    @Override
    public final Class<V> getValueClass() {
        return valueClass;
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
     * Returns the minimum number of attribute values.
     * The returned value is greater than or equal to zero.
     *
     * <p>To be valid, an {@code Attribute} instance of this {@code AttributeType} shall have at least
     * this minimum number of elements in its {@link AbstractAttribute#getValues() collection of values}.</p>
     *
     * @return The minimum number of attribute values.
     */
    @Override
    public final int getMinimumOccurs() {
        return super.getMinimumOccurs();
    }

    /**
     * Returns the maximum number of attribute values.
     * The returned value is greater than or equal to the {@link #getMinimumOccurs()} value.
     * If there is no maximum, then this method returns {@link Integer#MAX_VALUE}.
     *
     * <p>To be valid, an {@code Attribute} instance of this {@code AttributeType} shall have no more than
     * this maximum number of elements in its {@link AbstractAttribute#getValues() collection of values}.</p>
     *
     * @return The maximum number of attribute values, or {@link Integer#MAX_VALUE} if none.
     */
    @Override
    public final int getMaximumOccurs() {
        return super.getMaximumOccurs();
    }

    /**
     * Returns the default value for the attribute.
     * This value is used when an attribute is created and no value for it is specified.
     *
     * @return The default value for the attribute, or {@code null} if none.
     */
    @Override
    public V getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns a hash code value for this attribute type.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode() + valueClass.hashCode() + Objects.hashCode(defaultValue);
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
                   Objects.equals(defaultValue, that.defaultValue);
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
        return toString("AttributeType", this, Classes.getShortName(valueClass)).toString();
    }
}
