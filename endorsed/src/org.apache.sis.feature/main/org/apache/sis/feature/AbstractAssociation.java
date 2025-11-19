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
import org.apache.sis.feature.internal.Resources;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.FeatureAssociation;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.InvalidPropertyValueException;
import org.opengis.feature.MultiValuedPropertyException;


/**
 * An instance of an {@linkplain DefaultAssociationRole feature association role} containing the associated feature.
 * {@code AbstractAssociation} can be instantiated by calls to {@link DefaultAssociationRole#newInstance()}.
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li><b>Multi-threading:</b> {@code AbstractAssociation} instances are <strong>not</strong> thread-safe.
 *       Synchronization, if needed, shall be done externally by the caller.</li>
 *   <li><b>Serialization:</b> serialized objects of this class are not guaranteed to be compatible with future
 *       versions. Serialization should be used only for short term storage or RMI between applications running
 *       the same SIS version.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see AbstractFeature
 * @see DefaultAssociationRole
 *
 * @since 0.5
 */
public abstract class AbstractAssociation extends Field<Feature> implements FeatureAssociation, Cloneable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5992169056331267867L;

    /**
     * Information about the association.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    final FeatureAssociationRole role;

    /**
     * Creates a new association of the given role.
     *
     * @param role  information about the association.
     *
     * @see #create(FeatureAssociationRole)
     */
    protected AbstractAssociation(final FeatureAssociationRole role) {
        this.role = role;
    }

    /**
     * Creates a new association of the given role.
     *
     * @param  role  information about the association.
     * @return the new association.
     *
     * @see DefaultAssociationRole#newInstance()
     */
    public static AbstractAssociation create(final FeatureAssociationRole role) {
        return isSingleton(role.getMaximumOccurs())
               ? new SingletonAssociation(role)
               : new MultiValuedAssociation(role);
    }

    /**
     * Creates a new association of the given role initialized to the given value.
     *
     * @param  role   information about the association.
     * @param  value  the initial value (may be {@code null}).
     * @return the new association.
     */
    static AbstractAssociation create(final FeatureAssociationRole role, final Object value) {
        return isSingleton(role.getMaximumOccurs())
               ? new SingletonAssociation(role, (Feature) value)
               : new MultiValuedAssociation(role, value);
    }

    /**
     * Returns the name of this association as defined by its {@linkplain #getRole() role}.
     * This convenience method delegates to {@link FeatureAssociationRole#getName()}.
     *
     * @return the association name specified by its role.
     */
    @Override
    public GenericName getName() {
        return role.getName();
    }

    /**
     * Returns information about the association.
     *
     * @return information about the association.
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
     * @return the associated feature (may be {@code null}).
     * @throws MultiValuedPropertyException if this association contains more than one value.
     *
     * @see AbstractFeature#getPropertyValue(String)
     */
    @Override
    public abstract Feature getValue() throws MultiValuedPropertyException;

    /**
     * Returns all features, or an empty collection if none.
     * The returned collection is <em>live</em>: changes in the returned collection
     * will be reflected immediately in this {@code Association} instance, and conversely.
     *
     * <p>The default implementation returns a collection which will delegate its work to
     * {@link #getValue()} and {@link #setValue(Object)}.</p>
     *
     * @return the features in a <em>live</em> collection.
     */
    @Override
    public Collection<Feature> getValues() {
        return super.getValues();
    }

    /**
     * Sets the associated feature.
     *
     * <h4>Validation</h4>
     * The number of validations performed by this method is implementation dependent.
     * Usually, only the most basic constraints are verified. This is so for performance reasons
     * and also because some rules may be temporarily broken while constructing a feature.
     * A more exhaustive verification can be performed by invoking the {@link #quality()} method.
     *
     * @param  value  the new value, or {@code null}.
     * @throws InvalidPropertyValueException if the given feature is not valid for this association.
     *
     * @see AbstractFeature#setPropertyValue(String, Object)
     */
    @Override
    public abstract void setValue(final Feature value) throws InvalidPropertyValueException;

    /**
     * Sets the features. All previous values are replaced by the given collection.
     *
     * <p>The default implementation ensures that the given collection contains at most one element,
     * then delegates to {@link #setValue(Feature)}.</p>
     *
     * @param  values  the new values.
     * @throws InvalidPropertyValueException if the given collection contains too many elements.
     */
    @Override
    public void setValues(final Collection<? extends Feature> values) throws InvalidPropertyValueException {
        super.setValues(values);
    }

    /**
     * Ensures that storing a feature of the given type is valid for an association
     * expecting the given base type.
     */
    final void ensureValid(final FeatureType base, final FeatureType type) {
        if (base != type && !DefaultFeatureType.maybeAssignableFrom(base, type)) {
            throw new InvalidPropertyValueException(
                    Resources.format(Resources.Keys.IllegalFeatureType_4, 0, getName(), base.getName(), type.getName()));
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
     * @return reports on all constraint violations found.
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
     * @return a string representation of this association for debugging purpose.
     */
    @Override
    public String toString() {
        final String pt = DefaultAssociationRole.getTitleProperty(role);
        final Iterator<Feature> it = getValues().iterator();
        return FieldType.toString(isDeprecated(role), "FeatureAssociation", role.getName(),
                DefaultAssociationRole.getValueTypeName(role), new Iterator<Object>()
        {
            @Override public boolean hasNext() {
                return it.hasNext();
            }

            @Override public Object next() {
                return it.next().getPropertyValue(pt);
            }
        }).toString();
    }

    /**
     * Returns a copy of this association.
     * The default implementation returns a <em>shallow</em> copy:
     * the association {@linkplain #getValue() value} is <strong>not</strong> cloned.
     * However, subclasses may choose to do otherwise.
     *
     * @return a clone of this association.
     * @throws CloneNotSupportedException if this association cannot be cloned.
     *         The default implementation never throw this exception. However, subclasses may throw it.
     */
    @Override
    public AbstractAssociation clone() throws CloneNotSupportedException {
        return (AbstractAssociation) super.clone();
    }
}
