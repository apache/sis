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
import org.opengis.util.GenericName;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureAssociation;
import org.opengis.feature.FeatureAssociationRole;


/**
 * An association implementation which delegate its work to the parent feature.
 * This class is used for default implementation of {@link AbstractFeature#getProperty(String)}.
 *
 * <p><strong>This implementation is inefficient!</strong>
 * This class is for making easier to begin with a custom {@link AbstractFeature} implementation,
 * but developers are encouraged to provide their own {@link AbstractFeature#getProperty(String)}
 * implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
class AssociationView extends PropertyView<Feature> implements FeatureAssociation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -148100967531766909L;

    /**
     * The role of this association. Must be one of the properties listed in the {@link #feature}.
     */
    private final FeatureAssociationRole role;

    /**
     * Creates a new association which will delegate its work to the given feature.
     */
    private AssociationView(final Feature feature, final FeatureAssociationRole role) {
        super(feature, role.getName().toString());
        this.role = role;
    }

    /**
     * Creates a new association which will delegate its work to the given feature.
     *
     * @param feature  the feature from which to read and where to write the association value.
     * @param role     the role of this association. Must be one of the properties listed in the
     *                 {@link #feature} (this is not verified by this constructor).
     */
    static FeatureAssociation create(final Feature feature, final FeatureAssociationRole role) {
        if (isSingleton(role.getMaximumOccurs())) {
            return new Singleton(feature, role);
        } else {
            return new AssociationView(feature, role);
        }
    }

    /**
     * Returns the name of the role specified at construction time.
     */
    @Override
    public final GenericName getName() {
        return role.getName();
    }

    /**
     * Returns the role specified at construction time.
     */
    @Override
    public final FeatureAssociationRole getRole() {
        return role;
    }

    /**
     * Returns the class of values.
     */
    @Override
    final Class<Feature> getValueClass() {
        return Feature.class;
    }

    /**
     * Specialization of {@code AssociationView} when the amount of values can be only zero or one.
     * This implementation takes shortcuts for the {@code getValue()} and {@code getValues()} methods.
     * This specialization is provided because it is the most common case.
     */
    private static final class Singleton extends AssociationView {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 2574475751526292380L;

        /**
         * Creates a new association which will delegate its work to the given feature.
         */
        Singleton(final Feature feature, final FeatureAssociationRole role) {
            super(feature, role);
        }

        /**
         * Returns the single value, or {@code null} if none.
         */
        @Override
        public Feature getValue() {
            return (Feature) this.feature.getPropertyValue(this.name);
        }

        /**
         * Sets the value of this association. This method assumes that the
         * {@link Feature#setPropertyValue(String, Object)} implementation
         * will verify the argument type.
         */
        @Override
        public void setValue(final Feature value) {
            this.feature.setPropertyValue(this.name, value);
        }

        /**
         * Wraps the property value in a set.
         */
        @Override
        public Collection<Feature> getValues() {
            return singletonOrEmpty(getValue());
        }
    }
}
