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
import org.opengis.referencing.datum.Ellipsoid;

import static java.lang.Math.*;


/**
 * A ellipsoid which is spherical. This ellipsoid implements a faster
 * {@link #orthodromicDistance(double, double, double, double)} method.
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Unless otherwise noted in the javadoc, this condition holds if
 * all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@XmlTransient
final class Sphere extends DefaultEllipsoid {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7867565381280669821L;

    /**
     * Creates a new sphere using the specified radius.
     *
     * @param properties    The properties to be given to the identified object.
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
     * Eccentricity of a sphere is always zero.
     */
    @Override
    public double getEccentricitySquared() {
        return 0;
    }

    /**
     * Returns the flattening factor of the other ellipsoid, since the flattening factor of {@code this} is zero.
     */
    @Override
    public double flatteningDifference(final Ellipsoid other) {
        return 1 / other.getInverseFlattening();
    }

    /**
     * Returns the orthodromic distance between two geographic coordinates.
     * The orthodromic distance is the shortest distance between two points
     * on a sphere's surface. The orthodromic path is always on a great circle.
     *
     * @param  λ1 Longitude of first point (in decimal degrees).
     * @param  φ1 Latitude of first point (in decimal degrees).
     * @param  λ2 Longitude of second point (in decimal degrees).
     * @param  φ2 Latitude of second point (in decimal degrees).
     * @return The orthodromic distance (in the units of this ellipsoid's axis).
     */
    @Override
    public double orthodromicDistance(double λ1, double φ1, double λ2, double φ2) {
        φ1 = toRadians(φ1);
        φ2 = toRadians(φ2);
        final double dx = toRadians(abs(λ2-λ1) % 360);
        double rho = sin(φ1)*sin(φ2) + cos(φ1)*cos(φ2)*cos(dx);
        assert abs(rho) < 1.0000001 : rho;
        if (rho > +1) rho = +1; // Catch rounding error.
        if (rho < -1) rho = -1; // Catch rounding error.
        return acos(rho) * getSemiMajorAxis();
    }
}
