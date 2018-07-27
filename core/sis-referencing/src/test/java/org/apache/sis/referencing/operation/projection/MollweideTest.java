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

import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import static org.junit.Assert.*;
import org.junit.Test;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Tests for Mollveide transform.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@DependsOn(NormalizedProjectionTest.class)
public class MollweideTest extends TestCase {

    private double tolerance = 0.00001;

    public MollweideTest() {
    }

    @Test
    public void testTransform() throws TransformException, ParameterNotFoundException, FactoryException {
        final org.apache.sis.internal.referencing.provider.Mollweide provider = new org.apache.sis.internal.referencing.provider.Mollweide();
        final Parameters parameters = Parameters.castOrWrap(provider.getParameters().createValue());
        parameters.parameter(Constants.CENTRAL_MERIDIAN).setValue(0.0);
        parameters.parameter(Constants.FALSE_EASTING).setValue(0.0);
        parameters.parameter(Constants.FALSE_NORTHING).setValue(0.0);
        parameters.parameter(Constants.SEMI_MAJOR).setValue(6378137);
        parameters.parameter(Constants.SEMI_MINOR).setValue(6378137);


        final MathTransform trs = provider.createMathTransform(DefaultFactories.forClass(MathTransformFactory.class), parameters);
        final MathTransform invtrs = trs.inverse();

        final double[] in = new double[2];
        final double[] out = new double[2];

        // at (0,0) point should be unchanged
        in[0] = 0.0;
        in[1] = 0.0;
        trs.transform(in, 0, out, 0, 1);
        assertEquals(0.0, out[0], tolerance);
        assertEquals(0.0, out[1], tolerance);

        // at (0,±90) north/south poles singularity
        in[0] = 0.0;
        in[1] = 90;
        trs.transform(in, 0, out, 0, 1);
        assertEquals(0.0, out[0], tolerance);
        assertEquals(9020047.848073645, out[1], tolerance);
        invtrs.transform(out, 0, in, 0, 1);
        assertEquals(0.0, in[0], tolerance);
        assertEquals(90.0, in[1], tolerance);

        in[0] = 0.0;
        in[1] = -90;
        trs.transform(in, 0, out, 0, 1);
        assertEquals(0.0, out[0], tolerance);
        assertEquals(-9020047.848073645, out[1], tolerance);
        invtrs.transform(out, 0, in, 0, 1);
        assertEquals(0.0, in[0], tolerance);
        assertEquals(-90.0, in[1], tolerance);

        // at (0,~90) point near north pole singularity should be close to ~9.000.000
        in[0] = 0.0;
        in[1] = 89;
        trs.transform(in, 0, out, 0, 1);
        assertEquals(0.0, out[0], tolerance);
        assertEquals(8997266.89915323, out[1], tolerance);
        invtrs.transform(out, 0, in, 0, 1);
        assertEquals(0.0, in[0], tolerance);
        assertEquals(89.0, in[1], tolerance);

        //other random points
        //compared to epsg.io (Bad reference, find something more trustable)
        //https://epsg.io/transform#s_srs=4326&t_srs=54009&x=-150.0000000&y=-70.0000000
        in[0] = 12.0;
        in[1] = 50.0;
        trs.transform(in, 0, out, 0, 1);
        assertEquals(912759.82345261, out[0], tolerance);
        assertEquals(5873471.95621065, out[1], tolerance);
        invtrs.transform(out, 0, in, 0, 1);
        assertEquals(12.0, in[0], tolerance);
        assertEquals(50.0, in[1], tolerance);


        in[0] = -150.0;
        in[1] = -70.0;
        trs.transform(in, 0, out, 0, 1);
        assertEquals(-7622861.35718471, out[0], tolerance);
        assertEquals(-7774469.60789149, out[1], tolerance);
        invtrs.transform(out, 0, in, 0, 1);
        assertEquals(-150.0, in[0], tolerance);
        assertEquals(-70.0, in[1], tolerance);

        in[0] = -179.9999;
        in[1] = 0.0;
        trs.transform(in, 0, out, 0, 1);
        assertEquals(-18040085.67387191, out[0], tolerance);
        assertEquals(0.0, out[1], tolerance);
        invtrs.transform(out, 0, in, 0, 1);
        assertEquals(-179.9999, in[0], tolerance);
        assertEquals(0.0, in[1], tolerance);

        //outside of validity area, should have NaN with the reverse transform
        in[0] = -180.0001;
        in[1] = 0.0;
        trs.transform(in, 0, out, 0, 1);
        assertEquals(-1.8040105718422677E7, out[0], tolerance);
        assertEquals(0.0, out[1], tolerance);
        invtrs.transform(out, 0, in, 0, 1);
        assertEquals(Double.NaN, in[0], tolerance);
        assertEquals(0.0, in[1], tolerance);


    }

}
