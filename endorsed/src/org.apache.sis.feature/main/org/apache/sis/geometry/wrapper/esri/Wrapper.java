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
package org.apache.sis.geometry.wrapper.esri;

import java.util.Iterator;
import java.util.function.Supplier;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.MultiVertexGeometry;
import com.esri.core.geometry.Operator;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Point2D;
import com.esri.core.geometry.WktImportFlags;
import com.esri.core.geometry.WktExportFlags;
import com.esri.core.geometry.OperatorExportToWkt;
import com.esri.core.geometry.OperatorCentroid2D;
import com.esri.core.geometry.OperatorIntersects;
import com.esri.core.geometry.OperatorSimpleRelation;
import com.esri.core.geometry.ProgressTracker;
import com.esri.core.geometry.SpatialReference;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.filter.sqlmm.SQLMM;
import org.apache.sis.util.Debug;

// Specific to the main branch:
import org.apache.sis.pending.geoapi.filter.SpatialOperatorName;


/**
 * The wrapper of ERSI geometries.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
final class Wrapper extends GeometryWrapper {
    /**
     * The wrapped implementation.
     */
    private final Geometry geometry;

    /**
     * Creates a new wrapper around the given geometry.
     */
    Wrapper(final Geometry geometry) {
        this.geometry = geometry;
    }

    /**
     * Returns the implementation-dependent factory of geometric object.
     */
    @Override
    public Geometries<Geometry> factory() {
        return Factory.INSTANCE;
    }

    /**
     * Returns the geometry specified at construction time.
     */
    @Override
    protected Object implementation() {
        return geometry;
    }

    /**
     * Returns the ESRI envelope as an Apache SIS implementation.
     *
     * @return the envelope.
     */
    @Override
    public GeneralEnvelope getEnvelope() {
        final Envelope2D bounds = new Envelope2D();
        geometry.queryEnvelope2D(bounds);
        final GeneralEnvelope env = createEnvelope();
        env.setRange(0, bounds.xmin, bounds.xmax);
        env.setRange(1, bounds.ymin, bounds.ymax);
        return env;
    }

    /**
     * Returns the centroid of the wrapped geometry as a direct position.
     */
    @Override
    public DirectPosition getCentroid() {
        final Point2D c = OperatorCentroid2D.local().execute(geometry, null);
        return new DirectPosition2D(getCoordinateReferenceSystem(), c.x, c.y);
    }

    /**
     * If the wrapped geometry is a point, returns its coordinates. Otherwise returns {@code null}.
     * If non-null, the returned array may have a length of 2 or 3.
     */
    @Override
    public double[] getPointCoordinates() {
        if (!(geometry instanceof Point)) {
            return null;
        }
        final Point pt = (Point) geometry;
        final double[] coord;
        if (pt.hasZ()) {
            coord = new double[Factory.TRIDIMENSIONAL];
            coord[2] = pt.getZ();
        } else {
            coord = new double[Factory.BIDIMENSIONAL];
        }
        coord[1] = pt.getY();
        coord[0] = pt.getX();
        return coord;
    }

    /**
     * Returns all coordinate tuples in the wrapped geometry.
     * This method is currently used for testing purpose only.
     */
    @Debug
    @Override
    public double[] getAllCoordinates() {
        if (geometry instanceof MultiVertexGeometry) {
            final Point2D[] points = ((MultiVertexGeometry) geometry).getCoordinates2D();
            final double[] coordinates = new double[points.length * Factory.BIDIMENSIONAL];
            int i = 0;
            for (final Point2D p : points) {
                coordinates[i++] = p.x;
                coordinates[i++] = p.y;
            }
            return coordinates;
        }
        return null;
    }

    /**
     * Merges a sequence of points or paths after the wrapped geometry.
     *
     * @throws ClassCastException if an element in the iterator is not an ESRI geometry.
     */
    @Override
    public Geometry mergePolylines(final Iterator<?> polylines) {
        final Polyline path = new Polyline();
        boolean lineTo = false;
add:    for (Geometry next = geometry;;) {
            if (next instanceof Point) {
                final Point pt = (Point) next;
                if (pt.isEmpty()) {
                    lineTo = false;
                } else {
                    final double x = ((Point) next).getX();
                    final double y = ((Point) next).getY();
                    if (lineTo) {
                        path.lineTo(x, y);
                    } else {
                        path.startPath(x, y);
                        lineTo = true;
                    }
                }
            } else {
                path.add((MultiPath) next, false);
                lineTo = false;
            }
            /*
             * `polylines.hasNext()` check is conceptually part of `for` instruction,
             * except that we need to skip this condition during the first iteration.
             */
            do if (!polylines.hasNext()) break add;
            while ((next = (Geometry) polylines.next()) == null);
        }
        return path;
    }

    /**
     * Applies a filter predicate between this geometry and another geometry.
     * This method assumes that the two geometries are in the same CRS (this is not verified).
     *
     * <p><b>Note:</b> {@link SpatialOperatorName#BBOX} is implemented by {@code NOT DISJOINT}.
     * It is caller's responsibility to ensure that one of the geometries is rectangular,
     * for example by a call to {@link Geometry#getEnvelope()}.</p>
     *
     * @throws ClassCastException if the given wrapper is not for the same geometry library.
     */
    @Override
    protected boolean predicateSameCRS(final SpatialOperatorName type, final GeometryWrapper other) {
        final int ordinal = type.ordinal();
        if (ordinal >= 0 && ordinal < PREDICATES.length) {
            final Supplier<OperatorSimpleRelation> op = PREDICATES[ordinal];
            if (op != null) {
                return op.get().execute(geometry, ((Wrapper) other).geometry, srs(), null);
            }
        }
        return super.predicateSameCRS(type, other);
    }

    /**
     * All predicates recognized by {@link #predicateSameCRS(SpatialOperatorName, GeometryWrapper)}.
     * Array indices are {@link SpatialOperatorName#ordinal()} values.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static final Supplier<OperatorSimpleRelation>[] PREDICATES =
            new Supplier[SpatialOperatorName.OVERLAPS.ordinal() + 1];
    static {
        PREDICATES[SpatialOperatorName.EQUALS    .ordinal()] = com.esri.core.geometry.OperatorEquals    ::local;
        PREDICATES[SpatialOperatorName.DISJOINT  .ordinal()] = com.esri.core.geometry.OperatorDisjoint  ::local;
        PREDICATES[SpatialOperatorName.INTERSECTS.ordinal()] = com.esri.core.geometry.OperatorIntersects::local;
        PREDICATES[SpatialOperatorName.TOUCHES   .ordinal()] = com.esri.core.geometry.OperatorTouches   ::local;
        PREDICATES[SpatialOperatorName.CROSSES   .ordinal()] = com.esri.core.geometry.OperatorCrosses   ::local;
        PREDICATES[SpatialOperatorName.WITHIN    .ordinal()] = com.esri.core.geometry.OperatorWithin    ::local;
        PREDICATES[SpatialOperatorName.CONTAINS  .ordinal()] = com.esri.core.geometry.OperatorContains  ::local;
        PREDICATES[SpatialOperatorName.OVERLAPS  .ordinal()] = com.esri.core.geometry.OperatorOverlaps  ::local;
        PREDICATES[SpatialOperatorName.BBOX      .ordinal()] = new BBOX();
    }

    /** Implements {@code BBOX} operator as {@code NOT DISJOINT}. */
    private static final class BBOX extends OperatorSimpleRelation implements Supplier<OperatorSimpleRelation> {
        @Override public OperatorSimpleRelation get() {return this;}
        @Override public Operator.Type getType() {return Operator.Type.Intersects;}
        @Override public boolean execute(Geometry g1, Geometry g2, SpatialReference srs, ProgressTracker pt) {
            return !com.esri.core.geometry.OperatorDisjoint.local().execute(g1, g2, srs, pt);
        }
    }

    /**
     * Applies a SQLMM operation on this geometry.
     *
     * @param  operation  the SQLMM operation to apply.
     * @param  other      the other geometry, or {@code null} if the operation requires only one geometry.
     * @param  argument   an operation-specific argument, or {@code null} if not applicable.
     * @return result of the specified operation.
     * @throws ClassCastException if the argument is a geometry wrapper, but for a different geometry library.
     */
    @Override
    protected Object operationSameCRS(final SQLMM operation, final GeometryWrapper other, final Object argument) {
        final Geometry result;
        switch (operation) {
            case ST_Dimension:        return geometry.getDimension();
            case ST_CoordDim:         return geometry.hasZ() ? Geometries.TRIDIMENSIONAL : Geometries.BIDIMENSIONAL;
            case ST_GeometryType:     return geometry.getType().name();
            case ST_IsEmpty:          return geometry.isEmpty();
            case ST_Is3D:             return geometry.hasZ();
            case ST_IsMeasured:       return geometry.hasM();
            case ST_X:                return ((Point) geometry).getX();
            case ST_Y:                return ((Point) geometry).getY();
            case ST_Z:                return ((Point) geometry).getZ();
            case ST_Envelope:         return getEnvelope();
            case ST_Boundary:         result = geometry.getBoundary(); break;
            case ST_Simplify:         result = GeometryEngine.simplify             (geometry, srs()); break;
            case ST_ConvexHull:       result = GeometryEngine.convexHull           (geometry); break;
            case ST_Buffer:           result = GeometryEngine.buffer               (geometry, srs(), ((Number) argument).doubleValue()); break;
            case ST_Intersection:     result = GeometryEngine.intersect            (geometry, ((Wrapper) other).geometry,  srs()); break;
            case ST_Union:            result = GeometryEngine.union(new Geometry[] {geometry, ((Wrapper) other).geometry}, srs()); break;
            case ST_Difference:       result = GeometryEngine.difference           (geometry, ((Wrapper) other).geometry,  srs()); break;
            case ST_SymDifference:    result = GeometryEngine.symmetricDifference  (geometry, ((Wrapper) other).geometry,  srs()); break;
            case ST_Distance:         return   GeometryEngine.distance             (geometry, ((Wrapper) other).geometry,  srs());
            case ST_Equals:           return   GeometryEngine.equals               (geometry, ((Wrapper) other).geometry,  srs());
            case ST_Disjoint:         return   GeometryEngine.disjoint             (geometry, ((Wrapper) other).geometry,  srs());
            case ST_Touches:          return   GeometryEngine.touches              (geometry, ((Wrapper) other).geometry,  srs());
            case ST_Crosses:          return   GeometryEngine.crosses              (geometry, ((Wrapper) other).geometry,  srs());
            case ST_Within:           return   GeometryEngine.within               (geometry, ((Wrapper) other).geometry,  srs());
            case ST_Contains:         return   GeometryEngine.contains             (geometry, ((Wrapper) other).geometry,  srs());
            case ST_Overlaps:         return   GeometryEngine.overlaps             (geometry, ((Wrapper) other).geometry,  srs());
            case ST_AsText:           return   GeometryEngine.geometryToWkt        (geometry, WktExportFlags.wktExportDefaults);
            case ST_GeomFromText:     return   GeometryEngine.geometryFromWkt((String) argument, WktImportFlags.wktImportDefaults, Geometry.Type.Unknown);
            case ST_PointFromText:    return   GeometryEngine.geometryFromWkt((String) argument, WktImportFlags.wktImportDefaults, Geometry.Type.Point);
            case ST_MPointFromText:   return   GeometryEngine.geometryFromWkt((String) argument, WktImportFlags.wktImportDefaults, Geometry.Type.MultiPoint);
            case ST_LineFromText:     return   GeometryEngine.geometryFromWkt((String) argument, WktImportFlags.wktImportDefaults, Geometry.Type.Line);
            case ST_PolyFromText:     return   GeometryEngine.geometryFromWkt((String) argument, WktImportFlags.wktImportDefaults, Geometry.Type.Polygon);
            case ST_Intersects:       return OperatorIntersects.local().execute(geometry, ((Wrapper) other).geometry, srs(), null);
            case ST_Centroid:         result = new Point(OperatorCentroid2D.local().execute(geometry, null)); break;
            default:                  return super.operationSameCRS(operation, other, argument);
        }
        // Current version does not have metadata to copy, but it may be added in the future.
        return result;
    }

    /**
     * Returns the spatial reference system of this geometrY.
     * This is currently only a placeholder for future development.
     */
    private static SpatialReference srs() {
        return null;
    }

    /**
     * Returns the WKT representation of the wrapped geometry.
     */
    @Override
    public String formatWKT(final double flatness) {
        return OperatorExportToWkt.local().execute(WktExportFlags.wktExportDefaults, geometry, null);
    }
}
