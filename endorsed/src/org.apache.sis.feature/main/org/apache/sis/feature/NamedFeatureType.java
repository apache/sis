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
import java.util.Collections;
import java.io.Serializable;
import org.opengis.util.GenericName;
import org.apache.sis.util.internal.shared.Strings;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.util.Set;
import org.opengis.util.InternationalString;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.FeatureInstantiationException;
import org.apache.sis.feature.internal.Resources;


/**
 * A feature type identified only by its name. Instances of {@code NamedFeatureType} shall be used only as placeholder
 * while building a cyclic graphs of {@link DefaultFeatureType}. Instances of {@code NamedFeatureType} will be replaced
 * by instances of the actual feature type when the latter become known.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class NamedFeatureType implements FeatureType, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 178781891980017372L;

    /**
     * The name of the feature type for which this {@code NamedFeatureType} is a placeholder.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final GenericName name;

    /**
     * The feature type to use instead of the {@code NamedFeatureType}. Initially null, then set to the "real"
     * feature type after {@link DefaultAssociationRole#resolve(DefaultFeatureType, Collection)} has been able
     * to create it. This information is stored in case the same {@code NamedFeatureType} instance has been used
     * in more than one {@link DefaultFeatureType}.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    volatile FeatureType resolved;

    /**
     * Creates a new placeholder for a feature of the given name.
     */
    NamedFeatureType(final GenericName name) {
        this.name = name;
    }

    /**
     * Returns the name of this feature.
     */
    @Override
    public GenericName getName() {
        return name;
    }

    /**
     * Undefined.
     */
    @Override
    public InternationalString getDefinition() {
        return null;
    }

    /**
     * Declares that this feature shall not be instantiated.
     */
    @Override
    public boolean isAbstract() {
        return true;
    }

    /**
     * Conservatively assumes that the feature is not simple,
     * since we do not know what the actual feature will be.
     */
    @Override
    public boolean isSimple() {
        return false;
    }

    /**
     * Always throws {@link PropertyNotFoundException} since this feature type has no declared property yet.
     */
    @Override
    public PropertyType getProperty(final String name) throws PropertyNotFoundException {
        throw new PropertyNotFoundException(Resources.format(Resources.Keys.PropertyNotFound_2, getName(), name));
    }

    /**
     * Returns an empty set since this feature has no declared property yet.
     */
    @Override
    public Collection<? extends PropertyType> getProperties(final boolean includeSuperTypes) {
        return Collections.emptySet();
    }

    /**
     * Returns an empty set since this feature has no declared parent yet.
     */
    @Override
    public Set<? extends FeatureType> getSuperTypes() {
        return Collections.emptySet();
    }

    /**
     * This feature type is considered independent of all other feature types except itself.
     */
    @Override
    public boolean isAssignableFrom(FeatureType type) {
        if (type == this) {
            return true;
        }
        if (type instanceof NamedFeatureType) {
            type = ((NamedFeatureType) type).resolved;
        }
        if (type == null) {
            return false;
        }
        final FeatureType resolved = this.resolved;
        return (resolved != null) && resolved.isAssignableFrom(type);
    }

    /**
     * Unsupported operation, since the feature has not yet been resolved.
     */
    @Override
    public Feature newInstance() throws FeatureInstantiationException {
        throw new FeatureInstantiationException(Resources.format(Resources.Keys.UnresolvedFeatureName_1, getName()));
    }

    /**
     * Returns a string representation of this feature type.
     */
    @Override
    public String toString() {
        return Strings.bracket("FeatureType", name);
    }
}
