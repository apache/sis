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
package org.apache.sis.metadata.iso.extent;

import java.util.Date;
import javax.measure.unit.Unit;
import org.opengis.temporal.TemporalPrimitive;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.extent.BoundingPolygon;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.Range;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Static;

import static java.lang.Math.*;
import static org.apache.sis.internal.metadata.MetadataUtilities.getInclusion;
import static org.apache.sis.internal.metadata.ReferencingServices.AUTHALIC_RADIUS;


/**
 * Convenience static methods for extracting information from {@link Extent} objects.
 * This class provides methods for:
 *
 * <ul>
 *   <li>{@link #getGeographicBoundingBox(Extent)}, {@link #getVerticalRange(Extent)}
 *       and {@link #getDate(Extent, double)}
 *       for fetching geographic or temporal components in a convenient form.</li>
 *   <li>Methods for computing {@linkplain #intersection intersection} of bounding boxes
 *       and {@linkplain #area area} estimations.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.6
 * @module
 *
 * @see org.apache.sis.geometry.Envelopes
 */
public final class Extents extends Static {
    /**
     * Do no allow instantiation of this class.
     */
    private Extents() {
    }

    /**
     * A geographic extent ranging from 180°W to 180°E and 90°S to 90°N.
     * This extent has no vertical and no temporal components.
     */
    public static final Extent WORLD;
    static {
        final DefaultGeographicBoundingBox box = new DefaultGeographicBoundingBox(-180, 180, -90, 90);
        box.freeze();
        final DefaultExtent world = new DefaultExtent(
                Vocabulary.formatInternational(Vocabulary.Keys.World), box, null, null);
        world.freeze();
        WORLD = world;
    }

    /**
     * Returns a single geographic bounding box from the specified extent.
     * If no bounding box is found, then this method returns {@code null}.
     * If a single bounding box is found, then that box is returned directly.
     * If more than one box is found, then all those boxes are
     * {@linkplain DefaultGeographicBoundingBox#add added} together.
     *
     * @param  extent The extent to convert to a geographic bounding box, or {@code null}.
     * @return A geographic bounding box extracted from the given extent, or {@code null} in none.
     */
    public static GeographicBoundingBox getGeographicBoundingBox(final Extent extent) {
        GeographicBoundingBox candidate = null;
        if (extent != null) {
            DefaultGeographicBoundingBox modifiable = null;
            for (final GeographicExtent element : extent.getGeographicElements()) {
                final GeographicBoundingBox bounds;
                if (element instanceof GeographicBoundingBox) {
                    bounds = (GeographicBoundingBox) element;
                } else if (element instanceof BoundingPolygon) {
                    // TODO: iterates through all polygons and invoke Polygon.getEnvelope();
                    continue;
                } else {
                    continue;
                }
                /*
                 * A single geographic bounding box has been extracted. Now add it to previous
                 * ones (if any). All exclusion boxes before the first inclusion box are ignored.
                 */
                if (candidate == null) {
                    /*
                     * Reminder: 'inclusion' is a mandatory attribute, so it should never be
                     * null for a valid metadata object.  If the metadata object is invalid,
                     * it is better to get an exception than having a code doing silently
                     * some probably inappropriate work.
                     */
                    if (getInclusion(bounds.getInclusion())) {
                        candidate = bounds;
                    }
                } else {
                    if (modifiable == null) {
                        modifiable = new DefaultGeographicBoundingBox();
                        modifiable.setBounds(candidate);
                        candidate = modifiable;
                    }
                    modifiable.add(bounds);
                }
            }
        }
        return candidate;
    }

    /**
     * Returns the union of chosen vertical ranges found in the given extent, or {@code null} if none.
     * This method gives preference to heights above the Mean Sea Level when possible.
     * Depths have negative height values: if the
     * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getDirection() axis direction}
     * is toward down, then this method reverses the sign of minimum and maximum values.
     *
     * <div class="section">Multi-occurrences</div>
     * If the given {@code Extent} object contains more than one vertical extent, then this method
     * performs a choice based on the vertical datum and the unit of measurement:
     *
     * <ul class="verbose">
     *   <li><p><b>Choice based on vertical datum</b><br>
     *   Only the extents associated (indirectly, through their CRS) to the same non-null {@link VerticalDatumType}
     *   will be taken in account. If all datum types are null, then this method conservatively uses only the first
     *   vertical extent. Otherwise the datum type used for filtering the vertical extents is:</p>
     *
     *   <ul>
     *     <li>{@link VerticalDatumType#GEOIDAL} or {@link VerticalDatumType#DEPTH DEPTH} if at least one extent
     *         uses those datum types. For this method, {@code DEPTH} is considered as equivalent to {@code GEOIDAL}
     *         except for the axis direction.</li>
     *     <li>Otherwise, the first non-null datum type found in iteration order.</li>
     *   </ul>
     *
     *   <div class="note"><b>Rational:</b> like {@linkplain #getGeographicBoundingBox(Extent) geographic bounding box},
     *   the vertical range is an approximative information; the range returned by this method does not carry any
     *   information about the vertical CRS and this method does not attempt to perform coordinate transformation.
     *   But this method is more useful if the returned ranges are close to a frequently used surface, like the
     *   Mean Sea Level. The same simplification is applied in the
     *   <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#31">{@code VerticalExtent} element of
     *   Well Known Text (WKT) format</a>, which specifies that <cite>“Vertical extent is an approximate description
     *   of location; heights are relative to an unspecified mean sea level.”</cite></div></li>
     *
     *   <li><p><b>Choice based on units of measurement</b><br>
     *   If, after the choice based on the vertical datum described above, there is still more than one vertical
     *   extent to consider, then the next criterion checks for the units of measurement.</p>
     *   <ul>
     *     <li>If no range specify a unit of measurement, return the first range and ignore all others.</li>
     *     <li>Otherwise take the first range having a unit of measurement. Then:<ul>
     *       <li>All other ranges having an incompatible unit of measurement will be ignored.</li>
     *       <li>All other ranges having a compatible unit of measurement will be converted to
     *           the unit of the first retained range, and their union will be computed.</li>
     *     </ul></li>
     *   </ul>
     *
     *   <div class="note"><b>Example:</b>
     *   Heights or depths are often measured using some pressure units, for example hectopascals (hPa).
     *   An {@code Extent} could contain two vertical elements: one with the height measurements in hPa,
     *   and the other element with heights transformed to metres using an empirical formula.
     *   In such case this method will select the first vertical element on the assumption that it is
     *   the "main" one that the metadata producer intended to show. Next, this method will search for
     *   other vertical elements using pressure unit. In our example there is none, but if such elements
     *   were found, this method would compute their union.</div></li>
     * </ul>
     *
     * @param  extent The extent to convert to a vertical measurement range, or {@code null}.
     * @return A vertical measurement range created from the given extent, or {@code null} if none.
     *
     * @since 0.4
     */
    public static MeasurementRange<Double> getVerticalRange(final Extent extent) {
        MeasurementRange<Double> range = null;
        VerticalDatumType selectedType = null;
        if (extent != null) {
            for (final VerticalExtent element : extent.getVerticalElements()) {
                double min = element.getMinimumValue();
                double max = element.getMaximumValue();
                final VerticalCRS crs = element.getVerticalCRS();
                VerticalDatumType type = null;
                Unit<?> unit = null;
                if (crs != null) {
                    final VerticalDatum datum = crs.getDatum();
                    if (datum != null) {
                        type = datum.getVerticalDatumType();
                        if (VerticalDatumType.DEPTH.equals(type)) {
                            type = VerticalDatumType.GEOIDAL;
                        }
                    }
                    final CoordinateSystemAxis axis = crs.getCoordinateSystem().getAxis(0);
                    unit = axis.getUnit();
                    if (AxisDirection.DOWN.equals(axis.getDirection())) {
                        final double tmp = min;
                        min = -max;
                        max = -tmp;
                    }
                }
                if (range != null) {
                    /*
                     * If the new range does not specify any datum type or unit, then we do not know how to
                     * convert the values before to perform the union operation. Conservatively do nothing.
                     */
                    if (type == null || unit == null) {
                        continue;
                    }
                    /*
                     * If the new range is not measured relative to the same kind of surface than the previous range,
                     * then we do not know how to combine those ranges. Do nothing, unless the new range is a Mean Sea
                     * Level Height in which case we forget all previous ranges and use the new one instead.
                     */
                    if (!type.equals(selectedType)) {
                        if (!type.equals(VerticalDatumType.GEOIDAL)) {
                            continue;
                        }
                    } else if (selectedType != null) {
                        /*
                         * If previous range did not specify any unit, then unconditionally replace it by
                         * the new range since it provides more information. If both ranges specify units,
                         * then we will compute the union if we can, or ignore the new range otherwise.
                         */
                        final Unit<?> previous = range.unit();
                        if (previous != null) {
                            if (previous.isCompatible(unit)) {
                                range = (MeasurementRange<Double>) range.union(
                                        MeasurementRange.create(min, true, max, true, unit));
                            }
                            continue;
                        }
                    }
                }
                range = MeasurementRange.create(min, true, max, true, unit);
                selectedType = type;
            }
        }
        return range;
    }

    /**
     * Returns the union of all time ranges found in the given extent, or {@code null} if none.
     *
     * @param  extent The extent to convert to a time range, or {@code null}.
     * @return A time range created from the given extent, or {@code null} if none.
     *
     * @since 0.4
     */
    public static Range<Date> getTimeRange(final Extent extent) {
        Date min = null;
        Date max = null;
        if (extent != null) {
            for (final TemporalExtent t : extent.getTemporalElements()) {
                final Date startTime, endTime;
                if (t instanceof DefaultTemporalExtent) {
                    final DefaultTemporalExtent dt = (DefaultTemporalExtent) t;
                    startTime = dt.getStartTime(); // Maybe user has overridden those methods.
                    endTime   = dt.getEndTime();
                } else {
                    final TemporalPrimitive p = t.getExtent();
                    startTime = DefaultTemporalExtent.getTime(p, true);
                    endTime   = DefaultTemporalExtent.getTime(p, false);
                }
                if (startTime != null && (min == null || startTime.before(min))) min = startTime;
                if (  endTime != null && (max == null ||   endTime.after (max))) max =   endTime;
            }
        }
        if (min == null && max == null) {
            return null;
        }
        return new Range<Date>(Date.class, min, true, max, true);
    }

    /**
     * Returns an instant in the {@linkplain Extent#getTemporalElements() temporal elements} of the given extent,
     * or {@code null} if none. First, this method computes the union of all temporal elements. Then this method
     * computes the linear interpolation between the start and end time as in the following pseudo-code:
     *
     * {@preformat java
     *     return new Date(startTime + (endTime - startTime) * location);
     * }
     *
     * Special cases:
     * <ul>
     *   <li>If {@code location} is 0, then this method returns the {@linkplain DefaultTemporalExtent#getStartTime() start time}.</li>
     *   <li>If {@code location} is 1, then this method returns the {@linkplain DefaultTemporalExtent#getEndTime() end time}.</li>
     *   <li>If {@code location} is 0.5, then this method returns the average of start time and end time.</li>
     *   <li>If {@code location} is outside the [0 … 1] range, then the result will be outside the temporal extent.</li>
     * </ul>
     *
     * @param  extent   The extent from which to get an instant, or {@code null}.
     * @param  location 0 for the start time, 1 for the end time, 0.5 for the average time, or the
     *                  coefficient (usually in the [0 … 1] range) for interpolating an instant.
     * @return An instant interpolated at the given location, or {@code null} if none.
     *
     * @since 0.4
     */
    public static Date getDate(final Extent extent, final double location) {
        ArgumentChecks.ensureFinite("location", location);
        Date min = null;
        Date max = null;
        if (extent != null) {
            for (final TemporalExtent t : extent.getTemporalElements()) {
                Date startTime = null;
                Date   endTime = null;
                if (t instanceof DefaultTemporalExtent) {
                    final DefaultTemporalExtent dt = (DefaultTemporalExtent) t;
                    if (location != 1) startTime = dt.getStartTime(); // Maybe user has overridden those methods.
                    if (location != 0)   endTime = dt.getEndTime();
                } else {
                    final TemporalPrimitive p = t.getExtent();
                    if (location != 1) startTime = DefaultTemporalExtent.getTime(p, true);
                    if (location != 0)   endTime = DefaultTemporalExtent.getTime(p, false);
                }
                if (startTime != null && (min == null || startTime.before(min))) min = startTime;
                if (  endTime != null && (max == null ||   endTime.after (max))) max =   endTime;
            }
        }
        if (min == null) return max;
        if (max == null) return min;
        final long startTime = min.getTime();
        return new Date(startTime + Math.round((max.getTime() - startTime) * location)); // addExact on JDK8 branch.
    }

    /**
     * Returns the intersection of the given geographic bounding boxes. If any of the arguments is {@code null},
     * then this method returns the other argument (which may be null). Otherwise this method returns a box which
     * is the intersection of the two given boxes.
     *
     * <p>This method never modify the given boxes, but may return directly one of the given arguments if it
     * already represents the intersection result.</p>
     *
     * @param  b1 The first bounding box, or {@code null}.
     * @param  b2 The second bounding box, or {@code null}.
     * @return The intersection (may be any of the {@code b1} or {@code b2} argument if unchanged),
     *         or {@code null} if the two given boxes are null.
     * @throws IllegalArgumentException If the {@linkplain DefaultGeographicBoundingBox#getInclusion() inclusion status}
     *         is not the same for both boxes.
     *
     * @see DefaultGeographicBoundingBox#intersect(GeographicBoundingBox)
     *
     * @since 0.4
     */
    public static GeographicBoundingBox intersection(final GeographicBoundingBox b1, final GeographicBoundingBox b2) {
        if (b1 == null) return b2;
        if (b2 == null || b2 == b1) return b1;
        final DefaultGeographicBoundingBox box = new DefaultGeographicBoundingBox(b1);
        box.intersect(b2);
        return box;
    }

    /**
     * Returns an <em>estimation</em> of the area (in square metres) of the given bounding box.
     * Since {@code GeographicBoundingBox} provides only approximative information (for example
     * it does not specify the datum), the value returned by this method is also approximative.
     *
     * <p>The current implementation performs its computation on the
     * {@linkplain org.apache.sis.referencing.CommonCRS#SPHERE GRS 1980 Authalic Sphere}.
     * However this may change in any future SIS version.</p>
     *
     * @param  box The geographic bounding box for which to compute the area, or {@code null}.
     * @return An estimation of the area in the given bounding box (m²),
     *         or {@linkplain Double#NaN NaN} if the given box was null.
     *
     * @since 0.4
     */
    public static double area(final GeographicBoundingBox box) {
        if (box == null) {
            return Double.NaN;
        }
        double Δλ = box.getEastBoundLongitude() - box.getWestBoundLongitude(); // Negative if spanning the anti-meridian
        Δλ -= floor(Δλ / (Longitude.MAX_VALUE - Longitude.MIN_VALUE)) * (Longitude.MAX_VALUE - Longitude.MIN_VALUE);
        return (AUTHALIC_RADIUS * AUTHALIC_RADIUS) * toRadians(Δλ) *
               max(0, sin(toRadians(box.getNorthBoundLatitude())) -
                      sin(toRadians(box.getSouthBoundLatitude())));
    }
}
