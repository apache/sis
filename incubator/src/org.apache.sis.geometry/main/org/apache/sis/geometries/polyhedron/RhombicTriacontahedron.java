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
 * @see https://mathworld.wolfram.com/RhombicTriacontahedron.html
 * @author Johann Sorel (Geomatys)
 */
public final class RhombicTriacontahedron extends AbstractPolyhedron{

    /**
     * Scale applied to the 20 dodecahedron-direction vertices, so every face
     * is a planar golden rhombus.
     */
    private static final double INNER_RADIUS = 0.9105929973100293;

    /**
     * The first 12 vertices are the {@link Icosahedron} vertices (degree 5),
     * the last 20 are the {@link Dodecahedron} vertices (degree 3) scaled so
     * every face is a planar golden rhombus.
     */
    private static final Array VERTICES = ArrayFactoryJava.INSTANCE.builder()
        .dataType(DataType.DOUBLE).values(new ReadOnly.Vector<?>[]{
        fromLatLon(      ATAN_PHI,                 Math.PI/2),
        fromLatLon(     -ATAN_PHI,                 Math.PI/2),
        fromLatLon(      ATAN_PHI,                -Math.PI/2),
        fromLatLon(     -ATAN_PHI,                -Math.PI/2),
        fromLatLon(  ATAN_INV_PHI,                         0),
        fromLatLon(  ATAN_INV_PHI,                   Math.PI),
        fromLatLon( -ATAN_INV_PHI,                         0),
        fromLatLon( -ATAN_INV_PHI,                   Math.PI),
        fromLatLon(             0,                  ATAN_PHI),
        fromLatLon(             0,                 -ATAN_PHI),
        fromLatLon(             0,        Math.PI - ATAN_PHI),
        fromLatLon(             0,     -(Math.PI - ATAN_PHI)),
        fromLatLon(     ATAN_PHI2,                          0, INNER_RADIUS),
        fromLatLon(     ATAN_PHI2,                    Math.PI, INNER_RADIUS),
        fromLatLon(      CUBE_LAT,                  Math.PI/4, INNER_RADIUS),
        fromLatLon(      CUBE_LAT,                3*Math.PI/4, INNER_RADIUS),
        fromLatLon( ATAN_INV_PHI2,                  Math.PI/2, INNER_RADIUS),
        fromLatLon(    -ATAN_PHI2,                          0, INNER_RADIUS),
        fromLatLon(    -ATAN_PHI2,                    Math.PI, INNER_RADIUS),
        fromLatLon(     -CUBE_LAT,                  Math.PI/4, INNER_RADIUS),
        fromLatLon(     -CUBE_LAT,                3*Math.PI/4, INNER_RADIUS),
        fromLatLon(-ATAN_INV_PHI2,                  Math.PI/2, INNER_RADIUS),
        fromLatLon(      CUBE_LAT,                 -Math.PI/4, INNER_RADIUS),
        fromLatLon(      CUBE_LAT,               -3*Math.PI/4, INNER_RADIUS),
        fromLatLon( ATAN_INV_PHI2,                 -Math.PI/2, INNER_RADIUS),
        fromLatLon(     -CUBE_LAT,                 -Math.PI/4, INNER_RADIUS),
        fromLatLon(     -CUBE_LAT,               -3*Math.PI/4, INNER_RADIUS),
        fromLatLon(-ATAN_INV_PHI2,                 -Math.PI/2, INNER_RADIUS),
        fromLatLon(             0,              ATAN_INV_PHI2, INNER_RADIUS),
        fromLatLon(             0,             -ATAN_INV_PHI2, INNER_RADIUS),
        fromLatLon(             0,    Math.PI - ATAN_INV_PHI2, INNER_RADIUS),
        fromLatLon(             0, -(Math.PI - ATAN_INV_PHI2), INNER_RADIUS)
    }, true).build();

    private static final int[][] FACES = {
        {13,  2, 12,  0, 13},
        { 0, 12,  4, 14,  0},
        {15,  5, 13,  0, 15},
        { 0, 14,  8, 16,  0},
        {16, 10, 15,  0, 16},
        { 1, 17,  3, 18,  1},
        {19,  6, 17,  1, 19},
        { 1, 18,  7, 20,  1},
        {21,  8, 19,  1, 21},
        { 1, 20, 10, 21,  1},
        {22,  4, 12,  2, 22},
        { 2, 13,  5, 23,  2},
        {24,  9, 22,  2, 24},
        { 2, 23, 11, 24,  2},
        { 3, 17,  6, 25,  3},
        {26,  7, 18,  3, 26},
        { 3, 25,  9, 27,  3},
        {27, 11, 26,  3, 27},
        {29,  6, 28,  4, 29},
        {28,  8, 14,  4, 28},
        { 4, 22,  9, 29,  4},
        { 5, 30,  7, 31,  5},
        { 5, 15, 10, 30,  5},
        {31, 11, 23,  5, 31},
        { 6, 19,  8, 28,  6},
        {29,  9, 25,  6, 29},
        {30, 10, 20,  7, 30},
        { 7, 26, 11, 31,  7},
        {21, 10, 16,  8, 21},
        { 9, 24, 11, 27,  9}
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
