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
package org.apache.sis.storage.geotiff;

import java.util.Set;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Compares values declared in the {@link GeoKeys} class with values declared in Apache SIS operations.
 * Despite its name, this class is actually more a verification of GeoTIFF names and identifiers in the
 * {@link org.apache.sis.internal.referencing.provider} package than a verification of {@code GeoKeys}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class GeoKeysTest extends TestCase {
    /**
     * Tests {@link GeoKeys#name(short)}.
     */
    @Test
    public void testName() {
        assertEquals("Ellipsoid",  GeoKeys.name(GeoKeys.Ellipsoid));
        assertEquals("CenterLong", GeoKeys.name(GeoKeys.CenterLong));
    }

    /**
     * Verifies that GeoTIFF projection aliases registered in the {@link org.apache.sis.internal.referencing.provider}
     * package match the name of fields listed in {@link GeoIdentifiers} and that GeoTIFF numerical codes correspond.
     * This method verifies only projection names and identifiers, not parameter names.
     */
    @Test
    @DependsOnMethod("testName")
    public void verifyProjectionNames() {
        final MathTransformFactory factory = DefaultFactories.forBuildin(MathTransformFactory.class);
        for (final OperationMethod method : factory.getAvailableMethods(SingleOperation.class)) {
            final Identifier identifier = IdentifiedObjects.getIdentifier(method, Citations.GEOTIFF);
            final Set<String> names = IdentifiedObjects.getNames(method, Citations.GEOTIFF);
            /*
             * If there are no GeoTIFF identifiers, we should have no GeoTIFF name neither.
             * However we may have more than one name, since GeoTIFF defines also aliases.
             */
            assertEquals(method.getName().getCode(), identifier == null, names.isEmpty());
            if (identifier != null) {
                final int code = Short.parseShort(identifier.getCode());
                for (final String name : names) {
                    assertEquals(name, code, GeoIdentifiers.code(name));
                }
            }
        }
    }

    /**
     * Verifies that parameter names registered in the {@link org.apache.sis.internal.referencing.provider} package
     * match the name of fields listed in {@link GeoKeys}.
     */
    @Test
    @DependsOnMethod("testName")
    public void verifyParameterNames() {
        final MathTransformFactory factory = DefaultFactories.forBuildin(MathTransformFactory.class);
        for (final OperationMethod method : factory.getAvailableMethods(SingleOperation.class)) {
            for (final GeneralParameterDescriptor param : method.getParameters().descriptors()) {
                final Identifier identifier = IdentifiedObjects.getIdentifier(param, Citations.GEOTIFF);
                final Set<String> names = IdentifiedObjects.getNames(param, Citations.GEOTIFF);
                /*
                 * If there are no GeoTIFF identifiers, we should have no GeoTIFF name neither.
                 */
                assertEquals(param.getName().getCode(), identifier == null, names.isEmpty());
                if (identifier != null) {
                    final int code = Short.parseShort(identifier.getCode());
                    for (final String name : names) {
                        assertEquals(name, code(name), code);
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
            throw new IllegalArgumentException(e);
        }
    }
}
