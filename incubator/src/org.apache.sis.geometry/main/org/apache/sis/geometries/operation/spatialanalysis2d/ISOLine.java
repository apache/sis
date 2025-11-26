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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Triangle;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MeshPrimitiveVisitor;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.Vector3D;
import org.apache.sis.geometries.math.Vectors;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.geometries.math.Array;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ISOLine {

    private static final int Z = 2;

    private ISOLine(){}

    /**
     * https://en.wikipedia.org/wiki/Marching_squares#Contouring_triangle_meshes
     */
    public static MeshPrimitive.Lines createMesh(MeshPrimitive.Triangles triangles, double[] steps) throws FactoryException {

        final List<Vector3D.Double> segments = new ArrayList<>();
        final MeshPrimitiveVisitor visitor = new MeshPrimitiveVisitor(triangles) {
            @Override
            protected void visit(Triangle candidate) {
                final PointSequence points = candidate.getExteriorRing().getPoints();
                final double[] p0 = points.getPosition(0).toArrayDouble();
                final double[] p1 = points.getPosition(1).toArrayDouble();
                final double[] p2 = points.getPosition(2).toArrayDouble();

                for (double step : steps) {
                    final boolean p0a = p0[Z] >= step;
                    final boolean p1a = p1[Z] >= step;
                    final boolean p2a = p2[Z] >= step;
                    final boolean p0e = p0[Z] == step;
                    final boolean p1e = p1[Z] == step;
                    final boolean p2e = p2[Z] == step;

                    if (p1a && !p1e && !p2a && !p0a) {
                        // under : P0 P2  above : P1
                        segments.add(new Vector3D.Double(interpolateToArray(p0, p1, step)));
                        segments.add(new Vector3D.Double(interpolateToArray(p2, p1, step)));
                    } else if (p0a && !p0e && !p1a && !p2a) {
                        // under : P1 P2  above : P0
                        segments.add(new Vector3D.Double(interpolateToArray(p1, p0, step)));
                        segments.add(new Vector3D.Double(interpolateToArray(p2, p0, step)));
                    } else if (p2a && !p2e && !p0a && !p1a) {
                        // under : P0 P1  above : P2
                        segments.add(new Vector3D.Double(interpolateToArray(p0, p2, step)));
                        segments.add(new Vector3D.Double(interpolateToArray(p1, p2, step)));
                    } else if (p0a && !p1a && p2a) {
                        // under : P1  above : P0 P2
                        segments.add(new Vector3D.Double(interpolateToArray(p1, p0, step)));
                        segments.add(new Vector3D.Double(interpolateToArray(p1, p2, step)));
                    } else if (!p0a && p1a && p2a) {
                        // under : P0  above : P1 P2
                        segments.add(new Vector3D.Double(interpolateToArray(p0, p1, step)));
                        segments.add(new Vector3D.Double(interpolateToArray(p0, p2, step)));
                    } else if (p0a && p1a && !p2a) {
                        // under : P2  above : P0 P1
                        segments.add(new Vector3D.Double(interpolateToArray(p2, p0, step)));
                        segments.add(new Vector3D.Double(interpolateToArray(p2, p1, step)));
                    }
                }
            }

            @Override
            protected void visit(MeshPrimitive.Vertex vertex) {
            }
        };
        visitor.visit();

        final CoordinateReferenceSystem crs = CRS.compound(CommonCRS.WGS84.normalizedGeographic(), CommonCRS.Vertical.ELLIPSOIDAL.crs());

        final MeshPrimitive.Lines lines = new MeshPrimitive.Lines();
        Array position = NDArrays.of(SampleSystem.of(crs), DataType.DOUBLE, segments.size());
        Iterator<Vector3D.Double> iterator = segments.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            Vector3D.Double v3d = iterator.next();
            position.set(i, v3d);
            i++;
        }
        lines.setCoordinateReferenceSystem(crs);
        lines.setPositions(position);

        return lines;
    }

    static double[] interpolateToArray(double[] start, double[] end, double z) {
        final double startZ = start[Z];
        double ratio = end[Z] - startZ;
        if (ratio == 0) {
            if (z != startZ) throw new IllegalStateException("Start and end coordinates have the same value of "+Z+" interpolation to target value ("+startZ+") can't be performed.");
            return new double[]{start[0], start[1], z}; //flat line
        }
        ratio = (z - start[Z]) / ratio;
        if (ratio > 1 || ratio < 0) throw new IllegalStateException("Expected point of isoline out of the current triangulation segment");
        double[] res = Vectors.lerp(start, end, ratio);
        return new double[]{res[0], res[1], z};
    }

    static Coordinate interpolateToCoord2D(double[] start, double[] end, double z) {
        final double startZ = start[Z];
        double ratio = end[Z] - startZ;
        if (ratio == 0) {
            if (z != startZ) throw new IllegalStateException("Start and end coordinates have the same value of "+Z+" interpolation to target value ("+startZ+") can't be performed.");
            return new Coordinate(start[0], start[1], z); //flat line
        }
        ratio = (z - start[Z]) / ratio;
        if (ratio > 1 || ratio < 0) throw new IllegalStateException("Expected point of isoline out of the current triangulation segment");
        double[] res = Vectors.lerp(start, end, ratio);
        return new Coordinate(res[0], res[1], z);
    }
}
