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
package org.apache.sis.internal.netcdf.impl;

import org.apache.sis.internal.netcdf.Dimension;


/**
 * A dimension in a netCDF file. A dimension can been seen as an axis in the grid space
 * (not the geodetic space). Dimension are referenced by their index in other parts of
 * the netCDF file header. {@code Dimension} instances are unique, i.e. {@code a == b}
 * is a sufficient test for determining that {@code a} and {@code b} represent the same
 * dimension.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
final class DimensionInfo extends Dimension {
    /**
     * The dimension name.
     */
    final String name;

    /**
     * The number of grid cell value along this dimension, as an unsigned number.
     */
    final int length;

    /**
     * Whether this dimension is the "record" (also known as "unlimited") dimension.
     * There is at most one record dimension in a well-formed netCDF file.
     */
    final boolean isUnlimited;

    /**
     * Creates a new dimension of the given name and length.
     *
     * @param name         the dimension name.
     * @param length       the number of grid cell value along this dimension, as an unsigned number.
     * @param isUnlimited  whether this dimension is the "record" (also known as "unlimited") dimension.
     */
    DimensionInfo(final String name, final int length, final boolean isUnlimited) {
        this.name        = name;
        this.length      = length;
        this.isUnlimited = isUnlimited;
    }

    /**
     * Returns the name of this netCDF dimension.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the number of grid cell value along this dimension.
     * In this implementation, the length is never undetermined (negative).
     */
    @Override
    public long length() {
        return Integer.toUnsignedLong(length);
    }

    /**
     * Returns whether this dimension can grow.
     */
    @Override
    protected boolean isUnlimited() {
        return isUnlimited;
    }
}
