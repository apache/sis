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
import javafx.scene.Node;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.gui.map.StatusBar;
import org.apache.sis.internal.gui.LogHandler;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreListeners;


/**
 * A request for a two-dimensional view of a grid coverage. Those requests can be used for
 * {@linkplain GridCoverageResource#read(GridGeometry, int...) reading} or
 * {@linkplain GridCoverage#render(GridExtent) rendering} and image in a background thread.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see GridView#setImage(ImageRequest)
 * @see CoverageExplorer#setCoverage(ImageRequest)
 *
 * @since 1.1
 * @module
 */
public class ImageRequest {
    /**
     * The source from where to read the image, specified at construction time.
     * May be {@code null} if {@link #coverage} instance was specified at construction time.
     */
    final GridCoverageResource resource;

    /**
     * The source for rendering the image, specified at construction time.
     * After construction, only one of {@link #resource} and {@code coverage} fields is non-null.
     * But after {@link Loader} task execution, this field will be set to the coverage which has been read.
     *
     * @see #getCoverage()
     */
    private volatile GridCoverage coverage;

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
     * @see #getRange()
     */
    private final int[] range;

    /**
     * A subspace of the grid coverage extent to render, or {@code null} for the whole extent.
     * It can be used for specifying a slice in a <var>n</var>-dimensional data cube.
     * If not specified by the user, will be updated to the extent actually rendered.
     *
     * @see #getSliceExtent()
     * @see #setSliceExtent(GridExtent)
     */
    private volatile GridExtent sliceExtent;

    /**
     * Creates a new request with both a resource and a coverage. At least one argument shall be non-null.
     * If both arguments are non-null, then {@code data} must be the result of reading the given resource.
     * In the latter case we will not actually read data (because they are already read) and this instance
     * is used only for transferring data e.g. from {@link CoverageExplorer} to {@link CoverageCanvas}.
     *
     * <p>This constructor is not in public API because users should supply only a resource or a coverage,
     * not both.</p>
     */
    ImageRequest(final GridCoverageResource source, final GridCoverage data) {
        resource = source;
        coverage = data;
        domain   = null;
        range    = null;
    }

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
     * This method returns the first non-empty value in the following choices:
     *
     * <ol>
     *   <li>The last value specified to {@link #setSliceExtent(GridExtent)}.</li>
     *   <li>The value specified to the {@link #ImageRequest(GridCoverage, GridExtent)} constructor.</li>
     *   <li>The extent of the default slice selected by this {@code ImageRequest}
     *       after completion of the reading task.</li>
     * </ol>
     *
     * @return subspace of the grid coverage extent to render.
     *
     * @see GridCoverage#render(GridExtent)
     */
    public final Optional<GridExtent> getSliceExtent() {
        return Optional.ofNullable(sliceExtent);
    }

    /**
     * Sets a new subspace of the grid coverage extent to render.
     * This method can be used for specifying a two-dimensional slice in a <var>n</var>-dimensional data cube,
     * as specified in {@link GridCoverage#render(GridExtent)} documentation.
     *
     * <div class="note"><b>API design note:</b>
     * this {@code sliceExtent} argument is not specified
     * to the {@link #ImageRequest(GridCoverageResource, GridGeometry, int[])} constructor because when reading data
     * from a {@link GridCoverageResource}, a slicing can already be done by the {@link GridGeometry} {@code domain}
     * argument. This method is provided for the rare cases where it may be useful to specify both the {@code domain}
     * and the {@code sliceExtent}. The difference between the two ways to specify a slice is that the {@code domain}
     * argument is used at reading time for reducing the amount of data to load, while this {@link sliceExtent}
     * property is typically used after data has been read.</div>
     *
     * @param  sliceExtent  subspace of the grid coverage extent to render, or {@code null} for the whole extent.
     *         All dimensions except two shall have a size of 1 cell.
     *
     * @see GridCoverage#render(GridExtent)
     */
    public final void setSliceExtent(final GridExtent sliceExtent) {
        this.sliceExtent = sliceExtent;
    }

    /**
     * Loads the image. If the coverage has more than {@value #BIDIMENSIONAL} dimensions,
     * only two of them are taken for the image; for all other dimensions, only the values
     * at lowest index will be read.
     *
     * <p>If the {@link #coverage} field was null, it will be initialized as a side-effect.
     * No other fields will be modified.</p>
     *
     * <h4>Thread safety</h4>
     * This class does not need to be thread-safe because it should be used only once in a well-defined life cycle.
     * We nevertheless synchronize as a safety (e.g. user could give the same {@code ImageRequest} to two different
     * {@link CoverageExplorer} instances). In such case the {@link GridCoverage} will be loaded only once,
     * but no caching is done for the {@link RenderedImage} (because usually not needed).
     *
     * @param  task  the task invoking this method (for checking for cancellation).
     * @return the image loaded from the source given at construction time, or {@code null}
     *         if the task has been cancelled.
     * @throws DataStoreException if an error occurred while loading the grid coverage.
     */
    final synchronized RenderedImage load(final FutureTask<?> task) throws DataStoreException {
        GridCoverage cv = coverage;
        final Long id = LogHandler.loadingStart(resource);
        try {
            if (cv == null) {
                cv = MultiResolutionImageLoader.getInstance(resource, null).getOrLoad(domain, range);
            }
            coverage = cv = cv.forConvertedValues(true);
            GridExtent ex = sliceExtent;
            if (ex == null) {
                final GridGeometry gg = cv.getGridGeometry();
                if (gg.isDefined(GridGeometry.EXTENT)) {
                    ex = gg.getExtent();
                    if (gg.getDimension() > MultiResolutionImageLoader.BIDIMENSIONAL) {
                        ex = MultiResolutionImageLoader.slice(gg.derive(), ex).getIntersection();
                    }
                    sliceExtent = ex;
                }
            }
            if (task.isCancelled()) {
                return null;
            }
            return cv.render(ex);
        } finally {
            LogHandler.loadingStop(id);
        }
    }

    /**
     * Configures the given status bar with the geometry of the grid coverage we have just read.
     * This method is invoked in JavaFX thread after above {@link #load(FutureTask)} background
     * task completed, regardless if successful or not.
     * The two method calls are done (indirectly) by {@link GridView#setImage(ImageRequest)}.
     */
    final void configure(final StatusBar bar) {
        final Long id = LogHandler.loadingStart(resource);
        try {
            GridExtent ex = sliceExtent;
            GridCoverage cv = coverage;
            GridGeometry gg = (cv != null) ? cv.getGridGeometry() : null;
            if (gg != null && ex != null) {
                /*
                 * By `GridCoverage.render(GridExtent)` contract, the `RenderedImage` pixel coordinates are relative
                 * to the requested `GridExtent`. Consequently we need to translate the grid coordinates so that the
                 * request coordinates start at zero.
                 */
                final long[] offset = new long[ex.getDimension()];
                for (final int i : bar.getXYDimensions()) {
                    offset[i] = Math.negateExact(ex.getLow(i));
                }
                ex = ex.translate(offset);
                gg = gg.translate(offset);          // Does not change the "real world" envelope.
                try {
                    gg = gg.relocate(ex);           // Changes the "real world" envelope.
                } catch (TransformException e) {
                    bar.setErrorMessage(null, e);
                }
            }
            bar.applyCanvasGeometry(gg);
        } finally {
            LogHandler.loadingStop(id);
        }
    }

    /**
     * Reports an exception in a dialog box. This is a convenience method for
     * {@link javafx.concurrent.Task#succeeded()} implementations.
     *
     * @param  owner      control in the window which will own the dialog, or {@code null} if unknown.
     * @param  exception  the error that occurred.
     */
    final void reportError(final Node owner, final Throwable exception) {
        if (resource instanceof StoreListeners) {
            ExceptionReporter.canNotReadFile(owner, ((StoreListeners) resource).getSourceName(), exception);
        } else {
            ExceptionReporter.canNotUseResource(owner, exception);
        }
    }
}
