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
import org.apache.sis.util.Debug;

import static org.apache.sis.util.ArgumentChecks.*;


/**
 * Indicates the role played by the association between two features.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public class DefaultAssociationRole extends FieldType {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1592712639262027124L;

    /**
     * The type of feature instances to be associated.
     *
     * @see #getValueType()
     */
    private final DefaultFeatureType valueType;

    /**
     * Constructs an association role from the given properties. The properties map is given unchanged
     * to the {@linkplain AbstractIdentifiedType#AbstractIdentifiedType(Map) super-class constructor}.
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
     * @param properties    The name and other properties to be given to this association role.
     * @param valueType     The type of feature values.
     * @param minimumOccurs The minimum number of occurrences of the association within its containing entity.
     * @param maximumOccurs The maximum number of occurrences of the association within its containing entity,
     *                      or {@link Integer#MAX_VALUE} if there is no restriction.
     */
    public DefaultAssociationRole(final Map<String,?> properties, final DefaultFeatureType valueType,
            final int minimumOccurs, final int maximumOccurs)
    {
        super(properties, minimumOccurs, maximumOccurs);
        ensureNonNull("valueType", valueType);
        this.valueType = valueType;
    }

    /**
     * Returns the type of feature values.
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed
     * to {@code org.opengis.feature.FeatureType}. This change is pending GeoAPI revision.</div>
     *
     * @return The type of feature values.
     */
    public final DefaultFeatureType getValueType() {
        return valueType;
    }


    /**
     * Returns the minimum number of occurrences of the association within its containing entity.
     * The returned value is greater than or equal to zero.
     *
     * @return The minimum number of occurrences of the association within its containing entity.
     */
    @Override
    public final int getMinimumOccurs() {
        return super.getMinimumOccurs();
    }

    /**
     * Returns the maximum number of occurrences of the association within its containing entity.
     * The returned value is greater than or equal to the {@link #getMinimumOccurs()} value.
     * If there is no maximum, then this method returns {@link Integer#MAX_VALUE}.
     *
     * @return The maximum number of occurrences of the association within its containing entity,
     *         or {@link Integer#MAX_VALUE} if none.
     */
    @Override
    public final int getMaximumOccurs() {
        return super.getMaximumOccurs();
    }

    /**
     * Returns a hash code value for this association role.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode() + valueType.hashCode();
    }

    /**
     * Compares this association role with the given object for equality.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (super.equals(obj)) {
            final DefaultAssociationRole that = (DefaultAssociationRole) obj;
            return valueType.equals(that.valueType);
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
        return toString("AssociationRole", valueType.getName().toString()).toString();
    }
}
