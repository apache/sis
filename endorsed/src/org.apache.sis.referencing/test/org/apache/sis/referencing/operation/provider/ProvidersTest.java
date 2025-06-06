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
package org.apache.sis.referencing.operation.provider;

import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Assertions.assertBetween;


/**
 * Tests some consistency rules of all providers defined in this package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ProvidersTest extends TestCase {
    /**
     * Returns all providers to test.
     */
    private static Class<?>[] methods() {
        return new Class<?>[] {
            Affine.class,
            AxisOrderReversal.class,
            AxisOrderReversal3D.class,
            GeographicOffsets.class,
            GeographicOffsets2D.class,
            GeographicAndVerticalOffsets.class,
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
            GeocentricToTopocentric.class,
            GeographicToTopocentric.class,
            Geographic3Dto2D.class,
            Geographic2Dto3D.class,
            Spherical3Dto2D.class,
            Spherical2Dto3D.class,
            Molodensky.class,
            AbridgedMolodensky.class,
            PseudoPlateCarree.class,
            Equirectangular.class,
            Mercator1SP.class,
            Mercator2SP.class,
            MercatorSpherical.class,
            PseudoMercator.class,
            MercatorAuxiliarySphere.class,
            RegionalMercator.class,
            MillerCylindrical.class,
            LambertConformal1SP.class,
            LambertConformal2SP.class,
            LambertConformalWest.class,
            LambertConformalBelgium.class,
            LambertConformalMichigan.class,
            LambertCylindricalEqualArea.class,
            LambertCylindricalEqualAreaSpherical.class,
            LambertAzimuthalEqualArea.class,
            LambertAzimuthalEqualAreaSpherical.class,
            AlbersEqualArea.class,
            TransverseMercator.class,
            TransverseMercatorSouth.class,
            CassiniSoldner.class,
            HyperbolicCassiniSoldner.class,
            PolarStereographicA.class,
            PolarStereographicB.class,
            PolarStereographicC.class,
            PolarStereographicNorth.class,
            PolarStereographicSouth.class,
            ObliqueStereographic.class,
            ObliqueMercator.class,
            ObliqueMercatorCenter.class,
            ObliqueMercatorTwoPoints.class,
            ObliqueMercatorTwoPointsCenter.class,
            Orthographic.class,
            ModifiedAzimuthalEquidistant.class,
            AzimuthalEquidistantSpherical.class,
            EquidistantCylindrical.class,
            ZonedTransverseMercator.class,
            SatelliteTracking.class,
            Sinusoidal.class,
            PseudoSinusoidal.class,
            Polyconic.class,
            Mollweide.class,
            Robinson.class,
            SouthPoleRotation.class,
            NorthPoleRotation.class,
            NTv2.class,
            NTv1.class,
            NADCON.class,
            FranceGeocentricInterpolation.class,
            Interpolation1D.class,
            Wraparound.class
        };
    }

    /**
     * Creates a new test case.
     */
    public ProvidersTest() {
    }

    /**
     * Returns an instance of the operation method identified by the given class.
     *
     * @param  c  class of the operation method to instantiate.
     * @return an instance of the specified class.
     * @throws ReflectiveOperationException if the instantiation of a service provider failed.
     */
    private static AbstractProvider instance(final Class<?> c) throws ReflectiveOperationException {
        return (AbstractProvider) c.getConstructor((Class[]) null).newInstance((Object[]) null);
    }

    /**
     * Ensures that every parameter instance is unique. Actually this test is not strong requirement.
     * This is only for sharing existing resources by avoiding unnecessary objects duplication.
     *
     * @throws ReflectiveOperationException if the instantiation of a service provider failed.
     */
    @Test
    public void ensureParameterUniqueness() throws ReflectiveOperationException {
        final Map<GeneralParameterDescriptor, String> groupNames = new IdentityHashMap<>();
        final Map<GeneralParameterDescriptor, GeneralParameterDescriptor> parameters = new HashMap<>();
        final Map<Object, Object> namesAndIdentifiers = new HashMap<>();
        for (final Class<?> c : methods()) {
            final AbstractProvider method = instance(c);
            final ParameterDescriptorGroup group = method.getParameters();
            final String operationName = group.getName().getCode();
            for (final GeneralParameterDescriptor param : group.descriptors()) {
                if (operationName.equals(groupNames.put(param, operationName))) {
                    fail("Parameter declared twice in the same “" + operationName + "” group.");
                }
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
     * Performs some consistency checks on {@link AbstractProvider#minSourceDimension}.
     *
     * @throws ReflectiveOperationException if the instantiation of a service provider failed.
     */
    @Test
    public void validateMinSourceDimension() throws ReflectiveOperationException {
        for (final Class<?> c : methods()) {
            final AbstractProvider method = instance(c);
            final String name = c.getSimpleName();
            final int expected;
            if (method.sourceCSType == VerticalCS.class) {
                expected = 1;
            } else if (method instanceof MapProjection) {
                expected = 2;
            } else if (name.endsWith("1D")) {
                expected = 1;
            } else if (name.endsWith("2D")) {
                expected = name.contains("3D") ? 3 : 2;
            } else if (name.endsWith("3D")) {
                expected = name.contains("2D") ? 2 : 3;
            } else {
                assertBetween(1, 3, method.minSourceDimension, name);
                continue;
            }
            assertEquals(expected, method.minSourceDimension, name);
        }
    }

    /**
     * Tests the description provided in some parameters.
     */
    @Test
    public void testDescription() {
        assertNotEquals(0, SatelliteTracking.SATELLITE_ORBIT_INCLINATION.getDescription().orElseThrow().length());
        assertNotEquals(0, SatelliteTracking.SATELLITE_ORBITAL_PERIOD   .getDescription().orElseThrow().length());
        assertNotEquals(0, SatelliteTracking.ASCENDING_NODE_PERIOD      .getDescription().orElseThrow().length());
    }
}
