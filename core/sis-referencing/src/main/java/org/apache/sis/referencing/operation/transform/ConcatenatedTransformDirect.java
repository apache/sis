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

import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;


/**
 * Concatenated transform where the transfer dimension is the same than source and target dimension.
 * This fact allows some optimizations, the most important one being the possibility to avoid the use
 * of an intermediate buffer in some case.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
class ConcatenatedTransformDirect extends ConcatenatedTransform {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3568975979013908920L;

    /**
     * Constructs a concatenated transform.
     */
    public ConcatenatedTransformDirect(final MathTransform transform1,
                                       final MathTransform transform2)
    {
        super(transform1, transform2);
    }

    /**
     * Checks if transforms are compatibles with this implementation.
     */
    @Override
    boolean isValid() {
        return super.isValid() &&
               transform1.getSourceDimensions() == transform1.getTargetDimensions() &&
               transform2.getSourceDimensions() == transform2.getTargetDimensions();
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     */
    @Override
    public DirectPosition transform(final DirectPosition ptSrc, DirectPosition ptDst)
            throws TransformException
    {
        assert isValid();
        ptDst = transform1.transform(ptSrc, ptDst);
        return  transform2.transform(ptDst, ptDst);
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     */
    @Override
    public void transform(final double[] srcPts, final int srcOff,
                          final double[] dstPts, final int dstOff,
                          final int numPts) throws TransformException
    {
        assert isValid();
        transform1.transform(srcPts, srcOff, dstPts, dstOff, numPts);
        transform2.transform(dstPts, dstOff, dstPts, dstOff, numPts);
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     */
    @Override
    public void transform(final float[]  srcPts, final int srcOff,
                          final double[] dstPts, final int dstOff,
                          final int numPts) throws TransformException
    {
        assert isValid();
        transform1.transform(srcPts, srcOff, dstPts, dstOff, numPts);
        transform2.transform(dstPts, dstOff, dstPts, dstOff, numPts);
    }

    // Do NOT override the transform(..., float[]...) version because we really need to use
    // an intermediate buffer of type double[] for reducing rounding error.  Otherwise some
    // map projection degrades image quality in an unacceptable way.
}
