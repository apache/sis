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

import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Angle;
import org.opengis.referencing.datum.PrimeMeridian;
import org.apache.sis.test.mock.IdentifiedObjectMock;


/**
 * A dummy implementation of {@link PrimeMeridian}, which is also its own identifier.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@SuppressWarnings("serial")
final strictfp class PrimeMeridianMock extends IdentifiedObjectMock implements PrimeMeridian {
    /**
     * A mock for the Greenwich prime meridian.
     */
    public static final PrimeMeridian GREENWICH = new PrimeMeridianMock("Greenwich");

    /**
     * Creates a new prime meridian of the given name.
     */
    private PrimeMeridianMock(final String name) {
        super(name);
    }

    /**
     * Returns the longitude of this prime meridian relative to Greenwich.
     *
     * @return The prime meridian longitude.
     */
    @Override
    public double getGreenwichLongitude() {
        return 0;
    }

    /**
     * Returns the angular unit, which is fixed to {@link NonSI#DEGREE_ANGLE}.
     *
     * @return {@link NonSI#DEGREE_ANGLE}.
     */
    @Override
    public Unit<Angle> getAngularUnit() {
        return NonSI.DEGREE_ANGLE;
    }
}
