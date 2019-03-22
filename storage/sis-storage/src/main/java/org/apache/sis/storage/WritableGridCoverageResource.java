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
 * Subtype of {@linkplain GridCoverageResource} with writing capabilities.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public interface WritableGridCoverageResource extends GridCoverageResource {

    /**
     * Coverage writing options.
     * Common options can be found in {@link CommonOption}.
     * Different {@linkplain DataStoreProvider} may declare specific options
     * for example : compression, version, encryption.
     */
    static interface Option {}

    /**
     * Common writing options.
     */
    static enum CommonOption implements Option {
        /**
         * <ul>
         * <li>If a coverage already exist it will erase and replace existing datas
         * by the new coverage.</li>
         * <li>If there are no previous datas the new datas are inserted
         * as if this option wasn't defined.</li>
         * </ul>
         */
        TRUNCATE,
        /**
         * Update or append existing coverage with new datas.
         * <ul>
         * <li>Areas of the provided {@linkplain GridCoverage} that are within the exisintg {@linkplain GridGeometry}
         * will overwrite the existing datas.</li>
         * <li>Areas outside the existing {@linkplain GridGeometry} will result in expanding the
         * {@linkplain GridGeometry} with the new datas.</li>
         * <li>If there are no previous datas the new datas are inserted
         * as if this option wasn't defined.</li>
         * </ul>
         */
        UPDATE
    }

    /**
     * Write new datas in the resource.
     *
     * @param coverage new datas to write, should not be null
     * @param options specific writing options
     */
    void write(GridCoverage coverage, Option ... options);

}
