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
import jakarta.xml.bind.annotation.XmlTransient;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Length;
import org.opengis.referencing.datum.Ellipsoid;


/**
 * An ellipsoid which is spherical.
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Unless otherwise noted in the javadoc, this condition holds if
 * all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
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
     * @param properties     the properties to be given to the identified object.
     * @param radius         the equatorial and polar radius.
     * @param ivfDefinitive  {@code true} if the inverse flattening is definitive.
     * @param unit           the units of the radius value.
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
     * Returns a sphere of the same size as this ellipsoid but using the specified unit of measurement.
     */
    @Override
    public DefaultEllipsoid convertTo(final Unit<Length> target) {
        final UnitConverter c = getAxisUnit().getConverterTo(target);
        if (c.isIdentity()) {
            return this;
        }
        return new Sphere(properties(target), c.convert(getSemiMajorAxis()), isIvfDefinitive(), target);
    }
}
