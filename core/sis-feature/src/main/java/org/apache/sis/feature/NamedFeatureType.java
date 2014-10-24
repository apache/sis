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

import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import java.io.Serializable;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.Errors;


/**
 * A feature type identified only by its name. Instances of {@code NamedFeatureType} shall be used only as placeholder
 * while building a cyclic graphs of {@link DefaultFeatureType}. Instances of {@code NamedFeatureType} will be replaced
 * by instances of the actual feature type when the later become known.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class NamedFeatureType implements FeatureType, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 178781891980017372L;

    /**
     * The name of the feature type for which this {@code NamedFeatureType} is a placeholder.
     */
    private final GenericName name;

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

    /** Undefined. */ @Override public InternationalString getDefinition()  {return null;}
    /** Undefined. */ @Override public InternationalString getDesignation() {return null;}
    /** Undefined. */ @Override public InternationalString getDescription() {return null;}

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
     * Always throws {@link IllegalArgumentException} since this feature type has no declared property yet.
     */
    @Override
    public PropertyType getProperty(final String name) throws IllegalArgumentException {
        throw new IllegalArgumentException(Errors.format(Errors.Keys.PropertyNotFound_2, getName(), name));
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
     * This feature type is considered to all other features except itself.
     */
    @Override
    public boolean isAssignableFrom(final FeatureType type) {
        return (type instanceof NamedFeatureType);
    }

    /**
     * Returns a string representation of this feature type.
     */
    @Override
    public String toString() {
        return "FeatureType[“" + name + "”]";
    }
}
