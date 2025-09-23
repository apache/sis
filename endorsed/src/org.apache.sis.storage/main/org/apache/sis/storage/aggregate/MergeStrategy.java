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
package org.apache.sis.storage.aggregate;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.time.Instant;
import java.time.Duration;
import org.apache.sis.storage.Resource;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.referencing.internal.shared.ExtentSelector;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.internal.shared.Strings;


/**
 * Algorithm to apply when more than one grid coverage can be found at the same grid index.
 * A merge may happen if an aggregated coverage is created with {@link CoverageAggregator},
 * and the extent of some source coverages are overlapping in the dimension to aggregate.
 * {@code MergeStrategy} is ignored if only one coverage is contained in a requested extent.
 *
 * <h2>Example</h2>
 * A collection of {@link GridCoverage} instances may represent the same phenomenon
 * (for example, air temperature) over the same geographic area but at different days.
 * In such case, {@link CoverageAggregator} can build a three-dimensional data cube
 * where each source coverage is located at a different position on the time axis.
 * But if two coverages have overlapping time ranges, and if a user request data in the overlapping region,
 * then there is an ambiguity about which data to return.
 * This {@code MergeStrategy} specifies how to handle this multiplicity.
 *
 * <h2>Default behavior</h2>
 * If no merge strategy is specified, then the default behavior is to throw
 * {@link SubspaceNotSpecifiedException} in situations of ambiguity.
 * An ambiguity happens at {@link GridCoverage#render(GridExtent)} invocation time
 * if more than one source coverage (slice) is found for a specified grid index.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see CoverageAggregator#setMergeStrategy(MergeStrategy)
 *
 * @since 1.3
 */
public abstract class MergeStrategy {
    /**
     * Creates a new merge strategy.
     *
     * @since 1.5
     */
    protected MergeStrategy() {
    }

    /**
     * Builds an {@linkplain org.apache.sis.image.ImageProcessor#overlay image overlay} of all sources.
     * The source images added first have precedence (foreground). Images added last are in background.
     * All bands are referenced or copied verbatim, without special treatment for the alpha channel.
     * In other words, this merge strategy does not handle transparency in overlapping regions.
     *
     * @param  areaOfInterest  range of pixel coordinates, or {@code null} for the union of all images.
     * @return a merge strategy for building an overlay of all source images.
     *
     * @since 1.5
     */
    public static MergeStrategy opaqueOverlay(final Rectangle areaOfInterest) {
        Overlay strategy = Overlay.DEFAULT;
        if (areaOfInterest != null) {
            strategy = new Overlay(strategy.processor, new Rectangle(areaOfInterest));
        }
        return strategy;
    }

    /**
     * The implementation returned by {@link #opaqueOverlay(Rectangle)}.
     */
    private static final class Overlay extends MergeStrategy {
        /** The default instance with no particular area of interest specified. */
        static final Overlay DEFAULT = new Overlay(new ImageProcessor(), null);

        /** The image processor with the configuration to use. */
        final ImageProcessor processor;

        /** The area of interest, or {@code null} if none. */
        private final Rectangle areaOfInterest;

        /** Creates a new strategy for an image in the given area. */
        Overlay(final ImageProcessor processor, final Rectangle areaOfInterest) {
            this.processor = processor;
            this.areaOfInterest = areaOfInterest;
        }

        /** Aggregates the given sources. */
        @Override protected RenderedImage aggregate(RenderedImage[] sources) {
            return processor.overlay(sources, areaOfInterest);
        }

        /** Returns a string representation of this strategy for debugging purposes. */
        @Override public String toString() {
            return Strings.toString(MergeStrategy.class, null,
                    "opaqueOverlay", "areaOfInterest", areaOfInterest);
        }
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
     * and if there are two slices intersecting the TOI, with one slice being a raster of monthly
     * averages the other slice being a raster of daily data, we want the daily data to be selected
     * even if by coincidence the monthly averages is better centered.</div>
     *
     * If the {@code timeGranularity} argument is non-null, then intersections with TOI will be rounded
     * to an integer number of the specified granularity and the last criterion in above list is relaxed.
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
     * <h4>Limitations</h4>
     * Current implementation does not check the vertical dimension.
     * This check may be added in a future version.
     *
     * @param  timeGranularity  the temporal granularity of the Time of Interest (<abbr>TOI</abbr>), or {@code null} if none.
     * @return a merge strategy for selecting a slice based on temporal criteria first.
     */
    public static MergeStrategy selectByTimeThenArea(final Duration timeGranularity) {
        return (timeGranularity != null) ? new FilterByTime(timeGranularity) : FilterByTime.DEFAULT;
    }

    /**
     * The implementation returned by {@link #selectByTimeThenArea(Duration)}.
     */
    private static final class FilterByTime extends MergeStrategy {
        /**
         * The default instance with no time granularity.
         * Temporal positions are compared at their full precision.
         */
        static final FilterByTime DEFAULT = new FilterByTime(null);

        /**
         * Temporal granularity of the time of interest, or {@code null} if none.
         * If non-null, intersections with TOI will be rounded to an integer number of this granularity.
         * This is useful if data are expected at an approximately regular interval
         * and we want to ignore slight variations in the temporal extent declared for each image.
         */
        private final Duration timeGranularity;

        /**
         * Creates a new strategy for the given time granularity.
         */
        FilterByTime(final Duration timeGranularity) {
            this.timeGranularity = timeGranularity;
        }

        /**
         * Selects a single coverage using the strategy represented by this instance.
         * May return an empty array if there is no source that can be used.
         *
         * @param  request     the geographic area and temporal extent requested by user.
         * @param  candidates  grid geometry of all slices that intersect the request. Null elements are ignored.
         * @return index of best slice according the heuristic rules of this {@code MergeStrategy}, or empty.
         */
        @Override
        protected int[] filter(final GridGeometry request, final GridGeometry[] candidates) {
            final var selector = new ExtentSelector<Integer>(
                    request.getGeographicExtent().orElse(null),
                    request.getTemporalExtent());

            if (timeGranularity != null) {
                selector.setTimeGranularity(timeGranularity);
                selector.alternateOrdering = true;
            }
            for (int i=0; i < candidates.length; i++) {
                final GridGeometry candidate = candidates[i];
                if (candidate != null) {
                    final Instant[] t = candidate.getTemporalExtent();
                    final int n = t.length;
                    selector.evaluate(candidate.getGeographicExtent().orElse(null),
                                      (n == 0) ? null : t[0],
                                      (n == 0) ? null : t[n-1], i);
                }
            }
            final Integer best = selector.best();
            return (best != null) ? new int[] {best} : ArraysExt.EMPTY_INT;
        }

        /**
         * Returns the single image selected by the filter.
         * The array length should always be exactly one.
         */
        @Override
        protected RenderedImage aggregate(RenderedImage[] sources) {
            return sources[0];
        }

        /**
         * Returns a string representation of this strategy for debugging purposes.
         */
        @Override
        public String toString() {
            return Strings.toString(MergeStrategy.class, null,
                    "selectByTimeThenArea", "timeGranularity", timeGranularity);
        }
    }

    /**
     * Returns a resource with the same data as the specified resource, but using this merge strategy.
     * If the given resource is an instance created by {@link CoverageAggregator} and uses a different strategy,
     * then a new resource using this merge strategy is returned. Otherwise, the given resource is returned as-is.
     *
     * @param  resource  the resource for which to update the merge strategy, or {@code null}.
     * @return resource with updated merge strategy, or {@code null} if the given resource was null.
     */
    public Resource apply(final Resource resource) {
        if (resource instanceof AggregatedResource) {
            return ((AggregatedResource) resource).apply(this);
        }
        return resource;
    }

    /**
     * Returns the indexes of the coverages to use in the aggregation.
     * The {@code candidates} array contains the grid geometries of all coverages that intersect the request.
     * This method can decide to accept none of those candidates (by returning an empty array), or to select
     * exactly one (for example, based on {@linkplain #selectByTimeThenArea a temporal criterion}),
     * or on the contrary to select all of them, or any intermediate choice.
     *
     * <p>The default implementation selects all candidates (i.e., filter nothing).</p>
     *
     * @param  request     the geographic area and temporal extent requested by user.
     * @param  candidates  grid geometry of all slices that intersect the request.
     * @return indexes of the slices to use according the heuristic rules of this {@code MergeStrategy}.
     *
     * @since 1.5
     */
    protected int[] filter(GridGeometry request, GridGeometry[] candidates) {
        return ArraysExt.range(0, candidates.length);
    }

    /**
     * Aggregates images that have been accepted by the filter. The length of the {@code sources} array
     * is equal or smaller than the length of the index array returned by {@link #filter filter(…)}.
     * The array may be shorter if some images were outside the request, but the array always contains
     * at least one element.
     *
     * @param  sources  the images accepted by the filter.
     * @return the result of the aggregation.
     *
     * @since 1.5
     */
    protected abstract RenderedImage aggregate(RenderedImage[] sources);
}
