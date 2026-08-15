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
package org.apache.sis.geometries.polyhedron;

import org.apache.sis.geometries.Polygon;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.math.ArrayFactoryJava;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.ReadOnly;


/**
 *
 * @see https://mathworld.wolfram.com/RegularDodecahedron.html
 * @author Johann Sorel (Geomatys)
 */
public final class Dodecahedron extends AbstractPolyhedron{

    /**
     * Vertices are the dual of {@link Icosahedron} (each icosahedron face
     * centroid, normalized), so both solids share a consistent orientation.
     */
    private static final Array VERTICES = ArrayFactoryJava.INSTANCE.builder()
        .dataType(DataType.DOUBLE).values(new ReadOnly.Vector<?>[]{
        fromLatLon(     ATAN_PHI2,                         0),
        fromLatLon(     ATAN_PHI2,                   Math.PI),
        fromLatLon(      CUBE_LAT,                 Math.PI/4),
        fromLatLon(      CUBE_LAT,               3*Math.PI/4),
        fromLatLon( ATAN_INV_PHI2,                 Math.PI/2),
        fromLatLon(    -ATAN_PHI2,                         0),
        fromLatLon(    -ATAN_PHI2,                   Math.PI),
        fromLatLon(     -CUBE_LAT,                 Math.PI/4),
        fromLatLon(     -CUBE_LAT,               3*Math.PI/4),
        fromLatLon(-ATAN_INV_PHI2,                 Math.PI/2),
        fromLatLon(      CUBE_LAT,                -Math.PI/4),
        fromLatLon(      CUBE_LAT,              -3*Math.PI/4),
        fromLatLon( ATAN_INV_PHI2,                -Math.PI/2),
        fromLatLon(     -CUBE_LAT,                -Math.PI/4),
        fromLatLon(     -CUBE_LAT,              -3*Math.PI/4),
        fromLatLon(-ATAN_INV_PHI2,                -Math.PI/2),
        fromLatLon(             0,             ATAN_INV_PHI2),
        fromLatLon(             0,            -ATAN_INV_PHI2),
        fromLatLon(             0,   Math.PI - ATAN_INV_PHI2),
        fromLatLon(             0, -(Math.PI - ATAN_INV_PHI2))
    }, true).build();

    private static final int[][] FACES = {
        { 3,  1,  0,  2,  4,  3},
        {12, 10,  0,  1, 11, 12},
        {16,  2,  0, 10, 17, 16},
        {19, 11,  1,  3, 18, 19},
        { 9,  4,  2, 16,  7,  9},
        { 8, 18,  3,  4,  9,  8},
        { 9,  7,  5,  6,  8,  9},
        {14,  6,  5, 13, 15, 14},
        {17, 13,  5,  7, 16, 17},
        {18,  8,  6, 14, 19, 18},
        {13, 17, 10, 12, 15, 13},
        {15, 12, 11, 19, 14, 15}
    };

    @Override
    public int getFaceCount() {
        return FACES.length;
    }

    @Override
    public Polygon getFace(int index) {
        return toPolygon(VERTICES, FACES[index]);
    }

    @Override
    public int getFace(ReadOnly.Vector<?> unitVector) {
        return nearestFace(VERTICES, FACES, unitVector);
    }

}
