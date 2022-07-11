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

import java.util.Optional;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.GeneralEnvelope;

import static java.lang.StrictMath.*;


/**
 * A pseudo-transform for debugging purpose. The input points can be random numbers between 0 and 1.
 * The transformed points are build as below (when formatted in base 10):
 *
 * {@preformat text
 *     [1 digit for dimension] [3 first fraction digits] . [original digits from source]
 * }
 *
 * For example if the first input coordinate is (0.2, 0.5, 0.3), then the transformed coordinate will be:
 *
 * {@preformat text
 *     1002.2
 *     2005.5
 *     3003.3
 * }
 *
 * This inverse transform is not effective and this transform can not compute {@linkplain #derivative derivative}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.5
 * @module
 */
strictfp class PseudoTransform extends AbstractMathTransform {
    /**
     * The source and target dimensions.
     */
    protected final int sourceDimensions, targetDimensions;

    /**
     * Temporary buffer for copying the coordinates of a single source point.
     * Used in order to be compliant with {@link IterationStrategy} contract.
     */
    private final double[] buffer;

    /**
     * Creates a transform for the given dimensions.
     *
     * @param  sourceDimensions  the source dimension.
     * @param  targetDimensions  the target dimension.
     */
    public PseudoTransform(final int sourceDimensions, final int targetDimensions) {
        this.sourceDimensions = sourceDimensions;
        this.targetDimensions = targetDimensions;
        this.buffer = new double[sourceDimensions];
    }

    /**
     * Returns the number of source dimensions.
     */
    @Override
    public int getSourceDimensions() {
        return sourceDimensions;
    }

    /**
     * Returns the number of target dimensions.
     */
    @Override
    public int getTargetDimensions() {
        return targetDimensions;
    }

    /**
     * Returns the domain of input coordinates.
     */
    @Override
    public final Optional<Envelope> getDomain(DomainDefinition criteria) {
        final GeneralEnvelope domain = new GeneralEnvelope(sourceDimensions);
        for (int i=0; i<sourceDimensions; i++) {
            domain.setRange(i, 0, 1);
        }
        return Optional.of(domain);
    }

    /**
     * Pseudo-transform a point in the given array.
     *
     * @throws TransformException should never occurs in this class,
     *         but can occur in method overridden by subclasses.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        System.arraycopy(srcPts, srcOff, buffer, 0, sourceDimensions);
        for (int i=0; i<targetDimensions; i++) {
            double v = buffer[i % sourceDimensions];
            v += (i+1)*1000 + round(v * 1000);
            dstPts[dstOff + i] = v;
        }
        return null;
    }

    /**
     * Returns a dummy inverse transform. That transform can not apply any operation.
     */
    @Override
    public MathTransform inverse() {
        return new Inverse() {
            @Override public MathTransform inverse() {
                return PseudoTransform.this;
            }

            @Override public Matrix transform(double[] srcPts, int srcOff,
                                              double[] dstPts, int dstOff, boolean derivate)
                    throws TransformException
            {
                throw new TransformException("Not supported yet.");
            }
        };
    }
}
