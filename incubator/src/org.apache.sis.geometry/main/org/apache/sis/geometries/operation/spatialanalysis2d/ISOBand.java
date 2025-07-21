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
package org.apache.sis.geometries.operation.spatialanalysis2d;

import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MeshPrimitiveVisitor;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Triangle;
import static org.apache.sis.geometries.operation.spatialanalysis2d.ISOLine.interpolateToArray;
import org.apache.sis.geometries.math.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.util.FactoryException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @author Matthieu Bastianelli (Geomatys)
 */
public final class ISOBand {

    //force packed coordinates, use less memory space
    private static final GeometryFactory GF = new GeometryFactory(new PackedCoordinateSequenceFactory());
    private static final int Z = 2;

    private ISOBand(){}

    /**
     * <a href="https://en.wikipedia.org/wiki/Marching_squares#Contouring_triangle_meshes">...</a>
     */
    public static Map<NumberRange<Double>,List<Polygon>> create(MeshPrimitive.Triangles triangles, double[] steps) throws FactoryException {
        return create(triangles, steps, IsoInclusion.MIN);
    }
    public static Map<NumberRange<Double>,List<Polygon>> create(MeshPrimitive.Triangles triangles, double[] steps, final IsoInclusion isoInclusion) throws FactoryException {
        ArgumentChecks.ensureNonNull("Triangles", triangles);
        ArgumentChecks.ensureNonNull("Band steps", steps);
        ArgumentChecks.ensureNonNull("ISOBand Strategy", isoInclusion);

        final Map<NumberRange<Double>, List<Polygon>> levels = new HashMap<>();

        for (int i = 0, n = steps.length-1; i < n; i++) {
            double s0 = steps[i];
            double s1 = steps[i+1];
            if (s0 > s1) {
                s0 = steps[i+1];
                s1 = steps[i];
            } else if(s0 == s1) {
                throw new IllegalArgumentException("Values at index " + i +" and " + (i+1) + " are identical = " + s0);
            }
            levels.put(NumberRange.create(s0, true, s1, true), new ArrayList<>());
        }

        final MeshPrimitiveVisitor visitor = new MeshPrimitiveVisitor(triangles) {
            @Override
            protected void visit(Triangle candidate) {
                final PointSequence points = candidate.getExteriorRing().getPoints();
                Tuple t0 = points.getPosition(0);
                Tuple t1 = points.getPosition(1);
                Tuple t2 = points.getPosition(2);
                //sort points by Z, the point ordering to not matter for the algo
                if (t0.get(Z) < t1.get(Z)) {
                    Tuple t = t0;
                    t0 = t1;
                    t1 = t;
                }
                if (t1.get(Z) < t2.get(Z)) {
                    Tuple t = t1;
                    t1 = t2;
                    t2 = t;
                    if (t0.get(Z) < t1.get(Z)) {
                        t = t0;
                        t0 = t1;
                        t1 = t;
                    }
                }

                final double[] p0 = t0.toArrayDouble();
                final double[] p1 = t1.toArrayDouble();
                final double[] p2 = t2.toArrayDouble();

                for (NumberRange range : levels.keySet()) {
                    final double min = range.getMinDouble(true);
                    final double max = range.getMaxDouble(true);
                    final List<Polygon> polys = levels.get(range);
                    computeStepBand(min, max, p0, p1, p2, polys, isoInclusion);

                }
            }

            @Override
            protected void visit(MeshPrimitive.Vertex vertex) {
            }
        };
        visitor.visit();

        //merge polygons of same range
        for (Map.Entry<NumberRange<Double>,List<Polygon>> entry : levels.entrySet()) {
            List<Polygon> polys = entry.getValue();
            Geometry mp = GF.createMultiPolygon(polys.toArray(Polygon[]::new));
            mp = cleanAndUnion(mp);
            if (mp.isEmpty()) {
                polys.clear();
            } else if (mp instanceof GeometryCollection gc) {
                polys.clear();
                for (int i = 0; i < gc.getNumGeometries(); i++) {
                    polys.add((Polygon) gc.getGeometryN(i));
                }
            } else if (mp instanceof Polygon p) {
                polys.clear();
                polys.add(p);
            }

            if (mp.isEmpty()) {
                polys.clear();
            }
        }

        return levels;
    }

    /**
     * same as {@link #computeStepBand(double, double, double[], double[], double[], List, IsoInclusion)} with default
     * {@link IsoInclusion#MIN}
     */
    static void computeStepBand(double min, double max, double[] p0, double[] p1, double[] p2, List<Polygon> toFill) {
        computeStepBand(min, max, p0, p1, p2, toFill, IsoInclusion.MIN);
    }

    /**
     * Compute intersecting polygon for a step of {@link #Z} between min (inclusive) and max (inclusive)
     * @param min : assessed step's min value (inclusive)
     * @param max : assessed step's max value (inclusive)
     * @param p0, p1, p2 : not null double array of length 3 representing coordinates of an assessed triangle; It is expected
     *            those vertices to be ordered by the {@link #Z} axis such as p2[Z] <= p1[Z] <= p0[Z]
     * @param toFill : not null list of polygon to fill with the intersecting polygon.
     */
    static void computeStepBand(double min, double max, double[] p0, double[] p1, double[] p2, final List<Polygon> toFill, final IsoInclusion isoInclusion) {
        final double z0 = p0[Z];
        final double z1 = p1[Z];
        final double z2 = p2[Z];

        switch (isoInclusion) {
            case MAX: {
                if ((z0 == min) && (z2 == min)) return;
                break;
            }
            case MIN: {
                if ((z0 == max) && (z2 == max)) return;
                break;
            }
        }

        final boolean p0a = z0 >= max;
        final boolean p1a = z1 >= max;
        final boolean strictP2a = z2 > max;
        final boolean strictP0u = z0 < min;
        if (strictP2a || strictP0u) { // lower point (or highest) point is strictly above (or under) than the of band
            return;
        }
        final boolean p1u = z1 <= min;
        final boolean p2u = z2 <= min;
        final boolean p0i = z0 >= min && z0 <= max;
        final boolean p1i = z1 >= min && z1 <= max;
        final boolean p2i = z2 >= min && z2 <= max;


        //cas avec 3 points dedans
        if (p0i && p2i) {
            final double[] coords = new double[]{
                p0[0], p0[1], p0[2],
                p1[0], p1[1], p1[2],
                p2[0], p2[1], p2[2],
                p0[0], p0[1], p0[2]
            };
            toFill.add(GF.createPolygon(GF.createLinearRing(new PackedCoordinateSequence.Double(coords, 3, 0))));
        }

        //cas avec 1 point dedans et 2 points en dessous
        else if (p0i && p1u && p2u) {
            //if p0[Z] == min the result is a point, skip it
            if (z0 != min) {
                final double[] i1 = interpolateToArray(p1, p0, min);
                final double[] i2 = interpolateToArray(p2, p0, min);
                final double[] coords = new double[]{
                    p0[0], p0[1], p0[2],
                    i1[0], i1[1], i1[2],
                    i2[0], i2[1], i2[2],
                    p0[0], p0[1], p0[2]
                };
                toFill.add(GF.createPolygon(GF.createLinearRing(new PackedCoordinateSequence.Double(coords, 3, 0))));
            }
        }
        //cas avec 1 point dedans et 2 points au dessus
        else if (p0a && p1a && p2i) {
            //if p2[Z] == max the result is a point, skip it
            if (z2 != max) {
                final double[] i1 = interpolateToArray(p2, p1, max);
                final double[] i2 = interpolateToArray(p2, p0, max);
                final double[] coords = new double[]{
                    p2[0], p2[1], p2[2],
                    i1[0], i1[1], i1[2],
                    i2[0], i2[1], i2[2],
                    p2[0], p2[1], p2[2]
                };
                toFill.add(GF.createPolygon(GF.createLinearRing(new PackedCoordinateSequence.Double(coords, 3, 0))));
            }
        }

        //cas avec 2 points dedans et 1 point en dessous
        else if (p0i && p1i && p2u) {
            final double[] i1 = interpolateToArray(p2, p1, min);
            final double[] i2 = interpolateToArray(p2, p0, min);
            final double[] coords = new double[]{
                p0[0], p0[1], p0[2],
                p1[0], p1[1], p1[2],
                i1[0], i1[1], i1[2],
                i2[0], i2[1], i2[2],
                p0[0], p0[1], p0[2]
            };
            toFill.add(GF.createPolygon(GF.createLinearRing(new PackedCoordinateSequence.Double(coords, 3, 0))));
        }
        //cas avec 2 points dedans et 1 point au dessus
        else if (p0a && p1i && p2i) {
            final double[] i1 = interpolateToArray(p2, p0, max);
            final double[] i2 = interpolateToArray(p1, p0, max);
            final double[] coords = new double[]{
                p1[0], p1[1], p1[2],
                p2[0], p2[1], p2[2],
                i1[0], i1[1], i1[2],
                i2[0], i2[1], i2[2],
                p1[0], p1[1], p1[2]
            };
            toFill.add(GF.createPolygon(GF.createLinearRing(new PackedCoordinateSequence.Double(coords, 3, 0))));
        }

        //cas avec 1 point au dessus et 2 points en dessous
        else if (p0a && p1u && p2u) {
            final double[] se = interpolateToArray(p1, p0, max);
            final double[] i1 = interpolateToArray(p1, p0, min);
            final double[] i2 = interpolateToArray(p2, p0, min);
            final double[] i3 = interpolateToArray(p2, p0, max);
            final double[] coords = new double[]{
                se[0], se[1], se[2],
                i1[0], i1[1], i1[2],
                i2[0], i2[1], i2[2],
                i3[0], i3[1], i3[2],
                se[0], se[1], se[2]
            };
            toFill.add(GF.createPolygon(GF.createLinearRing(new PackedCoordinateSequence.Double(coords, 3, 0))));
        }
        //cas avec 2 points au dessus et 1 point en dessous
        else if (p0a && p1a && p2u) {
            final double[] se = interpolateToArray(p2, p0, min);
            final double[] i1 = interpolateToArray(p2, p0, max);
            final double[] i2 = interpolateToArray(p2, p1, max);
            final double[] i3 = interpolateToArray(p2, p1, min);
            final double[] coords = new double[]{
                se[0], se[1], se[2],
                i1[0], i1[1], i1[2],
                i2[0], i2[1], i2[2],
                i3[0], i3[1], i3[2],
                se[0], se[1], se[2]
            };
            toFill.add(GF.createPolygon(GF.createLinearRing(new PackedCoordinateSequence.Double(coords, 3, 0))));
        }
        //cas avec 1 point au dessus, 1 point dedans et 1 point en dessous
        else if (p0a && p1i && p2u) {
            final double[] se = interpolateToArray(p1, p0, max);
            final double[] i1 = interpolateToArray(p2, p1, min);
            final double[] i2 = interpolateToArray(p2, p0, min);
            final double[] i3 = interpolateToArray(p2, p0, max);
            final double[] coords = new double[]{
                se[0], se[1], se[2],
                p1[0], p1[1], p1[2],
                i1[0], i1[1], i1[2],
                i2[0], i2[1], i2[2],
                i3[0], i3[1], i3[2],
                se[0], se[1], se[2]
            };
            toFill.add(GF.createPolygon(GF.createLinearRing(new PackedCoordinateSequence.Double(coords, 3, 0))));
        }
    }

    private static Geometry cleanAndUnion(Geometry geom) {
        try {
            geom = geom.union();
        } catch (TopologyException ex) {
            try {
                geom = geom.buffer(0);
                geom = geom.union();
            } catch (TopologyException e) {
                //we have try
            }
        }

        return geom;
    }

}
