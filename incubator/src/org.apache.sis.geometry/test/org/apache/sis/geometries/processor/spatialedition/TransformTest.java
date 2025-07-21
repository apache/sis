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

import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MultiMeshPrimitive;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.geometries.operation.GeometryOperations;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class TransformTest {

    private static final CoordinateReferenceSystem CRS_SOURCE;
    private static final CoordinateReferenceSystem CRS_TARGET = CommonCRS.WGS84.geocentric();
    private static final MathTransform TRANSFORM;
    static {
        try {
            CRS_SOURCE = CRS.compound(CommonCRS.WGS84.normalizedGeographic(), CommonCRS.Vertical.ELLIPSOIDAL.crs());
            TRANSFORM = CRS.findOperation(CRS_SOURCE, CRS_TARGET, null).getMathTransform();
        } catch (FactoryException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private static MeshPrimitive createPrimitive() throws FactoryException {

        final MeshPrimitive primitive = new MeshPrimitive.Points();
        primitive.setPositions(TupleArrays.of(CRS_SOURCE,
                0.0, 0.0, 0.0, //point on earth equator, lon 0
                90.0, 0.0, 0.0, //point on earth equator, lon 90
                180.0, 0.0, 0.0, //point on earth equator, lon 180
                45.0, 45.0, 0.0 //point in east-europe
        ));
        primitive.setNormals(TupleArrays.of(3,
                0.0, 0.0, 1.0,
                0.0, 0.0, 1.0,
                0.0, 0.0, 1.0,
                0.0, 0.0, 1.0
        ));
        primitive.setTangents(TupleArrays.of(3,
                1.0, 0.0, 0.0,
                1.0, 0.0, 0.0,
                1.0, 0.0, 0.0,
                1.0, 0.0, 0.0
        ));
        primitive.setTexCoords(0,TupleArrays.of(2,
                1.0, 2.0,
                3.0, 4.0,
                5.0, 6.0,
                7.0, 8.0
        ));

        return primitive;
    }

    private static void testPrimitive(MeshPrimitive result) {
        assertEquals(CRS_TARGET, result.getCoordinateReferenceSystem());
        assertTrue(TupleArrays.of(CRS_TARGET,
                6378137.0, 0.0, 0.0,
                0.0, 6378137.0, 0.0,
               -6378137.0, 0.0, 0.0,
               3194419.145, 3194419.145, 4487348.408
        ).equals(result.getPositions(), 1e-3));
        assertTrue(TupleArrays.of(3,
                1.0, 0.0, 0.0,
                0.0, 1.0, 0.0,
               -1.0, 0.0, 0.0,
                0.5, 0.5, 0.707
        ).equals(result.getNormals(), 1e-3));
        assertTrue(TupleArrays.of(3,
                0.0, 1.0, 0.0,
               -1.0, 0.0, 0.0,
                0.0,-1.0, 0.0,
               -0.707,0.707, 0.0
        ).equals(result.getTangents(), 1e-3));
        assertTrue(TupleArrays.of(2,
                1.0, 2.0,
                3.0, 4.0,
                5.0, 6.0,
                7.0, 8.0
        ).equals(result.getTexCoords(0), 0.0));
    }


    /**
     * Test geometry transform modify position, normal, tangent attributes
     * and other attributes are left unchanged.
     */
    @Test
    public void Primitive() throws FactoryException, TransformException {
        final MeshPrimitive primitive = createPrimitive();
        final MeshPrimitive result = (MeshPrimitive) GeometryOperations.SpatialEdition.transform(primitive, CRS_TARGET, TRANSFORM);
        testPrimitive(result);
    }

    @Test
    public void MultiPrimitive() throws FactoryException, TransformException {
        final MultiMeshPrimitive mp = new MultiMeshPrimitive(createPrimitive());
        final MultiMeshPrimitive result = (MultiMeshPrimitive) GeometryOperations.SpatialEdition.transform(mp, CRS_TARGET, TRANSFORM);
        assertEquals(1, result.getNumGeometries());
        testPrimitive(result.getGeometryN(0));
    }

}
