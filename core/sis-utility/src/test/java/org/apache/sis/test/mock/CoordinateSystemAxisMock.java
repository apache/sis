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

import javax.measure.unit.Unit;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.RangeMeaning;


/**
 * A dummy implementation of {@link CoordinateSystemAxis}.
 * Implements also {@link CoordinateSystem} for the purpose of tests which need some context.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@SuppressWarnings("serial")
public strictfp class CoordinateSystemAxisMock extends IdentifiedObjectMock
        implements CoordinateSystemAxis, CoordinateSystem
{
    /**
     * The axis abbreviation.
     */
    final String abbreviation;

    /**
     * Creates a new axis for the given name.
     *
     * @param name         The axis name.
     * @param abbreviation The axis abbreviation.
     */
    public CoordinateSystemAxisMock(final String name, final String abbreviation) {
        super(name);
        this.abbreviation = abbreviation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object[] properties() {
        return new Object[] {getCode(), alias, abbreviation};
    }

    @Override public String               getAbbreviation()      {return abbreviation;}
    @Override public int                  getDimension()         {return 1;}
    @Override public CoordinateSystemAxis getAxis(int dimension) {return this;}
    @Override public AxisDirection        getDirection()         {return null;}
    @Override public double               getMinimumValue()      {return Double.NEGATIVE_INFINITY;}
    @Override public double               getMaximumValue()      {return Double.POSITIVE_INFINITY;}
    @Override public RangeMeaning         getRangeMeaning()      {return RangeMeaning.EXACT;}
    @Override public Unit<?>              getUnit()              {return null;}
}
