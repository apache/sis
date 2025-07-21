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
package org.apache.sis.geometries.operation.spatialrelations2d;

import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.operation.Operation;


/**
 * Returns TRUE if this geometric object is spatially related to anotherGeometry by testing for intersections between
 * the interior, boundary and exterior of the two geometric objects as specified by the values in the
 * intersectionPatternMatrix.
 * This returns FALSE if all the tested intersections are empty except exterior (this) intersect exterior (another).
 *
 * @see OGC Simple Feature Access 1.2.1 - 6.1.2.3 Methods for testing spatial relations between geometric objects
 * @author Johann Sorel (Geomatys)
 */
public final class Relate extends Operation.Binary<Relate> {

    public final int intersectionPatternMatrix;
    public boolean result;

    public Relate(Geometry geom1, Geometry geom2, int intersectionPatternMatrix) {
        super(geom1, geom2);
        this.intersectionPatternMatrix = intersectionPatternMatrix;
    }

}
