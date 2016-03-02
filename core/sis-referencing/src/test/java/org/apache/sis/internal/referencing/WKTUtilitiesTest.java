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

import org.opengis.referencing.cs.*;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.internal.referencing.WKTUtilities.*;


/**
 * Tests {@link WKTUtilities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(ReferencingUtilitiesTest.class)
public final strictfp class WKTUtilitiesTest extends TestCase {
    /**
     * Tests {@link ReferencingUtilities#toType(Class, Class)}.
     *
     * @see ReferencingUtilitiesTest#testToPropertyName()
     */
    @Test
    public void testToType() {
        assertNull  (                         toType(CoordinateSystem.class, CoordinateSystem.class));
        assertEquals(WKTKeywords.affine,      toType(CoordinateSystem.class, AffineCS        .class));
        assertEquals(WKTKeywords.Cartesian,   toType(CoordinateSystem.class, CartesianCS     .class));
        assertEquals(WKTKeywords.cylindrical, toType(CoordinateSystem.class, CylindricalCS   .class));
        assertEquals(WKTKeywords.ellipsoidal, toType(CoordinateSystem.class, EllipsoidalCS   .class));
        assertEquals(WKTKeywords.linear,      toType(CoordinateSystem.class, LinearCS        .class));
//      assertEquals(WKTKeywords.parametric,  toType(CoordinateSystem.class, ParametricCS    .class));
        assertEquals(WKTKeywords.polar,       toType(CoordinateSystem.class, PolarCS         .class));
        assertEquals(WKTKeywords.spherical,   toType(CoordinateSystem.class, SphericalCS     .class));
        assertEquals(WKTKeywords.temporal,    toType(CoordinateSystem.class, TimeCS          .class));
        assertEquals(WKTKeywords.vertical,    toType(CoordinateSystem.class, VerticalCS      .class));
    }
}
