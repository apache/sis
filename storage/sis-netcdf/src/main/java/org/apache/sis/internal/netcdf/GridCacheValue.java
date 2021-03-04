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

import java.util.Set;
import java.util.List;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.operation.builder.LocalizationGridBuilder;


/**
 * A value cached in {@link GridCacheKey.Global#CACHE}.
 * This is used for sharing common localization grids between different netCDF files.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class GridCacheValue {
    /**
     * The transform from grid coordinates to geographic or projected coordinates.
     */
    final MathTransform transform;

    /**
     * The target CRS of {@link #transform} if different than the CRS inferred by {@link CRSBuilder}.
     * This field is non-null if the target CRS has been changed by application of a linearizer.
     */
    private CoordinateReferenceSystem targetCRS;

    /**
     * Creates a new "grid to CRS" together with target CRS.
     */
    GridCacheValue(final Set<Linearizer> linearizers, final LocalizationGridBuilder grid,
                   final MathTransformFactory factory) throws FactoryException
    {
        transform = grid.create(factory);
        grid.linearizer(true).ifPresent((e) -> {
            final String name = e.getKey();
            for (final Linearizer linearizer : linearizers) {
                if (name.equals(linearizer.name())) {
                    targetCRS = linearizer.getTargetCRS();
                    break;
                }
            }
        });
    }

    /**
     * Adds the target CRS to the given list if that CRS is different than the CRS inferred by {@link CRSBuilder}.
     * This is an element of the list to provide to {@link CRSBuilder#assemble(Decoder, Grid, List, Matrix)}.
     */
    final void getLinearizationTarget(final List<CoordinateReferenceSystem> list) {
        if (targetCRS != null) {
            list.add(targetCRS);
        }
    }
}
