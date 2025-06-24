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

import java.io.Serializable;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.util.FactoryException;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.privy.Numerics;


/**
 * Raises the given value at some fixed power. Current implementation is defined mostly for the
 * needs of the {@link ExponentialTransform1D#concatenateLog(LogarithmicTransform1D, boolean)}.
 * Future version may expand on that.
 *
 * <p>Before to make this class public (if we do), we need to revisit the class name,
 * define parameters and improve the {@link #tryConcatenate(TransformJoiner)} method.</p>
 *
 * <h2>Serialization</h2>
 * Serialized instances of this class are not guaranteed to be compatible with future SIS versions.
 * Serialization should be used only for short term storage or RMI between applications running the
 * same SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class PowerTransform1D extends AbstractMathTransform1D implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4618931749313510016L;

    /**
     * The power.
     */
    final double power;

    /**
     * The inverse of this transform. Created only when first needed.
     * Serialized in order to avoid rounding error if this transform
     * is actually the one which was created from the inverse.
     */
    private PowerTransform1D inverse;

    /**
     * Constructs a new exponential transform. This constructor is provided for subclasses only.
     * Instances should be created using the {@linkplain #create(double) factory method}, which
     * may returns optimized implementations for some particular argument values.
     *
     * @param power  the power at which to raise the values.
     */
    protected PowerTransform1D(final double power) {
        this.power = power;
    }

    /**
     * Constructs a new power transform.
     *
     * @param  power  the power at which to raise the values.
     * @return the math transform.
     */
    public static MathTransform1D create(final double power) {
        if (power == 1) return IdentityTransform1D.INSTANCE;
        if (power == 0) return ConstantTransform1D.ONE;
        return new PowerTransform1D(power);
    }

    /**
     * Creates the inverse transform of this object.
     */
    @Override
    public MathTransform1D inverse() {
        if (inverse == null) {
            inverse = new PowerTransform1D(1 / power);
            inverse.inverse = this;
        }
        return inverse;
    }

    /**
     * Gets the derivative of this function at a value.
     */
    @Override
    public double derivative(final double value) {
        return power * Math.pow(value, power - 1);
    }

    /**
     * Transforms the specified value.
     */
    @Override
    public double transform(final double value) {
        return Math.pow(value, power);
    }

    /**
     * Transforms many positions in a sequence of coordinate tuples.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        if (srcPts!=dstPts || srcOff>=dstOff) {
            while (--numPts >= 0) {
                dstPts[dstOff++] = Math.pow(srcPts[srcOff++], power);
            }
        } else {
            srcOff += numPts;
            dstOff += numPts;
            while (--numPts >= 0) {
                dstPts[--dstOff] = Math.pow(srcPts[--srcOff], power);
            }
        }
    }

    /**
     * Transforms many positions in a sequence of coordinate tuples.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
        if (srcPts!=dstPts || srcOff>=dstOff) {
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) Math.pow(srcPts[srcOff++], power);
            }
        } else {
            srcOff += numPts;
            dstOff += numPts;
            while (--numPts >= 0) {
                dstPts[--dstOff] = (float) Math.pow(srcPts[--srcOff], power);
            }
        }
    }

    /**
     * Transforms many positions in a sequence of coordinate tuples.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff, final float[] dstPts, int dstOff, int numPts) {
        while (--numPts >= 0) {
            dstPts[dstOff++] = (float) Math.pow(srcPts[srcOff++], power);
        }
    }

    /**
     * Transforms many positions in a sequence of coordinate tuples.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        while (--numPts >= 0) {
            dstPts[dstOff++] = Math.pow(srcPts[srcOff++], power);
        }
    }

    /**
     * Concatenates in an optimized way a {@link MathTransform} {@code other} to this {@code MathTransform}.
     */
    @Override
    protected void tryConcatenate(final TransformJoiner context) throws FactoryException {
        int relativeIndex = +1;
        do {
            final MathTransform other = context.getTransform(relativeIndex).orElse(null);
            if (other instanceof PowerTransform1D) {
                // Valid for both concatenation and pre-concatenation.
                if (context.replace(relativeIndex, create(power + ((PowerTransform1D) other).power))) {
                    return;
                }
            }
        } while ((relativeIndex = -relativeIndex) < 0);
        // TODO: more optimization could go here for logarithmic and exponential cases.
        super.tryConcatenate(context);
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    protected int computeHashCode() {
        return super.computeHashCode() + Double.hashCode(power);
    }

    /**
     * Compares the specified object with this math transform for equality.
     *
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;                    // Optimization for a common case.
        }
        if (super.equals(object, mode)) {
            return Numerics.equals(power, ((PowerTransform1D) object).power);
        }
        return false;
    }
}
