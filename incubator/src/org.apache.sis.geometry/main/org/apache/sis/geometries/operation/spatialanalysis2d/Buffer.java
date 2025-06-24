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
 * Returns a geometric object that represents all Points whose distance from this geometric object is less than
 * or equal to distance. Calculations are in the spatial reference system of this geometric object. Because of the
 * limitations of linear interpolation, there will often be some relatively small error in this distance,
 * but it should be near the resolution of the coordinates used.
 *
 * @see OGC Simple Feature Access 1.2.1 - 6.1.2.4 Methods that support spatial analysis
 * @author Johann Sorel (Geomatys)
 */
public final class Buffer extends Operation<Buffer> {

    public double distance;
    public Geometry result;

    public Buffer(Geometry geom, double distance) {
        super(geom);
    }

}
