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
package org.apache.sis.storage.image;

import java.util.List;
import java.util.Optional;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.awt.image.BandedSampleModel;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import static java.lang.Math.toIntExact;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.storage.AbstractGridCoverageResource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.storage.base.StoreResource;
import static org.apache.sis.storage.modifier.CoverageModifier.BandSource;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.coverage.privy.RangeArgument;
import org.apache.sis.image.privy.ImageUtilities;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.privy.UnmodifiableArrayList;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.iso.Names;


/**
 * A single image in a {@link WorldFileStore}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
class WorldFileResource extends AbstractGridCoverageResource implements StoreResource {
    /**
     * The dimensions of <var>x</var> and <var>y</var> axes.
     * Static constants for now, may become configurable fields in the future.
     */
    static final int X_DIMENSION = 0, Y_DIMENSION = 1;

    /**
     * The parent data store, or {@code null} if this resource is not valid anymore.
     */
    private volatile WorldFileStore store;

    /**
     * Index of the image to read or write in the image file. This is usually 0.
     * May be decremented when other resources are {@linkplain WritableStore#remove removed}.
     *
     * @see #getImageIndex()
     */
    private int imageIndex;

    /**
     * The identifier as a sequence number in the namespace of the {@link WorldFileStore}.
     * The first image has the sequence number "1". This is computed when first needed.
     * {@link WritableResource} have no identifier because the numbers may change.
     *
     * @see #getIdentifier()
     */
    private GenericName identifier;

    /**
     * The grid geometry of this resource. The grid extent is the image size.
     *
     * @see #getGridGeometry()
     */
    private GridGeometry gridGeometry;

    /**
     * The ranges of sample values, computed when first needed. Shall be an unmodifiable list.
     *
     * @see #getSampleDimensions()
     */
    private List<SampleDimension> sampleDimensions;

    /**
     * Cached coverage for the full image, or {@code null} if none.
     */
    private SoftReference<GridCoverage> fullCoverage;

    /**
     * Creates a new resource. This resource will have its own set of listeners,
     * but the listeners of the data store that created this resource will be notified as well.
     */
    WorldFileResource(final WorldFileStore store, final StoreListeners parent,
                      final int imageIndex, final GridGeometry gridGeometry)
    {
        super(parent, store.isComponentHidden());
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
     * Returns the data store.
     *
     * @throws DataStoreException if this resource is not valid anymore.
     */
    final WorldFileStore store() throws DataStoreException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final WorldFileStore store = this.store;
        if (store != null) {
            return store;
        }
        throw new DataStoreException(Resources.format(Resources.Keys.ResourceRemoved));
    }

    /**
     * Returns the index of the image to read or write in the image file. This is usually 0.
     * Note that contrarily to {@link #getIdentifier()}, this index is not guaranteed to be constant.
     */
    final int getImageIndex() {
        return imageIndex;
    }

    /**
     * Decrements the image index. This is needed if images before this image have been removed.
     */
    final void decrementImageIndex() throws DataStoreException {
        getIdentifier();    // For identifier creation for keeping it constant.
        imageIndex--;
    }

    /**
     * Returns the resource identifier. The name space is the file name and
     * the local part of the name is the image index number, starting at 1.
     * This identifier should be constant.
     */
    @Override
    public final Optional<GenericName> getIdentifier() throws DataStoreException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final WorldFileStore store = store();
        synchronized (store) {
            if (identifier == null) {
                // TODO: get `base` from image metadata if available.
                final String base = String.valueOf(getImageIndex() + 1);
                String id = base;
                int n = 0;
                while (store.identifiers.putIfAbsent(id, Boolean.TRUE) != null) {
                    if (--n >= 0) {
                        throw new ArithmeticException();    // Paranoiac safety for avoiding never-ending loop.
                    }
                    id = base + n;
                }
                /*
                 * Get the filename and omit the extension. The `store.suffix` field is null if the input
                 * source was not a File/Path/URI/URL, in which case we do not try to trim the extension.
                 */
                String filename = store.getDisplayName();
                if (store.suffix != null) {
                    filename = IOUtilities.filenameWithoutExtension(filename);
                }
                GenericName name = Names.createLocalName(filename, null, id).toFullyQualifiedName();
                identifier = store.customizer.customize(store.source(imageIndex), name);
            }
            return Optional.of(identifier);
        }
    }

    /**
     * Returns the valid extent of grid coordinates together with the conversion from those grid coordinates
     * to real world coordinates. The CRS and "pixels to CRS" conversion may be unknown if this image is not
     * the {@link WorldFileStore#MAIN_IMAGE main image}, or if the {@code *.prj} and/or world auxiliary file has
     * not been found.
     */
    @Override
    public final GridGeometry getGridGeometry() throws DataStoreException {
        synchronized (store()) {
            return gridGeometry;
        }
    }

    /**
     * Returns the ranges of sample values in each band. Those sample dimensions describe colors
     * because the World File format does not provide more information.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public final List<SampleDimension> getSampleDimensions() throws DataStoreException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final WorldFileStore store = store();
        synchronized (store) {
            if (sampleDimensions == null) try {
                final ImageReader        reader = store.reader();
                final ImageTypeSpecifier type   = reader.getRawImageType(getImageIndex());
                final SampleDimension[]  bands  = new SampleDimension[type.getNumBands()];
                final SampleDimension.Builder b = new SampleDimension.Builder();
                final short[] names = ImageUtilities.bandNames(type.getColorModel(), type.getSampleModel());
                for (int i=0; i<bands.length; i++) {
                    final InternationalString name;
                    final short k;
                    if (i < names.length && (k = names[i]) != 0) {
                        name = Vocabulary.formatInternational(k);
                    } else {
                        name = Vocabulary.formatInternational(Vocabulary.Keys.Band_1, i+1);
                    }
                    var source = new BandSource(store, imageIndex, i, bands.length, null);
                    bands[i] = store.customizer.customize(source, b.setName(name));
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
     * @param  ranges  0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and ranges.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public final GridCoverage read(GridGeometry domain, int... ranges) throws DataStoreException {
        final boolean isFullCoverage = (domain == null && ranges == null);
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final WorldFileStore store = store();
        try {
            synchronized (store) {
                if (isFullCoverage && fullCoverage != null) {
                    final GridCoverage coverage = fullCoverage.get();
                    if (coverage != null) {
                        return coverage;
                    }
                    fullCoverage = null;
                }
                final ImageReader reader = store.reader();
                final ImageReadParam param = reader.getDefaultReadParam();
                if (domain == null) {
                    domain = gridGeometry;
                } else {
                    final GridDerivation  gd = gridGeometry.derive().rounding(GridRoundingMode.ENCLOSING).subgrid(domain);
                    final GridExtent  extent = gd.getIntersection();
                    final long[] subsampling = gd.getSubsampling();
                    final long[] offsets     = gd.getSubsamplingOffsets();
                    final long   subX        = subsampling[X_DIMENSION];
                    final long   subY        = subsampling[Y_DIMENSION];
                    final Rectangle region   = new Rectangle(
                            toIntExact(extent.getLow (X_DIMENSION)),
                            toIntExact(extent.getLow (Y_DIMENSION)),
                            toIntExact(extent.getSize(X_DIMENSION)),
                            toIntExact(extent.getSize(Y_DIMENSION)));
                    /*
                     * Ths subsampling offset Δx is defined differently in Image I/O and `GridGeometry`.
                     * The conversion from coordinate x in subsampled image to xₒ in original image is:
                     *
                     *     Image I/O:     xₒ = xᵣ + (x⋅s + Δx′)
                     *     GridGeometry:  xₒ = (truncate(xᵣ/s) + x)⋅s + Δx
                     *
                     * Where xᵣ is the lower coordinate of `region`, s is the subsampling and
                     * `truncate(xᵣ/s)` is given by the lower coordinate of subsampled extent.
                     * Rearranging equations:
                     *
                     *     Δx′ = truncate(xᵣ/s)⋅s + Δx - xᵣ
                     */
                    domain = gd.build();
                    GridExtent subExtent = domain.getExtent();
                    param.setSourceRegion(region);
                    param.setSourceSubsampling(
                            toIntExact(subX),
                            toIntExact(subY),
                            toIntExact(subExtent.getLow(X_DIMENSION) * subX + offsets[X_DIMENSION] - region.x),
                            toIntExact(subExtent.getLow(Y_DIMENSION) * subY + offsets[Y_DIMENSION] - region.y));
                }
                /*
                 * If a subset of the bands is requested, ideally we should forward this request to the `ImageReader`.
                 * But experience suggests that not all `ImageReader` implementations support band subsetting well.
                 * This code applies heuristic rules forwarding the request to the image reader only for what should
                 * be the easiest cases. More difficult cases will be handled after the reading.
                 * Those heuristic rules may be changed in any future version.
                 */
                List<SampleDimension> bands = getSampleDimensions();
                if (ranges != null) {
                    final ImageTypeSpecifier type = reader.getRawImageType(getImageIndex());
                    final RangeArgument args = RangeArgument.validate(type.getNumBands(), ranges, listeners);
                    if (args.isIdentity()) {
                        ranges = null;
                    } else {
                        bands = UnmodifiableArrayList.wrap(args.select(bands));
                        if (args.hasAllBands || type.getSampleModel() instanceof BandedSampleModel) {
                            ranges = args.getSelectedBands();
                            param.setSourceBands(ranges);
                            param.setDestinationBands(ArraysExt.range(0, ranges.length));
                            ranges = null;
                        }
                    }
                }
                RenderedImage image = reader.readAsRenderedImage(getImageIndex(), param);
                /*
                 * If the reader was presumed unable to handle the band subsetting, apply it now.
                 * It waste some memory because unused bands still in memory. But we do that as a
                 * workaround for limitations in some `ImageReader` implementations.
                 */
                if (ranges != null) {
                    image = new ImageProcessor().selectBands(image, ranges);
                }
                final GridCoverage coverage = new GridCoverage2D(domain, bands, image);
                if (isFullCoverage) {
                    fullCoverage = new SoftReference<>(coverage);
                }
                return coverage;
            }
        } catch (IOException | RuntimeException e) {
            throw canNotRead(store.getDisplayName(), domain, e);
        }
    }

    /**
     * Sets the grid coverage to the given value.
     * This is used during write operations only.
     */
    final void setGridCoverage(final GridCoverage coverage) {
        sampleDimensions = coverage.getSampleDimensions();
        gridGeometry     = coverage.getGridGeometry();
        fullCoverage     = new SoftReference<>(coverage);
    }

    /**
     * Notifies this resource that it should not be used anymore.
     */
    final void dispose() {
        if (identifier != null) {
            // For information purpose but not really used.
            store.identifiers.put(identifier.tip().toString(), Boolean.FALSE);
        }
        store            = null;
        identifier       = null;
        sampleDimensions = null;
        gridGeometry     = null;
        fullCoverage     = null;
    }
}
