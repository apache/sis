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

import java.util.List;
import java.util.ArrayList;
import org.opengis.metadata.Identifier;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests {@link OperationMethods}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
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
}
