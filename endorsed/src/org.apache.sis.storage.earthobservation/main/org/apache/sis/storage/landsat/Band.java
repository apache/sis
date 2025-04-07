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
package org.apache.sis.storage.landsat;

import java.nio.file.Path;
import java.util.Optional;
import org.opengis.util.LocalName;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.content.CoverageContentType;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataOptionKey;
import org.apache.sis.storage.geotiff.GeoTiffStore;
import org.apache.sis.storage.modifier.CoverageModifier;
import org.apache.sis.storage.base.GridResourceWrapper;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.content.DefaultImageDescription;
import org.apache.sis.metadata.iso.content.DefaultAttributeGroup;
import org.apache.sis.metadata.iso.content.DefaultSampleDimension;
import org.apache.sis.metadata.iso.content.DefaultBand;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;
import static org.apache.sis.util.privy.CollectionsExt.first;


/**
 * A band in a Landsat data set. Each band is represented by a separated GeoTIFF file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Band extends GridResourceWrapper implements CoverageModifier {
    /**
     * The data store that contains this band.
     * Also the object on which to perform synchronization locks.
     */
    private final LandsatStore parent;

    /**
     * The band for which this instance provides data.
     */
    final BandName band;

    /**
     * Identifier of the band for which this instance provides data.
     * Should not be modified after the end of metadata parsing.
     *
     * @see #getIdentifier()
     */
    LocalName identifier;

    /**
     * Filename of the file to read for band data.
     * This is relative to {@link LandsatStore#directory}.
     * Should not be modified after the end of metadata parsing.
     */
    String filename;

    /**
     * Metadata about the band.
     * Should not be modified after the end of metadata parsing.
     */
    final DefaultSampleDimension sampleDimension;

    /**
     * Creates a new resource for the specified band.
     */
    Band(final LandsatStore parent, final BandName band) {
        this.parent = parent;
        this.band   = band;
        if (band.wavelength != 0) {
            final DefaultBand b = new DefaultBand();
            b.setPeakResponse((double) band.wavelength);
            b.setBoundUnits(Units.NANOMETRE);
            sampleDimension = b;
        } else {
            sampleDimension = new DefaultSampleDimension();
        }
        sampleDimension.setDescription(band.title);
        // Can not set units in GeoAPI 3.0 because the API is restricted to units of length.
    }

    /**
     * Returns the object on which to perform all synchronizations for thread-safety.
     */
    @Override
    protected final Object getSynchronizationLock() {
        return parent;
    }

    /**
     * Creates the GeoTIFF reader and get the first image from it.
     */
    @Override
    protected GridCoverageResource createSource() throws DataStoreException {
        final Path file;
        if (parent.directory != null) {
            file = parent.directory.resolve(filename);
        } else {
            file = Path.of(filename);
        }
        final StorageConnector connector = new StorageConnector(file);
        connector.setOption(DataOptionKey.COVERAGE_MODIFIER, this);
        return new GeoTiffStore(parent, parent.getProvider(), connector, true).components().get(0);
    }

    /**
     * Returns the resource persistent identifier. The name is the {@link BandName#name()}
     * and the scope (namespace) is the name of the directory that contains this band.
     */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.of(identifier);
    }

    /**
     * Returns whether the given source is for the main image.
     */
    private static boolean isMain(final Source source) {
        return source.getCoverageIndex().orElse(-1) == 0;
    }

    /**
     * Invoked when the GeoTIFF reader creates the resource identifier.
     * We use the identifier of the enclosing {@link Band}.
     */
    @Override
    public GenericName customize(final Source source, final GenericName fallback) {
        return isMain(source) ? identifier : fallback;
    }

    /**
     * Invoked when the GeoTIFF reader creates a metadata.
     * This method modifies or completes some information inferred by the GeoTIFF reader.
     */
    @Override
    public Metadata customize(final Source source, final DefaultMetadata metadata) {
        if (isMain(source)) {
            for (final Identification id : metadata.getIdentificationInfo()) {
                final var c = (DefaultCitation) id.getCitation();
                if (c != null) {
                    c.setTitle(band.title);
                    break;
                }
            }
            /*
             * All collections below should be singleton and all casts should be safe because we use
             * one specific implementation (`GeoTiffStore`) which is known to build metadata that way.
             * A ClassCastException would be a bug in the handling of `isElectromagneticMeasurement(…)`.
             */
            final var content = (DefaultImageDescription) first(metadata.getContentInfo());
            final var group   = (DefaultAttributeGroup)   first(content.getAttributeGroups());
            final var sd      = (DefaultSampleDimension)  first(group.getAttributes());
            group.getContentTypes().add(CoverageContentType.PHYSICAL_MEASUREMENT);
            sd.setDescription(sampleDimension.getDescription());
            sd.setMinValue   (sampleDimension.getMinValue());
            sd.setMaxValue   (sampleDimension.getMaxValue());
            sd.setScaleFactor(sampleDimension.getScaleFactor());
            sd.setOffset     (sampleDimension.getOffset());
            sd.setUnits      (sampleDimension.getUnits());
            if (sampleDimension instanceof DefaultBand) {
                final DefaultBand s = (DefaultBand) sampleDimension;
                final DefaultBand t = (DefaultBand) sd;
                t.setPeakResponse(s.getPeakResponse());
                t.setBoundUnits(s.getBoundUnits());
            }
        }
        return metadata;
    }

    /**
     * Invoked when a sample dimension is created for a band in an image.
     */
    @Override
    public SampleDimension customize(final BandSource source, final SampleDimension.Builder dimension) {
        if (isMain(source) && source.getBandIndex() == 0) {
            dimension.setName(identifier);
            final NumberRange<?> sampleRange = source.getSampleRange().orElse(null);
            if (sampleRange != null) {
                final Number min    = sampleRange.getMinValue();
                final Number max    = sampleRange.getMaxValue();
                final Double scale  = sampleDimension.getScaleFactor();
                final Double offset = sampleDimension.getOffset();
                if (min != null && max != null && scale != null && offset != null) {
                    int lower = min.intValue();
                    if (lower >= 0) {           // Should always be zero but we are paranoiac.
                        dimension.addQualitative(null, 0);
                        if (lower == 0) lower = 1;
                    }
                    dimension.addQuantitative(this.band.group.measurement, lower, max.intValue(),
                                              scale, offset, sampleDimension.getUnits());
                }
            }
        }
        return dimension.build();
    }

    /**
     * Returns {@code true} if the converted values are measurement in the electromagnetic spectrum.
     */
    @Override
    public boolean isElectromagneticMeasurement(final Source source) {
        return isMain(source) && band.wavelength != 0;
    }
}
