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

import java.util.Set;
import java.util.Random;
import java.util.HashSet;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;

import static org.junit.Assert.*;


/**
 * A pseudo-transform where some coordinate fail to be transformed.
 * This is used for testing robustness to transformation failures.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final strictfp class RandomFailureTransform extends PseudoTransform {
    /**
     * The random number generator for determining if a transform should fail.
     */
    private final Random random;

    /**
     * The denominator of the fraction that determines the frequency of failures.
     * See constructor javadoc.
     */
    private final int denominator;

    /**
     * The {@link #ordinal} values of coordinates that failed to be transformed. This is provided
     * for information purpose but not used by this class. This is user's responsibility to clear
     * this set before to start transforming a new array.
     */
    public final Set<Integer> failures;

    /**
     * Incremented after every transformed point and stored in the {@link #failures} set in
     * case of failure. This is user's responsibility to set this field to O before to start
     * transforming a new array.
     */
    public int ordinal;

    /**
     * Creates a transform for the given dimensions. The argument is the denominator of the
     * fraction that determines the frequency of failures. For example if this value is 20,
     * then 1/20 (i.e. 5%) of the points to transform will fail.
     *
     * @param denominator The denominator of the fraction that determines the frequency of failures.
     */
    public RandomFailureTransform(final int denominator) {
        super(4,3);
        this.denominator = denominator;
        random = new Random(891914828L * denominator);
        failures = new HashSet<Integer>();
    }

    /**
     * Fills the given array with random number.
     *
     * @param array The array to fill.
     */
    public void fill(final double[] array) {
        for (int i=0; i<array.length; i++) {
            array[i] = random.nextDouble();
        }
    }

    /**
     * Fills the given array with random number.
     *
     * @param array The array to fill.
     */
    public void fill(final float[] array) {
        for (int i=0; i<array.length; i++) {
            array[i] = random.nextFloat();
        }
    }

    /**
     * Pseudo-transform a point in the given array, with intentional random failures.
     *
     * @throws TransformException Throws randomly at the frequency given at construction time.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        final Matrix derivative = super.transform(srcPts, srcOff, dstPts, dstOff, derivate);
        final int index = ordinal++;
        if (random.nextInt(denominator) == 0) {
            assertTrue("Clash in coordinate ordinal.", failures.add(index));
            throw new TransformException("Random exception for testing purpose.");
        }
        return derivative;
    }
}
