/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License)); Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing)); software
 * distributed under the License is distributed on an "AS IS" BASIS));
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND)); either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.geotiff.base;

import java.util.Set;
import java.lang.reflect.Field;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Compares values declared in the {@link GeoKeys} class with values declared in Apache SIS operations.
 * Despite its name, this class is actually more a verification of GeoTIFF names and identifiers in the
 * {@code org.apache.sis.referencing.operation.provider} package than a verification of {@code GeoKeys}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GeoKeysTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public GeoKeysTest() {
    }

    /**
     * Tests {@link GeoKeys#name(short)}.
     */
    @Test
    public void testName() {
        assertEquals("Ellipsoid",  GeoKeys.name(GeoKeys.Ellipsoid));
        assertEquals("CenterLong", GeoKeys.name(GeoKeys.CenterLong));
    }

    /**
     * Verifies GeoTIFF projection aliases and identifiers. Verification includes:
     * <ul>
     *   <li>that GeoTIFF projection aliases registered in the {@code org.apache.sis.referencing.operation.provider}
     *       package match the name of fields listed in {@link GeoIdentifiers}, and</li>
     *   <li>that GeoTIFF numerical codes correspond.</li>
     * </ul>
     * This method verifies only projection names and identifiers, not parameter names.
     */
    @Test
    public void verifyProjectionNames() {
        final MathTransformFactory factory = DefaultMathTransformFactory.provider();
        for (final OperationMethod method : factory.getAvailableMethods(SingleOperation.class)) {
            final Identifier identifier = IdentifiedObjects.getIdentifier(method, Citations.GEOTIFF);
            final Set<String> names = IdentifiedObjects.getNames(method, Citations.GEOTIFF);
            /*
             * If there are no GeoTIFF identifiers, we should have no GeoTIFF name neither.
             * However, we may have more than one name, since GeoTIFF defines also aliases.
             */
            assertEquals(identifier == null, names.isEmpty(), method.getName().getCode());
            if (identifier != null) {
                final int code = Short.parseShort(identifier.getCode());
                for (final String name : names) {
                    assertEquals(code, GeoIdentifiers.code(name), name);
                }
            }
        }
    }

    /**
     * Verifies GeoTIFF projection parameters.
     * This method verifies that parameter names registered in the
     * {@code org.apache.sis.referencing.operation.provider} package
     * match the name of fields listed in {@link GeoKeys}.
     */
    @Test
    public void verifyParameterNames() {
        final MathTransformFactory factory = DefaultMathTransformFactory.provider();
        for (final OperationMethod method : factory.getAvailableMethods(SingleOperation.class)) {
            for (final GeneralParameterDescriptor param : method.getParameters().descriptors()) {
                final Identifier identifier = IdentifiedObjects.getIdentifier(param, Citations.GEOTIFF);
                final Set<String> names = IdentifiedObjects.getNames(param, Citations.GEOTIFF);
                /*
                 * If there are no GeoTIFF identifiers, we should have no GeoTIFF name neither.
                 */
                assertEquals(identifier == null, names.isEmpty(), param.getName().getCode());
                if (identifier != null) {
                    final int code = Short.parseShort(identifier.getCode());
                    for (final String name : names) {
                        assertEquals(code(name), code, name);
                    }
                }
            }
        }
    }

    /**
     * Returns the numerical value of the given GeoTIFF key name.
     * This method is the converse of {@link GeoKeys#name(short)}.
     */
    private static short code(final String name) {
        try {
            return GeoKeys.class.getField(name).getShort(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Verifies the value of {@link GeoCodes#NUM_GEOKEYS}.
     */
    @Test
    public void verifyNumKeys() {
        final Field[] fields = GeoKeys.class.getFields();       // Include only public fields.
        assertEquals(fields.length, GeoCodes.NUM_GEOKEYS);
    }
}
