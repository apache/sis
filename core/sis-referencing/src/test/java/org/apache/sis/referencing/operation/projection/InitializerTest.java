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
package org.apache.sis.referencing.operation.projection;

import java.util.EnumMap;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.provider.ObliqueStereographic;
import org.apache.sis.parameter.Parameters;
import org.opengis.test.TestCase;
import org.junit.Test;

import static java.lang.StrictMath.*;
import static org.opengis.test.Assert.*;


/**
 * Tests the {@link Initializer} class.
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class InitializerTest extends TestCase{
    /**
     * Tests the {@link Initializer#radiusOfConformalSphere(double)} method.
     * This test computes the Radius of Conformal Sphere using the values given
     * by the <a href="http://www.iogp.org/pubs/373-07-2.pdf">EPSG guide</a> for
     * the <cite>Amersfoort / RD New</cite> projection (a Stereographic one).
     */
    @Test
    public void testRadiusOfConformalSphere() {
        final OperationMethod op = new ObliqueStereographic();
        final ParameterValueGroup p = op.getParameters().createValue();
        /*
         * Following parameters are not given explicitely by EPSG definitions since they are
         * usually inferred from the datum.  However in the particular case of this test, we
         * need to provide them. The names used below are either OGC names or SIS extensions.
         */
        p.parameter("semi_major").setValue(6377397.155);
        p.parameter("inverse_flattening").setValue(299.15281);
        /*
         * Following parameters are reproduced verbatim from EPSG registry and EPSG guide.
         */
        p.parameter("Latitude of natural origin").setValue(52.156160556);
        p.parameter("Longitude of natural origin").setValue(5.387638889);
        p.parameter("Scale factor at natural origin").setValue(0.9999079);
        p.parameter("False easting").setValue(155000.00);
        p.parameter("False northing").setValue(463000.00);
        /*
         * The following lines are a typical way to create an Initializer instance.
         * The EnumMap tells to the Initializer constructor which parameters to look for.
         * We construct this map here for testing purpose, but users normally do not have
         * to do that since this map is provided by the ObliqueStereographic class itself.
         */
        final EnumMap<NormalizedProjection.ParameterRole, ParameterDescriptor<Double>> roles =
                new EnumMap<NormalizedProjection.ParameterRole, ParameterDescriptor<Double>>(NormalizedProjection.ParameterRole.class);
        roles.put(NormalizedProjection.ParameterRole.CENTRAL_MERIDIAN, ObliqueStereographic.LONGITUDE_OF_ORIGIN);
        roles.put(NormalizedProjection.ParameterRole.SCALE_FACTOR,     ObliqueStereographic.SCALE_FACTOR);
        roles.put(NormalizedProjection.ParameterRole.FALSE_EASTING,    ObliqueStereographic.FALSE_EASTING);
        roles.put(NormalizedProjection.ParameterRole.FALSE_NORTHING,   ObliqueStereographic.FALSE_NORTHING);
        final Initializer initializer = new Initializer(op, (Parameters) p, roles, (byte) 0);
        /*
         * The following lines give an example of how Apache SIS projection constructors
         * use the Initializer class.
         */
        final double φ0 = toRadians(initializer.getAndStore(ObliqueStereographic.LATITUDE_OF_ORIGIN));
        assertTrue(φ0 > 0);
        assertEquals("Conformal Sphere Radius", 6382644.571, 6377397.155 *
                initializer.radiusOfConformalSphere(sin(φ0)), Formulas.LINEAR_TOLERANCE);
    }
}
