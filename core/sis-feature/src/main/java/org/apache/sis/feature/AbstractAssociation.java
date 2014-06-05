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
import java.util.Iterator;
import java.io.Serializable;
import org.opengis.util.GenericName;
import org.opengis.metadata.quality.DataQuality;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.FeatureAssociation;
import org.opengis.feature.FeatureAssociationRole;


/**
 * An instance of an {@linkplain DefaultAssociationRole feature association role} containing the associated feature.
 *
 * {@section Limitations}
 * <ul>
 *   <li><b>Multi-threading:</b> {@code AbstractAssociation} instances are <strong>not</strong> thread-safe.
 *       Synchronization, if needed, shall be done externally by the caller.</li>
 *   <li><b>Serialization:</b> serialized objects of this class are not guaranteed to be compatible with future
 *       versions. Serialization should be used only for short term storage or RMI between applications running
 *       the same SIS version.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see DefaultAssociationRole
 */
public abstract class AbstractAssociation extends Field<Feature> implements FeatureAssociation, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5992169056331267867L;

    /**
     * Information about the association.
     */
    final FeatureAssociationRole role;

    /**
     * Creates a new association of the given role.
     *
     * @param role Information about the association.
     *
     * @see #create(FeatureAssociationRole)
     */
    protected AbstractAssociation(final FeatureAssociationRole role) {
        this.role = role;
    }

    /**
     * Creates a new association of the given role.
     *
     * @param  role Information about the association.
     * @return The new association.
     */
    public static AbstractAssociation create(final FeatureAssociationRole role) {
        ArgumentChecks.ensureNonNull("role", role);
        return isSingleton(role.getMaximumOccurs())
               ? new SingletonAssociation(role)
               : new MultiValuedAssociation(role);
    }

    /**
     * Creates a new association of the given role initialized to the given value.
     *
     * @param  role  Information about the association.
     * @param  value The initial value (may be {@code null}).
     * @return The new association.
     */
    static AbstractAssociation create(final FeatureAssociationRole role, final Object value) {
        ArgumentChecks.ensureNonNull("role", role);
        return isSingleton(role.getMaximumOccurs())
               ? new SingletonAssociation(role, (Feature) value)
               : new MultiValuedAssociation(role, value);
    }

    /**
     * Returns the name of this association as defined by its {@linkplain #getRole() role}.
     * This convenience method delegates to {@link FeatureAssociationRole#getName()}.
     *
     * @return The association name specified by its role.
     */
    @Override
    public GenericName getName() {
        return role.getName();
    }

    /**
     * Returns information about the association.
     *
     * @return Information about the association.
     */
    @Override
    public FeatureAssociationRole getRole() {
        return role;
    }

    /**
     * Returns the associated feature, or {@code null} if none. This convenience method can be invoked in
     * the common case where the {@linkplain DefaultAssociationRole#getMaximumOccurs() maximum number} of
     * features is restricted to 1 or 0.
     *
     * @return The associated feature (may be {@code null}).
     * @throws IllegalStateException if this association contains more than one value.
     *
     * @see AbstractFeature#getPropertyValue(String)
     */
    @Override
    public abstract Feature getValue();

    /**
     * Returns all features, or an empty collection if none.
     * The returned collection is <cite>live</cite>: changes in the returned collection
     * will be reflected immediately in this {@code Association} instance, and conversely.
     *
     * <p>The default implementation returns a collection which will delegate its work to
     * {@link #getValue()} and {@link #setValue(Object)}.</p>
     *
     * @return The features in a <cite>live</cite> collection.
     */
    @Override
    public Collection<Feature> getValues() {
        return super.getValues();
    }

    /**
     * Sets the associated feature.
     *
     * {@section Validation}
     * The amount of validation performed by this method is implementation dependent.
     * Usually, only the most basic constraints are verified. This is so for performance reasons
     * and also because some rules may be temporarily broken while constructing a feature.
     * A more exhaustive verification can be performed by invoking the {@link #quality()} method.
     *
     * @param  value The new value, or {@code null}.
     * @throws IllegalArgumentException If the given feature is not valid for this association.
     *
     * @see AbstractFeature#setPropertyValue(String, Object)
     */
    @Override
    public abstract void setValue(final Feature value) throws IllegalArgumentException;

    /**
     * Sets the features. All previous values are replaced by the given collection.
     *
     * <p>The default implementation ensures that the given collection contains at most one element,
     * then delegates to {@link #setValue(AbstractFeature)}.</p>
     *
     * @param  values The new values.
     * @throws IllegalArgumentException if the given collection contains too many elements.
     */
    @Override
    public void setValues(final Collection<? extends Feature> values) throws IllegalArgumentException {
        super.setValues(values);
    }

    /**
     * Ensures that storing a feature of the given type is valid for an association
     * expecting the given base type.
     */
    final void ensureValid(final FeatureType base, final FeatureType type) {
        if (base != type && !DefaultFeatureType.maybeAssignableFrom(base, type)) {
            throw new IllegalArgumentException(
                    Errors.format(Errors.Keys.IllegalArgumentClass_3, getName(), base.getName(), type.getName()));
        }
    }

    /**
     * Verifies if the current association value mets the constraints defined by the association role.
     * This method returns at most one {@linkplain org.apache.sis.metadata.iso.quality.DefaultDataQuality#getReports()
     * report} with a {@linkplain org.apache.sis.metadata.iso.quality.DefaultDomainConsistency#getResults() result} for
     * each constraint violations found, if any.
     * See {@link AbstractAttribute#quality()} for an example.
     *
     * <p>This association is valid if this method does not report any
     * {@linkplain org.apache.sis.metadata.iso.quality.DefaultConformanceResult conformance result} having a
     * {@linkplain org.apache.sis.metadata.iso.quality.DefaultConformanceResult#pass() pass} value of {@code false}.</p>
     *
     * @return Reports on all constraint violations found.
     *
     * @see AbstractFeature#quality()
     */
    public DataQuality quality() {
        final Validator v = new Validator(null);
        v.validate(role, getValues());
        return v.quality;
    }

    /**
     * Returns a string representation of this association.
     * The returned string is for debugging purpose and may change in any future SIS version.
     *
     * @return A string representation of this association for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        final String pt = DefaultAssociationRole.getTitleProperty(role);
        final Iterator<Feature> it = getValues().iterator();
        return FieldType.toString("FeatureAssociation", role, role.getValueType().getName(), new Iterator<Object>() {
            @Override public boolean hasNext() {
                return it.hasNext();
            }

            @Override public Object next() {
                return it.next().getPropertyValue(pt);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        });
    }
}
