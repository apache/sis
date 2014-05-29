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

import static org.apache.sis.util.ArgumentChecks.*;

// Branch-dependent imports
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureType;


/**
 * Indicates the role played by the association between two features.
 * In the area of geographic information, there exist multiple kinds of associations:
 *
 * <ul>
 *   <li><b>Aggregation</b> represents associations between features which can exist even if the aggregate is destroyed.</li>
 *   <li><b>Composition</b> represents relationships where the owned features are destroyed together with the composite.</li>
 *   <li><b>Spatial</b> association represents spatial or topological relationships that may exist between features (e.g. “<cite>east of</cite>”).</li>
 *   <li><b>Temporal</b> association may represent for example a sequence of changes over time involving the replacement of some
 *       feature instances by other feature instances.</li>
 * </ul>
 *
 * {@section Immutability and thread safety}
 * Instances of this class are immutable if all properties ({@link GenericName} and {@link InternationalString}
 * instances) and all arguments (e.g. {@code valueType}) given to the constructor are also immutable.
 * Such immutable instances can be shared by many objects and passed between threads without synchronization.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see AbstractAssociation
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
    private final FeatureType valueType;

    /**
     * The name of the property to use as a title for the associated feature, or an empty string if none.
     * This field is initially null, then computed when first needed.
     * This information is used only by {@link AbstractAssociation#toString()} implementation.
     *
     * @see #getTitleProperty()
     */
    private volatile transient String titleProperty;

    /**
     * Constructs an association role from the given properties. The properties map is given unchanged
     * to the {@linkplain AbstractIdentifiedType#AbstractIdentifiedType(Map) super-class constructor}.
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
     * @param identification The name and other information to be given to this association role.
     * @param valueType      The type of feature values.
     * @param minimumOccurs  The minimum number of occurrences of the association within its containing entity.
     * @param maximumOccurs  The maximum number of occurrences of the association within its containing entity,
     *                       or {@link Integer#MAX_VALUE} if there is no restriction.
     */
    public DefaultAssociationRole(final Map<String,?> identification, final FeatureType valueType,
            final int minimumOccurs, final int maximumOccurs)
    {
        super(identification, minimumOccurs, maximumOccurs);
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
    public final FeatureType getValueType() {
        return valueType;
    }

    /**
     * Returns the name of the property to use as a title for the associated feature, or {@code null} if none.
     * This method search for the first attribute having a value class assignable to {@link CharSequence}.
     */
    final String getTitleProperty() {
        String p = titleProperty; // No synchronization - not a big deal if computed twice.
        if (p == null) {
            p = "";
            for (final PropertyType type : valueType.getProperties(true)) {
                if (type instanceof AttributeType<?>) {
                    final AttributeType<?> pt = (AttributeType<?>) type;
                    if (pt.getMaximumOccurs() != 0 && CharSequence.class.isAssignableFrom(pt.getValueClass())) {
                        p = pt.getName().toString();
                        break;
                    }
                }
            }
            titleProperty = p;
        }
        return p.isEmpty() ? null : p;
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
     * Returns a string representation of this association role.
     * The returned string is for debugging purpose and may change in any future SIS version.
     *
     * @return A string representation of this association role for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        return toString("FeatureAssociationRole", this, valueType.getName()).toString();
    }
}
