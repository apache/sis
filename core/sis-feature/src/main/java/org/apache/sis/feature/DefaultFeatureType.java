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
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.UnmodifiableArrayList;


/**
 * Abstraction of a real-world phenomena. A {@code FeatureType} instance describes the class of all
 * {@link DefaultFeature} instances of that type.
 *
 * <div class="note"><b>Note:</b>
 * Compared to the Java language, {@code FeatureType} is equivalent to {@link Class} while
 * {@code Feature} instances are equivalent to {@link Object} instances of that class.</div>
 *
 * A feature type can inherit the properties of one or more other feature types.
 * Each feature type can provide descriptions for the following properties:
 *
 * <ul>
 *   <li>{@linkplain DefaultAttributeType   Attributes}</li>
 *   <li>{@linkplain DefaultAssociationRole Associations to other feature types}</li>
 *   <li>{@linkplain DefaultOperationType   Operations}</li>
 * </ul>
 *
 * The description of all those properties are collectively called {@linkplain #characteristics() characteristics}.
 *
 * <div class="warning"><b>Warning:</b>
 * This class is expected to implement a GeoAPI {@code FeatureType} interface in a future version.
 * When such interface will be available, most references to {@code DefaultFeatureType} in the API
 * will be replaced by references to the {@code FeatureType} interface.</div>
 *
 * {@section Immutability and thread safety}
 * Instances of this class are immutable if all properties ({@link GenericName} and {@link InternationalString}
 * instances) and all arguments ({@link AttributeType} instances) given to the constructor are also immutable.
 * Such immutable instances can be shared by many objects and passed between threads without synchronization.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public class DefaultFeatureType extends AbstractIdentifiedType {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4357370600723922312L;

    /**
     * If {@code true}, the feature type acts as an abstract super-type.
     *
     * @see #isAbstract()
     */
    private final boolean isAbstract;

    /**
     * The parents of this feature type, or an empty set if none.
     */
    private final Set<DefaultFeatureType> superTypes;

    /**
     * Any feature operation, any feature attribute type and any feature association role
     * that carries characteristics of a feature type.
     */
    private final List<DefaultAttributeType<?>> characteristics;

    /**
     * A lookup table for fetching properties by name.
     * This map shall not be modified after construction.
     *
     * @see #getProperty(String)
     */
    private final transient Map<String, DefaultAttributeType<?>> byName;

    /**
     * Constructs a feature type from the given properties. The properties map is given unchanged to
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
     * <div class="warning"><b>Warning:</b> In a future SIS version, the type of array elements may be
     * changed to {@code org.opengis.feature.FeatureType} {@code org.opengis.feature.PropertyType}.
     * This change is pending GeoAPI revision.</div>
     *
     * @param properties The name and other properties to be given to this feature type.
     * @param isAbstract If {@code true}, the feature type acts as an abstract super-type.
     * @param superTypes The parents of this feature type, or {@code null} or empty if none.
     * @param characteristics Any feature operation, any feature attribute type and any feature
     *        association role that carries characteristics of a feature type.
     */
    public DefaultFeatureType(final Map<String,?> properties, final boolean isAbstract,
            final DefaultFeatureType[] superTypes, final DefaultAttributeType<?>... characteristics)
    {
        super(properties);
        ArgumentChecks.ensureNonNull("characteristics", characteristics);
        this.isAbstract = isAbstract;
        this.superTypes = (superTypes == null) ? Collections.<DefaultFeatureType>emptySet() :
                          CollectionsExt.<DefaultFeatureType>immutableSet(true, superTypes);
        this.characteristics = UnmodifiableArrayList.wrap(Arrays.copyOf(
                characteristics, characteristics.length, DefaultAttributeType[].class));
        byName = byName(this.characteristics);
    }

    /**
     * Creates a (<var>name</var>, <var>property</var>) map from the given list.
     * This is used only for performance purpose when searching for attributes.
     *
     * <p>As a side effect, this method checks for missing or duplicated names.</p>
     *
     * @param  characteristics The list to convert to a (<var>name</var>, <var>property</var>) map.
     * @return The map of properties.
     * @throws IllegalArgumentException if two properties have the same name.
     */
    private static Map<String, DefaultAttributeType<?>> byName(final List<DefaultAttributeType<?>> characteristics) {
        final int length = characteristics.size();
        final Map<String, DefaultAttributeType<?>> byName = new HashMap<>(Containers.hashMapCapacity(length));
        for (int i=0; i<length; i++) {
            final DefaultAttributeType<?> c = characteristics.get(i);
            ArgumentChecks.ensureNonNullElement("characteristics", i, c);
            final GenericName name = c.getName();
            if (name == null) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.MissingValueForProperty_1, "characteristics[" + i + "].name"));
            }
            final String s = name.toString();
            if (byName.put(s, c) != null) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.DuplicatedIdentifier_1, s));
            }
        }
        return byName;
    }

    /**
     * Invoked on deserialization for restoring the {@link #byName} map.
     *
     * @param  in The input stream from which to deserialize a feature type.
     * @throws IOException If an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException If the class serialized on the stream is not on the classpath.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        try {
            final Field field = DefaultFeatureType.class.getDeclaredField("byName");
            field.setAccessible(true);
            field.set(this, byName(characteristics));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns {@code true} if the feature type acts as an abstract super-type.
     *
     * @return {@code true} if the feature type acts as an abstract super-type.
     */
    public boolean isAbstract() {
        return isAbstract;
    }

    /**
     * Returns the parents of this feature type.
     *
     * <div class="warning"><b>Warning:</b>
     * The type of list elements will be changed to {@code FeatureType} if and when such interface
     * will be defined in GeoAPI.</div>
     *
     * @return The parents of this feature type, or an empty set if none.
     */
    public Set<DefaultFeatureType> superTypes() {
        return superTypes;
    }

    /**
     * Returns any feature operation, any feature attribute type and any feature association role
     * that carries characteristics of a feature type.
     *
     * <div class="warning"><b>Warning:</b>
     * The type of list elements will be changed to {@code PropertyType} if and when such interface
     * will be defined in GeoAPI.</div>
     *
     * @return Feature operation, attribute type and association role that carries characteristics of a feature type.
     */
    public List<DefaultAttributeType<?>> characteristics() {
        return characteristics;
    }

    /**
     * Returns the attribute, operation or association role for the given name.
     *
     * @param  name The name of the property to search.
     * @return The property for the given name, or {@code null} if none.
     */
    final DefaultAttributeType<?> getProperty(final String name) {
        return byName.get(name);
    }

    /**
     * Returns a hash code value for this feature type.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode() + superTypes.hashCode() + 37*superTypes.hashCode();
    }

    /**
     * Compares this feature type with the given object for equality.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (super.equals(obj)) {
            final DefaultFeatureType that = (DefaultFeatureType) obj;
            return isAbstract == that.isAbstract &&
                   superTypes.equals(that.superTypes) &&
                   characteristics.equals(that.characteristics);
        }
        return false;
    }
}
