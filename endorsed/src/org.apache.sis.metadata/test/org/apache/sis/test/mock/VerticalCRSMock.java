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
package org.apache.sis.test.mock;

import javax.measure.Unit;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.apache.sis.measure.Units;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.util.Optional;
import org.opengis.referencing.datum.RealizationMethod;


/**
 * A dummy implementation of {@link VerticalCRS}, which is also its own datum, coordinate system and axis.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings({"serial", "deprecation"})
public final class VerticalCRSMock extends IdentifiedObjectMock
        implements VerticalCRS, VerticalDatum, VerticalCS, CoordinateSystemAxis
{
    /**
     * Height in metres.
     */
    public static final VerticalCRS HEIGHT = new VerticalCRSMock("Height",
            RealizationMethod.GEOID,
            VerticalDatumType.GEOIDAL, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Units.METRE, true);

    /**
     * Height in feet.
     */
    public static final VerticalCRS HEIGHT_ft = new VerticalCRSMock("Height",
            RealizationMethod.GEOID,
            VerticalDatumType.GEOIDAL, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Units.FOOT, true);

    /**
     * Height estimated from hPa.
     */
    public static final VerticalCRS BAROMETRIC_HEIGHT = new VerticalCRSMock("Barometric height",
            RealizationMethod.LEVELLING,
            VerticalDatumType.BAROMETRIC, 0, Double.POSITIVE_INFINITY, Units.HECTOPASCAL, true);

    /**
     * Depth in metres.
     */
    public static final VerticalCRS DEPTH = new VerticalCRSMock("Depth",
            RealizationMethod.TIDAL,
            VerticalDatumType.DEPTH, 0, Double.POSITIVE_INFINITY, Units.METRE, false);

    /**
     * Depth as a fraction of the sea floor depth at the location of the point for which the depth is evaluated.
     */
    public static final VerticalCRS SIGMA_LEVEL = new VerticalCRSMock("Sigma level",
            null,
            VerticalDatumType.OTHER_SURFACE, 0, 1, Units.UNITY, false);

    /**
     * The realization method (geoid, tidal, <i>etc.</i>), or {@code null} if unspecified.
     */
    private final RealizationMethod method;

    /**
     * The datum type (geoidal, barometric, etc.).
     */
    private final VerticalDatumType type;

    /**
     * The minimum and maximum values.
     */
    private final double minimumValue, maximumValue;

    /**
     * The unit of measurement.
     */
    private final Unit<?> unit;

    /**
     * {@code true} if the axis direction is up, or {@code false} if down.
     */
    private final boolean up;

    /**
     * Creates a new vertical CRS for the given name.
     *
     * @param name          the CRS, CS, datum and axis name.
     * @param method        the realization method (geoid, tidal, <i>etc.</i>).
     * @param minimumValue  the minium value.
     * @param maximumValue  the maximum value.
     * @param unit          the unit of measurement.
     * @param up            {@code true} if the axis direction is up, or {@code false} if down.
     */
    private VerticalCRSMock(final String name, final RealizationMethod method, VerticalDatumType type,
            final double minimumValue, final double maximumValue, final Unit<?> unit, final boolean up)
    {
        super(name);
        this.type         = type;
        this.method       = method;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
        this.unit         = unit;
        this.up           = up;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object[] properties() {
        return new Object[] {getCode(), alias, minimumValue, maximumValue, unit, up};
    }

    @Override public String                      getAbbreviation()      {return up ? "h" : "d";}
    @Override public Optional<RealizationMethod> getRealizationMethod() {return Optional.ofNullable(method);}
    @Override public VerticalDatumType           getVerticalDatumType() {return type;}
    @Override public VerticalDatum               getDatum()             {return this;}
    @Override public VerticalCS                  getCoordinateSystem()  {return this;}
    @Override public int                         getDimension()         {return 1;}
    @Override public CoordinateSystemAxis        getAxis(int dimension) {return this;}
    @Override public AxisDirection               getDirection()         {return up ? AxisDirection.UP : AxisDirection.DOWN;}
    @Override public double                      getMinimumValue()      {return minimumValue;}
    @Override public double                      getMaximumValue()      {return maximumValue;}
    @Override public RangeMeaning                getRangeMeaning()      {return RangeMeaning.EXACT;}
    @Override public Unit<?>                     getUnit()              {return unit;}
}
