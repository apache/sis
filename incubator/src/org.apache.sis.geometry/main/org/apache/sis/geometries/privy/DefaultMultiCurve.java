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
package org.apache.sis.geometries.privy;

import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.MultiCurve;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultMultiCurve<T extends Curve> extends AbstractGeometry implements MultiCurve<T> {

    private final T[] curves;

    public DefaultMultiCurve(T[] geometries) {
        this.curves = geometries;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return curves[0].getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        for (Curve c : curves) {
            c.setCoordinateReferenceSystem(cs);
        }
    }

    @Override
    public int getNumGeometries() {
        return curves.length;
    }

    @Override
    public T getGeometryN(int n) {
        return curves[n];
    }

}
