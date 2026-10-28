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
package org.apache.sis.geometries.internal.shared;

import java.util.ArrayList;
import java.util.List;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;


/**
 * Parameterization shared by the spheres and the ellipsoids, whose axes are along the first three axes
 * of the coordinate system. The <var>u</var> parameter is the longitude and the <var>v</var> parameter
 * the latitude, both in radians:
 *
 * <blockquote><var>S</var>(<var>u</var>, <var>v</var>) = <var>c</var> +
 * (<var>a</var>⋅cos <var>u</var>⋅cos <var>v</var>,
 *  <var>b</var>⋅sin <var>u</var>⋅cos <var>v</var>,
 *  <var>c</var>⋅sin <var>v</var>)</blockquote>
 *
 * where <var>a</var>, <var>b</var> and <var>c</var> are the semi-axis lengths. The coordinates on the
 * axes after the third one are those of the center. The horizontal curves are the lines of constant
 * latitude and the vertical curves the lines of constant longitude, from the south pole to the north pole.
 *
 * @author Johann Sorel (Geomatys)
 */
final class EllipsoidalSurfaces {
    /**
     * Do not allow instantiation of this class.
     */
    private EllipsoidalSurfaces() {
    }

    /**
     * Returns the position at the given longitude and latitude.
     *
     * @param  center    center of the surface.
     * @param  semiAxes  semi-axis lengths along the first three axes.
     * @param  u         longitude in radians.
     * @param  v         latitude in radians.
     * @return position on the surface.
     * @throws IllegalStateException if the coordinate system has less than three dimensions.
     */
    static Vector<?> position(final Vector<?> center, final double[] semiAxes, final double u, final double v) {
        final int dimension = center.getDimension();
        if (dimension < 3) {
            throw new IllegalStateException("A sphere or an ellipsoid is a parametric surface in at least three"
                    + " dimensions, but the coordinate system has " + dimension + '.');
        }
        final double[] position = center.toArrayDouble();
        position[0] = center.get(0) + semiAxes[0] * Math.cos(u) * Math.cos(v);
        position[1] = center.get(1) + semiAxes[1] * Math.sin(u) * Math.cos(v);
        position[2] = center.get(2) + semiAxes[2] * Math.sin(v);
        return Vectors.create(position, DataType.DOUBLE);
    }

    /**
     * Returns the line of constant latitude: a circle if the first two semi-axes are equal,
     * an ellipse otherwise. At the poles, the line is reduced to a point, returned as a line string
     * of two identical positions.
     *
     * @param  crs       coordinate reference system of the surface.
     * @param  center    center of the surface.
     * @param  semiAxes  semi-axis lengths along the first three axes.
     * @param  v         latitude in radians.
     * @param  circle    whether to return a {@link org.apache.sis.geometries.curve.Circle} instead of an
     *                   {@link org.apache.sis.geometries.curve.EllipticArc}.
     * @return the line of constant latitude.
     */
    static Curve parallel(final CoordinateReferenceSystem crs, final Vector<?> center,
                          final double[] semiAxes, final double v, final boolean circle)
    {
        final GeometryFactory factory = GeometryFactory.DEFAULT;
        if (Math.abs(Math.cos(v)) < 1E-12) {
            final Vector<?> pole = position(center, semiAxes, 0, v);
            return factory.createLineString(factory.createDataPoints(array(crs, List.of(pole, pole))));
        }
        final double[] coordinates = center.toArrayDouble();
        coordinates[2] = center.get(2) + semiAxes[2] * Math.sin(v);
        final Vector<?> middle = Vectors.create(coordinates, DataType.DOUBLE);
        final var points  = new ArrayList<Vector<?>>();
        final var centers = new ArrayList<Vector<?>>();
        if (circle) {
            // Four quarters of circle, since an arc of a circle shall be less than a half turn.
            for (int i = 0; i <= 4; i++) {
                points.add(position(center, semiAxes, (i % 4) * Math.PI / 2, v));
                if (i < 4) centers.add(middle);
            }
            return factory.createCircle(factory.createDataPoints(array(crs, points)), array(crs, centers), null);
        }
        /*
         * Two halves of ellipse of four points each, the first point closing the curve. The longitudes are shifted
         * by π/12 because four points made of two pairs of points having the same coordinate along an axis of the
         * ellipse do not determine a single ellipse, which happens with shifts that are multiples of π/6.
         */
        for (int i = 0; i < 6; i++) {
            points.add(position(center, semiAxes, (2*i + 0.5) * Math.PI / 6, v));
        }
        return factory.createEllipticArc(factory.createDataPoints(array(crs, points)), array(crs, List.of(middle, middle)), true);
    }

    /**
     * Returns the line of constant longitude, from the south pole to the north pole: half a great circle
     * of a sphere, or half an ellipse of an ellipsoid.
     *
     * @param  crs       coordinate reference system of the surface.
     * @param  center    center of the surface.
     * @param  semiAxes  semi-axis lengths along the first three axes.
     * @param  u         longitude in radians.
     * @param  circle    whether to return an {@link org.apache.sis.geometries.curve.Arc} instead of an
     *                   {@link org.apache.sis.geometries.curve.EllipticArc}.
     * @return the line of constant longitude.
     */
    static Curve meridian(final CoordinateReferenceSystem crs, final Vector<?> center,
                          final double[] semiAxes, final double u, final boolean circle)
    {
        final GeometryFactory factory = GeometryFactory.DEFAULT;
        final Vector<?> c = center.copy(DataType.DOUBLE);
        if (circle) {
            // Two quarters of great circle, from the south pole to the equator then to the north pole.
            final List<Vector<?>> points = List.of(position(center, semiAxes, u, -Math.PI / 2),
                    position(center, semiAxes, u, 0), position(center, semiAxes, u, Math.PI / 2));
            return factory.createArc(factory.createDataPoints(array(crs, points)), array(crs, List.of(c, c)), null, false);
        }
        // Not symmetric around the equator, for the reason explained in `parallel(…)`.
        final List<Vector<?>> points = List.of(position(center, semiAxes, u, -Math.PI / 2),
                position(center, semiAxes, u, -Math.PI / 6), position(center, semiAxes, u, Math.PI / 4),
                position(center, semiAxes, u, Math.PI / 2));
        return factory.createEllipticArc(factory.createDataPoints(array(crs, points)), array(crs, List.of(c)), false);
    }

    /**
     * Returns the given positions as an array in the given reference system.
     */
    private static Array array(final CoordinateReferenceSystem crs, final List<? extends Vector<?>> positions) {
        return NDArrays.of(positions, SampleSystem.of(crs), DataType.DOUBLE);
    }
}
