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
package org.apache.sis.internal.storage.image;

import java.util.List;
import java.util.Optional;
import java.io.IOException;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.storage.AbstractGridCoverageResource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.internal.storage.StoreResource;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.util.iso.Names;

import static java.lang.Math.toIntExact;
import org.apache.sis.coverage.grid.GridCoverage2D;


/**
 * A single image in a {@link Store}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
class Image extends AbstractGridCoverageResource implements StoreResource {
    /**
     * The dimensions of <var>x</var> and <var>y</var> axes.
     * Static constants for now, may become configurable fields in the future.
     */
    private static final int X_DIMENSION = 0, Y_DIMENSION = 1;

    /**
     * The parent data store.
     */
    private final Store store;

    /**
     * Index of the image to read.
     */
    private final int imageIndex;

    /**
     * The identifier as a sequence number in the namespace of the {@link Store}.
     * The first image has the sequence number "1". This is computed when first needed.
     *
     * @see #getIdentifier()
     */
    private GenericName identifier;

    /**
     * The grid geometry of this resource. The grid extent is the image size.
     *
     * @see #getGridGeometry()
     */
    private final GridGeometry gridGeometry;

    /**
     * The ranges of sample values, computed when first needed. Shall be an unmodifiable list.
     *
     * @see #getSampleDimensions()
     */
    private List<SampleDimension> sampleDimensions;

    /**
     * Creates a new resource. This resource will have its own set of listeners,
     * but the listeners of the data store that created this resource will be notified as well.
     */
    Image(final Store store, final StoreListeners parent, final int imageIndex, final GridGeometry gridGeometry) {
        super(parent);
        this.store        = store;
        this.imageIndex   = imageIndex;
        this.gridGeometry = gridGeometry;
    }

    /**
     * Returns the data store that produced this resource.
     */
    @Override
    public final DataStore getOriginator() {
        return store;
    }

    /**
     * Returns the resource identifier. The name space is the file name and
     * the local part of the name is the image index number, starting at 1.
     */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        synchronized (store) {
            if (identifier == null) {
                identifier = Names.createLocalName(store.getDisplayName(), null, String.valueOf(imageIndex + 1));
            }
            return Optional.of(identifier);
        }
    }

    /**
     * Returns the valid extent of grid coordinates together with the conversion from those grid coordinates
     * to real world coordinates. The CRS and "pixels to CRS" conversion may be unknown if this image is not
     * the {@linkplain Store#MAIN_IMAGE main image}, or if the {@code *.prj} and/or world auxiliary file has
     * not been found.
     */
    @Override
    public final GridGeometry getGridGeometry() throws DataStoreException {
        return gridGeometry;
    }

    /**
     * Returns the ranges of sample values.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public final List<SampleDimension> getSampleDimensions() throws DataStoreException {
        synchronized (store) {
            if (sampleDimensions == null) try {
                final ImageReader        reader = store.reader();
                final ImageTypeSpecifier type   = reader.getRawImageType(imageIndex);
                final SampleDimension[]  bands  = new SampleDimension[type.getNumBands()];
                final SampleDimension.Builder b = new SampleDimension.Builder();
                for (int i=0; i<bands.length; i++) {
                    /*
                     * TODO: we could consider a mechanism similar to org.apache.sis.internal.geotiff.SchemaModifier
                     * if there is a need to customize the sample dimensions. `SchemaModifier` could become a shared
                     * public interface.
                     */
                    bands[i] = b.setName(i + 1).build();
                    b.clear();
                }
                sampleDimensions = UnmodifiableArrayList.wrap(bands);
            } catch (IOException e) {
                throw new DataStoreException(e);
            }
            return sampleDimensions;
        }
    }

    /**
     * Loads a subset of the image wrapped by this resource.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  range   0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and range.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public final GridCoverage read(GridGeometry domain, final int... range) throws DataStoreException {
        synchronized (store) {
            final ImageReader reader = store.reader();
            final ImageReadParam param = reader.getDefaultReadParam();
            if (domain == null) {
                domain = gridGeometry;
            } else {
                final GridDerivation gd = gridGeometry.derive().rounding(GridRoundingMode.ENCLOSING).subgrid(domain);
                final GridExtent extent = gd.getIntersection();
                final int[] subsampling = gd.getSubsampling();
                final int[] offsets     = gd.getSubsamplingOffsets();
                domain = gd.build();
                param.setSourceSubsampling(subsampling[X_DIMENSION], subsampling[Y_DIMENSION],
                                           offsets[X_DIMENSION], offsets[Y_DIMENSION]);
                param.setSourceRegion(new Rectangle(
                        toIntExact(extent.getLow (X_DIMENSION)),
                        toIntExact(extent.getLow (Y_DIMENSION)),
                        toIntExact(extent.getSize(X_DIMENSION)),
                        toIntExact(extent.getSize(Y_DIMENSION))));
            }
            if (range != null) {
                param.setSourceBands(range);
            }
            final List<SampleDimension> sampleDimensions = getSampleDimensions();
            final RenderedImage image;
            try {
                image = reader.readAsRenderedImage(imageIndex, param);
            } catch (IOException e) {
                throw new DataStoreException(e);
            }
            return new GridCoverage2D(domain, sampleDimensions, image);
        }
    }
}
