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
package org.apache.sis.storage;

import org.opengis.geometry.Envelope;

// Branch-dependent imports
import org.opengis.filter.QueryExpression;


/**
 * Definition of filtering to apply for fetching a resource subset.
 * Filtering can be applied on {@link FeatureSet} or on {@link GridCoverageResource}.
 * A query contains at least two parts:
 *
 * <ul>
 *   <li><b>Selection</b> for choosing the feature instances to fetch.
 *     This is equivalent to choosing rows in a database table.</li>
 *   <li><b>Projection</b> (not to be confused with map projection) for choosing the
 *     {@linkplain org.apache.sis.feature.DefaultFeatureType#getProperty feature properties} or the
 *     {@linkplain org.apache.sis.coverage.SampleDimension coverage sample dimensions} to fetch.
 *     This is equivalent to choosing columns in a database table.</li>
 * </ul>
 *
 * Compared to the SQL language, {@code Query} contains the information in the {@code SELECT} and
 * {@code WHERE} clauses of a SQL statement. A {@code Query} typically contains filtering capabilities
 * and (sometime) simple attribute transformations. Well known query languages include SQL and CQL.
 *
 * <h2>Optional values</h2>
 * All aspects of this query are optional and initialized to "none".
 * Unless otherwise specified, all methods accept a null argument or can return a null value, which means "none".
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.1
 *
 * @see FeatureSet#subset(Query)
 * @see GridCoverageResource#subset(Query)
 *
 * @since 0.8
 * @module
 */
public abstract class Query implements QueryExpression {
    /**
     * Creates a new, initially empty, query.
     */
    protected Query() {
    }

    /**
     * Sets the approximate area of feature instances or pixels to include in the subset.
     * For feature set, the domain is materialized by a {@link org.opengis.filter.Filter}.
     * For grid coverage resource, the given envelope specifies the coverage domain.
     *
     * <p>The given envelope is approximate.
     * Features may test intersections using only bounding boxes instead of full geometries.
     * Coverages may expand the envelope to an integer amount of tiles.</p>
     *
     * @param  domain  the approximate area of interest, or {@code null} if none.
     *
     * @since 1.1
     */
    public abstract void setSelection(Envelope domain);

    /**
     * Sets the properties to retrieve by their names. For features, the arguments are names of
     * {@linkplain org.apache.sis.feature.DefaultFeatureType#getProperty feature properties}.
     * For coverages, the arguments are names of
     * {@linkplain org.apache.sis.coverage.BandedCoverage#getSampleDimensions() sample dimensions}.
     *
     * <p><b>Note:</b> in this context, the "projection" word come from relational database terminology.
     * It is unrelated to <cite>map projection</cite>.</p>
     *
     * @param  properties  properties to retrieve, or {@code null} to retrieve all properties.
     * @throws IllegalArgumentException if a property is duplicated.
     *
     * @since 1.1
     */
    public abstract void setProjection(String... properties);
}
