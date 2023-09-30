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
package org.apache.sis.referencing.operation.provider;

import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform;


/**
 * Base class of mock provider for coordinate operations not yet implemented in Apache SIS.
 * This is used for operations needed for executing some Well Known Text (WKT) parsing tests
 * in the {@code org.apache.sis.io.wkt.WKTParserTest} class, without doing any real coordinate
 * operations with the parsed objects.
 *
 * <p>Subclasses may be promoted to a real operation if we implement their formulas in a future Apache SIS version.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("serial")
abstract class ProviderMock extends AbstractProvider {
    /**
     * Creates a new mock provider.
     */
    ProviderMock(final ParameterDescriptorGroup parameters) {
        super(SingleOperation.class, parameters,
              CoordinateSystem.class, false,
              CoordinateSystem.class, false);
    }

    /**
     * Not yet supported.
     *
     * @param  factory     ignored.
     * @param  parameters  ignored.
     * @return a dummy math transform.
     */
    @Override
    public final MathTransform createMathTransform(MathTransformFactory factory, ParameterValueGroup parameters) {
        return new AbstractMathTransform() {
            @Override public int getSourceDimensions() {return 2;}
            @Override public int getTargetDimensions() {return 2;}
            @Override
            public Matrix transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, boolean derivate) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
