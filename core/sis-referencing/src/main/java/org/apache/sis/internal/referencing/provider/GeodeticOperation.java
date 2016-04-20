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
package org.apache.sis.internal.referencing.provider;

import javax.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Transformation;


/**
 * Base class for providers that perform an operation on geographic or geocentric coordinates.
 * In the geographic case, those operations can have two-dimensional and three-dimensional variants
 * by adding or omitting the ellipsoidal height. Sometime those variants are explicitely declared
 * in the EPSG database and are implemented in this package as separated operations. Sometime those
 * variants are specific to Apache SIS and can be fetched only by a call to {@link #redimension(int, int)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
abstract class GeodeticOperation extends AbstractProvider {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5431035501162127059L;

    /**
     * The providers for all combinations between 2D and 3D cases, or {@code null} if none.
     * If non-null, then array length shall be 4. Indices are built with following rules:
     *
     * <ul>
     *   <li>Bit 1: dimension of source coordinates (0 for 2D, 1 for 3D).</li>
     *   <li>Bit 0: dimension of target coordinates (0 for 2D, 1 for 3D).</li>
     * </ul>
     *
     * <strong>Do not modify this array after construction</strong>, since the same array is shared by many
     * objects and there is no synchronization.
     */
    final GeodeticOperation[] redimensioned;

    /**
     * Constructs a math transform provider from a set of parameters. The provider name and
     * {@linkplain #getIdentifiers() identifiers} will be the same than the parameter ones.
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
     * @param sourceDimensions  number of dimensions in the source CRS of this operation method.
     * @param targetDimensions  number of dimensions in the target CRS of this operation method.
     * @param parameters        description of parameters expected by this operation.
     * @param redimensioned     providers for all combinations between 2D and 3D cases, or {@code null}.
     */
    GeodeticOperation(final int sourceDimensions,
                      final int targetDimensions,
                      final ParameterDescriptorGroup parameters,
                      final GeodeticOperation[] redimensioned)
    {
        super(sourceDimensions, targetDimensions, parameters);
        this.redimensioned = redimensioned;
    }

    /**
     * Returns the three-dimensional variant of this operation method, or {@code null} if none.
     * This method needs to be overridden only if the three-dimensional variant is an instance
     * of a different class than this instance.
     */
    Class<? extends GeodeticOperation> variant3D() {
        return null;
    }

    /**
     * Returns the elements of the given array at an index computed from the given dimensions.
     *
     * @param  sourceDimensions  the desired number of input dimensions.
     * @param  targetDimensions  the desired number of output dimensions.
     * @return the redimensioned operation method, or {@code null} if none.
     */
    @Override
    public final OperationMethod redimension(final int sourceDimensions, final int targetDimensions) {
        if (redimensioned != null && (sourceDimensions & ~1) == 2 && (targetDimensions & ~1) == 2) {
            final GeodeticOperation m = redimensioned[((sourceDimensions & 1) << 1) | (targetDimensions & 1)];
            if (m != null) {
                assert (m.getSourceDimensions() == sourceDimensions) : sourceDimensions;
                assert (m.getTargetDimensions() == targetDimensions) : targetDimensions;
                return m;
            }
        }
        return super.redimension(sourceDimensions, targetDimensions);
    }

    /**
     * Returns the interface implemented by all coordinate operations that extends this class.
     *
     * @return Fixed to {@link Transformation}.
     */
    @Override
    public final Class<Transformation> getOperationType() {
        return Transformation.class;
    }

    /**
     * The inverse of {@code GeodeticOperation} is usually the same operation with parameter signs inverted.
     *
     * @return {@code true} for most {@code GeodeticOperation} instances.
     */
    @Override
    public boolean isInvertible() {
        return true;
    }
}
