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
import org.opengis.referencing.crs.GeodeticCRS;
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
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.operation.DefaultConversion;
import static org.apache.sis.referencing.internal.shared.Formulas.ANGULAR_TOLERANCE;
import static org.apache.sis.referencing.internal.shared.Formulas.LINEAR_TOLERANCE;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.referencing.EPSGDependentTestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.HardCodedConversions;


/**
 * Tests envelope transformations using either {@link Envelopes} or {@link Shapes2D} transform methods.
 * This base class allows us to perform the same tests on both kinds of objects.
 * All tests performed by this class are two-dimensional.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 *
 * @param <G>  the type of geometric objects, either {@link GeneralEnvelope} or {@link java.awt.geom.Rectangle2D}.
 */
@SuppressWarnings("exports")
public abstract class TransformTestCase<G> extends EPSGDependentTestCase {
    /**
     * Creates an envelope or rectangle for the given CRS and coordinate values.
     */
    abstract G createFromExtremums(CoordinateReferenceSystem crs, double xmin, double ymin, double xmax, double ymax);

    /**
     * Transforms an envelope or rectangle using the given math transform.
     * This transformation cannot handle poles.
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
     * Asserts that the given envelope or rectangle is equal to the expected value.
     */
    abstract void assertGeometryEquals(G expected, G actual, double tolx, double toly);

    /**
     * Allows sub-classing in same package only.
     */
    TransformTestCase() {
    }

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
        final ProjectedCRS    targetCRS  = CommonCRS.WGS84.universal(10, -123.5);
        final GeodeticCRS     sourceCRS  = targetCRS.getBaseCRS();
        final Conversion      conversion = targetCRS.getConversionFromBase();
        final MathTransform2D transform  = (MathTransform2D) conversion.getMathTransform();
        /*
         * Transforms envelopes using MathTransform. Geographic coordinates are in (latitude, longitude) order.
         * Opportunistically check that the transform using a CoordinateOperation object produces the same result.
         */
        final G rectλφ = createFromExtremums(sourceCRS, -20, -126, 40, -120);
        final G rectXY = transform(targetCRS, transform, rectλφ);
        assertEquals(rectXY, transform(conversion, rectλφ), "Conversion should produce the same result.");
        /*
         * Expected values are determined empirically by projecting many points.
         * Those values are the same as in EnvelopesTest.testTransform().
         */
        final G expected = createFromExtremums(targetCRS, 166021.56, -2214294.03,
                                                          833978.44,  4432069.06);
        assertGeometryEquals(expected, rectXY, LINEAR_TOLERANCE, LINEAR_TOLERANCE);
        /*
         * Test the inverse conversion.
         * Final envelope should be slightly bigger than the original.
         */
        final G rectBack = transform(sourceCRS, transform.inverse(), rectXY);
        assertTrue(contains(rectBack, rectλφ), "Transformed envelope should not be smaller than the original one.");
        assertGeometryEquals(rectλφ, rectBack, 0.05, 1.0);
    }

    /**
     * Tests conversions of an envelope or rectangle over a pole using a coordinate operation.
     *
     * @throws FactoryException if an error occurred while creating the operation.
     * @throws TransformException if an error occurred while transforming the envelope.
     */
    @Test
    public final void testTransformOverPole() throws FactoryException, TransformException {
        final ProjectedCRS    sourceCRS  = HardCodedConversions.createCRS(HardCodedConversions.POLAR_STEREOGRAPHIC);
        final GeodeticCRS     targetCRS  = sourceCRS.getBaseCRS();
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
         * larger number of points.
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
    public final void testTransformNotOverPole() throws FactoryException, TransformException {
        final ProjectedCRS  sourceCRS  = CommonCRS.WGS84.universal(10, -3.5);
        final GeodeticCRS   targetCRS  = sourceCRS.getBaseCRS();
        final Conversion    conversion = inverse(sourceCRS.getConversionFromBase());
        final G rectangle = createFromExtremums(sourceCRS, 199980, 4490220, 309780, 4600020);
        final G expected  = createFromExtremums(targetCRS,
                40.50846282536367, -6.594124551832373,          // Computed by SIS (not validated by external authority).
                41.52923550023067, -5.246186118392847);
        final G actual = transform(conversion, rectangle);
        assertGeometryEquals(expected, actual, ANGULAR_TOLERANCE, ANGULAR_TOLERANCE);
    }

    /**
     * Tests transform of an envelope over the ±180° limit. The Mercator projection used in this test
     * is not expected to wrap the longitude around Earth when using only the {@code MathTransform}.
     * However, when the target CRS is known, then "wrap around" should be applied.
     *
     * @throws TransformException if an error occurred while transforming the envelope.
     */
    @Test
    public final void testTransformOverAntiMeridian() throws TransformException {
        final ProjectedCRS  sourceCRS  = HardCodedConversions.mercator();
        final GeodeticCRS   targetCRS  = sourceCRS.getBaseCRS();
        final Conversion    conversion = inverse(sourceCRS.getConversionFromBase());
        final G expected  = createFromExtremums(targetCRS, 179, 40, 181, 50);
        final G rectangle = createFromExtremums(sourceCRS,
                19926188.852, 4838471.398,                      // Computed by SIS (not validated by external authority).
                20148827.834, 6413524.594);
        final G actual = transform(conversion, rectangle);
        assertGeometryEquals(expected, actual, ANGULAR_TOLERANCE, ANGULAR_TOLERANCE);
    }

    /**
     * Tests conversion from a UTM projection to geographic CRS where the resulting envelope crosses the anti-meridian.
     * Contrarily to {@link #testTransformOverAntiMeridian()}, the longitude range is outside the [-180 … +180]° range.
     * This is because the projection has a large central meridian which is added to the result.
     *
     * @throws FactoryException if an error occurred while creating the operation.
     * @throws TransformException if an error occurred while transforming the envelope.
     */
    @Test
    public void testProjectionOutsideLongitudeRange() throws FactoryException, TransformException {
        final ProjectedCRS  sourceCRS  = HardCodedConversions.createCRS(HardCodedConversions.UTM);
        final GeodeticCRS   targetCRS  = sourceCRS.getBaseCRS();
        final Conversion    conversion = inverse(sourceCRS.getConversionFromBase());
        final G rectangle = createFromExtremums(sourceCRS,
                -402748, 7965673,                               // Computed by SIS (not validated by external authority).
                1312383, 9912935);

        // Longitude span anti-meridian (-214° to 45°).
        final G expected = createFromExtremums(targetCRS,
                -213.637, 70.141,
                 -44.959, 89.147);

        final G actual = transform(conversion, rectangle);
        assertGeometryEquals(expected, actual, 0.001, 0.001);
    }

    /**
     * Returns the inverse of the given conversion. This method is not strictly correct
     * since we reuse the properties (name, aliases, etc.) from the given conversion.
     * However, those properties are not significant for the purpose of this test.
     *
     * @see org.apache.sis.referencing.operation.CoordinateOperationRegistry#inverse(SingleOperation)
     */
    private static Conversion inverse(final Conversion conversion) throws NoninvertibleTransformException {
        return new DefaultConversion(IdentifiedObjects.getProperties(conversion, Conversion.IDENTIFIERS_KEY),
                conversion.getTargetCRS(), conversion.getSourceCRS(), null,
                conversion.getMethod(), conversion.getMathTransform().inverse());
    }

    /**
     * Tests a transformation where only the range of longitude axis is changed.
     *
     * @throws FactoryException if an error occurred while creating the operation.
     * @throws TransformException if an error occurred while transforming the envelope.
     */
    @Test
    public final void testAxisRangeChange() throws FactoryException, TransformException {
        final GeographicCRS sourceCRS = HardCodedCRS.WGS84;
        final GeographicCRS targetCRS = HardCodedCRS.WGS84.forConvention(AxesConvention.POSITIVE_RANGE);
        final G rectangle = createFromExtremums(sourceCRS, -178, -70, 165, 80);
        final G expected  = createFromExtremums(targetCRS,  182, -70, 165, 80);
        final G actual    = transform(CRS.findOperation(sourceCRS, targetCRS, null), rectangle);
        assertGeometryEquals(expected, actual, 0, 0);
    }
}
