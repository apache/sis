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

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.awt.image.RenderedImage;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.coverage.grid.PixelInCell;


/**
 * A Grid Coverage Resource stored in memory.
 * This resource wraps an arbitrary {@link GridCoverage} specified at construction time.
 * Metadata can be specified by overriding the {@link #createMetadata()} method.
 *
 * <h2>When to use</h2>
 * This class is useful for small grid coverages, or for testing purposes,
 * or when the coverage is in memory anyway (for example, a computation result).
 * It should generally not be used for large coverages read from files or databases.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @since 1.6
 */
public class MemoryGridCoverageResource extends AbstractGridCoverageResource {
    /**
     * A constant for identifying code that relying on having 2 dimensions.
     */
    private static final int BIDIMENSIONAL = 2;

    /**
     * The resource identifier, or {@code null} if none.
     */
    private final GenericName identifer;

    /**
     * The grid coverage specified at construction time.
     *
     * @see #getGridCoverage()
     */
    protected final GridCoverage coverage;

    /**
     * The grid coverage processor to use for selecting bands.
     * It may be configured with a colorizer for determining the color models.
     */
    protected final GridCoverageProcessor processor;

    /**
     * Whether to defer the calls to {@link GridCoverage#render(GridExtent)}.
     * If {@code false}, the calls will be done sooner (if possible)
     * as if data were read from a file immediately.
     */
    private boolean deferredRendering;

    /**
     * Creates a new coverage stored in memory.
     *
     * @param parent      the parent resource, or {@code null} if none.
     * @param identifier  resource identifier, or {@code null} if none.
     * @param coverage    stored coverage retained as-is (not copied). Cannot be null.
     * @param processor   the grid coverage processor for selecting bands, or {@code null} for default.
     */
    public MemoryGridCoverageResource(final Resource parent, final GenericName identifier,
                                      final GridCoverage coverage, final GridCoverageProcessor processor)
    {
        super(parent);
        this.identifer = identifier;
        this.coverage  = Objects.requireNonNull(coverage);
        this.processor = (processor != null) ? processor : new GridCoverageProcessor();
    }

    /**
     * Returns the resource identifier specified at construction time, if any.
     */
    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.ofNullable(identifer);
    }

    /**
     * Returns the grid coverage wrapped by this resource.
     * The grid coverage returned by this method shall be the same or equivalent
     * to the coverage that would be returned by a call to {@code read(null)},
     * and should not be expansive to get.
     *
     * @return the grid coverage wrapped by this resource.
     *
     * @see #read(GridGeometry, int...)
     */
    public GridCoverage getGridCoverage() {
        return coverage;
    }

    /**
     * Returns information about the <i>domain</i> of wrapped grid coverage.
     *
     * @return extent of grid coordinates together with their mapping to "real world" coordinates.
     */
    @Override
    public GridGeometry getGridGeometry() {
        return coverage.getGridGeometry();
    }

    /**
     * Returns information about the <i>ranges</i> of wrapped grid coverage.
     *
     * @return ranges of sample values together with their mapping to "real values".
     */
    @Override
    public List<SampleDimension> getSampleDimensions() {
        return coverage.getSampleDimensions();
    }

    /**
     * Returns an indication about whether {@code read(…)} tries to force the loading of data.
     */
    @Override
    public RasterLoadingStrategy getLoadingStrategy() {
        return deferredRendering ? RasterLoadingStrategy.AT_RENDER_TIME
                                 : RasterLoadingStrategy.AT_READ_TIME;
    }

    /**
     * Sets the preference about whether {@code read(…)} should try to force the loading of data.
     */
    @Override
    public boolean setLoadingStrategy(final RasterLoadingStrategy strategy) {
        deferredRendering = !strategy.equals(RasterLoadingStrategy.AT_READ_TIME);
        return strategy == getLoadingStrategy();
    }

    /**
     * Returns a subset of the wrapped grid coverage. If a null grid geometry and a null or empty range is specified,
     * then this method shall return the same grid coverage as {@link #getGridCoverage()} or an equivalent coverage.
     * If a non-null grid geometry is specified, then this method tries to return a grid coverage matching the given
     * grid geometry on a best-effort basis. It may be the whole coverage.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for the whole domain.
     * @param  ranges  0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and ranges.
     */
    @Override
    public GridCoverage read(GridGeometry domain, final int... ranges) {
        /*
         * If the user requested a subset of the bands, select the bands on the grid coverage.
         * It is slightly more expensive than selecting the bands on the image produced at the
         * end of this method, except if the coverage is a `BandAggregateGridCoverage` because
         * the latter will do rendering only on the selected coverage components.
         */
        GridCoverage subset = coverage;
        if (ranges != null && ranges.length != 0) {
            subset = processor.selectSampleDimensions(subset, ranges);
        }
        if (domain == null) {
            return subset;
        }
        /*
         * The given `domain` may use arbitrary `gridToCRS` and `CRS` properties.
         * For this simple implementation we need the same `gridToCRS` and `CRS`
         * than the wrapped coverage; only domain `extent` is allowed to differ.
         * Subsampling is ignored for now because it is an expensive operation.
         * Clipping and ranges selection are light and do not copy any data.
         *
         * TODO: a future implementation may apply subsampling efficiently,
         *       by adjusting the pixel stride in SampleModel.
         */
        final GridGeometry source = subset.getGridGeometry();
        GridExtent intersection = source.derive()
                .rounding(GridRoundingMode.ENCLOSING)
                .subgrid(domain).getIntersection();             // Take in account the change of CRS if needed.
        if (deferredRendering || intersection.getDegreesOfFreedom() > BIDIMENSIONAL) {
            return processor.clip(subset, intersection);
        }
        /*
         * Invoke `GridCoverage.render(GridExtent)- immediately. It simulates a "load data at `read(…)` invocation time" strategy.
         * Note: we would consider to remove all the remaining code in this method and unconditionally rely on above clipping.
         */
        if (intersection.contains(source.getExtent())) {
            return subset;
        }
        final int dimX, dimY;
        {   // For local scope of `gridDimensions`.
            int[] gridDimensions = intersection.getSubspaceDimensions(BIDIMENSIONAL);
            dimX = gridDimensions[0];
            dimY = gridDimensions[1];
        }
        /*
         * After `render(…)` execution, the (minX, minY) image coordinates are the differences between
         * the extent that we requested and the one that we got. If these differences are not zero,
         * we need to translate the `GridExtent` in order to make it matches what we got. But before to
         * apply that translation, we adjust the grid size (because it may add another translation).
         */
        final RenderedImage data = subset.render(intersection);
        final long ox = intersection.getLow(dimX);
        final long oy = intersection.getLow(dimY);
        final var changes = new long[Math.max(dimX, dimY) + 1];
        for (int i = changes.length; --i >= 0;) {
            changes[i] = intersection.getSize(i);       // We need only the dimensions that may change.
        }
        changes[dimX] = data.getWidth();
        changes[dimY] = data.getHeight();
        intersection  = intersection.resize(changes);
        /*
         * Apply the translation after we resized the grid extent, because the resize operation
         * may have caused an additional translation. We cancel that translation with terms that
         * restore the (ox,oy) lower coordinates before to add the data minimum X,Y.
         */
        Arrays.fill(changes, 0);
        changes[dimX] = Math.addExact(ox - intersection.getLow(dimX), data.getMinX());
        changes[dimY] = Math.addExact(oy - intersection.getLow(dimY), data.getMinX());
        intersection  = intersection.translate(changes);
        /*
         * If the result is the same intersection as the source coverage,
         * we can return that coverage directly.
         */
        if (intersection.equals(source.getExtent())) {
            return subset;
        }
        var crs = source.isDefined(GridGeometry.CRS) ? source.getCoordinateReferenceSystem() : null;
        domain = new GridGeometry(intersection, PixelInCell.CELL_CORNER, source.getGridToCRS(PixelInCell.CELL_CORNER), crs);
        return new GridCoverageBuilder()
                .setValues(data)
                .setDomain(domain)
                .setRanges(subset.getSampleDimensions())
                .build();
    }

    /**
     * Tests whether this memory grid coverage resource is wrapping the same coverage as the given object.
     * This method checks also that the listeners and the grid coverage processor are equal.
     *
     * @param  obj  the object to compare.
     * @return whether the two objects are memory resources wrapping the same coverage.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj != null && obj.getClass() == getClass()) {
            final var other = (MemoryGridCoverageResource) obj;
            return coverage.equals(other.coverage)   &&
                   processor.equals(other.processor) &&
                   listeners.equals(other.listeners);
        }
        return false;
    }

    /**
     * Returns a hash code value for consistency with {@code equals(Object)}.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return coverage.hashCode() + 31 * processor.hashCode() + 37 * listeners.hashCode();
    }

    /**
     * Returns a string representation of this resource.
     * The default implementation returns the string representation of the wrapped coverage.
     *
     * @return the string representation of this resource.
     */
    @Override
    public String toString() {
        return coverage.toString();
    }
}
