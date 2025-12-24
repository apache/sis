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
package org.apache.sis.storage.netcdf.ucar;

import java.util.List;
import java.util.Collection;
import ucar.nc2.Dimension;


/**
 * Wrapper around a UCAR {@link Dimension} for transmitting information to classes outside this package.
 * Temporary instances of this class are created when required by API for the needs of classes outside this package,
 * then discarded. We do not save references to {@code DimensionWrapper} since they do not provide new capabilities.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class DimensionWrapper extends org.apache.sis.storage.netcdf.base.Dimension {
    /**
     * Wraps all given dimensions.
     */
    static List<org.apache.sis.storage.netcdf.base.Dimension> wrap(final Collection<Dimension> dimensions) {
        final var wrappers = new DimensionWrapper[dimensions.size()];
        int i = 0;
        for (final Dimension dim : dimensions) {
            wrappers[i] = new DimensionWrapper(dim, i);
            i++;
        }
        return List.of(wrappers);
    }

    /**
     * Unwraps all given dimensions.
     */
    static Dimension[] unwrap(final org.apache.sis.storage.netcdf.base.Dimension[] dimensions) {
        final var ncd = new Dimension[dimensions.length];
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
     * Index of the dimension in the variable, or -1 if unknown. Used during comparisons of
     * dimensions that are private to a variable, because those dimensions may be unnamed.
     * Consequently, value -1 should be used only for shared dimensions.
     *
     * @see <a href="https://github.com/Unidata/netcdf-java/issues/951">Issue #951 on netcdf-java</a>
     */
    private final int index;

    /**
     * Wraps the given netCDF dimension object.
     */
    DimensionWrapper(final Dimension netcdf, final int index) {
        this.netcdf = netcdf;
        this.index  = index;
    }

    /**
     * Returns the name of this netCDF dimension, or {@code null} if none.
     * Should always be non-null if {@link Dimension#isShared()} is {@code true},
     * but may be null for non-shared dimensions (i.e. dimensions private to a variable).
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

    /**
     * Returns a dimension with its index decremented by 1. This method is invoked for trailing dimensions
     * after a previous dimension has been removed from a list.
     *
     * @return a dimension equals to this one but with its list index (if any) decremented.
     */
    @Override
    protected org.apache.sis.storage.netcdf.base.Dimension decrementIndex() {
        return new DimensionWrapper(netcdf, index - 1);
    }

    /**
     * Returns {@code true} if the given object represents the same dimension as this object.
     * If the dimension is shared, then it has a unique name and {@link Dimension#equals(Object)}
     * can distinguish dimensions based on their name. But if the dimension is private to a variable,
     * then the dimension name can be null and the only remaining discriminant is the dimension length.
     * A problem is that the length may by coincidence be the same for different dimensions.
     * Consequently, for non-shared dimensions we need to add {@link #index} in the comparison.
     *
     * @param  obj  the other object to compare with this dimension.
     * @return whether the other object wraps the same netCDF dimension as this object.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof DimensionWrapper) {
            final DimensionWrapper other = (DimensionWrapper) obj;
            if (netcdf.equals(other.netcdf)) {
                return netcdf.isShared() || other.index == index;
            }
        }
        return false;
    }

    /**
     * Returns a hash code value for this dimension.
     * See {@link #equals(Object)} for a discussion about the use of {@link #index}.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        int code = ~netcdf.hashCode();
        if (!netcdf.isShared()) {
            code += 37 * index;
        }
        return code;
    }
}
