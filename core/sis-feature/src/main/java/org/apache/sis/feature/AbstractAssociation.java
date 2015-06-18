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


/**
 * An instance of an {@linkplain DefaultAssociationRole feature association role} containing the associated feature.
 * {@code AbstractAssociation} can be instantiated by calls to {@link DefaultAssociationRole#newInstance()}.
 *
 * <div class="section">Limitations</div>
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
 * @version 0.6
 * @module
 *
 * @see DefaultAssociationRole#newInstance()
 */
public abstract class AbstractAssociation extends Field<AbstractFeature> implements Cloneable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5992169056331267867L;

    /**
     * Information about the association.
     */
    final DefaultAssociationRole role;

    /**
     * Creates a new association of the given role.
     *
     * @param role Information about the association.
     *
     * @see #create(DefaultAssociationRole)
     */
    protected AbstractAssociation(final DefaultAssociationRole role) {
        this.role = role;
    }

    /**
     * Creates a new association of the given role.
     *
     * @param  role Information about the association.
     * @return The new association.
     *
     * @see DefaultAssociationRole#newInstance()
     */
    public static AbstractAssociation create(final DefaultAssociationRole role) {
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
    static AbstractAssociation create(final DefaultAssociationRole role, final Object value) {
        ArgumentChecks.ensureNonNull("role", role);
        return isSingleton(role.getMaximumOccurs())
               ? new SingletonAssociation(role, (AbstractFeature) value)
               : new MultiValuedAssociation(role, value);
    }

    /**
     * Returns the name of this association as defined by its {@linkplain #getRole() role}.
     * This convenience method delegates to {@link DefaultAssociationRole#getName()}.
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
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed
     * to {@code org.opengis.feature.AssociationRole}. This change is pending GeoAPI revision.</div>
     *
     * @return Information about the association.
     */
    public DefaultAssociationRole getRole() {
        return role;
    }

    /**
     * Returns the associated feature, or {@code null} if none. This convenience method can be invoked in
     * the common case where the {@linkplain DefaultAssociationRole#getMaximumOccurs() maximum number} of
     * features is restricted to 1 or 0.
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed
     * to {@code org.opengis.feature.Feature}. This change is pending GeoAPI revision.</div>
     *
     * @return The associated feature (may be {@code null}).
     * @throws IllegalStateException if this association contains more than one value.
     *
     * @see AbstractFeature#getPropertyValue(String)
     */
    @Override
    public abstract AbstractFeature getValue() throws IllegalStateException;

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
    public Collection<AbstractFeature> getValues() {
        return super.getValues();
    }

    /**
     * Sets the associated feature.
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the argument type may be changed
     * to {@code org.opengis.feature.Feature}. This change is pending GeoAPI revision.</div>
     *
     * <div class="section">Validation</div>
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
    public abstract void setValue(final AbstractFeature value) throws IllegalArgumentException;

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
    public void setValues(final Collection<? extends AbstractFeature> values) throws IllegalArgumentException {
        super.setValues(values);
    }

    /**
     * Ensures that storing a feature of the given type is valid for an association
     * expecting the given base type.
     */
    final void ensureValid(final DefaultFeatureType base, final DefaultFeatureType type) {
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
        final Iterator<AbstractFeature> it = getValues().iterator();
        return FieldType.toString("FeatureAssociation", role, DefaultAssociationRole.getValueTypeName(role), new Iterator<Object>() {
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
        }).toString();
    }

    /**
     * Returns a copy of this association.
     * The default implementation returns a <em>shallow</em> copy:
     * the association {@linkplain #getValue() value} is <strong>not</strong> cloned.
     * However subclasses may choose to do otherwise.
     *
     * @return A clone of this association.
     * @throws CloneNotSupportedException if this association can not be cloned.
     *         The default implementation never throw this exception. However subclasses may throw it.
     */
    @Override
    public AbstractAssociation clone() throws CloneNotSupportedException {
        return (AbstractAssociation) super.clone();
    }
}
