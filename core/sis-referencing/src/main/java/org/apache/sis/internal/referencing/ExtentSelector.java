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


/**
 * Selects an object in a sequence of objects using their extent as a criterion.
 * Current implementation uses only the geographic extent.
 * This may be extended to other kind of extent in any future SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param <T>  the type of object to be selected.
 *
 * @since 0.4
 * @module
 */
public final class ExtentSelector<T> {
    /**
     * The area of interest, or {@code null} if none.
     * This is specified at construction time, but can be modified later.
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
     */
    private double largestArea;

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
     *
     * @param  a1  first area of interest as a bounding box, or {@code null}.
     * @param  a2  second area of interest as an extent, or {@code null}.
     */
    public final void setAreaOfInterest(final GeographicBoundingBox a1, final Extent a2) {
        areaOfInterest = Extents.intersection(a1, Extents.getGeographicBoundingBox(a2));
    }

    /**
     * Evaluates the given extent against the criteria represented by the {@code ExtentSelector}.
     * If the intersection between the given extent and the area of interest is greater than any
     * previous intersection, then the given extent and object are remembered as the best match
     * found so far.
     *
     * @param  extent  the extent to evaluate, or {@code null} if none.
     * @param  object  an optional user object associated to the given extent.
     * @return {@code true} if the given extent is a better match than any previous extents given to this method.
     */
    public boolean evaluate(final Extent extent, final T object) {
        final double area = Extents.area(Extents.intersection(Extents.getGeographicBoundingBox(extent), areaOfInterest));
        if (best != null && !(area > largestArea)) {    // Use '!' for catching NaN.
            /*
             * At this point, the given extent is not greater than the previous one.
             * However if the previous object had no extent information at all (i.e.
             * 'largestArea' is NaN) while the new object has a valid extent, then
             * the new object will have precedence.
             */
            if (Double.isNaN(area) || !Double.isNaN(largestArea)) {
                return false;
            }
        }
        largestArea = area;
        best = object;
        return true;
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
