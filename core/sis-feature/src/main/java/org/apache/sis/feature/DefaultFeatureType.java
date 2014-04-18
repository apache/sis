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
import java.util.Collections;
import org.opengis.util.GenericName;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
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
 * <div class="warning"><b>Warning:</b>
 * This class is expected to implement a GeoAPI {@code FeatureType} interface in a future version.
 * When such interface will be available, most references to {@code DefaultFeatureType} in the API
 * will be replaced by references to the {@code FeatureType} interface.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public class DefaultFeatureType extends AbstractIdentifiedType {
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
     * @param properties The name and other properties to be given to this feature type.
     * @param isAbstract If {@code true}, the feature type acts as an abstract super-type.
     * @param superTypes The parents of this feature type, or {@code null} or empty if none.
     * @param characteristics Any feature operation, any feature attribute type and any feature
     *        association role that carries characteristics of a feature type.
     */
    public DefaultFeatureType(final Map<String,?> properties, final boolean isAbstract,
            final DefaultFeatureType[] superTypes, final DefaultAttributeType... characteristics)
    {
        super(properties);
        ArgumentChecks.ensureNonNull("characteristics", characteristics);
        this.isAbstract = isAbstract;
        this.superTypes = (superTypes == null) ? Collections.<DefaultFeatureType>emptySet() :
                          CollectionsExt.<DefaultFeatureType>immutableSet(true, superTypes);
        final DefaultAttributeType<?>[] copy = new DefaultAttributeType<?>[characteristics.length];
        for (int i=0; i<characteristics.length; i++) {
            copy[i] = characteristics[i];
            ArgumentChecks.ensureNonNullElement("characteristics", i, copy);
            /*
             * Ensure there is no conflict in property names.
             */
            final GenericName name = copy[i].getName();
            if (name == null) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.MissingValueForProperty_1, "characteristics[" + i + "].name"));
            }
            for (int j=i; --j >= 0;) {
                if (name.equals(copy[j].getName())) {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.DuplicatedIdentifier_1, name));
                }
            }
        }
        this.characteristics = UnmodifiableArrayList.wrap(copy);
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
    public List<DefaultAttributeType<?>> getCharacteristics() {
        return characteristics;
    }
}
