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
import java.util.Locale;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.function.Function;
import java.util.function.BiConsumer;
import static java.lang.Math.*;
import java.time.ZoneId;
import java.time.Instant;
import java.time.DateTimeException;
import java.time.temporal.Temporal;
import javax.measure.Unit;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Geometry;
import org.opengis.temporal.TemporalPrimitive;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.extent.BoundingPolygon;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicDescription;
import org.opengis.metadata.identification.Identification;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.apache.sis.metadata.InvalidMetadataException;
import org.apache.sis.metadata.privy.ReferencingServices;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.Range;
import org.apache.sis.pending.jdk.JDK23;
import org.apache.sis.util.OptionalCandidate;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Emptiable;
import org.apache.sis.util.Static;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.temporal.TemporalDate;
import static org.apache.sis.util.privy.CollectionsExt.nonNull;
import static org.apache.sis.util.collection.Containers.isNullOrEmpty;
import static org.apache.sis.metadata.privy.ReferencingServices.AUTHALIC_RADIUS;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.RealizationMethod;
import org.opengis.coordinate.MismatchedCoordinateMetadataException;


/**
 * Convenience static methods for extracting information from {@link Extent} or {@link Metadata} objects.
 * This class provides methods for:
 *
 * <ul>
 *   <li>{@linkplain #getGeographicBoundingBox Fetching geographic},
 *       {@linkplain #getVerticalRange vertical} or
 *       {@linkplain #getTimeRange temporal} ranges in a convenient form.</li>
 *   <li>Computing {@linkplain #intersection intersection} of bounding boxes.</li>
 *   <li>Computing {@linkplain #area area} estimations.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.geometry.Envelopes
 *
 * @since 0.3
 */
public final class Extents extends Static {
    /**
     * The bounding box computed by this class.
     */
    private GeographicBoundingBox bounds;

    /**
     * If more than one {@link GeographicBoundingBox} is found,
     * a new instance used for computing the union of all bounding boxes.
     */
    private DefaultGeographicBoundingBox modifiable;

    /**
     * Do no allow instantiation of this class, except for internal purposes.
     */
    private Extents() {
    }

    /**
     * A geographic extent ranging from 180°W to 180°E and 90°S to 90°N.
     * This extent has no vertical and no temporal components.
     *
     * @see #isWorld(Extent)
     */
    public static final Extent WORLD;
    static {
        final var box = new DefaultGeographicBoundingBox(
                Longitude.MIN_VALUE, Longitude.MAX_VALUE,
                 Latitude.MIN_VALUE,  Latitude.MAX_VALUE);
        box.transitionTo(DefaultGeographicBoundingBox.State.FINAL);
        var world = new DefaultExtent(Vocabulary.formatInternational(Vocabulary.Keys.World), box, null, null);
        world.transitionTo(DefaultExtent.State.FINAL);
        WORLD = world;
    }

    /**
     * Returns the extents found in all {@code Identification} elements of the given metadata.
     * If there is only one {@link Identification} element (which is the usual case), then its
     * collection of extents is returned <em>as-is</em>; the collection is not copied.
     *
     * <p>In the less usual case where there is many {@link Identification} elements providing
     * non-empty collection of extents, then this method returns the union of all collections
     * without duplicated elements (duplication determined by {@link Object#equals(Object)}).
     * In the special case where the first non-empty collection of extents contains all other
     * collections, then that collection is returned <em>as-is</em>.</p>
     *
     * <div class="note"><b>Rational:</b>
     * above policy makes a best effort for avoiding to create new collections.
     * The reason is that collection implementations may perform lazy calculations of {@link Extent} elements.
     * This method tries to preserve the lazy behavior (if any).</div>
     *
     * @param  metadata  the metadata, or {@code null} if none.
     * @return extents found in all {@link Identification} elements, or an empty collection if none.
     *
     * @since 1.3
     */
    public static Collection<? extends Extent> fromIdentificationInfo(final Metadata metadata) {
        Collection<? extends Extent> result = null;
        if (metadata != null) {
            Set<Extent> union = null;
            for (final Identification id : nonNull(metadata.getIdentificationInfo())) {
                if (id != null) {       // Should not be allowed, but we are paranoiac.
                    final Collection<? extends Extent> extents = id.getExtents();
                    if (extents != result && !isNullOrEmpty(extents)) {
                        if (result == null) {
                            result = extents;
                        } else {
                            if (union == null) {
                                union = new LinkedHashSet<>(result);
                            }
                            if (union.addAll(extents)) {
                                result = union;
                            }
                        }
                    }
                }
            }
        }
        return (result != null) ? result : Collections.emptyList();
    }

    /**
     * Returns a single geographic bounding box from the specified metadata. If the given metadata
     * contains many {@link Identification} or many {@link Extent} instances, then this method returns
     * the {@linkplain DefaultGeographicBoundingBox#add(GeographicBoundingBox) union} of all of them.
     *
     * <h4>Use case</h4>
     * This convenience method is useful when the metadata is expected to contain only one bounding box,
     * typically because the metadata were obtained from a {@linkplain org.apache.sis.storage.Resource
     * resource} which is known to support only singletons (one raster or one set of features).
     * For more general cases, it is often more appropriate to handle each bounding box separately
     * using {@link #getGeographicBoundingBox(Extent)}.
     *
     * @param  metadata  the metadata from which to get a global bounding box, or {@code null} if none.
     * @return a global bounding box for all extents found in the given metadata, or {@code null} if none.
     *
     * @since 1.1
     */
    @OptionalCandidate
    public static GeographicBoundingBox getGeographicBoundingBox(final Metadata metadata) {
        if (metadata == null) {
            return null;
        }
        final Extents m = new Extents();
        try {
            for (final Identification id : nonNull(metadata.getIdentificationInfo())) {
                if (id != null) for (final Extent extent : nonNull(id.getExtents())) {
                    if (extent != null) {
                        m.addHorizontal(extent);
                    }
                }
            }
        } catch (TransformException e) {
            throw new InvalidMetadataException(Errors.format(Errors.Keys.CanNotTransformEnvelope), e);
        }
        return m.bounds;
    }

    /**
     * Returns a single geographic bounding box for the given extents.
     * For each extent, the bounding box is fetched with {@link #getGeographicBoundingBox(Extent)}.
     * If more than one geographic bound is found, this method computes their union.
     *
     * <p>This is a convenience method for fetching the domain of validity of
     * {@link org.opengis.referencing.datum.Datum},
     * {@link CoordinateReferenceSystem} or
     * {@link CoordinateOperation} objects.</p>
     *
     * @param  extents  the extents for which to get a single geographic bounding box.
     * @return the union of all geographic bounding boxes found in all extents.
     * @throws InvalidMetadataException if an envelope cannot be transformed to a geographic bounding box.
     *
     * @see CoordinateReferenceSystem#getDomains()
     * @see org.apache.sis.referencing.CRS#getDomainOfValidity(CoordinateReferenceSystem)
     *
     * @since 1.4
     */
    public static Optional<GeographicBoundingBox> getGeographicBoundingBox(final Stream<? extends Extent> extents) {
        final Extents m = new Extents();
        extents.forEach((extent) -> {
            if (extent != null) try {
                m.addHorizontal(extent);
            } catch (TransformException e) {
                throw new InvalidMetadataException(Errors.format(Errors.Keys.CanNotTransformEnvelope), e);
            }
        });
        return Optional.ofNullable(m.bounds);
    }

    /**
     * Returns a single geographic bounding box from the specified extent.
     * This method tries to find the bounding box in the cheapest way
     * before to fallback on more expensive computations:
     *
     * <ol>
     *   <li>First, this method searches geographic elements that are instance of {@link GeographicBoundingBox}.<ul>
     *     <li>If exactly one such instance is found, then this method returns that instance directly (no copy).</li>
     *     <li>If more than one instance is found, then this method computes and returns the
     *         {@linkplain DefaultGeographicBoundingBox#add union} of all bounding boxes.</li>
     *   </ul></li>
     *   <li>If above step found no {@code GeographicBoundingBox}, then this method inspects geographic elements
     *       that are instance of {@link BoundingPolygon}, taking in account only the envelopes associated to a
     *       coordinate reference system of kind {@link GeographicCRS}. If such envelopes are found, then this
     *       method computes and returns their union.</li>
     *   <li>If above step found no polygon's envelope associated to a geographic CRS, then in last resort this
     *       method uses all polygon's envelopes regardless their coordinate reference system (provided that the
     *       CRS is not null), applying coordinate transformations if needed.</li>
     *   <li>If above step found no polygon's envelope, then this method returns {@code null}.</li>
     * </ol>
     *
     * @param  extent  the extent to convert to a geographic bounding box, or {@code null}.
     * @return a geographic bounding box extracted from the given extent, or {@code null} if none.
     * @throws InvalidMetadataException if an envelope cannot be transformed to a geographic bounding box.
     *
     * @see org.apache.sis.referencing.CRS#getDomainOfValidity(CoordinateReferenceSystem)
     */
    @OptionalCandidate
    public static GeographicBoundingBox getGeographicBoundingBox(final Extent extent) {
        if (extent == null) {
            return null;
        }
        final var m = new Extents();
        try {
            m.addHorizontal(extent);
        } catch (TransformException e) {
            throw new InvalidMetadataException(Errors.format(Errors.Keys.CanNotTransformEnvelope), e);
        }
        return m.bounds;
    }

    /**
     * Implementation of {@link #getGeographicBoundingBox(Extent)}.
     * Defined in as a class member for allowing accumulation of many extents.
     *
     * @param  extent  the extent to add. Must be non-null.
     * @throws TransformException if an envelope cannot be transformed to a geographic bounding box.
     */
    private void addHorizontal(final Extent extent) throws TransformException {
        boolean useOnlyGeographicEnvelopes = false;
        final var fallbacks = new ArrayList<Envelope>();
        for (final GeographicExtent element : nonNull(extent.getGeographicElements())) {
            /*
             * If a geographic bounding box can be obtained, add it to the previous boxes (if any).
             * All exclusion boxes before the first inclusion box are ignored.
             */
            if (element instanceof GeographicBoundingBox) {
                final var item = (GeographicBoundingBox) element;
                if (bounds == null) {
                    /*
                     * We use DefaultGeographicBoundingBox.getInclusion(Boolean) below because
                     * add(…) method that we use cares about the case where inclusion is false.
                     */
                    if (DefaultGeographicBoundingBox.getInclusion(item.getInclusion())) {
                        bounds = item;
                    }
                } else if (!bounds.equals(item)) {
                    if (modifiable == null) {
                        bounds = modifiable = new DefaultGeographicBoundingBox(bounds);
                    }
                    modifiable.add(item);
                }
            } else if (bounds == null && element instanceof BoundingPolygon) {
                /*
                 * If no GeographicBoundingBox has been found so far but we found a BoundingPolygon, remember
                 * its Envelope but do not transform it yet. We will transform envelopes later only if needed.
                 *
                 * No need for DefaultGeographicBoundingBox.getInclusion(Boolean) below because we do not perform
                 * any processing (apart just ignoring the element) for cases where the inclusion value is false.
                 */
                if (!Boolean.FALSE.equals(element.getInclusion())) {
                    for (final Geometry geometry : nonNull(((BoundingPolygon) extent).getPolygons())) {
                        final Envelope envelope = geometry.getEnvelope();
                        if (envelope != null) {
                            final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
                            if (crs != null) {
                                if (crs instanceof GeographicCRS) {
                                    if (!useOnlyGeographicEnvelopes) {
                                        useOnlyGeographicEnvelopes = true;
                                        fallbacks.clear();
                                    }
                                } else if (useOnlyGeographicEnvelopes) {
                                    continue;
                                }
                                fallbacks.add(envelope);
                            }
                        }
                    }
                }
            }
        }
        /*
         * If we found not explicit GeographicBoundingBox element, use the information that we
         * collected in BoundingPolygon elements. This may involve coordinate transformations.
         */
        if (bounds == null) {
            for (final Envelope envelope : fallbacks) {
                final var item = new DefaultGeographicBoundingBox();
                item.setBounds(envelope);
                if (bounds == null) {
                    bounds = modifiable = item;
                } else {
                    modifiable.add(item);
                }
            }
        }
    }

    /**
     * Returns the union of a subset of vertical ranges found in the given extent, or {@code null} if none.
     * If the given {@code Extent} object contains more than one vertical extent, then this method
     * performs a choice based on the vertical datum and the unit of measurement:
     *
     * <ul class="verbose">
     *   <li><p><b>Choice based on realization method</b><br>
     *   Only the extents associated (indirectly, through their CRS) to the same non-null {@link RealizationMethod}
     *   will be taken in account. If all realization methods are absent, then this method conservatively uses only
     *   the first vertical extent. Otherwise the realization method used for filtering the vertical extents is:</p>
     *
     *   <ul>
     *     <li>{@link RealizationMethod#GEOID} if at least one extent uses this realization method.</li>
     *     <li>Otherwise, {@link RealizationMethod#TIDAL} if at least one extent uses this realization method.</li>
     *     <li>Otherwise, the first non-null realization type found in iteration order.</li>
     *   </ul>
     *
     *   <div class="note"><b>Rational:</b> like {@linkplain #getGeographicBoundingBox(Extent) geographic bounding box},
     *   the vertical range is an approximated information; the range returned by this method does not carry any
     *   information about the vertical CRS and this method does not attempt to perform coordinate transformation.
     *   But this method is more useful if the returned ranges are close to a frequently used surface, like the geoid.
     *   The same simplification is applied in the {@code VerticalExtent} element of Well Known Text (WKT) format,
     *   which specifies that <q>Vertical extent is an approximate description of location;
     *   heights are relative to an unspecified mean sea level.</q></div></li>
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
     * @param  extent  the extent to convert to a vertical measurement range, or {@code null}.
     * @return a vertical measurement range created from the given extent, or {@code null} if none.
     * @throws InvalidMetadataException if a vertical range contains a {@link Double#NaN} value.
     *
     * @since 0.4
     */
    @OptionalCandidate
    public static MeasurementRange<Double> getVerticalRange(final Extent extent) {
        MeasurementRange<Double> range = null;
        RealizationMethod selectedMethod = null;
        if (extent != null) try {
            for (final VerticalExtent element : nonNull(extent.getVerticalElements())) {
                Double min = element.getMinimumValue();
                Double max = element.getMaximumValue();
                final VerticalCRS crs = element.getVerticalCRS();
                RealizationMethod method = null;
                Unit<?> unit = null;
                if (crs != null) {
                    final VerticalDatum datum = crs.getDatum();
                    if (datum != null) {
                        method = datum.getRealizationMethod().orElse(method);
                    }
                    final CoordinateSystemAxis axis = crs.getCoordinateSystem().getAxis(0);
                    unit = axis.getUnit();
                    if (axis.getDirection() == AxisDirection.DOWN) {
                        final Double tmp = min;
                        min = -max;
                        max = -tmp;
                    }
                }
                if (min == null) min = Double.NEGATIVE_INFINITY;
                if (max == null) max = Double.POSITIVE_INFINITY;
                if (range != null) {
                    /*
                     * If the new range does not specify any realization method or unit, we do not know how
                     * to convert the values before to perform the union operation. Conservatively do nothing.
                     */
                    if (method == null || unit == null) {
                        continue;
                    }
                    /*
                     * If the new range is not measured relative to the same kind of surface than the previous range,
                     * then we do not know how to combine those ranges. Do nothing, unless the new range is a geoidal
                     * height in which case we forget all previous ranges and use the new one instead.
                     */
                    if (method != selectedMethod) {
                        if (selectedMethod == RealizationMethod.GEOID ||
                                   (method != RealizationMethod.GEOID &&
                                    method != RealizationMethod.TIDAL))
                        {
                            continue;
                        }
                    } else if (selectedMethod != null) {
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
                selectedMethod = method;
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidMetadataException(e.toString(), e);
        }
        return range;
    }

    /**
     * Returns the union of all time ranges found in the given extent, or {@code null} if none.
     *
     * @param  extent  the extent to convert to a time range, or {@code null}.
     * @return a time range created from the given extent, or {@code null} if none.
     *
     * @since 0.4
     */
    @OptionalCandidate
    public static Range<Date> getTimeRange(final Extent extent) {
        final Temporal[] period = getPeriod(extent);
        final Date min = TemporalDate.toDate(period[0]);
        final Date max = TemporalDate.toDate(period[1]);
        if (min == null && max == null) {
            return null;
        }
        return new Range<>(Date.class, min, true, max, true);
    }

    /*
     * Returns the union of all time ranges found in the given extent.
     *
     * @param  extent  the extent to convert to a time range, or {@code null}.
     * @param  zone    the timezone to use if a time is local, or {@code null} if none.
     * @return a time range created from the given extent.
     *
     * @todo Not yet provided because the return type should be `Temporal`, but that type
     * does not extend `Comparable`. An alterlative would be to return `Period` instead.
     *
    public static Optional<Range<Instant>> getTimeRange(final Extent extent, final ZoneId zone) {
    }
     */

    /**
     * Returns a date in the {@linkplain Extent#getTemporalElements() temporal elements} of the given extent.
     *
     * @param  extent    the extent from which to get an instant, or {@code null}.
     * @param  location  0 for the start time, 1 for the end time, 0.5 for the average time, or the
     *                   coefficient (usually in the [0 … 1] range) for interpolating an instant.
     * @return an instant interpolated at the given location, or {@code null} if none.
     *
     * @since 0.4
     *
     * @deprecated Replaced by {@link #getInstant(Extent, ZoneId, double)} in order to transition to {@code java.time} API.
     */
    @Deprecated(since="1.5", forRemoval=true)
    public static Date getDate(final Extent extent, final double location) {
        return TemporalDate.toDate(getInstant(extent, null, location).orElse(null));
    }

    /**
     * Returns an instant in the {@linkplain Extent#getTemporalElements() temporal elements} of the given extent,
     * or {@code null} if none. First, this method computes the union of all temporal elements. Then this method
     * computes the linear interpolation between the start and end time as in the following pseudo-code:
     *
     * {@snippet lang="java" :
     *     return new Date(startTime + (endTime - startTime) * location);
     *     }
     *
     * Special cases:
     * <ul>
     *   <li>If {@code location} is 0, then this method returns the {@linkplain DefaultTemporalExtent#getBeginning() beginning}.</li>
     *   <li>If {@code location} is 1, then this method returns the {@linkplain DefaultTemporalExtent#getEnding() ending}.</li>
     *   <li>If {@code location} is 0.5, then this method returns the average of start time and end time.</li>
     *   <li>If {@code location} is outside the [0 … 1] range, then the result will be outside the temporal extent.</li>
     * </ul>
     *
     * @param  extent    the extent from which to get an instant, or {@code null}.
     * @param  zone      the timezone to use if a time is local, or {@code null} if none.
     * @param  location  0 for the beginning, 1 for the ending, 0.5 for the average time, or the
     *                   coefficient (usually in the [0 … 1] range) for interpolating an instant.
     * @return an instant interpolated at the given location.
     * @throws DateTimeException if {@code zone} is null and a temporal value
     *         cannot be converted to an instant without that information.
     *
     * @since 1.5
     */
    public static Optional<Instant> getInstant(final Extent extent, final ZoneId zone, final double location) {
        ArgumentChecks.ensureFinite("location", location);
        final Temporal[] period = getPeriod(extent);
        Instant min = TemporalDate.toInstant(period[0], zone);
        Instant max = TemporalDate.toInstant(period[1], zone);
        if (min == null || location == 1) {
            return Optional.ofNullable(max);
        }
        if (max != null && location != 0) {
            min = min.plusMillis(Math.round(location * JDK23.until(min, max).toMillis()));
        }
        return Optional.of(min);
    }

    /**
     * Returns the minimum and maximum temporal values in an array of length 2.
     *
     * @param  extent  the extent on which to apply a function, or {@code null}.
     * @return the minimum and maximum values. Never null, but may contain null elements.
     * @throws DateTimeException if there is more than one temporal extent, and some temporal values are not comparable.
     */
    private static Temporal[] getPeriod(final Extent extent) {
        Temporal min = null;
        Temporal max = null;
        if (extent != null) {
            for (final TemporalExtent t : nonNull(extent.getTemporalElements())) {
                final Temporal startTime, endTime;
                if (t instanceof DefaultTemporalExtent) {
                    final var dt = (DefaultTemporalExtent) t;
                    // Maybe user has overridden those methods.
                    startTime = dt.getBeginning().orElse(null);
                    endTime   = dt.getEnding()   .orElse(null);
                } else {
                    final TemporalPrimitive p = t.getExtent();
                    startTime = DefaultTemporalExtent.getBound(p, true);
                    endTime   = DefaultTemporalExtent.getBound(p, false);
                }
                if (startTime != null && (min == null || TemporalDate.compare(startTime, min) < 0)) min = startTime;
                if (  endTime != null && (max == null || TemporalDate.compare(  endTime, max) > 0)) max = endTime;
            }
        }
        return new Temporal[] {min, max};
    }

    /**
     * Returns the description of the given extent, or {@code null} if none.
     *
     * @param  extent  the extent from which to get a description, or {@code null}.
     * @param  locale  desired locale, or {@code null} for default.
     * @return description of the given extent, or {@code null} if none.
     *
     * @since 1.1
     */
    @OptionalCandidate
    public static String getDescription(final Extent extent, final Locale locale) {
        return (extent != null) ? Types.toString(extent.getDescription(), locale) : null;
    }

    /**
     * Returns the position at the median longitude and latitude values of the given bounding box.
     * This method does not check the {@linkplain DefaultGeographicBoundingBox#getInclusion() inclusion} status.
     * This method takes in account bounding boxes that cross the anti-meridian.
     *
     * @param  bbox  the bounding box for which to get the median longitude and latitude values, or {@code null}.
     * @return a median position of the given bounding box, or {@code null} if none.
     */
    public static DirectPosition centroid(final GeographicBoundingBox bbox) {
        if (bbox != null) {
            double y    = (bbox.getNorthBoundLatitude() + bbox.getSouthBoundLatitude()) / 2;
            double x    =  bbox.getWestBoundLongitude();
            double xmax =  bbox.getEastBoundLongitude();
            if (xmax < x) {
                xmax += (Longitude.MAX_VALUE - Longitude.MIN_VALUE);
            }
            x = Longitude.normalize((x + xmax) / 2);
            if (Double.isFinite(x) || Double.isFinite(y)) {
                return ReferencingServices.getInstance().geographic(x, y);
            }
        }
        return null;
    }

    /**
     * Returns an <em>estimation</em> of the area (in square metres) of the given bounding box.
     * Since {@code GeographicBoundingBox} provides only approximated information (for example
     * it does not specify the datum), the value returned by this method is also approximated.
     *
     * <p>The current implementation performs its computation on the
     * {@linkplain org.apache.sis.referencing.CommonCRS#SPHERE GRS 1980 Authalic Sphere}.
     * However, this may change in any future SIS version.</p>
     *
     * @param  box  the geographic bounding box for which to compute the area, or {@code null}.
     * @return an estimation of the area in the given bounding box (m²),
     *         or {@linkplain Double#NaN NaN} if the given box was null.
     *
     * @since 0.4
     */
    public static double area(final GeographicBoundingBox box) {
        if (box == null) {
            return Double.NaN;
        }
        double Δλ = box.getEastBoundLongitude() - box.getWestBoundLongitude();
        final double span = Longitude.MAX_VALUE - Longitude.MIN_VALUE;
        if (Δλ > span) {
            Δλ = span;
        } else if (Δλ < 0) {
            if (Δλ < -span) {
                Δλ = -span;
            } else {
                Δλ += span;
            }
        }
        return (AUTHALIC_RADIUS * AUTHALIC_RADIUS) * toRadians(Δλ) *
               max(0, sin(toRadians(box.getNorthBoundLatitude())) -
                      sin(toRadians(box.getSouthBoundLatitude())));
    }

    /**
     * Returns the union of the given geographic bounding boxes.
     * If any of the arguments is {@code null}, then this method returns the other argument (which may be null).
     * Otherwise, this method returns a box which is the union of the two given boxes.
     *
     * <p>This method never modify the given boxes, but may return directly one of the given arguments
     * if it already represents the union result.</p>
     *
     * @param  b1  the first bounding box, or {@code null}.
     * @param  b2  the second bounding box, or {@code null}.
     * @return the union (may be any of the {@code b1} or {@code b2} argument if unchanged),
     *         or {@code null} if the two given boxes are null.
     *
     * @see DefaultGeographicBoundingBox#add(GeographicBoundingBox)
     *
     * @since 1.2
     */
    public static GeographicBoundingBox union(final GeographicBoundingBox b1, final GeographicBoundingBox b2) {
        return apply(b1, b2, DefaultGeographicBoundingBox::new, DefaultGeographicBoundingBox::add);
    }

    /**
     * Returns the intersection of the given geographic bounding boxes. If any of the arguments is {@code null},
     * then this method returns the other argument (which may be null). Otherwise this method returns a box which
     * is the intersection of the two given boxes. If there is no intersection, the returned bounding box contains
     * {@link Double#NaN} bounds.
     *
     * <p>This method never modify the given boxes, but may return directly one of the given arguments if it
     * already represents the intersection result.</p>
     *
     * @param  b1  the first bounding box, or {@code null}.
     * @param  b2  the second bounding box, or {@code null}.
     * @return the intersection (may be any of the {@code b1} or {@code b2} argument if unchanged),
     *         or {@code null} if the two given boxes are null. May contain {@link Double#NaN} bounds.
     * @throws IllegalArgumentException if the {@linkplain DefaultGeographicBoundingBox#getInclusion() inclusion status}
     *         is not the same for both boxes.
     *
     * @see DefaultGeographicBoundingBox#intersect(GeographicBoundingBox)
     *
     * @since 0.4
     */
    public static GeographicBoundingBox intersection(final GeographicBoundingBox b1, final GeographicBoundingBox b2) {
        return apply(b1, b2, DefaultGeographicBoundingBox::new, DefaultGeographicBoundingBox::intersect);
    }

    /**
     * May compute an intersection between the given geographic extents.
     * Current implementation supports only {@link GeographicBoundingBox};
     * all other kinds are handled as if they were {@code null}.
     *
     * <p>We may improve this method in future Apache SIS version, but it is not yet clear how.
     * For example, how to handle {@link GeographicDescription} or {@link BoundingPolygon}?
     * This method should not be public before we find a better contract.</p>
     */
    static GeographicExtent intersection(final GeographicExtent e1, final GeographicExtent e2) {
        return intersection(e1 instanceof GeographicBoundingBox ? (GeographicBoundingBox) e1 : null,
                            e2 instanceof GeographicBoundingBox ? (GeographicBoundingBox) e2 : null);
    }

    /**
     * Returns the intersection of the given vertical extents. If any of the arguments is {@code null},
     * then this method returns the other argument (which may be null). Otherwise this method returns a
     * vertical extent which is the intersection of the two given extents.
     *
     * <p>This method never modify the given extents, but may return directly one of the given arguments
     * if it already represents the intersection result.</p>
     *
     * <h4>Advantage and inconvenient of this method</h4>
     * This method cannot intersect extents defined with different datums because height transformations
     * generally require the geodetic positions (latitudes and longitudes) of the heights to transform.
     * For more general transformations, it is better to convert all extent components into a single envelope,
     * then {@linkplain org.apache.sis.geometry.Envelopes#transform(CoordinateOperation, Envelope) transform
     * the envelope at once}. On the other hand, this {@code intersect(…)} method preserves better
     * the {@link org.apache.sis.xml.NilReason} (if any).
     *
     * @param  e1  the first extent, or {@code null}.
     * @param  e2  the second extent, or {@code null}.
     * @return the intersection (may be any of the {@code e1} or {@code e2} argument if unchanged),
     *         or {@code null} if the two given extents are null.
     * @throws MismatchedCoordinateMetadataException if the two extents do not use the same datum, ignoring metadata.
     *
     * @see DefaultVerticalExtent#intersect(VerticalExtent)
     *
     * @since 0.8
     */
    public static VerticalExtent intersection(final VerticalExtent e1, final VerticalExtent e2) {
        return apply(e1, e2, DefaultVerticalExtent::new, DefaultVerticalExtent::intersect);
    }

    /**
     * Returns the intersection of the given temporal extents. If any of the arguments is {@code null},
     * then this method returns the other argument (which may be null). Otherwise this method returns a
     * temporal extent which is the intersection of the two given extents.
     *
     * <p>This method never modify the given extents, but may return directly one of the given arguments
     * if it already represents the intersection result.</p>
     *
     * @param  e1  the first extent, or {@code null}.
     * @param  e2  the second extent, or {@code null}.
     * @return the intersection (may be any of the {@code e1} or {@code e2} argument if unchanged),
     *         or {@code null} if the two given extents are null.
     *
     * @see DefaultTemporalExtent#intersect(TemporalExtent)
     *
     * @since 0.8
     */
    public static TemporalExtent intersection(final TemporalExtent e1, final TemporalExtent e2) {
        return apply(e1, e2, DefaultTemporalExtent::new, DefaultTemporalExtent::intersect);
    }

    /**
     * Returns the intersection of the given extents. If any of the arguments is {@code null},
     * then this method returns the other argument (which may be null). Otherwise this method
     * returns an extent which is the intersection of all geographic, vertical and temporal
     * elements in the two given extents.
     *
     * <p>This method never modify the given extents, but may return directly one of the given
     * arguments if it already represents the intersection result.</p>
     *
     * <p>If there is no intersection, then the returned object implements {@link Emptiable}
     * and the {@link Emptiable#isEmpty()} method returns {@code true}.</p>
     *
     * @param  e1  the first extent, or {@code null}.
     * @param  e2  the second extent, or {@code null}.
     * @return the intersection (may be any of the {@code e1} or {@code e2} argument if unchanged),
     *         or {@code null} if the two given extents are null.
     * @throws IllegalArgumentException if two elements to intersect are not compatible (e.g. mismatched
     *         {@linkplain DefaultGeographicBoundingBox#getInclusion() bounding box inclusion status} or
     *         mismatched {@linkplain DefaultVerticalExtent#getVerticalCRS() vertical datum}).
     *
     * @see DefaultExtent#intersect(Extent)
     *
     * @since 0.8
     */
    public static Extent intersection(final Extent e1, final Extent e2) {
        return apply(e1, e2, DefaultExtent::new, DefaultExtent::intersect);
    }

    /**
     * Implementation of {@code intersection(…)} and {@code union(…)} methods.
     *
     * <h4>API note</h4>
     * The <var>C</var> parameter type should be {@code <C extends ISOMetadata & I>}.
     * But this is not allowed by current Java compiler, because of complexity. See
     * <a href="https://bugs.openjdk.java.net/browse/JDK-4899305">JDK-4899305</a>.
     *
     * @param  <I>          the metadata interface.
     * @param  <C>          the metadata implementation class. Shall implement {@code <I>}.
     * @param  e1           the first extent, or {@code null}.
     * @param  e2           the second extent, or {@code null}.
     * @param  constructor  copy constructor of metadata implementation class.
     * @param  operator     the union or intersection operator to apply.
     * @return the intersection or union of the given metadata objects.
     */
    @SuppressWarnings("unchecked")      // Workaround for Java above-cited compiler restriction.
    private static <I, C extends ISOMetadata> I apply(final I e1, final I e2,
            final Function<I,C> constructor, final BiConsumer<C,I> operator)
    {
        if (e1 == null) return e2;
        if (e2 == null || e2 == e1) return e1;
        final C result = constructor.apply(e1);
        operator.accept(result, e2);
        if (result.equals(e1, ComparisonMode.BY_CONTRACT)) return e1;
        if (result.equals(e2, ComparisonMode.BY_CONTRACT)) return e2;
        return (I) result;
    }

    /**
     * Returns {@code true} if the given extent covers the world.
     * The current implementation checks if at least one geographic bounding box has
     * a latitude range of {@value Latitude#MIN_VALUE} to {@value Latitude#MAX_VALUE} and
     * a longitude range of {@value Longitude#MIN_VALUE} to {@value Longitude#MAX_VALUE}.
     *
     * @param  extent  the extent to check, or {@code null} if none.
     * @return whether the given extent covers the world.
     *
     * @see #WORLD
     * @since 1.5
     */
    public static boolean isWorld(final Extent extent) {
        if (extent != null) {
            for (final GeographicExtent element : nonNull(extent.getGeographicElements())) {
                if (element instanceof GeographicBoundingBox) {
                    final var item = (GeographicBoundingBox) element;
                    if (item.getWestBoundLongitude() <= Longitude.MIN_VALUE &&
                        item.getEastBoundLongitude() >= Longitude.MAX_VALUE &&
                        item.getSouthBoundLatitude() <=  Latitude.MIN_VALUE &&
                        item.getNorthBoundLatitude() >=  Latitude.MAX_VALUE)
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
