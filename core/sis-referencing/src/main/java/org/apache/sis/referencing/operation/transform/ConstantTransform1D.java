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

import java.util.Arrays;


/**
 * A one dimensional, constant transform. Output values are set to a constant value regardless of input values.
 * This class is a special case of {@link LinearTransform1D} in which <code>{@linkplain #scale} = 0</code> and
 * <code>{@linkplain #offset} = constant</code>. However, this specialized {@code ConstantTransform1D} class is
 * faster.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class ConstantTransform1D extends LinearTransform1D {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1583675681650985947L;

    /**
     * A transform for the positive zero constant.
     */
    static final ConstantTransform1D ZERO = new ConstantTransform1D(0);

    /**
     * A transform for the one constant.
     */
    static final ConstantTransform1D ONE = new ConstantTransform1D(1);

    /**
     * Constructs a new constant transform.
     *
     * @param offset The {@code offset} term in the linear equation.
     */
    ConstantTransform1D(final double offset) {
        super(0, offset);
    }

    /**
     * Transforms the specified value.
     */
    @Override
    public double transform(double value) {
        return offset;
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
    {
        Arrays.fill(dstPts, dstOff, dstOff + numPts, offset);
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff,
                          final float[] dstPts, int dstOff, int numPts)
    {
        Arrays.fill(dstPts, dstOff, dstOff + numPts, (float) offset);
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final float [] dstPts, int dstOff, int numPts)
    {
        Arrays.fill(dstPts, dstOff, dstOff + numPts, (float) offset);
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     */
    @Override
    public void transform(final float [] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
    {
        Arrays.fill(dstPts, dstOff, dstOff + numPts, offset);
    }
}
