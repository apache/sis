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

import org.opengis.filter.spatial.BBOX;

public class PostGISInterpreter extends ANSIInterpreter {

    /**
     * Filter encoding specifies bbox as a filter between envelopes. Default ANSI interpreter performs a standard
     * intersection between geometries, which is not compliant. PostGIS has its own bbox operator:
     * <a href="https://postgis.net/docs/geometry_overlaps.html">Geometry overlapping</a>.
     * @param filter BBox filter specifying properties to compare.
     * @param extraData A context to handle some corner cases. Not used. Can be null.
     * @return A text (sql query) representation of input filter.
     */
    @Override
    public CharSequence visit(BBOX filter, Object extraData) {
        if (filter.getExpression1() == null || filter.getExpression2() == null)
            throw new UnsupportedOperationException("Not supported yet : bbox over all geometric properties");
        return join(filter, "&&", extraData);
    }
}
