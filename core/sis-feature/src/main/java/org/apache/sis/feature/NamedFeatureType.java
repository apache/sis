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


/**
 * A feature type identified only by its name. Instances of {@code NamedFeatureType} shall be used only as placeholder
 * while building a cyclic graphs of {@link DefaultFeatureType}. Instances of {@code NamedFeatureType} will be replaced
 * by instances of the actual feature type when the later become known.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
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

    /**
     * Returns an empty set since this feature has no declared property yet.
     */
    @Override
    public Collection<AbstractIdentifiedType> getProperties(final boolean includeSuperTypes) {
        return Collections.emptySet();
    }

    /**
     * This feature type is considered independent of all other feature types except itself.
     */
    @Override
    public boolean isAssignableFrom(final DefaultFeatureType type) {
        return false;
    }

    /**
     * Returns a string representation of this feature type.
     */
    @Override
    public String toString() {
        return "FeatureType[“" + name + "”]";
    }
}
