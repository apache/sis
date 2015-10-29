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
package org.apache.sis.internal.referencing.provider;

import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.test.ToleranceModifier;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.transform.CoordinateDomain;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static java.lang.StrictMath.toRadians;
import static org.junit.Assert.*;


/**
 * Tests {@link GeocentricTranslation} and {@link GeocentricTranslation3D}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    AffineTest.class,
    org.apache.sis.referencing.operation.transform.EllipsoidalToCartesianTransformTest.class
})
public final strictfp class GeocentricTranslationTest extends MathTransformTestCase {
    /**
     * Returns the sample point for a step in the example given by the EPSG guidance note.
     *
     * <blockquote><b>Source:</b>
     * §2.4.3.5 <cite>Three-parameter geocentric translations</cite> in
     * IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015
     * </blockquote>
     *
     * The example transforms a point in the North Sea from WGS84 to ED50.
     * The transform can be decomposed in 4 steps:
     * <pre>
     *   <b>Step 1:</b> Source point in WGS84:  53°48'33.820"N,  02°07'46.380"E,  73.00 metres.
     *   <b>Step 2:</b> Geocentric (X,Y,Z):      3771793.968,  140253.342,  5124304.349 metres.
     *   <b>Step 3:</b> (X,Y,Z) after shift:         +84.87,      +96.49,      +116.95  metres
     *   <b>Step 4:</b> Target point in ED50:   53°48'36.565"N,  02'07"51.477"E,  28.02 metres.
     * </pre>
     *
     * @param  step The step as a value from 1 to 4 inclusive.
     * @return The sample point at the given step.
     */
    public static double[] samplePoint(final int step) {
        switch (step) {
            case 1: return new double[] {
                         2 + ( 7 + 46.380/60)/60,   // λ: Longitude
                        53 + (48 + 33.820/60)/60,   // φ: Latitude
                        73.00                       // h: Height
                    };
            case 2: return new double[] {
                        3771793.968,                // X: Toward prime meridian
                         140253.342,                // Y: Toward 90° east
                        5124304.349                 // Z: Toward north pole
                    };
            case 3: return new double[] {
                        3771878.84,                 // X: Toward prime meridian
                         140349.83,                 // Y: Toward 90° east
                        5124421.30                  // Z: Toward north pole
                    };
            case 4: return new double[] {
                         2 + ( 7 + 51.477/60)/60,   // λ: Longitude
                        53 + (48 + 36.565/60)/60,   // φ: Latitude
                        28.02                       // h: Height
                    };
            default: throw new AssertionError(step);
        }
    }

    /**
     * Returns the sample point precision for the given step.
     *
     * @param  step The step as a value from 1 to 4 inclusive.
     * @return The sample point precision for the given step.
     */
    public static double precision(final int step) {
        switch (step) {
            case 1:
            case 4: return 0.001 / 60 / 60 / 2;     // Half the precision of (λ,φ) values given by EPSG
            case 2: return 0.001 / 2;               // Half the precision for (X,Y,Z) values given by EPSG
            case 3: return 0.01 / 2;                // Half the precision for (X,Y,Z) values given by EPSG
            default: throw new AssertionError(step);
        }
    }

    /**
     * Tests transformation of the sample point from WGS84 to ED50.
     *
     * @param method     The method to test.
     * @param sourceStep The {@link #samplePoint(int)} to use as the source coordinate.
     * @param targetStep The {@link #samplePoint(int)} expected as a result of the transformation.
     */
    private void datumShift(final GeocentricAffine method, final int sourceStep, final int targetStep)
            throws FactoryException, TransformException
    {
        final ParameterValueGroup values = method.getParameters().createValue();
        values.parameter("X-axis translation").setValue( 84.87);
        values.parameter("Y-axis translation").setValue( 96.49);
        values.parameter("Z-axis translation").setValue(116.95);
        if ((method.getType() & GeocentricAffine.GEOGRAPHIC) != 0) {
            setEllipsoids(values, CommonCRS.WGS84.ellipsoid(), CommonCRS.ED50.ellipsoid());
        }
        tolerance = precision(targetStep);
        transform = method.createMathTransform(DefaultFactories.forBuildin(MathTransformFactory.class), values);
        verifyTransform(samplePoint(sourceStep), samplePoint(targetStep));
    }

    /**
     * Sets the source and target ellipsoid axes in the given parameter value group.
     */
    private static void setEllipsoids(final ParameterValueGroup values, final Ellipsoid source, final Ellipsoid target) {
        values.parameter("src_semi_major").setValue(source.getSemiMajorAxis());
        values.parameter("src_semi_minor").setValue(source.getSemiMinorAxis());
        values.parameter("tgt_semi_major").setValue(target.getSemiMajorAxis());
        values.parameter("tgt_semi_minor").setValue(target.getSemiMinorAxis());
    }

    /**
     * Tests <cite>"Geocentric translations (geocentric domain)"</cite> (EPSG:1031).
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if transformation of a point failed.
     */
    @Test
    public void testGeocentricDomain() throws FactoryException, TransformException {
        derivativeDeltas = new double[] {100, 100, 100};    // In metres
        datumShift(new GeocentricTranslation(), 2, 3);
        assertTrue(transform instanceof LinearTransform);
        validate();
    }

    /**
     * Tests <cite>"Geocentric translations (geog3D domain)"</cite> (EPSG:1035).
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if transformation of a point failed.
     */
    @Test
    @DependsOnMethod("testGeocentricDomain")
    public void testGeographicDomain() throws FactoryException, TransformException {
        final double delta = toRadians(100.0 / 60) / 1852;      // Approximatively 100 metres
        derivativeDeltas = new double[] {delta, delta, 100};    // (Δλ, Δφ, Δh)
        zTolerance = Formulas.LINEAR_TOLERANCE / 2;             // Half the precision of h value given by EPSG
        zDimension = new int[] {2};                             // Dimension of h where to apply zTolerance
        datumShift(new GeocentricTranslation3D(), 1, 4);
        assertFalse(transform instanceof LinearTransform);
        validate();
    }

    /**
     * Tests conversion of random points.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if transformation of a point failed.
     */
    @Test
    @DependsOnMethod("testGeographicDomain")
    public void testRandomPoints() throws FactoryException, TransformException {
        testGeographicDomain();     // For creating the transform.
        tolerance = Formulas.LINEAR_TOLERANCE;
        toleranceModifier = ToleranceModifier.GEOGRAPHIC;
        verifyInDomain(CoordinateDomain.GEOGRAPHIC, 831342815);
    }

    /**
     * Tests Well Known Text formatting.
     * The main point of this test is to verify that the affine transform between the two
     * Geographic/Geocentric conversions have been replaced by Bursa-Wolf parameters for
     * formatting purpose.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if transformation of a sample point failed.
     */
    @Test
    @DependsOnMethod("testGeographicDomain")
    public void testWKT() throws FactoryException, TransformException {
        testGeographicDomain();     // For creating the transform.
        assertWktEquals("CONCAT_MT[PARAM_MT[“Ellipsoid_To_Geocentric”,\n" +
                        "    PARAMETER[“semi_major”, 6378137.0],\n" +
                        "    PARAMETER[“semi_minor”, 6356752.314245179]],\n" +
                        "  PARAM_MT[“Geocentric translations (geocentric domain)”,\n" +
                        "    PARAMETER[“dx”, 84.87],\n" +
                        "    PARAMETER[“dy”, 96.49],\n" +
                        "    PARAMETER[“dz”, 116.95]],\n" +
                        "  PARAM_MT[“Geocentric_To_Ellipsoid”,\n" +
                        "    PARAMETER[“semi_major”, 6378388.0],\n" +
                        "    PARAMETER[“semi_minor”, 6356911.9461279465]]]");
        /*
         * In memory, what we have between the two Geographic/Geocentric conversions
         * is an affine transform.
         */
        assertInternalWktEquals(
                "Concat_MT[Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_0_0”, 0.017453292519943295],\n" +
                "    Parameter[“elt_1_1”, 0.017453292519943295],\n" +
                "    Parameter[“elt_2_2”, 1.567855942887398E-7]],\n" +
                "  Param_MT[“Ellipsoidal to Cartesian”,\n" +
                "    Parameter[“excentricity”, 0.08181919084262157],\n" +
                "    Parameter[“dim”, 3]],\n" +
                "  Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_0_0”, 0.9999606483644456],\n" +
                "    Parameter[“elt_0_3”, 1.3305869758942228E-5],\n" +
                "    Parameter[“elt_1_1”, 0.9999606483644456],\n" +
                "    Parameter[“elt_1_3”, 1.512764667185502E-5],\n" +
                "    Parameter[“elt_2_2”, 0.9999606483644456],\n" +
                "    Parameter[“elt_2_3”, 1.8335353697517302E-5]],\n" +
                "  Param_MT[“Cartesian to ellipsoidal”,\n" +
                "    Parameter[“excentricity”, 0.08199188997902956],\n" +
                "    Parameter[“dim”, 3]],\n" +
                "  Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_0_0”, 57.29577951308232],\n" +
                "    Parameter[“elt_1_1”, 57.29577951308232],\n" +
                "    Parameter[“elt_2_2”, 6378388.0]]]");
    }
}
