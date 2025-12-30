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
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.internal.shared.CloneAccess;
import org.apache.sis.feature.internal.Resources;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.MultiValuedPropertyException;


/**
 * An instance of an {@linkplain DefaultAssociationRole association role} containing an arbitrary number of values.
 *
 * <div class="note"><b>Note:</b> in the common case where the {@linkplain DefaultAssociationRole association role}
 * restricts the cardinality to [0 … 1], the {@link SingletonAssociation} implementation consumes less memory.</div>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li><b>Multi-threading:</b> {@code MultiValuedAssociation} instances are <strong>not</strong> thread-safe.
 *       Synchronization, if needed, shall be done externally by the caller.</li>
 *   <li><b>Serialization:</b> serialized objects of this class are not guaranteed to be compatible with future
 *       versions. Serialization should be used only for short term storage or RMI between applications running
 *       the same SIS version.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see DefaultAssociationRole
 */
final class MultiValuedAssociation extends AbstractAssociation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5089428248556035466L;

    /**
     * The association values.
     */
    @SuppressWarnings("serial")     // Should be an instance of `CheckedArrayList`.
    private Collection<Feature> values;

    /**
     * Creates a new association of the given role.
     *
     * @param role Information about the association.
     */
    public MultiValuedAssociation(final FeatureAssociationRole role) {
        super(role);
        values = Containers.newCheckedList(null, Feature.class);
    }

    /**
     * Creates a new association of the given role initialized to the given values.
     *
     * @param role   Information about the association.
     * @param values The initial values, or {@code null} for initializing to an empty list.
     */
    MultiValuedAssociation(final FeatureAssociationRole role, final Object values) {
        super(role);
        if (values == null) {
            this.values = Containers.newCheckedList(null, Feature.class);
        } else {
            this.values = AbstractFeature.castOrCopyAsCheckedList((Collection<?>) values, Feature.class);
        }
    }

    /**
     * Returns the feature, or {@code null} if none.
     *
     * @return the feature (may be {@code null}).
     * @throws MultiValuedPropertyException if this association contains more than one value.
     */
    @Override
    public Feature getValue() {
        switch (values.size()) {
            case 0:  return null;
            case 1:  return values.iterator().next();
            default: throw new MultiValuedPropertyException(Resources.format(Resources.Keys.NotASingleton_1, getName()));
        }
    }

    /**
     * Returns all features, or an empty collection if none.
     * The returned collection is <em>live</em>: changes in the returned collection
     * will be reflected immediately in this {@code Association} instance, and conversely.
     *
     * @return the features in a <em>live</em> collection.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Collection<Feature> getValues() {
        return values;
    }

    /**
     * Sets the feature.
     *
     * @param  value  the new value, or {@code null} for removing all values from this association.
     */
    @Override
    public void setValue(final Feature value) {
        values.clear();
        if (value != null) {
            ensureValid(role.getValueType(), value.getType());
            values.add(value);
        }
    }

    /**
     * Sets the feature values. All previous values are replaced by the given collection.
     *
     * @param  newValues  the new values.
     */
    @Override
    public void setValues(final Collection<? extends Feature> newValues) {
        if (newValues != values) {
            ArgumentChecks.ensureNonNull("values", newValues);      // The parameter name in public API is "values".
            final FeatureType base = role.getValueType();
            values.clear();
            for (final Feature value : newValues) {
                ensureValid(base, value.getType());
                values.add(value);
            }
        }
    }

    /**
     * Returns a copy of this association.
     * This implementation returns a <em>shallow</em> copy:
     * the association {@linkplain #getValues() values} are <strong>not</strong> cloned.
     *
     * @return a clone of this association.
     * @throws CloneNotSupportedException if this association cannot be cloned.
     */
    @Override
    @SuppressWarnings("unchecked")
    public MultiValuedAssociation clone() throws CloneNotSupportedException {
        final var clone = (MultiValuedAssociation) super.clone();
        clone.values = (Collection<Feature>) ((CloneAccess) clone.values).clone();
        return clone;
    }

    /**
     * Returns a hash code value for this association.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return role.hashCode() + values.hashCode();
    }

    /**
     * Compares this association with the given object for equality.
     *
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof MultiValuedAssociation) {
            final var that = (MultiValuedAssociation) obj;
            return role.equals(that.role) && values.equals(that.values);
        }
        return false;
    }
}
