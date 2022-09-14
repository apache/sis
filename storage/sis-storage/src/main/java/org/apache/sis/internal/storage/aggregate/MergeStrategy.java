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
package org.apache.sis.internal.storage.aggregate;

import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;


/**
 * Algorithm to apply when more than one grid coverage can be found at the same grid index.
 * A merge may happen if an aggregated coverage is created with {@link CoverageAggregator},
 * and the extent of some source coverages are overlapping in the dimension to aggregate.
 *
 * <div class="note"><b>Example:</b>
 * a collection of {@link GridCoverage} instances may represent the same phenomenon
 * (for example Sea Surface Temperature) over the same geographic area but at different dates and times.
 * {@link CoverageAggregator} can be used for building a single data cube with a time axis.
 * But if two coverages have overlapping time ranges, and if an user request data in the overlapping region,
 * then the aggregated coverages have more than one source coverages capable to provide the requested data.
 * This enumeration specify how to handle this multiplicity.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
public enum MergeStrategy {
    /**
     * Do not perform any merge. It will cause a {@link SubspaceNotSpecifiedException} to be thrown by
     * {@link GridCoverage#render(GridExtent)} if more than one source coverage is found for a specified slice.
     *
     * <p>This is the default strategy.</p>
     */
    FAIL
}
