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
 * @see https://mathworld.wolfram.com/TruncatedIcosahedron.html
 * @author Johann Sorel (Geomatys)
 */
public final class TruncatedIcosahedron extends AbstractPolyhedron{

    /**
     * atan(1/(3*phi)) : one of the two latitude values of the vertices
     * shared between a pentagon and two hexagons.
     */
    private static final double ATAN_INV_3PHI = Math.atan(1 / (3*PHI));

    /**
     * Remaining distinct latitude/longitude magnitudes appearing in the
     * vertices below. Unlike the other solids, a truncated icosahedron
     * vertex is a 1/3-interpolation between two {@link Icosahedron}
     * vertices, normalized back onto the unit sphere ; this does not reduce
     * to a simple expression of PI or phi, so these are plain precise
     * radian constants rather than derived formulas.
     */
    private static final double LAT_1 = 1.3676273807451667;
    private static final double LAT_2 = 1.0250282040502057;
    private static final double LAT_3 = 0.81835937075291465;
    private static final double LAT_4 = 0.71147906437076591;
    private static final double LAT_5 = 0.33257431379403996;
    private static final double LAT_6 = 0.41539154898382641;
    private static final double LON_1 = 0.89058134991397542;
    private static final double LON_2 = 0.29970859976855624;
    private static final double LON_3 = 1.3011353287662717;
    private static final double LON_4 = 1.1296873864711223;

    /**
     * Vertices are obtained by truncating each {@link Icosahedron} edge at
     * its 1/3 and 2/3 points (the uniform truncation ratio that keeps every
     * new edge, pentagon and hexagon, the same length), then normalized.
     */
    private static final Array VERTICES = ArrayFactoryJava.INSTANCE.builder()
        .dataType(DataType.DOUBLE).values(new ReadOnly.Vector<?>[]{
        fromLatLon(         LAT_1,                    Math.PI/2),
        fromLatLon(         LAT_1,                   -Math.PI/2),
        fromLatLon(         LAT_2,                        LON_1),
        fromLatLon(         LAT_3,                        LON_2),
        fromLatLon(         LAT_2,              Math.PI - LON_1),
        fromLatLon(         LAT_3,              Math.PI - LON_2),
        fromLatLon(         LAT_4,                        LON_3),
        fromLatLon(         LAT_5,                        LON_4),
        fromLatLon(         LAT_4,              Math.PI - LON_3),
        fromLatLon(         LAT_5,              Math.PI - LON_4),
        fromLatLon(        -LAT_1,                    Math.PI/2),
        fromLatLon(        -LAT_1,                   -Math.PI/2),
        fromLatLon(        -LAT_2,                        LON_1),
        fromLatLon(        -LAT_3,                        LON_2),
        fromLatLon(        -LAT_2,              Math.PI - LON_1),
        fromLatLon(        -LAT_3,              Math.PI - LON_2),
        fromLatLon(        -LAT_4,                        LON_3),
        fromLatLon(        -LAT_5,                        LON_4),
        fromLatLon(        -LAT_4,              Math.PI - LON_3),
        fromLatLon(        -LAT_5,              Math.PI - LON_4),
        fromLatLon(         LAT_2,                       -LON_1),
        fromLatLon(         LAT_3,                       -LON_2),
        fromLatLon(         LAT_2,           -(Math.PI - LON_1)),
        fromLatLon(         LAT_3,           -(Math.PI - LON_2)),
        fromLatLon(         LAT_4,                       -LON_3),
        fromLatLon(         LAT_5,                       -LON_4),
        fromLatLon(         LAT_4,           -(Math.PI - LON_3)),
        fromLatLon(         LAT_5,           -(Math.PI - LON_4)),
        fromLatLon(        -LAT_2,                       -LON_1),
        fromLatLon(        -LAT_3,                       -LON_2),
        fromLatLon(        -LAT_2,           -(Math.PI - LON_1)),
        fromLatLon(        -LAT_3,           -(Math.PI - LON_2)),
        fromLatLon(        -LAT_4,                       -LON_3),
        fromLatLon(        -LAT_5,                       -LON_4),
        fromLatLon(        -LAT_4,           -(Math.PI - LON_3)),
        fromLatLon(        -LAT_5,           -(Math.PI - LON_4)),
        fromLatLon( ATAN_INV_3PHI,                            0),
        fromLatLon(-ATAN_INV_3PHI,                            0),
        fromLatLon(         LAT_6,                ATAN_INV_PHI2),
        fromLatLon( ATAN_INV_3PHI,              2*ATAN_INV_PHI2),
        fromLatLon(         LAT_6,               -ATAN_INV_PHI2),
        fromLatLon( ATAN_INV_3PHI,             -2*ATAN_INV_PHI2),
        fromLatLon( ATAN_INV_3PHI,                      Math.PI),
        fromLatLon(-ATAN_INV_3PHI,                      Math.PI),
        fromLatLon(         LAT_6,      Math.PI - ATAN_INV_PHI2),
        fromLatLon( ATAN_INV_3PHI,    Math.PI - 2*ATAN_INV_PHI2),
        fromLatLon(         LAT_6,   -(Math.PI - ATAN_INV_PHI2)),
        fromLatLon( ATAN_INV_3PHI, -(Math.PI - 2*ATAN_INV_PHI2)),
        fromLatLon(        -LAT_6,                ATAN_INV_PHI2),
        fromLatLon(-ATAN_INV_3PHI,              2*ATAN_INV_PHI2),
        fromLatLon(        -LAT_6,               -ATAN_INV_PHI2),
        fromLatLon(-ATAN_INV_3PHI,             -2*ATAN_INV_PHI2),
        fromLatLon(        -LAT_6,      Math.PI - ATAN_INV_PHI2),
        fromLatLon(-ATAN_INV_3PHI,    Math.PI - 2*ATAN_INV_PHI2),
        fromLatLon(        -LAT_6,   -(Math.PI - ATAN_INV_PHI2)),
        fromLatLon(-ATAN_INV_3PHI, -(Math.PI - 2*ATAN_INV_PHI2)),
        fromLatLon(             0,                       LAT_1),
        fromLatLon(             0,             Math.PI - LAT_1),
        fromLatLon(             0,                      -LAT_1),
        fromLatLon(             0,           -(Math.PI - LAT_1))
    }, true).build();

    private static final int[][] FACES = {
        { 8,  4,  0,  2,  6,  8},
        {16, 12, 10, 14, 18, 16},
        {24, 20,  1, 22, 26, 24},
        {34, 30, 11, 28, 32, 34},
        {36, 38,  3, 21, 40, 36},
        {46, 23,  5, 44, 42, 46},
        {50, 29, 13, 48, 37, 50},
        {43, 52, 15, 31, 54, 43},
        {17, 56,  7, 39, 49, 17},
        {51, 41, 25, 58, 33, 51},
        {53, 45,  9, 57, 19, 53},
        {35, 59, 27, 47, 55, 35},
        { 3,  2,  0,  1, 20, 21,  3},
        { 1,  0,  4,  5, 23, 22,  1},
        { 7,  6,  2,  3, 38, 39,  7},
        { 5,  4,  8,  9, 45, 44,  5},
        { 9,  8,  6,  7, 56, 57,  9},
        {11, 10, 12, 13, 29, 28, 11},
        {15, 14, 10, 11, 30, 31, 15},
        {13, 12, 16, 17, 49, 48, 13},
        {19, 18, 14, 15, 52, 53, 19},
        {17, 16, 18, 19, 57, 56, 17},
        {21, 20, 24, 25, 41, 40, 21},
        {27, 26, 22, 23, 46, 47, 27},
        {25, 24, 26, 27, 59, 58, 25},
        {33, 32, 28, 29, 50, 51, 33},
        {31, 30, 34, 35, 55, 54, 31},
        {35, 34, 32, 33, 58, 59, 35},
        {39, 38, 36, 37, 48, 49, 39},
        {37, 36, 40, 41, 51, 50, 37},
        {43, 42, 44, 45, 53, 52, 43},
        {47, 46, 42, 43, 54, 55, 47}
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
