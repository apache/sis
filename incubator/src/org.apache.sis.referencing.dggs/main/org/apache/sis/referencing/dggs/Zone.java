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

import java.util.Collection;
import java.util.stream.Stream;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.storage.dggs.DiscreteGlobalGridSystems;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.gazetteer.Location;
import org.opengis.util.InternationalString;

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

    @Override
    default Envelope getEnvelope() {
        final Geometry geometry = DiscreteGlobalGridSystems.toSISPolygon(getGeographicExtent());
        if (geometry == null) {
            return CRS.getDomainOfValidity(CommonCRS.WGS84.normalizedGeographic());
        }
        return new GeneralEnvelope(geometry.getEnvelope());
    }

    /**
     * @return geometry type name of the zone
     */
    String getShapeType();

    /**
     * @return area of the zone, can be null if not 2D
     */
    Double getAreaMetersSquare();

    /**
     * @return volume if the zone, can be null if not 3D
     */
    Double volumeMetersCube();

    /**
     * @return duration of the zone, can be null if it has no temporal dimension
     */
    Double temporalDurationSeconds();

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
     * @return true if zones are siblings
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
     * If zone have the same level, they are sorted using the indexed identifier.
     *
     * @param o to compare against
     */
    @Override
    public default int compareTo(Zone o) {
        final int level = getLocationType().getRefinementLevel();
        final int levelOther = o.getLocationType().getRefinementLevel();
        if (level == levelOther) {
            return Long.compareUnsigned(getLongIdentifier(), o.getLongIdentifier());
        } else if (level < levelOther) {
            return -1; //place it before
        } else {
            return +1; //place it after
        }
    }
}
