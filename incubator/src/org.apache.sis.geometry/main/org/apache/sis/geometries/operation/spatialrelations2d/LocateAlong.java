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
 * Returns a derived geometry collection value that matches the specified m coordinate value.
 * See Subclause 6.1.2.6 “Measures on Geometry” for more details.
 *
 * @see OGC Simple Feature Access 1.2.1 - 6.1.2.3 Methods for testing spatial relations between geometric objects
 * @author Johann Sorel (Geomatys)
 */
public final class LocateAlong extends Operation<LocateAlong> {

    public final double mValue;
    public Geometry result;

    public LocateAlong(Geometry geom, double mValue) {
        super(geom);
        this.mValue = mValue;
    }

}
