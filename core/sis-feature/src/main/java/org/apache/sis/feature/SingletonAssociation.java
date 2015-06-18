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

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * An instance of an {@linkplain DefaultAssociationRole association role} containing at most one value.
 * The majority of features types contain associations restricted to such [0 … 1] cardinality.
 * While {@link MultiValuedAssociation} would be suitable to all cases, this {@code SingletonAssociation}
 * consumes less memory.
 *
 * <div class="section">Limitations</div>
 * <ul>
 *   <li><b>Multi-threading:</b> {@code SingletonAssociation} instances are <strong>not</strong> thread-safe.
 *       Synchronization, if needed, shall be done externally by the caller.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 *
 * @see DefaultAssociationRole
 */
final class SingletonAssociation extends AbstractAssociation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5247767277033831214L;

    /**
     * The associated feature.
     */
    private AbstractFeature value;

    /**
     * Creates a new association of the given role.
     *
     * @param role Information about the association.
     */
    public SingletonAssociation(final DefaultAssociationRole role) {
        super(role);
        assert isSingleton(role.getMaximumOccurs());
    }

    /**
     * Creates a new association of the given role initialized to the given value.
     *
     * @param role  Information about the association.
     * @param value The initial value (may be {@code null}).
     */
    SingletonAssociation(final DefaultAssociationRole role, final AbstractFeature value) {
        super(role);
        assert isSingleton(role.getMaximumOccurs());
        this.value = value;
        if (value != null) {
            ensureValid(role.getValueType(), value.getType());
        }
    }

    /**
     * Returns the associated feature.
     *
     * @return The associated feature (may be {@code null}).
     */
    @Override
    public AbstractFeature getValue() {
        return value;
    }

    /**
     * Sets the associated feature.
     *
     * @param  value The new value, or {@code null}.
     * @throws IllegalArgumentException If the given feature is not valid for this association.
     */
    @Override
    public void setValue(final AbstractFeature value) throws IllegalArgumentException {
        if (value != null) {
            ensureValid(role.getValueType(), value.getType());
        }
        this.value = value;
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
        if (obj instanceof SingletonAssociation) {
            final SingletonAssociation that = (SingletonAssociation) obj;
            return role.equals(that.role) && Objects.equals(value, that.value);
        }
        return false;
    }
}
