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
package org.apache.sis.storage.gdal;


/**
 * <abbr>GDAL</abbr> open options.
 * This enumeration mirrors the C/C++ {@code GDAL_OF_*} enumeration in the GDAL's API.
 * The following flags are intentionally omitted:
 *
 * <ul>
 *   <li>{@code READONLY}: this is the default when {@link #UPDATE} is not present.</li>
 *   <li>{@code ALL}: this is the default when {@link #VECTOR}, {@link #RASTER},
 *       {@link #MULTIDIM_RASTER} and {@link #NETWORK_MODEL} are not present.</li>
 *   <li>{@code DEFAULT_BLOCK_ACCESS}: this is the default when {@link #ARRAY_BLOCK_ACCESS}
 *       and {@link #HASHSET_BLOCK_ACCESS} are not present.</li>
 * </ul>
 *
 * @author  Quentin Bialota (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
enum OpenFlag {
    /*
     * DECLARATION ORDER MATTER: we want the ordinal values to be the position of the bit to set.
     * Note also that READONLY, ALL and DEFAULT_BLOCK_ACCESS are intentionally omitted (see Javadoc).
     */

    /**
     * Open in update mode.
     *
     * <p>C/C++ source: {@code GDAL_OF_UPDATE = 0x01}.</p>
     */
    UPDATE,

    /**
     * Allow raster drivers to be used.
     * {@code RASTER} and {@link #MULTIDIM_RASTER} are generally mutually exclusive.
     * This type is included by default if none of {@link #VECTOR}, {@code RASTER},
     * {@link #MULTIDIM_RASTER} and {@link #NETWORK_MODEL} is specified.
     *
     * <p>C/C++ source: {@code GDAL_OF_RASTER = 0x02}.</p>
     */
    RASTER,

    /**
     * Allow vector drivers to be used.
     * This type is included by default if none of {@code VECTOR}, {@link #RASTER},
     * {@link #MULTIDIM_RASTER} and {@link #NETWORK_MODEL} is specified.
     *
     * <p>C/C++ source: {@code GDAL_OF_VECTOR = 0x04}.</p>
     */
    VECTOR,

    /**
     * Allow Geographic Network Model (<abbr>GNM</abbr>) drivers to be used.
     * This type is included by default if none of {@link #VECTOR}, {@link #RASTER},
     * {@link #MULTIDIM_RASTER} and {@code NETWORK_MODEL} is specified.
     *
     * <p>C/C++ source: {@code GDAL_OF_GNM = 0x08}.</p>
     */
    NETWORK_MODEL,

    /**
     * Allow multidimensional raster drivers to be used.
     * {@link #RASTER} and {@code MULTIDIM_RASTER} are generally mutually exclusive.
     * This type is <em>not</em> included by default.
     *
     * <p>C/C++ source: {@code GDAL_OF_MULTIDIM_RASTER = 0x10}.</p>
     */
    MULTIDIM_RASTER,

    /**
     * Open in shared mode.
     * If set, it allows the sharing of {@link GDALStore} with other callers that have set {@code SHARED}.
     * <abbr>GDAL</abbr> will first consult its list of currently open and shared data sets,
     * and if the description for one exactly matches the filename passed to {@code open}
     * it will be referenced and returned, if {@code open} is called from the same thread.
     *
     * <p>C/C++ source: {@code GDAL_OF_SHARED = 0x20}.</p>
     */
    SHARED,

    /**
     * Emit error message in case of failed open.
     * If set, a failed attempt to open the file will lead to an error message to be reported.
     *
     * <p>C/C++ source: {@code GDAL_OF_VERBOSE_ERROR = 0x40}.</p>
     */
    VERBOSE_ERROR,

    /**
     * Open as internal dataset. Such dataset is not registered in the global list of opened dataset.
     *
     * <p>C/C++ source: {@code GDAL_OF_INTERNAL = 0x80}.</p>
     */
    INTERNAL,

    /**
     * Use a array-based storage strategy for cached blocks.
     *
     * <p>C/C++ source: {@code GDAL_OF_ARRAY_BLOCK_ACCESS = 0x100}.</p>
     */
    ARRAY_BLOCK_ACCESS,

    /**
     * Use a hashset-based storage strategy for cached blocks.
     *
     * <p>C/C++ source: {@code GDAL_OF_HASHSET_BLOCK_ACCESS = 0x200}.</p>
     */
    HASHSET_BLOCK_ACCESS;

    /**
     * The {@code GDALRWFlag.GF_Read} value.
     * <abbr>GDAL</abbr> intentionally fix this value to an absence of open flags.
     */
    static final int READ = 0;

    /**
     * The {@code GDALRWFlag.GF_Write} value.
     * <abbr>GDAL</abbr> intentionally fix this value to the {@link #UPDATE} mask.
     */
    static final int WRITE = 1;

    /**
     * Returns the bitmask to give to <abbr>GDAL</abbr> for a given array of open flags.
     *
     * @param  flags  the open flags, or {@code null} if none.
     * @return the bitmask to give to <abbr>GDAL</abbr>.
     */
    static int mask(final OpenFlag[] flags) {
        int mask = 0;
        if (flags != null) {
            for (OpenFlag flag : flags) {
                mask |= (1 << flag.ordinal());
            }
        }
        return mask;
    }
}
