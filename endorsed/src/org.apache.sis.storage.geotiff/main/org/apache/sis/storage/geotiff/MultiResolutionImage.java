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
import java.util.Optional;
import java.io.IOException;
import org.opengis.util.NameSpace;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.storage.base.GridResourceWrapper;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.internal.shared.DirectPositionView;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import static org.apache.sis.storage.geotiff.reader.GridGeometryBuilder.BIDIMENSIONAL;


/**
 * A list of Image File Directory (FID) where the first entry is the image at finest resolution
 * and following entries are images at finer resolutions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class MultiResolutionImage extends GridResourceWrapper implements StoreResource {
    /**
     * Name of the image at finest resolution.
     * This is used as the namespace for overviews.
     */
    private NameSpace namespace;

    /**
     * Descriptions of each <i>Image File Directory</i> (IFD) in the GeoTIFF file.
     * Should have at least 2 elements. The full-resolution image shall be at index 0.
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
     * The last coordinate operation returned by {@link #getTransformFrom(CoordinateReferenceSystem)}.
     * Used as an optimization in the common case where the same CRS is used for many requests.
     */
    private volatile CoordinateOperation lastOperation;

    /**
     * Creates a multi-resolution images with all the given reduced-resolution (overview) images,
     * from finest resolution to coarsest resolution. The full-resolution image shall be at index 0.
     */
    MultiResolutionImage(final List<ImageFileDirectory> overviews) {
        levels = overviews.toArray(ImageFileDirectory[]::new);
        resolutions = new double[levels.length][];
    }

    /**
     * Returns the data store that produced this resource.
     */
    @Override
    public final DataStore getOriginator() {
        return levels[0].getOriginator();
    }

    /**
     * Gets the paths to files used by this resource, or an empty value if unknown.
     */
    @Override
    public final Optional<FileSet> getFileSet() throws DataStoreException {
        return levels[0].getFileSet();
    }

    /**
     * Returns the object on which to perform all synchronizations for thread-safety.
     */
    @Override
    protected final Object getSynchronizationLock() {
        return levels[0].getSynchronizationLock();
    }

    /**
     * Creates the resource to which to delegate operations.
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
        assert Thread.holdsLock(getSynchronizationLock());
        final ImageFileDirectory dir = levels[index];
        if (dir.hasDeferredEntries) {
            dir.reader.resolveDeferredEntries(dir);
        }
        if (dir.validateMandatoryTags() && index != 0) {
            if (namespace == null) {
                final ImageFileDirectory base = levels[0];
                // Identifier should never be empty (see `DataCube.getIdentifier()` contract).
                namespace = base.reader.store.nameFactory.createNameSpace(base.getIdentifier().get(), null);
            }
            dir.setOverviewIdentifier(namespace, index);
        }
        return dir;
    }

    /**
     * Returns the resolution (in units of <abbr>CRS</abbr> axes) for the given level.
     * If there is a temporal dimension, its resolution is set to NaN because we don't
     * know the duration.
     *
     * @param  level  the desired resolution level, numbered from finest to coarsest resolution.
     * @return resolution at the specified level, not cloned (caller shall not modify).
     */
    private double[] resolution(final int level) throws DataStoreException {
        double[] resolution = resolutions[level];
        if (resolution == null) try {
            final ImageFileDirectory image = getImageFileDirectory(level);
            final ImageFileDirectory base  = getImageFileDirectory(0);
            final double[] scales = image.initReducedResolution(base);
            final GridGeometry geometry = base.getGridGeometry();
            if (geometry.isDefined(GridGeometry.GRID_TO_CRS)) {
                final GridExtent fullExtent = geometry.getExtent();
                DirectPosition poi = new DirectPositionView.Double(fullExtent.getPointOfInterest(PixelInCell.CELL_CENTER));
                MatrixSIS gridToCRS = MatrixSIS.castOrCopy(geometry.getGridToCRS(PixelInCell.CELL_CENTER).derivative(poi));
                resolution = gridToCRS.multiply(scales);
            } else {
                // Assume an identity transform for the `gridToCRS` of full resolution image.
                resolution = scales;
            }
            // Set to NaN only after all matrix multiplications are done.
            int i = Math.min(BIDIMENSIONAL, resolution.length);
            Arrays.fill(scales, BIDIMENSIONAL, i, Double.NaN);
            while (--i >= 0) {
                resolution[i] = Math.abs(resolution[i]);
            }
            resolutions[level] = resolution;
        } catch (TransformException e) {
            throw new DataStoreReferencingException(e.getMessage(), e);
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
     * Converts a resolution from units in the given CRS to units of this coverage CRS.
     *
     * @param  domain  the geometry from which to get the resolution.
     * @return resolution from the given grid geometry in units of this coverage CRS, or {@code null}.
     */
    private double[] getResolution(final GridGeometry domain) throws DataStoreException {
        if (domain == null || !domain.isDefined(GridGeometry.RESOLUTION)) {
            return null;
        }
        double[] resolution = domain.getResolution(true);
        if (domain.isDefined(GridGeometry.CRS | GridGeometry.ENVELOPE)) try {
            final CoordinateReferenceSystem crs = domain.getCoordinateReferenceSystem();
            CoordinateOperation op = lastOperation;
            if (op == null || !crs.equals(op.getTargetCRS())) {
                final GridGeometry gg = getGridGeometry();
                op = CRS.findOperation(crs, gg.getCoordinateReferenceSystem(), gg.getGeographicExtent().orElse(null));
                lastOperation = op;
            }
            final MathTransform sourceToCoverage = op.getMathTransform();
            if (!sourceToCoverage.isIdentity()) {
                /*
                 * If the `domain` grid geometry has a resolution and an envelope, then it should have
                 * an extent and a "grid to CRS" transform (otherwise it may be a `GridGeometry` bug)
                 */
                DirectPosition poi = new DirectPositionView.Double(domain.getExtent().getPointOfInterest(PixelInCell.CELL_CENTER));
                poi = domain.getGridToCRS(PixelInCell.CELL_CENTER).transform(poi, null);
                final MatrixSIS derivative = MatrixSIS.castOrCopy(sourceToCoverage.derivative(poi));
                resolution = derivative.multiply(resolution);
                for (int i=0; i<resolution.length; i++) {
                    resolution[i] = Math.abs(resolution[i]);
                }
            }
        } catch (FactoryException | TransformException e) {
            throw new DataStoreReferencingException(e.getMessage(), e);
        }
        return resolution;
    }

    /**
     * Loads a subset of the grid coverage represented by this resource.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  ranges  0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and ranges.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public GridCoverage read(final GridGeometry domain, final int... ranges) throws DataStoreException {
        final double[] request = getResolution(domain);
        int level = (request != null) ? resolutions.length : 1;
        synchronized (getSynchronizationLock()) {
finer:      while (--level > 0) {
                final double[] resolution = resolution(level);
                for (int i = Math.min(request.length, BIDIMENSIONAL); --i >= 0;) {
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
            return image.read(domain, ranges);
        }
    }
}
