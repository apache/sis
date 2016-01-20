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

import java.util.Date;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.quantity.Length;
import org.opengis.util.InternationalString;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.apache.sis.test.mock.IdentifiedObjectMock;
import org.apache.sis.internal.metadata.ReferencingServices;


/**
 * A dummy implementation of {@link GeodeticDatum}, which is also its own ellipsoid.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@SuppressWarnings("serial")
public final strictfp class GeodeticDatumMock extends IdentifiedObjectMock implements GeodeticDatum, Ellipsoid {
    /**
     * The "World Geodetic System 1984" datum.
     */
    public static final GeodeticDatum WGS84 = new GeodeticDatumMock("WGS84", 6378137, 6356752.314245179, 298.257223563, true);

    /**
     * The "World Geodetic System 1972" datum.
     */
    public static final GeodeticDatum WGS72 = new GeodeticDatumMock("WGS72", 6378135, 6356750.520016094, 298.26, true);

    /**
     * The "North American Datum 1983" datum with "GRS 1980" ellipsoid.
     */
    public static final GeodeticDatum NAD83 = new GeodeticDatumMock("NAD83", 6378137, 6356752.314140356, 298.257222101, true);

    /**
     * The "North American Datum 1927" datum with "Clarke 1866" ellipsoid.
     */
    public static final GeodeticDatum NAD27 = new GeodeticDatumMock("NAD27", 6378206.4, 6356583.8, 294.97869821390583, false);

    /**
     * The "European Datum 1950" datum with "International 1924" ellipsoid.
     */
    public static final GeodeticDatum ED50 = new GeodeticDatumMock("ED50", 6378388, 6356912, 297, true);

    /**
     * The "Nouvelle Triangulation Française" (EPSG:6807) datum with "Clarke 1880 (IGN)" ellipsoid.
     * This is the same datum than "Nouvelle Triangulation Française (Paris)" (EPSG:6275) except
     * for the prime meridian, which is Greenwich instead of Paris.
     *
     * @since 0.5
     */
    public static final GeodeticDatum NTF = new GeodeticDatumMock("NTF", 6378249.2, 6356515, 293.4660212936269, false);

    /**
     * The sphere based on the GRS 1980 Authalic sphere.
     */
    public static final GeodeticDatum SPHERE = new GeodeticDatumMock("SPHERE", ReferencingServices.AUTHALIC_RADIUS,
            ReferencingServices.AUTHALIC_RADIUS, Double.POSITIVE_INFINITY, false);

    /**
     * The equatorial radius.
     */
    private final double semiMajorAxis;

    /**
     * The polar radius.
     */
    private final double semiMinorAxis;

    /**
     * The inverse of the flattening value, or {@link Double#POSITIVE_INFINITY} if the ellipsoid is a sphere.
     */
    private final double inverseFlattening;

    /**
     * {@code true} if {@link #inverseFlattening} is definitive.
     */
    private final boolean isIvfDefinitive;

    /**
     * Creates a new datum of the given name, semi-major and semi-minor axis length.
     */
    private GeodeticDatumMock(final String name, final double semiMajorAxis, final double semiMinorAxis,
            final double inverseFlattening, final boolean isIvfDefinitive)
    {
        super(name);
        this.semiMajorAxis     = semiMajorAxis;
        this.semiMinorAxis     = semiMinorAxis;
        this.inverseFlattening = inverseFlattening;
        this.isIvfDefinitive   = isIvfDefinitive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object[] properties() {
        return new Object[] {getCode(), alias, semiMajorAxis, semiMinorAxis, inverseFlattening, isIvfDefinitive};
    }

    @Override public PrimeMeridian        getPrimeMeridian()      {return PrimeMeridianMock.GREENWICH;}
    @Override public Ellipsoid            getEllipsoid()          {return this;}
    @Override public Unit<Length>         getAxisUnit()           {return SI.METRE;}
    @Override public double               getSemiMajorAxis()      {return semiMajorAxis;}
    @Override public double               getSemiMinorAxis()      {return semiMinorAxis;}
    @Override public double               getInverseFlattening()  {return inverseFlattening;}
    @Override public boolean              isSphere()              {return semiMajorAxis == semiMinorAxis;}
    @Override public boolean              isIvfDefinitive()       {return isIvfDefinitive;}
    @Override public InternationalString  getAnchorPoint()        {return null;}
    @Override public Date                 getRealizationEpoch()   {return null;}
    @Override public Extent               getDomainOfValidity()   {return null;}
    @Override public InternationalString  getScope()              {return null;}
}
