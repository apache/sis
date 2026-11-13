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
package org.apache.sis.storage.geojson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.storage.geojson.binding.GeoJSONGeometry;
import org.apache.sis.storage.geojson.binding.GeoJSONMultiPolygon;
import org.apache.sis.storage.geojson.binding.GeoJSONPolygon;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.identification.Identification;

/**
 * The geographic bounding boxes of a metadata record, as GeoJSON geometry and bbox.
 *
 * <p>Only the bounding boxes of the first identification are used, since it is the only one encoded.
 *
 * <p>Constraints:
 * <ul>
 *   <li>bounding boxes with {@code extentTypeCode} set to {@code false} (exclusion) are ignored,</li>
 *   <li>bounding boxes with non-finite bounds are ignored,</li>
 *   <li>a bounding box crossing the anti-meridian (west &gt; east) is split in two polygons.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 4.3"
 * @see "ISO-19115-4:2026 section 5.1.3 j)"
 */
final class GeographicFootprint {

    private static final double MIN_LONGITUDE = -180;
    private static final double MAX_LONGITUDE = 180;

    private final List<GeographicBoundingBox> boxes = new ArrayList<>();

    /**
     * Collects the bounding boxes of the first identification of the given metadata.
     */
    GeographicFootprint(final Metadata metadata) {
        final Collection<? extends Identification> identifications = metadata.getIdentificationInfo();
        if (identifications == null) {
            return;
        }
        final Iterator<? extends Identification> it = identifications.iterator();
        final Identification identification = it.hasNext() ? it.next() : null;
        if (identification == null || identification.getExtents() == null) {
            return;
        }
        for (final Extent extent : identification.getExtents()) {
            if (extent == null || extent.getGeographicElements() == null) {
                continue;
            }
            for (final GeographicExtent element : extent.getGeographicElements()) {
                if (element instanceof GeographicBoundingBox box && !Boolean.FALSE.equals(box.getInclusion())
                        && Double.isFinite(box.getWestBoundLongitude()) && Double.isFinite(box.getEastBoundLongitude())
                        && Double.isFinite(box.getSouthBoundLatitude()) && Double.isFinite(box.getNorthBoundLatitude()))
                {
                    boxes.add(box);
                }
            }
        }
    }

    /**
     * Returns the bounding boxes as a GeoJSON polygon or multi-polygon, or {@code null} if there is no box.
     */
    GeoJSONGeometry toGeometry() {
        final List<List<List<List<Double>>>> polygons = new ArrayList<>();
        for (final GeographicBoundingBox box : boxes) {
            final double west  = box.getWestBoundLongitude();
            final double east  = box.getEastBoundLongitude();
            final double south = box.getSouthBoundLatitude();
            final double north = box.getNorthBoundLatitude();
            if (west <= east) {
                polygons.add(polygon(west, south, east, north));
            } else {
                polygons.add(polygon(west, south, MAX_LONGITUDE, north));
                polygons.add(polygon(MIN_LONGITUDE, south, east, north));
            }
        }
        if (polygons.isEmpty()) {
            return null;
        } else if (polygons.size() == 1) {
            final GeoJSONPolygon polygon = new GeoJSONPolygon();
            polygon.setCoordinates(polygons.get(0));
            return polygon;
        } else {
            final GeoJSONMultiPolygon multi = new GeoJSONMultiPolygon();
            multi.setCoordinates(polygons);
            return multi;
        }
    }

    /**
     * Returns the union of all bounding boxes as {@code [west, south, east, north]}, or {@code null} if there is no box.
     *
     * <p>West may be greater than east if the union crosses the anti-meridian, as allowed by RFC 7946.
     */
    List<Double> toBbox() {
        if (boxes.isEmpty()) {
            return null;
        }
        final DefaultGeographicBoundingBox union = new DefaultGeographicBoundingBox(boxes.get(0));
        for (int i = 1; i < boxes.size(); i++) {
            union.add(boxes.get(i));
        }
        return List.of(union.getWestBoundLongitude(), union.getSouthBoundLatitude(),
                       union.getEastBoundLongitude(), union.getNorthBoundLatitude());
    }

    /**
     * Returns a counterclockwise rectangular polygon with a single exterior ring.
     */
    private static List<List<List<Double>>> polygon(final double west, final double south,
                                                    final double east, final double north)
    {
        final List<List<Double>> ring = List.of(
                List.of(west, south),
                List.of(east, south),
                List.of(east, north),
                List.of(west, north),
                List.of(west, south));
        return List.of(ring);
    }
}
