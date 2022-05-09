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
import org.opengis.metadata.content.TransferFunctionType;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.util.FactoryException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.StringBuilders;
import org.apache.sis.util.Characters;
import org.apache.sis.referencing.operation.matrix.Matrix2;


/**
 * The function converting raster <cite>sample values</cite> to <cite>geophysics values</cite>.
 * The function is usually linear, but can sometime be logarithmic or exponential.
 * The latter occur most often when measuring concentration of something.
 *
 * <table class="sis">
 *   <caption>Supported transfer function types</caption>
 *   <tr><th>Type</th><th>Equation</th></tr>
 *   <tr><td>{@link TransferFunctionType#LINEAR LINEAR}</td>
 *       <td><var>y</var> = scale⋅<var>x</var> + offset</td></tr>
 *   <tr><td>{@link TransferFunctionType#LOGARITHMIC LOGARITHMIC}</td>
 *       <td><var>y</var> = scale⋅<b>log</b><sub>base</sub>(<var>x</var>) + offset</td></tr>
 *   <tr><td>{@link TransferFunctionType#EXPONENTIAL EXPONENTIAL}</td>
 *       <td><var>y</var> = scale⋅base<sup><var>x</var></sup> + offset</td></tr>
 * </table>
 *
 * <h2>Missing values</h2>
 * This {@code TransferFunction} class handles only the continuous part of transfer functions.
 * This class does <strong>not</strong> handle missing values other than {@code NaN}.
 * For a more complete class with support for non-NaN missing values,
 * see {@code org.apache.sis.coverage.SampleDimension}.
 *
 * <h2>Serialization</h2>
 * Serialized instances of this class are not guaranteed to be compatible with future SIS versions.
 * Serialization should be used only for short term storage or RMI between applications running the
 * same SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see org.apache.sis.coverage.SampleDimension#getTransferFunction()
 *
 * @since 0.5
 * @module
 */
public class TransferFunction implements Cloneable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 185931909755748004L;

    /**
     * Whether the function is linear, logarithmic or exponential.
     */
    private TransferFunctionType type;

    /**
     * The logarithmic base. Ignored if {@link #type} is {@code LINEAR}.
     */
    private double base;

    /**
     * The scale factor, or {@code NaN} if unknown.
     */
    private double scale;

    /**
     * The scale factor, or {@code NaN} if unknown.
     */
    private double offset;

    /**
     * The transform created from above information, or {@code null} if not yet created.
     * Conversely, may be the transform given to the constructor from which above information
     * were inferred.
     *
     * <p>This field is serialized because the transform may be a user-provided one.</p>
     */
    private MathTransform1D transform;

    /**
     * Creates a transfer function initialized to the identity transform.
     *
     * @see #isIdentity()
     */
    public TransferFunction() {
        type  = TransferFunctionType.LINEAR;
        base  = 10;
        scale =  1;
    }

    /**
     * Returns {@code true} if this transfer function is identity. The function is identity if
     * its {@linkplain #getType() type} is {@linkplain TransferFunctionType#LINEAR linear},
     * its {@linkplain #getScale() scale} is 1 and its {@linkplain #getOffset() offset} is 0.
     *
     * @return whether this transfer function is identity.
     *
     * @since 1.0
     */
    public boolean isIdentity() {
        return TransferFunctionType.LINEAR.equals(type) && scale == 1 && offset == 0;
    }

    /**
     * Returns the transfer function type (linear, logarithmic or exponential).
     *
     * @return the transfer function type.
     */
    public TransferFunctionType getType() {
        return type;
    }

    /**
     * Sets the transfer function type.
     * The default value is {@link TransferFunctionType#LINEAR}.
     *
     * @param  type  the transfer function type.
     */
    public void setType(final TransferFunctionType type) {
        ArgumentChecks.ensureNonNull("type", type);
        this.type = type;
        transform = null;
    }

    /**
     * Returns the logarithm or exponent base in the transfer function.
     * This value is always 1 for {@link TransferFunctionType#LINEAR},
     * and usually (but not necessarily) 10 for the logarithmic and exponential types.
     *
     * @return the logarithmic or exponent base.
     */
    public double getBase() {
        return TransferFunctionType.LINEAR.equals(type) ? 1 : base;
    }

    /**
     * Sets the logarithm or exponent base in the transfer function.
     * This value is ignored for {@link TransferFunctionType#LINEAR}.
     * For other supported types, the default value is 10.
     *
     * @param  base  the new logarithm or exponent base.
     * @throws IllegalArgumentException if the given base is NaN, negative, zero or infinite.
     */
    public void setBase(final double base) {
        ArgumentChecks.ensureStrictlyPositive("base", base);
        ArgumentChecks.ensureFinite("base", base);
        this.base = base;
        transform = null;
    }

    /**
     * Returns the scale factor of the transfer function.
     *
     * @return the scale factor.
     */
    public double getScale() {
        return scale;
    }

    /**
     * Sets the scale factor of the transfer function.
     * The default value is 1.
     *
     * @param  scale  the new scale factor.
     * @throws IllegalArgumentException if the given scale is NaN, zero or infinite.
     */
    public void setScale(final double scale) {
        ArgumentChecks.ensureFinite("scale", scale);
        if (scale == 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "scale", scale));
        }
        this.scale = scale;
        transform = null;
    }

    /**
     * Returns the offset of the transfer function.
     *
     * @return the offset.
     */
    public double getOffset() {
        return offset;
    }

    /**
     * Sets the offset of the transfer function.
     * The default value is 0.
     *
     * @param  offset  the new offset.
     * @throws IllegalArgumentException if the given scale is NaN or infinite.
     */
    public void setOffset(final double offset) {
        ArgumentChecks.ensureFinite("offset",  offset);
        this.offset = offset;
        transform = null;
    }

    /**
     * Returns a linear transform created using the given factory.
     * This method is used only when the user specify explicitly a factory to use.
     *
     * @param  factory    the factory to use.
     * @param  withScale  {@code true} for using {@link #scale}, or {@code false} for replacing the scale by zero.
     */
    private MathTransform createAffineTransform(final MathTransformFactory factory, final boolean withScale) throws FactoryException {
        return factory.createAffineTransform(new Matrix2(withScale ? scale : 0, offset, 0, 1));
    }

    /**
     * Creates a transform from sample values to geophysics values using the given factory.
     * This method constructs a transform mathematically equivalent to the one returned by
     * {@link #getTransform()}, but allows to specify another factory than the default one.
     *
     * @param  factory  the factory to use for creating a transform.
     * @return the transform from sample to geophysics values.
     * @throws FactoryException if the given factory failed to create a transform.
     *
     * @since 1.0
     */
    public MathTransform createTransform(final MathTransformFactory factory) throws FactoryException {
        ArgumentChecks.ensureNonNull("factory", factory);
        MathTransform mt;
        if (TransferFunctionType.LINEAR.equals(type)) {
            mt = createAffineTransform(factory, true);
        } else if (TransferFunctionType.EXPONENTIAL.equals(type)) {
            mt = ExponentialTransform1D.create(base, scale);
            if (offset != 0) {
                mt = factory.createConcatenatedTransform(mt, createAffineTransform(factory, false));
            }
        } else if (TransferFunctionType.LOGARITHMIC.equals(type)) {
            if (scale == 1) {
                mt = LogarithmicTransform1D.create(base, offset);
            } else {
                mt = factory.createConcatenatedTransform(
                        LogarithmicTransform1D.create(base, 0),
                        createAffineTransform(factory, true));
            }
        } else {
            throw new IllegalStateException(Errors.format(Errors.Keys.UnknownType_1, type));
        }
        return mt;
    }

    /**
     * Returns the transform from sample values to geophysics values, as specified by the
     * current properties of this {@code TransferFunction}.
     *
     * @return the transform from sample to geophysics values.
     */
    public MathTransform1D getTransform() {
        MathTransform1D mt = transform;
        if (mt == null) {
            if (TransferFunctionType.LINEAR.equals(type)) {
                mt = LinearTransform1D.create(scale, offset);
            } else if (TransferFunctionType.EXPONENTIAL.equals(type)) {
                mt = ExponentialTransform1D.create(base, scale);
                if (offset != 0) {                                          // Rarely occurs in practice.
                    mt = MathTransforms.concatenate(mt, LinearTransform1D.create(0, offset));
                }
            } else if (TransferFunctionType.LOGARITHMIC.equals(type)) {
                if (scale == 1) {
                    mt = LogarithmicTransform1D.create(base, offset);
                } else {
                    /*
                     * This case rarely occurs in practice, so we do not provide optimized constructor.
                     * The ExponentialTransform1D.concatenate(…) method will rewrite the equation using
                     * mathematical identities. The result will be a function with a different base.
                     */
                    mt = MathTransforms.concatenate(
                            LogarithmicTransform1D.create(base, 0),
                            LinearTransform1D.create(scale, offset));
                }
            } else {
                throw new IllegalStateException(Errors.format(Errors.Keys.UnknownType_1, type));
            }
            transform = mt;     // Set only after all steps completed successfully.
        }
        return mt;
    }

    /**
     * Sets the transform from sample values to geophysics values.
     * This method infers the {@linkplain #getBase() base}, {@linkplain #getScale() scale} and
     * {@linkplain #getOffset() offset} values from the given transform.
     *
     * @param  function  the transform to set.
     * @throws IllegalArgumentException if this method does not recognize the given transform.
     */
    public void setTransform(final MathTransform1D function) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("function", function);
        if (function instanceof LinearTransform) {
            setLinearTerms((LinearTransform) function);
            type = TransferFunctionType.LINEAR;
        } else if (function instanceof ExponentialTransform1D) {
            final ExponentialTransform1D f = (ExponentialTransform1D) function;
            type   = TransferFunctionType.EXPONENTIAL;
            base   = f.base;
            scale  = f.scale;
            offset = 0;
        } else if (function instanceof LogarithmicTransform1D) {
            final LogarithmicTransform1D f = (LogarithmicTransform1D) function;
            type   = TransferFunctionType.LOGARITHMIC;
            base   = f.base();
            offset = f.offset();
            scale  = 1;
        } else {
            /*
             * If we did not recognized one of the known types, maybe the given function
             * is the result of some concatenation. Try to concatenate a logarithmic or
             * exponential transform and see if the result is linear.
             */
            final LogarithmicTransform1D log = LogarithmicTransform1D.Base10.INSTANCE;
            MathTransform1D f = MathTransforms.concatenate(function, log);
            if (f instanceof LinearTransform) {
                setLinearTerms((LinearTransform) f);
                type = TransferFunctionType.EXPONENTIAL;
                base = 10;
            } else {
                f = MathTransforms.concatenate(log.inverse(), function);
                if (f instanceof LinearTransform) {
                    setLinearTerms((LinearTransform) f);
                    type = TransferFunctionType.LOGARITHMIC;
                    base = 10;
                } else {
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.UnknownType_1, function.getClass()));
                }
            }
        }
        transform = function;
    }

    /**
     * Sets the {@link #scale} and {@link #offset} terms from the given function.
     *
     * @param  function  the transform to set.
     * @throws IllegalArgumentException if this method does not recognize the given transform.
     */
    private void setLinearTerms(final LinearTransform function) throws IllegalArgumentException {
        final Matrix m = function.getMatrix();
        final int numRow = m.getNumRow();
        final int numCol = m.getNumCol();
        if (numRow != 2 || numCol != 2) {
            final Integer two = 2;
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedMatrixSize_4, two, two, numRow, numCol));
        }
        scale  = m.getElement(0, 0);
        offset = m.getElement(0, 1);
    }

    /**
     * Returns a clone of this transfer function.
     *
     * @return a clone of this transfer function.
     *
     * @since 0.8
     */
    @Override
    public TransferFunction clone() {
        try {
            return (TransferFunction) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);                    // Should never happen since we are cloneable.
        }
    }

    /**
     * Returns a string representation of this transfer function for debugging purpose.
     * The string returned by this method may change in any future SIS version.
     *
     * @return a string representation of this transfer function.
     */
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder("y = ");
        if (scale != 1) {
            if (scale == -1) {
                b.append('−');
            } else {
                StringBuilders.trimFractionalPart(b.append(scale).append('⋅'));
            }
        }
        if (TransferFunctionType.LINEAR.equals(type)) {
            b.append('x');
        } else if (TransferFunctionType.EXPONENTIAL.equals(type)) {
            if (base == Math.E) {
                b.append('e');
            } else {
                StringBuilders.trimFractionalPart(b.append(base));
            }
            b.append('ˣ');
        } else if (TransferFunctionType.LOGARITHMIC.equals(type)) {
            if (base == Math.E) {
                b.append("ln");
            } else {
                b.append('㏒');
                if (base != 10) {
                    final int c = (int) base;
                    if (c == base && c >= 0 && c <= 9) {
                        b.append(Characters.toSubScript((char) (c - '0')));
                    } else {
                        StringBuilders.trimFractionalPart(b.append(base));
                    }
                }
            }
            b.append('⒳');
        } else {
            b.append('?');
        }
        if (offset != 0) {
            StringBuilders.trimFractionalPart(b.append(' ').append(offset < 0 ? '−' : '+').append(' ').append(Math.abs(offset)));
        }
        return b.toString();
    }
}
