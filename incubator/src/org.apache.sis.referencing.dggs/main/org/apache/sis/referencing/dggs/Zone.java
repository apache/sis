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

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import javax.measure.Quantity;
import javax.measure.quantity.Area;
import javax.measure.quantity.Volume;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.gazetteer.Location;
import org.opengis.util.InternationalString;
import com.google.common.geometry.S2Polygon;
import org.apache.sis.geometry.wrapper.s2.S2;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.dggs.DiscreteGlobalGridSystems;
import org.apache.sis.util.SimpleInternationalString;


/**
 * Particular region of space-time.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Zone extends Location, Comparable<Zone> {

    /**
     * Spatiotemporal reference that uniquely identifies a zone.
     */
    Object getIdentifier();

    /**
     * Spatiotemporal reference in the form of a string that uniquely identifies a zone.
     */
    CharSequence getTextIdentifier();

    /**
     * Spatiotemporal reference in the form of a compact 64bit integer that uniquely identifies a zone.
     */
    long getLongIdentifier();

    /**
     * Get a more accurate geometry of the zone by subdividing the cell edges.
     *
     * @param subdivision subdivide cell edges, must be 1 or more
     * @return
     */
    default GeographicExtent getGeographicExtent(int subdivision) {
        return getGeographicExtent();
    }

    /**
     * Spatiotemporal reference in the form of a label or code that uniquely identifies a zone.
     */
    @Override
    default InternationalString getGeographicIdentifier() {
        return new SimpleInternationalString(getTextIdentifier().toString());
    }

    /**
     * Returns the CRS:84 envelope of the zone geographic extent.
     * The envelope includes the edges bulging between vertices and the poles inside the zone.
     * The lower longitude is greater than the upper one if the zone crosses the anti-meridian.
     *
     * @return envelope in CRS:84, the whole world if the zone has no geographic extent
     */
    @Override
    default Envelope getEnvelope() {
        final S2Polygon polygon = DiscreteGlobalGridSystems.toS2Polygon(getGeographicExtent());
        if (polygon == null) {
            return CRS.getDomainOfValidity(CommonCRS.WGS84.normalizedGeographic());
        }
        return S2.toEnvelope(polygon);
    }

    /**
     * @return geometry type name of the zone
     */
    String getShapeType();

    /**
     * @return area of the zone, or empty if the zone is not two-dimensional.
     */
    Optional<Quantity<Area>> getArea();

    /**
     * @return volume of the zone, or empty if the zone is not three-dimensional.
     */
    Optional<Quantity<Volume>> getVolume();

    /**
     * @return duration of the zone, or empty if the zone has no temporal dimension.
     */
    Optional<Duration> getDuration();

    /**
     * @return refinement level
     */
    @Override
    RefinementLevel getLocationType();

    /**
     * Get maint parent zone of given zone.
     * Most DGGRS zones have a single parent, but DGGRS which have children who do not exactly overlap the parent
     * zone may then have several parents, int this case the first parent should be the one which has the most overlapping.
     *
     * @return main parent or null.
     */
    Zone getFirstParent();

    /**
     * @return the parent which most covers this zone at given level.
     */
    Zone getFirstParent(int refinementLevel);

    /**
     * Get parent zones of given zone.
     * Most DGGRS zones have a single parent, but DGGRS which have children who do not exactly overlap the parent
     * zone may then have several parents.
     *
     * @return list of parents
     */
    @Override
    Collection<? extends Zone> getParents();

    /**
     * Get zone children zones.
     * The children are expecte to fully cover the parent zone.
     *
     * @return list of childrens
     */
    @Override
    Collection<? extends Zone> getChildren();

    /**
     * Get neighbor zones.
     *
     * @return list of neighbors
     */
    Collection<? extends Zone> getNeighbors();

    /**
     * Test if given zone is a neighbor.
     * They must be on the same level and share an edge.
     *
     * @param zone not null
     * @return true if zones are neighbors
     */
    boolean isNeighbor(Object zone);

    /**
     * Test if given zone is a sibling.
     * They must be on the same level and share a parent.
     *
     * @param zone not null
     * @return true if zones are siblings
     */
    boolean isSibling(Object zone);

    /**
     * Test if this zone is a ancestor of given zone.
     *
     * @param zone not null
     * @param maxRelativeDepth maximum relative depth to search
     * @return true if this zone is an ancestor of given zone
     */
    boolean isAncestorOf(Object zone, int maxRelativeDepth);

    /**
     * Test if this zone is a descendant of given zone.
     *
     * @param zone not null
     * @param maxRelativeDepth maximum relative depth to search
     * @return true if this zone is an descendant of given zone
     */
    boolean isDescendantOf(Object zone, int maxRelativeDepth);

    /**
     * Test if given zone overlaps.
     * They must be of different level and one must be a child of the other.
     *
     * @param zone not null
     * @return true if one zone is an ancestor of the other
     */
    boolean overlaps(Object zone);

    /**
     * @param depth children zones to count.
     * @return children zones count
     */
    default long countChildrenAtRelativeDepth(int depth) {
        return getChildrenAtRelativeDepth(depth).count();
    }

    /**
     * @param depth children zones to return, a depth of 0 returns this zone.
     * @return children zones
     */
    default Stream<Zone> getChildrenAtRelativeDepth(int depth) {
        Stream<Zone> stream = Stream.of(this);
        for (int i = 0; i < depth; i++) {
            stream = stream.flatMap((z) -> z.getChildren().stream());
        }
        return stream;
    }

    /**
     * If zone have different levels, the lowest level is place first.
     * If zone have the same level, they are sorted using the long identifier,
     * or the text identifier if the hierarchy of one of the zones has no long identifiers.
     *
     * @param o to compare against
     */
    @Override
    public default int compareTo(Zone o) {
        final int level = getLocationType().getRefinementLevel();
        final int levelOther = o.getLocationType().getRefinementLevel();
        if (level != levelOther) {
            return Integer.compare(level, levelOther);
        }
        if (hasLongIdentifier(this) && hasLongIdentifier(o)) {
            return Long.compareUnsigned(getLongIdentifier(), o.getLongIdentifier());
        }
        return getTextIdentifier().toString().compareTo(o.getTextIdentifier().toString());
    }

    /**
     * Returns whether the hierarchy of the given zone has long identifiers.
     */
    private static boolean hasLongIdentifier(final Zone zone) {
        return zone.getLocationType().getReferenceSystem().getGridSystem().getHierarchy().supportLongIdentifiers();
    }
}
