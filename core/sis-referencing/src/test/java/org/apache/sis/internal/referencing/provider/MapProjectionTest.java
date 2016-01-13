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

import java.util.Iterator;
import org.opengis.util.GenericName;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;
import static org.apache.sis.internal.util.Constants.*;


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
        assertParamEquals(null, SEMI_MAJOR, true, MapProjection.SEMI_MAJOR);
        assertParamEquals(null, SEMI_MINOR, true, MapProjection.SEMI_MINOR);
    }

    /**
     * Verifies some parameters of {@link Equirectangular}. Note that {@code Equirectangular} is the first projection
     * to be loaded by {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory} and defines
     * some parameters which will be reused by other projections.
     *
     * <p><b>Note:</b> there is no test for {@code Equirectangular.createMathTransform(…)} in this class because
     * the math transforms are tested in the {@link org.apache.sis.referencing.operation.projection} package.</p>
     */
    @Test
    @DependsOnMethod("testSemiAxes")
    public void testEquirectangular() {
        final Iterator<GeneralParameterDescriptor> it = Equirectangular.PARAMETERS.descriptors().iterator();
        assertParamEquals("Equidistant Cylindrical (Spherical)", "Equirectangular",    true,  Equirectangular.PARAMETERS);
        assertParamEquals(null,                                   SEMI_MAJOR,          true,  it.next());
        assertParamEquals(null,                                   SEMI_MINOR,          true,  it.next());
        assertParamEquals("Latitude of 1st standard parallel",    STANDARD_PARALLEL_1, true,  it.next());
        assertParamEquals("Latitude of natural origin",           LATITUDE_OF_ORIGIN,  false, it.next());
        assertParamEquals("Longitude of natural origin",          CENTRAL_MERIDIAN,    true,  it.next());
        assertParamEquals("False easting",                        FALSE_EASTING,       true,  it.next());
        assertParamEquals("False northing",                       FALSE_NORTHING,      true,  it.next());
        assertFalse(it.hasNext());
        assertIsForcedToZero((ParameterDescriptor<?>) Equirectangular.PARAMETERS.descriptor(LATITUDE_OF_ORIGIN));
    }

    /**
     * Verifies some {@link Mercator1SP} parameter descriptors.
     */
    @Test
    @DependsOnMethod("testEquirectangular")
    public void testMercator1SP() {
        final Iterator<GeneralParameterDescriptor> it = Mercator1SP.PARAMETERS.descriptors().iterator();
        assertParamEquals("Mercator (variant A)",          "Mercator_1SP",       true, Mercator1SP.PARAMETERS);
        assertParamEquals(null,                             SEMI_MAJOR,          true, it.next());
        assertParamEquals(null,                             SEMI_MINOR,          true, it.next());
        assertParamEquals("Latitude of natural origin",     LATITUDE_OF_ORIGIN,  true, it.next());
        assertParamEquals("Longitude of natural origin",    CENTRAL_MERIDIAN,    true, it.next());
        assertParamEquals("Scale factor at natural origin", SCALE_FACTOR,        true, it.next());
        assertParamEquals("False easting",                  FALSE_EASTING,       true, it.next());
        assertParamEquals("False northing",                 FALSE_NORTHING,      true, it.next());
        assertFalse(it.hasNext());
        assertIsForcedToZero((ParameterDescriptor<?>) Mercator1SP.PARAMETERS.descriptor(LATITUDE_OF_ORIGIN));
    }

    /**
     * Verifies some {@link Mercator2SP} parameter descriptors.
     */
    @Test
    @DependsOnMethod("testMercator1SP")
    public void testMercator2SP() {
        final Iterator<GeneralParameterDescriptor> it = Mercator2SP.PARAMETERS.descriptors().iterator();
        assertParamEquals("Mercator (variant B)",             "Mercator_2SP",        true,  Mercator2SP.PARAMETERS);
        assertParamEquals(null,                                SEMI_MAJOR,           true,  it.next());
        assertParamEquals(null,                                SEMI_MINOR,           true,  it.next());
        assertParamEquals("Latitude of 1st standard parallel", STANDARD_PARALLEL_1,  true,  it.next());
        assertParamEquals("Latitude of natural origin",        LATITUDE_OF_ORIGIN,   false, it.next());
        assertParamEquals("Longitude of natural origin",       CENTRAL_MERIDIAN,     true,  it.next());
        assertParamEquals(null,                                SCALE_FACTOR,         false, it.next());
        assertParamEquals("False easting",                     FALSE_EASTING,        true,  it.next());
        assertParamEquals("False northing",                    FALSE_NORTHING,       true,  it.next());
        assertFalse(it.hasNext());
        assertIsForcedToZero((ParameterDescriptor<?>) Mercator1SP.PARAMETERS.descriptor(LATITUDE_OF_ORIGIN));
    }

    /**
     * Asserts that the primary name of the given parameter is the given name in the EPSG namespace.
     * Then asserts that the first alias (ignoring other EPSG alias) of the given parameter is the
     * given name in the OGC namespace.
     */
    private static void assertParamEquals(final String epsgName, final String ogcName, final boolean isMandatory,
            final GeneralParameterDescriptor actual)
    {
        if (epsgName != null) {
            assertEpsgIdentifierEquals(epsgName, actual.getName());
        } else {
            assertOgcIdentifierEquals(ogcName, actual.getName());
        }
        assertEquals("minimumOccurs", isMandatory ? 1 : 0, actual.getMinimumOccurs());
        if (epsgName != null) {
            for (final GenericName alias : actual.getAlias()) {
                if (alias instanceof ReferenceIdentifier && ((ReferenceIdentifier) alias).getAuthority() != Citations.EPSG) {
                    assertOgcIdentifierEquals(ogcName, (ReferenceIdentifier) alias);
                    return;
                }
            }
            fail("OGC alias not found.");
        }
    }

    /**
     * Asserts the the given parameter forces its value to zero.
     * This test is mostly for {@link Equirectangular#LATITUDE_OF_ORIGIN}.
     */
    private static void assertIsForcedToZero(final ParameterDescriptor<?> parameter) {
        assertEquals("minimumValue", -0.0, parameter.getMinimumValue());
        assertEquals("maximumValue", +0.0, parameter.getMaximumValue());
    }
}
