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
package org.apache.sis.internal.feature;

import java.util.Iterator;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.internal.referencing.j2d.ShapeUtilities;
import org.apache.sis.math.Vector;


/**
 * Centralizes usages of some (not all) Java2D geometry API by Apache SIS.
 * We use this class for isolating dependencies from the {@code org.apache.feature} package
 * to ESRI's API or to Java Topology Suite (JTS) API.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 */
final class Java2D extends Geometries {
    /**
     * Creates the singleton instance.
     */
    Java2D() {
        super(GeometryLibrary.JAVA2D, Shape.class, Point2D.class, Shape.class, Shape.class);
    }

    /**
     * If the given object is a Java2D geometry and its envelope is non-empty, returns
     * that envelope as an Apache SIS implementation. Otherwise returns {@code null}.
     *
     * @param  geometry  the geometry from which to get the envelope, or {@code null}.
     * @return the envelope of the given object, or {@code null} if the object is not
     *         a recognized geometry or its envelope is empty.
     */
    @Override
    final GeneralEnvelope tryGetEnvelope(final Object geometry) {
        if (geometry instanceof Shape) {
            final Rectangle2D bounds = ((Shape) geometry).getBounds2D();
            if (!bounds.isEmpty()) {                                     // Test if there is NaN values.
                final GeneralEnvelope env = new GeneralEnvelope(2);
                env.setRange(0, bounds.getMinX(), bounds.getMaxX());
                env.setRange(1, bounds.getMinY(), bounds.getMaxY());
                return env;
            }
        }
        return null;
    }

    /**
     * If the given point is an implementation of this library, returns its coordinate.
     * Otherwise returns {@code null}.
     */
    @Override
    final double[] tryGetCoordinate(final Object point) {
        if (point instanceof Point2D) {
            final Point2D pt = (Point2D) point;
            return new double[] {
                pt.getX(),
                pt.getY()
            };
        }
        return null;
    }

    /**
     * Creates a two-dimensional point from the given coordinate.
     */
    @Override
    public Object createPoint(double x, double y) {
        return new Point2D.Double(x, y);
    }

    /**
     * Returns {@code true} if all values in the given vector can be casted to {@code float} without precision lost.
     *
     * @param  data  the data to test.
     * @return whether all the given data can be casted to {@code float} type.
     */
    private static boolean isConvertibleToFloats(final Vector data) {
        for (int i=data.size(); --i >= 0;) {
            final double value = data.doubleValue(i);
            if (Double.doubleToRawLongBits(value) != Double.doubleToRawLongBits((float) value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a path from the given ordinate values.
     * Each {@link Double#NaN} ordinate value start a new path.
     * The implementation returned by this method must be an instance of {@link #rootClass}.
     */
    @Override
    public Object createPolyline(final int dimension, final Vector ordinates) {
        if (dimension != 2) {
            throw unsupported(dimension);
        }
        final boolean isFloat = isConvertibleToFloats(ordinates);
        final int size = ordinates.size();
        /*
         * Note: Point2D is not an instance of Shape, so we can not make a special case for it.
         */
        if (size == 4) {
            final double x1, y1, x2, y2;
            if (!Double.isNaN(x1 = ordinates.doubleValue(0)) &&
                !Double.isNaN(y1 = ordinates.doubleValue(1)) &&
                !Double.isNaN(x2 = ordinates.doubleValue(2)) &&
                !Double.isNaN(y2 = ordinates.doubleValue(3)))
            {
                final Line2D path = isFloat ? new Line2D.Float() : new Line2D.Double();
                path.setLine(x1, y1, x2, y2);
                return path;
            }
        }
        final Path2D path = isFloat ? new Path2D.Float (Path2D.WIND_NON_ZERO, size/2)
                                    : new Path2D.Double(Path2D.WIND_NON_ZERO, size/2);
        boolean lineTo = false;
        for (int i=0; i<size;) {
            final double x = ordinates.doubleValue(i++);
            final double y = ordinates.doubleValue(i++);
            if (Double.isNaN(x) || Double.isNaN(y)) {
                lineTo = false;
            } else if (lineTo) {
                path.lineTo(x, y);
            } else {
                path.moveTo(x, y);
                lineTo = true;
            }
        }
        return path;
    }

    /**
     * Merges a sequence of points or paths if the first instance is an implementation of this library.
     *
     * @throws ClassCastException if an element in the iterator is not a {@link Shape} or a {@link Point2D}.
     */
    @Override
    final Object tryMergePolylines(Object next, final Iterator<?> polylines) {
        if (!(next instanceof Shape || next instanceof Point2D)) {
            return null;
        }
        final Path2D path = new Path2D.Double();
        boolean lineTo = false;
        for (;; next = polylines.next()) {
            if (next != null) {
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
            }
            if (!polylines.hasNext()) {         // Should be part of the 'for' instruction, but we need
                break;                          // to skip this condition during the first iteration.
            }
        }
        return ShapeUtilities.toPrimitive(path);
    }
}
