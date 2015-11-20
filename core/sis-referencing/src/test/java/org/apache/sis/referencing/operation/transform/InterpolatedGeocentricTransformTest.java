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
package org.apache.sis.referencing.operation.transform;

import java.net.URL;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.FranceGeocentricInterpolation;
import org.apache.sis.internal.referencing.provider.FranceGeocentricInterpolationTest;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests {@link InterpolatedGeocentricTransform}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(FranceGeocentricInterpolationTest.class)
public final strictfp class InterpolatedGeocentricTransformTest extends MathTransformTestCase {
    /**
     * Creates the <cite>"France geocentric interpolation"</cite> transform.
     *
     * @throws FactoryException if an error occurred while loading the grid.
     */
    private void create() throws FactoryException {
        final URL file = FranceGeocentricInterpolationTest.class.getResource("GR3DF97A.txt");
        assertNotNull("Test file \"GR3DF97A.txt\" not found.", file);
        final Ellipsoid source = HardCodedDatum.NTF.getEllipsoid();     // Clarke 1880 (IGN)
        final Ellipsoid target = CommonCRS.ETRS89.ellipsoid();          // GRS 1980 ellipsoid
        final FranceGeocentricInterpolation provider = new FranceGeocentricInterpolation();
        final ParameterValueGroup values = provider.getParameters().createValue();
        values.parameter("src_semi_major").setValue(source.getSemiMajorAxis());
        values.parameter("src_semi_minor").setValue(source.getSemiMinorAxis());
        values.parameter("tgt_semi_major").setValue(target.getSemiMajorAxis());
        values.parameter("tgt_semi_minor").setValue(target.getSemiMinorAxis());
        values.parameter("Geocentric translations file").setValue(file);    // Automatic conversion from URL to Path.
        transform = provider.createMathTransform(DefaultFactories.forBuildin(MathTransformFactory.class), values);
        tolerance = Formulas.ANGULAR_TOLERANCE;
    }

    /**
     * Tests transformation of sample point from RGF93 to NTF.
     * We call this transformation "forward" because it uses the grid values directly,
     * without doing first an approximation followed by an iteration.
     *
     * @throws FactoryException if an error occurred while loading the grid.
     * @throws TransformException if an error occurred while transforming the coordinate.
     */
    @Test
    public void testForwardTransform() throws FactoryException, TransformException {
        create();   // Create the inverse of the transform we are interrested in.
        transform = transform.inverse();
        isInverseTransformSupported = false;
        verifyTransform(FranceGeocentricInterpolationTest.samplePoint(1),
                        FranceGeocentricInterpolationTest.samplePoint(3));
        /*
         * Input:     2.424971108333333    48.84444583888889
         * Expected:  2.425671861111111    48.84451225
         * Actual:    2.4256718922236735   48.84451219111167
         */
    }

    /**
     * Tests transformation of sample point from NTF to RGF93.
     *
     * @throws FactoryException if an error occurred while loading the grid.
     * @throws TransformException if an error occurred while transforming the coordinate.
     */
    @Test
    @DependsOnMethod("testForwardTransform")
    public void testInverseTransform() throws FactoryException, TransformException {
        create();
        isInverseTransformSupported = false;
        verifyTransform(FranceGeocentricInterpolationTest.samplePoint(3),
                        FranceGeocentricInterpolationTest.samplePoint(1));
    }
}
