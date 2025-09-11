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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.factory.sql.EPSGFactory;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.abort;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
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
        final var groupNames = new IdentityHashMap<GeneralParameterDescriptor, String>();
        final var parameters = new HashMap<GeneralParameterDescriptor, GeneralParameterDescriptor>();
        final var namesAndIdentifiers = new HashMap<Object, Object>();
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

    /**
     * Compares the method and parameter names against the declarations in the <abbr>EPSG</abbr> database.
     *
     * @throws ReflectiveOperationException if the instantiation of a service provider failed.
     * @throws FactoryException if an error occurred while using the <abbr>EPSG</abbr> database.
     */
    @Test
    public void compareWithEPSG() throws ReflectiveOperationException, FactoryException {
        assumeTrue(RUN_EXTENSIVE_TESTS, "Extensive tests not enabled.");
        final EPSGFactory factory;
        try {
            factory = (EPSGFactory) CRS.getAuthorityFactory(Constants.EPSG);
        } catch (ClassCastException e) {
            abort("This test requires the EPSG geodetic dataset.");
            throw e;
        }
        final var methodAliases   = new HashMap<AbstractProvider, String[]>(256);
        final var aliasUsageCount = new HashMap<String, Integer>(256);
        for (final Class<?> c : methods()) {
            final AbstractProvider method = instance(c);
            final String identifier = getCodeEPSG(method);
            if (identifier != null) {
                final OperationMethod authoritative = factory.createOperationMethod(identifier);
                final String[] aliases = getAliases(authoritative);
                for (final String alias : aliases) {
                    aliasUsageCount.merge(alias, 1, Math::addExact);
                }
                /*
                 * Verify that the name of the operation method is identical to the name used in the EPSG database.
                 * Aliases will be checked later, after we know which aliases are used multiple times.
                 */
                final String classe = c.getName();
                assertNull(methodAliases.put(method, aliases), classe);
                assertEquals(authoritative.getName().getCode(), method.getName().getCode(), classe);
                /*
                 * Verify that all parameters declared in the EPSG database are present with an identical name.
                 * The Apache SIS's method provider may contain additional parameters. They will be ignored.
                 */
                int index = 0;
                final List<GeneralParameterDescriptor> parameters = method.getParameters().descriptors();
                for (GeneralParameterDescriptor expected : authoritative.getParameters().descriptors()) {
                    final String name = expected.getName().getCode();
                    GeneralParameterDescriptor parameter;
                    do {
                        if (index >= parameters.size()) {
                            fail("Parameter \"" + name + "\" not found or not in expected order in class " + classe);
                        }
                        parameter = parameters.get(index++);
                    } while (!name.equals(parameter.getName().getCode()));
                    /*
                     * Found a match. The EPSG code must be identical.
                     * Check also the aliases, ignoring the deprecated ones.
                     */
                    assertEquals(getCodeEPSG(expected), getCodeEPSG(parameter), name);
                    final var hardCoded = new HashSet<String>(Arrays.asList(getAliases(parameter)));
                    for (final String alias : getAliases(expected)) {
                        assertTrue(hardCoded.remove(alias),
                                () -> "Alias \"" + alias + "\" not found in parameter \"" + name + "\" of class " + classe);
                    }
                    assertTrue(hardCoded.isEmpty(),
                            () -> "Unexpected alias \"" + hardCoded.iterator().next()
                                    + "\" in parameter \"" + name + "\" of class " + classe);
                }
            }
        }
        /*
         * AFter we checked all operation methods, execute a second loop for checking method aliases.
         * We need to ignore the aliases that are used by more than one method.
         */
        for (final Map.Entry<AbstractProvider, String[]> entry : methodAliases.entrySet()) {
            final AbstractProvider method = entry.getKey();
            final String classe = method.getClass().getName();
            final var hardCoded = new HashSet<String>(Arrays.asList(getAliases(method)));
            for (final String alias : entry.getValue()) {
                if (aliasUsageCount.get(alias) == 1) {
                    assertTrue(hardCoded.remove(alias), () -> "Alias \"" + alias + "\" not found in class " + classe);
                }
            }
            assertTrue(hardCoded.isEmpty(),
                    () -> "Unexpected alias \"" + hardCoded.iterator().next() + "\" in " + classe);
        }
    }

    /**
     * Returns the identifier code in <abbr>EPSG</abbr> namespace for the given object, or {@code null} if none.
     */
    private static String getCodeEPSG(final IdentifiedObject object) {
        Identifier identifier = IdentifiedObjects.getIdentifier(object, Citations.EPSG);
        return (identifier != null) ? identifier.getCode() : null;
    }

    /**
     * Returns the collection of <abbr>EPSG</abbr> aliases or abbreviations for the given object.
     */
    private static String[] getAliases(final IdentifiedObject object) {
        return object.getAlias()
                .stream()
                .filter((alias) -> alias.scope().name().toString().startsWith(Constants.EPSG))
                .map(GenericName::toString)
                .toArray(String[]::new);
    }
}
