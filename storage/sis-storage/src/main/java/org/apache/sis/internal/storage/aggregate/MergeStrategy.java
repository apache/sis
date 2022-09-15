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

import java.time.Instant;
import java.time.Duration;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.internal.referencing.ExtentSelector;
import org.apache.sis.internal.util.Strings;


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
 * If no merge strategy is specified, then the default behavior is to throw
 * {@link SubspaceNotSpecifiedException} when the {@link GridCoverage#render(GridExtent)} method
 * is invoked and more than one source coverage (slice) is found for a specified grid index.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
public final class MergeStrategy {
    /**
     * Temporal granularity of the time of interest, or {@code null} if none.
     * If non-null, intersections with TOI will be rounded to an integer amount of this granularity.
     * This is useful if data are expected at an approximately regular interval
     * and we want to ignore slight variations in the temporal extent declared for each image.
     */
    private final Duration timeGranularity;

    /**
     * Creates a new merge strategy. This constructor is private for now because
     * we have not yet decided a callback API for custom merges.
     */
    private MergeStrategy(final Duration timeGranularity) {
        this.timeGranularity = timeGranularity;
    }

    /**
     * Selects a single slice using criteria based first on temporal extent, then on geographic area.
     * This strategy applies the following rules, in order:
     *
     * <ol>
     *   <li>Slice having largest intersection with the time of interest (TOI) is selected.</li>
     *   <li>If two or more slices have the same intersection with TOI,
     *       then the one with less "overtime" (time outside TOI) is selected.</li>
     *   <li>If two or more slices are considered equal after above criteria,
     *       then the one best centered on the TOI is selected.</li>
     * </ol>
     *
     * <div class="note"><b>Rational:</b>
     * the "smallest time outside" criterion (rule 2) is before "best centered" criterion (rule 3)
     * because of the following scenario: if a user specifies a "time of interest" (TOI) of 1 day
     * and if there is two slices intersecting the TOI, with one slice being a raster of monthly
     * averages the other slice being a raster of daily data, we want the daily data to be selected
     * even if by coincidence the monthly averages is better centered.</div>
     *
     * If the {@code timeGranularity} argument is non-null, then intersections with TOI will be rounded
     * to an integer amount of the specified granularity and the last criterion in above list is relaxed.
     * This is useful when data are expected at an approximately regular time interval (for example one remote
     * sensing image per day) and we want to ignore slight variations in the temporal extent declared for each image.
     *
     * <p>If there is no time of interest, or the slices do not declare time range,
     * or some slices are still at equality after application of above criteria,
     * then the selection continues on the basis of geographic criteria:</p>
     *
     * <ol>
     *   <li>Largest intersection with the area of interest (AOI) is selected.</li>
     *   <li>If two or more slices have the same intersection area with AOI, then the one with the less
     *       "irrelevant" material is selected. "Irrelevant" material are area outside the AOI.</li>
     *   <li>If two or more slices are considered equal after above criteria,
     *       the one best centered on the AOI is selected.</li>
     *   <li>If two or more slices are considered equal after above criteria,
     *       then the first of those candidates is selected.</li>
     * </ol>
     *
     * If two slices are still considered equal after all above criteria, then an arbitrary one is selected.
     *
     * @param  timeGranularity  the temporal granularity of the Time of Interest (TOI), or {@code null} if none.
     * @return a merge strategy for selecting a slice based on temporal criteria first.
     */
    public static MergeStrategy selectByTimeThenArea(final Duration timeGranularity) {
        return new MergeStrategy(timeGranularity);
    }

    /**
     * Applies the merge using the strategy represented by this instance.
     * Current implementation does only a slice selection.
     * A future version may allow real merge operations.
     *
     * @param  request     the geographic area and temporal extent requested by user.
     * @param  candidates  grid geometry of all slices that intersect the request.
     * @return index of best slice according the heuristic rules of this {@code MergeStrategy}.
     */
    final Integer apply(final GridGeometry request, final GridGeometry[] candidates) {
        final ExtentSelector<Integer> selector = new ExtentSelector<>(
                request.getGeographicExtent().orElse(null),
                request.getTemporalExtent());

        if (timeGranularity != null) {
            selector.setTimeGranularity(timeGranularity);
            selector.alternateOrdering = true;
        }
        for (int i=0; i < candidates.length; i++) {
            final GridGeometry candidate = candidates[i];
            final Instant[] t = candidate.getTemporalExtent();
            final int n = t.length;
            selector.evaluate(candidate.getGeographicExtent().orElse(null),
                              (n == 0) ? null : t[0],
                              (n == 0) ? null : t[n-1], i);
        }
        return selector.best();
    }

    /**
     * Returns a string representation of this strategy for debugging purposes.
     *
     * @return string representation of this strategy.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "algo", "selectByTimeThenArea", "timeGranularity", timeGranularity);
    }
}
