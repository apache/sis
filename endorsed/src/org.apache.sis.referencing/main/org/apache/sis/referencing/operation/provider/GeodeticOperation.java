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

import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.SingleOperation;


/**
 * Base class for providers that perform an operation on geographic or geocentric coordinates.
 * In the geographic case, those operations can have two-dimensional and three-dimensional variants
 * by adding or omitting the ellipsoidal height. Sometimes those variants are explicitly declared
 * in the EPSG database and are implemented in this package as separated operations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@XmlTransient
abstract class GeodeticOperation extends AbstractProvider {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2030570546566544546L;

    /**
     * Returns the provider for the specified combinations of 2D and 3D cases, or {@code null} if none.
     * The {@code indexOfDim} argument is a bitmask with the following rules:
     *
     * <ul>
     *   <li>Bit 1: dimension of source coordinates (0 for 2D, 1 for 3D).</li>
     *   <li>Bit 0: dimension of target coordinates (0 for 2D, 1 for 3D).</li>
     * </ul>
     *
     * <h4>Historical note</h4>
     * In ISO 19111:2007, the {@code OperationMethod} type had two attributes for the number of source
     * and target dimensions. Those attributes have been removed in ISO 19111:2019 revision because not
     * really needed in practice. However, the EPSG database still distinguishes between 2D and 3D variants
     * for some of those operations, so we still need the capability to switch operation methods according
     * to the number of dimensions.
     *
     * @return the provider for the specified combination of source and target dimensions, or {@code null} if none.
     *
     * @see #indexOfDim
     * @see #redimension(int, int)
     */
    GeodeticOperation redimensioned(int indexOfDim) {
        return null;
    }

    /**
     * Number of dimensions as the bitmask documented in {@link #redimensioned}.
     * This is also the index of this operation in the {@link #redimensioned} array.
     */
    private final int indexOfDim;

    /**
     * The pseudo-index value for a transform between one-dimensional source and target.
     * This is not a valid index for the {@link #redimensioned} array.
     */
    static final int INDEX_OF_1D = -1;

    /**
     * The index value in {@link #redimensioned} array for a transform between two-dimensional source and target.
     * This is the bitmask value when the rules documented on {@link #redimensioned} are applied.
     */
    static final int INDEX_OF_2D = 0;

    /**
     * The index value in {@link #redimensioned} array for a transform between three-dimensional source and target.
     * This is the bitmask value when the rules documented on {@link #redimensioned} are applied.
     */
    static final int INDEX_OF_3D = 3;

    /**
     * Creates a copy of this provider.
     *
     * @deprecated This is a temporary constructor before replacement by a {@code provider()} method with JDK9.
     */
    @Deprecated
    GeodeticOperation(final GeodeticOperation copy) {
        super(copy);
        indexOfDim = copy.indexOfDim;
    }

    /**
     * Constructs a math transform provider from a set of parameters. The provider name and
     * {@linkplain #getIdentifiers() identifiers} will be the same as the parameter ones.
     * This method expects an array either {@code null} or of length 4 with methods of the
     * following dimensions:
     *
     * <ol>
     *   <li>2 → 2 dimensions in {@code redimensioned[0]}</li>
     *   <li>2 → 3 dimensions in {@code redimensioned[1]}</li>
     *   <li>3 → 2 dimensions in {@code redimensioned[2]}</li>
     *   <li>3 → 3 dimensions in {@code redimensioned[3]}</li>
     * </ol>
     *
     * @param operationType      base interface of the {@code CoordinateOperation} instances that use this method.
     * @param parameters         description of parameters expected by this operation.
     * @param indexOfDim         number of dimensions as the index in {@link #redimensioned} array.
     * @param sourceCSType       base interface of the coordinate system of source coordinates.
     * @param sourceOnEllipsoid  whether the operation needs source ellipsoid axis lengths.
     * @param targetCSType       base interface of the coordinate system of target coordinates.
     * @param targetOnEllipsoid  whether the operation needs target ellipsoid axis lengths.
     */
    GeodeticOperation(Class<? extends SingleOperation> operationType, ParameterDescriptorGroup parameters, int indexOfDim,
                      Class<? extends CoordinateSystem> sourceCSType, boolean sourceOnEllipsoid,
                      Class<? extends CoordinateSystem> targetCSType, boolean targetOnEllipsoid)
    {
        super(operationType, parameters,
              sourceCSType, sourceOnEllipsoid,
              targetCSType, targetOnEllipsoid);
        this.indexOfDim = indexOfDim;
    }

    /**
     * Returns the number of source dimensions.
     */
    @Override
    @SuppressWarnings("deprecation")
    public final Integer getSourceDimensions() {
        if (indexOfDim >= 0) {
            return (indexOfDim >>> 1) + 2;
        }
        return 1;
    }

    /**
     * Returns the number of target dimensions.
     */
    @Override
    @SuppressWarnings("deprecation")
    public final Integer getTargetDimensions() {
        if (indexOfDim >= 0) {
            return (indexOfDim & 1) + 2;
        }
        return 1;
    }

    /**
     * Returns the elements of the given array at an index computed from the given dimensions.
     *
     * @param  sourceDimensions  the desired number of input dimensions.
     * @param  targetDimensions  the desired number of output dimensions.
     * @return the redimensioned operation method, or {@code null} if none.
     */
    @Override
    public final AbstractProvider redimension(final int sourceDimensions, final int targetDimensions) {
        final int i = ((sourceDimensions - 2) << 1) | (targetDimensions - 2);
        if (i >= INDEX_OF_2D && i <= INDEX_OF_3D) {
            if (i == indexOfDim) {
                return this;
            }
            final GeodeticOperation redimensioned = redimensioned(i);
            if (redimensioned != null) {
                assert (redimensioned.getSourceDimensions() == sourceDimensions) : sourceDimensions;
                assert (redimensioned.getTargetDimensions() == targetDimensions) : targetDimensions;
                return redimensioned;
            }
        }
        return super.redimension(sourceDimensions, targetDimensions);
    }

    /**
     * The inverse of {@code GeodeticOperation} is usually the same operation with parameter signs inverted.
     *
     * @return {@code this} for most {@code GeodeticOperation} instances.
     */
    @Override
    public AbstractProvider inverse() {
        return this;
    }
}
