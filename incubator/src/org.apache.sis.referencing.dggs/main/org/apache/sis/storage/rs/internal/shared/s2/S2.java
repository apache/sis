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
package org.apache.sis.storage.rs.internal.shared.s2;

import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2Loop;
import com.google.common.geometry.S2Point;
import com.google.common.geometry.S2Polygon;
import com.google.common.geometry.S2PolygonBuilder;
import com.google.common.geometry.S2Region;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.metadata.iso.extent.DefaultBoundingPolygon;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.Utilities;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.BoundingPolygon;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class S2 {

    private static final GeometryFactory GF = new GeometryFactory();

    /**
     * Do not allow instantiation of this class.
     */
    private S2() {
    }

    /**
     * @param env in CRS:84
     * @return S2 polygon
     */
    public static S2Polygon toS2Polygon(Envelope env) {
        if (!Utilities.equalsIgnoreMetadata(env.getCoordinateReferenceSystem(), CommonCRS.WGS84.normalizedGeographic())) {
            throw new IllegalArgumentException("Envelope must be in CRS:84");
        }

        GeneralEnvelope genv = GeneralEnvelope.castOrCopy(env);

        final double minLon = genv.getLower(0);
        final double minLat = genv.getLower(1);
        final double maxLon = genv.getUpper(0);
        final double maxLat = genv.getUpper(1);
        final double spanLon = genv.getSpan(0);
        final double spanLat = genv.getSpan(1);

        final double[] lons;
        final double[] lats;

        if (spanLon >= 360) {
            //add 2 split points
            lons = new double[]{
                minLon,
                minLon + (maxLon - minLon) * (1.0/3.0),
                minLon + (maxLon - minLon) * (2.0/3.0),
                maxLon
            };
        } else if (spanLon >= 180) {
            lons = new double[]{
                minLon,
                minLon + (maxLon - minLon) * (1.0/2.0),
                maxLon
            };
        } else {
            lons = new double[]{
                minLon,
                maxLon
            };
        }

        if (spanLat >= 180) {
            lats = new double[]{
                minLat,
                minLat + (maxLat - minLat) * (1.0/2.0),
                maxLat
            };
        } else {
            lats = new double[]{
                minLat,
                maxLat
            };
        }

        //crosses the antimeridian
        if (minLon > maxLon) {
            //reverse array to preserve CCW direction
            for (int i = 0; i < lons.length / 2; i++) {
                double t = lons[i];
                lons[i] = lons[lons.length - 1 - i];
                lons[lons.length - 1 - i] = t;
            }
        }

        //create polygon in counter-clockwise direction
        final List<S2Point> points = new ArrayList<>();
        for (int i = 0; i < lons.length; i++) {
            points.add(S2LatLng.fromDegrees(lats[0], lons[i]).toPoint());
        }
        for (int i = 1; i < lats.length-1; i++) { //start at 1, end at size-2, do not duplicate corner point
            points.add(S2LatLng.fromDegrees(lats[i], lons[lons.length-1]).toPoint());
        }
        for (int i = lons.length-1; i >= 0; i--) {
            points.add(S2LatLng.fromDegrees(lats[lats.length-1], lons[i]).toPoint());
        }
        for (int i = lats.length-2; i >= 1; i--) { //start at size-2, end at 1, do not duplicate corner point
            points.add(S2LatLng.fromDegrees(lats[i], lons[0]).toPoint());
        }

        return new S2Polygon(new S2Loop(points));
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
