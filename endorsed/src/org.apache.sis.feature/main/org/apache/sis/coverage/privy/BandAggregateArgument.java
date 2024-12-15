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
package org.apache.sis.coverage.privy;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.lang.reflect.Array;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.IllegalGridGeometryException;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.resources.Errors;


/**
 * Helper class for building a combined domain or range from aggregated sources.
 * This helper class is shared for aggregation operations on different sources:
 * rendered images, grid coverages and resources.
 *
 * <p>Instances of this class should be short-lived.
 * They are used only the time needed for constructing an image or coverage operation.</p>
 *
 * <p>This class can optionally verify if some sources are themselves aggregated images or coverages.
 * This is done by an {@link #unwrap(Consumer)}, which should be invoked in order to get a flattened
 * view of nested aggregations.</p>
 *
 * <p>All methods in this class may return direct references to internal arrays.
 * This is okay if instances of this class are discarded immediately after usage.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <S>  type of objects that are the source of sample dimensions.
 */
@SuppressWarnings("ReturnOfCollectionOrArrayField")     // See class Javadoc.
public final class BandAggregateArgument<S> {
    /**
     * The user-specified sources, usually grid coverages or rendered images.
     * This is initially a copy of the array specified at construction time.
     * This array is modified in-place by {@code validate(…)} methods for
     * removing empty sources and flattening nested aggregations.
     *
     * @see #sources()
     */
    private S[] sources;

    /**
     * Indices of selected bands or sample dimensions for each source.
     * This array is modified in-place by {@code validate(…)} methods
     * for removing empty elements and flattening nested aggregations.
     *
     * The length of this array must be always equal to the {@link #sources} array length.
     * The array is non-null but may contain {@code null} elements for meaning "all bands"
     * before validation. After validation, all null elements are replaced by sequences.
     *
     * @see #bandsPerSource(boolean)
     */
    private int[][] bandsPerSource;

    /**
     * Number of bands for each source source. This information is necessary
     * for determining whether a selection of bands is an identity operation.
     *
     * <p>This field is initially null and assigned on validation.
     * Consequently this field can also be used for checking whether
     * one of the {@code validate(…)} methods has been invoked.</p>
     *
     * @see #completeAndValidate(Function)
     * @see #validate(ToIntFunction)
     */
    private int[] numBandsPerSource;

    /**
     * Number of valid elements in {@link #sources} array after empty elements have been removed.
     * This is initially zero and is set after a {@code validate(…)} method has been invoked.
     */
    private int validatedSourceCount;

    /**
     * Total number of bands. This is the length of the {@link #ranges} list,
     * except that this information is provided even if {@code ranges} is null.
     */
    private int totalBandCount;

    /**
     * Union of all selected bands in all specified sources, or {@code null} if not applicable.
     */
    private List<SampleDimension> ranges;

    /**
     * Translations in units of grid cells to apply for obtaining a grid geometry
     * compatible with the "grid to CRS" transform of a source.
     */
    private long[][] gridTranslations;

    /**
     * Index of a source having the same "grid to CRS" transform than the grid geometry
     * returned by {@link #domain(Function)}. If there is none, then this value is -1.
     */
    private int sourceOfGridToCRS = -1;

    /**
     * A method which may decompose a source in a sequence of deeper sources associated with their bands to select.
     * Shall be set (if desired) before a {@code validate(…)} method is invoked.
     *
     * @see #unwrap(Consumer)
     */
    private Consumer<Unwrapper> unwrapper;

    /**
     * Prepares an argument validator for the given sources and bands arguments.
     * The optional {@code bandsPerSource} argument specifies the bands to select in each source images.
     * That array can be {@code null} for selecting all bands in all source images,
     * or may contain {@code null} elements for selecting all bands of the corresponding image.
     * An empty array element (i.e. zero band to select) discards the corresponding source image.
     *
     * <p>One of the {@code validate(…)} method shall be invoked after this constructor.</p>
     *
     * @param  sources         the sources from which to get the sample dimensions.
     * @param  bandsPerSource  sample dimensions for each source. May contain {@code null} elements.
     */
    public BandAggregateArgument(S[] sources, int[][] bandsPerSource) {
        /*
         * Ensure that both arrays are non-null and have the same length.
         * Copy those arrays because their content will be overwritten.
         */
        ArgumentChecks.ensureNonEmpty("sources", sources);
        final int n = sources.length;
        if (bandsPerSource != null) {
            if (bandsPerSource.length > n) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.TooManyCollectionElements_3,
                        "bandsPerSource", bandsPerSource.length, n));
            }
            bandsPerSource = Arrays.copyOf(bandsPerSource, n);
        } else {
            bandsPerSource = new int[n][];
        }
        this.sources        = sources.clone();
        this.bandsPerSource = bandsPerSource;
    }

    /**
     * Ensures that a {@code validate(…)} method has been invoked (or not).
     *
     * @param  expected  {@code true} if the caller expects validation to be done, or
     *                   {@code false} if the caller expects validation to not be done yet.
     */
    private void checkValidationState(final boolean expected) {
        if ((numBandsPerSource == null) == expected) {
            throw new IllegalStateException();
        }
    }

    /**
     * Specifies a method which, given a source, may decompose that source
     * in a sequence of deeper sources associated with their bands to select.
     * The consumer will be invoked for all sources specified to the constructor.
     * If a source can be decomposed, then the specified consumer should invoke
     * {@code apply(…)} on the given {@code Unwrapper} instance.
     *
     * @param  filter  the method to invoke for getting the sources of an image or coverage.
     */
    public void unwrap(final Consumer<Unwrapper> filter) {
        checkValidationState(false);
        unwrapper = filter;
    }

    /**
     * Asks to the {@linkplain #unwrapper} if the given source can be decomposed into deeper sources.
     *
     * @param  index   index of {@code source} in the {@link #sources} array.
     * @param  source  the source to potentially unwrap.
     * @param  bands   the bands to use in the source. Shall not be {@code null}.
     * @return whether the source has been decomposed.
     */
    private boolean unwrap(int index, S source, int[] bands) {
        if (unwrapper == null) {
            return false;
        }
        final Unwrapper handler = new Unwrapper(index, source, bands);
        unwrapper.accept(handler);
        return handler.done;
    }

    /**
     * Replace a user supplied source by a deeper source with the bands to select.
     * This is used for getting a flattened view of nested aggregations.
     */
    public final class Unwrapper {
        /**
         * Index of {@link #source} in the {@link #sources} array.
         */
        private final int index;

        /**
         * The source to potentially unwrap.
         */
        public final S source;

        /**
         * The bands to use in the source (never {@code null}).
         * This array shall not modified because it may be a reference to an internal array.
         */
        public final int[] bands;

        /**
         * Whether the source has been decomposed in deeper sources.
         */
        private boolean done;

        /**
         * Creates a new instance to be submitted to user supplied {@link #unwrapper}.
         */
        private Unwrapper(final int index, final S source, final int[] bands) {
            this.index  = index;
            this.source = source;
            this.bands  = bands;
        }

        /**
         * Invoke {@code apply(…)} with components that are a subset of an existing aggregate.
         * This is a helper method for decomposing an aggregate into its component.
         *
         * @param sources         all sources of the aggregate to decompose.
         * @param bandsPerSource  selected bands of the aggregate to decompose. May contain null elements.
         * @param getter          same getter as {@link #completeAndValidate(Function)}, used for getting the number of bands.
         */
        public void applySubset(final S[] sources, final int[][] bandsPerSource,
                                final Function<S, List<SampleDimension>> getter)
        {
            @SuppressWarnings("unchecked")
            final S[] components = (S[]) Array.newInstance(sources.getClass().getComponentType(), bands.length);
            final int[][] componentBands = new int[bands.length][];

            int   sourceIndex = -1;
            int[] sourceBands = null;       // Value of `bandsPerSource[sourceIndex]`.
            S     component   = null;       // Value of `sources[sourceIndex]` potentially used as component.
            int   lower=0, upper=0;         // Range of band indices in which `component` is valid.
            for (int i=0; i<bands.length; i++) {
                int band = bands[i];
                if (band < lower) {
                    lower = upper = 0;
                    sourceIndex = -1;
                }
                while (band >= upper) {
                    component   = sources[++sourceIndex];
                    sourceBands = bandsPerSource[sourceIndex];
                    lower       = upper;
                    upper      += (sourceBands != null) ? sourceBands.length : getter.apply(component).size();
                }
                band -= lower;
                if (sourceBands != null) {
                    band = sourceBands[band];
                }
                componentBands[i] = new int[] {band};
                components[i] = component;
            }
            /*
             * Tne same component may be repeated many times in the `sources` array, each time with only one band specified.
             * We rely on the encloding class post-processing for merging multiple references to a single one for each source.
             */
            apply(components, componentBands);
        }

        /**
         * Notifies the enclosing {@code BandAggregateArgument} that the {@linkplain #source}
         * shall be replaced by deeper sources. The {@code componentBands} array specifies
         * the bands to use for each source and shall take in account the {@link #bands} subset.
         *
         * @param components      the deeper sources to use in replacement to {@link #source}.
         * @param componentBands  the bands to use in replacement for {@link #bands}.
         */
        public void apply(final S[] components, final int[][] componentBands) {
            final int n = components.length;
            if (componentBands.length != n) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedArrayLengths));
            }
            if (done) throw new IllegalStateException();
            sources        = ArraysExt.insert(sources,        index+1, n-1);
            bandsPerSource = ArraysExt.insert(bandsPerSource, index+1, n-1);
            System.arraycopy(components,     0, sources,        index, n);
            System.arraycopy(componentBands, 0, bandsPerSource, index, n);
            done = true;
        }
    }

    /**
     * Validates the arguments given to the constructor.
     *
     * @param  counter  method to invoke for counting the number of bands in a source.
     * @throws IllegalArgumentException if some band indices are duplicated or outside their range of validity.
     */
    public void validate(final ToIntFunction<S> counter) {
        checkValidationState(false);
        validate(null, Objects.requireNonNull(counter));
    }

    /**
     * Computes the union of bands in the source given at construction time, then validates.
     * The union of bands is stored in {@link #ranges()}.
     *
     * @param  getter  method to invoke for getting the list of sample dimensions.
     * @throws IllegalArgumentException if some band indices are duplicated or outside their range of validity.
     */
    public void completeAndValidate(final Function<S, List<SampleDimension>> getter) {
        checkValidationState(false);
        ranges = new ArrayList<>();
        validate(Objects.requireNonNull(getter), null);
    }

    /**
     * Clones and validates the arguments given to the constructor.
     * This method ensures that all band indices are in their ranges of validity with no duplicated value.
     * Then this method stores a copy of the band indices, replacing {@code null} values by sequences.
     * If an empty array of bands is specified, then the corresponding source is omitted.
     *
     * <p>Exactly one of {@code getter} or {@code counter} arguments shall be non-null.</p>
     *
     * @param  getter   method to invoke for getting the list of sample dimensions.
     * @param  counter  method to invoke for counting the number of bands in a source.
     * @throws IllegalArgumentException if some band indices are duplicated or outside their range of validity.
     */
    private void validate(final Function<S, List<SampleDimension>> getter, final ToIntFunction<S> counter) {
        final var identityPool = new HashMap<Integer,int[]>();
        numBandsPerSource = new int[sources.length];
next:   for (int i=0; i<sources.length; i++) {          // `sources.length` may change during the loop.
            S source;
            int[] selected;
            List<SampleDimension> sourceBands;
            int numSourceBands;
            RangeArgument range;
            do {
                selected = bandsPerSource[i];
                if (selected != null && selected.length == 0) {
                    // Note that the source is allowed to be null in this particular case.
                    continue next;
                }
                source = sources[i];
                ArgumentChecks.ensureNonNullElement("sources", i, source);
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
                 * This replacement must be done before the check for duplicated image references.
                 * The call to `unwrap` may result in a need to grow `numBandsPerSource` array.
                 */
            } while (unwrap(i, source, selected));
            /*
             * Now that the arguments have been validated, overwrite the array elements.
             * The new values may be written at an index lower than `i` if some empty
             * sources have been excluded.
             */
            if (validatedSourceCount >= numBandsPerSource.length) {
                // Needed if `unwrap(source)` has expanded the sources array.
                numBandsPerSource = Arrays.copyOf(numBandsPerSource, sources.length);
            }
            if (ranges != null) {
                for (int j : selected) {
                    ranges.add(sourceBands.get(j));
                }
            }
            if (range.isIdentity()) {
                int[] previous = identityPool.putIfAbsent(numSourceBands, selected);
                if (previous != null) selected = previous;
            }
            sources          [validatedSourceCount] = source;
            bandsPerSource   [validatedSourceCount] = selected;
            numBandsPerSource[validatedSourceCount] = numSourceBands;
            totalBandCount += range.getNumBands();
            validatedSourceCount++;
        }
    }

    /**
     * For each source which is repeated in consecutive positions, merges the repetition in a single reference.
     * This method does the same work as {@link #mergeDuplicatedSources()}, except that it is restricted to
     * repetitions in consecutive positions. Because of this restriction, the band order is never modified by
     * this method call.
     */
    public void mergeConsecutiveSources() {
        checkValidationState(true);
        for (int i=1; i < validatedSourceCount;) {
            if (sources[i] == sources[i-1]) {
                bandsPerSource[i-1] = ArraysExt.concatenate(bandsPerSource[i-1], bandsPerSource[i]);
                final int remaining = --validatedSourceCount - i;
                System.arraycopy(sources,           i+1, sources,           i, remaining);
                System.arraycopy(bandsPerSource,    i+1, bandsPerSource,    i, remaining);
                System.arraycopy(numBandsPerSource, i+1, numBandsPerSource, i, remaining);
            } else {
                i++;
            }
        }
    }

    /**
     * If the same sources are repeated many times, merges each repetition in a single reference.
     * The {@link #sources()} and {@link #bandsPerSource(boolean)} values are modified in-place.
     * The bands associated to each source reference are merged together, but not necessarily in the same order.
     * Caller must perform a "band select" operation using the array returned by this method
     * in order to reconstitute the band order specified by the user.
     *
     * <p>This method does the same work as {@link #mergeConsecutiveSources()} except that this method can merge
     * sources that are not necessarily at consecutive positions. The sources can be repeated at random positions.
     * But the cost of this flexibility is the possible modification of band order.</p>
     *
     * <h4>Use cases</h4>
     * {@code BandAggregateImage.subset(…)} and
     * {@code BandAggregateGridResource.read(…)}
     * implementations rely on this optimization.
     *
     * @return the bands to specify in a "band select" operation for reconstituting the user-specified band order.
     */
    public int[] mergeDuplicatedSources() {
        checkValidationState(true);
        /*
         * Merge together the bands of all sources that are repeated.
         * The band indices are stored in 64 bits tuples as below:
         *
         *     (band in source) | (band in target aggregate)
         */
        final var mergedBands = new IdentityHashMap<S,long[]>();
        int targetBand = 0;
        for (int i=0; i<validatedSourceCount; i++) {
            final int[] selected = bandsPerSource[i];
            final long[] tuples = new long[selected.length];
            for (int j=0; j<selected.length; j++) {
                tuples[j] = Numerics.tuple(selected[j], targetBand++);
            }
            mergedBands.merge(sources[i], tuples, ArraysExt::concatenate);
        }
        /*
         * Iterate again over the sources, rewriting the arrays with consolidated bands.
         * We need to keep trace of how the bands were reordered.
         */
        final int[] reordered = new int[totalBandCount];
        final int count = validatedSourceCount;
        validatedSourceCount = 0;
        targetBand = 0;
        for (int i=0; i<count; i++) {
            final S      source = sources[i];
            final long[] tuples = mergedBands.remove(source);
            if (tuples != null) {
                int[] selected = bandsPerSource[i];
                if (tuples.length > selected.length) {
                    /*
                     * Found a case where the same source appears two ore more times.
                     * Sort the bands in increasing order for making easier to detect
                     * duplicated values, and because it increases the chances to get
                     * an identity selection (bands in same order) for that source.
                     */
                    Arrays.sort(tuples);
                    selected = new int[tuples.length];
                }
                /*
                 * Rewrite the `selected` array with the potentially merged bands.
                 * If the source was not repeated, `selected` should be unchanged.
                 * But we loop anyway because we also need to write `reordered`.
                 */
                for (int j=0; j < tuples.length; j++) {
                    final long t = tuples[j];
                    reordered[(int) t] = targetBand + j;
                    selected[j] = (int) (t >>> Integer.SIZE);
                }
                targetBand += tuples.length;
                numBandsPerSource[validatedSourceCount] = numBandsPerSource[i];
                bandsPerSource[validatedSourceCount] = selected;
                sources[validatedSourceCount++] = source;
            }
        }
        return reordered;
    }

    /**
     * Returns {@code true} if there is only one source with all bands selected.
     *
     * @return whether {@code sources[0]} could be used directly.
     */
    public boolean isIdentity() {
        checkValidationState(true);
        return validatedSourceCount == 1 && isIdentity(0);
    }

    /**
     * Returns {@code true} if the band selection at the specified index is an identity operation.
     *
     * @param  i  index of a source.
     * @return whether band selection for that source is an identity operation.
     */
    private boolean isIdentity(final int i) {
        final int[] selected = bandsPerSource[i];
        return selected.length == numBandsPerSource[i] && ArraysExt.isRange(0, selected);
    }

    /**
     * Returns all sources coverages as a (potentially modified)
     * copy of the array argument given to the constructor.
     *
     * @return all validated sources.
     */
    public S[] sources() {
        checkValidationState(true);
        return sources = ArraysExt.resize(sources, validatedSourceCount);
    }

    /**
     * Returns the indices of selected bands as (potentially modified)
     * copies of the arrays argument given to the constructor.
     *
     * @param  identityAsNull  whether to use {@code null} elements for meaning "all bands".
     * @return indices of selected sample dimensions for each source.
     *         Never null but may contain null elements if {@code identityAsNull} is {@code true}.
     */
    public int[][] bandsPerSource(final boolean identityAsNull) {
        checkValidationState(true);
        bandsPerSource = ArraysExt.resize(bandsPerSource, validatedSourceCount);
        if (identityAsNull) {
            for (int i=0; i<validatedSourceCount; i++) {
                if (isIdentity(i)) {
                    bandsPerSource[i] = null;
                }
            }
        }
        return bandsPerSource;
    }

    /**
     * Returns the total number of bands.
     *
     * @return total number of bands.
     */
    public int numBands() {
        checkValidationState(true);
        return totalBandCount;
    }

    /**
     * Returns the union of all selected bands in all specified sources.
     * The returned list is modifiable.
     *
     * @return all selected sample dimensions.
     */
    public List<SampleDimension> ranges() {
        if (ranges != null) return ranges;
        throw new IllegalStateException();
    }

    /**
     * Computes the intersection of the grid geometries of all sources.
     * This method also verifies that all grid geometries are compatible.
     *
     * @param  getter  the method to invoke for getting grid geometry from a source.
     * @return intersection of all grid geometries.
     * @throws IllegalGridGeometryException if a grid geometry is not compatible with the others.
     */
    public GridGeometry domain(final Function<S, GridGeometry> getter) {
        checkValidationState(true);
        final var finder = new CommonDomainFinder(PixelInCell.CELL_CORNER);
        finder.setFromGridAligned(Arrays.stream(sources).map(getter).toArray(GridGeometry[]::new));
        sourceOfGridToCRS = finder.sourceOfGridToCRS();
        gridTranslations  = finder.gridTranslations();
        return finder.result();
    }

    /**
     * Returns the translations in units of grid cells to apply for obtaining a grid geometry
     * compatible with the "grid to CRS" transform of a source.
     *
     * <p>The returned array should not be modified because it is not cloned.</p>
     *
     * @return translations from the common grid geometry to all items. This array is not cloned.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public long[][] gridTranslations() {
        checkValidationState(true);
        return gridTranslations;
    }

    /**
     * Returns the index of a source having the same "grid to CRS" transform than the grid geometry
     * returned by {@link #domain(Function)}.
     *
     * @return index of a sources having the same "grid to CRS" than the domain, or -1 if none.
     */
    public int sourceOfGridToCRS() {
        checkValidationState(true);
        return sourceOfGridToCRS;
    }
}
