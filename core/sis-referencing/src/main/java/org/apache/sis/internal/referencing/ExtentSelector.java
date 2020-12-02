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
package org.apache.sis.internal.referencing;

import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.Extents;

import static java.lang.Double.isNaN;


/**
 * Selects an object in a sequence of objects using their extent as a criterion.
 * The selection is based on the geographic area using the following rules:
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
 * <div class="note"><b>Rational:</b>
 * the "minimize area outside" criterion (rule 2) is before "best centered" criterion (rule 3)
 * for consistency with criteria applied on the temporal axis. If "geographic area" is replaced
 * by "time range", we could have the following scenario: a user specified a "time of interest"
 * (TOI) of 1 day. By coincidence a raster containing monthly averages has a median time closer
 * to TOI center than raster containing daily averages. If rules 2 and 3 were interchanged, the
 * monthly averages would be selected. By checking time outside TOI first, the daily data is
 * returned instead.</div>
 *
 * Usage:
 *
 * {@preformat java
 *     ExtentSelector<Foo> selector = new ExtentSelector<>(areaOfInterest);
 *     for (Foo candidate : candidates) {
 *         selector.evaluate(candidate.extent, candidate),
 *     }
 *     Foo best = selector.best();
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <T>  the type of object to be selected.
 *
 * @since 0.4
 * @module
 */
public final class ExtentSelector<T> {
    /**
     * The area of interest, or {@code null} if none.
     * This is specified at construction time, but can be modified later.
     *
     * @see #setAreaOfInterest(GeographicBoundingBox, Extent)
     */
    private GeographicBoundingBox areaOfInterest;

    /**
     * The best object found so far.
     */
    private T best;

    /**
     * The area covered by the {@linkplain #best} object (m²). The initial value is zero,
     * which imply that only intersection areas greater than zero will be accepted.
     * This is the desired behavior in order to filter out empty intersections.
     *
     * <p>This is the first criterion cited in class javadoc.</p>
     */
    private double largestArea;

    /**
     * Area of {@linkplain #best} object which is outside the area of interest.
     * This is used as a discriminatory criterion only when {@link #largestArea}
     * has the same value for two or more objects.
     *
     * <p>This is the second criterion cited in class javadoc.</p>
     */
    private double outsideArea;

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
     * Creates a selector for the given area of interest.
     *
     * @param areaOfInterest  the area of interest, or {@code null} if none.
     */
    public ExtentSelector(final Extent areaOfInterest) {
        this.areaOfInterest = Extents.getGeographicBoundingBox(areaOfInterest);
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
     * Sets the area of interest to the intersection of the two given arguments.
     * This method should be invoked only if {@link #best()} returned {@code null}.
     * It allows to make a second search with a different AOI when the search using
     * previous AOI gave no result.
     *
     * @param  a1  first area of interest as a bounding box, or {@code null}.
     * @param  a2  second area of interest as an extent, or {@code null}.
     */
    public final void setAreaOfInterest(final GeographicBoundingBox a1, final Extent a2) {
        areaOfInterest = Extents.intersection(a1, Extents.getGeographicBoundingBox(a2));
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
        final double dφ = (area.getNorthBoundLatitude() + area.getSouthBoundLatitude()) - cφ;
        final double dλ = (area.getEastBoundLongitude() - areaOfInterest.getEastBoundLongitude())
                        + (area.getWestBoundLongitude() - areaOfInterest.getWestBoundLongitude())
                        * Math.cos(cφ * (Math.PI/180 / 2));
        return dφ*dφ + dλ*dλ;
    }

    /**
     * Evaluates the given extent against the criteria represented by this {@code ExtentSelector}.
     * If the intersection between the given extent and the area of interest is greater than any
     * previous intersection, then the given object is remembered as the best match found so far.
     * Otherwise other criteria documented in class javadoc are applied.
     *
     * @param  extent  the extent to evaluate, or {@code null} if none.
     * @param  object  an user object associated to the given extent.
     */
    public void evaluate(final Extent extent, final T object) {
        final GeographicBoundingBox bbox = Extents.getGeographicBoundingBox(extent);
        final double area = Extents.area(Extents.intersection(bbox, areaOfInterest));
        /*
         * Accept the given object if it is the first one (`best = null`), or if it covers a larger area than
         * previous object, or if the previous object had no extent information at all (`largestArea` is NaN)
         * while the new object has a valid extent.
         */
        // Use `!(…)` form for accepting NaN in `area > largestArea`.
        if (!(best == null || area > largestArea || (isNaN(largestArea) && !isNaN(area)))) {
            if (notEquals(area, largestArea)) {
                return;
            }
            /*
             * If the two extents have the same area, second criterion is to select the object having
             * smallest amount of area outside the AOI, with same NaN handling than for intersection.
             * If still equal, third and last criterion is to select the object closest to center
             * (determined in an approximated way).
             */
            final double out = Extents.area(bbox) - area;
            if (!(out < outsideArea || (isNaN(outsideArea) && !isNaN(out)))) {
                if (notEquals(out, outsideArea)) {
                    return;
                }
                final double pd = pseudoDistance(bbox);
                if (!(pd < pseudoDistance)) {
                    return;
                }
                pseudoDistance = pd;
            } else {
                pseudoDistance = pseudoDistance(bbox);
            }
            outsideArea = out;
        } else {
            pseudoDistance = pseudoDistance(bbox);
            outsideArea    = Extents.area(bbox) - area;
        }
        largestArea = area;
        best = object;
    }

    /**
     * Returns {@code true} if the given values are not equals.
     * {@link Double#NaN} values are compared equal to other NaN values.
     * Sign of positive/negative zero is ignored.
     */
    private static boolean notEquals(final double a, final double b) {
        return (a != b) && !(isNaN(a) && isNaN(b));
    }

    /**
     * Returns the object associated to the largest area found so far.
     *
     * @return the object associated to the largest area found so far, or {@code null}.
     */
    public T best() {
        return best;
    }

    /**
     * Returns {@code true} if an intersection has been found.
     *
     * @return whether an intersection has been found.
     */
    public boolean hasIntersection() {
        return largestArea > 0;
    }
}
