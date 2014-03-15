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
package org.apache.sis.parameter;

import java.util.Map;
import java.util.List;
import java.util.Random;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.opengis.test.Validators.validate;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.apache.sis.parameter.TensorParameters.WKT1;
import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests the {@link TensorParameters} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn(ParametersTest.class)
public final strictfp class TensorParametersTest extends TestCase {
    /**
     * Tests the list of WKT descriptors for a 1×1, 2×3 and 3×3 matrices.
     */
    @Test
    public void testDescriptors() {
        List<GeneralParameterDescriptor> descriptors = WKT1.descriptors(new int[] {1, 1});
        assertEquals("num_row", descriptors.get(0).getName().getCode());
        assertEquals("num_col", descriptors.get(1).getName().getCode());
        assertEquals("elt_0_0", descriptors.get(2).getName().getCode());
        assertEquals("size", 3, descriptors.size());

        descriptors = WKT1.descriptors(new int[] {2, 3});
        assertEquals("num_row", descriptors.get(0).getName().getCode());
        assertEquals("num_col", descriptors.get(1).getName().getCode());
        assertEquals("elt_0_0", descriptors.get(2).getName().getCode());
        assertEquals("elt_0_1", descriptors.get(3).getName().getCode());
        assertEquals("elt_0_2", descriptors.get(4).getName().getCode());
        assertEquals("elt_1_0", descriptors.get(5).getName().getCode());
        assertEquals("elt_1_1", descriptors.get(6).getName().getCode());
        assertEquals("elt_1_2", descriptors.get(7).getName().getCode());
        assertEquals("size", 8, descriptors.size());

        descriptors = WKT1.descriptors(new int[] {3, 3});
        assertEquals("num_row",  descriptors.get( 0).getName().getCode());
        assertEquals("num_col",  descriptors.get( 1).getName().getCode());
        assertEquals("elt_0_0",  descriptors.get( 2).getName().getCode());
        assertEquals("elt_0_1",  descriptors.get( 3).getName().getCode());
        assertEquals("elt_0_2",  descriptors.get( 4).getName().getCode());
        assertEquals("elt_1_0",  descriptors.get( 5).getName().getCode());
        assertEquals("elt_1_1",  descriptors.get( 6).getName().getCode());
        assertEquals("elt_1_2",  descriptors.get( 7).getName().getCode());
        assertEquals("elt_2_0",  descriptors.get( 8).getName().getCode());
        assertEquals("elt_2_1",  descriptors.get( 9).getName().getCode());
        assertEquals("elt_2_2",  descriptors.get(10).getName().getCode());
        assertEquals("size", 11, descriptors.size());
    }

    /**
     * Tests {@link TensorParameters#createValueGroup(Map, Matrix)} and its converse
     * {@link TensorParameters#toMatrix(ParameterValueGroup)}.
     */
    @Test
    @DependsOnMethod("testDescriptors")
    public void testMatrixConversion() {
        final int size = 8;
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int numRow = 2; numRow <= size; numRow++) {
            for (int numCol = 2; numCol <= size; numCol++) {
                final Matrix matrix = Matrices.createZero(numRow, numCol);
                for (int j=0; j<numRow; j++) {
                    for (int i=0; i<numCol; i++) {
                        matrix.setElement(j, i, 200*random.nextDouble() - 100);
                    }
                }
                final ParameterValueGroup group = WKT1.createValueGroup(singletonMap(NAME_KEY, "Test"), matrix);
                validate(group);
                assertEquals("num_row",  numRow, group.parameter("num_row").intValue());
                assertEquals("num_col",  numCol, group.parameter("num_col").intValue());
                assertEquals("elements", matrix, WKT1.toMatrix(group));
            }
        }
    }

    /**
     * Tests WKT formatting.
     */
    @Test
    @DependsOnMethod("testMatrixConversion")
    public void testWKT() {
        final Matrix matrix = Matrices.createIdentity(4);
        matrix.setElement(0,2,  4);
        matrix.setElement(1,0, -2);
        matrix.setElement(2,3,  7);
        final ParameterValueGroup group = WKT1.createValueGroup(singletonMap(NAME_KEY, "Affine"), matrix);
        validate(group);
        assertWktEquals(
                "ParameterGroup[“Affine”,\n"      +
                "  Parameter[“num_row”, 4],\n"    +
                "  Parameter[“num_col”, 4],\n"    +
                "  Parameter[“elt_0_2”, 4.0],\n"  +
                "  Parameter[“elt_1_0”, -2.0],\n" +
                "  Parameter[“elt_2_3”, 7.0]]", group);
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(TensorParameters.WKT1);
    }
}
