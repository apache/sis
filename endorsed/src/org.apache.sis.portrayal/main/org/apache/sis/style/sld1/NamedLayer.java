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

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import org.apache.sis.style.Style;
import org.apache.sis.style.se1.Symbology;


/**
 * A layer of a map, identified by referencing a layer known to a map server,
 * together with optional feature type constraints and the styles to apply.
 * The styles can be references to styles known to the map server ({@link NamedStyle})
 * or styles defined inline ({@link Symbology}).
 *
 * @author  Johann Sorel (Geomatys)
 */
public class NamedLayer extends Layer {
    /**
     * Constraints on the feature types available for this layer, or {@code null} if none.
     *
     * @see #getConstraints()
     * @see #setConstraints(LayerFeatureConstraints)
     */
    protected LayerFeatureConstraints constraints;

    /**
     * The named or user styles applicable to this layer, as a live list.
     * Elements are either {@link NamedStyle} or {@link Symbology} instances.
     *
     * @see #styles()
     */
    private List<Style> styles;

    /**
     * Creates an initially empty named layer.
     * Callers should set the {@linkplain #setName(String) name} after construction,
     * since this property is mandatory for named layers.
     */
    public NamedLayer() {
        styles = new ArrayList<>();
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public NamedLayer(final NamedLayer source) {
        super(source);
        constraints = source.constraints;
        styles      = new ArrayList<>(source.styles);
    }

    /**
     * Returns the constraints on the feature types available for this layer.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this layer, and conversely.
     *
     * @return constraints on the feature types available for this layer.
     */
    public Optional<LayerFeatureConstraints> getConstraints() {
        return Optional.ofNullable(constraints);
    }

    /**
     * Sets the constraints on the feature types available for this layer.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new constraints on the feature types, or {@code null} if none.
     */
    public void setConstraints(final LayerFeatureConstraints value) {
        constraints = value;
    }

    /**
     * Returns the styles applicable to this layer.
     * Elements should be {@link NamedStyle} or {@link Symbology} instances.
     * Other implementations of {@link Style} are not supported by the XML encoding.
     *
     * <p>The returned collection is <em>live</em>:
     * changes in that collection are reflected into this object, and conversely.</p>
     *
     * @return styles applicable to this layer, as a live collection.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<Style> styles() {
        return styles;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {name, description, constraints, styles};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but filters, features and online resources are not.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public NamedLayer clone() {
        final var clone = (NamedLayer) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (constraints != null) constraints = constraints.clone();
        styles = new ArrayList<>(styles);
        styles.replaceAll(NamedLayer::cloneStyle);
    }

    /**
     * Returns a clone of the given style if its class is known, or the style itself otherwise.
     *
     * @param  style  the style to clone.
     * @return a clone of the given style, or {@code style} if it cannot be cloned.
     */
    private static Style cloneStyle(final Style style) {
        if (style instanceof NamedStyle) {
            return ((NamedStyle) style).clone();
        }
        if (style instanceof Symbology) {
            return ((Symbology) style).clone();
        }
        return style;
    }
}
