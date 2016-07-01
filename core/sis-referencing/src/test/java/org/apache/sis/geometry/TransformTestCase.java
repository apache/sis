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
package org.apache.sis.geometry;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.internal.referencing.Formulas.ANGULAR_TOLERANCE;
import static org.apache.sis.internal.referencing.Formulas.LINEAR_TOLERANCE;
import static org.apache.sis.test.ReferencingAssert.*;


/**
 * Tests envelope transformations using either {@link Envelopes} or {@link Shapes2D} transform methods.
 * This base class allows us to perform the same tests on both kind of objects.
 * All tests performed by this class are two-dimensional.
 *
 * @param <G> the type of geometric objects, either {@link GeneralEnvelope} or {@link java.awt.geom.Rectangle2D}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@DependsOn(CurveExtremumTest.class)
public abstract strictfp class TransformTestCase<G> extends TestCase {
    /**
     * Creates an envelope or rectangle for the given CRS and coordinate values.
     */
    abstract G createFromExtremums(CoordinateReferenceSystem crs, double xmin, double ymin, double xmax, double ymax);

    /**
     * Transforms an envelope or rectangle using the given math transform.
     * This transformation can not handle poles.
     */
    abstract G transform(CoordinateReferenceSystem targetCRS, MathTransform2D transform, G envelope) throws TransformException;

    /**
     * Transforms an envelope or rectangle using the given operation.
     * This transformation can handle poles.
     */
    abstract G transform(CoordinateOperation operation, G envelope) throws TransformException;

    /**
     * Returns {@code true} if the outer envelope or rectangle contains the inner one.
     */
    abstract boolean contains(G outer, G inner);

    /**
     * Asserts that the given envelope or rectangle is equals to the expected value.
     */
    abstract void assertGeometryEquals(G expected, G actual, double tolx, double toly);

    /**
     * Tests the transformation of an envelope or rectangle. This is a relatively simple test case
     * working in the two-dimensional space only, with a coordinate operation of type "conversion"
     * (not a "transformation") and with no need to adjust for poles.
     *
     * @throws FactoryException if an error occurred while creating the operation.
     * @throws TransformException if an error occurred while transforming the envelope.
     */
    @Test
    public final void testTransform() throws FactoryException, TransformException {
        final ProjectedCRS    targetCRS  = CommonCRS.WGS84.UTM(10, -123.5);
        final GeographicCRS   sourceCRS  = targetCRS.getBaseCRS();
        final Conversion      conversion = targetCRS.getConversionFromBase();
        final MathTransform2D transform  = (MathTransform2D) conversion.getMathTransform();
        /*
         * Transforms envelopes using MathTransform. Geographic coordinates are in (latitude, longitude) order.
         * Opportunistically check that the transform using a CoordinateOperation object produces the same result.
         */
        final G rectλφ = createFromExtremums(sourceCRS, -20, -126, 40, -120);
        final G rectXY = transform(targetCRS, transform, rectλφ);
        assertEquals("Conversion should produce the same result.", rectXY, transform(conversion, rectλφ));
        /*
         * Expected values are determined empirically by projecting many points.
         * Those values are the same than in EnvelopesTest.testTransform().
         */
        final G expected = createFromExtremums(targetCRS, 166021.56, -2214294.03,
                                                          833978.44,  4432069.06);
        assertGeometryEquals(expected, rectXY, LINEAR_TOLERANCE, LINEAR_TOLERANCE);
        /*
         * Test the inverse conversion.
         * Final envelope should be slightly bigger than the original.
         */
        final G rectBack = transform(sourceCRS, transform.inverse(), rectXY);
        assertTrue("Transformed envelope should not be smaller than the original one.", contains(rectBack, rectλφ));
        assertGeometryEquals(rectλφ, rectBack, 0.05, 1.0);
    }

    /**
     * Tests conversions of an envelope or rectangle over a pole using a coordinate operation.
     *
     * @throws FactoryException if an error occurred while creating the operation.
     * @throws TransformException if an error occurred while transforming the envelope.
     */
    @Test
    @DependsOnMethod("testTransform")
    public final void testTransformOverPole() throws FactoryException, TransformException {
        final ProjectedCRS sourceCRS = (ProjectedCRS) CRS.fromWKT(
                "PROJCS[“WGS 84 / Antarctic Polar Stereographic”,\n" +
                "  GEOGCS[“WGS 84”,\n" +
                "    DATUM[“World Geodetic System 1984”,\n" +
                "      SPHEROID[“WGS 84”, 6378137.0, 298.257223563]],\n" +
                "    PRIMEM[“Greenwich”, 0.0],\n" +
                "    UNIT[“degree”, 0.017453292519943295]],\n" +
                "  PROJECTION[“Polar Stereographic (variant B)”],\n" +
                "  PARAMETER[“standard_parallel_1”, -71.0],\n" +
                "  UNIT[“m”, 1.0]]");
        final GeographicCRS   targetCRS  = sourceCRS.getBaseCRS();
        final Conversion      conversion = inverse(sourceCRS.getConversionFromBase());
        final MathTransform2D transform  = (MathTransform2D) conversion.getMathTransform();
        /*
         * The rectangle to test, which contains the South pole.
         */
        G rectangle = createFromExtremums(sourceCRS,
                -3943612.4042124213, -4078471.954436003,
                 3729092.5890516187,  4033483.085688618);
        /*
         * This is what we get without special handling of singularity point.
         * Note that is does not include the South pole as we would expect.
         * The commented out values are what we get by projecting an arbitrary
         * larger amount of points.
         */
        G expected = createFromExtremums(targetCRS,
            //  -178.4935231040927  -56.61747883535035          // empirical values
                -179.8650137390031, -88.99136583196396,         // anti-regression values
            //   178.8122742080059  -40.90577500420587]         // empirical values
                 137.9769431693009, -40.90577500420587);        // anti-regression values
        /*
         * Tests what we actually get. First, test using the method working on MathTransform.
         * Next, test again the same transform, but using the API on Envelope objects.
         */
        G actual = transform(targetCRS, transform, rectangle);
        assertGeometryEquals(expected, actual, ANGULAR_TOLERANCE, ANGULAR_TOLERANCE);
        /*
         * Using the transform(CoordinateOperation, …) method,
         * the singularity at South pole is taken in account.
         */
        expected = createFromExtremums(targetCRS, -180, -90, 180, -40.905775004205864);
        actual   = transform(conversion, rectangle);
        assertGeometryEquals(expected, actual, ANGULAR_TOLERANCE, ANGULAR_TOLERANCE);
        /*
         * Another rectangle containing the South pole, but this time the south
         * pole is almost in a corner of the rectangle
         */
        rectangle = createFromExtremums(sourceCRS, -4000000, -4000000, 300000, 30000);
        expected  = createFromExtremums(targetCRS, -180, -90, 180, -41.03163170198091);
        actual    = transform(conversion, rectangle);
        assertGeometryEquals(expected, actual, ANGULAR_TOLERANCE, ANGULAR_TOLERANCE);
        /*
         * Another rectangle with the South pole close to the border.
         * This test should execute the step #3 in the transform method code.
         */
        rectangle = createFromExtremums(sourceCRS, -2000000, -1000000, 200000, 2000000);
        expected  = createFromExtremums(targetCRS, -180, -90, 180, -64.3861643256928);
        actual    = transform(conversion, rectangle);
        assertGeometryEquals(expected, actual, ANGULAR_TOLERANCE, ANGULAR_TOLERANCE);
    }

    /**
     * Tests conversions of an envelope or rectangle which is <strong>not</strong> over a pole,
     * but was wrongly considered as over a pole before SIS-329 fix.
     *
     * @throws FactoryException if an error occurred while creating the operation.
     * @throws TransformException if an error occurred while transforming the envelope.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-329">SIS-329</a>
     */
    @Test
    @DependsOnMethod("testTransform")
    public final void testTransformNotOverPole() throws FactoryException, TransformException {
        final ProjectedCRS  sourceCRS  = CommonCRS.WGS84.UTM(10, -3.5);
        final GeographicCRS targetCRS  = sourceCRS.getBaseCRS();
        final Conversion    conversion = inverse(sourceCRS.getConversionFromBase());
        final G rectangle = createFromExtremums(sourceCRS, 199980, 4490220, 309780, 4600020);
        final G expected  = createFromExtremums(targetCRS,
                40.50846282536367, -6.594124551832373,          // Computed by SIS (not validated by external authority).
                41.52923550023067, -5.246186118392847);
        final G actual = transform(conversion, rectangle);
        assertGeometryEquals(expected, actual, ANGULAR_TOLERANCE, ANGULAR_TOLERANCE);
    }

    /**
     * Returns the inverse of the given conversion. This method is not strictly correct
     * since we reuse the properties (name, aliases, etc.) from the given conversion.
     * However those properties are not significant for the purpose of this test.
     */
    private static Conversion inverse(final Conversion conversion) throws NoninvertibleTransformException {
        return new DefaultConversion(IdentifiedObjects.getProperties(conversion), conversion.getTargetCRS(),
                conversion.getSourceCRS(), null, conversion.getMethod(), conversion.getMathTransform().inverse());
    }
}
