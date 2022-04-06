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
package org.apache.sis.storage;

import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;


/**
 * A {@link GridCoverageResource} with writing capabilities. {@code WritableGridCoverageResource} inherits the reading
 * capabilities from its parent and adds a {@linkplain #write write} operation. Some aspects of the write operation can
 * be controlled by options, which may be {@link DataStore}-specific.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public interface WritableGridCoverageResource extends GridCoverageResource {
    /**
     * Configuration of the process of writing a coverage in a data store.
     * By default, the {@linkplain #write write operation} is conservative: no operation is executed
     * if it would result in data lost. {@code Option} allows to modify this behavior for example by
     * allowing the {@linkplain CommonOption#TRUNCATE replacement} of previous data.
     * Options can also configure other aspects like compression, version or encryption.
     *
     * <p>Some options may be {@link DataStore}-specific.
     * Options that may apply to any data store are provided in the {@link CommonOption} enumeration.
     * Other options may be defined by the {@linkplain DataStoreProvider} of specific formats.</p>
     *
     * @author  Johann Sorel (Geomatys)
     * @version 1.0
     * @since   1.0
     * @module
     */
    interface Option {}

    /**
     * Write options that may apply to any data store. The coverage {@linkplain #write write operation}
     * is configured by instances of {@link Option}, sometime in a {@link DataStore}-specific basis.
     * This {@code CommonOption} enumeration provides options that do not depend on the data store.
     *
     * @author  Johann Sorel (Geomatys)
     * @version 1.0
     * @since   1.0
     * @module
     */
    enum CommonOption implements Option {
        /**
         * Instructs the write operation to replace existing coverage if one exists.
         * By default the {@linkplain #write write operation} does not overwrite existing data.
         * If this option is specified, then there is a choice:
         *
         * <ul>
         *   <li>If a coverage already exists in the {@link GridCoverageResource}, then it will be erased.
         *       The existing data will be replaced by the new coverage.</li>
         *   <li>If there are no existing coverages in the {@link GridCoverageResource},
         *       then the new coverage will be inserted as if this option was not provided.</li>
         * </ul>
         */
        TRUNCATE,

        /**
         * Updates or appends existing coverage with new data.
         * If a coverage already exists when the {@linkplain #write write operation} is executed with this option, then:
         *
         * <ul>
         *   <li>Areas of the provided {@link GridCoverage} that are within the existing {@link GridGeometry}
         *       will overwrite the existing data.</li>
         *   <li>Areas outside the existing {@link GridGeometry} will result in expanding the grid geometry
         *       with the new data.</li>
         * </ul>
         *
         * If there are no previous coverages, then the new coverage is inserted as if this option was not provided.
         */
        UPDATE
    }

    /**
     * Writes a new coverage in the data store for this resource. If a coverage already exists for this resource,
     * then the behavior of this method is determined by the given options. If no option is specified, the default
     * behavior is to fail if writing a coverage would cause an existing coverage to be overwritten.
     * This behavior can be modified by requesting the {@linkplain CommonOption#TRUNCATE replacement}
     * or {@linkplain CommonOption#UPDATE update} of existing coverages.
     *
     * @param  coverage  new data to write in the data store for this resource.
     * @param  options   configuration of the write operation. May be {@link DataStore}-specific options
     *                   (e.g. for compression, encryption, <i>etc</i>).
     * @throws DataStoreException if an error occurred while writing data in the underlying data store.
     */
    void write(GridCoverage coverage, Option... options) throws DataStoreException;
}
