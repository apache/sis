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
import org.apache.sis.style.se1.Symbology;


/**
 * A layer of a map for which the styling is provided inline.
 * A user layer may also specify the source of the data to be styled
 * (either a remote service or inline features), and constraints on the
 * feature types or coverages available for this layer.
 *
 * @author  Johann Sorel (Geomatys)
 */
public class UserLayer extends Layer {
    /**
     * Source of the data to be styled, or {@code null} if none.
     *
     * @see #getSource()
     * @see #setSource(LayerSource)
     */
    protected LayerSource source;

    /**
     * Constraints on the feature types or coverages available for this layer, or {@code null} if none.
     *
     * @see #getConstraints()
     * @see #setConstraints(LayerConstraints)
     */
    protected LayerConstraints<?> constraints;

    /**
     * The user styles applicable to this layer, as a live list.
     *
     * @see #styles()
     */
    private List<Symbology> styles;

    /**
     * Creates an initially empty user layer.
     * Callers should add at least one {@linkplain #styles() style} after construction.
     */
    public UserLayer() {
        styles = new ArrayList<>();
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public UserLayer(final UserLayer source) {
        super(source);
        this.source = source.source;
        constraints = source.constraints;
        styles      = new ArrayList<>(source.styles);
    }

    /**
     * Returns the source of the data to be styled.
     * This is either a {@link RemoteOWS} or an {@link InlineFeature}.
     * If absent, the data source is determined by the context.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this layer, and conversely.
     *
     * @return source of the data to be styled.
     */
    public Optional<LayerSource> getSource() {
        return Optional.ofNullable(source);
    }

    /**
     * Sets the source of the data to be styled.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new source of the data to be styled, or {@code null} if none.
     */
    public void setSource(final LayerSource value) {
        source = value;
    }

    /**
     * Returns the constraints on the feature types or coverages available for this layer.
     * This is either a {@link LayerFeatureConstraints} or a {@link LayerCoverageConstraints}.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this layer, and conversely.
     *
     * @return constraints on the feature types or coverages available for this layer.
     */
    public Optional<LayerConstraints<?>> getConstraints() {
        return Optional.ofNullable(constraints);
    }

    /**
     * Sets the constraints on the feature types or coverages available for this layer.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new constraints on the feature types or coverages, or {@code null} if none.
     */
    public void setConstraints(final LayerConstraints<?> value) {
        constraints = value;
    }

    /**
     * Returns the user styles applicable to this layer.
     * A user layer should contain at least one style.
     *
     * <p>The returned collection is <em>live</em>:
     * changes in that collection are reflected into this object, and conversely.</p>
     *
     * @return styles applicable to this layer, as a live collection.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<Symbology> styles() {
        return styles;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {name, description, source, constraints, styles};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but filters, features and online resources are not.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public UserLayer clone() {
        final var clone = (UserLayer) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (source      != null) source      = source.clone();
        if (constraints != null) constraints = constraints.clone();
        styles = new ArrayList<>(styles);
        styles.replaceAll(Symbology::clone);
    }
}
