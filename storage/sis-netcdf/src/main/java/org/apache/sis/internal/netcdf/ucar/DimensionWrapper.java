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
package org.apache.sis.internal.netcdf.ucar;

import java.util.List;
import java.util.stream.Collectors;
import ucar.nc2.Dimension;


/**
 * Wrapper around a UCAR {@link Dimension} for transmitting information to classes outside this package.
 * Temporary instances of this class are created when required by API for the needs of classes outside this package,
 * then discarded. We do not save references to {@code DimensionWrapper} since they do not provide new capabilities.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class DimensionWrapper extends org.apache.sis.internal.netcdf.Dimension {
    /**
     * Wraps all given dimensions.
     */
    static List<org.apache.sis.internal.netcdf.Dimension> wrap(final List<Dimension> dimensions) {
        return dimensions.stream().map(DimensionWrapper::new).collect(Collectors.toList());
    }

    /**
     * Unwraps all given dimensions.
     */
    static Dimension[] unwrap(final org.apache.sis.internal.netcdf.Dimension[] dimensions) {
        final Dimension[] ncd = new Dimension[dimensions.length];
        for (int i=0; i<ncd.length; i++) {
            ncd[i] = ((DimensionWrapper) dimensions[i]).netcdf;
        }
        return ncd;
    }

    /**
     * The netCDF dimension object.
     */
    private final Dimension netcdf;

    /**
     * Wraps the given netCDF dimension object.
     */
    private DimensionWrapper(final Dimension netcdf) {
        this.netcdf = netcdf;
    }

    /**
     * Returns the name of this netCDF dimension.
     */
    @Override
    public String getName() {
        return netcdf.getShortName();
    }

    /**
     * Returns the number of grid cell values along this dimension, or a negative number if undetermined.
     * The length may be undetermined if this dimension {@linkplain #isUnlimited() is unlimited}.
     */
    @Override
    public long length() {
        return netcdf.getLength();      // May be negative.
    }

    /**
     * Returns whether this dimension can grow.
     */
    @Override
    protected boolean isUnlimited() {
        return netcdf.isUnlimited();
    }
}
