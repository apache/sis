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
package org.apache.sis.gui.coverage;

import java.util.Optional;
import java.util.concurrent.FutureTask;
import java.awt.image.RenderedImage;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.gui.map.StatusBar;
import org.apache.sis.internal.gui.LogHandler;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.DataStoreException;


/**
 * A request for a two-dimensional view of a grid coverage. Those requests can be used for
 * {@linkplain GridCoverageResource#read(GridGeometry, int...) reading} or
 * {@linkplain GridCoverage#render(GridExtent) rendering} and image in a background thread.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class ImageRequest {
    /**
     * The {@value} value, for identifying code that assume two-dimensional objects.
     */
    private static final int BIDIMENSIONAL = 2;

    /**
     * The source from where to read the image, specified at construction time.
     * Can not be {@code null}.
     */
    final GridCoverageResource resource;

    /**
     * The source for rendering the image, specified at construction time.
     * After construction, only one of {@link #resource} and {@link #coverage} is non-null.
     * But after task execution, this field will be set to the coverage which has been read.
     */
    private GridCoverage coverage;

    /**
     * Desired grid extent and resolution, or {@code null} for reading the whole domain.
     * This is used only if the data source is a {@link GridCoverageResource}.
     *
     * @see #getDomain()
     */
    private final GridGeometry domain;

    /**
     * 0-based indices of sample dimensions to read, or {@code null} for reading them all.
     * This is used only if the data source is a {@link GridCoverageResource}.
     *
     * @see #getDomain()
     */
    private final int[] range;

    /**
     * A subspace of the grid coverage extent to render, or {@code null} for the whole extent.
     * If the extent has more than two dimensions, then the image will be rendered along the
     * two first dimensions having a size greater than 1 cell.
     *
     * @see #getSliceExtent()
     * @see #setSliceExtent(GridExtent)
     */
    private GridExtent sliceExtent;

    /**
     * The relative position of slice in dimensions other than the 2 visible dimensions,
     * as a ratio between 0 and 1. This may become configurable in a future version.
     *
     * @see GridDerivation#sliceByRatio(double, int[])
     */
    private static final double SLICE_RATIO = 0;

    /**
     * The coverage explorer to inform after loading completed, or {@code null} if none.
     * We do not provide a more generic listeners API for now, but we could do that
     * in the future if there is a need.
     */
    CoverageExplorer listener;

    /**
     * Creates a new request for loading an image from the specified resource.
     * If {@code domain} and {@code range} arguments are null, then the full coverage will be loaded.
     * For loading a smaller amount of data, sub-domain or sub-range can be specified as documented
     * in the {@linkplain GridCoverageResource#read(GridGeometry, int...) read method javadoc}.
     *
     * @param  source  source of the image to load.
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  range   0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     *
     * @see GridCoverageResource#read(GridGeometry, int...)
     */
    public ImageRequest(final GridCoverageResource source, final GridGeometry domain, final int... range) {
        ArgumentChecks.ensureNonNull("source", source);
        this.resource = source;
        this.domain   = domain;
        this.range    = (range != null && range.length != 0) ? range : null;
        /*
         * To be strict we should clone the array, but ImageRequest is just passing the array to
         * GridCoverageResource, which is the class making real use of it. This is not sensitive
         * object state here.
         */
    }

    /**
     * Creates a new request for loading an image from the specified coverage.
     * If the {@code sliceExtent} argument is null, then the full coverage will be rendered
     * in the first two dimensions having a size greater than 1 cell. For rendering a smaller amount of data,
     * or for rendering data along other dimensions, a slice extent can be specified as documented in the
     * {@linkplain GridCoverage#render(GridExtent) render method javadoc}.
     *
     * @param  source       source of the image to load.
     * @param  sliceExtent  a subspace of the grid coverage extent to render, or {@code null} for the whole extent.
     *
     * @see GridCoverage#render(GridExtent)
     */
    public ImageRequest(final GridCoverage source, final GridExtent sliceExtent) {
        ArgumentChecks.ensureNonNull("source", source);
        this.resource    = null;
        this.domain      = null;
        this.range       = null;
        this.coverage    = source;
        this.sliceExtent = sliceExtent;
    }

    /**
     * Returns the coverage, or an empty value if not yet known. This is either the value specified explicitly
     * to the constructor, or otherwise the coverage obtained after a read operation.
     *
     * @return the coverage.
     */
    public final Optional<GridCoverage> getCoverage() {
        return Optional.ofNullable(coverage);
    }

    /**
     * Returns the desired grid extent and resolution, or an empty value for reading the full domain.
     * This is the {@code domain} argument specified to the following constructor:
     *
     * <blockquote>{@link #ImageRequest(GridCoverageResource, GridGeometry, int[])}</blockquote>
     *
     * This argument will be forwarded verbatim to the following method
     * (see its javadoc for more explanation):
     *
     * <blockquote>{@link GridCoverageResource#read(GridGeometry, int...)}</blockquote>
     *
     * This property is always empty if this image request has been created with the
     * {@link #ImageRequest(GridCoverage, GridExtent)} constructor, since no read
     * operation will happen in such case.
     *
     * @return the desired grid extent and resolution of the coverage.
     */
    public final Optional<GridGeometry> getDomain() {
        return Optional.ofNullable(domain);
    }

    /**
     * Returns the 0-based indices of sample dimensions to read, or an empty value for reading them all.
     * This is the {@code range} argument specified to the following constructor:
     *
     * <blockquote>{@link #ImageRequest(GridCoverageResource, GridGeometry, int[])}</blockquote>
     *
     * This argument will be forwarded verbatim to the following method
     * (see its javadoc for more explanation):
     *
     * <blockquote>{@link GridCoverageResource#read(GridGeometry, int...)}</blockquote>
     *
     * This property is always empty if this image request has been created with the
     * {@link #ImageRequest(GridCoverage, GridExtent)} constructor, since no read
     * operation will happen in such case.
     *
     * @return the 0-based indices of sample dimensions to read.
     */
    public final Optional<int[]> getRange() {
        /*
         * To be strict we should clone the array, but ImageRequest is just passing the array to
         * GridCoverageResource, which is the class making real use of it. This is not sensitive
         * object state here.
         */
        return Optional.ofNullable(range);
    }

    /**
     * Returns the subspace of the grid coverage extent to render.
     * This is the {@code sliceExtent} argument specified to the following constructor:
     *
     * <blockquote>{@link #ImageRequest(GridCoverage, GridExtent)}</blockquote>
     *
     * This argument will be forwarded verbatim to the following method
     * (see its javadoc for more explanation):
     *
     * <blockquote>{@link GridCoverage#render(GridExtent)}</blockquote>
     *
     * If non-empty, then all dimensions except two should have a size of 1 cell.
     *
     * @return subspace of the grid coverage extent to render.
     */
    public final Optional<GridExtent> getSliceExtent() {
        return Optional.ofNullable(sliceExtent);
    }

    /**
     * Sets a new subspace of the grid coverage extent to render.
     *
     * <div class="note"><b>API design note:</b>
     * this {@code sliceExtent} argument is not specified
     * to the {@link #ImageRequest(GridCoverageResource, GridGeometry, int[])} constructor because when reading data
     * from a {@link GridCoverageResource}, a slicing can already be done by the {@link GridGeometry} {@code domain}
     * argument. This method is provided for the rare cases where it may be useful to specify both the {@code domain}
     * and the {@code sliceExtent}.</div>
     *
     * @param  sliceExtent  subspace of the grid coverage extent to render.
     */
    public final void setSliceExtent(final GridExtent sliceExtent) {
        this.sliceExtent = sliceExtent;
    }

    /**
     * Computes a two dimension slice of the given grid geometry.
     * This method selects the two first dimensions having a size greater than 1 cell.
     *
     * @param  domain  the grid geometry in which to choose a two-dimensional slice.
     * @return a builder configured for returning the desired two-dimensional slice.
     */
    private static GridDerivation slice(final GridGeometry domain) {
        final GridExtent extent = domain.getExtent();
        final int dimension = extent.getDimension();
        final int[] sliceDimensions = new int[BIDIMENSIONAL];
        int k = 0;
        for (int i=0; i<dimension; i++) {
            if (extent.getLow(i) != extent.getHigh(i)) {
                sliceDimensions[k] = i;
                if (++k >= BIDIMENSIONAL) break;
            }
        }
        return domain.derive().sliceByRatio(ImageRequest.SLICE_RATIO, sliceDimensions);
    }

    /**
     * Loads the image. Current implementation reads the full image. If the coverage has more than
     * {@value #BIDIMENSIONAL} dimensions, only two of them are taken for the image; for all other
     * dimensions, only the values at lowest index will be read.
     *
     * <p>If the {@link #coverage} field was null, it will be initialized as a side-effect.
     * No other fields will be modified.</p>
     *
     * <p>This class does not need to be thread-safe since it should be used only once in a well-defined
     * life cycle. We nevertheless synchronize as a safety (user could give the same {@code ImageRequest}
     * to two different {@link CoverageExplorer} instances).</p>
     *
     * @param  task       the task invoking this method (for checking for cancellation).
     * @param  converted  {@code true} for a coverage containing converted values,
     *                    or {@code false} for a coverage containing packed values.
     * @return the image loaded from the source given at construction time,
     *         or {@code null} if the task has been cancelled.
     * @throws DataStoreException if an error occurred while loading the grid coverage.
     */
    final synchronized RenderedImage load(final FutureTask<?> task, final boolean converted) throws DataStoreException {
        final Long id = LogHandler.loadingStart(resource);
        try {
            if (coverage == null) {
                GridGeometry domain = this.domain;
                if (domain == null) {
                    domain = resource.getGridGeometry();
                }
                if (domain != null && domain.getDimension() > BIDIMENSIONAL) {
                    domain = slice(domain).build();
                }
                /*
                 * TODO: We restrict loading to a two-dimensional slice for now.
                 * Future version will need to give user control over slices.
                 */
                coverage = resource.read(domain, range);                    // May be long to execute.
                coverage = coverage.forConvertedValues(converted);
            }
            if (task.isCancelled()) {
                return null;
            }
            GridExtent se = sliceExtent;
            if (se == null) {
                final GridGeometry cd = coverage.getGridGeometry();
                if (cd != null && cd.getDimension() > BIDIMENSIONAL) {      // Should never be null but we are paranoiac.
                    se = slice(cd).getIntersection();
                }
            }
            return coverage.render(se);
        } finally {
            LogHandler.loadingStop(id);
        }
    }

    /**
     * Configures the given status bar with the geometry of the grid coverage we have just read.
     * This method is invoked in JavaFX thread after {@link GridView#setImage(ImageRequest)}
     * successfully loaded in background thread a new image.
     */
    final void configure(final StatusBar bar) {
        final Long id = LogHandler.loadingStart(resource);
        try {
            final GridCoverage cv = coverage;
            final GridExtent request = sliceExtent;
            bar.applyCanvasGeometry(cv != null ? cv.getGridGeometry() : null);
            /*
             * By `GridCoverage.render(GridExtent)` contract, the `RenderedImage` pixel coordinates are relative
             * to the requested `GridExtent`. Consequently we need to translate the image coordinates so that it
             * become the coordinates of the original `GridGeometry` before to apply `gridToCRS`.  It is okay to
             * modify `StatusBar.localToObjectiveCRS` because we do not associate it to a `MapCanvas`, so it will
             * not be overwritten by gesture events (zoom, pan, etc).
             */
            if (request != null) {
                final double[] origin = new double[request.getDimension()];
                for (int i=0; i<origin.length; i++) {
                    origin[i] = request.getLow(i);
                }
                bar.localToObjectiveCRS.set(MathTransforms.concatenate(
                        MathTransforms.translation(origin), bar.localToObjectiveCRS.get()));
            }
        } finally {
            LogHandler.loadingStop(id);
        }
    }
}
