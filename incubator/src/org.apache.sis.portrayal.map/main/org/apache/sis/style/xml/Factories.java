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
package org.apache.sis.style.xml;

import org.apache.sis.coverage.BandedCoverage;
import org.apache.sis.style.se1.CoverageStyle;
import org.apache.sis.style.se1.FeatureTypeStyle;
import org.apache.sis.style.se1.StyleFactory;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.filter.FilterFactory;


/**
 * The factories to use for creating the style elements, filters and expressions applied on resources of type {@code R}.
 *
 * @author  Johann Sorel (Geomatys)
 *
 * @param <R>     the type of resources to style, such as {@code Feature} or {@code BandedCoverage}.
 * @param style   the factory of style elements.
 * @param filter  the factory of filters and expressions.
 */
record Factories<R>(StyleFactory<R> style, FilterFactory<R,Object,Object> filter) {
    /**
     * Returns the factories for styling features.
     *
     * @param  filter  the factory of filters and expressions operating on features.
     * @return the factories for styling features.
     */
    static Factories<Feature> forFeatures(final FilterFactory<Feature,Object,Object> filter) {
        return new Factories<>(FeatureTypeStyle.FACTORY, filter);
    }

    /**
     * Returns the factories for styling coverages.
     * Apache SIS does not yet provide a filter factory operating on coverages.
     * As a workaround, the factory for features is used. This is okay for literals,
     * which are independent of the resource type, and for the functions.
     *
     * @param  filter  the factory of filters and expressions operating on features.
     * @return the factories for styling coverages.
     */
    @SuppressWarnings("unchecked")
    static Factories<BandedCoverage> forCoverages(final FilterFactory<Feature,Object,Object> filter) {
        return new Factories<>(CoverageStyle.FACTORY, (FilterFactory<BandedCoverage,Object,Object>) (FilterFactory<?,?,?>) filter);
    }
}
