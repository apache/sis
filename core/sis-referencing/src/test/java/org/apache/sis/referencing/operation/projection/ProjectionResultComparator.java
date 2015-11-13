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

import java.util.Arrays;
import java.util.List;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Debug;

import static org.junit.Assert.*;


/**
 * Compares the result of two map projection implementations.
 * This class is used in two cases:
 *
 * <ul>
 *   <li>When a point has been projected using spherical formulas, compares with the same point
 *       transformed using elliptical formulas and throw an exception if the result differ.</li>
 *   <li>Same as the above, but with a projection which is considered a limiting case of another
 *       more general projection.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@SuppressWarnings("serial")
final strictfp class ProjectionResultComparator extends NormalizedProjection {
    /**
     * Maximum difference allowed when comparing the result of an inverse projections, in radians.
     * A value of 1E-11 radians is approximatively 5 centimetres.
     * Note that inverse projections are typically less accurate than forward projections.
     */
    private static final double INVERSE_TOLERANCE = NormalizedProjection.ANGULAR_TOLERANCE;

    /**
     * Maximum difference allowed when comparing the result of forward projections,
     * in distance on the unit ellipse. A value of 1E-8 is approximatively 0.01 metres.
     */
    private static final double FORWARD_TOLERANCE = Formulas.LINEAR_TOLERANCE;

    /**
     * Maximum difference allowed between spherical and elliptical formulas when comparing derivatives.
     * Units are the same than {@link #FORWARD_TOLERANCE}.
     */
    private static final double DERIVATIVE_TOLERANCE = FORWARD_TOLERANCE;

    /**
     * The map projection to be used as the reference projection.
     */
    private final NormalizedProjection reference;

    /**
     * The map projection to be compared with the {@link #reference} one.
     */
    private final NormalizedProjection tested;

    /**
     * Creates a projection which will compare the results of the two given projections.
     */
    ProjectionResultComparator(final NormalizedProjection reference, final NormalizedProjection tested) {
        super(reference);
        this.reference = reference;
        this.tested    = tested;
    }

    /**
     * Replaces the given {@code transform} steps, which is expected to contain exactly one {@link NormalizedProjection}
     * instance using spherical formulas, by new steps for the same map projection, but comparing the spherical formulas
     * with the elliptical ones. This method searches for a {@link NormalizedProjection} instance, which is expected to
     * be an inner class named "Spherical". The inner class is then converted to the outer class using reflection, and
     * those two classes are given to the {@link ProjectionResultComparator} constructor. The later is inserted in the
     * chain in place of the original spherical formulas.
     */
    static MathTransform sphericalAndEllipsoidal(MathTransform transform) {
        int numReplacements = 0;
        final List<MathTransform> steps = MathTransforms.getSteps(transform);
        for (int i=steps.size(); --i >= 0;) {
            final MathTransform step = steps.get(i);
            if (step instanceof NormalizedProjection) {
                final NormalizedProjection spherical = (NormalizedProjection) step;
                final Class<?> sphericalClass = spherical.getClass();
                final Class<?> ellipticalClass = sphericalClass.getSuperclass();
                assertEquals("Class name for the spherical formulas.", "Spherical", sphericalClass.getSimpleName());
                assertEquals("Eccentricity of spherical case.", 0, spherical.eccentricity, 0);
                assertSame("In SIS implementation, the spherical cases are defined as inner classes named “Spherical”"
                        + " which extend their enclosing class. This is only a convention, which we verify here. But"
                        + " there is nothing wrong if a future version choose to not follow this convention anymore.",
                        sphericalClass.getEnclosingClass(), ellipticalClass);
                final Object elliptical;
                try {
                    elliptical = ellipticalClass.getDeclaredConstructor(ellipticalClass).newInstance(spherical);
                } catch (Exception e) {  // ReflectiveOperationException on the JDK7 branch.
                    throw new AssertionError(e);    // Considered as a test failure.
                }
                /*
                 * Arbitrarily selects spherical formulas as the reference implementation because
                 * they are simpler, so less bug-prone, than the elliptical formulas.
                 */
                steps.set(i, new ProjectionResultComparator(spherical, (NormalizedProjection) elliptical));
                numReplacements++;
            }
        }
        assertEquals("Unexpected number of NormalizedTransform instances in the transformation chain.", 1, numReplacements);
        transform = steps.get(0);
        for (int i=1; i<steps.size(); i++) {
            transform = MathTransforms.concatenate(transform, steps.get(i));
        }
        return transform;
    }

    /**
     * Checks if transform using {@link #tested} formulas produces the same result than the {@link #reference} formulas.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff, boolean derivate) throws ProjectionException
    {
        final double[] point = Arrays.copyOfRange(srcPts, srcOff, srcOff + 2);
        final Matrix derivative = tested.transform(srcPts, srcOff, dstPts, dstOff, derivate);
        final Matrix expected = reference.transform(point, 0, point, 0, derivate);
        if (dstPts != null) {
            assertEquals("x", point[0], dstPts[dstOff  ], FORWARD_TOLERANCE);
            assertEquals("y", point[1], dstPts[dstOff+1], FORWARD_TOLERANCE);
        }
        if (expected != null && derivative != null) {
            assertEquals("m00", expected.getElement(0,0), derivative.getElement(0,0), DERIVATIVE_TOLERANCE);
            assertEquals("m01", expected.getElement(0,1), derivative.getElement(0,1), DERIVATIVE_TOLERANCE);
            assertEquals("m10", expected.getElement(1,0), derivative.getElement(1,0), DERIVATIVE_TOLERANCE);
            assertEquals("m11", expected.getElement(1,1), derivative.getElement(1,1), DERIVATIVE_TOLERANCE);
        }
        return derivative;
    }

    /**
     * Checks if transform using {@link #tested} inverse formulas produces the same result than the
     * {@link #reference} inverse formulas.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff) throws ProjectionException
    {
        final double[] point = Arrays.copyOfRange(srcPts, srcOff, srcOff + 2);
        tested.inverseTransform(srcPts, srcOff, dstPts, dstOff);
        reference.inverseTransform(point, 0, point, 0);
        assertEquals("φ", point[0], dstPts[dstOff  ], INVERSE_TOLERANCE);
        assertEquals("λ", point[1], dstPts[dstOff+1], INVERSE_TOLERANCE);
    }

    /**
     * Delegates to the {@link #tested} implementation.
     */
    @Debug
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return tested.getParameterDescriptors();
    }

    /**
     * Delegates to the {@link #tested} implementation.
     */
    @Debug
    @Override
    public ParameterValueGroup getParameterValues() {
        return tested.getParameterValues();
    }

    /**
     * Delegates to the {@link #tested} implementation.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        return tested.equals(object, mode);
    }
}
