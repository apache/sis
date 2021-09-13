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
package org.apache.sis.geometry;

import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;


/**
 * The strategy to use for representing a region crossing the anti-meridian or other wraparound limit.
 * Envelopes crossing the anti-meridian have a "lower" longitude value greater than the "upper" value
 * ("lower" and "upper" should actually be understood as "starting point" and "ending point" in those cases) —
 * an illustration is provided in the {@link GeneralEnvelope} documentation.
 * When such regions are used with libraries or algorithms that are not designed for handling wraparound axes,
 * it may be necessary to convert those regions to simpler geometric representations that algorithm can handle.
 * The {@link #CONTIGUOUS}, {@link #CONTIGUOUS_LOWER}, {@link #CONTIGUOUS_UPPER} and {@link #EXPAND} enumeration
 * values are strategies for simplifying the problem at the cost of shifting geometries partially outside the
 * coordinate system domain, or making geometry bigger than necessary.
 *
 * <h2>Alternatives</h2>
 * All methods in this enumeration change the envelope or geometry without changing the coordinate system.
 * Another approach for solving the anti-meridian problem is to change the range of longitude values, for example
 * using [0 … 360]° instead of [−180 … +180]°, and then {@linkplain GeneralEnvelope#normalize() normalize envelopes}
 * in that new range. But this approach is interesting only when all geometries are known to fit in the new range of
 * longitude values, otherwise this approach is only moving the problem to another part of the world.
 *
 * <h2>Generalization</h2>
 * The documentation in this class talks about longitude values crossing the anti-meridian because it is the most
 * common case. But Apache SIS actually handles wraparound axes in a generic way. The same discussion applies also
 * for example to the temporal axis of climatological data (January averages, February averages, …, December averages
 * then back to January).
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see GeneralEnvelope#wraparound(WraparoundMethod)
 *
 * @since 1.1
 * @module
 */
public enum WraparoundMethod {
    /**
     * No check performed for wraparound axes. If a {@linkplain GeneralEnvelope#normalize() normalized envelope}
     * crosses the anti-meridian (lower longitude value {@literal >} upper value), then a polygon created from
     * the 4 envelope corners would define a different area than the envelope:
     * the "interior" and "exterior" of the geometry would be interchanged.
     * Use this method only when knowing that the envelopes or geometries do not cross the anti-meridian.
     *
     * <p><b>Example:</b>
     * given the {@code BBOX(+170 0, -170 1)} envelope, a polygon created from the 4 corners
     * and ignoring the fact that the envelope crosses the anti-meridian may be as below:</p>
     *
     * {@preformat wkt
     *   POLYGON(+170 0, -170 0, -170 1, +170 1, +170 0)
     * }
     */
    NONE,

    /**
     * Envelope represented in a way where "lower" value may be greater than "upper" value.
     * This method can represent envelopes crossing the anti-meridian without the sacrifices imposed by
     * other methods (moving a corner outside the coordinate system domain or expanding the envelope).
     * However this method can be used only with algorithms designed for handling this representation.
     * This is the case of Apache SIS {@link GeneralEnvelope} but often not the case of geometry libraries.
     *
     * <p>This method is said "normalized" because it is the only representation in Apache SIS which is guaranteed to
     * produce consistent results when {@linkplain GeneralEnvelope#add(DirectPosition) adding points to an envelope}
     * or when computing {@linkplain GeneralEnvelope#add(Envelope) unions} and {@linkplain GeneralEnvelope#intersect(Envelope)
     * intersections} of envelopes. All other methods may produce unpredictable or sub-optimal results,
     * depending for example whether two geometries have been made contiguous on the same side of the
     * coordinate system domain ({@link #CONTIGUOUS_LOWER} versus {@link #CONTIGUOUS_UPPER}).</p>
     *
     * @see GeneralEnvelope#normalize()
     */
    NORMALIZE,

    /**
     * Make geometries contiguous by possibly shifting upper corner outside the coordinate system domain.
     * If a {@linkplain GeneralEnvelope#normalize() normalized envelope} crosses the anti-meridian
     * (upper longitude value {@literal <} lower value), then an integer amount of cycles (360°)
     * will be added to the upper longitude value until we get {@code upper > lower}.
     * This will usually result in an upper corner value outside the [−180 … +180]° longitude range.
     *
     * <p><b>Example:</b>
     * given the {@code BBOX(+170 0, -170 1)} envelope,
     * a polygon created after shifting the "upper" corner (-170 1) may be as below:</p>
     *
     * {@preformat wkt
     *   POLYGON(+170 0, +190 0, +190 1, +170 1, +170 0)
     * }
     */
    CONTIGUOUS_UPPER,

    /**
     * Make geometries contiguous by possibly shifting lower corner outside the coordinate system domain.
     * If a {@linkplain GeneralEnvelope#normalize() normalized envelope} crosses the anti-meridian
     * (lower longitude value {@literal >} upper value), then an integer amount of cycles (360°)
     * will be subtracted from the lower longitude value until we get {@code lower < upper}.
     * This will usually result in a lower corner value outside the [−180 … +180]° longitude range.
     *
     * <p><b>Example:</b>
     * given the {@code BBOX(+170 0, -170 1)} envelope,
     * a polygon created after shifting the "lower" corner (+170 0) may be as below:</p>
     *
     * {@preformat wkt
     *   POLYGON(-190 0, -170 0, -170 1, -190 1, -190 0)
     * }
     */
    CONTIGUOUS_LOWER,

    /**
     * Make geometries contiguous by possibly shifting any corner outside the coordinate system domain.
     * This method is equivalent to either {@link #CONTIGUOUS_LOWER} or {@link #CONTIGUOUS_UPPER},
     * depending which method minimizes the area outside the coordinate system domain.
     */
    CONTIGUOUS,

    /**
     * Possibly expand the envelope to include the whole area in a single envelope or geometry.
     * If a {@linkplain GeneralEnvelope#normalize() normalized envelope} crosses the anti-meridian
     * (lower corner values {@literal >} upper corner values), then the envelope is expanded to span
     * an area from axis minimum value to axis maximum value. This ensures that the geometry contains
     * the original envelope area but is wider. Compared to the {@link #CONTIGUOUS} methods,
     * this {@code EXPAND} method does not move any parts outside the coordinate system domain.
     *
     * <p><b>Example:</b>
     * given the {@code BBOX(+170 0, -170 1)} envelope,
     * a polygon created after expanding the envelope may be as below:</p>
     *
     * {@preformat wkt
     *   POLYGON(-180 0, +180 0, +180 1, -180 1, -180 0)
     * }
     *
     * @see GeneralEnvelope#simplify()
     */
    EXPAND,

    /**
     * Possibly separate the envelope in as many simple envelopes or geometries as needed.
     * If a {@linkplain GeneralEnvelope#normalize() normalized envelope} crosses the anti-meridian
     * (lower corner values {@literal >} upper corner values), then a geometry created from this envelope
     * will be composed of 2 or more polygons on each side of the coordinate system.
     * This ensures that the geometries represent exactly the envelope interior but with more complexity.
     *
     * <p><b>Example:</b>
     * given the {@code BBOX(+170 0, -170 1)} envelope,
     * polygons created after splitting the envelope may be as below:</p>
     *
     * {@preformat wkt
     *   MULTI-POLYGON(
     *       (-180 0,  -170 0,  -170 1,  -180 1,  -180 0)
     *       (+170 0,  +180 0,  +180 1,  +170 1,  +170 0))
     * }
     *
     * @see GeneralEnvelope#toSimpleEnvelopes()
     */
    SPLIT
}
