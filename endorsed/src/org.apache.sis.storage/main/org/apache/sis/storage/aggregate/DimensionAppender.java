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
package org.apache.sis.storage.aggregate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.IllegalGridGeometryException;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.RasterLoadingStrategy;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.base.StoreUtilities;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;


/**
 * A wrapper over an existing grid coverage resource with dimensions appended.
 * This wrapper delegates the work to {@link GridCoverageProcessor} after a coverage has been read.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class DimensionAppender implements GridCoverageResource {
    /**
     * The grid coverage processor to use for creating grid coverages with extra dimensions.
     */
    private final GridCoverageProcessor processor;

    /**
     * The source grid coverage resource for which to append extra dimensions.
     */
    private final GridCoverageResource source;

    /**
     * The dimensions added to the source grid coverage.
     * Should have a grid size of one cell in all dimensions.
     */
    private final GridGeometry dimToAdd;

    /**
     * The grid geometry with dimensions appended.
     * Created when first requested.
     *
     * @see #getGridGeometry()
     */
    private volatile GridGeometry gridGeometry;

    /**
     * Creates a new dimension appender for the given grid coverage resource.
     * This constructor does not verify the grid geometry validity.
     * It is caller's responsibility to verify that the size is 1 cell.
     *
     * @param  processor  the grid coverage processor to use for creating grid coverages with extra dimensions.
     * @param  source     the source grid coverage for which to append extra dimensions.
     * @param  dimToAdd   the dimensions to add to the source grid coverage.
     */
    private DimensionAppender(final GridCoverageProcessor processor, final GridCoverageResource source, final GridGeometry dimToAdd) {
        this.processor = processor;
        this.source    = source;
        this.dimToAdd  = dimToAdd;
    }

    /**
     * Creates a grid coverage resource augmented with the given dimensions.
     * The grid extent of {@code dimToAdd} shall have a grid size of one cell in all dimensions.
     *
     * @param  processor  the grid coverage processor to use for creating grid coverages with extra dimensions.
     * @param  source     the source grid coverage for which to append extra dimensions.
     * @param  dimToAdd   the dimensions to add to the source grid coverage.
     * @throws IllegalGridGeometryException if a dimension has more than one grid cell.
     */
    static GridCoverageResource create(final GridCoverageProcessor processor, GridCoverageResource source, GridGeometry dimToAdd) {
        ArgumentChecks.ensureNonNull("source", source);
        final GridExtent extent = dimToAdd.getExtent();
        int i = extent.getDimension();
        if (i == 0) {
            return source;
        }
        do {
            final long size = extent.getSize(--i);
            if (size != 1) {
                Object name = extent.getAxisType(i).orElse(null);
                if (name == null) name = i;
                throw new IllegalGridGeometryException(Resources.format(Resources.Keys.NoSliceSpecified_2, name, size));
            }
        } while (i != 0);
        if (source instanceof DimensionAppender) try {
            final var a = (DimensionAppender) source;
            dimToAdd = new GridGeometry(a.dimToAdd, dimToAdd);
            source = a.source;
        } catch (FactoryException e) {
            throw new IllegalGridGeometryException(e.getMessage(), e);
        }
        return new DimensionAppender(processor, source, dimToAdd);
    }

    /**
     * Returns the identifier of the original resource.
     */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return source.getIdentifier();
    }

    /**
     * Returns the metadata of the original resource.
     */
    @Override
    public Metadata getMetadata() throws DataStoreException {
        return source.getMetadata();
    }

    /**
     * Returns the sample dimensions of the original resource.
     * Those dimensions are not impacted by the change of domain dimensions.
     */
    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return source.getSampleDimensions();
    }

    /**
     * Returns the grid geometry of the original resource augmented with the dimensions to append.
     */
    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        GridGeometry gg = gridGeometry;
        if (gg == null) try {
            gg = new GridGeometry(source.getGridGeometry(), dimToAdd);
            gridGeometry = gg;
        } catch (FactoryException | RuntimeException e) {
            throw new DataStoreReferencingException(e.getMessage(), e);
        }
        return gg;
    }

    /**
     * Returns a subset of this grid coverage resource.
     * The result will have the same "dimensions to add" than this resource.
     *
     * @param  query  the query to execute.
     * @return subset of this coverage resource.
     */
    @Override
    public GridCoverageResource subset(final Query query) throws DataStoreException {
        final GridCoverageResource subset = source.subset(query);
        if (subset == source) return this;
        return new DimensionAppender(processor, subset, dimToAdd);
    }

    /**
     * Reads the data and wraps the result with the dimensions to add.
     */
    @Override
    public GridCoverage read(GridGeometry domain, int... ranges) throws DataStoreException {
        return processor.appendDimensions(source.read(domain, ranges), dimToAdd);
    }

    /**
     * Returns an indication about when the "physical" loading of raster data will happen.
     */
    @Override
    public RasterLoadingStrategy getLoadingStrategy() throws DataStoreException {
        return source.getLoadingStrategy();
    }

    /**
     * Sets the preferred strategy about when to do the "physical" loading of raster data.
     */
    @Override
    public boolean setLoadingStrategy(RasterLoadingStrategy strategy) throws DataStoreException {
        return source.setLoadingStrategy(strategy);
    }

    /**
     * Registers a listener to notify when the specified kind of event occurs in this resource or in children.
     */
    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        source.addListener(eventType, listener);
    }

    /**
     * Unregisters a listener previously added to this resource for the given type of events.
     */
    @Override
    public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
        source.removeListener(eventType, listener);
    }

    /**
     * Returns a string representation of this wrapper for debugging purposes.
     */
    @Override
    public String toString() {
        final var sb = new StringBuilder(40);
        sb.append(source).append(" + dimensions[");
        final GridExtent extent = dimToAdd.getExtent();
        final double[] coordinates = ArraysExt.copyAsDoubles(extent.getLow().getCoordinateValues());
        try {
            dimToAdd.getGridToCRS(PixelInCell.CELL_CORNER).transform(coordinates, 0, coordinates, 0, 1);
        } catch (RuntimeException | TransformException e) {
            // Should never happen because the transform should be linear.
            Logging.unexpectedException(StoreUtilities.LOGGER, DimensionAppender.class, "toString", e);
            Arrays.fill(coordinates, Double.NaN);
        }
        for (int i=0; i<coordinates.length; i++) {
            if (i != 0) sb.append(", ");
            extent.getAxisType(i).ifPresent((type) -> sb.append(type.name()).append('='));
            sb.append(coordinates[i]);
        }
        return sb.append(']').toString();
    }
}
