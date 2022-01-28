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
package org.apache.sis.storage.geotiff;

import java.util.List;
import java.util.Arrays;
import java.io.IOException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.internal.storage.GridResourceWrapper;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;


/**
 * A list of Image File Directory (FID) where the first entry is the image at finest resolution
 * and following entries are images at finer resolutions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class MultiResolutionImage extends GridResourceWrapper {
    /**
     * Descriptions of each <cite>Image File Directory</cite> (IFD) in the GeoTIFF file.
     */
    private final ImageFileDirectory[] levels;

    /**
     * Resolutions (in units of CRS axes) of each level from finest to coarsest resolution.
     * Array elements may be {@code null} if not yet computed.
     *
     * @see #resolution(int)
     * @see #getResolutions()
     */
    private final double[][] resolutions;

    /**
     * Creates a multi-resolution images with all the given reduced-resolution (overview) images,
     * from finest resolution to coarsest resolution. The full-resolution image shall be at index 0.
     */
    MultiResolutionImage(final List<ImageFileDirectory> overviews) {
        levels = overviews.toArray(new ImageFileDirectory[overviews.size()]);
        resolutions = new double[levels.length][];
    }

    /**
     * Returns the object on which to perform all synchronizations for thread-safety.
     */
    @Override
    protected final Object getSynchronizationLock() {
        return levels[0].getSynchronizationLock();
    }

    /**
     * Creates the resource on which to delegate operations.
     * The source is the first image, the one having finest resolution.
     * By Cloud Optimized GeoTIFF (COG) convention, this is the image containing metadata (CRS).
     * This method is invoked in a synchronized block when first needed and the result is cached.
     */
    @Override
    protected GridCoverageResource createSource() throws DataStoreException {
        try {
            return getImageFileDirectory(0);
        } catch (IOException e) {
            throw levels[0].reader.store.errorIO(e);
        }
    }

    /**
     * Completes and returns the image at the given pyramid level.
     * Indices are in the same order as the images appear in the TIFF file,
     * with 0 for the full resolution image.
     *
     * @param  index  image index (level) in the pyramid, with 0 for finest resolution.
     * @return image at the given pyramid level.
     */
    private ImageFileDirectory getImageFileDirectory(final int index) throws IOException, DataStoreException {
        final ImageFileDirectory dir = levels[index];
        if (dir.hasDeferredEntries) {
            dir.reader.resolveDeferredEntries(dir);
        }
        dir.validateMandatoryTags();
        return dir;
    }

    /**
     * Returns the resolution (in units of CRS axes) for the given level.
     *
     * @param  level  the desired resolution level, numbered from finest to coarsest resolution.
     * @return resolution at the specified level, not cloned (caller shall not modify).
     */
    private double[] resolution(final int level) throws DataStoreException {
        double[] resolution = resolutions[level];
        if (resolution == null) try {
            final ImageFileDirectory image      = getImageFileDirectory(level);
            final ImageFileDirectory base       = getImageFileDirectory(0);
            final GridGeometry       geometry   = base.getGridGeometry();
            final GridExtent         fullExtent = geometry.getExtent();
            final GridExtent         subExtent  = image.getExtent();
            final MatrixSIS          gridToCRS  = MatrixSIS.castOrCopy(geometry.getGridToCRS(PixelInCell.CELL_CENTER)
                    .derivative(new DirectPositionView.Double(fullExtent.getPointOfInterest())));
            final double[] scales = new double[fullExtent.getDimension()];
            for (int i=0; i<scales.length; i++) {
                scales[i] = fullExtent.getSize(i, false) / subExtent.getSize(i, false);
            }
            image.initReducedResolution(base, scales);
            resolution = gridToCRS.multiply(scales);
            for (int i=0; i<resolution.length; i++) {
                resolution[i] = Math.abs(resolution[i]);
            }
            resolutions[level] = resolution;
        } catch (TransformException e) {
            throw new DataStoreReferencingException(e);
        } catch (IOException e) {
            throw levels[level].reader.store.errorIO(e);
        }
        return resolution;
    }

    /**
     * Returns the preferred resolutions (in units of CRS axes) for read operations in this data store.
     * Elements are ordered from finest (smallest numbers) to coarsest (largest numbers) resolution.
     */
    @Override
    public List<double[]> getResolutions() throws DataStoreException {
        final double[][] copy = new double[resolutions.length][];
        synchronized (getSynchronizationLock()) {
            for (int i=0; i<copy.length; i++) {
                copy[i] = resolution(i).clone();
            }
        }
        return Arrays.asList(copy);
    }

    /**
     * Loads a subset of the grid coverage represented by this resource.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  range   0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and range.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public GridCoverage read(final GridGeometry domain, final int... range) throws DataStoreException {
        final double[] request;
        int level;
        if (domain != null && domain.isDefined(GridGeometry.RESOLUTION)) {
            request = domain.getResolution(true);
            level   = resolutions.length;
        } else {
            request = null;
            level   = 1;
        }
        synchronized (getSynchronizationLock()) {
finer:      while (--level > 0) {
                final double[] resolution = resolution(level);
                for (int i=0; i<request.length; i++) {
                    if (!(request[i] >= resolution[i])) {            // Use `!` for catching NaN.
                        continue finer;
                    }
                }
                break;
            }
            final ImageFileDirectory image;
            try {
                image = getImageFileDirectory(level);
            } catch (IOException e) {
                throw levels[level].reader.store.errorIO(e);
            }
            image.setLoadingStrategy(getLoadingStrategy());
            return image.read(domain, range);
        }
    }
}
