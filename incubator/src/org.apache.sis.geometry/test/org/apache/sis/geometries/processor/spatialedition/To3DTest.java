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
package org.apache.sis.geometries.processor.spatialedition;

import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.geometries.operation.GeometryOperations;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class To3DTest {

    private static final CoordinateReferenceSystem CRS2D = CommonCRS.WGS84.normalizedGeographic();
    private static final CoordinateReferenceSystem CRS2DZ;
    private static final CoordinateReferenceSystem CRS3D = CommonCRS.WGS84.geocentric();

    static {
        try {
            CRS2DZ = CRS.compound(CRS2D, CommonCRS.Vertical.ELLIPSOIDAL.crs());
        } catch (FactoryException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Test
    public void testPrimitive() {

        final MeshPrimitive primitive = new MeshPrimitive.Points();
        primitive.setPositions(TupleArrays.of(CRS2D,
                0.0, 1.0,
                2.0, 3.0,
                4.0, 5.0,
                6.0, 7.0
        ));

        Geometry result = GeometryOperations.SpatialEdition.to3D(primitive, CRS3D, (Tuple t) -> t.set(2, 15));
        assertTrue(result instanceof MeshPrimitive);
        assertEquals(CRS3D, result.getCoordinateReferenceSystem());
        MeshPrimitive p = (MeshPrimitive) result;

        assertArrayEquals(new double[]{
                0.0, 1.0, 15.0,
                2.0, 3.0, 15.0,
                4.0, 5.0, 15.0,
                6.0, 7.0, 15.0
                },
                p.getPositions().toArrayDouble(), 0.0);

    }
}
