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
package org.apache.sis.internal.feature;

import java.util.HashMap;
import java.util.Map;
import org.opengis.util.GenericName;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.util.ArgumentChecks;

// Branch-dependent imports
import org.opengis.feature.IdentifiedType;


/**
 * Base class of feature and attribute builders.
 * This base class provide the method needed for filling the {@code identification} map.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
abstract class Builder {
    /**
     * The feature name, definition, designation and description.
     * The name is mandatory; all other information are optional.
     */
    final Map<String,Object> identification = new HashMap<>();

    /**
     * Creates a new builder instance.
     */
    Builder() {
    }

    /**
     * Resets this builder to its initial state. After invocation of this method,
     * this builder is in the same state than after construction.
     */
    public void clear() {
        identification.clear();
    }

    /**
     * Sets this builder state to properties inferred from the given type.
     */
    final void copy(final IdentifiedType template) {
        clear();
        setName       (template.getName());
        setDescription(template.getDescription());
        setDefinition (template.getDefinition());
        setDesignation(template.getDesignation());
    }

    /**
     * Sets the feature type name as a simple string without scope.
     * The name will be an instance of {@link org.opengis.util.LocalName}.
     *
     * <p>This convenience method creates a {@link GenericName} instance,
     * then delegates to {@link #setName(GenericName)}.</p>
     *
     * @param localPart  the local part of the generic name (can not be {@code null}).
     */
    public void setName(String localPart) {
        ArgumentChecks.ensureNonNull("localPart", localPart);
        setName(name(null, localPart));
    }

    /**
     * Sets the feature type name as a string in the given scope.
     * If the given scope is non-null, then the name will be an instance of {@link org.opengis.util.ScopedName}.
     *
     * <p>This convenience method creates a {@link GenericName} instance,
     * then delegates to {@link #setName(GenericName)}.</p>
     *
     * @param scope      the scope of the name to create, or {@code null} if the name is local.
     * @param localPart  the local part of the generic name (can not be {@code null}).
     */
    public void setName(String scope, String localPart) {
        ArgumentChecks.ensureNonNull("localPart", localPart);
        setName(name(scope, localPart));
    }

    /**
     * Sets the feature type name as a generic name.
     * If another name was defined before this method call, that previous value will be discarded.
     *
     * <div class="note"><b>Note for subclasses:</b>
     * all {@code setName(…)} convenience methods in this builder delegate to this method.
     * Consequently this method can be used as a central place where to control the creation of all names.</div>
     *
     * @param name  the generic name (can not be {@code null}).
     *
     * @see DefaultFeatureType#NAME_KEY
     */
    public void setName(GenericName name) {
        ArgumentChecks.ensureNonNull("name", name);
        identification.put(DefaultFeatureType.NAME_KEY, name);
    }

    /**
     * Returns the current {@code FeatureType} name, or {@code null} if undefined.
     * This method returns the value built from the last call to a {@code setName(…)} method.
     *
     * @return the current {@code FeatureType} name, or {@code null} if the name has not yet been specified.
     */
    public GenericName getName() {
        return (GenericName) identification.get(DefaultFeatureType.NAME_KEY);
    }

    /**
     * Sets optional information beyond that required for concise definition of the element.
     * The description may assist in understanding the feature scope and application.
     *
     * @param description  information beyond that required for concise definition of the element, or {@code null} if none.
     *
     * @see DefaultFeatureType#DESCRIPTION_KEY
     */
    public void setDescription(CharSequence description) {
        identification.put(DefaultFeatureType.DESCRIPTION_KEY, description);
    }

    /**
     * Sets a natural language designator for the element.
     * This can be used as an alternative to the {@linkplain #getName() name} in user interfaces.
     *
     * @param designation a natural language designator for the element, or {@code null} if none.
     *
     * @see DefaultFeatureType#DESIGNATION_KEY
     */
    public void setDesignation(CharSequence designation) {
        identification.put(DefaultFeatureType.DESIGNATION_KEY, designation);
    }

    /**
     * Sets a concise definition of the element.
     *
     * @param definition a concise definition of the element, or {@code null} if none.
     *
     * @see DefaultFeatureType#DEFINITION_KEY
     */
    public void setDefinition(CharSequence definition) {
        identification.put(DefaultFeatureType.DEFINITION_KEY, definition);
    }

    /**
     * Creates a generic name from the given scope and local part.
     *
     * @param scope      the scope of the name to create, or {@code null} if the name is local.
     * @param localPart  the local part of the generic name (can not be {@code null}).
     */
    abstract GenericName name(String scope, String localPart);
}
