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
package org.apache.sis.style.se1;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;


/**
 * A set of styles to be applied on different types of features.
 * This class contains a list of {@link FeatureTypeStyle}.
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Johann Sorel (Geomatys)
 * @author  Chris Dillard (SYS Technologies)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public class Style extends StyleElement {
    /**
     * Name for this style, or {@code null} if none.
     *
     * @see #getName()
     * @see #setName(String)
     */
    private String name;

    /**
     * Information for user interfaces, or {@code null} if none.
     *
     * @see #getDescription()
     * @see #setDescription(Description)
     */
    private Description description;

    /**
     * Whether this style is the default one.
     *
     * @see #isDefault()
     * @see #setDefault(boolean)
     */
    private boolean isDefault;

    /**
     * Collection of styles to apply for different types of features.
     *
     * @see #featureTypeStyles()
     */
    private List<FeatureTypeStyle> fts;

    /**
     * The default symbolizer to use if no rule return {@code true}.
     *
     * @see #getDefaultSpecification()
     * @see #setDefaultSpecification(Symbolizer)
     */
    private Symbolizer defaultSpecification;

    /**
     * Creates an initially empty style.
     */
    public Style() {
        fts = new ArrayList<>();
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public Style(final Style source) {
        super(source);
        name        = source.name;
        description = source.description;
        isDefault   = source.isDefault;
        defaultSpecification = source.defaultSpecification;
        fts = new ArrayList<>(source.fts);
    }

    /**
     * Returns the name for this style.
     * This can be any string that uniquely identifies this style within a given canvas.
     * It is not meant to be human-friendly. For a human-friendly label,
     * see the {@linkplain Description#getTitle() title} instead.
     *
     * @return a name for this style.
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Sets a name for this style.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new name for this style, or {@code null} if none.
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
    public Optional<Description> getDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * Sets a description of this style.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new information for user interfaces, or {@code null} if none.
     */
    public void setDescription(final Description value) {
        description = value;
    }

    /**
     * Returns the list of styles to apply for different types of features.
     * The returned collection is <em>live</em>:
     * changes in that collection are reflected into this object, and conversely.
     *
     * @return list of styles, as a live collection.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<FeatureTypeStyle> featureTypeStyles() {
        return fts;
    }

    /**
     * Returns whether this style is the default one.
     *
     * @return Whether this style is the default one.
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Sets whether this style is the default one.
     *
     * @param  value  whether this style is the default one.
     */
    public void setDefault(final boolean value) {
        isDefault = value;
    }

    /**
     * Returns the default symbolizer to use if no rule return {@code true}.
     * This specification should not use any external functions.
     * This specification should use at least one spatial attribute.
     *
     * @return the default symbolizer to use if no rule return {@code true}.
     */
    public Optional<Symbolizer> getDefaultSpecification() {
        return Optional.ofNullable(defaultSpecification);
    }

    /**
     * Sets the default symbolizer to use if no rule return {@code true}.
     *
     * @param  value  new default symbolizer to use if no rule return {@code true}.
     */
    public void setDefaultSpecification(final Symbolizer value) {
        defaultSpecification = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {name, description, isDefault, fts, defaultSpecification};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public Style clone() {
        final var clone = (Style) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (description != null) {
            description = description.clone();
        }
        if (defaultSpecification != null) {
            defaultSpecification = defaultSpecification.clone();
        }
        fts = new ArrayList<>(fts);
        fts.replaceAll(FeatureTypeStyle::clone);
    }
}
