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
package org.apache.sis.referencing.rs;

import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.gazetteer.GazetteerException;
import org.apache.sis.referencing.gazetteer.GeohashReferenceSystem;
import org.apache.sis.referencing.gazetteer.MilitaryGridReferenceSystem;
import org.apache.sis.referencing.rs.internal.shared.CodeOperations;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class CodeOperationTest {

    private final MilitaryGridReferenceSystem MGRS = new MilitaryGridReferenceSystem();
    private final TemporalCRS TIME_JAVA = CommonCRS.Temporal.JAVA.crs();
    private final TemporalCRS TIME_JULIAN = CommonCRS.Temporal.JULIAN.crs();
    private final GeohashReferenceSystem GEOHASH;

    public CodeOperationTest() throws GazetteerException {
        GEOHASH = new GeohashReferenceSystem(GeohashReferenceSystem.Format.BASE32, CommonCRS.WGS84.normalizedGeographic());
    }

    @Test
    public void MGRStoGeoHash84() throws FactoryException, TransformException {

        final CodeOperation operation = ReferenceSystems.findOperation(MGRS, GEOHASH, null);
        assertTrue(operation instanceof CodeOperations.Concatenate);
        final CodeOperations.Concatenate co = (CodeOperations.Concatenate) operation;
        assertTrue(co.getOperation1() instanceof CodeOperations.RbiToCrs);
        assertTrue(co.getOperation2() instanceof CodeOperations.CrsToRbi);
        final CodeOperations.RbiToCrs op1 = (CodeOperations.RbiToCrs) co.getOperation1();
        final CodeOperations.CrsToRbi op2 = (CodeOperations.CrsToRbi) co.getOperation2();
        assertEquals(op1.getSourceRS(), MGRS);
        assertEquals(op1.getTargetRS(), CommonCRS.WGS84.normalizedGeographic());
        assertEquals(op2.getSourceRS(), CommonCRS.WGS84.normalizedGeographic());
        assertEquals(op2.getTargetRS(), GEOHASH);

        //test conversion
        final Code code = operation.transform(new Code(MGRS, "4Q FJ 12 67"), null);
        assertEquals("87z9y8fhdbff", code.getOrdinate(0));

    }

    @Test
    public void MGRS3DtoGeoHash() throws FactoryException {

        {
            final ReferenceSystem mrgs3d = ReferenceSystems.createCompound(MGRS, TIME_JAVA);
            final CodeOperation operation = ReferenceSystems.findOperation(mrgs3d, GEOHASH, null);

            assertTrue(operation instanceof CodeOperations.Concatenate);
            final CodeOperations.Concatenate concat1 = (CodeOperations.Concatenate) operation;
            assertTrue(concat1.getOperation1() instanceof CodeOperations.Reorder);
            assertTrue(concat1.getOperation2() instanceof CodeOperations.Concatenate);
            final CodeOperations.Reorder op1 = (CodeOperations.Reorder) concat1.getOperation1();
            final CodeOperations.Concatenate op2 = (CodeOperations.Concatenate) concat1.getOperation2();

            assertArrayEquals(new int[]{0},op1.getTargetMapping());

            assertTrue(op2.getOperation1() instanceof CodeOperations.RbiToCrs);
            assertTrue(op2.getOperation2() instanceof CodeOperations.CrsToRbi);
            final CodeOperations.RbiToCrs op11 = (CodeOperations.RbiToCrs) op2.getOperation1();
            final CodeOperations.CrsToRbi op12 = (CodeOperations.CrsToRbi) op2.getOperation2();
            assertEquals(op11.getSourceRS(), MGRS);
            assertEquals(op11.getTargetRS(), CommonCRS.WGS84.normalizedGeographic());
            assertEquals(op12.getSourceRS(), CommonCRS.WGS84.normalizedGeographic());
            assertEquals(op12.getTargetRS(), GEOHASH);

            try {
                operation.inverse();
                fail("Operation can not be inverted");
            } catch (NoninvertibleTransformException e) {
                //ok
            }
        }

        {
            final ReferenceSystem mrgs3d = ReferenceSystems.createCompound(TIME_JAVA, MGRS);
            final CodeOperation operation = ReferenceSystems.findOperation(mrgs3d, GEOHASH, null);

            assertTrue(operation instanceof CodeOperations.Concatenate);
            final CodeOperations.Concatenate concat1 = (CodeOperations.Concatenate) operation;
            assertTrue(concat1.getOperation1() instanceof CodeOperations.Reorder);
            assertTrue(concat1.getOperation2() instanceof CodeOperations.Concatenate);
            final CodeOperations.Reorder op1 = (CodeOperations.Reorder) concat1.getOperation1();
            final CodeOperations.Concatenate op2 = (CodeOperations.Concatenate) concat1.getOperation2();

            assertArrayEquals(new int[]{1},op1.getTargetMapping());

            assertTrue(op2.getOperation1() instanceof CodeOperations.RbiToCrs);
            assertTrue(op2.getOperation2() instanceof CodeOperations.CrsToRbi);
            final CodeOperations.RbiToCrs op11 = (CodeOperations.RbiToCrs) op2.getOperation1();
            final CodeOperations.CrsToRbi op12 = (CodeOperations.CrsToRbi) op2.getOperation2();
            assertEquals(op11.getSourceRS(), MGRS);
            assertEquals(op11.getTargetRS(), CommonCRS.WGS84.normalizedGeographic());
            assertEquals(op12.getSourceRS(), CommonCRS.WGS84.normalizedGeographic());
            assertEquals(op12.getTargetRS(), GEOHASH);

            try {
                operation.inverse();
                fail("Operation can not be inverted");
            } catch(NoninvertibleTransformException e) {
                //ok
            }
        }

        {
            try {
                final ReferenceSystem mrgs3d = ReferenceSystems.createCompound(TIME_JAVA, MGRS);
                final CodeOperation operation = ReferenceSystems.findOperation(GEOHASH, mrgs3d, null);
                fail("Operation should not be possible");
            } catch (FactoryException e) {
                //ok
            }
        }
    }

    @Test
    public void MGRS3DtoGeoHash3D() throws FactoryException {

        {
            final ReferenceSystem mrgs3d = ReferenceSystems.createCompound(MGRS, TIME_JAVA);
            final ReferenceSystem geohash3d = ReferenceSystems.createCompound(GEOHASH, TIME_JULIAN);
            final CodeOperation operation = ReferenceSystems.findOperation(mrgs3d, geohash3d, null);

            assertTrue(operation instanceof CodeOperations.Compound);
            final CodeOperations.Compound compound = (CodeOperations.Compound) operation;
            assertTrue(compound.getOperation1() instanceof CodeOperations.Concatenate);
            assertTrue(compound.getOperation2() instanceof CodeOperations.CrsToCrs);
            final CodeOperations.Concatenate op1 = (CodeOperations.Concatenate) compound.getOperation1();
            final CodeOperations.CrsToCrs op2 = (CodeOperations.CrsToCrs) compound.getOperation2();

            assertTrue(op1 instanceof CodeOperations.Concatenate);
            final CodeOperations.Concatenate co = (CodeOperations.Concatenate) op1;
            assertTrue(co.getOperation1() instanceof CodeOperations.RbiToCrs);
            assertTrue(co.getOperation2() instanceof CodeOperations.CrsToRbi);
            final CodeOperations.RbiToCrs op11 = (CodeOperations.RbiToCrs) co.getOperation1();
            final CodeOperations.CrsToRbi op12 = (CodeOperations.CrsToRbi) co.getOperation2();
            assertEquals(op11.getSourceRS(), MGRS);
            assertEquals(op11.getTargetRS(), CommonCRS.WGS84.normalizedGeographic());
            assertEquals(op12.getSourceRS(), CommonCRS.WGS84.normalizedGeographic());
            assertEquals(op12.getTargetRS(), GEOHASH);

            assertEquals(op2.getSourceRS(), TIME_JAVA);
            assertEquals(op2.getTargetRS(), TIME_JULIAN);

        }

    }
}
