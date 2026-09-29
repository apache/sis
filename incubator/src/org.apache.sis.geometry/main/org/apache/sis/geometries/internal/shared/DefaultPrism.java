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
package org.apache.sis.geometries.internal.shared;

import java.util.Objects;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.Prism;
import org.apache.sis.measure.NumberRange;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;


/**
 * A base shape extruded from an optional lower limit to an upper limit.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultPrism extends AbstractGeometry implements Prism {

    /**
     * Base shape which is extruded.
     */
    protected final Geometry base;

    /**
     * Lower and upper limits of the extrusion.
     */
    protected final NumberRange<?> extrusionRange;

    /**
     * Coordinate reference system in which the extrusion range is expressed.
     */
    protected final SingleCRS extrusionCrs;

    /**
     * Creates a prism by extruding the given base shape.
     *
     * @param  base            base shape of the prism, for example a polygon or a circle.
     * @param  extrusionRange  lower and upper limits of the extrusion.
     * @param  extrusionCrs    coordinate reference system of the extrusion range.
     */
    public DefaultPrism(final Geometry base, final NumberRange<?> extrusionRange, final SingleCRS extrusionCrs) {
        this.base           = Objects.requireNonNull(base);
        this.extrusionRange = Objects.requireNonNull(extrusionRange);
        this.extrusionCrs   = extrusionCrs;
    }

    @Override
    public Geometry getBase() {
        return base;
    }

    @Override
    public NumberRange<?> getExtrusionRange() {
        return extrusionRange;
    }

    @Override
    public SingleCRS getExtrusionCrs() {
        return extrusionCrs;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return base.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException {
        base.setCoordinateReferenceSystem(crs);
    }

    @Override
    public boolean isEmpty() {
        return base.isEmpty();
    }

    @Override
    public Envelope getEnvelope() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
