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

import java.io.Serializable;
import org.opengis.util.GenericName;
import org.opengis.metadata.quality.DataQuality;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

// Related to JDK7
import java.util.Objects;


/**
 * Indicates the role played by the association between two features.
 *
 * {@section Usage in multi-thread environment}
 * {@code DefaultAssociation} are <strong>not</strong> thread-safe.
 * Synchronization, if needed, shall be done externally by the caller.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see DefaultAssociationRole
 */
public class DefaultAssociation extends Property implements Cloneable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1175014792131253528L;

    /**
     * Information about the association.
     */
    private final DefaultAssociationRole role;

    /**
     * The associated feature.
     */
    private AbstractFeature value;

    /**
     * Creates a new association of the given type.
     *
     * @param role Information about the association.
     */
    public DefaultAssociation(final DefaultAssociationRole role) {
        ArgumentChecks.ensureNonNull("role", role);
        this.role = role;
    }

    /**
     * Creates a new association of the given type initialized to the given value.
     *
     * @param role  Information about the association.
     * @param value The initial value.
     */
    public DefaultAssociation(final DefaultAssociationRole role, final AbstractFeature value) {
        ArgumentChecks.ensureNonNull("role", role);
        this.role  = role;
        this.value = value;
        if (value != null) {
            ensureValid(role.getValueType(), value.getType());
        }
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
     * Returns the associated feature.
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed
     * to {@code org.opengis.feature.Feature}. This change is pending GeoAPI revision.</div>
     *
     * @return The associated feature (may be {@code null}).
     *
     * @see AbstractFeature#getPropertyValue(String)
     */
    public AbstractFeature getValue() {
        return value;
    }

    /**
     * Sets the associated feature.
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the argument type may be changed
     * to {@code org.opengis.feature.Feature}. This change is pending GeoAPI revision.</div>
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
    public void setValue(final AbstractFeature value) {
        if (value != null) {
            ensureValid(role.getValueType(), value.getType());
        }
        this.value = value;
    }

    /**
     * Ensures that storing a feature of the given type is valid for an association
     * expecting the given base type.
     */
    private void ensureValid(final DefaultFeatureType base, final DefaultFeatureType type) {
        if (base != type && !base.maybeAssignableFrom(type)) {
            throw new IllegalArgumentException(
                    Errors.format(Errors.Keys.IllegalArgumentClass_3, getName(), base.getName(), type.getName()));
        }
    }

    /**
     * Verifies if the current association value mets the constraints defined by the association role.
     * This method returns at most one {@linkplain org.apache.sis.metadata.iso.quality.DefaultDataQuality#getReports()
     * report} with a {@linkplain org.apache.sis.metadata.iso.quality.DefaultDomainConsistency#getResults() result} for
     * each constraint violations found, if any.
     * See {@link DefaultAttribute#quality()} for an example.
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
        v.validate(role, value);
        return v.quality;
    }

    /**
     * Returns a shallow copy of this association.
     * The association {@linkplain #getValue() value} is <strong>not</strong> cloned.
     *
     * @return A clone of this association.
     */
    @Override
    public DefaultAssociation clone() {
        final DefaultAssociation clone;
        try {
            clone = (DefaultAssociation) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e); // Should never happen since we are cloneable.
        }
        return clone;
    }

    /**
     * Returns a hash code value for this association.
     *
     * @return A hash code value.
     */
    @Override
    public int hashCode() {
        return role.hashCode() + Objects.hashCode(value);
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
        if (obj != null && obj.getClass() == getClass()) {
            final DefaultAssociation that = (DefaultAssociation) obj;
            return role.equals(that.role) &&
                   Objects.equals(value, that.value);
        }
        return false;
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
        final StringBuilder buffer = role.toString("FeatureAssociation", role.getValueType().getName());
        if (value != null) {
            final String pt = role.getTitleProperty();
            if (pt != null) {
                buffer.append(" = ").append(value.getPropertyValue(pt));
            }
        }
        return buffer.toString();
    }
}
