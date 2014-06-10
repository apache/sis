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
import org.opengis.referencing.operation.MathTransform1D;
import org.apache.sis.util.resources.Errors;


/**
 * The function converting <cite>sample values</cite> in a raster to <cite>geophysics values</cite>.
 * The function is usually linear, but can sometime be logarithmic or exponential. The later occur
 * most often when measuring concentration of something.
 *
 * <table class="sis">
 *   <caption>Supported transfer functions</caption>
 *   <tr><th>Type</th><th>Equation</th></tr>
 *   <tr><td>{@link TransferFunctionType#LINEAR LINEAR}</td>
 *       <td><var>y</var> = scale⋅<var>x</var> + offset</td></tr>
 *   <tr><td>{@link TransferFunctionType#LOGARITHMIC LOGARITHMIC}</td>
 *       <td><var>y</var> = scale⋅<b>log</b><sub>base</sub>(<var>x</var>) + offset</td></tr>
 *   <tr><td>{@link TransferFunctionType#EXPONENTIAL EXPONENTIAL}</td>
 *       <td><var>y</var> = scale⋅base<sup><var>x</var></sup> + offset</td></tr>
 * </table>
 *
 * {@section Missing values}
 * This {@code TransferFunction} class handles only the continuous part of transfer functions.
 * This class does <strong>not</strong> handle missing values other than {@code NaN}.
 * For a more complete class with support for non-NaN missing values,
 * see {@link org.apache.sis.coverage.grid.GridSampleDimension}.
 *
 * {@section Serialization}
 * Serialized instances of this class are not guaranteed to be compatible with future SIS versions.
 * Serialization should be used only for short term storage or RMI between applications running the
 * same SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public class TransferFunction implements Cloneable, Serializable {
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
     */
    public TransferFunction() {
        type  = TransferFunctionType.LINEAR;
        base  = 10;
        scale =  1;
    }

    /**
     * Returns the transfer function type (linear, logarithmic or exponential).
     *
     * @return The transfer function type.
     */
    public TransferFunctionType getType() {
        return type;
    }

    /**
     * Sets the transfer function type.
     * The default value is {@link TransferFunctionType#LINEAR}.
     *
     * @param type The transfer function type.
     */
    public void setType(final TransferFunctionType type) {
        this.type = type;
        transform = null;
    }

    /**
     * Returns the logarithm or exponent base in the transfer function.
     * This value is always 1 for {@link TransferFunctionType#LINEAR},
     * and usually (but not necessarily) 10 for the other types.
     *
     * @return The logarithmic or exponent base.
     */
    public double getBase() {
        return TransferFunctionType.LINEAR.equals(type) ? 1 : base;
    }

    /**
     * Sets the logarithm or exponent base in the transfer function.
     * This value is ignored for {@link TransferFunctionType#LINEAR}.
     * For other supported types, the default value is 10.
     *
     * @param base The new logarithm or exponent base.
     */
    public void setBase(final double base) {
        this.base = base;
        transform = null;
    }

    /**
     * Returns the scale factor of the transfer function.
     *
     * @return The scale factor.
     */
    public double getScale() {
        return scale;
    }

    /**
     * Sets the scale factor of the transfer function.
     * The default value is 1.
     *
     * @param scale The new scale factor.
     */
    public void setScale(final double scale) {
        this.scale = scale;
        transform = null;
    }

    /**
     * Returns the offset of the transfer function.
     *
     * @return The offset.
     */
    public double getOffset() {
        return offset;
    }

    /**
     * Sets the offset of the transfer function.
     * The default value is 0.
     *
     * @param offset The new offset.
     */
    public void setOffset(final double offset) {
        this.offset = offset;
        transform = null;
    }

    /**
     * Returns the transform from sample values to geophysics values, as specified by the
     * current properties of this {@code TransferFunction}.
     *
     * @return The transform from sample to geophysics values.
     */
    public MathTransform1D getTransform() {
        if (transform == null) {
            if (TransferFunctionType.LINEAR.equals(type)) {
                transform = LinearTransform1D.create(scale, offset);
            } else if (TransferFunctionType.EXPONENTIAL.equals(type)) {
                transform = ExponentialTransform1D.create(base, scale);
                if (offset != 0) {  // Rarely occurs in practice.
                    transform = (MathTransform1D) ConcatenatedTransform.create(
                            transform, LinearTransform1D.create(0, offset));
                }
            } else if (TransferFunctionType.LOGARITHMIC.equals(type)) {
                if (scale == 1) {
                    transform = LogarithmicTransform1D.create(base, offset);
                } else {
                    /*
                     * This case rarely occurs in practice, so we do not provide optimized constructor.
                     * The ExponentialTransform1D.concatenate(…) method will rewrite the equation using
                     * mathematical identities. The result will be a function with a different base.
                     */
                    transform = (MathTransform1D) ConcatenatedTransform.create(
                            LogarithmicTransform1D.create(base, 0),
                            LinearTransform1D.create(scale, offset));
                }
            } else {
                throw new IllegalStateException(Errors.format(Errors.Keys.UnknownType_1, type));
            }
        }
        return transform;
    }
}
