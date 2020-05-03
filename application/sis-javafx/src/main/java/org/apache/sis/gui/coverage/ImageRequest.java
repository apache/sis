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
import javafx.concurrent.WorkerStateEvent;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.gui.map.StatusBar;
import org.apache.sis.referencing.operation.transform.MathTransforms;


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
     * The source from where to read the image, specified at construction time.
     */
    GridCoverageResource resource;

    /**
     * The source for rendering the image, specified at construction time.
     * After class initialization, only one of {@link #resource} and {@link #coverage} is non-null.
     * But after task execution, this field will be set to the coverage which has been read.
     */
    volatile GridCoverage coverage;

    /**
     * Desired grid extent and resolution, or {@code null} for reading the whole domain.
     * This is used only if the data source is a {@link GridCoverageResource}.
     */
    private GridGeometry domain;

    /**
     * 0-based indices of sample dimensions to read, or {@code null} for reading them all.
     * This is used only if the data source is a {@link GridCoverageResource}.
     */
    private int[] range;

    /**
     * A subspace of the grid coverage extent to render, or {@code null} for the whole extent.
     * If the extent has more than two dimensions, then the image will be rendered along the
     * two first dimensions having a size greater than 1 cell.
     */
    private GridExtent sliceExtent;

    /**
     * The relative position of slice in dimensions other than the 2 visible dimensions,
     * as a ratio between 0 and 1. This may become configurable in a future version.
     *
     * @see GridDerivation#sliceByRatio(double, int[])
     */
    static final double SLICE_RATIO = 0;

    /**
     * For transferring a listener to {@link ImageLoader#listener} before background execution starts.
     * We do not provide a more generic listeners API for now, but it could be done in the future
     * if there is a need for that.
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
        this.coverage    = source;
        this.sliceExtent = sliceExtent;
    }

    /**
     * Returns the desired grid extent and resolution, or an empty value for reading the full domain.
     * This is the {@code domain} argument specified to the following constructor:
     *
     * <blockquote>{@link #ImageRequest(GridCoverageResource, GridGeometry, int[])}</blockquote>
     *
     * and this argument will be transferred verbatim to the following method
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
    public Optional<GridGeometry> getDomain() {
        return Optional.ofNullable(domain);
    }

    /**
     * Returns the 0-based indices of sample dimensions to read, or an empty value for reading them all.
     * This is the {@code range} argument specified to the following constructor:
     *
     * <blockquote>{@link #ImageRequest(GridCoverageResource, GridGeometry, int[])}</blockquote>
     *
     * and this argument will be transferred verbatim to the following method
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
    public Optional<int[]> getRange() {
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
     * and this argument will be transferred verbatim to the following method
     * (see its javadoc for more explanation):
     *
     * <blockquote>{@link GridCoverage#render(GridExtent)}</blockquote>
     *
     * If non-empty, then all dimensions except two should have a size of 1 cell.
     *
     * @return subspace of the grid coverage extent to render.
     */
    public Optional<GridExtent> getSliceExtent() {
        return Optional.ofNullable(sliceExtent);
    }

    /**
     * Sets a new subspace of the grid coverage extent to render. This {@code sliceExtent} argument is not specified
     * to the {@link #ImageRequest(GridCoverageResource, GridGeometry, int[])} constructor because when reading data
     * from a {@link GridCoverageResource}, a slicing can already be done by the {@link GridGeometry} {@code domain}
     * argument. This method is provided for the rare cases where it may be useful to specify both the {@code domain}
     * and the {@code sliceExtent}.
     *
     * @param  sliceExtent  subspace of the grid coverage extent to render.
     */
    public void setSliceExtent(final GridExtent sliceExtent) {
        this.sliceExtent = sliceExtent;
    }

    /**
     * Configures the given status bar with the geometry of the grid coverage we have just read.
     * This method is invoked by {@link GridView#onImageLoaded(WorkerStateEvent)} in JavaFX thread
     * after {@link ImageLoader} successfully loaded in background thread a new image.
     */
    final void configure(final StatusBar bar) {
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
    }
}
