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
package org.apache.sis.storage.base;

import java.util.List;
import java.util.Optional;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.RasterLoadingStrategy;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * A grid coverage resource which is a wrapper around another grid coverage resource.
 * Wrappers can be used for delaying data loading, modifying the identifier, completing metadata, <i>etc</i>.
 * The wrapped resource is created only when first needed.
 *
 * <p>The default implementation assumes that the wrapper only delays data loading,
 * without making substantive changes to the data. If the wrapper changes the data,
 * then a {@code DerivedGridCoverageResource} subclass should be used instead.</p>
 *
 * @todo Define {@code DerivedGridCoverageResource}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class GridResourceWrapper implements GridCoverageResource {
    /**
     * The coverage resource instance which provides the data.
     * This is initially {@code null} and created when first needed.
     */
    private volatile GridCoverageResource source;

    /**
     * Creates a new wrapper.
     */
    protected GridResourceWrapper() {
    }

    /**
     * Returns the object on which to perform all synchronizations for thread-safety.
     *
     * @return the object on which to perform synchronizations.
     */
    protected abstract Object getSynchronizationLock();

    /**
     * Creates the resource to which to delegate operations.
     * This method is invoked in a synchronized block when first needed and the result is cached.
     *
     * @return the resource to which to delegate operations.
     * @throws DataStoreException if the resource cannot be created.
     */
    protected abstract GridCoverageResource createSource() throws DataStoreException;

    /**
     * Returns the potentially cached source.
     * This method invokes {@link #createSource()} when first needed and caches the result.
     *
     * @return the resource to which to delegate operations.
     * @throws DataStoreException if the resource cannot be created.
     */
    protected final GridCoverageResource source() throws DataStoreException {
        GridCoverageResource s = source;
        if (s == null) {
            synchronized (getSynchronizationLock()) {
                s = source;
                if (s == null) {
                    source = s = createSource();
                }
            }
        }
        return s;
    }

    /**
     * Returns the resource persistent identifier.
     * The default implementation delegates to the source.
     *
     * @return a persistent identifier unique within the data store, or absent if this resource has no such identifier.
     * @throws DataStoreException if an error occurred while fetching the identifier.
     */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return source().getIdentifier();
    }

    /**
     * Returns information about this resource.
     * The default implementation delegates to the source.
     *
     * @return information about this resource. Should not be {@code null}.
     * @throws DataStoreException if an error occurred while reading the metadata.
     */
    @Override
    public Metadata getMetadata() throws DataStoreException {
        return source().getMetadata();
    }

    /**
     * Returns the spatiotemporal extent of this resource in its most natural coordinate reference system.
     * This is not necessarily the smallest bounding box encompassing all data.
     * The default implementation delegates to the source.
     *
     * @return the spatiotemporal resource extent. May be absent if none or too costly to compute.
     * @throws DataStoreException if an error occurred while reading or computing the envelope.
     */
    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return source().getEnvelope();
    }

    /**
     * Returns the valid extent of grid coordinates together with the conversion from those grid
     * coordinates to real world coordinates. The default implementation delegates to the source.
     *
     * @return extent of grid coordinates together with their mapping to "real world" coordinates.
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        return source().getGridGeometry();
    }

    /**
     * Returns the ranges of sample values together with the conversion from samples to real values.
     * The default implementation delegates to the source.
     *
     * @return ranges of sample values together with their mapping to "real values".
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return source().getSampleDimensions();
    }

    /**
     * Returns the preferred resolutions (in units of CRS axes) for read operations in this data store.
     * Elements are ordered from finest (smallest numbers) to coarsest (largest numbers) resolution.
     *
     * @return preferred resolutions for read operations in this data store, or an empty array if none.
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    @Override
    public List<double[]> getResolutions() throws DataStoreException {
        return source().getResolutions();
    }

    /**
     * Loads a subset of the grid coverage represented by this resource.
     * The default implementation delegates to the source.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  ranges  0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and ranges.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public GridCoverage read(GridGeometry domain, int... ranges) throws DataStoreException {
        return source().read(domain, ranges);
    }

    /**
     * Returns an indication about when the "physical" loading of raster data will happen.
     * The default implementation delegates to the source.
     */
    @Override
    public RasterLoadingStrategy getLoadingStrategy() throws DataStoreException {
        return source().getLoadingStrategy();
    }

    /**
     * Sets the preferred strategy about when to do the "physical" loading of raster data.
     * The default implementation delegates to the source.
     */
    @Override
    public boolean setLoadingStrategy(final RasterLoadingStrategy strategy) throws DataStoreException {
        return source().setLoadingStrategy(strategy);
    }

    /*
     * Do not override `subset(Query)`. We want the subset to delegate to this wrapper.
     */

    /**
     * Registers a listener to notify when the specified kind of event occurs in this resource or in children.
     * The default implementation delegates to the source.
     *
     * @param  <T>        compile-time value of the {@code eventType} argument.
     * @param  listener   listener to notify about events.
     * @param  eventType  type of {@link StoreEvent} to listen (cannot be {@code null}).
     */
    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final GridCoverageResource source;
        try {
            source = source();
        } catch (DataStoreException e) {
            throw new BackingStoreException(e);
        }
        source.addListener(eventType, listener);
    }

    /**
     * Unregisters a listener previously added to this resource for the given type of events.
     * The default implementation delegates to the source.
     *
     * @param  <T>        compile-time value of the {@code eventType} argument.
     * @param  listener   listener to stop notifying about events.
     * @param  eventType  type of {@link StoreEvent} which were listened (cannot be {@code null}).
     */
    @Override
    public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
        final GridCoverageResource s = source;      // No need to invoke the `source()` method here.
        if (s != null) {
            s.removeListener(eventType, listener);
        }
    }

    /**
     * Closes the data store associated to the resource, then discards the resource.
     * This method does not verify if the data store is still used by other resources.
     * This method can be invoked asynchronously for interrupting a long reading process.
     *
     * @throws DataStoreException if an error occurred while closing the data store.
     */
    public final void closeDataStore() throws DataStoreException {
        final GridCoverageResource s = source;
        source = null;
        if (s instanceof StoreResource) {
            ((StoreResource) s).getOriginator().close();
        }
    }
}
