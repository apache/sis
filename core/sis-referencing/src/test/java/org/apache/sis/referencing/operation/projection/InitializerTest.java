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
import org.junit.Test;

import static org.apache.sis.internal.referencing.provider.ObliqueStereographic.*;
import static org.opengis.test.Assert.*;
import org.opengis.test.TestCase;


/**
 * Test about computing method from
 * <a href = http://www.iogp.org/pubs/373-07-2.pdf> EPSG guide</a>
 *
 * @author Rémi Marechal (Geomatys).
 * @author Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public strictfp class InitializerTest extends TestCase{
    /**
     * Test computing of Radius of conformal Sphere.
     *
     * @see Initializer#radiusOfConformalSphere(double)
     */
    @Test
    public void testConformalSphereRadius() {

        final EnumMap<NormalizedProjection.ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(NormalizedProjection.ParameterRole.class);
        roles.put(NormalizedProjection.ParameterRole.CENTRAL_MERIDIAN, LONGITUDE_OF_ORIGIN);
        roles.put(NormalizedProjection.ParameterRole.SCALE_FACTOR,     SCALE_FACTOR);
        roles.put(NormalizedProjection.ParameterRole.FALSE_EASTING,    FALSE_EASTING);
        roles.put(NormalizedProjection.ParameterRole.FALSE_NORTHING,   FALSE_NORTHING);

        final OperationMethod op = new ObliqueStereographic();

        final ParameterValueGroup p = op.getParameters().createValue();

        //-- implicit names from OGC.
        p.parameter("semi_major").setValue(6377397.155);
        p.parameter("inverse_flattening").setValue(299.15281);

        //-- Name parameters from Epsg registry
        p.parameter("Latitude of natural origin").setValue(52.156160556);
        p.parameter("Longitude of natural origin").setValue(5.387638889);
        p.parameter("Scale factor at natural origin").setValue(0.9999079);
        p.parameter("False easting").setValue(155000.00);
        p.parameter("False northing").setValue(463000.00);

        final Initializer initializer = new Initializer(op, (Parameters) p, roles, (byte) 0);

        assertEquals("Conformal Sphere Radius", 6382644.571, 6377397.155 * initializer.radiusOfConformalSphere(Math.sin(Math.toRadians(52.156160556))), Formulas.LINEAR_TOLERANCE);
    }
}
