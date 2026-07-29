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

import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.Ellipsoid;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractDiscreteGlobalGridSystem implements DiscreteGlobalGridSystem {

    protected final CoordinateReferenceSystem crs;
    protected final double surfaceArea;

    public AbstractDiscreteGlobalGridSystem(CoordinateReferenceSystem crs) {
        this.crs = crs;

        final GeographicCRS gcrs = (GeographicCRS) crs;
        final Ellipsoid ellipsoid = DatumOrEnsemble.asDatum(gcrs).getEllipsoid();
        final double semiMajorAxis = ellipsoid.getSemiMajorAxis();
        final double semiMinorAxis = ellipsoid.getSemiMinorAxis();
        final double r = (semiMajorAxis + semiMinorAxis) / 2;
        surfaceArea = 4.0 * Math.PI * r * r;
    }

    @Override
    public final CoordinateReferenceSystem getCrs() {
        return crs;
    }

    @Override
    public final double getCelestialBodySurface() {
        return surfaceArea;
    }

}
