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
package org.apache.sis.internal.sql.feature;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.SpatialOperator;
import org.opengis.filter.SpatialOperatorName;


/**
 * Use PostGIS-specific syntax in SQL statements where appropriate.
 *
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class PostGISInterpreter extends ANSIInterpreter {
    /**
     * Creates a new instance.
     */
    PostGISInterpreter() {
    }

    /**
     * Appends the SQL fragment to use for {@link SpatialOperatorName#BBOX} type of filter.
     * The filter encoding specification defines BBOX as a filter between envelopes.
     * The default ANSI interpreter performs a standard intersection between geometries,
     * which is not compliant. PostGIS has its own BBOX operator.
     *
     * @see <a href="https://postgis.net/docs/geometry_overlaps.html">Geometry overlapping</a>
     */
    @Override
    void bbox(final StringBuilder sb, final SpatialOperator<Feature> filter) {
        join(sb, filter, "&&");
    }
}
