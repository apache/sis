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
package org.apache.sis.geometry.wrapper.j2d;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Array;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.IllegalPathStateException;
import org.apache.sis.referencing.internal.shared.AbstractShape;
import org.apache.sis.util.StringBuilders;


/**
 * Provides some information about a {@link Shape} object.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ShapeProperties {
    /**
     * The geometry.
     */
    private final Shape geometry;

    /**
     * {@code true} if last call to {@link #coordinates(double)} found only closed shapes.
     * In such case, the shapes are presumed polygons. Note that we do not verify if some
     * polygons are actually holes inside other polygons.
     */
    private boolean isPolygon;

    /**
     * Creates a new inspector for the given geometry.
     *
     * @param  geometry  the shape for which to inspect properties.
     */
    public ShapeProperties(final Shape geometry) {
        this.geometry = geometry;
    }

    /**
     * Appends the two first coordinates of {@code source} at the end of {@code target}, expanding the target array if necessary.
     *
     * @param  source  array of coordinates to add. Only the two first values are used.
     * @param  target  where to add the two coordinates.
     * @param  index   index in {@code target} where to add the two coordinates.
     * @return {@code target}, possible as a new array if it was necessary to expand it.
     */
    private static double[] addPoint(final double[] source, double[] target, final int index) {
        if (index >= target.length) {
            target = Arrays.copyOf(target, index*2);
        }
        System.arraycopy(source, 0, target, index, 2);
        return target;
    }

    /**
     * Same as {@link #addPoint(double[], double[], int)} but for single-precision numbers.
     */
    private static float[] addPoint(final float[] source, float[] target, final int index) {
        if (index >= target.length) {
            target = Arrays.copyOf(target, index*2);
        }
        System.arraycopy(source, 0, target, index, 2);
        return target;
    }

    /**
     * Returns coordinates of the given geometry as a list of (<var>x</var>,<var>y</var>) tuples in {@code float[]}
     * or {@code double[]} arrays. This method guarantees that all arrays have at least 2 points (4 coordinates).
     * It should be invoked only for small or medium shapes. For large shapes, the path iterator should be used
     * directly without copy to arrays.
     *
     * @param  flatness   maximal distance between the approximated segments and any point on the curve.
     * @return coordinate tuples. They are presumed polygons if {@link #isPolygon} is {@code true}.
     */
    private List<?> coordinates(final double flatness) {
        final PathIterator it = geometry.getPathIterator(null, flatness);
        isPolygon = true;
        if (AbstractShape.isFloat(geometry)) {
            return coordinatesAsFloats(it);
        } else {
            return coordinatesAsDoubles(it);
        }
    }

    /**
     * Returns coordinates of the given geometry as a list of (<var>x</var>,<var>y</var>) tuples
     * in {@code double[]} arrays. This method guarantees that all arrays have at least 2 points (4 coordinates).
     * It should be invoked only for small or medium shapes. For large shapes, the path iterator should be used
     * directly without copy to arrays.
     *
     * @return coordinate tuples as (<var>x</var>,<var>y</var>) tuples.
     * @throws IllegalPathStateException if the given path iterator contains curves.
     */
    public List<double[]> coordinatesAsDoubles() {
        isPolygon = true;
        return coordinatesAsDoubles(geometry.getPathIterator(null));
    }

    /**
     * {@link #coordinates(double)} implementation for the double-precision case.
     * The {@link #isPolygon} field needs to be set before to invoke this method.
     *
     * @param  it  path iterator of the geometry for which to get the coordinate tuples.
     * @return coordinate tuples as (<var>x</var>,<var>y</var>) tuples.
     * @throws IllegalPathStateException if the given path iterator contains curves.
     */
    private List<double[]> coordinatesAsDoubles(final PathIterator it) {
        final List<double[]> polylines = new ArrayList<>();
        double[] polyline = new double[10];
        final double[] coords = new double[6];
        /*
         * Double-precision variant of this method. Source code below is identical to the single-precision variant,
         * but the methods invoked are different because of method overloading. Trying to have a common code is too
         * complex (too many code are different despite looking the same).
         */
        int i = 0;
        while (!it.isDone()) {
            switch (it.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO: {
                    if (i > 2) {
                        isPolygon = false;          // MOVETO without CLOSE: this is a linestring instead of a polygon.
                        polylines.add(Arrays.copyOf(polyline, i));
                    }
                    System.arraycopy(coords, 0, polyline, 0, 2);
                    i = 2;
                    break;
                }
                case PathIterator.SEG_LINETO: {
                    polyline = addPoint(coords, polyline, i);
                    i += 2;
                    break;
                }
                case PathIterator.SEG_CLOSE: {
                    if (i > 2) {
                        if (polyline[0] != polyline[i-2] || polyline[1] != polyline[i-1]) {
                            polyline = addPoint(polyline, polyline, i);
                            i += 2;
                        }
                        polylines.add(Arrays.copyOf(polyline, i));
                    }
                    i = 0;
                    break;
                }
                default: throw new IllegalPathStateException();
            }
            it.next();
        }
        if (i > 2) {
            isPolygon = false;          // LINETO without CLOSE: this is a linestring instead of a polygon.
            polylines.add(Arrays.copyOf(polyline, i));
        }
        return polylines;
    }

    /**
     * {@link #coordinates(double)} implementation for the single-precision case.
     * The {@link #isPolygon} field needs to be set before to invoke this method.
     *
     * @param  it  path iterator of the geometry for which to get the coordinate tuples.
     * @return coordinate tuples as (<var>x</var>,<var>y</var>) tuples.
     * @throws IllegalPathStateException if the given path iterator contains curves.
     */
    private List<float[]> coordinatesAsFloats(final PathIterator it) {
        final List<float[]> polylines = new ArrayList<>();
        float[] polyline = new float[10];
        final float[] coords = new float[6];
        /*
         * Single-precision variant of this method. Source code below is identical to the double-precision variant,
         * but the methods invoked are different because of method overloading. Trying to have a common code is too
         * complex (too many code are different despite looking the same).
         */
        int i = 0;
        while (!it.isDone()) {
            switch (it.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO: {
                    if (i > 2) {
                        isPolygon = false;          // MOVETO without CLOSE: this is a linestring instead of a polygon.
                        polylines.add(Arrays.copyOf(polyline, i));
                    }
                    System.arraycopy(coords, 0, polyline, 0, 2);
                    i = 2;
                    break;
                }
                case PathIterator.SEG_LINETO: {
                    polyline = addPoint(coords, polyline, i);
                    i += 2;
                    break;
                }
                case PathIterator.SEG_CLOSE: {
                    if (i > 2) {
                        if (polyline[0] != polyline[i-2] || polyline[1] != polyline[i-1]) {
                            polyline = addPoint(polyline, polyline, i);
                            i += 2;
                        }
                        polylines.add(Arrays.copyOf(polyline, i));
                    }
                    i = 0;
                    break;
                }
                default: throw new IllegalPathStateException();
            }
            it.next();
        }
        if (i > 2) {
            isPolygon = false;          // LINETO without CLOSE: this is a linestring instead of a polygon.
            polylines.add(Arrays.copyOf(polyline, i));
        }
        return polylines;
    }

    /**
     * Returns a WKT representation of the geometry.
     * Current implementation assumes that all closed shapes are polygons and that polygons have no hole
     * (i.e. if a polygon is followed by more data, this method assumes that the additional data is a disjoint polygon).
     *
     * @param  flatness   maximal distance between the approximated segments and any point on the curve.
     * @return Well Known Text representation of the geometry.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry">Well-known text on Wikipedia</a>
     */
    public String toWKT(final double flatness) {
        final List<?> polylines = coordinates(flatness);
        final boolean isMulti;
        switch (polylines.size()) {
            case 0:  return "POLYGON EMPTY";
            case 1:  isMulti = false; break;
            default: isMulti = true;  break;
        }
        final StringBuilder buffer = new StringBuilder(80);
        if (isMulti) {
            buffer.append("MULTI");
        }
        buffer.append(isPolygon ? "POLYGON" : "LINESTRING").append(' ');
        if (isMulti) buffer.append('(');
        for (int j=0; j<polylines.size(); j++) {
            final Object polyline = polylines.get(j);
            if (j != 0) buffer.append(", ");
            buffer.append('(');
            if (isPolygon) buffer.append('(');
            final int length = Array.getLength(polyline);
            for (int i=0; i<length; i++) {
                if (i != 0) {
                    if ((i & 1) == 0) buffer.append(',');
                    buffer.append(' ');
                }
                if (polyline instanceof double[]) {
                    buffer.append(((double[]) polyline)[i]);
                } else {
                    buffer.append(((float[]) polyline)[i]);
                }
                StringBuilders.trimFractionalPart(buffer);
            }
            if (isPolygon) buffer.append(')');
            buffer.append(')');
        }
        if (isMulti) buffer.append(')');
        return buffer.toString();
    }
}
