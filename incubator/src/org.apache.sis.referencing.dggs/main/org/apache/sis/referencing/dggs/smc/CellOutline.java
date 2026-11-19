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
package org.apache.sis.referencing.dggs.smc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2Loop;
import com.google.common.geometry.S2Point;
import com.google.common.geometry.S2Polygon;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.ArgumentChecks;


/**
 * Outline of an SMC cell as a polygon on the sphere, and its envelope.
 * <p>
 * Polygon edges are great circle arcs while cell edges along parallels are not, so edges are densified.
 * Meridians are great circles, but after a pole rotation their geographic extremes may lie between
 * the cell corners, so they are densified too for accurate envelopes.
 *
 * <ul>
 *   <li>Edges are split in steps of at most 1° of lattice coordinates, then each step by the subdivision.</li>
 *   <li>Vertices are counter-clockwise. The south polar ring therefore runs westward.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
final class CellOutline {

    /**
     * Maximal length in degrees of an edge segment before subdivision.
     */
    private static final double MAX_STEP = 1;

    private final SmcZone zone;

    /**
     * Grid system converting lattice coordinates to the base CRS.
     */
    private final SmcDggs dggs;

    CellOutline(final SmcZone zone) {
        this.zone = zone;
        this.dggs = zone.grid.dggrs.dggs;
    }

    /**
     * Returns the outline vertices as (longitude, latitude) pairs in lattice coordinates, without closing point.
     * A polar cell has a single edge along its parallel, all around the pole.
     *
     * @param  subdivision  number of segments per edge step, at least 1.
     * @return counter-clockwise vertices.
     */
    double[] latticeRing(final int subdivision) {
        ArgumentChecks.ensureStrictlyPositive("subdivision", subdivision);
        final double west  = zone.west();
        final double east  = zone.east();
        final double south = zone.south();
        final double north = zone.north();
        final DoubleStream.Builder ring = DoubleStream.builder();
        if (!zone.isPolar()) {
            addEdge(ring, west, south, east, south, subdivision);
            addEdge(ring, east, south, east, north, subdivision);
            addEdge(ring, east, north, west, north, subdivision);
            addEdge(ring, west, north, west, south, subdivision);
        } else if (zone.isNorthPolar()) {
            addEdge(ring, west, south, east, south, subdivision);
        } else {
            addEdge(ring, east, north, west, north, subdivision);
        }
        return ring.build().toArray();
    }

    /**
     * Adds the vertices of an edge, from its start point inclusive to its end point exclusive.
     */
    private static void addEdge(final DoubleStream.Builder ring, final double startLongitude, final double startLatitude,
                                final double endLongitude, final double endLatitude, final int subdivision)
    {
        final double length = Math.max(Math.abs(endLongitude - startLongitude), Math.abs(endLatitude - startLatitude));
        final int count = Math.max(1, (int) Math.ceil(length / MAX_STEP)) * subdivision;
        for (int i = 0; i < count; i++) {
            ring.add(startLongitude + (endLongitude - startLongitude) * i / count)
                .add(startLatitude  + (endLatitude  - startLatitude)  * i / count);
        }
    }

    /**
     * Returns the outline as an S2 polygon in the base CRS.
     *
     * @param  subdivision  number of segments per edge step, at least 1.
     * @return polygon of the zone.
     * @throws TransformException if a vertex cannot be converted.
     */
    S2Polygon toPolygon(final int subdivision) throws TransformException {
        final double[] ring = latticeRing(subdivision);
        dggs.toBase(ring, 0, ring.length / 2);
        final List<S2Point> points = new ArrayList<>(ring.length / 2);
        for (int i = 0; i < ring.length; i += 2) {
            points.add(S2LatLng.fromDegrees(ring[i + 1], ring[i]).toPoint());
        }
        return new S2Polygon(new S2Loop(points));
    }

    /**
     * Returns the envelope of the outline in the base CRS.
     * <p>
     * The longitude range is the smallest one containing all vertices, so it may cross the anti-meridian
     * (west &gt; east). A cell containing a geographic pole spans all longitudes up to that pole.
     * For rotated lattices, latitude extremes are those of the densified vertices.
     *
     * @return envelope of the zone.
     * @throws TransformException if a vertex cannot be converted.
     */
    GeneralEnvelope envelope() throws TransformException {
        final double[] ring = latticeRing(1);
        final int count = ring.length / 2;
        dggs.toBase(ring, 0, count);
        final double[] longitudes = new double[count];
        double south = Double.POSITIVE_INFINITY;
        double north = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < count; i++) {
            longitudes[i] = ring[2 * i];
            south = Math.min(south, ring[2 * i + 1]);
            north = Math.max(north, ring[2 * i + 1]);
        }
        Arrays.sort(longitudes);
        double west = longitudes[0];
        double east = longitudes[count - 1];
        double gap  = 360 - (east - west);
        for (int i = 1; i < count; i++) {
            final double candidate = longitudes[i] - longitudes[i - 1];
            if (candidate > gap) {
                gap  = candidate;
                west = longitudes[i];
                east = longitudes[i - 1];
            }
        }
        if (east == -180 && west > east) {
            east = 180;
        }
        if (containsPole(90)) {
            north = 90;
            west  = -180;
            east  = 180;
        }
        if (containsPole(-90)) {
            south = -90;
            west  = -180;
            east  = 180;
        }
        final GeneralEnvelope envelope = new GeneralEnvelope(dggs.getCrs());
        envelope.setRange(0, west, east);
        envelope.setRange(1, south, north);
        return envelope;
    }

    /**
     * Returns whether the cell contains the geographic pole of the given latitude.
     */
    private boolean containsPole(final double latitude) throws TransformException {
        final double[] pole = new double[] {0, latitude};
        dggs.toLattice(pole, 0, pole, 0);
        return zone.equals(zone.grid.zoneAt(pole[0], pole[1]));
    }
}
