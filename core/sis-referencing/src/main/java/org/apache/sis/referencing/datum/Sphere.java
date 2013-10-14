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
package org.apache.sis.referencing.datum;

import java.util.Map;
import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.sis.util.Immutable;

import static java.lang.Math.*;


/**
 * A ellipsoid which is spherical. This ellipsoid implements a faster
 * {@link #orthodromicDistance(double, double, double, double)} method.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
@Immutable
@XmlTransient
final class Sphere extends DefaultEllipsoid {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7867565381280669821L;

    /**
     * Creates a new sphere using the specified radius.
     *
     * @param properties    Set of properties. Should contains at least {@code "name"}.
     * @param radius        The equatorial and polar radius.
     * @param ivfDefinitive {@code true} if the inverse flattening is definitive.
     * @param unit          The units of the radius value.
     */
    protected Sphere(Map<String,?> properties, double radius, boolean ivfDefinitive, Unit<Length> unit) {
        super(properties, radius, radius, Double.POSITIVE_INFINITY, ivfDefinitive, unit);
    }

    /**
     * Returns {@code true} since this object is a sphere.
     */
    @Override
    public boolean isSphere() {
        return true;
    }

    /**
     * This ellipsoid is already a sphere, so returns its radius directly.
     */
    @Override
    public double getAuthalicRadius() {
        return getSemiMajorAxis();
    }

    /**
     * Eccentricity of a sphere is always zero.
     */
    @Override
    public double getEccentricity() {
        return 0;
    }

    /**
     * Returns the orthodromic distance between two geographic coordinates.
     * The orthodromic distance is the shortest distance between two points
     * on a sphere's surface. The orthodromic path is always on a great circle.
     *
     * @param  x1 Longitude of first point (in decimal degrees).
     * @param  y1 Latitude of first point (in decimal degrees).
     * @param  x2 Longitude of second point (in decimal degrees).
     * @param  y2 Latitude of second point (in decimal degrees).
     * @return The orthodromic distance (in the units of this ellipsoid's axis).
     */
    @Override
    public double orthodromicDistance(double x1, double y1, double x2, double y2) {
        /*
         * The calculation of orthodromic distance on an ellipsoidal surface is complex,
         * subject to rounding errors and has no solution near the poles. In some situation
         * we use a calculation based on a spherical shape of the earth.  A Fortran program
         * which calculates orthodromic distances on an ellipsoidal surface can be downloaded
         * from the NOAA site:
         *
         *            ftp://ftp.ngs.noaa.gov/pub/pcsoft/for_inv.3d/source/
         */
        y1 = toRadians(y1);
        y2 = toRadians(y2);
        final double dx = toRadians(abs(x2-x1) % 360);
        double rho = sin(y1)*sin(y2) + cos(y1)*cos(y2)*cos(dx);
        assert abs(rho) < 1.0000001 : rho;
        if (rho > +1) rho = +1; // Catch rounding error.
        if (rho < -1) rho = -1; // Catch rounding error.
        return acos(rho) * getSemiMajorAxis();
    }
}
