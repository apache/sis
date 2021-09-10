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
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.geometry.DirectPosition2D;


/**
 * Wraps a {@link MathTransform} as a {@link MathTransform2D}. This adapter should not be needed with
 * Apache SIS implementations. It is provided in case we got a foreigner implementation that do not
 * implement the expected interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see MathTransforms#bidimensional(MathTransform)
 *
 * @since 1.1
 * @module
 */
final class TransformAdapter2D extends AbstractMathTransform2D implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7587206692912120654L;

    /**
     * The math transform which was supposed to implement the {@link MathTransform2D} interface..
     */
    private final MathTransform impl;

    /**
     * Creates a wrapper for the given transform.
     */
    TransformAdapter2D(final MathTransform impl) {
        this.impl = impl;
    }

    /** Transforms a single point and opportunistically compute its derivative. */
    @Override public Matrix transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, boolean derivate) throws TransformException {
        Matrix d = derivate ? impl.derivative(new DirectPosition2D(srcPts[srcOff], srcPts[srcOff + 1])) : null;
        if (dstPts != null) impl.transform(srcPts, srcOff, dstPts, dstOff, 1);
        return d;
    }

    /** Delegates to wrapped transform. */
    @Override public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        impl.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /** Delegates to wrapped transform. */
    @Override public void transform(double[] srcPts, int srcOff, float[]  dstPts, int dstOff, int numPts) throws TransformException {
        impl.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /** Delegates to wrapped transform. */
    @Override public void transform(float[]  srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        impl.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /** Delegates to wrapped transform. */
    @Override public void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) throws TransformException {
        impl.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /** Delegates to wrapped transform. */
    @Override public DirectPosition transform(DirectPosition ptSrc, DirectPosition ptDst) throws TransformException {
        return impl.transform(ptSrc, ptDst);
    }

    /** Delegates to wrapped transform. */
    @Override public MathTransform2D inverse() throws NoninvertibleTransformException {
        return MathTransforms.bidimensional(impl.inverse());
    }

    /** Delegates to wrapped transform. */
    @Override public String toWKT() {
        return impl.toWKT();
    }
}
