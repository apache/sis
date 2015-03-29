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
package org.apache.sis.internal.referencing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.transform.MathTransformsTest;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests {@link OperationMethods}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn({
    MathTransformsTest.class,
    org.apache.sis.referencing.operation.DefaultOperationMethodTest.class
})
public final strictfp class OperationMethodsTest extends TestCase {
    /**
     * Tests {@link OperationMethods#hasCommonIdentifier(Iterable, Iterable)}.
     */
    @Test
    public void testHasCommonIdentifier() {
        final List<Identifier> id1 = new ArrayList<>(3);
        final List<Identifier> id2 = new ArrayList<>(2);
        assertNull(OperationMethods.hasCommonIdentifier(id1, id2));
        /*
         * Add codes for two Operation Methods which are implemented in Apache SIS by the same class:
         *
         *  - EPSG:9804  —  "Mercator (variant A)" (formerly known as "Mercator (1SP)").
         *  - EPSG:1026  —  "Mercator (Spherical)"
         *  - GeoTIFF:7  —  "CT_Mercator"
         */
        id1.add(new ImmutableIdentifier(null, "EPSG", "9804"));
        id1.add(new ImmutableIdentifier(null, "EPSG", "1026"));
        id1.add(new ImmutableIdentifier(null, "GeoTIFF", "7"));
        assertNull(OperationMethods.hasCommonIdentifier(id1, id2));
        /*
         * EPSG:9841 is a legacy (now deprecated) code for "Mercator (1SP)".
         * We could have declared it as a deprecated code in the above list,
         * but for the sake of this test we do not.
         */
        id2.add(new ImmutableIdentifier(null, "EPSG", "9841"));
        assertEquals(Boolean.FALSE, OperationMethods.hasCommonIdentifier(id1, id2));
        id2.add(new ImmutableIdentifier(null, "EPSG", "9804"));
        assertEquals(Boolean.TRUE, OperationMethods.hasCommonIdentifier(id1, id2));
    }

    /**
     * Creates a dummy operation method of the given dimensions.
     */
    private static OperationMethod createOperationMethod(final int sourceDimension, final int targetDimension) {
        final ParameterDescriptorGroup parameters = new DefaultParameterDescriptorGroup(
                Collections.singletonMap(ParameterDescriptorGroup.NAME_KEY, "Dummy"), 1, 1);
        return new DefaultOperationMethod(Collections.singletonMap(OperationMethod.NAME_KEY, parameters.getName()),
                sourceDimension, targetDimension, parameters);
    }

    /**
     * Tests {@link OperationMethods#checkDimensions(OperationMethod, MathTransform, Map)}.
     */
    @Test
    public void testCheckDimensions() {
        final Map<String,?> properties = Collections.singletonMap(DefaultOperationMethod.LOCALE_KEY, Locale.ENGLISH);
        final MathTransform tr = MathTransformsTest.createConcatenateAndPassThrough();
        OperationMethods.checkDimensions(createOperationMethod(3, 3), tr, properties);
        OperationMethods.checkDimensions(createOperationMethod(1, 1), tr, properties);
        try {
            OperationMethods.checkDimensions(createOperationMethod(2, 2), tr, properties);
            fail("MathTransform.sourceDimension == 3 shall be considered incompatible.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            assertEquals(e.getMessage(), "The transform has 1 source dimension, while 2 was expected.");
        }
        try {
            OperationMethods.checkDimensions(createOperationMethod(3, 1), tr, properties);
            fail("MathTransform.targetDimension == 3 shall be considered incompatible.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            assertEquals(e.getMessage(), "The transform has 3 target dimensions, while 1 was expected.");
        }
    }
}
