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

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.FactoryException;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.operation.Operation;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.referencing.CRS;


/**
 * Returns a geometric object that represents a transformed version of the geometry.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Transform extends Operation<Transform> {

    public final CoordinateReferenceSystem crs;
    public final MathTransform transform;
    public Geometry result;

    /**
     *
     * @param geom geometry to transform
     * @param crs target CRS, if null geometry crs is unchanged but transform will still be applied
     * @param transform transform to apply, if null, geometry crs to target crs will be used
     */
    public Transform(Geometry geom, CoordinateReferenceSystem crs, MathTransform transform) {
        super(geom);
        if (crs == null) {
            this.crs = geom.getCoordinateReferenceSystem();
            this.transform = transform;
        } else if (transform == null) {
            this.crs = crs;
            try {
                this.transform = CRS.findOperation(geom.getCoordinateReferenceSystem(), crs, null).getMathTransform();
            } catch (FactoryException ex) {
                throw new OperationException(ex.getMessage(), ex);
            }
        } else {
            this.crs = crs;
            this.transform = transform;
        }
    }

}
