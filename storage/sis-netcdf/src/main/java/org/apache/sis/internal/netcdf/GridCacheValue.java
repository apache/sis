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
package org.apache.sis.internal.netcdf;

import java.util.Map;
import java.util.Set;
import java.util.Optional;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.operation.builder.LocalizationGridBuilder;


/**
 * A value cached in {@link GridCacheKey.Global#CACHE}.
 * This is used for sharing common localization grids between different netCDF files.
 * {@code GridCacheValue}s are associated to {@link GridCacheKey}s in a hash map.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
final class GridCacheValue {

    /**
     * The transform from grid coordinates to geographic or projected coordinates.
     */
    final MathTransform gridToCRS;

    /**
     * If a linearization has been applied, the linearization type. Otherwise {@code null}.
     * This field together with {@link #linearizationTarget} and {@link #axisSwap} fields are copies of
     * {@link Linearizer} fields, copied for making sure that this {@code GridCacheValue} is immutable.
     */
    final Linearizer.Type linearizationType;

    /**
     * The target CRS of {@link #gridToCRS} if different than the CRS inferred by {@link CRSBuilder}.
     * This field is non-null if the target CRS has been changed by application of a linearizer.
     */
    final SingleCRS linearizationTarget;

    /**
     * Whether axes need to be swapped in order to have the same direction before and after linearization.
     * For example if input coordinates stored in the localization grid have (east, north) directions,
     * then {@link #linearizationTarget} coordinates shall have (east, north) directions as well.
     * This flag specifies whether input coordinates must be swapped for making above condition true.
     *
     * <p>This flag assumes that {@link #linearizationTarget} has two dimensions.</p>
     */
    final boolean axisSwap;

    /**
     * Creates a new "grid to CRS" together with target CRS.
     */
    GridCacheValue(final Set<Linearizer> linearizers, final LocalizationGridBuilder grid,
                   final MathTransformFactory factory) throws FactoryException
    {
        gridToCRS = grid.create(factory);
        final Optional<Map.Entry<String,MathTransform>> e = grid.linearizer(true);
        if (e.isPresent()) {
            final String name = e.get().getKey();
            for (final Linearizer linearizer : linearizers) {
                if (name.equals(linearizer.name())) {
                    linearizationType   = linearizer.type;
                    linearizationTarget = linearizer.getTargetCRS();
                    axisSwap            = linearizer.axisSwap();
                    return;
                }
            }
        }
        linearizationType   = null;
        linearizationTarget = null;
        axisSwap = false;
    }
}
