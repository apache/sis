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
package org.apache.sis.gui.dataset;

import java.util.Arrays;
import java.util.Map;
import java.util.Collection;
import java.util.List;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;


/**
 * A feature where property values are specific elements of multi-valued properties of other features.
 * The element is identified by an index. The same index value is used for all multi-valued properties
 * of the {@linkplain #source} feature. Each property may have a different number of elements, so the
 * {@link #index} value can be between 0 (inclusive) and the maximal collection size (exclusive).
 * If a property is requested for which {@link #index} is too large, {@code null} is returned.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ExpandedFeature extends AbstractFeature {
    /**
     * The array for properties having no value.
     * This is the fill value for {@link #values} array.
     */
    private static final Object[] EMPTY = new Object[0];

    /**
     * The feature from which to get type and property values.
     * This feature may contain a mix of single-valued and multi-valued properties.
     * Values of multi-valued properties are copied in the {@link #values} array.
     */
    private final AbstractFeature source;

    /**
     * Mapping from property names to index in the {@link #values} array.
     * <strong>Do not modify,</strong> because all {@link ExpandedFeature} instances created
     * after a call to {@link FeatureTable#setFeatureType(DefaultFeatureType)} share the same map.
     */
    private final Map<String,Integer> nameToIndex;

    /**
     * All values from the {@linkplain #source} feature for properties named in the {@link #nameToIndex} keys.
     * The {@code values[i]} elements may be empty arrays but never null. This {@code ExpandedFeature} instance
     * uses only the values at {@code values[…][index]}. Other {@code ExpandedFeature} instances will share the
     * same arrays but with different {@link #index} values.
     */
    private final Object[][] values;

    /**
     * Index of the element to extract in each {@link #values} sub-array.
     *
     * @see #getPropertyValue(String)
     */
    final int index;

    /**
     * Creates a new feature wrapping the given source.
     */
    private ExpandedFeature(final AbstractFeature source, final Map<String,Integer> nameToIndex,
                            final Object[][] values, final int index)
    {
        super(source.getType());
        this.source      = source;
        this.nameToIndex = nameToIndex;
        this.values      = values;
        this.index       = index;
    }

    /**
     * Creates the wrappers for all elements in the properties of the given feature.
     * If non-null, the returned array is guaranteed to have at least two elements.
     * See {@link ExpandableList#expansion} for an explanation about why we do not
     * allow empty arrays.
     *
     * @param  source       the "real" feature to wrap, or {@code null} if none.
     * @param  nameToIndex  mapping from property names to index in the {@code values} array.
     * @return pseudo-features for property elements at all indices, or {@code null} if none.
     */
    static ExpandedFeature[] create(final AbstractFeature source, final Map<String,Integer> nameToIndex) {
        if (source != null) {
            final Object[][] values = new Object[nameToIndex.size()][];
            Arrays.fill(values, EMPTY);
            int count = 0;
            for (final Map.Entry<String,Integer> entry : nameToIndex.entrySet()) {
                final Object value = source.getPropertyValue(entry.getKey());
                if (value != null) {                                    // Should not be null, but be paranoiac.
                    final int i = entry.getValue();
                    if (value instanceof Collection<?>) {
                        final Object[] elements = ((Collection<?>) value).toArray();
                        if (elements != null) {                         // Should not be null, but be paranoiac.
                            values[i] = elements;
                        }
                    } else {
                        values[i] = new Object[] {value};               // Should not happen, but be paranoiac.
                    }
                    final int n = values[i].length;
                    if (n > count) count = n;
                }
            }
            if (count > 1) {                                            // Required by this method contract.
                final ExpandedFeature[] features = new ExpandedFeature[count];
                for (int i=0; i<count; i++) {
                    features[i] = new ExpandedFeature(source, nameToIndex, values, i);
                }
                return features;
            }
        }
        return null;
    }

    /**
     * Returns the source feature type verbatim.
     */
    @Override
    public DefaultFeatureType getType() {
        return source.getType();
    }

    /**
     * Returns the property of the given name.
     * This method is not used by {@link FeatureTable} so we just delegate to the source.
     */
    @Override
    public Object getProperty(final String name) {
        return source.getProperty(name);
    }

    /**
     * Sets the property of the given name.
     * This method is not used by {@link FeatureTable} so we just forward to the source.
     */
    @Override
    public void setProperty(final Object property) {
        source.setProperty(property);
    }

    /**
     * Returns a single value for the property at the given name.
     * If the property is multi-valued, only the value at the index managed by this instance is returned.
     */
    @Override
    public Object getPropertyValue(final String name) {
        final Integer i = nameToIndex.get(name);
        if (i == null) {
            // Not a multi-valued property.
            return source.getPropertyValue(name);
        }
        final Object[] elements = values[i];
        return (index < elements.length)
               ? List.of(elements[index])
               : List.of();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public void setPropertyValue(String name, Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a hash code value for this feature.
     * Defined consistently with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return source.hashCode() + index;
    }

    /**
     * Compares this feature with the specified object for equality.
     * The comparison ignores {@link #nameToIndex} and {@link #values}
     * because they are derived from {@link #source} and {@link #index}.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ExpandedFeature) {
            final ExpandedFeature other = (ExpandedFeature) obj;
            return index == other.index && source.equals(other.source);
        }
        return false;
    }

    /**
     * Returns the feature string representation for debugging purpose.
     */
    @Override
    public String toString() {
        return source.toString();
    }
}
