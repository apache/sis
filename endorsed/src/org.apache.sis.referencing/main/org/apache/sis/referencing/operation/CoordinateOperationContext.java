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
package org.apache.sis.referencing.operation;

import java.util.Locale;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.function.Predicate;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Localized;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;


/**
 * Optional information about the context in which a requested coordinate operation will be used.
 * The context can provide information such as:
 *
 * <ul>
 *   <li>The geographic area where the transformation will be used.</li>
 *   <li>The desired accuracy. A coarser accuracy may allow <abbr>SIS</abbr> to choose a faster transformation method.</li>
 * </ul>
 *
 * While optional, those information can help {@link DefaultCoordinateOperationFactory}
 * to choose the most suitable coordinate transformation between two CRS.
 *
 * <p>{@code CoordinateOperationContext} is part of the <abbr>API</abbr>
 * used by <abbr>SIS</abbr> for implementing the <i>late binding</i> model.
 * See {@linkplain org.apache.sis.referencing.operation package javadoc}
 * for a note on early binding versus late binding implementations.</p>
 *
 * <h2>Example</h2>
 * If a transformation from NAD27 to WGS84 is requested without providing context, then Apache SIS will return the
 * transformation applicable to the widest North American surface. But if the user provides a context saying that
 * he wants to transform coordinates in Texas, then Apache SIS may return another coordinate transformation with
 * different {@linkplain org.apache.sis.referencing.datum.BursaWolfParameters Bursa-Wolf parameters} more suitable
 * to Texas, but not suitable to the rest of North-America.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 * @since   0.7
 *
 * @todo Should also take the country of a {@link java.util.Locale}. The EPSG database contains ISO2 and ISO3
 *       identifiers that we can use.
 */
public class CoordinateOperationContext implements Localized {
    /**
     * The spatiotemporal area of interest, or {@code null} if none. This instance may be updated or
     * replaced by other methods in this class, or (indirectly) by {@link CoordinateOperationFinder}.
     */
    private DefaultExtent areaOfInterest;

    /**
     * The desired accuracy in metres, or 0 for the best accuracy available.
     * See {@link #getDesiredAccuracy()} for more details about what we mean by <q>best accuracy</q>.
     */
    private double desiredAccuracy;

    /**
     * Coordinate values that can be considered as constant, or {@code null} if none.
     *
     * @see #getConstantCoordinates()
     */
    private DirectPosition constantCoordinates;

    /**
     * The locale for error messages, or {@code null} for the default locale.
     */
    private Locale locale;

    /**
     * Whether to log a warning.
     */
    private transient Filter logFilter;

    /**
     * Creates a new context with no area of interest and the best accuracy available.
     */
    public CoordinateOperationContext() {
    }

    /**
     * Creates a new context with the given area of interest and desired accuracy.
     *
     * @param area      the area of interest, or {@code null} if none.
     * @param accuracy  the desired accuracy in metres, or 0 for the best accuracy available.
     * See {@link #getDesiredAccuracy()} for more details about what we mean by <q>best accuracy</q>.
     */
    public CoordinateOperationContext(final Extent area, final double accuracy) {
        ArgumentChecks.ensurePositive("accuracy", accuracy);
        if (area != null) {
            areaOfInterest = new DefaultExtent(area);
        }
        desiredAccuracy = accuracy;
    }

    /**
     * Creates an operation context for the given area of interest, which may be null or
     * {@link org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#isEmpty() undefined}.
     * This is a convenience method for a frequently-used operation.
     *
     * @param  areaOfInterest  the area of interest, or {@code null} if none.
     * @return the operation context, or {@code null} if the given bounding box was null, undefined
     *         or covers the whole world (in which case Apache SIS does not need that we specify a context).
     *
     * @since 1.0
     */
    public static CoordinateOperationContext fromBoundingBox(final GeographicBoundingBox areaOfInterest) {
        if (areaOfInterest != null) {
            /*
             * If the area of interest covers the world, we omit creating a context in order to make
             * easier for DefaultCoordinateOperationFactory to detect that it can use its cache.
             */
            if (areaOfInterest.getSouthBoundLatitude() >  Latitude.MIN_VALUE ||
                areaOfInterest.getNorthBoundLatitude() <  Latitude.MAX_VALUE ||
                areaOfInterest.getWestBoundLongitude() > Longitude.MIN_VALUE ||
                areaOfInterest.getEastBoundLongitude() < Longitude.MAX_VALUE)
            {
                final var context = new CoordinateOperationContext();
                context.setAreaOfInterest(areaOfInterest);
                return context;
            }
        }
        return null;
    }

    /**
     * Returns the spatiotemporal area of interest, or {@code null} if none.
     *
     * @return the spatiotemporal area of interest, or {@code null} if none.
     *
     * @see Extents#getGeographicBoundingBox(Extent)
     */
    public Extent getAreaOfInterest() {
        return areaOfInterest;
    }

    /**
     * Sets the spatiotemporal area of interest, or {@code null} if none.
     *
     * @param  area  the spatiotemporal area of interest, or {@code null} if none.
     */
    public void setAreaOfInterest(final Extent area) {
        areaOfInterest = (area != null) ? new DefaultExtent(area) : null;
    }

    /**
     * Sets the geographic element of the area of interest, or {@code null} if none.
     * This convenience method sets the given bounding box into the spatiotemporal {@link Extent}.
     *
     * <p>The reverse operation can be done with <code>{@linkplain Extents#getGeographicBoundingBox(Extent)
     * Extents.getGeographicBoundingBox}({@linkplain #getAreaOfInterest()})</code>.</p>
     *
     * @param  area  the new geographic area of interest, or {@code null} if none.
     */
    public void setAreaOfInterest(final GeographicBoundingBox area) {
        areaOfInterest = setGeographicBoundingBox(areaOfInterest, area);
    }

    /**
     * Adds a geographic element of the area of interest.
     * The given area is added to the list returned by {@link Extent#getGeographicElements()}.
     * If the list contains two or more elements, {@link DefaultCoordinateOperationFactory}
     * will use the union of all elements.
     *
     * @param  area  the geographic area of interest to add, or {@code null} if none.
     * @since 1.6
     */
    public void addAreaOfInterest(final GeographicBoundingBox area) {
        if (area != null) {
            if (areaOfInterest == null) {
                setAreaOfInterest(area);
            } else {
                areaOfInterest.getGeographicElements().add(area);
            }
        }
    }

    /**
     * Adds the geographic, vertical and temporal elements of an envelope as areas of interest.
     * The geographic element is added to the list returned by {@link Extent#getGeographicElements()},
     * and likewise for vertical and temporal components. If the envelope cannot be transformed to the
     * <abbr>CRS</abbr> expected by a component list, then the exception is reported to the
     * {@linkplain #getLogFilter() log filter} and eventually logged.
     *
     * @param area  the envelope to add as an area of interest, or {@code null} if none.
     * @since 1.6
     */
    public void addAreaOfInterest(final Envelope area) {
        if (area != null) try {
            if (areaOfInterest == null) {
                areaOfInterest = new DefaultExtent();
            }
            areaOfInterest.addElements(area);
        } catch (TransformException e) {
            recoverableException("addAreaOfInterest", e);
        }
    }

    /**
     * Sets the given geographic bounding box in the given extent.
     * Modifies the extent given in parameters if non-null, or otherwise creates a new extent.
     *
     * @param  areaOfInterest  the extent to update, or {@code null}.
     * @return the updated extent. May be the given one or a new instance.
     */
    static DefaultExtent setGeographicBoundingBox(DefaultExtent areaOfInterest, final GeographicBoundingBox bbox) {
        if (areaOfInterest != null) {
            areaOfInterest.setGeographicElements(Containers.singletonOrEmpty(bbox));
        } else if (bbox != null) {
            areaOfInterest = new DefaultExtent(null, bbox, null, null);
        }
        return areaOfInterest;
    }

    /**
     * Returns the desired accuracy in metres.
     * A value of 0 means to search for the most accurate operation.
     *
     * <p>When searching for the most accurate operation, SIS considers only the operations specified by the authority.
     * For example, the <cite>Molodensky</cite> method is a better datum shift approximation than <cite>Abridged Molodensky</cite>.
     * But if all coordinate operations defined by the authority use the Abridged Molodensky method, then SIS will ignore
     * the Molodensky one.</p>
     *
     * @return the desired accuracy in metres.
     */
    public double getDesiredAccuracy() {
        return desiredAccuracy;
    }

    /**
     * Sets the desired accuracy in metres.
     * A value of 0 means to search for the most accurate operation.
     * See {@link #getDesiredAccuracy()} for more details about what we mean by <q>most accurate</q>.
     *
     * @param  accuracy  the desired accuracy in metres.
     */
    public void setDesiredAccuracy(final double accuracy) {
        ArgumentChecks.ensurePositive("accuracy", accuracy);
        desiredAccuracy = accuracy;
    }

    /**
     * Returns a filter that can be used for applying additional restrictions on the coordinate operation.
     *
     * @todo Not yet implemented. This is currently only a hook for a possible future feature.
     */
    final Predicate<CoordinateOperation> getOperationFilter() {
        return null;
    }

    /**
     * Returns coordinate values that may be considered as constant, or {@code null} if none.
     * This method is invoked when at least one coordinate in the target <abbr>CRS</abbr>
     * cannot be computed from the coordinates in the source <abbr>CRS</abbr>.
     * For example, if the source <abbr>CRS</abbr> has (<var>x</var>, <var>y</var>) axes
     * and the target <abbr>CRS</abbr> has (<var>x</var>, <var>y</var>, <var>t</var>) axes,
     * then this method is invoked for determining which value to assign to the <var>t</var> coordinate.
     * All other coordinates will be ignored and can be NaN.
     *
     * <h4>Coordinate reference system</h4>
     * If {@link DirectPosition#getCoordinateReferenceSystem()} returns null, the default coordinate reference system
     * is the target <abbr>CRS</abbr> of the requested coordinate operation. Otherwise, the <abbr>CRS</abbr> of the
     * returned position should be one of the components of the target <abbr>CRS</abbr>. If the <abbr>CRS</abbr> is
     * not recognized, the returned position may be ignored.
     *
     * @return coordinates to take as constants, or {@code null} if none.
     * @since 1.6
     */
    public DirectPosition getConstantCoordinates() {
        return constantCoordinates;
    }

    /**
     * Sets coordinate values that may be considered as constant.
     * This position will be used if at least one coordinate in the target <abbr>CRS</abbr>
     * cannot be computed from the coordinates in the source <abbr>CRS</abbr>.
     *
     * <h4>Allowed coordinate reference systems</h4>
     * The Coordinate Reference System (<abbr>CRS</abbr>) of the given position is optional.
     * If absent, it is assumed to be the target <abbr>CRS</abbr> of the coordinate operation.
     * If present, it should be either the target <abbr>CRS</abbr> of the requested operation,
     * or one of the components of the target <abbr>CRS</abbr> (if the latter is compound).
     * If this condition does not hold, the position may be ignored (it will not be transformed).
     *
     * @param coordinates  coordinate values that may be considered as constant, or {@code null} if none.
     * @since 1.6
     */
    public void setConstantCoordinates(final DirectPosition coordinates) {
        constantCoordinates = coordinates;
    }

    /**
     * Returns whether it is safe to use cached operation.
     * This is {@code false} if the operation result may depend on external configuration.
     */
    final boolean canReadFromCache() {
        return getAreaOfInterest() == null && getDesiredAccuracy() == 0 && getConstantCoordinates() == null;
    }

    /**
     * Returns the locale for error messages.
     * By default, this is the <abbr>JVM</abbr> default locale.
     * Note that this locale has no incidence on the selection of coordinate operations.
     * In particular, this locale does not determine a geographic area.
     *
     * @return the locale for error messages.
     * @since 1.6
     */
    @Override
    public Locale getLocale() {
        return (locale != null) ? locale : Locale.getDefault();
    }

    /**
     * Sets the locale for error messages.
     * Locale specified by this method are used on a best-effort basis only.
     * Note that this locale has no incidence on the selection of coordinate operations.
     * In particular, this locale does not determine a geographic area.
     *
     * @param  locale  the locale for error messages, or {@code null} for the <abbr>JVM</abbr> default locale.
     * @since 1.6
     */
    public void setLocale(final Locale locale) {
        this.locale = locale;
    }

    /**
     * Returns a filter which will receive warnings, or {@code null} if none.
     * If that filter returns {@code false} for a given log record, then that
     * record will not be logged by {@link CoordinateOperationFinder}.
     *
     * @return a filter which will receive warnings, or {@code null} if none.
     * @since 1.6
     */
    public Filter getLogFilter() {
        return logFilter;
    }

    /**
     * Specifies a filter which will receive warnings, or {@code null} if none.
     * If that filter returns {@code false} for a given log record, then that
     * record will not be logged by {@link CoordinateOperationFinder}.
     *
     * <h4>Usage example</h4>
     * This filter can be used as a way to redirect the logs to something else,
     * such as {@linkplain org.apache.sis.storage.event.StoreListener data store listeners}.
     * In such case, the return value of the filter controls whether the record should be also
     * logged with the usual loggers.
     *
     * @param logFilter  a filter which will receive warnings, or {@code null} if none.
     * @since 1.6
     */
    public void setLogFilter(final Filter logFilter) {
        this.logFilter = logFilter;
    }

    /**
     * Logs a record about an exception that we can ignore.
     *
     * @param method  the method to declare as the source.
     * @param e       the exception to report.
     */
    private void recoverableException(final String method, final Exception e) {
        final var record = new LogRecord(Level.FINE, e.getLocalizedMessage());
        record.setThrown(e);
        record.setSourceMethodName(method);
        log(this, CoordinateOperationContext.class, AbstractCoordinateOperation.LOGGER, record);
    }

    /**
     * Logs the given record. The logger name of the record is unconditionally set to the name of the given logger.
     * Then, if the context is non-null and has a non-null log filter, the record is submitted to the filter.
     * If the filter didn't returned {@code false}, the record is logged.
     *
     * @param context  the context, or {@code null} if none.
     * @param caller   the class to report as the source of the log.
     * @param logger   the logger where to log the record.
     * @param record   the record to log.
     */
    static void log(CoordinateOperationContext context, Class<?> caller, Logger logger, LogRecord record) {
        record.setSourceClassName(caller.getCanonicalName());
        record.setLoggerName(logger.getName());
        if (context != null) {
            final Filter logFilter = context.getLogFilter();
            if (logFilter != null && !logFilter.isLoggable(record)) {
                return;
            }
        }
        logger.log(record);
    }
}
