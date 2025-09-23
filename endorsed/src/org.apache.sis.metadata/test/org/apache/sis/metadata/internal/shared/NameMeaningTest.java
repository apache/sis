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
package org.apache.sis.metadata.internal.shared;

import javax.measure.Unit;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.parameter.ParameterDescriptor;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link NameMeaning}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class NameMeaningTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public NameMeaningTest() {
    }

    /**
     * Tests {@link NameMeaning#toObjectType(Class)}.
     */
    @Test
    public void testToObjectType() {
        assertEquals("crs",                 NameMeaning.toObjectType(GeographicCRS       .class));
        assertEquals("crs",                 NameMeaning.toObjectType(ProjectedCRS        .class));
        assertEquals("crs",                 NameMeaning.toObjectType(VerticalCRS         .class));
        assertEquals("crs",                 NameMeaning.toObjectType(TemporalCRS         .class));
        assertEquals("datum",               NameMeaning.toObjectType(GeodeticDatum       .class));
        assertEquals("datum",               NameMeaning.toObjectType(VerticalDatum       .class));
        assertEquals("datum",               NameMeaning.toObjectType(TemporalDatum       .class));
        assertEquals("ellipsoid",           NameMeaning.toObjectType(Ellipsoid           .class));
        assertEquals("meridian",            NameMeaning.toObjectType(PrimeMeridian       .class));
        assertEquals("cs",                  NameMeaning.toObjectType(EllipsoidalCS       .class));
        assertEquals("cs",                  NameMeaning.toObjectType(CartesianCS         .class));
        assertEquals("axis",                NameMeaning.toObjectType(CoordinateSystemAxis.class));
        assertEquals("referenceSystem",     NameMeaning.toObjectType(ReferenceSystem     .class));
        assertEquals("coordinateOperation", NameMeaning.toObjectType(CoordinateOperation .class));
        assertEquals("method",              NameMeaning.toObjectType(OperationMethod     .class));
        assertEquals("parameter",           NameMeaning.toObjectType(ParameterDescriptor .class));
        assertEquals("uom",                 NameMeaning.toObjectType(Unit                .class));
    }

    /**
     * Tests {@link NameMeaning#toURN(Class, String, String, String)}.
     */
    @Test
    public void testToURN() {
        assertEquals("urn:ogc:def:crs:EPSG::4326",    NameMeaning.toURN(GeodeticCRS.class,   "EPSG", null, "4326"));
        assertEquals("urn:ogc:def:crs:OGC:1.3:CRS84", NameMeaning.toURN(GeographicCRS.class, "CRS",  null,   "84"));
        assertEquals("urn:ogc:def:datum:EPSG::6326",  NameMeaning.toURN(GeodeticDatum.class, "EPSG", null, "6326"));
        assertNull  (NameMeaning.toURN(GeographicCRS.class, null,   null, "4326"), "Authority is not optional.");
    }
}
