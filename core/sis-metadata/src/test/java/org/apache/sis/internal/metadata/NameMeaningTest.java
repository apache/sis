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
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link NameMeaning}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from 0.4)
 * @version 0.7
 * @module
 */
public final strictfp class NameMeaningTest extends TestCase {
    /**
     * Tests {@link NameMeaning#toObjectType(Class)}.
     */
    @Test
    public void testToObjectType() {
        assertEquals("crs",             NameMeaning.toObjectType(GeographicCRS       .class));
        assertEquals("crs",             NameMeaning.toObjectType(ProjectedCRS        .class));
        assertEquals("crs",             NameMeaning.toObjectType(VerticalCRS         .class));
        assertEquals("crs",             NameMeaning.toObjectType(TemporalCRS         .class));
        assertEquals("datum",           NameMeaning.toObjectType(GeodeticDatum       .class));
        assertEquals("datum",           NameMeaning.toObjectType(VerticalDatum       .class));
        assertEquals("datum",           NameMeaning.toObjectType(TemporalDatum       .class));
        assertEquals("ellipsoid",       NameMeaning.toObjectType(Ellipsoid           .class));
        assertEquals("meridian",        NameMeaning.toObjectType(PrimeMeridian       .class));
        assertEquals("cs",              NameMeaning.toObjectType(EllipsoidalCS       .class));
        assertEquals("cs",              NameMeaning.toObjectType(CartesianCS         .class));
        assertEquals("axis",            NameMeaning.toObjectType(CoordinateSystemAxis.class));
        assertEquals("referenceSystem", NameMeaning.toObjectType(ReferenceSystem     .class));
    }

    /**
     * Tests {@link NameMeaning#toURN(Class, String, String, String)}.
     *
     * @since 0.7
     */
    @Test
    @DependsOnMethod("testToObjectType")
    public void testToURN() {
        assertEquals("urn:ogc:def:crs:OGC:1.3:CRS84", NameMeaning.toURN(GeographicCRS.class, "CRS",  null,   "84"));
        assertEquals("urn:ogc:def:datum:EPSG::6326",  NameMeaning.toURN(GeodeticDatum.class, "EPSG", null, "6326"));
    }
}
