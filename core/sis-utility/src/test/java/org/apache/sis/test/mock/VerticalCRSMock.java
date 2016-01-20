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

import java.util.Date;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.opengis.util.InternationalString;


/**
 * A dummy implementation of {@link VerticalCRS}, which is also its own datum, coordinate system and axis.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
@SuppressWarnings("serial")
public final strictfp class VerticalCRSMock extends IdentifiedObjectMock
        implements VerticalCRS, VerticalDatum, VerticalCS, CoordinateSystemAxis
{
    /**
     * Height in metres.
     */
    public static final VerticalCRS HEIGHT = new VerticalCRSMock("Height",
            VerticalDatumType.GEOIDAL, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, SI.METRE, true);

    /**
     * Height in feet.
     */
    public static final VerticalCRS HEIGHT_ft = new VerticalCRSMock("Height",
            VerticalDatumType.GEOIDAL, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, NonSI.FOOT, true);

    /**
     * Height estimated from hPa.
     */
    public static final VerticalCRS BAROMETRIC_HEIGHT = new VerticalCRSMock("Barometric height",
            VerticalDatumType.BAROMETRIC, 0, Double.POSITIVE_INFINITY, SI.MetricPrefix.HECTO(SI.PASCAL), true);

    /**
     * Depth in metres.
     */
    public static final VerticalCRS DEPTH = new VerticalCRSMock("Depth",
            VerticalDatumType.DEPTH, 0, Double.POSITIVE_INFINITY, SI.METRE, false);

    /**
     * Depth as a fraction of the sea floor depth at the location of the point for which the depth is evaluated.
     */
    public static final VerticalCRS SIGMA_LEVEL = new VerticalCRSMock("Sigma level",
            VerticalDatumType.OTHER_SURFACE, 0, 1, Unit.ONE, false);

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
     * @param name         The CRS, CS, datum and axis name.
     * @param up           {@code true} if the axis direction is up, or {@code false} if down.
     * @param unit         The unit of measurement.
     * @param minimumValue The minium value.
     * @param maximumValue The maximum value.
     */
    private VerticalCRSMock(final String name, VerticalDatumType type,
            final double minimumValue, final double maximumValue, final Unit<?> unit, final boolean up)
    {
        super(name);
        this.type         = type;
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

    @Override public String               getAbbreviation()      {return up ? "h" : "d";}
    @Override public InternationalString  getScope()             {return null;}
    @Override public InternationalString  getAnchorPoint()       {return null;}
    @Override public Date                 getRealizationEpoch()  {return null;}
    @Override public Extent               getDomainOfValidity()  {return null;}
    @Override public VerticalDatumType    getVerticalDatumType() {return type;}
    @Override public VerticalDatum        getDatum()             {return this;}
    @Override public VerticalCS           getCoordinateSystem()  {return this;}
    @Override public int                  getDimension()         {return 1;}
    @Override public CoordinateSystemAxis getAxis(int dimension) {return this;}
    @Override public AxisDirection        getDirection()         {return up ? AxisDirection.UP : AxisDirection.DOWN;}
    @Override public double               getMinimumValue()      {return minimumValue;}
    @Override public double               getMaximumValue()      {return maximumValue;}
    @Override public RangeMeaning         getRangeMeaning()      {return RangeMeaning.EXACT;}
    @Override public Unit<?>              getUnit()              {return unit;}
}
