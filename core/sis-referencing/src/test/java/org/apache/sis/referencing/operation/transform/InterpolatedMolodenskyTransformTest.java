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

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.FranceGeocentricInterpolationTest;
import org.apache.sis.internal.referencing.Formulas;

// Test dependencies
import org.apache.sis.internal.referencing.provider.GeocentricTranslationTest;
import org.apache.sis.internal.referencing.provider.MolodenskyInterpolation;
import org.apache.sis.test.DependsOn;
import org.junit.Test;


/**
 * Tests {@link InterpolatedMolodenskyTransform}. The accuracy of using the Molodensky approximation
 * instead than the real geocentric translation is verified by the following tests:
 *
 * <ul>
 *   <li>{@link GeocentricTranslationTest#testFranceGeocentricInterpolationPoint()}</li>
 *   <li>{@link MolodenskyTransformTest#testFranceGeocentricInterpolationPoint()}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    MolodenskyTransformTest.class,
    GeocentricTranslationTest.class,
    InterpolatedGeocentricTransformTest.class
})
public final strictfp class InterpolatedMolodenskyTransformTest extends InterpolatedGeocentricTransformTest {
    /**
     * Creates an approximation of the <cite>"France geocentric interpolation"</cite> transform
     * using the Molodensky transform. This method relax slightly the tolerance threshold since
     * Molodensky transformations are approximation of translations in geocentric domain.
     *
     * @throws FactoryException if an error occurred while loading the grid.
     */
    @Override
    final void createGeodeticTransformation() throws FactoryException {
        createGeodeticTransformation(new MolodenskyInterpolation());
        tolerance = Formulas.ANGULAR_TOLERANCE;     // Relax tolerance threshold.
    }

    /**
     * Tests the Well Known Text (version 1) formatting.
     * The result is what we show to users, but may quite different than what SIS has in memory.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException should never happen.
     */
    @Test
    @Override
    public void testWKT() throws FactoryException, TransformException {
        createGeodeticTransformation();
        transform = transform.inverse();
        assertWktEqualsRegex("(?m)\\Q" +
                "PARAM_MT[“Molodensky interpolation”,\n" +
                "  PARAMETER[“dim”, 2],\n" +
                "  PARAMETER[“src_semi_major”, 6378137.0],\n" +
                "  PARAMETER[“src_semi_minor”, 6356752.314140356],\n" +
                "  PARAMETER[“tgt_semi_major”, 6378249.2],\n" +
                "  PARAMETER[“tgt_semi_minor”, 6356515.0],\n" +
                "  PARAMETER[“Geocentric translation file”, “\\E.*\\W\\Q" +
                             FranceGeocentricInterpolationTest.TEST_FILE + "”]]\\E");

        transform = transform.inverse();
        assertWktEqualsRegex("(?m)\\Q" +
                "PARAM_MT[“Molodensky inverse interpolation”,\n" +
                "  PARAMETER[“dim”, 2],\n" +
                "  PARAMETER[“src_semi_major”, 6378249.2],\n" +
                "  PARAMETER[“src_semi_minor”, 6356515.0],\n" +
                "  PARAMETER[“tgt_semi_major”, 6378137.0],\n" +
                "  PARAMETER[“tgt_semi_minor”, 6356752.314140356],\n" +
                "  PARAMETER[“Geocentric translation file”, “\\E.*\\W\\Q" +
                             FranceGeocentricInterpolationTest.TEST_FILE + "”]]\\E");
    }

    /**
     * Tests the internal Well Known Text formatting.
     * This WKT shows what SIS has in memory for debugging purpose.
     * This is normally not what we show to users.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException should never happen.
     */
    @Test
    @Override
    public void testInternalWKT() throws FactoryException, TransformException {
        createGeodeticTransformation();
        assertInternalWktEqualsRegex("(?m)\\Q" +
                "Concat_MT[\n" +
                "  Param_MT[“Affine parametric transformation”,\n" +
                "    Parameter[“A0”, 0.017453292519943295, Id[“EPSG”, 8623]],\n" +   // Degrees to radians conversion
                "    Parameter[“B1”, 0.017453292519943295, Id[“EPSG”, 8640]]],\n" +
                "  Param_MT[“Molodensky inverse interpolation (radians domain)”,\n" +
                "    Parameter[“src_semi_major”, 6378249.2],\n" +
                "    Parameter[“src_semi_minor”, 6356515.0],\n" +
                "    Parameter[“Semi-major axis length difference”, -112.2],\n" +
                "    Parameter[“Flattening difference”, -5.4738838833299144E-5],\n" +
                "    ParameterFile[“Geocentric translation file”, “\\E.*\\W\\Q" +
                                   FranceGeocentricInterpolationTest.TEST_FILE + "”, Id[“EPSG”, 8727],\n" +
                "      Remark[“\\E.*\\Q”]],\n" +
                "    Parameter[“dim”, 2]],\n" +
                "  Param_MT[“Affine parametric transformation”,\n" +
                "    Parameter[“A0”, 57.29577951308232, Id[“EPSG”, 8623]],\n" +      // Radians to degrees conversion
                "    Parameter[“B1”, 57.29577951308232, Id[“EPSG”, 8640]]]]\\E");
    }
}
