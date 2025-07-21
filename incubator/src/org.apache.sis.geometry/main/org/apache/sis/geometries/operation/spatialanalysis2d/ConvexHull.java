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
package org.apache.sis.geometries.operation.spatialanalysis2d;

import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.operation.Operation;


/**
 * Returns a geometric object that represents the convex hull of this geometric object.
 * Convex hulls, being dependent on straight lines, can be accurately represented in linear interpolations
 * for any geometry restricted to linear interpolations.
 *
 * @see OGC Simple Feature Access 1.2.1 - 6.1.2.4 Methods that support spatial analysis
 * @author Johann Sorel (Geomatys)
 */
public final class ConvexHull extends Operation<ConvexHull> {

    public Geometry result;

    public ConvexHull(Geometry geom) {
        super(geom);
    }

}
