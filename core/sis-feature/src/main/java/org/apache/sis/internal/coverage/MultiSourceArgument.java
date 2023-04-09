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
package org.apache.sis.internal.coverage;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.IllegalGridGeometryException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ComparisonMode;


/**
 * Helper class for building a list of sample dimensions from aggregated sources.
 * This helper class is shared for aggregation operations on different sources:
 * rendered images, grid coverages and resources.
 *
 * <p>Instances of this class should be short-lived.
 * They are used only the time needed for constructing an image or coverage operation.</p>
 *
 * <p>This class can optionally verify if a source is itself an aggregated image or coverage.
 * This is done by an "unwrapper", which should be specified in order to provide a flattened
 * view of nested aggregations.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @param  <S>  type of objects that are the source of sample dimensions.
 *
 * @since 1.4
 */
public final class MultiSourceArgument<S> {
    /**
     * The sources of sample dimensions with empty sources removed.
     * After a {@code validate(…)} method has been invoked, this array become a
     * (potentially modified) copy of the array argument given to the constructor.
     */
    private S[] sources;

    /**
     * Indices of selected sample dimensions for each source.
     * After a {@code validate(…)} method has been invoked, this array become a
     * (potentially modified) copy of the array argument given to the constructor,
     * with the same length than {@link #sources} and all elements themselves copied.
     * The array is non-null but may contain {@code null} elements for meaning "all bands".
     */
    private int[][] bandsPerSource;

    /**
     * Whether to allow null elements in {@link #bandsPerSource} for meaning "all bands".
     */
    private boolean identityAsNull;

    /**
     * A function which, given an (image, bands) pair, may return the source of the image.
     * If the source is returned, then the bands array is updated with the indices in that
     * source.
     */
    private BiFunction<S,int[],S> unwrapper;

    /**
     * Union of all selected bands in all specified sources, or {@code null} if not applicable.
     */
    private List<SampleDimension> ranges;

    /**
     * Total number of bands. This is the length of the {@link #ranges} list,
     * except that this information is provided even if {@code ranges} is null.
     */
    private int numBands;

    /**
     * Index of a source having the same "grid to CRS" transform than the grid geometry
     * returned by {@link #domain(Function)}. If there is none, then this value is -1.
     */
    private int sourceOfGridToCRS;

    /**
     * Whether one of the {@code validate(…)} methods has been invoked.
     */
    private boolean validated;

    /**
     * Prepares an argument validator for the given sources and bands arguments.
     * One of the {@code validate(…)} method should be invoked after this constructor.
     *
     * @param  sources         the sources from which to get the sample dimensions.
     * @param  bandsPerSource  sample dimensions for each source. May contain {@code null} elements.
     */
    public MultiSourceArgument(final S[] sources, final int[][] bandsPerSource) {
        this.sources = sources;
        this.bandsPerSource = bandsPerSource;
        sourceOfGridToCRS = -1;
    }

    /**
     * Requests the use of {@code null} elements for meaning "all bands".
     * The null elements can appear in the {@link #bandsPerSource()} array,
     * but the array itself will still never null.
     */
    public void identityAsNull() {
        if (validated) throw new IllegalStateException();
        identityAsNull = true;
    }

    /**
     * Specifies a function which, given an (image, bands) pair, may return the source of the image.
     * If the source is returned, then the bands array is updated with the indices in that source.
     * The function shall modify the given {@code int[]} in-place and return the new source,
     * or return the {@code S} value unchanged if no unwrapping has been done.
     *
     * @param  filter  the function to invoke for getting the source of an image or coverage.
     */
    public void unwrap(final BiFunction<S,int[],S> filter) {
        if (validated) throw new IllegalStateException();
        unwrapper = filter;
    }

    /**
     * Clones and validates the arguments given to the constructor.
     *
     * @param  counter  method to invoke for counting the number of bands in a source.
     * @throws IllegalArgumentException if some band indices are duplicated or outside their range of validity.
     */
    public void validate(final ToIntFunction<S> counter) {
        validate(null, Objects.requireNonNull(counter));
    }

    /**
     * Clones and validates the arguments given to the constructor.
     * Also computes the union of bands in the sources given at construction time.
     * The union result is stored in {@link #ranges}.
     *
     * @param  getter  method to invoke for getting the list of sample dimensions.
     * @throws IllegalArgumentException if some band indices are duplicated or outside their range of validity.
     */
    public void validate(final Function<S, List<SampleDimension>> getter) {
        ranges = new ArrayList<>();
        validate(Objects.requireNonNull(getter), null);
    }

    /**
     * Computes the union of bands in the sources given at construction time.
     * This method also verifies the indices in band arguments.
     * Sources with no indices are removed from the iterator.
     *
     * <p>Exactly one of {@code getter} or {@code count} arguments shall be non-null.</p>
     *
     * @param  getter   method to invoke for getting the list of sample dimensions.
     * @param  counter  method to invoke for counting the number of bands in a source.
     * @throws IllegalArgumentException if some band indices are duplicated or outside their range of validity.
     */
    private void validate(final Function<S, List<SampleDimension>> getter, final ToIntFunction<S> counter) {
        /*
         * Ensure that both arrays are non-null and have the same length.
         * Copy those arrays as their content may be overwritten.
         */
        ArgumentChecks.ensureNonEmpty("sources", sources);
        final int sourceCount = sources.length;
        if (bandsPerSource != null) {
            if (bandsPerSource.length > sourceCount) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.TooManyCollectionElements_3,
                        "bandsPerSource", bandsPerSource.length, sourceCount));
            }
            bandsPerSource = Arrays.copyOf(bandsPerSource, sourceCount);
        } else {
            bandsPerSource = new int[sourceCount][];
        }
        sources = sources.clone();
        /*
         * Compute the number of sources and the total number of bands.
         * This loop ensures that all band indices are in their ranges of validity
         * with no duplicated value, then stores a copy of the band indices or null.
         * If an empty array of bands is specified, then the source is omitted.
         */
        final HashMap<Integer,int[]> pool = identityAsNull ? null : new HashMap<>();
        int filteredCount = 0;
        for (int i=0; i<sourceCount; i++) {
            int[] selected = bandsPerSource[i];
            if (selected != null && selected.length == 0) {
                // Note that the source is allowed to be null in this particular case.
                continue;
            }
            S source = sources[i];
            ArgumentChecks.ensureNonNullElement("sources", i, source);
            /*
             * Get the number of bands, or optionally the bands themselves.
             * This information is required before to validate arguments.
             */
            List<SampleDimension> sourceBands;
            int numSourceBands;
            RangeArgument range;
            do {
                if (getter != null) {
                    sourceBands = getter.apply(source);
                    numSourceBands = sourceBands.size();
                } else {
                    sourceBands = null;
                    numSourceBands = counter.applyAsInt(source);
                }
                range = RangeArgument.validate(numSourceBands, selected, null);
                selected = range.getSelectedBands();
                /*
                 * Verify if the source is a nested aggregation, in order to get a flattened view.
                 * This replacement must be done before the optimization for consecutive images.
                 */
            } while (unwrapper != null && source != (source = unwrapper.apply(source, selected)));
            /*
             * Store now the sample dimensions before the `selected` array get modified.
             */
            if (ranges != null) {
                for (int b : selected) {
                    ranges.add(sourceBands.get(b));
                }
            }
            /*
             * If the source in current iteration is the same than the previous source, merge the bands together.
             * The `BandAggregateGridResource.read(…)` implementation relies on that optimization.
             */
            if (filteredCount > 0 && sources[filteredCount-1] == source) {
                final int[] previous = bandsPerSource[--filteredCount];
                ArgumentChecks.ensureNonNullElement("bandsPerSource", filteredCount,   previous);
                ArgumentChecks.ensureNonNullElement("bandsPerSource", filteredCount+1, selected);
                numBands -= previous.length;   // Rollback the value added in previous iteration.

                final int[] merged = Arrays.copyOf(previous, previous.length + selected.length);
                System.arraycopy(selected, 0, merged, previous.length, selected.length);
                range = RangeArgument.validate(numSourceBands, merged, null);
                selected = range.getSelectedBands();
            }
            /*
             * Store a copy of the `bandsPerSource` argument given at construction time.
             * Its validation has been done by `RangeArgument.validate(…)` above calls.
             */
            if (range.isIdentity()) {
                if (pool != null) {
                    int[] previous = pool.putIfAbsent(numSourceBands, selected);
                    if (previous != null) selected = previous;
                } else {
                    selected = null;
                }
            }
            bandsPerSource[filteredCount] = selected;
            sources[filteredCount++] = source;
            numBands += range.getNumBands();
        }
        sources = ArraysExt.resize(sources, filteredCount);
        bandsPerSource = ArraysExt.resize(bandsPerSource, filteredCount);
        validated = true;
    }

    /**
     * Returns {@code true} if there is only one source with all bands selected.
     *
     * @return whether {@code sources[0]} could be used directly.
     */
    public boolean isIdentity() {
        return bandsPerSource.length == 1 && bandsPerSource[0] == null;
    }

    /**
     * Returns all sources coverages as a (potentially modified)
     * copy of the array argument given to the constructor.
     *
     * @return all validated sources.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public S[] sources() {
        if (validated) return sources;
        throw new IllegalStateException();
    }

    /**
     * Computes the intersection of the grid geometries of all sources.
     * This method also verifies that all grid geometries are compatible.
     *
     * @param  getter  the method to invoke for getting grid geometry from a source.
     * @return intersection of all grid geometries.
     * @throws IllegalGridGeometryException if a grid geometry is not compatible with the others.
     *
     * @todo Current implementation requires that all grid geometry are equal. We need to relax that.
     */
    public GridGeometry domain(final Function<S, GridGeometry> getter) {
        GridGeometry intersection = getter.apply(sources[0]);
        for (int i=1; i < sources.length; i++) {
            if (!intersection.equals(getter.apply(sources[i]), ComparisonMode.IGNORE_METADATA)) {
                throw new IllegalGridGeometryException("Not yet supported on coverages with different grid geometries.");
            }
        }
        sourceOfGridToCRS = 0;      // TODO: to be computed when different grid geometries will be allowed. Prefer widest extent.
        return intersection;
    }

    /**
     * Returns the union of all selected bands in all specified sources.
     * The returned list is modifiable.
     *
     * @return all selected sample dimensions.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<SampleDimension> ranges() {
        if (ranges != null) return ranges;
        throw new IllegalStateException();
    }

    /**
     * Returns the total number of bands.
     *
     * @return total number of bands.
     */
    public int numBands() {
        if (validated) return numBands;
        throw new IllegalStateException();
    }

    /**
     * Returns the indices of selected bands as (potentially modified)
     * copies of the arrays argument given to the constructor.
     *
     * @return indices of selected sample dimensions for each source.
     *         Never null but may contain null elements if {@link #identityAsNull()} has been invoked.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public int[][] bandsPerSource() {
        if (validated) return bandsPerSource;
        throw new IllegalStateException();
    }

    /**
     * Returns the index of a source having the same "grid to CRS" transform than the grid geometry
     * returned by {@link #domain(Function)}.
     *
     * @return index of a sources having the same "grid to CRS" than the domain, or -1 if none.
     */
    public int sourceOfGridToCRS() {
        return sourceOfGridToCRS;
    }
}
