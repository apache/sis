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
package org.apache.sis.internal.feature;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link GeometryType}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class GeometryTypeTest extends TestCase {
    /**
     * Tests {@link GeometryType#forBinaryType(int)} and verifies {@link GeometryType#binaryType()} values.
     */
    @Test
    public void testBinaryType() {
        for (final GeometryType type : GeometryType.values()) {
            assertSame(type.name(), type, GeometryType.forBinaryType(type.binaryType()));
        }
    }

    /**
     * Tests {@link GeometryType#forName(String)}.
     */
    @Test
    public void testForName() {
        assertSame(GeometryType.MULTI_POLYGON, GeometryType.forName("multi_polygon"));
        assertSame(GeometryType.MULTI_POLYGON, GeometryType.forName("MULTIPOLYGON"));
        assertSame(GeometryType.GEOMETRY_COLLECTION, GeometryType.forName("GEOMETRY_COLLECTION"));
        assertSame(GeometryType.GEOMETRY_COLLECTION, GeometryType.forName("GeomCollection"));
    }
}
