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
package org.apache.sis.referencing.dggs;

import java.util.stream.Stream;
import com.google.common.geometry.S2Polygon;
import javax.measure.Quantity;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.rs.internal.shared.s2.S2;


/**
 * Set of zones at the same refinement level, that uniquely and completely cover a globe.
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#term-dgg
 */
public interface DiscreteGlobalGrid {

    /**
     * Numerical order of a discrete global grid in the tessellation sequence
     * <p>
     * The discrete global grid with the least number of zones has a refinement level of 0.
     *
     * @return refinement level
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#term-refinement-level
     */
    int getRefinementLevel();

    /**
     * @return approximative zone precision
     */
    Quantity<?> getPrecision();

    /**
     * Create a math transform from long type identifiers to CRS.
     *
     * The returned MathTransform expect long identifiers as double values.
     * Use Double.doubleToRawLongBits(double) to read an identifier
     * and Double.longBitsToDouble(long) to write an identifier.
     *
     * @return MathTransform, never null
     * @throws UnsupportedOperationException if long type identifiers are not supported
     */
    MathTransform createTransformToCrs() throws UnsupportedOperationException;

    /**
     * Create a math transform from CRS to long type identifiers.
     *
     * The returned MathTransform encode long identifiers as double values.
     * Use Double.doubleToRawLongBits(double) to read an identifier
     * and Double.longBitsToDouble(long) to write an identifier.
     *
     * @return MathTransform, never null
     * @throws UnsupportedOperationException if long type identifiers are not supported
     */
    MathTransform createTransformToIdentifiers() throws UnsupportedOperationException;

    /**
     * Get the number of zones at the hierarchy level.
     * @return number of zones
     */
    long getZoneCount();

    /**
     * Get all zones in at this grid level.
     *
     * @return stream of matching zones
     */
    default Stream<Zone> getZones() throws TransformException {
        return getZones((Envelope) null);
    }

    /**
     * Get zone at given location
     *
     * @return matching zone
     */
    Zone getZone(DirectPosition position) throws TransformException;

    /**
     * Get all zones which intersect the given envelope.
     *
     * @param env searched zone, can be null
     * @return stream of matching zones
     */
    default Stream<Zone> getZones(Envelope env) throws TransformException {
        if (env == null) return getZones((GeographicExtent) null);
        env = Envelopes.transform(env, CommonCRS.WGS84.normalizedGeographic());

        if (env.getSpan(0) == 0 && env.getSpan(1) == 0) {
            //envelope degenerates to a point
            final DirectPosition dp = new DirectPosition2D(env.getCoordinateReferenceSystem(), env.getMinimum(0), env.getMinimum(1));
            return Stream.of(getZone(dp));
        }

        S2Polygon polygon = S2.toS2Polygon(env);
        return getZones(S2.toGeographicExtent(polygon));
    }

    /**
     * Get all zones which intersect the given extent.
     *
     * @param parent searched parent
     * @return stream of matching zones
     */
    default Stream<Zone> getZones(Zone parent) throws TransformException {
        return getZones(parent.getGeographicExtent());
    }

    /**
     * Get all zones which intersect the given extent.
     *
     * @param extent searched extent, can be null
     * @return stream of matching zones
     */
    Stream<Zone> getZones(GeographicExtent extent) throws TransformException;

}
