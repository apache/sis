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

import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link Providers} and some consistency rules of all providers defined in this package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
@DependsOn({
    org.apache.sis.referencing.operation.DefaultOperationMethodTest.class,
    AffineTest.class,
    LongitudeRotationTest.class,
    MapProjectionTest.class
})
public final strictfp class ProvidersTest extends TestCase {
    /**
     * Returns all providers to test.
     */
    private static Class<?>[] methods() {
        return new Class<?>[] {
            Affine.class,
            GeographicOffsets.class,
            GeographicOffsets2D.class,
            VerticalOffset.class,
            LongitudeRotation.class,
            CoordinateFrameRotation.class,
            CoordinateFrameRotation2D.class,
            CoordinateFrameRotation3D.class,
            PositionVector7Param.class,
            PositionVector7Param2D.class,
            PositionVector7Param3D.class,
            GeocentricTranslation.class,
            GeocentricTranslation2D.class,
            GeocentricTranslation3D.class,
            GeographicToGeocentric.class,
            GeocentricToGeographic.class,
            Geographic3Dto2D.class,
            Geographic2Dto3D.class,
            Molodensky.class,
            AbridgedMolodensky.class,
            Equirectangular.class,
            Mercator1SP.class,
            Mercator2SP.class,
            MercatorSpherical.class,
            PseudoMercator.class,
            RegionalMercator.class,
            MillerCylindrical.class,
            LambertConformal1SP.class,
            LambertConformal2SP.class,
            LambertConformalWest.class,
            LambertConformalBelgium.class,
            LambertConformalMichigan.class,
            TransverseMercator.class,
            TransverseMercatorSouth.class,
            PolarStereographicA.class,
            PolarStereographicB.class,
            PolarStereographicC.class,
            PolarStereographicNorth.class,
            PolarStereographicSouth.class,
            ObliqueStereographic.class,
            NTv2.class,
            NADCON.class,
            FranceGeocentricInterpolation.class,
            MolodenskyInterpolation.class,
            Interpolation1D.class
        };
    }

    /**
     * Returns the subset of {@link #methods()} which are expected to support
     * {@link AbstractProvider#redimension(int, int)}.
     */
    private static Class<?>[] redimensionables() {
        return new Class<?>[] {
            Affine.class,
            LongitudeRotation.class,
            GeographicOffsets.class,
            GeographicOffsets2D.class,
            CoordinateFrameRotation2D.class,
            CoordinateFrameRotation3D.class,
            PositionVector7Param2D.class,
            PositionVector7Param3D.class,
            GeocentricTranslation2D.class,
            GeocentricTranslation3D.class,
            Molodensky.class,
            AbridgedMolodensky.class,
            FranceGeocentricInterpolation.class
        };
    }

    /**
     * Ensures that every parameter instance is unique. Actually this test is not strong requirement.
     * This is only for sharing existing resources by avoiding unnecessary objects duplication.
     *
     * @throws Exception if the instantiation of a service provider failed.
     */
    @Test
    public void ensureParameterUniqueness() throws Exception {
        final Map<GeneralParameterDescriptor, String> groupNames = new IdentityHashMap<GeneralParameterDescriptor, String>();
        final Map<GeneralParameterDescriptor, GeneralParameterDescriptor> parameters = new HashMap<GeneralParameterDescriptor, GeneralParameterDescriptor>();
        final Map<Object, Object> namesAndIdentifiers = new HashMap<Object, Object>();
        for (final Class<?> c : methods()) {
            final OperationMethod method = (OperationMethod) c.newInstance();
            final ParameterDescriptorGroup group = method.getParameters();
            final String operationName = group.getName().getCode();
            for (final GeneralParameterDescriptor param : group.descriptors()) {
                assertFalse("Parameter declared twice in the same group.",
                        operationName.equals(groupNames.put(param, operationName)));
                /*
                 * Ensure uniqueness of the parameter descriptor as a whole.
                 */
                final Identifier name = param.getName();
                Object existing = parameters.put(param, param);
                if (existing != null && existing != param) {
                    fail("Parameter “" + name.getCode() + "” defined in “" + operationName + '”'
                            + " was already defined in “" + groupNames.get(existing) + "”."
                            + " The same instance could be shared.");
                }
                /*
                 * Ensure uniqueness of each name and identifier.
                 */
                existing = namesAndIdentifiers.put(name, name);
                if (existing != null && existing != name) {
                    fail("The name of parameter “" + name.getCode() + "” defined in “" + operationName + '”'
                            + " was already defined elsewhere. The same instance could be shared.");
                }
                for (final GenericName alias : param.getAlias()) {
                    existing = namesAndIdentifiers.put(alias, alias);
                    if (existing != null && existing != alias) {
                        fail("Alias “" + alias + "” of parameter “" + name.getCode() + "” defined in “" + operationName + '”'
                                + " was already defined elsewhere. The same instance could be shared.");
                    }
                }
                for (final Identifier id : param.getIdentifiers()) {
                    existing = namesAndIdentifiers.put(id, id);
                    if (existing != null && existing != id) {
                        fail("Identifier “" + id + "” of parameter “" + name.getCode() + "” defined in “" + operationName + '”'
                                + " was already defined elsewhere. The same instance could be shared.");
                    }
                }
            }
        }
    }

    /**
     * Tests {@link AbstractProvider#redimension(int, int)} on all providers managed by {@link Providers}.
     */
    @Test
    public void testRedimension() {
        final Map<Class<?>,Boolean> redimensionables = new HashMap<Class<?>,Boolean>(100);
        for (final Class<?> type : methods()) {
            assertNull(type.getName(), redimensionables.put(type, Boolean.FALSE));
        }
        for (final Class<?> type : redimensionables()) {
            assertEquals(type.getName(), Boolean.FALSE, redimensionables.put(type, Boolean.TRUE));
        }
        final Providers providers = new Providers();
        for (final OperationMethod method : providers) {
            if (method instanceof ProviderMock) {
                continue;                           // Skip the methods that were defined only for test purpose.
            }
            final int sourceDimensions = method.getSourceDimensions();
            final int targetDimensions = method.getTargetDimensions();
            final Boolean isRedimensionable = redimensionables.get(method.getClass());
            assertNotNull(method.getClass().getName(), isRedimensionable);
            if (isRedimensionable) {
                for (int newSource = 2; newSource <= 3; newSource++) {
                    for (int newTarget = 2; newTarget <= 3; newTarget++) {
                        final OperationMethod redim = ((DefaultOperationMethod) method).redimension(newSource, newTarget);
                        assertEquals("sourceDimensions", newSource, redim.getSourceDimensions().intValue());
                        assertEquals("targetDimensions", newTarget, redim.getTargetDimensions().intValue());
                        if (!(method instanceof Affine)) {
                            if (newSource == sourceDimensions && newTarget == targetDimensions) {
                                assertSame("When asking the original number of dimensions, expected the original instance.", method, redim);
                            } else {
                                assertNotSame("When asking a different number of dimensions, expected a different instance.", method, redim);
                            }
                            assertSame("When asking the original number of dimensions, expected the original instance.",
                                    method, ((DefaultOperationMethod) redim).redimension(sourceDimensions, targetDimensions));
                        }
                    }
                }
            } else try {
                ((DefaultOperationMethod) method).redimension(sourceDimensions ^ 1, targetDimensions ^ 1);
                fail("Type " + method.getClass().getName() + " is not in our list of redimensionable methods.");
            } catch (IllegalArgumentException e) {
                final String message = e.getMessage();
                assertTrue(message, message.contains(method.getName().getCode()));
            }
        }
    }
}
