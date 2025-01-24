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
package org.apache.sis.map.coverage;

import java.util.Arrays;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.text.NumberFormat;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.storage.RasterLoadingStrategy;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.io.TableAppender;
import org.apache.sis.system.Configuration;
import org.apache.sis.pending.jdk.JDK16;


/**
 * A helper class for reading {@link GridCoverage} instances at various resolutions.
 * The resolutions are inferred from {@link GridCoverageResource#getResolutions()},
 * using default values if necessary. The objective CRS does not need to be the same
 * than the coverage CRS, in which case transformations are applied at the point in
 * the center of the display bounds.
 *
 * <h2>Multi-threading</h2>
 * Instances of this class are immutable (except for the cache) and safe for use by multiple threads.
 * However, it assumes that the {@link GridCoverageResource} given to the constructor is also thread-safe;
 * this class does not synchronize accesses to the resource (because it may be used outside this class anyway).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class MultiResolutionCoverageLoader {
    /**
     * Approximate size in pixels of the pyramid level having coarsest resolution.
     * This is used by {@link #defaultResolutions(GridGeometry, double[])} when no
     * resolution levels are explicitly given by the {@linkplain #resource}.
     */
    @Configuration
    private static final int DEFAULT_SIZE = 512;

    /**
     * Value of log₂(rₙ₊₁/rₙ) where rₙ is the resolution at a level and rₙ₊₁ is the resolution at the coarser level.
     * The usual value is 1, which means that there is a scale factor of 2 between each level. We use a higher value
     * because if the {@linkplain #resource} did not declared a pyramid, reading coverages at low resolution may be
     * as costly as high resolution. in that case, we want to reduce the number of read operations.
     */
    private static final int DEFAULT_SCALE_LOG = 3;         // Scale factor of 2³ = 8.

    /**
     * Arbitrary number of levels if we cannot compute it from {@link #DEFAULT_SIZE} and {@link #DEFAULT_SCALE_LOG}.
     * Reminder: the multiplication factor between two levels is 2^{@value #DEFAULT_SCALE_LOG}, so the resolution
     * goes down very fast.
     */
    private static final int DEFAULT_NUM_LEVELS = 4;

    /**
     * The resource from which to read grid coverages.
     */
    public final GridCoverageResource resource;

    /**
     * Squares of resolution at each pyramid level, from finest (smaller numbers) to coarsest (largest numbers).
     * This is same same order as {@link GridCoverageResource#getResolutions()}. For a given level, the array
     * {@code resolutionSquared[level]} gives the squares of the resolution for each CRS dimension.
     */
    private final double[][] resolutionSquared;

    /**
     * The weak or soft references to coverages for each pyramid level.
     * The array length is at least 1, even if {@link #resolutionSquared} is empty.
     * Accesses to this array should be synchronized on {@code coverages}.
     */
    private final Reference<GridCoverage>[] coverages;

    /**
     * The area of interest in any CRS (transformations will be applied as needed),
     * or {@code null} for not restricting the coverage to a sub-area.
     */
    private final Envelope areaOfInterest;

    /**
     * 0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     */
    private final int[] readRanges;

    /**
     * Creates a new loader of grid coverages from the given resource. The loader assumes a pyramid with
     * resolutions declared by the given resource if present, or computes default resolutions otherwise.
     *
     * @param  resource  the resource from which to read grid coverages. Should be thread-safe.
     * @param  domain    desired spatiotemporal region in any CRS, or {@code null} for no sub-area.
     * @param  range     0-based indices of sample dimensions to read, or {@code null} for all.
     * @throws DataStoreException if an error occurred while querying the resource for resolutions.
     */
    @SuppressWarnings({"unchecked","rawtypes"})         // Generic array creation.
    public MultiResolutionCoverageLoader(final GridCoverageResource resource, final Envelope domain,
                                         final int[] range) throws DataStoreException
    {
        this.resource  = resource;
        areaOfInterest = domain;
        readRanges     = range;
        double[][] resolutions = resource.getResolutions().toArray(double[][]::new);
        if (resolutions.length <= 1) {
            final GridGeometry gg = resource.getGridGeometry();
            if (resolutions.length != 0) {
                resolutions = defaultResolutions(gg, resolutions[0]);
            } else if (gg.isDefined(GridGeometry.RESOLUTION)) {
                resolutions = defaultResolutions(gg, gg.getResolution(true));
            }
        }
        resolutionSquared = resolutions;
        for (final double[] r : resolutions) {
            for (int i=0; i<r.length; i++) {
                r[i] *= r[i];
            }
        }
        coverages = new Reference[Math.max(resolutions.length, 1)];
        resource.setLoadingStrategy(RasterLoadingStrategy.AT_GET_TILE_TIME);
    }

    /**
     * Computes default resolutions starting from the given finest level.
     * This method uses a scale factor determined by {@link #DEFAULT_SCALE_LOG} between each level.
     * The coarsest level will have a size of approximately {@value #DEFAULT_SIZE} pixels.
     *
     * @param  envelope  bounding box of the coverage in units of the coverage CRS.
     * @param  base      resolution of the finest level.
     * @return default resolutions from finest to coarsest. The first element is always {@code base}.
     */
    private static double[][] defaultResolutions(final GridGeometry gg, double[] base) {
        /*
         * Estimate the number of levels in a pyramid starting from a level with `base` resolution
         * up to a level having approximately `DEFAULT_SIZE` pixels, assuming that the resolution
         * at each level is 8× the resolution at previous level.
         */
        int numLevels = 1;
        if (gg.isDefined(GridGeometry.ENVELOPE)) {
            final Envelope envelope = gg.getEnvelope();
            for (int i = envelope.getDimension(); --i >= 0;) {
                // Multiplication factor from finest resolution to coarsest one.
                final double f = envelope.getSpan(i) / (DEFAULT_SIZE * base[i]);
                int n = Math.getExponent(f);                    // floor(log₂(f))
                if (n < Double.MAX_EXPONENT && (n /= DEFAULT_SCALE_LOG) > numLevels) {
                    numLevels = n;
                }
            }
        } else {
            numLevels = DEFAULT_NUM_LEVELS;     // Arbitrary number of levels if we cannot compute it.
        }
        /*
         * Build the arrays of resolutions from finest to coarsest.
         * The `base` array is cloned then updated to become the base of next level.
         */
        final double[][] resolutions = new double[numLevels][];
        resolutions[0] = base;
        for (int j=1; j<numLevels; j++) {
            resolutions[j] = base = base.clone();
            for (int i=0; i<base.length; i++) {
                base[i] *= (1 << DEFAULT_SCALE_LOG);
            }
        }
        return resolutions;
    }

    /**
     * Returns the maximal level (the level with coarsest resolution).
     */
    final int getLastLevel() {
        return Math.max(resolutionSquared.length - 1, 0);
    }

    /**
     * Returns the pyramid level for a zoom defined by the given "objective to display" transform.
     * Only the scale factors of the given transform will be considered; translations are ignored.
     *
     * @param  dataToObjective     transform from data CRS to the CRS for rendering, or {@code null} if none.
     * @param  objectiveToDisplay  transform used for rendering the coverage on screen.
     * @param  objectivePOI        point where to compute resolution, in coordinates of objective CRS.
     *                             Can be null if {@code dataToObjective} is null or linear.
     * @return pyramid level for the zoom determined by the given transform. Finest level is 0.
     * @throws TransformException if an error occurred while computing resolution from given transforms.
     */
    final int findPyramidLevel(final MathTransform dataToObjective, final LinearTransform objectiveToDisplay,
                               final DirectPosition objectivePOI) throws TransformException
    {
        int level = getLastLevel();
        if (level != 0) {
            final LinearTransform displayToObjective = objectiveToDisplay.inverse();
            final Matrix m = displayToObjective.getMatrix();
            final Matrix d;
            if (dataToObjective != null && !dataToObjective.isIdentity()) {
                d = dataToObjective.inverse().derivative(objectivePOI);
            } else {
                d = null;
            }
            final int srcDim = m.getNumCol() - 1;
            final int objDim = m.getNumRow() - 1;                       // -1 for ignoring the translation column.
            final int tgtDim = (d != null) ? d.getNumRow() : objDim;    // No -1 because `d` is not a transform.
dimensions: for (int j=0; j<tgtDim; j++) {
                double sum = 0;
                for (int i=0; i<srcDim; i++) {
                    double e;
                    if (d == null) {
                        e = m.getElement(j,i);
                    } else {
                        /*
                         * Compute the value of `(d × m).getElement(j,i)` where (d × m) is "display to objective"
                         * transform followed by "objective to data". We do the multiplication inline here instead
                         * of invoking `d.multiply(m)` because the two matrices do not have compatible size:
                         * `m` is an affine transform (including translations) while `d` is a Jacobian matrix.
                         * It also allows to skip some calculations if `level` become 0 early.
                         */
                        e = 0;
                        for (int k=0; k<objDim; k++) {
                            e = Math.fma(d.getElement(j,k), m.getElement(k,i), e);
                        }
                    }
                    sum += e * e;
                }
                /*
                 * Cannot use `Arrays.binarySearch(…)` because elements are not guaranteed to be sorted.
                 * Even if `GridCoverageResource.getResolutions()` contract said "finest to coarsest",
                 * it may not be possible to respect this condition on all dimensions in same time.
                 * The main goal is to have a `level` value as high as possible while having a resolution
                 * equals or better than `sum`.
                 */
                int levelOfMin = level;
                double minimum = Double.POSITIVE_INFINITY, r;
                while ((r = resolutionSquared[level][j]) > sum) {
                    if (r < minimum) {
                        minimum = r;
                        levelOfMin = level;
                    }
                    if (level == 0) {
                        level = levelOfMin;
                        break dimensions;
                    }
                    level--;
                }
            }
        }
        return level;
    }

    /**
     * Returns the coverage at the given level if it is present in the cache, or loads and caches it otherwise.
     *
     * @param  level  pyramid level of the desired coverage.
     * @return the coverage at the specified level (never null).
     * @throws DataStoreException if an error occurred while loading the coverage.
     */
    public final GridCoverage getOrLoad(final int level) throws DataStoreException {
        synchronized (coverages) {
            final Reference<GridCoverage> ref = coverages[level];
            if (ref != null) {
                final GridCoverage coverage = ref.get();
                if (coverage != null) return coverage;
                coverages[level] = null;
            }
        }
        GridGeometry domain = null;
        if (resolutionSquared.length != 0) {
            final double[] resolutions = resolutionSquared[level].clone();
            for (int i=0; i<resolutions.length; i++) {
                resolutions[i] = Math.sqrt(resolutions[i]);
            }
            final MathTransform gridToCRS = MathTransforms.scale(resolutions);
            domain = new GridGeometry(PixelInCell.CELL_CORNER, gridToCRS, areaOfInterest, GridRoundingMode.ENCLOSING);
        }
        final GridCoverage coverage = resource.read(domain, readRanges);
        /*
         * Cache and return the coverage. The returned coverage may be a different instance
         * if another coverage has been cached concurrently for the same level.
         */
        synchronized (coverages) {
            final Reference<GridCoverage> ref = coverages[level];
            if (ref != null) {
                final GridCoverage c = ref.get();
                if (c != null) return c;
            }
            coverages[level] = new SoftReference<>(coverage);
        }
        return coverage;
    }

    /**
     * If the a grid coverage for the given domain and range is in the cache, returns that coverage.
     * Otherwise loads the coverage and eventually caches it. The caching happens only if the given
     * domain and range are managed by this loader.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  range   0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and range.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    public final GridCoverage getOrLoad(GridGeometry domain, final int[] range) throws DataStoreException {
        if (domain == null && areaOfInterest == null && Arrays.equals(readRanges, range)) {
            /*
             * Fot now we leverage the cache only at level 0.
             * Future versions of this class may try to use the cache at other levels too.
             */
            return getOrLoad(0);
        }
        if (domain == null) {
            domain = resource.getGridGeometry();
        }
        return resource.read(domain, readRanges);
    }

    /**
     * Returns a string representation of this loader for debugging purpose.
     * Default implementation formats the resolution thresholds in a table
     * with "cached" word after the level having a cached coverage.
     *
     * @return a string representation of this loader.
     */
    @Override
    public String toString() {
        final int count = getLastLevel();
        double delta = magnitude(0);
        if (count != 0) {
            delta = (magnitude(count) - delta) / count;
        }
        final int n = Math.max(Math.min(DecimalFunctions.fractionDigitsForDelta(delta, false), 6), 0);
        final NumberFormat f = NumberFormat.getInstance();
        f.setMinimumFractionDigits(n);
        f.setMaximumFractionDigits(n);
        final var table = new TableAppender("  ");
        table.setCellAlignment(TableAppender.ALIGN_RIGHT);
        for (int level=0; level <= count; level++) {
            final double[] rs = resolutionSquared[level];
            for (final double r : rs) {
                table.append(f.format(Math.sqrt(r)));
                table.nextColumn();
            }
            final Reference<GridCoverage> ref = coverages[level];
            if (ref != null && !JDK16.refersTo(ref, null)) {
                table.append("cached");
            }
            table.nextLine();
        }
        return table.toString();
    }

    /**
     * Returns the magnitude of resolution at the given level.
     */
    private double magnitude(final int level) {
        return Math.sqrt(Arrays.stream(resolutionSquared[level]).average().orElse(1));
    }
}
