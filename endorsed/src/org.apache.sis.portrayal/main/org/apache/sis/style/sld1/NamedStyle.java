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
import org.apache.sis.style.Style;
import org.apache.sis.style.se1.Description;


/**
 * Reference, by name, to a style known to the map server.
 * Named styles can be used only in {@link NamedLayer}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public class NamedStyle extends SLDElement implements Style {
    /**
     * Name of the referenced style, or {@code null} if none.
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
     * Creates an initially empty named style.
     * Callers should set the {@linkplain #setName(String) name} after construction,
     * since this property is mandatory.
     */
    public NamedStyle() {
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public NamedStyle(final NamedStyle source) {
        name        = source.name;
        description = source.description;
    }

    /**
     * Returns the name of the referenced style.
     * This is the name of a style known to the map server.
     *
     * @return name of the referenced style.
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Sets the name of the referenced style.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new name of the referenced style, or {@code null} if none.
     */
    public void setName(final String value) {
        name = value;
    }

    /**
     * Returns the description of this style.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this style, and conversely.
     *
     * @return information for user interfaces.
     */
    public Optional<Description<?>> getDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * Sets a description of this style.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new information for user interfaces, or {@code null} if none.
     */
    public void setDescription(final Description<?> value) {
        description = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {name, description};
    }

    /**
     * Returns a deep clone of this object.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public NamedStyle clone() {
        final var clone = (NamedStyle) super.clone();
        if (clone.description != null) {
            clone.description = clone.description.clone();
        }
        return clone;
    }
}
