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
 * @see https://mathworld.wolfram.com/RegularOctahedron.html
 * @author Johann Sorel (Geomatys)
 */
public final class Octahedron extends AbstractPolyhedron{

    private static final Array VERTICES = ArrayFactoryJava.INSTANCE.builder()
        .dataType(DataType.DOUBLE).values(new ReadOnly.Vector<?>[]{
        fromLatLon(         0,          0),
        fromLatLon(         0,    Math.PI),
        fromLatLon(         0,  Math.PI/2),
        fromLatLon(         0, -Math.PI/2),
        fromLatLon( Math.PI/2,          0),
        fromLatLon(-Math.PI/2,          0)
    }, true).build();

    private static final int[][] FACES = {
        {4, 0, 2, 4},
        {2, 0, 5, 2},
        {3, 0, 4, 3},
        {5, 0, 3, 5},
        {2, 1, 4, 2},
        {5, 1, 2, 5},
        {4, 1, 3, 4},
        {3, 1, 5, 3}
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
