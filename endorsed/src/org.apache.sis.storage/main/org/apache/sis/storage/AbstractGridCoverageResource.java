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

import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.concurrent.TimeUnit;
import java.math.RoundingMode;
import java.awt.image.RasterFormatException;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.util.logging.PerformanceLevel;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.internal.Resources;


/**
 * Default implementations of several methods for classes that want to implement the {@link GridCoverageResource} interface.
 * Subclasses should override the following methods:
 *
 * <ul>
 *   <li>{@link #getGridGeometry()} (mandatory)</li>
 *   <li>{@link #getSampleDimensions()} (mandatory)</li>
 * </ul>
 *
 * This class also provides the following helper methods for implementation
 * of the {@link #read(GridGeometry, int...) read(…)} method in subclasses:
 *
 * <ul>
 *   <li>{@link #canNotRead(String, GridGeometry, Throwable)} for reporting a failure to read operation.</li>
 *   <li>{@link #logReadOperation(Object, GridGeometry, long)} for logging a notice about a read operation.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.2
 */
public abstract class AbstractGridCoverageResource extends AbstractResource implements GridCoverageResource {
    /**
     * Creates a new resource, potentially as a child of another resource.
     * The parent resource is typically, but not necessarily, an {@link Aggregate}.
     *
     * @param  parent  the parent resource, or {@code null} if none.
     *
     * @since 1.4
     */
    protected AbstractGridCoverageResource(final Resource parent) {
        super(parent);
    }

    /**
     * Creates a new resource which can send notifications to the given set of listeners.
     * If {@code hidden} is {@code false} (the recommended value), then this resource will have its own set of
     * listeners with this resource declared as the {@linkplain StoreListeners#getSource() source of events}.
     * It will be possible to add and remove listeners independently from the set of parent listeners.
     * Conversely if {@code hidden} is {@code true}, then the given listeners will be used directly
     * and this resource will not appear as the source of any event.
     *
     * <p>In any cases, the listeners of all parents (ultimately the data store that created this resource)
     * will always be notified, either directly if {@code hidden} is {@code true}
     * or indirectly if {@code hidden} is {@code false}.</p>
     *
     * @param  parentListeners  listeners of the parent resource, or {@code null} if none.
     *         This is usually the listeners of the {@link DataStore} that created this resource.
     * @param  hidden  {@code false} if this resource shall use its own {@link StoreListeners}
     *         with the specified parent, or {@code true} for using {@code parentListeners} directly.
     */
    protected AbstractGridCoverageResource(final StoreListeners parentListeners, final boolean hidden) {
        super(parentListeners, hidden);
    }

    /**
     * Returns the envelope of the grid geometry if known.
     * The envelope is absent if the grid geometry does not provide this information.
     *
     * @return the grid geometry envelope.
     * @throws DataStoreException if an error occurred while computing the grid geometry.
     *
     * @see GridGeometry#getEnvelope()
     */
    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return GridCoverageResource.super.getEnvelope();
    }

    /**
     * Invoked in a synchronized block the first time that {@code getMetadata()} is invoked.
     * The default implementation populates metadata based on information provided by
     * {@link #getIdentifier()       getIdentifier()},
     * {@link #getEnvelope()         getEnvelope()},
     * {@link #getGridGeometry()     getGridGeometry()} and
     * {@link #getSampleDimensions() getSampleDimensions()}.
     * Subclasses should override if they can provide more information.
     * The default value can be completed by casting to {@link org.apache.sis.metadata.iso.DefaultMetadata}.
     *
     * @return the newly created metadata, or {@code null} if unknown.
     * @throws DataStoreException if an error occurred while reading metadata from this resource.
     */
    @Override
    protected Metadata createMetadata() throws DataStoreException {
        final var builder = new MetadataBuilder();
        builder.addDefaultMetadata(this, listeners);
        return builder.build();
    }

    /**
     * Creates an exception for a failure to load data.
     * The exception sub-type is inferred from the arguments.
     * If the failure is caused by an envelope outside the resource domain,
     * then that envelope will be inferred from the {@code request} argument.
     *
     * @param  filename  some identification (typically a file name) of the data that cannot be read.
     * @param  request   the requested domain, or {@code null} if unspecified.
     * @param  cause     the cause of the failure, or {@code null} if none.
     * @return the exception to throw.
     *
     * @see NoSuchDataException
     * @see DataStoreReferencingException
     * @see DataStoreContentException
     */
    protected DataStoreException canNotRead(final String filename, final GridGeometry request, Throwable cause) {
        final int DOMAIN = 1, REFERENCING = 2, CONTENT = 3;
        int type = 0;               // One of above constants, with 0 for "none of above".
        Envelope bounds = null;
        if (cause instanceof DisjointExtentException) {
            type = DOMAIN;
            if (request != null && request.isDefined(GridGeometry.ENVELOPE)) {
                bounds = request.getEnvelope();
            }
        } else if (cause instanceof RuntimeException) {
            Throwable c = cause.getCause();
            if (isReferencing(c)) {
                type = REFERENCING;
                cause = c;
            } else if (cause instanceof ArithmeticException || cause instanceof RasterFormatException) {
                type = CONTENT;
            }
        } else if (isReferencing(cause)) {
            type = REFERENCING;
        }
        final String message = createExceptionMessage(filename, bounds);
        switch (type) {
            case DOMAIN:      return new NoSuchDataException(message, cause);
            case REFERENCING: return new DataStoreReferencingException(message, cause);
            case CONTENT:     return new DataStoreContentException(message, cause);
            default:          return new DataStoreException(message, cause);
        }
    }

    /**
     * Returns {@code true} if the given exception is {@link FactoryException} or {@link TransformException}.
     * This is for deciding if an exception should be rethrown as an {@link DataStoreReferencingException}.
     *
     * @param  cause  the exception to verify.
     * @return whether the given exception is {@link FactoryException} or {@link TransformException}.
     */
    private static boolean isReferencing(final Throwable cause) {
        return (cause instanceof FactoryException || cause instanceof TransformException);
    }

    /**
     * Logs the execution of a {@link #read(GridGeometry, int...)} operation.
     * The log level will be {@link Level#FINE} if the operation was quick enough,
     * or {@link PerformanceLevel#SLOWNESS} or higher level otherwise.
     *
     * @param  file       the file that was opened, or {@code null} for {@link StoreListeners#getSourceName()}.
     * @param  domain     domain of the created grid coverage.
     * @param  startTime  value of {@link System#nanoTime()} when the loading process started.
     */
    protected void logReadOperation(final Object file, final GridGeometry domain, final long startTime) {
        final Logger logger = listeners.getLogger();
        final long   nanos  = System.nanoTime() - startTime;
        final Level  level  = PerformanceLevel.forDuration(nanos, TimeUnit.NANOSECONDS);
        if (logger.isLoggable(level)) {
            final Locale locale = listeners.getLocale();
            final Object[] parameters = new Object[6];
            parameters[0] = IOUtilities.filename(file != null ? file : listeners.getSourceName());
            parameters[5] = nanos / (double) Constants.NANOS_PER_SECOND;
            domain.getGeographicExtent().ifPresentOrElse((box) -> {
                final AngleFormat f = new AngleFormat(locale);
                double min = box.getSouthBoundLatitude();
                double max = box.getNorthBoundLatitude();
                f.setPrecision(max - min, true);
                f.setRoundingMode(RoundingMode.FLOOR);   parameters[1] = f.format(new Latitude(min));
                f.setRoundingMode(RoundingMode.CEILING); parameters[2] = f.format(new Latitude(max));
                min = box.getWestBoundLongitude();
                max = box.getEastBoundLongitude();
                f.setPrecision(max - min, true);
                f.setRoundingMode(RoundingMode.FLOOR);   parameters[3] = f.format(new Longitude(min));
                f.setRoundingMode(RoundingMode.CEILING); parameters[4] = f.format(new Longitude(max));
            }, () -> {
                // If no geographic coordinates, fallback on the 2 first dimensions.
                if (domain.isDefined(GridGeometry.ENVELOPE)) {
                    final Envelope box = domain.getEnvelope();
                    final int dimension = Math.min(box.getDimension(), 2);
                    for (int t=1, i=0; i<dimension; i++) {
                        parameters[t++] = box.getMinimum(i);
                        parameters[t++] = box.getMaximum(i);
                    }
                } else if (domain.isDefined(GridGeometry.EXTENT)) {
                    final GridExtent box = domain.getExtent();
                    final int dimension = Math.min(box.getDimension(), 2);
                    for (int t=1, i=0; i<dimension; i++) {
                        parameters[t++] = box.getLow (i);
                        parameters[t++] = box.getHigh(i);
                    }
                }
            });
            final LogRecord record = Resources.forLocale(locale)
                    .createLogRecord(level, Resources.Keys.LoadedGridCoverage_6, parameters);
            record.setSourceClassName(GridCoverageResource.class.getName());
            record.setSourceMethodName("read");
            record.setLoggerName(logger.getName());
            logger.log(record);
        }
    }
}
