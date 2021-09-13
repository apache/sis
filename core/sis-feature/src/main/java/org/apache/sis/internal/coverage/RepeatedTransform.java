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
package org.apache.sis.internal.coverage;

import java.util.Arrays;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;


/**
 * An special case of {@link CompoundTransform} where the components are the same transform repeated many times.
 * This optimization allows to replace many single {@code transform(…)} calls by a single {@code transform(…)}
 * call on a larger array segment.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 */
final class RepeatedTransform extends CompoundTransform {
    /**
     * The transform which is repeated.
     */
    private final MathTransform component;

    /**
     * Number of times that the {@linkplain #component} is repeated.
     * Should be greater than 1 (otherwise the use of this class is pointless).
     */
    private final int repetition;

    /**
     * Creates a new compound transform.
     *
     * @param  component   the transform which is repeated.
     * @param  repetition  number of times that the component is repeated.
     */
    RepeatedTransform(final MathTransform component, final int repetition) {
        this.component  = component;
        this.repetition = repetition;
    }

    /**
     * Returns the components of this compound transform.
     */
    @Override
    final MathTransform[] components() {
        final MathTransform[] components = new MathTransform[repetition];
        Arrays.fill(components, component);
        return components;
    }

    /**
     * Returns the number of source dimensions of this compound transform.
     * This is the sum of the number of source dimensions of all components.
     */
    @Override
    public int getSourceDimensions() {
        return component.getSourceDimensions() * repetition;
    }

    /**
     * Returns the number of target dimensions of this compound transform.
     * This is the sum of the number of target dimensions of all components.
     */
    @Override
    public int getTargetDimensions() {
        return component.getTargetDimensions() * repetition;
    }

    /**
     * Tests whether this transform does not move any points.
     */
    @Override
    public boolean isIdentity() {
        return component.isIdentity();
    }

    /**
     * Transforms a single coordinate point in an array, and optionally computes the transform derivative
     * at that location.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        final MatrixSIS m;
        if (derivate) {
            m = Matrices.createZero(repetition, repetition);
            for (int i=0; i<repetition; i++) {
                /*
                 * TODO: implementation restricted to MathTransform1D component for now.
                 *       In a future version, we should port code from PassthroughTransform.
                 */
                m.setElement(i, i, ((MathTransform1D) component).derivative(srcPts[srcOff + i]));
            }
        } else {
            m = null;
        }
        component.transform(srcPts, srcOff, dstPts, dstOff, repetition);
        return m;
    }

    /**
     * Transforms a list of coordinate points.
     */
    @Override
    public void transform(final double[] srcPts, final int srcOff,
                          final double[] dstPts, final int dstOff, final int numPts) throws TransformException
    {
        component.transform(srcPts, srcOff, dstPts, dstOff, numPts * repetition);
    }

    /**
     * Transforms a list of coordinate points.
     */
    @Override
    public void transform(final float[] srcPts, final int srcOff,
                          final float[] dstPts, final int dstOff, final int numPts) throws TransformException
    {
        component.transform(srcPts, srcOff, dstPts, dstOff, numPts * repetition);
    }

    /**
     * Transforms a list of coordinate points.
     */
    @Override
    public void transform(final double[] srcPts, final int srcOff,
                          final float [] dstPts, final int dstOff, final int numPts) throws TransformException
    {
        component.transform(srcPts, srcOff, dstPts, dstOff, numPts * repetition);
    }

    /**
     * Transforms a list of coordinate points.
     */
    @Override
    public void transform(final float [] srcPts, final int srcOff,
                          final double[] dstPts, final int dstOff, final int numPts) throws TransformException
    {
        component.transform(srcPts, srcOff, dstPts, dstOff, numPts * repetition);
    }
}
