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
package org.apache.sis.geometry.wrapper.s2;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2Loop;
import com.google.common.geometry.S2Point;
import com.google.common.geometry.S2Polygon;
import com.google.common.geometry.S2PolygonBuilder;
import com.google.common.geometry.S2Region;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.BoundingPolygon;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.metadata.iso.extent.DefaultBoundingPolygon;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.Utilities;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class S2 {

    /**
     * Maximal length in degrees of envelope edge segments.
     */
    private static final double MAX_STEP = 1;

    private static final GeometryFactory GF = new GeometryFactory();

    /**
     * Do not allow instantiation of this class.
     */
    private S2() {
    }

    /**
     * Converts an envelope to a polygon whose edges follow the envelope parallels and meridians.
     * <p>
     * Polygon edges are great circle arcs, so envelope edges are split in steps of at most 1°.
     * <ul>
     *   <li>The envelope must be in CRS:84.</li>
     *   <li>Envelopes crossing the anti-meridian (lower &gt; upper longitude) are supported.</li>
     *   <li>An envelope spanning 360° of longitude becomes a cap when it reaches one pole,
     *       the whole sphere when it reaches both, and a band otherwise.</li>
     *   <li>An envelope reaching a pole with a narrower longitude span has a single vertex on that pole.</li>
     * </ul>
     *
     * @param env in CRS:84
     * @return S2 polygon
     */
    public static S2Polygon toS2Polygon(Envelope env) {
        if (!Utilities.equalsIgnoreMetadata(env.getCoordinateReferenceSystem(), CommonCRS.WGS84.normalizedGeographic())) {
            throw new IllegalArgumentException("Envelope must be in CRS:84");
        }
        final GeneralEnvelope genv = GeneralEnvelope.castOrCopy(env);
        final double west  = genv.getLower(0);
        final double width = genv.getSpan(0);
        final double south = Math.max(-90, genv.getLower(1));
        final double north = Math.min( 90, genv.getUpper(1));
        final boolean southPole = south <= -90;
        final boolean northPole = north >=  90;

        if (width >= 360) {
            if (southPole && northPole) {
                return new S2Polygon(S2Loop.full());
            }
            // The polygon interior is made of points contained by an odd number of loops.
            final List<S2Loop> loops = new ArrayList<>(2);
            if (southPole) {
                loops.add(parallelLoop(north, west, false));
            } else {
                loops.add(parallelLoop(south, west, true));
                if (!northPole) {
                    loops.add(parallelLoop(north, west, true));
                }
            }
            return new S2Polygon(loops);
        }

        final int columns = Math.max(1, (int) Math.ceil(width / MAX_STEP));
        final int rows    = Math.max(1, (int) Math.ceil((north - south) / MAX_STEP));
        final double east = west + width;
        final List<S2Point> points = new ArrayList<>();
        // Counter-clockwise: south edge eastward, east edge northward, north edge westward, west edge southward.
        if (southPole) {
            points.add(S2LatLng.fromDegrees(-90, west).toPoint());
        } else {
            for (int i = 0; i < columns; i++) {
                points.add(S2LatLng.fromDegrees(south, west + width * i / columns).toPoint());
            }
        }
        for (int i = southPole ? 1 : 0; i < rows; i++) {
            points.add(S2LatLng.fromDegrees(south + (north - south) * i / rows, east).toPoint());
        }
        if (northPole) {
            points.add(S2LatLng.fromDegrees(90, east).toPoint());
        } else {
            for (int i = 0; i < columns; i++) {
                points.add(S2LatLng.fromDegrees(north, east - width * i / columns).toPoint());
            }
        }
        for (int i = northPole ? 1 : 0; i < rows; i++) {
            points.add(S2LatLng.fromDegrees(north - (north - south) * i / rows, west).toPoint());
        }
        return new S2Polygon(new S2Loop(points));
    }

    /**
     * Returns a loop following a parallel over 360° of longitude.
     *
     * @param  latitude  latitude of the parallel in degrees.
     * @param  start     longitude of the first vertex in degrees.
     * @param  eastward  {@code true} for a loop enclosing the cap north of the parallel,
     *                   {@code false} for the cap south of it.
     */
    private static S2Loop parallelLoop(final double latitude, final double start, final boolean eastward) {
        final int count = (int) Math.ceil(360 / MAX_STEP);
        final double direction = eastward ? 360 : -360;
        final List<S2Point> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            points.add(S2LatLng.fromDegrees(latitude, start + direction * i / count).toPoint());
        }
        return new S2Loop(points);
    }

    /**
     *
     * @param jts in WGS:84 (Lon,Lat)
     * @return S2Polygon
     */
    public static S2Polygon toS2Polygon(Polygon p) {
        final S2PolygonBuilder builder = new S2PolygonBuilder();
        builder.addLoop(toLoop(p.getExteriorRing(), false));
        final int interior = p.getNumInteriorRing();
        for (int i = 0; i < interior; i++) {
            builder.addLoop(toLoop(p.getInteriorRingN(i), true));
        }
        return builder.assemblePolygon();
    }

    /**
     *
     * @param jts in WGS:84 (Lon,Lat)
     * @return S2Polygon
     */
    public static S2Polygon toS2Polygon(MultiPolygon p) {
        final int nb = p.getNumGeometries();
        final S2PolygonBuilder builder = new S2PolygonBuilder();
        for (int i = 0; i < nb; i++) {
            Geometry n = p.getGeometryN(i);
            builder.addPolygon(toS2Polygon((Polygon)n));
        }
        return builder.assemblePolygon();
    }

    /**
     * Convert S2 region to SIS bounding polygon.
     * @param polygon not null
     * @return BoundingPolygon
     */
    public static BoundingPolygon toGeographicExtent(S2Region polygon) {
        return new DefaultBoundingPolygon(Factory.INSTANCE.castOrWrap(polygon));
    }

    /**
     * Convert JTS LinearRing to S2Loop.
     *
     * @param ring not null
     * @param isHole true if ring is a hole
     * @return S2 loop
     */
    public static S2Loop toLoop(org.locationtech.jts.geom.LinearRing ring, boolean isHole) {
        final List<S2Point> vertices = new ArrayList<>();
        final CoordinateSequence cs = ring.getCoordinateSequence();
        for (int i = 0, n = cs.size()-1; i < n ;i++) {
            final S2LatLng ll = S2LatLng.fromDegrees(cs.getY(i), cs.getX(i));
            if (!ll.isValid()) {
                throw new IllegalArgumentException();
            }
            vertices.add(ll.toPoint());
        }
        final S2Loop loop = new S2Loop(vertices);
        if (isHole) {
            if (loop.isNormalized()) {
                loop.invert();
            }
        } else {
            if (!loop.isNormalized()) {
                loop.invert();
            }
        }
        return loop;
    }


    public static Polygon toJTSPolygon(S2Polygon s2) {
        if (s2 == null) return null;
        if (s2.getLoops().size() > 1) {
            throw new UnsupportedOperationException();
        }
        final LinearRing ring = toRing(s2.getLoops().get(0));
        final Polygon polygon = GF.createPolygon(ring);
        polygon.setUserData(CommonCRS.WGS84.normalizedGeographic());
        return polygon;
    }

    public static LinearRing toRing(S2Loop s2) {
        final double[] coords = toArray(s2);
        final CoordinateSequence cs = new PackedCoordinateSequence.Double(coords, 2, 0);
        return GF.createLinearRing(cs);
    }

    /**
     * @return polygon points, as a closed (first point == last point)
     */
    public static double[] toArray(S2Loop s2) {
        if (s2 == null) return null;
        /*
        S2 ensure the interior is in the left side.
        if it's not the case then we have crossed the date-line
        */
        final List<S2Point> vertices = s2.orientedVertices();
        final double[] coords = new double[(vertices.size()+1)*2];
        for (int k = 0, i = 0, n = vertices.size(); i < n; i++) {
            final S2Point pt = vertices.get(i);
            final S2LatLng latlng = new S2LatLng(pt);
            coords[k++] = latlng.lngDegrees();
            coords[k++] = latlng.latDegrees();
        }
        coords[coords.length-2] = coords[0];
        coords[coords.length-1] = coords[1];

        // We compute the area which will give us the polygon orientation.
        double area = 0.0;
        for (int i = 0; i < coords.length-2; i+=2) {
            area += (coords[i+2]-coords[i]) * (coords[i+3]+coords[i+1]);
        }

        if (area > 0.0) {
            //polygon is clockwise, which means it's crossing the dateline, we need to fix it
            //push every longitude coordinate which is negative by +360
            boolean allOver180 = true;
            for (int i = 0; i < coords.length; i+=2) {
                if (coords[i] < 0) coords[i] += 360;
                if (coords[i] < 180) allOver180 = false;
            }
            if (allOver180) {
                for (int i = 0; i < coords.length; i+=2) {
                    coords[i] += -360;
                }
            }
        }

        return coords;
    }
}
