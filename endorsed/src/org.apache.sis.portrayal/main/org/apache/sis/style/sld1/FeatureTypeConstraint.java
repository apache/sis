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
import org.opengis.util.GenericName;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;


/**
 * Restricts the layer's feature instances to a specific feature type,
 * optionally further restricted by a filter and by value extents on some attributes.
 *
 * @author  Johann Sorel (Geomatys)
 */
public class FeatureTypeConstraint extends SLDElement {
    /**
     * Name of the feature type to which this constraint applies, or {@code null} if none.
     *
     * @see #getFeatureTypeName()
     * @see #setFeatureTypeName(GenericName)
     */
    protected GenericName featureTypeName;

    /**
     * Filter restricting the feature instances shown, or {@code null} if none.
     *
     * @see #getFilter()
     * @see #setFilter(Filter)
     */
    protected Filter<? super Feature> filter;

    /**
     * Value extents on some feature attributes, as a live list.
     *
     * @see #extents()
     */
    private List<Extent> extents;

    /**
     * Creates an initially empty feature type constraint.
     */
    public FeatureTypeConstraint() {
        extents = new ArrayList<>();
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public FeatureTypeConstraint(final FeatureTypeConstraint source) {
        featureTypeName = source.featureTypeName;
        filter          = source.filter;
        extents         = new ArrayList<>(source.extents);
    }

    /**
     * Returns the name of the feature type to which this constraint applies.
     *
     * @return name of the feature type to which this constraint applies.
     */
    public Optional<GenericName> getFeatureTypeName() {
        return Optional.ofNullable(featureTypeName);
    }

    /**
     * Sets the name of the feature type to which this constraint applies.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new name of the feature type, or {@code null} if none.
     */
    public void setFeatureTypeName(final GenericName value) {
        featureTypeName = value;
    }

    /**
     * Returns the filter restricting the feature instances shown.
     * If absent, all instances of the feature type are shown.
     *
     * @return filter restricting the feature instances shown.
     */
    public Optional<Filter<? super Feature>> getFilter() {
        return Optional.ofNullable(filter);
    }

    /**
     * Sets the filter restricting the feature instances shown.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new filter restricting the feature instances, or {@code null} if none.
     */
    public void setFilter(final Filter<? super Feature> value) {
        filter = value;
    }

    /**
     * Returns the value extents on some feature attributes.
     *
     * <p>The returned collection is <em>live</em>:
     * changes in that collection are reflected into this object, and conversely.</p>
     *
     * @return value extents on some feature attributes, as a live collection.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<Extent> extents() {
        return extents;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {featureTypeName, filter, extents};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but the filter is not on the assumption that it is immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public FeatureTypeConstraint clone() {
        final var clone = (FeatureTypeConstraint) super.clone();
        clone.extents = new ArrayList<>(extents);
        clone.extents.replaceAll(Extent::clone);
        return clone;
    }
}
