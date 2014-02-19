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
package org.apache.sis.internal.metadata;

import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.internal.metadata.ReferencingUtilities.*;


/**
 * Tests {@link ReferencingUtilities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class ReferencingUtilitiesTest extends TestCase {
    /**
     * Tests {@link ReferencingUtilities#toURNType(Class)}.
     */
    @Test
    public void testToURNType() {
        assertEquals("crs",             toURNType(GeographicCRS       .class));
        assertEquals("crs",             toURNType(ProjectedCRS        .class));
        assertEquals("crs",             toURNType(VerticalCRS         .class));
        assertEquals("crs",             toURNType(TemporalCRS         .class));
        assertEquals("datum",           toURNType(GeodeticDatum       .class));
        assertEquals("datum",           toURNType(VerticalDatum       .class));
        assertEquals("datum",           toURNType(TemporalDatum       .class));
        assertEquals("ellipsoid",       toURNType(Ellipsoid           .class));
        assertEquals("meridian",        toURNType(PrimeMeridian       .class));
        assertEquals("cs",              toURNType(EllipsoidalCS       .class));
        assertEquals("cs",              toURNType(CartesianCS         .class));
        assertEquals("axis",            toURNType(CoordinateSystemAxis.class));
        assertEquals("referenceSystem", toURNType(ReferenceSystem     .class));
    }

    /**
     * Tests {@link ReferencingUtilities#toWKTType(Class, Class)}.
     */
    @Test
    public void testType() {
        assertNull  (               toWKTType(CoordinateSystem.class, CoordinateSystem.class));
        assertEquals("affine",      toWKTType(CoordinateSystem.class, AffineCS        .class));
        assertEquals("Cartesian",   toWKTType(CoordinateSystem.class, CartesianCS     .class));
        assertEquals("cylindrical", toWKTType(CoordinateSystem.class, CylindricalCS   .class));
        assertEquals("ellipsoidal", toWKTType(CoordinateSystem.class, EllipsoidalCS   .class));
        assertEquals("linear",      toWKTType(CoordinateSystem.class, LinearCS        .class));
//      assertEquals("parametric",  toWKTType(CoordinateSystem.class, ParametricCS    .class));
        assertEquals("polar",       toWKTType(CoordinateSystem.class, PolarCS         .class));
        assertEquals("spherical",   toWKTType(CoordinateSystem.class, SphericalCS     .class));
        assertEquals("temporal",    toWKTType(CoordinateSystem.class, TimeCS          .class));
        assertEquals("vertical",    toWKTType(CoordinateSystem.class, VerticalCS      .class));
    }
}
