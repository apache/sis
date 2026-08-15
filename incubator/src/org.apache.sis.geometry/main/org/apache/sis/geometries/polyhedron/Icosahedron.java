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
 * @see https://mathworld.wolfram.com/RegularIcosahedron.html
 * @author Johann Sorel (Geomatys)
 */
public final class Icosahedron extends AbstractPolyhedron{

    /**
     * Vertices are all cyclic permutations of (0, +-1, +-phi), normalized,
     * where phi is the golden ratio.
     */
    private static final Array VERTICES = ArrayFactoryJava.INSTANCE.builder()
        .dataType(DataType.DOUBLE).values(new ReadOnly.Vector<?>[]{
        fromLatLon(     ATAN_PHI,            Math.PI/2),
        fromLatLon(    -ATAN_PHI,            Math.PI/2),
        fromLatLon(     ATAN_PHI,           -Math.PI/2),
        fromLatLon(    -ATAN_PHI,           -Math.PI/2),
        fromLatLon( ATAN_INV_PHI,                    0),
        fromLatLon( ATAN_INV_PHI,              Math.PI),
        fromLatLon(-ATAN_INV_PHI,                    0),
        fromLatLon(-ATAN_INV_PHI,              Math.PI),
        fromLatLon(            0,             ATAN_PHI),
        fromLatLon(            0,            -ATAN_PHI),
        fromLatLon(            0,   Math.PI - ATAN_PHI),
        fromLatLon(            0, -(Math.PI - ATAN_PHI))
    }, true).build();

    private static final int[][] FACES = {
        { 4,  0,  2,  4},
        { 2,  0,  5,  2},
        { 8,  0,  4,  8},
        { 5,  0, 10,  5},
        {10,  0,  8, 10},
        { 3,  1,  6,  3},
        { 7,  1,  3,  7},
        { 6,  1,  8,  6},
        {10,  1,  7, 10},
        { 8,  1, 10,  8},
        { 4,  2,  9,  4},
        {11,  2,  5, 11},
        { 9,  2, 11,  9},
        { 9,  3,  6,  9},
        { 7,  3, 11,  7},
        {11,  3,  9, 11},
        { 8,  4,  6,  8},
        { 6,  4,  9,  6},
        { 7,  5, 10,  7},
        {11,  5,  7, 11}
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
