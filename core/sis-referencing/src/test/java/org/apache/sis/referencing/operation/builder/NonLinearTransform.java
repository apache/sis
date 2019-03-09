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
package org.apache.sis.referencing.operation.builder;

import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform2D;


/**
 * A two-dimensional non-linear transform for {@link LinearTransformBuilderTest} purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final strictfp class NonLinearTransform extends AbstractMathTransform2D {
    /**
     * Creates a new instance of this class.
     */
    NonLinearTransform() {
    }

    /**
     * Applies an arbitrary non-linear transform.
     */
    @Override
    public Matrix transform(final double[] srcPts, int srcOff,
                            final double[] dstPts, int dstOff, boolean derivate)
    {
        final double x = srcPts[srcOff++];
        final double y = srcPts[srcOff  ];
        dstPts[dstOff++] = x * x;
        dstPts[dstOff  ] = y * y * y;
        return null;
    }
}
