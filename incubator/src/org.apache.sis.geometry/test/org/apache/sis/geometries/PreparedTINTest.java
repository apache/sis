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
package org.apache.sis.geometries;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class PreparedTINTest {

    private static final CoordinateReferenceSystem CRS3D;
    static {
        try {
            CRS3D = CRS.compound(CommonCRS.WGS84.normalizedGeographic(), CommonCRS.Vertical.ELLIPSOIDAL.crs());
        } catch (FactoryException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Test single prepared tin.
     */
    @Test
    public void testSingle() throws TransformException {

        final MeshPrimitive.Triangles geometry = new MeshPrimitive.Triangles();
        geometry.setPositions(TupleArrays.of(CRS3D,
                0,1,2,
                3,4,5,
                6,7,8,
                9,10,11));
        geometry.setIndex(TupleArrays.ofUnsigned(1, 0, 1, 2, 2, 3, 1));

        final PreparedTIN tin = PreparedTIN.create(geometry);
        assertEquals(2, tin.getPatches(null).count());

        GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
        env.setRange(0, 8.9, 10);
        env.setRange(1, 8, 9);
        assertEquals(1, tin.getPatches(env).count());

    }

    /**
     * Test multi prepared tin.
     */
    @Test
    public void testMulti() throws TransformException {

        final MeshPrimitive.Triangles geometry = new MeshPrimitive.Triangles();
        geometry.setPositions(TupleArrays.of(CRS3D,
                0,1,2,
                3,4,5,
                6,7,8,
                9,10,11));
        geometry.setIndex(TupleArrays.ofUnsigned(1, 0, 1, 2, 2, 3, 1));

        final PreparedTIN tin = PreparedTIN.create(geometry, geometry);
        assertEquals(4, tin.getPatches(null).count());

        GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
        env.setRange(0, 8.9, 10);
        env.setRange(1, 8, 9);
        assertEquals(2, tin.getPatches(env).count());

        assertEquals(4, tin.getNumPatches());
        assertEquals("TRIANGLE ((0.0 1.0 2.0,3.0 4.0 5.0,6.0 7.0 8.0))", tin.getPatchN(0).asText());
        assertEquals("TRIANGLE ((6.0 7.0 8.0,9.0 10.0 11.0,3.0 4.0 5.0))", tin.getPatchN(1).asText());
        assertEquals("TRIANGLE ((0.0 1.0 2.0,3.0 4.0 5.0,6.0 7.0 8.0))", tin.getPatchN(2).asText());
        assertEquals("TRIANGLE ((6.0 7.0 8.0,9.0 10.0 11.0,3.0 4.0 5.0))", tin.getPatchN(3).asText());
    }
}
