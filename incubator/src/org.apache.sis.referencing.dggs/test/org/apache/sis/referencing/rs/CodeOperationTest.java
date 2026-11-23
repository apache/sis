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

import java.util.Map;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.operation.transform.MathTransforms;
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

    /**
     * Tests {@link CodeOperations.Reorder} selecting a single component.
     */
    @Test
    public void reorderSelect() throws TransformException {
        {   //first component
            final ReferenceSystem source = ReferenceSystems.createCompound(MGRS, TIME_JAVA);
            final CodeOperation operation = CodeOperations.reorder(source, MGRS, new int[]{0});
            final Code code = operation.transform(new Code(source, "4Q FJ 12 67", 1000.0), null);
            assertEquals(MGRS, code.getReferenceSystem());
            assertArrayEquals(new Object[]{"4Q FJ 12 67"}, code.getOrdinates());
        }
        {   //second component
            final ReferenceSystem source = ReferenceSystems.createCompound(TIME_JAVA, MGRS);
            final CodeOperation operation = CodeOperations.reorder(source, MGRS, new int[]{1});
            final Code code = operation.transform(new Code(source, 1000.0, "4Q FJ 12 67"), null);
            assertEquals(MGRS, code.getReferenceSystem());
            assertArrayEquals(new Object[]{"4Q FJ 12 67"}, code.getOrdinates());
        }
    }

    /**
     * Tests {@link CodeOperations.Reorder} swapping components, and its inverse.
     */
    @Test
    public void reorderSwap() throws TransformException {
        final ReferenceSystem source = ReferenceSystems.createCompound(MGRS, TIME_JAVA);
        final ReferenceSystem target = ReferenceSystems.createCompound(TIME_JAVA, MGRS);
        final CodeOperation operation = CodeOperations.reorder(source, target, new int[]{1, 0});

        final Code code = operation.transform(new Code(source, "4Q FJ 12 67", 1000.0), null);
        assertEquals(target, code.getReferenceSystem());
        assertArrayEquals(new Object[]{1000.0, "4Q FJ 12 67"}, code.getOrdinates());

        final CodeOperation inverse = operation.inverse();
        assertEquals(target, inverse.getSourceRS());
        assertEquals(source, inverse.getTargetRS());
        final Code back = inverse.transform(code, null);
        assertEquals(source, back.getReferenceSystem());
        assertArrayEquals(new Object[]{"4Q FJ 12 67", 1000.0}, back.getOrdinates());
    }

    /**
     * Tests {@link CodeOperations.Reorder} with a component of more than one dimension,
     * the mapping is on components, not on ordinates.
     */
    @Test
    public void reorderMultiDimensionalComponent() throws TransformException {
        final ReferenceSystem geographic = CommonCRS.WGS84.normalizedGeographic();
        final ReferenceSystem source = ReferenceSystems.createCompound(geographic, TIME_JAVA);
        {   //swap
            final ReferenceSystem target = ReferenceSystems.createCompound(TIME_JAVA, geographic);
            final CodeOperation operation = CodeOperations.reorder(source, target, new int[]{1, 0});
            final Code code = operation.transform(new Code(source, 10.0, 20.0, 1000.0), null);
            assertArrayEquals(new Object[]{1000.0, 10.0, 20.0}, code.getOrdinates());
        }
        {   //select the multi-dimensional component
            final CodeOperation operation = CodeOperations.reorder(source, geographic, new int[]{0});
            final Code code = operation.transform(new Code(source, 10.0, 20.0, 1000.0), null);
            assertArrayEquals(new Object[]{10.0, 20.0}, code.getOrdinates());
        }
        {   //select the one-dimensional component after it
            final CodeOperation operation = CodeOperations.reorder(source, TIME_JAVA, new int[]{1});
            final Code code = operation.transform(new Code(source, 10.0, 20.0, 1000.0), null);
            assertArrayEquals(new Object[]{1000.0}, code.getOrdinates());
        }
    }

    /**
     * Tests {@link CodeOperations.Reorder} writing in a given target code.
     */
    @Test
    public void reorderInTarget() throws TransformException {
        final ReferenceSystem source = ReferenceSystems.createCompound(MGRS, TIME_JAVA);
        final CodeOperation operation = CodeOperations.reorder(source, MGRS, new int[]{0});
        final Code target = new Code(MGRS);
        final Code code = operation.transform(new Code(source, "4Q FJ 12 67", 1000.0), target);
        assertSame(target, code);
        assertArrayEquals(new Object[]{"4Q FJ 12 67"}, code.getOrdinates());
    }

    /**
     * Tests operations found by {@link ReferenceSystems#findOperation} which drop a component.
     */
    @Test
    public void reorderFromFindOperation() throws FactoryException, TransformException {
        {   //only a reorder is needed
            final ReferenceSystem mrgs3d = ReferenceSystems.createCompound(MGRS, TIME_JAVA);
            final CodeOperation operation = ReferenceSystems.findOperation(mrgs3d, MGRS, null);
            assertTrue(operation instanceof CodeOperations.Reorder);
            final Code code = operation.transform(new Code(mrgs3d, "4Q FJ 12 67", 1000.0), null);
            assertArrayEquals(new Object[]{"4Q FJ 12 67"}, code.getOrdinates());
        }
        {   //reorder followed by a conversion
            final ReferenceSystem mrgs3d = ReferenceSystems.createCompound(TIME_JAVA, MGRS);
            final CodeOperation operation = ReferenceSystems.findOperation(mrgs3d, GEOHASH, null);
            final Code code = operation.transform(new Code(mrgs3d, 1000.0, "4Q FJ 12 67"), null);
            assertEquals("87z9y8fhdbff", code.getOrdinate(0));
        }
    }

    /**
     * Tests an operation found by {@link ReferenceSystems#findOperation} between reference systems
     * of three components, which requires a compound of three operations.
     */
    @Test
    public void compoundOfThreeComponents() throws FactoryException, TransformException {
        final ReferenceSystem vertical = CommonCRS.Vertical.MEAN_SEA_LEVEL.crs();
        final ReferenceSystem source = ReferenceSystems.createCompound(MGRS, TIME_JAVA, vertical);
        final ReferenceSystem target = ReferenceSystems.createCompound(TIME_JAVA, vertical, MGRS);
        final CodeOperation operation = ReferenceSystems.findOperation(source, target, null);
        final Code code = operation.transform(new Code(source, "4Q FJ 12 67", 1000.0, 50.0), null);
        assertArrayEquals(new Object[]{1000.0, 50.0, "4Q FJ 12 67"}, code.getOrdinates());
    }

    /**
     * Tests the inverse of {@link CodeOperations.CrsToCrs} when no operation can be found from the target to the source CRS.
     * The inverse shall be built from the inverse transform.
     */
    @Test
    public void crsToCrsInverseWithoutReverseOperation() throws Exception {
        final CoordinateReferenceSystem source = CommonCRS.Engineering.GRID.crs();
        final CoordinateReferenceSystem target = CommonCRS.Engineering.DISPLAY.crs();
        assertThrows(FactoryException.class, () -> CRS.findOperation(target, source, null));

        final OperationMethod affine = DefaultMathTransformFactory.provider().getOperationMethod("Affine");
        final CoordinateOperation forward = new DefaultConversion(Map.of(DefaultConversion.NAME_KEY, "Scale"),
                source, target, null, affine, MathTransforms.scale(2, 2));
        final CodeOperation inverse = CodeOperations.CrsToCrs(forward).inverse();
        assertSame(target, inverse.getSourceRS());
        assertSame(source, inverse.getTargetRS());
    }

    /**
     * Tests the inverse of {@link CodeOperations.Reorder} swapping a two-dimensional component with a one-dimensional one.
     */
    @Test
    public void reorderInverseMultiDimensionalComponent() throws TransformException {
        final ReferenceSystem geographic = CommonCRS.WGS84.normalizedGeographic();
        final ReferenceSystem source = ReferenceSystems.createCompound(geographic, TIME_JAVA);
        final ReferenceSystem target = ReferenceSystems.createCompound(TIME_JAVA, geographic);
        final CodeOperation inverse = CodeOperations.reorder(source, target, new int[]{1, 0}).inverse();
        final Code code = inverse.transform(new Code(target, 1000.0, 10.0, 20.0), null);
        assertArrayEquals(new Object[]{10.0, 20.0, 1000.0}, code.getOrdinates());
    }

    /**
     * Tests an operation between two CRS, with one ordinate per axis.
     */
    @Test
    public void crsToCrsPerAxis() throws FactoryException, TransformException {
        final CodeOperation operation = ReferenceSystems.findOperation(
                CommonCRS.WGS84.geographic(), CommonCRS.WGS84.normalizedGeographic(), null);
        final Code code = operation.transform(new Code(CommonCRS.WGS84.geographic(), 10.0, 20.0), null);
        assertArrayEquals(new Object[]{20.0, 10.0}, code.getOrdinates());
    }

    /**
     * Tests the conversion of a MGRS code to the leaning CRS, with one ordinate per axis.
     * The expected position is the center of the geohash {@code 87z9y8fhdbff}.
     */
    @Test
    public void rbiToCrsPerAxis() throws FactoryException, TransformException {
        final CodeOperation operation = ReferenceSystems.findOperation(MGRS, CommonCRS.WGS84.normalizedGeographic(), null);
        final Code code = operation.transform(new Code(MGRS, "4Q FJ 12 67"), null);
        assertEquals(2, code.getDimension());
        assertEquals(-157.914612, (Double) code.getOrdinate(0), 1E-5, "Longitude");
        assertEquals(  21.406264, (Double) code.getOrdinate(1), 1E-5, "Latitude");
    }

    /**
     * Tests transforming a code with a referencing by identifiers and a temporal component.
     */
    @Test
    public void transformCompoundPerAxis() throws FactoryException, TransformException {
        final ReferenceSystem source = ReferenceSystems.createCompound(MGRS, TIME_JAVA);
        final ReferenceSystem target = ReferenceSystems.createCompound(GEOHASH, TIME_JULIAN);
        final Code code = ReferenceSystems.findOperation(source, target, null).transform(new Code(source, "4Q FJ 12 67", 1000.0), null);
        assertEquals("87z9y8fhdbff", code.getOrdinate(0));
        assertEquals(2440587.5 + 1000.0 / 86400000, (Double) code.getOrdinate(1), 1E-9, "Julian day of 1 second after 1970");
    }

    /**
     * Tests the conversion of a zone identifier in its native type, without conversion to text.
     */
    @Test
    public void dggrsToCrsWithNativeIdentifier() throws Exception {
        final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
        final Zone zone = dggrs.getGridSystem().getHierarchy().getGrids().get(3).getZones().findFirst().orElseThrow();
        final CodeOperation operation = ReferenceSystems.findOperation(dggrs, CommonCRS.WGS84.normalizedGeographic(), null);
        final Code code = operation.transform(new Code(dggrs, zone.getIdentifier()), null);
        assertEquals(zone.getPosition().getCoordinate(0), (Double) code.getOrdinate(0), 1E-9);
        assertEquals(zone.getPosition().getCoordinate(1), (Double) code.getOrdinate(1), 1E-9);
    }

    /**
     * Tests that converting a position to a DGGRS gives the zone identifier in its native type.
     */
    @Test
    public void crsToDggrsWithNativeIdentifier() throws Exception {
        final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
        final CodeOperation operation = ReferenceSystems.findOperation(CommonCRS.WGS84.normalizedGeographic(), dggrs, null);
        final Code code = operation.transform(new Code(CommonCRS.WGS84.normalizedGeographic(), 15.0, 30.0), null);
        final Object expected = dggrs.createCoder().encodeIdentifier(new DirectPosition2D(CommonCRS.WGS84.normalizedGeographic(), 15, 30));
        assertEquals(expected, code.getOrdinate(0));
    }
}
