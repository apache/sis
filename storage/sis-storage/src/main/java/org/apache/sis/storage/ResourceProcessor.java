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

import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.coverage.grid.IncompleteGridGeometryException;
import org.apache.sis.image.DataType;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.internal.storage.ConvertedCoverageResource;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.ArgumentChecks.ensureValidIndex;

/**
 * A predefined set of operations on resources as convenience methods.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.3
 * @since 1.3
 * @module
 */
public class ResourceProcessor implements Cloneable {

    private final GridCoverageProcessor processor;

    /**
     * Creates a new processor with default configuration.
     */
    public ResourceProcessor() { this(null); }

    public ResourceProcessor(GridCoverageProcessor processor) {
        this.processor = processor == null ? new GridCoverageProcessor() : processor;
    }

    /**
     * @return The processor used internally to transform produced {@link GridCoverage grid coverages}. Not null.
     */
    public GridCoverageProcessor getProcessor() { return processor; }

    @Override
    public ResourceProcessor clone() {
        return new ResourceProcessor(processor.clone());
    }

    /**
     * Returns a coverage resource with sample values converted by the given functions.
     * The number of sample dimensions in the returned coverage is the length of the {@code converters} array,
     * which must be greater than 0 and not greater than the number of sample dimensions in the source coverage.
     * If the {@code converters} array length is less than the number of source sample dimensions,
     * then all sample dimensions at index ≥ {@code converters.length} will be ignored.
     *
     * <h4>Sample dimensions customization</h4>
     * By default, this method creates new sample dimensions with the same names and categories than in the
     * previous coverage, but with {@linkplain org.apache.sis.coverage.Category#getSampleRange() sample ranges}
     * converted using the given converters and with {@linkplain SampleDimension#getUnits() units of measurement}
     * omitted. This behavior can be modified by specifying a non-null {@code sampleDimensionModifier} function.
     * If non-null, that function will be invoked with, as input, a pre-configured sample dimension builder.
     * The {@code sampleDimensionModifier} function can {@linkplain SampleDimension.Builder#setName(CharSequence)
     * change the sample dimension name} or {@linkplain SampleDimension.Builder#categories() rebuild the categories}.
     *
     * <h4>Result relationship with source</h4>
     * If the source coverage is backed by a {@link java.awt.image.WritableRenderedImage},
     * then changes in the source coverage are reflected in the returned coverage and conversely.
     *
     * @see GridCoverageProcessor#convert(org.apache.sis.coverage.grid.GridCoverage, org.opengis.referencing.operation.MathTransform1D[], java.util.function.Function)
     * @see ImageProcessor#convert(RenderedImage, NumberRange[], MathTransform1D[], DataType, ColorModel)
     */
    public GridCoverageResource convert(final GridCoverageResource source, MathTransform1D[] converters,
            Function<SampleDimension.Builder, SampleDimension> sampleDimensionModifier)
    {
        return new ConvertedCoverageResource(source, converters, sampleDimensionModifier);
    }

    /**
     * Wraps a given resource with a resample operator, to ensure it fits a provided grid geometry.
     * Neither resampling nor read is triggered immediately. Instead, a virtual resource is returned.
     * It will launch read then resample operations upon call to {@link GridCoverageResource#read(GridGeometry, int...)}.
     *
     * @param source Resource to resample. Must not be null.
     * @param target Grid geometry to use for output/resampled resource. Must not be null.
     * @param targetName An optional name for returned resource. If null, output {@link Resource#getIdentifier() resource identifier} will not be present.
     * @return A resource decorating provided one. It triggers a {@link GridCoverageProcessor#resample(GridCoverage, GridGeometry) resampling operation} upon reads. Never null.
     */
    public GridCoverageResource resample(final GridCoverageResource source, final GridGeometry target, GenericName targetName) {
        return new ResampledGridCoverageResource(source, target, targetName, processor);
    }

    /**
     * Reprojects provided resource. Note that:
     * <ul>
     *     <li>Provided resource metadata and grid geometry will be immediately fetched</li>
     *     <li>Resampling will be postponed until a call to {@link GridCoverageResource#read(GridGeometry, int...)}</li>
     * </ul>
     * @return Either the input resource if no reprojection is needed for conversion to target CRS. Otherwise, a virtual dataset performing resample on read.
     * @throws DataStoreException If input resource metadata or grid geometry cannot be acquired.
     * @throws FactoryException If referencing database is not reachable, or if it is not possible to find any valid operation from input resource system to provided CRS.
     * @throws TransformException If an error occurs while transforming input resource geometry to the target CRS.
     * @throws IncompleteGridGeometryException If input resource geometry does not provide enough information to build a resampling pipeline (i.e. No CRS or no envelope).
     */
    public GridCoverageResource resample(final GridCoverageResource source, final CoordinateReferenceSystem target, GenericName targetName) throws DataStoreException, FactoryException, TransformException {
        ensureNonNull("Source", source);
        ensureNonNull("Target CRS", target);
        final GridGeometry sourceGeom = source.getGridGeometry();
        final CoordinateReferenceSystem sourceCrs = sourceGeom.getCoordinateReferenceSystem();

        final GridGeometry reprojected;
        if (sourceGeom.isDefined(GridGeometry.GRID_TO_CRS + GridGeometry.EXTENT)) {
            final CoordinateOperation op = CRS.findOperation(sourceCrs, target, searchGeographicExtent(source).orElse(null));
            if (op.getMathTransform() == null || op.getMathTransform().isIdentity()) return source;
            reprojected = new GridGeometry(sourceGeom.getExtent(), PixelInCell.CELL_CENTER,
                    MathTransforms.concatenate(sourceGeom.getGridToCRS(PixelInCell.CELL_CENTER), op.getMathTransform()), target);
        } else if (sourceGeom.isDefined(GridGeometry.ENVELOPE)) {
            reprojected = new GridGeometry(null, null, sourceGeom.getEnvelope(target), GridRoundingMode.ENCLOSING);
        } else throw new IncompleteGridGeometryException("Cannot reproject a grid coverage resource whose geometry defines neither an envelope nor a conversion for grid to CRS");

        return new ResampledGridCoverageResource(source, reprojected, targetName, processor);
    }

    public GridCoverageResource aggregateBands(GridCoverageResource... bands) throws DataStoreException {
        return aggregateBands(null, Arrays.asList(bands), null, null);
    }

    public GridCoverageResource aggregateBands(GenericName name, List<GridCoverageResource> resources, List<int[]> bandSelections, ColorModel userColors) throws DataStoreException {
        ensureNonNull("resources", resources);
        if (bandSelections != null) ensureValidIndex(resources.size(), bandSelections.size() - 1);
        List<BandAggregateGridResource.BandSelection> selections = new ArrayList<>(resources.size());
        for (int i = 0 ; i < resources.size() ; i++) {
            int[] bands = (bandSelections == null || bandSelections.size() <= i) ? null : bandSelections.get(i);
            selections.add(new BandAggregateGridResource.BandSelection(resources.get(i), bands));
        }
        return new BandAggregateGridResource(name, selections, userColors);
    }

    private static Optional<GeographicBoundingBox> searchGeographicExtent(GridCoverageResource source) throws DataStoreException {
        final Optional<GeographicBoundingBox> bbox = source.getMetadata().getIdentificationInfo().stream()
                .flatMap(it -> it.getExtents().stream())
                .flatMap(it -> it.getGeographicElements().stream())
                .filter(GeographicBoundingBox.class::isInstance)
                .map(it -> (GeographicBoundingBox) it)
                .reduce(ResourceProcessor::union);

        if (bbox.isPresent()) return bbox;

        return source.getEnvelope()
                .map(it -> {
                    DefaultGeographicBoundingBox g = new DefaultGeographicBoundingBox();
                    try {
                        g.setBounds(it);
                    } catch (TransformException e) {
                        Logger.getLogger(Loggers.COORDINATE_OPERATION)
                                .log(Level.FINE, "Cannot extract geographic extent from source resource", e);
                        return null;
                    }
                    return g;
                });
    }

    private static GeographicBoundingBox union(GeographicBoundingBox g1, GeographicBoundingBox g2) {
        final DefaultGeographicBoundingBox union = new DefaultGeographicBoundingBox(g1);
        union.add(g2);
        return union;
    }
}
