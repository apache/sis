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
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArgumentChecks;


/**
 * A request for a two-dimensional view of a grid coverage. Those requests can be used for
 * {@linkplain GridCoverageResource#read(GridGeometry, int...) reading} or
 * {@linkplain GridCoverage#render(GridExtent) rendering} and image in a background thread.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @see GridView#setImage(ImageRequest)
 * @see CoverageExplorer#setCoverage(ImageRequest)
 *
 * @since 1.1
 * @module
 */
public class ImageRequest {
    /**
     * The source from where to read the image.
     * One of {@code resource} and {@link #coverage} fields is non-null.
     */
    final GridCoverageResource resource;

    /**
     * The source for rendering the image.
     * One of {@link #resource} and {@code coverage} fields is non-null.
     *
     * @see #getCoverage()
     */
    final GridCoverage coverage;

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
     *
     * @see #getSliceExtent()
     */
    final GridExtent slice;

    /**
     * The initial objective CRS and zoom to use in a new {@link CoverageCanvas}, or {@code null} if none.
     * This is used only if we want to create a new canvas initialized to the same viewing region and zoom
     * level than an existing canvas.
     */
    GridGeometry zoom;

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
        slice    = null;
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
        this.coverage = null;
        this.slice    = null;
    }

    /**
     * Creates a new request for loading an image from the specified coverage.
     * If the {@code sliceExtent} argument is null, then the full coverage will be rendered
     * in the first two dimensions having a size greater than 1 cell. For rendering a smaller amount of data,
     * or for rendering data along other dimensions, a slice extent can be specified as documented in the
     * {@linkplain GridCoverage#render(GridExtent) render method javadoc}.
     *
     * @param  source  source of the image to load.
     * @param  slice   a subspace of the grid coverage extent to render, or {@code null} for the whole extent.
     *
     * @see GridCoverage#render(GridExtent)
     */
    public ImageRequest(final GridCoverage source, final GridExtent slice) {
        ArgumentChecks.ensureNonNull("source", source);
        this.coverage = source;
        this.slice    = slice;
        this.resource = null;
        this.domain   = null;
        this.range    = null;
    }

    /**
     * Returns the resource specified at construction time, or an empty value if none.
     *
     * @return the resource to read.
     */
    public final Optional<GridCoverageResource> getResource() {
        return Optional.ofNullable(resource);
    }

    /**
     * Returns the coverage specified at construction time, or an empty value if none.
     *
     * @return the coverage to render.
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
     *
     * @see GridCoverage#render(GridExtent)
     */
    public final Optional<GridExtent> getSliceExtent() {
        return Optional.ofNullable(slice);
    }

    /**
     * Returns or loads the coverage. This method should be invoked in a background thread.
     *
     * @return the coverage. May be a cached instance.
     * @throws DataStoreException if an error occurred during the loading process.
     */
    final GridCoverage load() throws DataStoreException {
        if (coverage != null) {
            return coverage;
        }
        return MultiResolutionImageLoader.getInstance(resource, null).getOrLoad(domain, range);
    }
}
