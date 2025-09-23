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
package org.apache.sis.storage.csv;

import java.util.Date;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.function.Consumer;
import java.lang.reflect.Array;
import java.time.Instant;
import org.apache.sis.math.Vector;
import org.apache.sis.geometry.wrapper.Dimensions;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.feature.internal.shared.MovingFeatures;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;

// Specific to the main branch:
import org.apache.sis.feature.AbstractAttribute;
import org.apache.sis.feature.AbstractFeature;


/**
 * Builder of feature where the geometry is a trajectory and some property values may change with time.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class MovingFeatureBuilder extends MovingFeatures {
    /**
     * The properties having values that may change in time.
     * May contain the values of arbitrary properties (e.g. as {@link String} instances),
     * or may contain the coordinates of part of a trajectory as array of primitive type like {@code float[]}.
     * Trajectories may be specified in many parts if for example, the different parts are given on different
     * lines of a CSV file.
     */
    private final Period[] properties;

    /**
     * Number of {@code Property} instances added for each index of the {@link #properties} table.
     */
    private final int[] count;

    /**
     * A dynamic property value together with the period of time in which this property is valid.
     */
    private static final class Period {
        /**
         * Beginning in milliseconds since Java epoch of the period when the property value is valid.
         */
        final long startTime;

        /**
         * End in milliseconds since Java epoch of the period when the property value is valid.
         * This end time will be adjusted if the next added period can be merged with this period.
         */
        long endTime;

        /**
         * The property value.
         */
        final Object value;

        /**
         * Previous property in a chained list of properties.
         */
        final Period previous;

        /**
         * Creates a new property value valid on the given period of time.
         */
        Period(final Period previous, final long startTime, final long endTime, final Object value) {
            this.previous  = previous;
            this.startTime = startTime;
            this.endTime   = endTime;
            this.value     = value;
        }
    }

    /**
     * Overall start and end time over all properties.
     */
    private long tmin = Long.MAX_VALUE,
                 tmax = Long.MIN_VALUE;

    /**
     * Creates a new moving feature.
     *
     * @param share          other builder that may share time vectors, or {@code null} if none.
     * @param numProperties  maximal number of dynamic properties.
     */
    public MovingFeatureBuilder(final MovingFeatureBuilder share, final int numProperties) {
        super(share);
        properties = new Period[numProperties];
        count      = new int   [numProperties];
    }

    /**
     * Adds a time range.
     * The minimal and maximal values will be used by {@link #storeTimeRange(String, String, AbstractFeature)}.
     *
     * @param  startTime  beginning in milliseconds since Java epoch of the period when the property value is valid.
     * @param  endTime    end in milliseconds since Java epoch of the period when the property value is valid.
     */
    public final void addTimeRange(final long startTime, final long endTime) {
        if (startTime < tmin) tmin = startTime;
        if (  endTime > tmax) tmax =   endTime;
    }

    /**
     * Adds a dynamic property value. This method shall be invoked with time periods in chronological order.
     *
     * @param  index      the property index.
     * @param  startTime  beginning in milliseconds since Java epoch of the period when the property value is valid.
     * @param  endTime    end in milliseconds since Java epoch of the period when the property value is valid.
     * @param  value      the property value which is valid during the given period.
     */
    public final void addValue(final int index, final long startTime, final long endTime, final Object value) {
        final Period p = properties[index];
        if (p != null && p.endTime == startTime && Objects.equals(p.value, value)) {
            p.endTime = endTime;
        } else {
            properties[index] = new Period(p, startTime, endTime, value);
            count[index]++;
        }
    }

    /**
     * Stores the start time and end time in the given feature.
     *
     * @param  startTime  name of the property where to store the start time.
     * @param  endTime    name of the property where to store the end time.
     * @param  dest       feature where to store the start time and end time.
     */
    public final void storeTimeRange(final String startTime, final String endTime, final AbstractFeature dest) {
        if (tmin < tmax) {
            final Instant t = Instant.ofEpochMilli(tmin);
            dest.setPropertyValue(startTime, t);
            dest.setPropertyValue(endTime, (tmin == tmax) ? t : Instant.ofEpochMilli(tmax));
        }
    }

    /**
     * Sets the values of the given attribute to the values collected by this {@code MovingFeatures}.
     * This method sets also the {@code "datetimes"} characteristic.
     *
     * @param  <V>    the type of values in the given attribute.
     * @param  index  index of the property for which values are desired.
     * @param  dest   attribute where to store the value.
     */
    @SuppressWarnings("unchecked")
    public final <V> void storeAttribute(final int index, final AbstractAttribute<V> dest) {
        int n = count[index];
        final long[] times  = new long[n];
        final V[]    values = (V[]) Array.newInstance(dest.getType().getValueClass(), n);
        for (Period p = properties[index]; p != null; p = p.previous) {
            times [--n] = p.startTime;
            values[  n] = (V) p.value;
        }
        if (n != 0) {
            // Should never happen unless this object has been modified concurrently in another thread.
            throw new CorruptedObjectException();
        }
        dest.setValues(UnmodifiableArrayList.wrap(values));
        setInstants(dest, times);
    }

    /**
     * Sets the geometry of the given attribute to the values collected by this {@code MovingFeatures}.
     * This method sets also the {@code "datetimes"} characteristic.
     *
     * @param  <G>              the type of the geometry value.
     * @param  featureName      the name of the feature containing the attribute to update, for logging purpose.
     * @param  index            index of the property for which geometry value is desired.
     * @param  dimension        number of dimensions for all coordinates.
     * @param  factory          the factory to use for creating the geometry object.
     * @param  dest             attribute where to store the geometry value.
     * @param  warningListener  where to report warnings. Implementation should set the source class name,
     *                          source method name and logger name, then forward to a {@code WarningListener}.
     */
    public final <G> void storeGeometry(final String featureName, final int index, final int dimension,
            final Geometries<G> factory, final AbstractAttribute<G> dest, final Consumer<LogRecord> warningListener)
    {
        int n = count[index];
        final var vectors = new Vector[n];
        for (Period p = properties[index]; p != null; p = p.previous) {
            vectors[--n] = Vector.create(p.value, false);
        }
        if (n != 0) {
            // Should never happen unless this object has been modified concurrently in another thread.
            throw new CorruptedObjectException();
        }
        int    warnings = 10;                   // Maximal number of warnings, for avoiding to flood the logger.
        int    numPts   = 0;                    // Total number of points in all vectors, ignoring null vectors.
        Vector previous = null;                 // If non-null, shall be non-empty.
        for (int i=0; i<vectors.length; i++) {
            Vector v = vectors[i];
            int length;
            if (v == null || (length = v.size()) == 0) {
                continue;
            }
            if ((length % dimension) != 0) {
                if (--warnings >= 0) {
                    Period p = properties[index];
                    for (int j=i; --j >= 0;) {          // This is inefficient but used only in case of warnings.
                        p = p.previous;
                    }
                    warningListener.accept(Resources.forLocale(null).createLogRecord(
                            Level.WARNING,
                            Resources.Keys.UnexpectedNumberOfCoordinates_4,
                            featureName,
                            new Date(p.startTime),
                            dimension,
                            length));
                }
                continue;
            }
            /*
             * At this point we have a non-empty valid sequence of coordinate values. If the first point of current
             * vector is equal to the last point of previous vector, assume that they form a continuous polyline.
             */
            if (previous != null) {
                if (equals(previous, v, dimension)) {
                    v = v.subList(dimension, length);   // Skip the first coordinate.
                    length -= dimension;
                    if (length == 0) {
                        vectors[i] = null;
                        continue;
                    }
                    vectors[i] = v;
                }
            }
            numPts += length;
            previous = v;
        }
        /*
         * At this point we got the list of all coordinates to join together in a polyline.
         * We will create the geometry at the end of this method. Before that, interpolate
         * the dates and times.
         */
        int i = vectors.length;
        numPts /= dimension;
        final long[] times = new long[numPts];
        for (Period p = properties[index]; p != null; p = p.previous) {
            final Vector v = vectors[--i];
            if (v != null) {
                int c = v.size() / dimension;
                if (c == 1) {
                    times[--numPts] = p.endTime;
                } else {
                    final long startTime = p.startTime;
                    final double scale = (p.endTime - startTime) / (double) (c-1);
                    while (--c >= 0) {
                        times[--numPts] = startTime + Math.round(scale * c);
                    }
                }
            }
        }
        if (numPts != 0) {
            // Should never happen unless this object has been modified concurrently in another thread.
            throw new CorruptedObjectException();
        }
        /*
         * Store the geometry and characteristics in the attribute.
         */
        dest.setValue(factory.createPolyline(false, Dimensions.forCount(dimension, false), vectors));
        setInstants(dest, times);
    }

    /**
     * Returns {@code true} if the last coordinate of the {@code previous} vector is equal to the first
     * coordinate of the {@code next} vector.
     *
     * @param previous   the previous vector.
     * @param next       the next vector.
     * @param dimension  number of dimension in each coordinate.
     */
    private static boolean equals(final Vector previous, final Vector next, int dimension) {
        int p = previous.size();
        while (--dimension >= 0) {
            if (next.doubleValue(dimension) != previous.doubleValue(--p)) {
                return false;
            }
        }
        return true;
    }
}
