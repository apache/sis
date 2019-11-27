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
package org.apache.sis.internal.feature.jts;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Set of Utility function and methods for mapping geometry types.
 *
 * @author Quentin Boileau (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public final class JTSMapping {

    private static final GeometryFactory GF = new GeometryFactory();

    private JTSMapping() {
    }

    /**
     * Force geometry type. If the given geometry is not of given class it will
     * be adapted.
     */
    public static <T extends Geometry> T convertType(final Geometry geom, final Class<T> targetClass) {
        if (geom == null) {
            return null;
        }

        if (targetClass.isInstance(geom)) {
            return (T) geom;
        }

        Geometry result;
        if (targetClass == Point.class) {
            result = convertToPoint(geom);
        } else if (targetClass == MultiPoint.class) {
            result = convertToMultiPoint(geom);
        } else if (targetClass == LineString.class) {
            result = convertToLineString(geom);
        } else if (targetClass == MultiLineString.class) {
            result = convertToMultiLineString(geom);
        } else if (targetClass == Polygon.class) {
            result = convertToPolygon(geom);
        } else if (targetClass == MultiPolygon.class) {
            result = convertToMultiPolygon(geom);
        } else if (targetClass == GeometryCollection.class) {
            result = convertToGeometryCollection(geom);
        } else {
            result = null;
        }

        if (result != null) {
            //copy srid and user data
            result.setSRID(geom.getSRID());
            result.setUserData(geom.getUserData());
        }

        return targetClass.cast(result);
    }

    // Convert to Point --------------------------------------------------------
    private static Point convertToPoint(final Geometry geom) {
        if (geom instanceof Point) {
            return convertToPoint((Point) geom);
        } else if (geom instanceof MultiPoint) {
            return convertToPoint((MultiPoint) geom);
        } else if (geom instanceof LineString) {
            return convertToPoint((LineString) geom);
        } else if (geom instanceof MultiLineString) {
            return convertToPoint((MultiLineString) geom);
        } else if (geom instanceof Polygon) {
            return convertToPoint((Polygon) geom);
        } else if (geom instanceof MultiPolygon) {
            return convertToPoint((MultiPolygon) geom);
        }
        return null;
    }

    private static Point convertToPoint(final Point pt) {
        return pt;
    }

    private static Point convertToPoint(final MultiPoint pt) {
        return pt.getCentroid();
    }

    private static Point convertToPoint(final LineString pt) {
        return pt.getCentroid();
    }

    private static Point convertToPoint(final MultiLineString pt) {
        return pt.getCentroid();
    }

    private static Point convertToPoint(final Polygon pt) {
        return pt.getCentroid();
    }

    private static Point convertToPoint(final MultiPolygon pt) {
        return pt.getCentroid();
    }

    // Convert to MultiPoint ---------------------------------------------------
    private static MultiPoint convertToMultiPoint(final Geometry geom) {
        if (geom instanceof Point) {
            return convertToMultiPoint((Point) geom);
        } else if (geom instanceof MultiPoint) {
            return convertToMultiPoint((MultiPoint) geom);
        } else if (geom instanceof LineString) {
            return convertToMultiPoint((LineString) geom);
        } else if (geom instanceof MultiLineString) {
            return convertToMultiPoint((MultiLineString) geom);
        } else if (geom instanceof Polygon) {
            return convertToMultiPoint((Polygon) geom);
        } else if (geom instanceof MultiPolygon) {
            return convertToMultiPoint((MultiPolygon) geom);
        }
        return null;
    }

    private static MultiPoint convertToMultiPoint(final Point pt) {
        return GF.createMultiPoint(new Point[]{pt});
    }

    private static MultiPoint convertToMultiPoint(final MultiPoint pt) {
        return pt;
    }

    private static MultiPoint convertToMultiPoint(final LineString pt) {
        return GF.createMultiPoint(pt.getCoordinates());
    }

    private static MultiPoint convertToMultiPoint(final MultiLineString pt) {
        return GF.createMultiPoint(pt.getCoordinates());
    }

    private static MultiPoint convertToMultiPoint(final Polygon pt) {
        return GF.createMultiPoint(pt.getCoordinates());
    }

    private static MultiPoint convertToMultiPoint(final MultiPolygon pt) {
        return GF.createMultiPoint(pt.getCoordinates());
    }

    // Convert to LineString ---------------------------------------------------
    private static LineString convertToLineString(final Geometry geom) {
        if (geom instanceof Point) {
            return convertToLineString((Point) geom);
        } else if (geom instanceof MultiPoint) {
            return convertToLineString((MultiPoint) geom);
        } else if (geom instanceof LineString) {
            return convertToLineString((LineString) geom);
        } else if (geom instanceof MultiLineString) {
            return convertToLineString((MultiLineString) geom);
        } else if (geom instanceof Polygon) {
            return convertToLineString((Polygon) geom);
        } else if (geom instanceof MultiPolygon) {
            return convertToLineString((MultiPolygon) geom);
        }
        return null;
    }

    private static LineString convertToLineString(final Point pt) {
        return GF.createLineString(new Coordinate[]{pt.getCoordinate(), pt.getCoordinate()});
    }

    private static LineString convertToLineString(final MultiPoint pt) {
        final Coordinate[] coords = pt.getCoordinates();
        if (coords.length == 1) {
            return GF.createLineString(new Coordinate[]{coords[0], coords[0]});
        } else {
            return GF.createLineString(coords);
        }
    }

    private static LineString convertToLineString(final LineString pt) {
        return pt;
    }

    private static LineString convertToLineString(final MultiLineString pt) {
        return GF.createLineString(pt.getCoordinates());
    }

    private static LineString convertToLineString(final Polygon pt) {
        return GF.createLineString(pt.getCoordinates());
    }

    private static LineString convertToLineString(final MultiPolygon pt) {
        return GF.createLineString(pt.getCoordinates());
    }

    // Convert to MultiLineString ----------------------------------------------
    private static MultiLineString convertToMultiLineString(final Geometry geom) {
        if (geom instanceof Point) {
            return convertToMultiLineString((Point) geom);
        } else if (geom instanceof MultiPoint) {
            return convertToMultiLineString((MultiPoint) geom);
        } else if (geom instanceof LineString) {
            return convertToMultiLineString((LineString) geom);
        } else if (geom instanceof MultiLineString) {
            return convertToMultiLineString((MultiLineString) geom);
        } else if (geom instanceof Polygon) {
            return convertToMultiLineString((Polygon) geom);
        } else if (geom instanceof MultiPolygon) {
            return convertToMultiLineString((MultiPolygon) geom);
        }
        return null;
    }

    private static MultiLineString convertToMultiLineString(final Point pt) {
        return convertToMultiLineString(convertToLineString(pt));
    }

    private static MultiLineString convertToMultiLineString(final MultiPoint pt) {
        return convertToMultiLineString(convertToLineString(pt));
    }

    private static MultiLineString convertToMultiLineString(final LineString pt) {
        return GF.createMultiLineString(new LineString[]{pt});
    }

    private static MultiLineString convertToMultiLineString(final MultiLineString pt) {
        return pt;
    }

    private static MultiLineString convertToMultiLineString(final Polygon pt) {
        return convertToMultiLineString(GF.createLineString(pt.getCoordinates()));
    }

    private static MultiLineString convertToMultiLineString(final MultiPolygon pt) {
        final int n = pt.getNumGeometries();
        final LineString[] geoms = new LineString[n];
        for (int i = 0; i < n; i++) {
            geoms[i] = convertToLineString(pt.getGeometryN(i));
        }
        return GF.createMultiLineString(geoms);
    }

    // Convert to Polygon ------------------------------------------------------
    private static Polygon convertToPolygon(final Geometry geom) {
        if (geom instanceof Point) {
            return convertToPolygon((Point) geom);
        } else if (geom instanceof MultiPoint) {
            return convertToPolygon((MultiPoint) geom);
        } else if (geom instanceof LineString) {
            return convertToPolygon((LineString) geom);
        } else if (geom instanceof MultiLineString) {
            return convertToPolygon((MultiLineString) geom);
        } else if (geom instanceof Polygon) {
            return convertToPolygon((Polygon) geom);
        } else if (geom instanceof MultiPolygon) {
            return convertToPolygon((MultiPolygon) geom);
        }
        return null;
    }

    private static Polygon convertToPolygon(final Point pt) {
        LinearRing ring = GF.createLinearRing(new Coordinate[]{pt.getCoordinate(), pt.getCoordinate(), pt.getCoordinate(), pt.getCoordinate()});
        return GF.createPolygon(ring, new LinearRing[0]);
    }

    private static Polygon convertToPolygon(final MultiPoint pt) {
        return convertToPolygon(convertToLineString(pt));
    }

    private static Polygon convertToPolygon(final LineString pt) {
        return GF.createPolygon(GF.createLinearRing(pt.getCoordinates()), new LinearRing[0]);
    }

    private static Polygon convertToPolygon(final MultiLineString pt) {
        return convertToPolygon(convertToLineString(pt));
    }

    private static Polygon convertToPolygon(final Polygon pt) {
        return pt;
    }

    private static Polygon convertToPolygon(final MultiPolygon pt) {
        final int nbGeom = pt.getNumGeometries();
        if (nbGeom == 0) {
            return GF.createPolygon();
        } else if (nbGeom == 1) {
            return (Polygon) pt.getGeometryN(0);
        } else {
            return convertToPolygon(pt.convexHull());
        }
    }

    // Convert to MultiPolygon -------------------------------------------------
    private static MultiPolygon convertToMultiPolygon(final Geometry geom) {
        if (geom instanceof Point) {
            return convertToMultiPolygon((Point) geom);
        } else if (geom instanceof MultiPoint) {
            return convertToMultiPolygon((MultiPoint) geom);
        } else if (geom instanceof LineString) {
            return convertToMultiPolygon((LineString) geom);
        } else if (geom instanceof MultiLineString) {
            return convertToMultiPolygon((MultiLineString) geom);
        } else if (geom instanceof Polygon) {
            return convertToMultiPolygon((Polygon) geom);
        } else if (geom instanceof MultiPolygon) {
            return convertToMultiPolygon((MultiPolygon) geom);
        }
        return null;
    }

    private static MultiPolygon convertToMultiPolygon(final Point pt) {
        return convertToMultiPolygon(convertToPolygon(pt));
    }

    private static MultiPolygon convertToMultiPolygon(final MultiPoint pt) {
        return convertToMultiPolygon(convertToPolygon(pt));
    }

    private static MultiPolygon convertToMultiPolygon(final LineString pt) {
        return convertToMultiPolygon(convertToPolygon(pt));
    }

    private static MultiPolygon convertToMultiPolygon(final MultiLineString pt) {
        return convertToMultiPolygon(convertToPolygon(pt));
    }

    private static MultiPolygon convertToMultiPolygon(final Polygon pt) {
        return GF.createMultiPolygon(new Polygon[]{pt});
    }

    private static MultiPolygon convertToMultiPolygon(final MultiPolygon pt) {
        return pt;
    }

    private static GeometryCollection convertToGeometryCollection(final Geometry geom) {
        if (geom instanceof GeometryCollection) {
            return (GeometryCollection) geom;
        }

        if (geom instanceof Point) {
            return convertToMultiPoint(geom);
        } else if (geom instanceof LineString) {
            return convertToMultiLineString(geom);
        } else if (geom instanceof Polygon) {
            return convertToMultiPolygon(geom);
        }
        return null;
    }

}
