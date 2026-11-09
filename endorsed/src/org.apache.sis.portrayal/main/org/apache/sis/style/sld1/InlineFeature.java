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

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;


/**
 * Features embedded in the styled layer descriptor, to be styled by a {@link UserLayer}.
 * In a XML document, the features are encoded as one or many GML feature collections.
 * This class merges the members of all those collections into a single list.
 *
 * @author  Johann Sorel (Geomatys)
 */
public class InlineFeature extends LayerSource {
    /**
     * The embedded features, as a live list.
     *
     * @see #features()
     */
    private List<Feature> features;

    /**
     * Creates an initially empty list of inline features.
     */
    public InlineFeature() {
        features = new ArrayList<>();
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public InlineFeature(final InlineFeature source) {
        features = new ArrayList<>(source.features);
    }

    /**
     * Returns the features embedded in the styled layer descriptor.
     * The features may be instances of different feature types.
     *
     * <p>The returned collection is <em>live</em>:
     * changes in that collection are reflected into this object, and conversely.</p>
     *
     * @return the embedded features, as a live collection.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<Feature> features() {
        return features;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {features};
    }

    /**
     * Returns a clone of this object.
     * The list is copied, but the features themselves are not cloned.
     *
     * @return a clone of this element.
     */
    @Override
    public InlineFeature clone() {
        final var clone = (InlineFeature) super.clone();
        clone.features = new ArrayList<>(features);
        return clone;
    }
}
