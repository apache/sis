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
package org.apache.sis.internal.feature.j2d;

import java.util.List;
import java.util.Iterator;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWithCRS;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.internal.filter.sqlmm.SQLMM;
import org.apache.sis.internal.referencing.j2d.ShapeUtilities;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Debug;


/**
 * The wrapper of Java2D geometries.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class Wrapper extends GeometryWithCRS<Shape> {
    /**
     * The wrapped implementation.
     */
    private final Shape geometry;

    /**
     * Creates a new wrapper around the given geometry.
     */
    Wrapper(final Shape geometry) {
        this.geometry = geometry;
    }

    /**
     * Returns the implementation-dependent factory of geometric object.
     */
    @Override
    public Geometries<Shape> factory() {
        return Factory.INSTANCE;
    }

    /**
     * Returns the geometry specified at construction time.
     */
    @Override
    public Object implementation() {
        return geometry;
    }

    /**
     * Returns the Java2D envelope as an Apache SIS implementation.
     *
     * @return the envelope of the geometry.
     */
    @Override
    public GeneralEnvelope getEnvelope() {
        final Rectangle2D bounds = geometry.getBounds2D();
        final GeneralEnvelope env = createEnvelope();
        env.setRange(0, bounds.getMinX(), bounds.getMaxX());
        env.setRange(1, bounds.getMinY(), bounds.getMaxY());
        return env;
    }

    /**
     * Returns the centroid of the wrapped geometry as a direct position.
     */
    @Override
    public DirectPosition getCentroid() {
        final RectangularShape frame = (geometry instanceof RectangularShape)
                        ? (RectangularShape) geometry : geometry.getBounds2D();
        return new DirectPosition2D(getCoordinateReferenceSystem(), frame.getCenterX(), frame.getCenterY());
    }

    /**
     * Returns {@code null} since {@link Shape} are never points in Java2D API.
     */
    @Override
    public double[] getPointCoordinates() {
        return null;
    }

    /**
     * Returns all coordinate tuples in the wrapped geometry.
     * This method is currently used for testing purpose only.
     */
    @Debug
    @Override
    public double[] getAllCoordinates() {
        final List<double[]> coordinates = new ShapeProperties(geometry).coordinatesAsDoubles();
        switch (coordinates.size()) {
            case 0:  return ArraysExt.EMPTY_DOUBLE;
            case 1:  return coordinates.get(0);
            default: {
                /*
                 * Concatenate the coordinates of all polygons in a single array. We lost the distinction
                 * between the different polygons, which is why this method should not be used except for
                 * testing.
                 */
                final double[] tgt = new double[coordinates.stream().mapToInt((a) -> a.length).sum()];
                int p = 0;
                for (final double[] src : coordinates) {
                    System.arraycopy(src, 0, tgt, p, src.length);
                    p += src.length;
                }
                return tgt;
            }
        }
    }

    /**
     * Merges a sequence of points or paths after this geometry.
     *
     * @throws ClassCastException if an element in the iterator is not a {@link Shape} or a {@link Point2D}.
     */
    @Override
    protected Shape mergePolylines(final Iterator<?> polylines) {
        return mergePolylines(geometry, polylines);
    }

    /**
     * Implementation of {@link #mergePolylines(Iterator)} also shared by {@link PointWrapper}.
     */
    static Shape mergePolylines(Object next, final Iterator<?> polylines) {
        boolean isFloat = ShapeUtilities.isFloat(next);
        Path2D path = isFloat ? new Path2D.Float() : new Path2D.Double();
        boolean lineTo = false;
add:    for (;;) {
            if (next instanceof Point2D) {
                final double x = ((Point2D) next).getX();
                final double y = ((Point2D) next).getY();
                if (Double.isNaN(x) || Double.isNaN(y)) {
                    lineTo = false;
                } else if (lineTo) {
                    path.lineTo(x, y);
                } else {
                    path.moveTo(x, y);
                    lineTo = true;
                }
            } else {
                path.append((Shape) next, false);
                lineTo = false;
            }
            /*
             * 'polylines.hasNext()' check is conceptually part of 'for' instruction,
             * except that we need to skip this condition during the first iteration.
             */
            do if (!polylines.hasNext()) break add;
            while ((next = polylines.next()) == null);
            /*
             * Convert the path from single-precision to double-precision if needed.
             */
            if (isFloat && !ShapeUtilities.isFloat(next)) {
                path = new Path2D.Double(path);
                isFloat = false;
            }
        }
        return ShapeUtilities.toPrimitive(path);
    }

    /**
     * Applies a SQLMM operation on this geometry.
     *
     * @param  operation  the SQLMM operation to apply.
     * @param  other      the other geometry, or {@code null} if the operation requires only one geometry.
     * @param  argument   an operation-specific argument, or {@code null} if not applicable.
     * @return result of the specified operation.
     */
    @Override
    protected Object operationSameCRS(final SQLMM operation, final GeometryWrapper<Shape> other, final Object argument) {
        switch (operation) {
            case ST_Centroid: {
                final RectangularShape frame = (geometry instanceof RectangularShape)
                                ? (RectangularShape) geometry : geometry.getBounds2D();
                return new Point2D.Double(frame.getCenterX(), frame.getCenterY());
            }
            default: return super.operationSameCRS(operation, other, argument);
        }
    }

    /**
     * Builds a WKT representation of the wrapped shape.
     * Current implementation assumes that all closed shapes are polygons and that polygons have no hole
     * (i.e. if a polygon is followed by more data, this method assumes that the additional data is a disjoint polygon).
     *
     * @see <a href="https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry">Well-known text on Wikipedia</a>
     */
    @Override
    public String formatWKT(final double flatness) {
        return new ShapeProperties(geometry).toWKT(flatness);
    }
}
