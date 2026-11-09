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
package org.apache.sis.style.sld1;

import java.util.Optional;
import org.apache.sis.style.se1.Description;


/**
 * A layer of a map described by a {@link StyledLayerDescriptor}.
 * A layer is either a {@link NamedLayer}, which references styles known to the map server,
 * or a {@link UserLayer}, for which the styling is provided inline.
 *
 * @author  Johann Sorel (Geomatys)
 */
public abstract class Layer extends SLDElement {
    /**
     * Name for this layer, or {@code null} if none.
     *
     * @see #getName()
     * @see #setName(String)
     */
    protected String name;

    /**
     * Information for user interfaces, or {@code null} if none.
     *
     * @see #getDescription()
     * @see #setDescription(Description)
     */
    protected Description<?> description;

    /**
     * Creates an initially empty layer.
     */
    Layer() {
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    Layer(final Layer source) {
        name        = source.name;
        description = source.description;
    }

    /**
     * Returns the name for this layer.
     * For a {@link NamedLayer}, this is the name of a layer known to the map server.
     * For a {@link UserLayer}, this can be any string that identifies the layer.
     * It is not meant to be human-friendly. For a human-friendly label,
     * see the {@linkplain Description#getTitle() title} instead.
     *
     * @return a name for this layer.
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Sets a name for this layer.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new name for this layer, or {@code null} if none.
     */
    public void setName(final String value) {
        name = value;
    }

    /**
     * Returns the description of this layer.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this layer, and conversely.
     *
     * @return information for user interfaces.
     */
    public Optional<Description<?>> getDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * Sets a description of this layer.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new information for user interfaces, or {@code null} if none.
     */
    public void setDescription(final Description<?> value) {
        description = value;
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but filters, features and online resources are not.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public Layer clone() {
        final var clone = (Layer) super.clone();
        if (clone.description != null) {
            clone.description = clone.description.clone();
        }
        return clone;
    }
}
