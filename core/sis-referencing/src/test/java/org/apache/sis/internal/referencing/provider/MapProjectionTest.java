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

import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.test.ReferencingAssert;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


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
        assertOgcNameEquals(Constants.SEMI_MAJOR, MapProjection.SEMI_MAJOR);
        assertOgcNameEquals(Constants.SEMI_MINOR, MapProjection.SEMI_MINOR);
    }

    /**
     * Verifies some {@link Mercator1SP} parameter descriptors.
     */
    @Test
    public void testMercator1SP() {
        assertEpsgNameEquals("Mercator (variant A)",           Mercator1SP.PARAMETERS);
        assertEpsgNameEquals("Latitude of natural origin",     Mercator1SP.LATITUDE_OF_ORIGIN);
        assertEpsgNameEquals("Longitude of natural origin",    Mercator1SP.CENTRAL_MERIDIAN);
        assertEpsgNameEquals("Scale factor at natural origin", Mercator1SP.SCALE_FACTOR);
        assertEpsgNameEquals("False easting",                  Mercator1SP.FALSE_EASTING);
        assertEpsgNameEquals("False northing",                 Mercator1SP.FALSE_NORTHING);

        assertOgcAliasEquals("Mercator_1SP",       Mercator1SP.PARAMETERS);
        assertOgcAliasEquals("latitude_of_origin", Mercator1SP.LATITUDE_OF_ORIGIN);
        assertOgcAliasEquals("central_meridian",   Mercator1SP.CENTRAL_MERIDIAN);
        assertOgcAliasEquals("scale_factor",       Mercator1SP.SCALE_FACTOR);
        assertOgcAliasEquals("false_easting",      Mercator1SP.FALSE_EASTING);
        assertOgcAliasEquals("false_northing",     Mercator1SP.FALSE_NORTHING);
    }

    /**
     * Verifies some {@link Mercator2SP} parameter descriptors.
     */
    @Test
    public void testMercator2SP() {
        assertEpsgNameEquals("Mercator (variant B)",              Mercator2SP.PARAMETERS);
        assertEpsgNameEquals("Latitude of 1st standard parallel", Mercator2SP.STANDARD_PARALLEL);
        assertEpsgNameEquals("Latitude of natural origin",        Mercator2SP.LATITUDE_OF_ORIGIN);
        assertEpsgNameEquals("Longitude of natural origin",       Mercator2SP.CENTRAL_MERIDIAN);
        assertEpsgNameEquals("Scale factor at natural origin",    Mercator2SP.SCALE_FACTOR);
        assertEpsgNameEquals("False easting",                     Mercator2SP.FALSE_EASTING);
        assertEpsgNameEquals("False northing",                    Mercator2SP.FALSE_NORTHING);

        assertOgcAliasEquals("Mercator_2SP",        Mercator2SP.PARAMETERS);
        assertOgcAliasEquals("standard_parallel_1", Mercator2SP.STANDARD_PARALLEL);
        assertOgcAliasEquals("latitude_of_origin",  Mercator2SP.LATITUDE_OF_ORIGIN);
        assertOgcAliasEquals("central_meridian",    Mercator2SP.CENTRAL_MERIDIAN);
        assertOgcAliasEquals("scale_factor",        Mercator2SP.SCALE_FACTOR);
        assertOgcAliasEquals("false_easting",       Mercator2SP.FALSE_EASTING);
        assertOgcAliasEquals("false_northing",      Mercator2SP.FALSE_NORTHING);
    }

    /**
     * Asserts that the primary name of the given parameter is the given name in the EPSG namespace.
     */
    private static void assertEpsgNameEquals(final String expected, final GeneralParameterDescriptor actual) {
        ReferencingAssert.assertEpsgIdentifierEquals(expected, actual.getName());
    }

    /**
     * Asserts that the primary name of the given parameter is the given name in the OGC namespace.
     */
    private static void assertOgcNameEquals(final String expected, final GeneralParameterDescriptor actual) {
        ReferencingAssert.assertOgcIdentifierEquals(expected, actual.getName());
    }

    /**
     * Asserts that the first alias (ignoring other EPSG alias)
     * of the given parameter is the given name in the OGC namespace.
     */
    private static void assertOgcAliasEquals(final String expected, final GeneralParameterDescriptor actual) {
        for (final GenericName alias : actual.getAlias()) {
            if (alias instanceof Identifier && ((Identifier) alias).getAuthority() != Citations.OGP) {
                ReferencingAssert.assertOgcIdentifierEquals(expected, (Identifier) alias);
                return;
            }
        }
        fail("OGC alias not found.");
    }
}
