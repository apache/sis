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
package org.apache.sis.internal.referencing.provider;

import org.apache.sis.internal.util.Constants;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;


/**
 * Verifies some parameters of {@link MapProjection} and a few subclasses.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final strictfp class MapProjectionTest extends TestCase {
    /**
     * Verifies {@link MapProjection#SEMI_MAJOR} and {@link MapProjection#SEMI_MINOR} parameter descriptors.
     */
    @Test
    public void testSemiAxes() {
        assertOgcIdentifierEquals(Constants.SEMI_MAJOR, MapProjection.SEMI_MAJOR.getName());
        assertOgcIdentifierEquals(Constants.SEMI_MINOR, MapProjection.SEMI_MINOR.getName());
    }

    /**
     * Verifies some {@link Mercator1SP} parameter descriptors.
     */
    @Test
    public void testMercator1SP() {
        assertEpsgIdentifierEquals("Mercator (variant A)",           new Mercator1SP().getName());
        assertEpsgIdentifierEquals("Latitude of natural origin",     Mercator1SP.LATITUDE_OF_ORIGIN.getName());
        assertEpsgIdentifierEquals("Longitude of natural origin",    Mercator1SP.CENTRAL_MERIDIAN  .getName());
        assertEpsgIdentifierEquals("Scale factor at natural origin", Mercator1SP.SCALE_FACTOR      .getName());
        assertEpsgIdentifierEquals("False easting",                  Mercator1SP.FALSE_EASTING     .getName());
        assertEpsgIdentifierEquals("False northing",                 Mercator1SP.FALSE_NORTHING    .getName());
    }
}
