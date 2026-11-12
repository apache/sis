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
package org.apache.sis.storage.hdf;

import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.storage.DataStoreException;


/**
 * A dataset of an <abbr>HDF5</abbr> file: a multi-dimensional array of values.
 * Details such as the datatype, the storage layout or the filters are not part of this interface,
 * they are shown in the {@linkplain HDF5Store#getNativeMetadata() native metadata}.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Dataset extends Node {
    /**
     * Returns the extent of this dataset, with one dimension per dataspace dimension.
     * A scalar dataset has an extent of dimension zero.
     *
     * @return the extent of this dataset in dataspace coordinates.
     */
    GridExtent getExtent();

    /**
     * Returns the Java type of the values of this dataset.
     * This is a primitive type such as {@code double.class} for numerical values,
     * {@code String.class} for character strings, {@code Object[].class} for compound values
     * or {@code ObjectReference.class} for references.
     *
     * @return the Java type of the values of this dataset.
     */
    Class<?> getValueClass();

    /**
     * Reads the values in the given extent.
     * The result is a multi-dimensional Java array with one level per dimension,
     * having elements of the {@linkplain #getValueClass() value class},
     * or a single value for a scalar dataset.
     *
     * @param  extent  region to read in dataspace coordinates, or {@code null} for the whole dataset.
     * @return the values in the given extent.
     * @throws DataStoreException if the extent is not contained in the dataset extent,
     *         if the data layout or storage is not supported, or if an I/O error occurred.
     */
    Object read(GridExtent extent) throws DataStoreException;
}
