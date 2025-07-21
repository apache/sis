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
package org.apache.sis.geometries.operation.spatialedition;

import java.util.function.Consumer;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.operation.Operation;
import org.apache.sis.geometries.math.Tuple;


/**
 * Add a Z axis on the geometry and configure it's ordinates.
 * @author Johann Sorel (Geomatys)
 */
public final class To3D extends Operation<To3D> {

    public final CoordinateReferenceSystem crs3d;
    public final Consumer<Tuple> Zeditor;
    public Geometry result;

    /**
     *
     * @param geom geometry to transform, if it already has a 3D crs and given crs is null, geometry crs will be preserved.
     * @param crs3d the result crs in 3d, if null an ellipsoid height is assumed
     * @param zEdit called to configure the Z value on each position, if null, value 0.0 will be used
     */
    public To3D(Geometry geom, CoordinateReferenceSystem crs3d, Consumer<Tuple> zEdit) {
        super(geom);
        this.crs3d = crs3d;
        this.Zeditor = zEdit;
    }

}
