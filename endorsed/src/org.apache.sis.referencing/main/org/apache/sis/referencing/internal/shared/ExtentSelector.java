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
package org.apache.sis.referencing.internal.shared;

import java.util.Date;
import java.time.Duration;
import java.time.DateTimeException;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoField;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.Range;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.temporal.TemporalDate;


/**
 * Selects an object in a sequence of objects using their extent as a criterion.
 * The selection is based on the temporal extent and geographic area using the following rules:
 *
 * <ol>
 *   <li>Object having largest intersection with the time of interest (TOI) is selected.</li>
 *   <li>If two or more candidates have the same intersection with TOI,
 *       then the one with less "overtime" (time outside TOI) is selected.</li>
 *   <li>If two or more candidates are considered equal after above criteria,
 *       then the one best centered on the TOI is selected.</li>
 * </ol>
 *
 * <div class="note"><b>Rational:</b>
 * the "smallest time outside" criterion (rule 2) is before "best centered" criterion (rule 3)
 * because of the following scenario: if a user specifies a "time of interest" (TOI) of 1 day
 * and if the candidates are a raster of monthly averages and a raster of daily data, we want
 * the daily data to be selected even if by coincidence the monthly averages is more centered.</div>
 *
 * If there is no time of interest, or the candidate objects do not declare time range,
 * or some objects are still at equality after application of above criteria,
 * then the selection continues on the basis of geographic criteria:
 *
 * <ol>
 *   <li>Largest intersection with the {@linkplain #areaOfInterest area of interest} (AOI) is selected.</li>
 *   <li>If two or more candidates have the same intersection area with AOI, then the one with the less
 *       "irrelevant" material is selected. "Irrelevant" material are area outside the AOI.</li>
 *   <li>If two or more candidates are considered equal after above criteria,
 *       the one best centered on the AOI is selected.</li>
 *   <li>If two or more candidates are considered equal after above criteria,
 *       then the first of those candidates is selected.</li>
 * </ol>
 *
 * <h2>Change of rule order</h2>
 * The following configuration flags change the order in which above rules are applied.
 *
 * <ul>
 *   <li>{@link #alternateOrdering} — whether the time center criterion is tested last.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * Example:
 *
 * {@snippet lang="java" :
 *     ExtentSelector<Foo> selector = new ExtentSelector<>(areaOfInterest);
 *     for (Foo candidate : candidates) {
 *         selector.evaluate(candidate.extent, candidate),
 *     }
 *     Foo best = selector.best();
 *     }
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <T>  the type of object to be selected.
 */
public final class ExtentSelector<T> {
    /**
     * The area of interest (AOI), or {@code null} if unbounded.
     * This is initialized at construction time, but can be modified later.
     *
     * @see #setExtentOfInterest(Extent, GeographicBoundingBox, Temporal[])
     */
    private GeographicBoundingBox areaOfInterest;

    /**
     * Start/end of the time of interest (TOI), or {@code null} if unbounded.
     * This is initialized at construction time, but can be modified later.
     *
     * @see #setExtentOfInterest(Extent, GeographicBoundingBox, Temporal[])
     */
    private Temporal minTOI, maxTOI;

    /**
     * Granularity of the time of interest in seconds, or 0 if none.
     * If non-zero, this this is always positive.
     *
     * @see #setTimeGranularity(Duration)
     * @see #round(Duration)
     */
    private long granularity;

    /**
     * Whether to use an alternate conditions order where the time center criterion is tested last.
     * This flag can be set to {@code true} if the Time Of Interest (TOI) is expected to be larger
     * than the temporal extent of candidate objects, in which case many objects may fit in the TOI.
     * Those candidates may be considered practically equally good regarding temporal aspect,
     * in which case the caller may want to give precedence to geographic area.
     *
     * <p>This flag is often used together with {@link #setTimeGranularity(Duration)} method
     * for reducing the preponderance of temporal criteria.</p>
     */
    public boolean alternateOrdering;

    /**
     * The best object found so far.
     */
    private T best;

    /**
     * The area covered by the {@linkplain #best} object (m²).
     * This is the first criterion cited in class javadoc.
     */
    private double largestArea;

    /**
     * Duration of the {@linkplain #best} object, or {@code null} if none.
     * This is equivalent to {@link #largestArea} in the temporal domain.
     * Value is rounded by {@link #round(Duration)}.
     */
    private Duration longestTime;

    /**
     * Area of {@linkplain #best} object which is outside the area of interest.
     * This is used as a discriminatory criterion only when {@link #largestArea}
     * has the same value for two or more objects.
     * This is the second criterion cited in class javadoc.
     */
    private double outsideArea;

    /**
     * Duration of {@linkplain #best} object which is outside the time of interest.
     * This is used as a discriminatory criterion only when {@link #longestTime}
     * has the same value for two or more objects.
     * This is equivalent to {@link #outsideArea} in the temporal domain.
     */
    private Duration overtime;

    /**
     * A pseudo-distance from {@linkplain #best} object center to {@link #areaOfInterest} center.
     * This is <strong>not</strong> a real distance, neither great circle distance or rhumb line.
     * The only requirements are: a value equals to zero when the two centers are coincident and
     * increasing when the centers are mowing away.
     *
     * <p>This value is used as a discriminatory criterion only when {@link #largestArea}
     * and {@link #outsideArea} have the same values for two or more objects.
     * This is the third criterion cited in class javadoc.</p>
     */
    private double pseudoDistance;

    /**
     * Time between {@linkplain #best} entry center and TOI center.
     * This value is used as a discriminatory criterion only when {@link #longestTime}
     * and {@link #overtime} have the same values for two or more objects.
     * This is equivalent to {@link #pseudoDistance} in the temporal domain.
     */
    private double temporalDistance;

    /**
     * Creates a selector for the given area and time of interest.
     *
     * @param  aoi  the area of interest, or {@code null} if unbounded.
     * @param  toi  the time of interest, or {@code null} or empty if unbounded.
     *              The first element is start time and the last element is end time.
     */
    public ExtentSelector(final GeographicBoundingBox aoi, final Temporal[] toi) {
        areaOfInterest = aoi;
        final int n;
        if (toi != null && (n = toi.length) != 0) {
            minTOI = toi[0];
            maxTOI = toi[n-1];
        }
    }

    /**
     * Creates a selector for the given area of interest.
     *
     * @param  domain  the area and time of interest, or {@code null} if none.
     * @throws IllegalArgumentException if AOI or TOI has an invalid range.
     */
    public ExtentSelector(final Extent domain) {
        if (!setExtentOfInterest(domain, null, null)) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Sets the area of interest (AOI) and time of interest (TOI) to the intersection of given arguments.
     * This method should be invoked only if {@link #best()} returned {@code null}. It allows to make new
     * attempts with a different domain of interest when the search using previous AOI/TOI gave no result.
     *
     * <p>Callers should not use this {@code ExtentSelector} if this method returns {@code false},
     * except for invoking this {@code setExtentOfInterest(…)} method again with different values
     * until this method returns {@code true}.</p>
     *
     * @param  domain  the area and time of interest, or {@code null} if none.
     * @param  aoi     second area of interest as a bounding box, or {@code null} if none.
     * @param  toi     second time of interest as a an array of length 2, 1 or 0, or {@code null}.
     *                 If array length is 2, it contains start time and end time in that order.
     *                 If array length is 1, start time and end time are assumed the same.
     *                 If array length is 0 or array reference is null, there is no temporal range to intersect.
     * @return whether the intersections of {@code domain} with {@code aoi} and {@code toi} have valid ranges.
     */
    public final boolean setExtentOfInterest(final Extent domain, final GeographicBoundingBox aoi, final Temporal[] toi) {
        areaOfInterest = Extents.intersection(aoi, Extents.getGeographicBoundingBox(domain));
        minTOI = maxTOI = null;
        final Range<Date> tr = Extents.getTimeRange(domain);
        if (tr != null) {
            minTOI = TemporalDate.toTemporal(tr.getMinValue());
            maxTOI = TemporalDate.toTemporal(tr.getMaxValue());
        }
        if (toi != null && toi.length != 0) {
            Temporal t = toi[0];
            if (minTOI == null || (t != null && TemporalDate.compare(t, minTOI) > 0)) {
                minTOI = t;     // `t` is after `minTOI`: reduce the range to intersection.
            }
            if (toi.length >= 2) t = toi[1];
            if (maxTOI == null || (t != null && TemporalDate.compare(t, maxTOI) < 0)) {
                maxTOI = t;     // `t` is before `maxTOI`: reduce the range to intersection.
            }
        }
        return (minTOI == null || maxTOI == null || TemporalDate.compare(minTOI, maxTOI) <= 0) &&
                ((areaOfInterest == null) ||
                    (areaOfInterest.getNorthBoundLatitude() >= areaOfInterest.getSouthBoundLatitude()
                        && Double.isFinite(areaOfInterest.getWestBoundLongitude())
                        && Double.isFinite(areaOfInterest.getEastBoundLongitude())
                        && !Boolean.FALSE.equals(areaOfInterest.getInclusion())));
    }

    /**
     * Returns the area of interest.
     *
     * @return area of interest, or {@code null} if none.
     */
    public final GeographicBoundingBox getAreaOfInterest() {
        return areaOfInterest;
    }

    /**
     * Returns the time of interest as an array of length 2, or {@code null} if none.
     *
     * @return the start time and end time of interest, or {@code null} if none.
     */
    public final Temporal[] getTimeOfInterest() {
        return (minTOI == null && maxTOI == null) ? null : new Temporal[] {minTOI, maxTOI};
    }

    /**
     * Sets the temporal granularity of the Time of Interest (TOI). If non-null, intersections with TOI
     * will be rounded to an integer number of this granularity. This is useful if data are expected at
     * an approximately regular interval (for example one remote sensing image per day) and we want to
     * ignore slight variations in the temporal extent declared for each image.
     *
     * <p>This method is often used together with {@link #alternateOrdering} flag
     * for reducing the preponderance of temporal criteria.</p>
     *
     * @param  resolution  granularity of the time of interest, or {@code null} if none.
     * @throws IllegalArgumentException if the given resolution is zero or negative.
     */
    public final void setTimeGranularity(final Duration resolution) {
        if (resolution == null) {
            granularity = 0;
        } else if (resolution.isZero() || resolution.isNegative()) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "resolution", resolution));
        } else {
            granularity = resolution.getSeconds();
        }
    }

    /**
     * Returns the given duration rounded to the nearest integer number of temporal granularity.
     * If no granularity has been specified, then this method returns the given duration unmodified.
     */
    private Duration round(Duration duration) {
        if (duration != null && !duration.isZero() && granularity != 0) {
            long t = duration.getSeconds();
            long r = t % granularity;
            if (r != 0) {
                t -= r;
                if (t == 0 || r >= (granularity >> 1)) {
                    t += granularity;
                }
                duration = Duration.ofSeconds(t);
            }
        }
        return duration;
    }

    /**
     * Computes a pseudo-distance between the center of given area and of {@link #areaOfInterest}.
     * This is <strong>not</strong> a real distance, neither great circle distance or rhumb line.
     * May be {@link Double#NaN} if information is unknown.
     *
     * @see #pseudoDistance
     */
    private double pseudoDistance(final GeographicBoundingBox area) {
        if (areaOfInterest == null || area == null) {
            return Double.NaN;
        }
        /*
         * Following calculation omits division by 2 and square root because we do not need a real distance;
         * we only need increasing values as `area` center moves away from `areaOfInterest` center. Note that
         * even if above operations were applied, it still NOT a valid great circle or rhumb line distance.
         * A cheap calculation is sufficient here.
         */
        final double cφ = areaOfInterest.getNorthBoundLatitude()
                        + areaOfInterest.getSouthBoundLatitude();
        final double dφ =  (area.getNorthBoundLatitude() + area.getSouthBoundLatitude()) - cφ;
        final double dλ = ((area.getEastBoundLongitude() - areaOfInterest.getEastBoundLongitude())
                        +  (area.getWestBoundLongitude() - areaOfInterest.getWestBoundLongitude()))
                        * Math.cos(cφ * (Math.PI/180 / 2));
        return dφ*dφ + dλ*dλ;
    }

    /**
     * Computes a temporal distance between the center of given range and center of time of interest.
     * This is always a positive value or {@link Double#NaN}. Unit is irrelevant (as long as constant)
     * because we only compare distances with other distances.
     *
     * @see #temporalDistance
     */
    private double temporalDistance(final Temporal startTime, final Temporal endTime) {
        return Math.abs(median(startTime, endTime) - median(minTOI, maxTOI));
    }

    /**
     * Returns instant (in milliseconds) in the middle of given time range, or {@link Double#NaN} if none.
     * Used for {@link #temporalDistance(Temporal, Temporal)} implementation only.
     *
     * @throws DateTimeException if the distance cannot be computed between the given objects.
     */
    private static double median(Temporal tmin, Temporal tmax) {
        if (tmin == null) {
            if (tmax == null) {
                return Double.NaN;
            }
            tmin = tmax;
        } else if (tmax == null) {
            tmax = tmin;
        }
        final long t0 = tmin.getLong(ChronoField.INSTANT_SECONDS);
        final long n0 = tmin.getLong(ChronoField.NANO_OF_SECOND);
        if (tmax != tmin) {
            final long t1 = tmax.getLong(ChronoField.INSTANT_SECONDS);
            final long n1 = tmax.getLong(ChronoField.NANO_OF_SECOND);
            return MathFunctions.average(t0, t1) + MathFunctions.average(n0, n1) / Constants.NANOS_PER_SECOND;
        }
        return t0 + n0 / (double) Constants.NANOS_PER_SECOND;
    }

    /**
     * Computes the amount of time outside the time of interest (TOI). The returned value is always positive
     * because {@code intersection} should always be less than {@code endTime} − {@code startTime} duration.
     * Value is rounded by {@link #round(Duration)}.
     *
     * @param  startTime     start time of of the candidate object, or {@code null} if none (unbounded).
     * @param  endTime       end time of the candidate object, or {@code null} if none (unbounded).
     * @param  intersection  duration of the intersection of [{@code startTime} … {@code endTime}]
     *                       with [{@link #minTOI} … {@link #maxTOI}].
     * @throws DateTimeException if the duration cannot be computed between the given objects.
     */
    private Duration overtime(final Temporal startTime, final Temporal endTime, final Duration intersection) {
        return (startTime != null && endTime != null && intersection != null)
                ? round(Duration.between(startTime, endTime).minus(intersection)) : null;
    }

    /**
     * Evaluates the given extent against the criteria represented by this {@code ExtentSelector}.
     * See class javadoc for a list of criteria and the order in which they are applied.
     * Implementation delegates to {@link #evaluate(GeographicBoundingBox, Temporal, Temporal, Object)}.
     *
     * @param  domain  the extent to evaluate, or {@code null} if none.
     * @param  object  a user object associated to the given extent.
     * @throws DateTimeException if this method cannot perform temporal calculation between the given objects.
     */
    public void evaluate(final Extent domain, final T object) {
        final Range<Date> tr = Extents.getTimeRange(domain);
        evaluate(Extents.getGeographicBoundingBox(domain),
                 (tr != null) ? TemporalDate.toTemporal(tr.getMinValue()) : null,
                 (tr != null) ? TemporalDate.toTemporal(tr.getMaxValue()) : null,
                 object);
    }

    /**
     * Evaluates the given bounding box and time range against the criteria represented by this {@code ExtentSelector}.
     * See class javadoc for a list of criteria and the order in which they are applied.
     *
     * @param  bbox       the geographic extent of {@code object}, or {@code null} if none.
     * @param  startTime  start time of {@code object}, or {@code null} if none (unbounded).
     * @param  endTime    end time of {@code object}, or {@code null} if none (unbounded).
     * @param  object     a user object associated to the given extent.
     * @throws DateTimeException if this method cannot perform temporal calculation between the given objects.
     */
    @SuppressWarnings("fallthrough")
    public void evaluate(final GeographicBoundingBox bbox, final Temporal startTime, final Temporal endTime, final T object) {
        /*
         * Get the geographic and temporal intersections. If there is no intersection, no more analysis is done.
         * Note that the intersection is allowed to be zero (empty), which is not the same as no intersection.
         * An empty intersection may happen if the AOI is a single point or the TOI is a single instant.
         */
        Temporal tmin = startTime;
        Temporal tmax = endTime;
        if (tmin != null && minTOI != null && TemporalDate.compare(tmin, minTOI) < 0) tmin = minTOI;
        if (tmax != null && maxTOI != null && TemporalDate.compare(tmax, maxTOI) > 0) tmax = maxTOI;
        final Duration duration;
        if (tmin != null && tmax != null) {
            duration = Duration.between(tmin, tmax);
            if (duration.isNegative()) return;
        } else {
            duration = null;
        }
        final double area = Extents.area(Extents.intersection(bbox, areaOfInterest));
        if (Double.isNaN(area) && bbox != null) {
            return;
        }
        /*
         * Accept the given object if it is the first one (`best == null`) or if it meets the first
         * criterion documented in class javadoc (i.e. covers a longer time than previous object).
         * Other special cases:
         *
         *   - duration == null while old value has  a duration: reject (with comparison < 0).
         *   - duration != null while old value had no duration: accept (with comparison > 0).
         *
         * Those special cases are controlled by the +1 or -1 argument in calls to `compare(…)`.
         * The same pattern is applied for all criteria in inner conditions, using one of:
         *
         *     comparison(…, -1) <= 0
         *     comparison(…, +1) >= 0
         *
         * The criteria are always tested as below:
         *
         *     if ((comparison = comparison(…, ±1)) ⪌ 0) {
         *         if (comparison != 0) return;
         *         // Compute and test criteria.
         *     }
         */
        final Duration durationRounded = round(duration);
        int comparison, remainingFieldsToCompute = OVERTIME;
        if (best != null && (comparison = compare(durationRounded, longestTime, -1)) <= 0) {
            if (comparison != 0) return;
            /*
             * Criterion #2: select the object having smallest amount of time outside Time Of Interest (TOI).
             * See class javadoc for a rational about why this criterion is applied before `temporalDistance`.
             */
            remainingFieldsToCompute = TEMPORAL_DISTANCE;
            final Duration et = overtime(startTime, endTime, duration);
            if ((comparison = compare(et, overtime, +1)) >= 0) {
                if (comparison != 0) return;
                /*
                 * Criterion #3: select the object having median time closest to TOI median time.
                 * This condition is skipped in the "alternate condition ordering" mode.
                 */
                remainingFieldsToCompute = OUTSIDE_AREA;
                final double td = temporalDistance(startTime, endTime);
                if (alternateOrdering || (comparison = compare(td, temporalDistance, +1)) >= 0) {
                    if (comparison != 0) return;
                    /*
                     * Criterion #4: select the object covering largest geographic area.
                     */
                    if ((comparison = compare(area, largestArea, -1)) <= 0) {
                        if (comparison != 0) return;
                        /*
                         * Criterion #5: select the object having less surface outside Area Of Interest (AOI).
                         * Tested before `pseudoDistance` criterion for consistency with temporal domain.
                         */
                        remainingFieldsToCompute = PSEUDO_DISTANCE;
                        final double out = Extents.area(bbox) - area;
                        if ((comparison = compare(out, outsideArea, +1)) >= 0) {
                            if (comparison != 0) return;
                            /*
                             * Criterion #5: select the object having center closest to AOI center.
                             * Distances are computed with inexact formulas (not a real distance).
                             * TOI is also compared here in "alternate condition ordering" mode.
                             */
                            remainingFieldsToCompute = NONE;
                            final double pd = pseudoDistance(bbox);
                            if (compare(pd, pseudoDistance, +1) >= 0) {
                                if (comparison != 0 || !alternateOrdering) return;
                            }
                            if (alternateOrdering && compare(td, temporalDistance, +1) >= 0) {
                                return;
                            }
                            pseudoDistance = pd;
                        }
                        outsideArea = out;
                    }
                    // largestArea = area; assigned below because was computed early.
                }
                temporalDistance = td;
            }
            overtime = et;
        }
        longestTime = durationRounded;
        largestArea = area;
        switch (remainingFieldsToCompute) {           // Intentional fallthrough in every cases.
            case OVERTIME:          overtime          = overtime(startTime, endTime, duration);
            case TEMPORAL_DISTANCE: temporalDistance  = temporalDistance(startTime, endTime);
            case OUTSIDE_AREA:      outsideArea       = Extents.area(bbox) - area;
            case PSEUDO_DISTANCE:   pseudoDistance    = pseudoDistance(bbox);
        }
        best = object;
    }

    /**
     * Identification of which fields need to be recomputed. This is an ordered enumeration:
     * recomputing a field implies recomputing all following fields (identified by greater values).
     * For example if the {@link #temporalDistance} field needs to be recomputed,
     * then the {@link #outsideArea} and {@link #pseudoDistance} fields must be recomputed as well.
     * This is a consequence of the order in which criteria documented in class javadoc are applied.
     */
    private static final int OVERTIME = 0, TEMPORAL_DISTANCE = 1, OUTSIDE_AREA = 2, PSEUDO_DISTANCE = 3, NONE = 4;

    /**
     * Compares the given duration as documented in {@link Duration#compareTo(Duration)} with the addition
     * of supporting {@code null} values. The {@code missing} argument tells whether null values shall be
     * considered smaller (-1) or greater (+1) than all non-null values.
     */
    private static int compare(final Duration a, final Duration b, final int missing) {
        if (a != null) {
            return (b != null) ? a.compareTo(b) : -missing;
        } else {
            return (b != null) ? missing : 0;
        }
    }

    /**
     * Compares the given values as documented in {@link Double#compareTo(Double)} except in the handling
     * of zero and NaN values.
     *
     * <ul>
     *   <li>The {@code missing} argument tells whether NaN values shall be considered smaller (-1)
     *       or greater (+1) than all non-NaN values.</li>
     *   <li>Positive and negative zeros are considered equal.</li>
     * </ul>
     */
    private static int compare(final double a, final double b, int missing) {
        if (a < b) return -1;
        if (a > b) return +1;
        final boolean n = Double.isNaN(b);
        if (Double.isNaN(a) == n) return 0;
        if (n) missing = -missing;
        return missing;
    }

    /**
     * Returns the object associated to the largest area found so far.
     *
     * @return the object associated to the largest area found so far, or {@code null}.
     */
    public T best() {
        return best;
    }
}
