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

import javax.measure.unit.NonSI;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;

// Test dependencies
import org.opengis.test.referencing.TransformTestCase;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.test.DependsOn;
import org.junit.Test;


/**
 * Tests the {@link GeographicOffsets}, {@link GeographicOffsets2D} and {@link VerticalOffset} classes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(AffineTest.class)
public final strictfp class GeographicOffsetsTest extends TransformTestCase {
    /**
     * Tests {@code GeographicOffsets2D.createMathTransform(…)}.
     * This test uses the sample point given in §2.4.4.3 of EPSG guide (April 2015).
     *
     * @throws FactoryException should never happen.
     * @throws TransformException should never happen.
     */
    @Test
    public void testGeographicOffsets2D() throws FactoryException, TransformException {
        testCreateMathTransform(new GeographicOffsets());
    }

    /**
     * Tests {@code GeographicOffsets.createMathTransform(…)}.
     *
     * @throws FactoryException should never happen.
     * @throws TransformException should never happen.
     */
    @Test
    public void testGeographicOffsets3D() throws FactoryException, TransformException {
        testCreateMathTransform(new GeographicOffsets2D());
    }

    /**
     * Tests the {@code createMathTransform(…)} method of the given provider.
     * This test uses the two-dimensional sample point given in §2.4.4.3 of EPSG guide (April 2015),
     * leaving the height (if any) to zero.
     */
    private void testCreateMathTransform(final GeographicOffsets provider) throws FactoryException, TransformException {
        final ParameterValueGroup pv = provider.getParameters().createValue();
        pv.parameter("Latitude offset" ).setValue(-5.86 / 3600);
        pv.parameter("Longitude offset").setValue(+0.28 / 3600);
        transform = provider.createMathTransform(null, pv);
        tolerance = Formulas.ANGULAR_TOLERANCE;
        final double[] source = new double[transform.getSourceDimensions()];
        final double[] target = new double[transform.getTargetDimensions()];
        source[1] = 38 + ( 8 + 36.565 /60) /60;     // 38°08′36.565″N
        target[1] = 38 + ( 8 + 30.705 /60) /60;     // 38°08′30.705″N
        source[0] = 23 + (48 + 16.235 /60) /60;     // 23°48′16.235″E
        target[0] = 23 + (48 + 16.515 /60) /60;     // 23°48′16.515″E
        verifyTransform(source, target);
    }

    /**
     * Tests {@code VerticalOffset.createMathTransform(…)}.
     * This test uses the sample point given in §2.4.2.1 of EPSG guide (April 2015)
     * for the <cite>"KOC CD height to KOC WD depth (ft) (1)"</cite> transformation (EPSG:5453).
     *
     * <p><b>IMPORTANT:</b> since the source and target axis directions are opposite, the input coordinate
     * need to be multiplied by -1 <strong>before</strong> the operation is applied. This order is required
     * for consistency with the sign of <cite>"Vertical Offset"</cite> parameter value.</p>
     *
     * @throws FactoryException should never happen.
     * @throws TransformException should never happen.
     */
    @Test
    public void testVerticalOffset() throws FactoryException, TransformException {
        final VerticalOffset provider = new VerticalOffset();
        final ParameterValueGroup pv = provider.getParameters().createValue();
        pv.parameter("Vertical Offset").setValue(15.55, NonSI.FOOT);
        transform = provider.createMathTransform(null, pv);
        tolerance = Formulas.LINEAR_TOLERANCE;
        final double[] source = new double[transform.getSourceDimensions()];
        final double[] target = new double[transform.getTargetDimensions()];
        source[0] = -2.55;              // 2.55 metres up, sign reversed in order to match target axis direction.
        target[0] =  7.18 * 0.3048;     // 7.18 feet down.
        verifyTransform(source, target);
    }

    /**
     * Tests {@code VerticalOffset.createMathTransform(…)} indirectly, through a call to the math transform factory
     * with the source and target coordinate systems specified. The intend of this test is to verify that the change
     * of axis direction is properly handled, given source CRS axis direction up and target CRS axis direction down.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException should never happen.
     */
    @Test
    public void testCreateWithContext() throws FactoryException, TransformException {
        final DefaultMathTransformFactory factory = DefaultFactories.forBuildin(
                MathTransformFactory.class, DefaultMathTransformFactory.class);
        final ParameterValueGroup pv = factory.getDefaultParameters("Vertical Offset");
        pv.parameter("Vertical Offset").setValue(15.55, NonSI.FOOT);
        /*
         * Now create the MathTransform. But at the difference of the above testVerticalOffset() method,
         * we supply information about axis directions. The operation parameter shall have the same sign
         * than in the EPSG database (which is positive), and the source and target ordinates shall have
         * the same sign than in the EPSG example (positive too). However we do not test unit conversion
         * in this method (EPSG sample point uses feet units), only axis direction.
         */
        final DefaultMathTransformFactory.Context context = new DefaultMathTransformFactory.Context();
        context.setSource(HardCodedCS.GRAVITY_RELATED_HEIGHT);  // Direction up, in metres.
        context.setTarget(HardCodedCS.DEPTH);                   // Direction down, in metres.
        transform = factory.createParameterizedTransform(pv, context);
        tolerance = Formulas.LINEAR_TOLERANCE;
        final double[] source = new double[transform.getSourceDimensions()];
        final double[] target = new double[transform.getTargetDimensions()];
        source[0] = 2.55;              // 2.55 metres up, same sign than in EPSG example (positive).
        target[0] = 7.18 * 0.3048;     // 7.18 feet down.
        verifyTransform(source, target);
    }
}
