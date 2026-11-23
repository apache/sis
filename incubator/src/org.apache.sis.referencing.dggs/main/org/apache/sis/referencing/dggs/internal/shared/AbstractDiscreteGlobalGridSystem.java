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
package org.apache.sis.referencing.dggs.internal.shared;

import javax.measure.Quantity;
import javax.measure.UnitConverter;
import javax.measure.quantity.Area;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.Ellipsoid;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridSystem;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractDiscreteGlobalGridSystem implements DiscreteGlobalGridSystem {

    protected final CoordinateReferenceSystem crs;

    /**
     * Surface of the celestial body, in square metres.
     */
    protected final Quantity<Area> surfaceArea;

    public AbstractDiscreteGlobalGridSystem(CoordinateReferenceSystem crs) {
        this.crs = crs;

        final GeographicCRS gcrs = (GeographicCRS) crs;
        final Ellipsoid ellipsoid = DatumOrEnsemble.asDatum(gcrs).getEllipsoid();
        final UnitConverter toMetre = ellipsoid.getAxisUnit().getConverterTo(Units.METRE);
        final double r = toMetre.convert((ellipsoid.getSemiMajorAxis() + ellipsoid.getSemiMinorAxis()) / 2);
        surfaceArea = Quantities.create(4.0 * Math.PI * r * r, Units.SQUARE_METRE);
    }

    @Override
    public final CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    @Override
    public final Quantity<Area> getCelestialBodySurface() {
        return surfaceArea;
    }

}
