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
    private DefaultFeature value;

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
    public DefaultAssociation(final DefaultAssociationRole role, final DefaultFeature value) {
        ArgumentChecks.ensureNonNull("role", role);
        this.role  = role;
        this.value = value;
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
     * @see DefaultFeature#getPropertyValue(String)
     */
    public DefaultFeature getValue() {
        return value;
    }

    /**
     * Sets the associated feature.
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the argument type may be changed
     * to {@code org.opengis.feature.Feature}. This change is pending GeoAPI revision.</div>
     *
     * @param  value The new value, or {@code null}.
     * @throws RuntimeException If this method performs validation and the given value does not meet the conditions.
     *         <span style="color:firebrick">This exception may be changed to {@code IllegalPropertyException} in a
     *         future SIS version.</span>
     *
     * @see DefaultFeature#setPropertyValue(String, Object)
     */
    public void setValue(final DefaultFeature value) {
        if (value != null) {
            final DefaultFeatureType base = role.getValueType();
            final DefaultFeatureType type = value.getType();
            if (!base.equals(type) && !isAssignableFrom(base, type.superTypes())) {
                throw new RuntimeException( // TODO: IllegalPropertyException, pending GeoAPI revision.
                        Errors.format(Errors.Keys.IllegalArgumentClass_3, role.getName(), base.getName(), type.getName()));
            }
        }
        this.value = value;
    }

    /**
     * Returns {@code true} if the given {@code base} is assignable from any of the given types.
     */
    private static boolean isAssignableFrom(final DefaultFeatureType base, final Iterable<DefaultFeatureType> types) {
        for (final DefaultFeatureType type : types) {
            if (base.equals(type)) {
                return true;
            }
            if (isAssignableFrom(base, type.superTypes())) {
                return true;
            }
        }
        return false;
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
        return role.toString("Association", role.getValueType().getName().toString())
                .append(" = ").append(value).toString();
    }
}
