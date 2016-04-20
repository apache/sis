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
package org.apache.sis.referencing.operation;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.parameter.DefaultParameterDescriptorTest;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests {@link DefaultConversion}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn({
    DefaultTransformationTest.class     // Because similar to DefaultConversion but simpler.
})
public final strictfp class DefaultConversionTest extends TestCase {
    /**
     * The rotation from a CRS using the Paris prime meridian to a CRS using the Greenwich prime meridian,
     * in degrees. The definitive value is 2.5969213 grades.
     */
    private static final double OFFSET = 2.33722917;

    /**
     * Creates a CRS using the same ellipsoid than the "Nouvelle Triangulation Française (Paris)" datum (EPSG:6807),
     * but with the prime meridian optionally set to Greenwich. Such CRS is not in real usage, but this is convenient
     * for testing a conversion consisting of only a longitude rotation, which is a very simple operation easy to test.
     *
     * @param isSource {@code true} if creating the source CRS, or {@code false} if creating the target CRS.
     * @param cs {@link HardCodedCS#GEODETIC_2D}, {@link HardCodedCS#GEODETIC_φλ} or other compatible coordinate system.
     * @param useGreenwich {@code true} for using Greenwich prime meridian, or {@code false} for staying on the Paris one.
     *
     * @see HardCodedCRS#NTF
     */
    private static GeographicCRS createParisCRS(final boolean isSource, final EllipsoidalCS cs, final boolean useGreenwich) {
        DefaultGeodeticDatum datum = HardCodedDatum.NTF;
        if (useGreenwich) {
            datum = new DefaultGeodeticDatum(Collections.singletonMap(DefaultGeodeticDatum.NAME_KEY, datum.getName()),
                    datum.getEllipsoid(), HardCodedDatum.GREENWICH);
        }
        return new DefaultGeographicCRS(Collections.singletonMap(GeographicCRS.NAME_KEY,
                isSource ? HardCodedCRS.NTF.getName() : "Back to Greenwich"), datum, cs);
    }

    /**
     * Changes only the coordinate system of the given CRS, which is supposed geographic.
     */
    private static GeographicCRS changeCS(final CoordinateReferenceSystem crs, final EllipsoidalCS cs) {
        return new DefaultGeographicCRS(Collections.singletonMap(DefaultGeographicCRS.NAME_KEY,
                crs.getName()), ((GeodeticCRS) crs).getDatum(), cs);
    }

    /**
     * Creates a pseudo-conversion performing a longitude rotation between two-dimensional normalized CRS.
     * The source CRS uses the Paris prime meridian and the target CRS "conceptually" uses the Greenwich
     * prime meridian.
     *
     * <p><b>This is not really a valid conversion</b> since, strictly speaking, <cite>Longitude rotations</cite>
     * are coordinate <em>transformations</em> rather than conversions (because they change the datum, since they
     * change the prime meridian). However we handle them as conversions for testing purpose only, because the
     * longitude rotation is a very simple operation easy to test.</p>
     *
     * @param  useGreenwich {@code true} for using Greenwich prime meridian in the {@code targetCRS},
     *         or {@code false} for staying on the Paris one.
     * @return A pseudo-conversion performing a longitude rotation.
     */
    public static DefaultConversion createLongitudeRotation(final boolean useGreenwich) {
        return createLongitudeRotation(HardCodedCRS.NTF_NORMALIZED_AXES,
                createParisCRS(false, HardCodedCS.GEODETIC_2D, useGreenwich), null);
    }

    /**
     * Creates a very simple conversion performing a longitude rotation.
     * The source CRS shall use the Paris prime meridian and the target CRS the Greenwich prime meridian,
     * at least conceptually. See {@link #createLongitudeRotation(boolean)} for an explanation about why
     * this is not really a valid conversion.
     *
     * @param sourceCRS A CRS using the Paris prime meridian.
     * @param targetCRS A CRS using the Greenwich prime meridian.
     * @param interpolationCRS A dummy interpolation CRS, or {@code null} if none.
     */
    private static DefaultConversion createLongitudeRotation(final GeographicCRS sourceCRS,
            final GeographicCRS targetCRS, final TemporalCRS interpolationCRS)
    {
        /*
         * The following code fills the parameter values AND creates itself the MathTransform instance
         * (indirectly, through the matrix). The later step is normally not our business, since we are
         * supposed to only fill the parameter values and let MathTransformFactory creates the transform
         * from the parameters. But we don't do the normal steps here because this class is a unit test:
         * we want to test DefaultConversion in isolation of MathTransformFactory.
         */
        final int interpDim = ReferencingUtilities.getDimension(interpolationCRS);
        final int sourceDim = sourceCRS.getCoordinateSystem().getDimension();
        final int targetDim = targetCRS.getCoordinateSystem().getDimension();
        final OperationMethod method = DefaultOperationMethodTest.create(
                "Longitude rotation", "9601", "EPSG guidance note #7-2", sourceDim,
                DefaultParameterDescriptorTest.createEPSG("Longitude offset", (short) 8602));
        final ParameterValueGroup pg = method.getParameters().createValue();
        pg.parameter("Longitude offset").setValue(OFFSET);
        final Matrix rotation = Matrices.createDiagonal(
                targetDim + interpDim + 1,      // Number of rows.
                sourceDim + interpDim + 1);     // Number of columns.
        rotation.setElement(interpDim, interpDim + sourceDim, OFFSET);
        /*
         * In theory we should not need to provide the parameters explicitly to the constructor since
         * we are supposed to be able to find them from the MathTransform. But in this simple test we
         * did not bothered to define a specialized MathTransform class for our case. So we will help
         * a little bit DefaultConversion by telling it the parameters that we used.
         */
        final Map<String,Object> properties = new HashMap<String,Object>(4);
        properties.put(DefaultTransformation.NAME_KEY, "Paris to Greenwich");
        properties.put(ReferencingServices.PARAMETERS_KEY, pg);
        return new DefaultConversion(properties, sourceCRS, targetCRS, interpolationCRS,
                method, MathTransforms.linear(rotation));
    }

    /**
     * Asserts that at least some of the properties of the given {@code op} instance have the expected values
     * for an instance created by {@link #createLongitudeRotation(GeographicCRS, GeographicCRS)}.
     */
    @SuppressWarnings("SuspiciousToArrayCall")
    private static void verifyProperties(final DefaultConversion op, final boolean swapSourceAxes) {
        assertEquals("name",       "Paris to Greenwich", op.getName().getCode());
        assertEquals("sourceCRS",  "NTF (Paris)",        op.getSourceCRS().getName().getCode());
        assertEquals("targetCRS",  "Back to Greenwich",  op.getTargetCRS().getName().getCode());
        assertEquals("method",     "Longitude rotation", op.getMethod().getName().getCode());
        assertEquals("parameters", "Longitude rotation", op.getParameterDescriptors().getName().getCode());

        final ParameterValueGroup parameters = op.getParameterValues();
        final ParameterValue<?>[] values = parameters.values().toArray(new ParameterValue<?>[1]);
        assertEquals("parameters",    "Longitude rotation", parameters.getDescriptor().getName().getCode());
        assertEquals("parameters[0]", "Longitude offset",    values[0].getDescriptor().getName().getCode());
        assertEquals("parameters[0]", OFFSET, values[0].doubleValue(), STRICT);
        assertEquals(1, values.length);

        final Matrix3 expected = new Matrix3();
        expected.m02 = OFFSET;
        if (swapSourceAxes) {
            expected.m00 = expected.m11 = 0;
            expected.m01 = expected.m10 = 1;
        }
        assertMatrixEquals("Longitude rotation of a two-dimensional CRS", expected,
                MathTransforms.getMatrix(op.getMathTransform()), STRICT);
    }

    /**
     * Tests a simple two-dimensional conversion performing a longitude rotation.
     */
    @Test
    public void testConstruction() {
        /*
         * Test construction of a valid conversion.
         * This conversion use the same datum for the source and target CRS.
         */
        verifyProperties(createLongitudeRotation(false), false);
        /*
         * Test a conversion with a source and target CRS using different datum.
         * This is a violation of conversion definition, but SIS is tolerant to
         * such violation. See DefaultConversion constructor javadoc for discussion.
         */
        verifyProperties(createLongitudeRotation(true), false);
    }

    /**
     * Creates a defining conversion and tests {@link DefaultConversion#specialize DefaultConversion.specialize(…)}.
     * This test includes a swapping of axis order in the <em>source</em> CRS.
     *
     * <div class="note"><b>Note:</b>
     * By contrast, {@link #testSpecialize()} will test swapping axis order in the <em>target</em> CRS.</div>
     *
     * @throws FactoryException Should not happen in this test.
     */
    @Test
    @DependsOnMethod("testConstruction")
    public void testDefiningConversion() throws FactoryException {
        final DefaultConversion reference = createLongitudeRotation(true);
        final DefaultConversion definingConversion = new DefaultConversion(
                IdentifiedObjects.getProperties(reference),
                reference.getMethod(),
                reference.getMathTransform(),
                reference.getParameterValues());
        /*
         * By definition, defining conversions have no source and target CRS.
         * This make them different from "normal" conversions.
         */
        assertNull("sourceCRS", definingConversion.getSourceCRS());
        assertNull("targetCRS", definingConversion.getTargetCRS());
        assertFalse(definingConversion.equals(reference));
        assertFalse(reference.equals(definingConversion));
        /*
         * Now create a normal conversion from the defining one,
         * but add a swapping of (latitude, longitude) axes.
         */
        final DefaultConversion completed = definingConversion.specialize(
                DefaultConversion.class,    // In normal use, this would be 'Conversion.class'.
                changeCS(reference.getSourceCRS(), HardCodedCS.GEODETIC_φλ),
                reference.getTargetCRS(),
                DefaultFactories.forBuildin(MathTransformFactory.class));

        verifyProperties(completed, true);
    }

    /**
     * Tests {@link DefaultConversion#specialize DefaultConversion.specialize(…)} with new source and target CRS.
     * This test attempts to swap axis order and change the number of dimensions of the <em>target</em> CRS.
     *
     * <div class="note"><b>Note:</b>
     * By contrast, {@link #testDefiningConversion()} tested swapping axis order in the <em>source</em> CRS.</div>
     *
     * @throws FactoryException Should not happen in this test.
     */
    @Test
    @DependsOnMethod("testDefiningConversion")
    public void testSpecialize() throws FactoryException {
        final MathTransformFactory factory = DefaultFactories.forBuildin(MathTransformFactory.class);
        DefaultConversion op = createLongitudeRotation(
                createParisCRS(true,  HardCodedCS.GEODETIC_3D, false),
                createParisCRS(false, HardCodedCS.GEODETIC_3D, true), null);
        assertMatrixEquals("Longitude rotation of a three-dimensional CRS", new Matrix4(
                1, 0, 0, OFFSET,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1), MathTransforms.getMatrix(op.getMathTransform()), STRICT);
        /*
         * When asking for a "specialization" with the same properties,
         * we should get the existing instance since no change is needed.
         */
        assertSame(op, op.specialize(Conversion.class, op.getSourceCRS(), op.getTargetCRS(), factory));
        /*
         * Reducing the number of dimensions to 2 and swapping (latitude, longitude) axes.
         */
        op = op.specialize(DefaultConversion.class, op.getSourceCRS(),
                changeCS(op.getTargetCRS(), HardCodedCS.GEODETIC_φλ), factory);
        assertMatrixEquals("Longitude rotation of a two-dimensional CRS", Matrices.create(3, 4, new double[] {
                0, 1, 0, 0,
                1, 0, 0, OFFSET,
                0, 0, 0, 1}), MathTransforms.getMatrix(op.getMathTransform()), STRICT);
    }


    /**
     * Tests {@link DefaultConversion#specialize DefaultConversion.specialize(…)} with an interpolation CRS.
     * In this test, we invent an imaginary scenario where the longitude rotation to apply varies with time
     * (a "moving prime meridian").
     *
     * <div class="note"><b>Note:</b>
     * from some point of view, this scenario is not as weird as it may look like. The Greenwich prime meridian
     * was initially the meridian passing through the telescope of the Greenwich observatory. But when a new
     * more powerful telescopes was built, is was installed a few metres far from the old one. So if we were
     * staying to a strict interpretation like "the meridian passing through the main telescope",
     * that meridian would indeed more with time.</div>
     *
     * @throws FactoryException Should not happen in this test.
     */
    @Test
    @DependsOnMethod("testDefiningConversion")
    public void testWithInterpolationCRS() throws FactoryException {
        DefaultConversion op = createLongitudeRotation(HardCodedCRS.NTF_NORMALIZED_AXES,
                createParisCRS(false, HardCodedCS.GEODETIC_2D, true), HardCodedCRS.TIME);
        assertMatrixEquals("Longitude rotation of a time-varying CRS", new Matrix4(
                1, 0, 0, 0,
                0, 1, 0, OFFSET,
                0, 0, 1, 0,
                0, 0, 0, 1), MathTransforms.getMatrix(op.getMathTransform()), STRICT);

        op = op.specialize(
                DefaultConversion.class,    // In normal use, this would be 'Conversion.class'.
                op.getSourceCRS(),          // Keep the same source CRS.
                changeCS(op.getTargetCRS(), HardCodedCS.GEODETIC_φλ),   // Swap axis order.
                DefaultFactories.forBuildin(MathTransformFactory.class));

        assertMatrixEquals("Longitude rotation of a time-varying CRS", new Matrix4(
                1, 0, 0, 0,
                0, 0, 1, 0,
                0, 1, 0, OFFSET,
                0, 0, 0, 1), MathTransforms.getMatrix(op.getMathTransform()), STRICT);
    }

    /**
     * Ensures that {@link DefaultConversion#specialize DefaultConversion.specialize(…)} verifies the datum.
     *
     * @throws FactoryException Should not happen in this test.
     */
    @Test
    public void testDatumCheck() throws FactoryException {
        final MathTransformFactory factory = DefaultFactories.forBuildin(MathTransformFactory.class);
        final DefaultConversion op = createLongitudeRotation(true);
        try {
            op.specialize(Conversion.class, HardCodedCRS.WGS84, HardCodedCRS.NTF_NORMALIZED_AXES, factory);
            fail("Should not have accepted to change the geodetic datum.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("sourceCRS"));
            assertTrue(message, message.contains("Nouvelle Triangulation Française"));
        }
        try {
            op.specialize(Conversion.class, HardCodedCRS.NTF_NORMALIZED_AXES, HardCodedCRS.WGS84, factory);
            fail("Should not have accepted to change the geodetic datum.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("targetCRS"));
            assertTrue(message, message.contains("Nouvelle Triangulation Française"));
        }
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testConstruction")
    public void testSerialization() {
        verifyProperties(assertSerializedEquals(createLongitudeRotation(false)), false);
    }
}
