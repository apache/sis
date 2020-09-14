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
package org.apache.sis.internal.referencing;

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;


/**
 * Proxy to {@link MathTransforms} method which can be redirected to a {@link MathTransformFactory}.
 * The method signature in this class mirrors the one in {@link MathTransforms}. We do not provide
 * this functionality as a {@link MathTransformFactory} implementation because we do not override
 * all methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
public class MathTransformsOrFactory {
    /**
     * The unique instance to use when no {@link MathTransformFactory} is specified.
     */
    private static final MathTransformsOrFactory INSTANCE = new MathTransformsOrFactory();

    /**
     * Do not allow instantiation of this class, except the inner sub-class.
     */
    private MathTransformsOrFactory() {
    }

    /**
     * Returns the instance to use for the given factory.
     *
     * @param  factory  the factory, which may be {@code null}.
     * @return the instance to use.
     */
    public static MathTransformsOrFactory wrap(final MathTransformFactory factory) {
        return (factory != null) ? new Specified(factory) : INSTANCE;
    }

    /**
     * Creates an arbitrary linear transform from the specified matrix.
     *
     * @param  matrix  the matrix used to define the linear transform.
     * @return the linear (usually affine) transform.
     * @throws FactoryException if a factory is wrapped and can not perform the operation.
     */
    public MathTransform linear(final Matrix matrix) throws FactoryException {
        return MathTransforms.linear(matrix);
    }

    /**
     * Creates a transform which passes through a subset of coordinates to another transform.
     *
     * @param  firstAffectedCoordinate  index of the first affected coordinate.
     * @param  subTransform             the sub-transform to apply on modified coordinates.
     * @param  numTrailingCoordinates   number of trailing coordinates to pass through.
     * @return a pass-through transform, potentially as a {@link PassThroughTransform} instance but not necessarily.
     * @throws FactoryException if a factory is wrapped and can not perform the operation.
     */
    public MathTransform passThrough(int firstAffectedCoordinate, MathTransform subTransform, int numTrailingCoordinates) throws FactoryException {
        return MathTransforms.passThrough(firstAffectedCoordinate, subTransform, numTrailingCoordinates);
    }

    /**
     * Concatenates the two given transforms.
     *
     * @param  tr1  the first math transform.
     * @param  tr2  the second math transform.
     * @return the concatenated transform.
     * @throws FactoryException if a factory is wrapped and can not perform the operation.
     */
    public MathTransform concatenate(MathTransform tr1, MathTransform tr2) throws FactoryException {
        return MathTransforms.concatenate(tr1, tr2);
    }

    /**
     * Concatenates the two given transforms, switching their order if {@code applyOtherFirst} is {@code true}.
     *
     * @param  tr               the first math transform.
     * @param  other            the second math transform.
     * @param  applyOtherFirst  whether {@code other} should be first in the concatenation chain.
     * @return the concatenated transform.
     * @throws FactoryException if a factory is wrapped and can not perform the operation.
     */
    public final MathTransform concatenate(boolean applyOtherFirst, MathTransform tr, MathTransform other) throws FactoryException {
        if (applyOtherFirst) {
            return concatenate(other, tr);
        } else {
            return concatenate(tr, other);
        }
    }

    /**
     * A {@link MathTransformsOrFactory} implementation which delegate method calls to a {@link MathTransformFactory}
     * specified by the user.
     */
    private static final class Specified extends MathTransformsOrFactory {
        /** The factory where to delegate method calls. */
        private final MathTransformFactory factory;

        /** Creates a new instance delegating transform creations to the given factory. */
        Specified(final MathTransformFactory factory) {
            this.factory = factory;
        }

        /** Delegate to {@link MathTransformFactory#createAffineTransform(Matrix)}. */
        @Override public MathTransform linear(Matrix matrix) throws FactoryException {
            return factory.createAffineTransform(matrix);
        }

        /** Delegate to {@link MathTransformFactory#createPassThroughTransform(int, MathTransform, int)}. */
        @Override public MathTransform passThrough(int firstAffectedCoordinate, MathTransform subTransform, int numTrailingCoordinates) throws FactoryException {
            return factory.createPassThroughTransform(firstAffectedCoordinate, subTransform, numTrailingCoordinates);
        }

        /** Delegate to {@link MathTransformFactory#createConcatenatedTransform(MathTransform, MathTransform)}. */
        @Override public MathTransform concatenate(MathTransform tr, MathTransform other) throws FactoryException {
            return factory.createConcatenatedTransform(tr, other);
        }
    }
}
