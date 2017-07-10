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
package org.apache.sis.storage.gdal;

import java.util.Date;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;

import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;

import org.apache.sis.measure.Units;
import org.apache.sis.internal.simple.SimpleIdentifier;

import static java.lang.Math.*;


/**
 * Wraps the <a href="http://proj.osgeo.org/">Proj4</a> {@code PJ} native data structure in a geodetic datum.
 * The PJ structure combines datum, ellipsoid and prime meridian information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class PJDatum extends PJ implements GeodeticDatum, PrimeMeridian, Ellipsoid {
    /**
     * Tolerance factor for comparison of floating point numbers.
     * Must be fine enough for distinguish {@code FOOT} from {@code FOOT_SURVEY_US}.
     */
    static final double EPS = 1E-7;

    /**
     * The datum or ellipsoid name, or {@code null} if none.
     */
    private final Identifier name;

    /**
     * The Proj4 parameters, formatted at construction time because often used.
     */
    private final String definition;

    /**
     * Creates a new {@code PJ} structure from the given Proj4 data.
     *
     * @param name        the datum identifier, or {@code null} for inferring it from the definition.
     * @param definition  the Proj4 definition string.
     */
    PJDatum(Identifier name, final String definition) throws IllegalArgumentException {
        super(definition);
        this.definition = super.getDefinition();
        if (name == null) {
            final String param = getParameter("+datum=");
            if (param != null) {
                name = new SimpleIdentifier(null, param, false);
            }
        }
        this.name = name;
    }

    /**
     * Creates the base CRS of the given projected CRS.
     */
    PJDatum(final PJDatum projected) throws IllegalArgumentException {
        super(projected);
        definition = super.getDefinition();
        name = projected.name;
    }

    /**
     * Returns the definition cached at construction time. This avoid the need to
     * recreate the definition from Proj.4 native definition at every method call.
     */
    @Override
    public String getDefinition() {
        return definition;
    }

    /**
     * Returns the name given at construction time, or {@code null} if none.
     * Note that this attribute is mandatory according ISO 19111, but this
     * simple Proj.4 wrapper is lenient about that.
     */
    @Override
    public Identifier getName() {
        return name;
    }

    /*
     * Various GeoAPI method having no direct mapping in the Proj4 library.
     */
    @Override public Collection<GenericName>  getAlias()            {return Collections.emptySet();}
    @Override public Set<Identifier>          getIdentifiers()      {return Collections.emptySet();}
    @Override public InternationalString      getScope()            {return null;}
    @Override public InternationalString      getRemarks()          {return null;}
    @Override public InternationalString      getAnchorPoint()      {return null;}
    @Override public Date                     getRealizationEpoch() {return null;}
    @Override public Extent                   getDomainOfValidity() {return null;}

    /**
     * Returns the ellipsoid associated with the geodetic datum.
     */
    @Override
    public Ellipsoid getEllipsoid() {
        return this;
    }

    /**
     * Returns the prime meridian associated with the geodetic datum.
     */
    @Override
    public PrimeMeridian getPrimeMeridian() {
        return this;
    }

    /**
     * Returns the ellipsoid axis unit, which is assumed metres in the case of the Proj4 library.
     */
    @Override
    public Unit<Length> getAxisUnit() {
        return Units.METRE;
    }

    /**
     * Returns the linear unit for the horizontal or the vertical axes.
     */
    public Unit<Length> getLinearUnit(final boolean vertical) {
        return Units.METRE.divide(getLinearUnitToMetre(vertical));
    }

    /**
     * Returns the units of the prime meridian.
     * All angular units are converted from radians to degrees in those wrappers.
     */
    @Override
    public Unit<Angle> getAngularUnit() {
        return Units.DEGREE;
    }

    /**
     * Returns {@code true} unconditionally since the inverse eccentricity squared in definitive
     * in the Proj4 library, and the eccentricity is directly related to the flattening.
     */
    @Override
    public boolean isIvfDefinitive() {
        return true;
    }

    /**
     * Returns the inverse flattening, computed from the eccentricity.
     */
    @Override
    public double getInverseFlattening() {
        return 1 / (1 - sqrt(1 - getEccentricitySquared()));
    }

    /**
     * Returns {@code true} if the ellipsoid is a sphere.
     */
    @Override
    public boolean isSphere() {
        return getEccentricitySquared() == 0;
    }

    /**
     * Returns the value of the given parameter, or {@code null} if none. The given parameter key
     * shall include the {@code '+'} prefix and {@code '='} suffix, for example {@code "+proj="}.
     *
     * @param  key  the parameter name.
     * @return the parameter value.
     */
    final String getParameter(final String key) {
        int i = definition.indexOf(key);
        if (i >= 0) {
            i += key.length();
            final int stop = definition.indexOf(' ', i);
            String value = (stop >= 0) ? definition.substring(i, stop) : definition.substring(i);
            if (!(value = value.trim()).isEmpty()) {
                return value;
            }
        }
        return null;
    }

    /**
     * Throws unconditionally an exception since there is no WKt formatting provided by the Proj4 library.
     */
    @Override
    public String toWKT() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
