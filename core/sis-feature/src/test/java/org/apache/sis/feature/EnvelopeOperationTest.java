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
package org.apache.sis.feature;

import java.util.Arrays;
import java.util.Map;
import java.util.Collections;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.Polygon;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.CharacteristicTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.referencing.CRS;
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.geometry.GeneralEnvelope;

// Test dependencies
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;

// Branch-dependent imports
import org.opengis.feature.PropertyType;


/**
 * Tests {@link EnvelopeOperation}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 0.7
 * @since   0.7
 * @module
 */
@DependsOn(LinkOperationTest.class)
public final strictfp class EnvelopeOperationTest extends TestCase {

    private static final AttributeType<CoordinateReferenceSystem> CRS_CHARACTERISTIC = new FeatureTypeBuilder()
            .addAttribute(CoordinateReferenceSystem.class)
            .setName(AttributeConvention.CRS_CHARACTERISTIC)
            .setMinimumOccurs(0)
            .build();

    /**
     * Creates a feature type with a bounds operation.
     * The feature contains the following properties:
     *
     * <ul>
     *   <li>{@code name} as a {@link String}</li>
     *   <li>{@code classes} as a {@link Polygon}</li>
     *   <li>{@code climbing wall} as a {@link Point}</li>
     *   <li>{@code gymnasium} as a {@link Polygon}</li>
     *   <li>{@code sis:geometry} as a link to the default geometry</li>
     *   <li>{@code bounds} as the feature envelope attribute.</li>
     * </ul>
     *
     * @param  defaultGeometry  1 for using "classes" as the default geometry, or 3 for "gymnasium".
     * @return the feature for a school.
     */
    private static DefaultFeatureType school(final int defaultGeometry) throws FactoryException {
        final DefaultAttributeType<?> standardCRS = new DefaultAttributeType<>(
                name(AttributeConvention.CRS_CHARACTERISTIC), CoordinateReferenceSystem.class, 1, 1, HardCodedCRS.WGS84_φλ);

        final DefaultAttributeType<?> normalizedCRS = new DefaultAttributeType<>(
                name(AttributeConvention.CRS_CHARACTERISTIC), CoordinateReferenceSystem.class, 1, 1, HardCodedCRS.WGS84);

        final PropertyType[] attributes = {
            new DefaultAttributeType<>(name("name"),          String.class,  1, 1, null),
            new DefaultAttributeType<>(name("classes"),       Polygon.class, 1, 1, null, standardCRS),
            new DefaultAttributeType<>(name("climbing wall"), Point.class,   1, 1, null, standardCRS),
            new DefaultAttributeType<>(name("gymnasium"),     Polygon.class, 1, 1, null, normalizedCRS),
            null,
            null
        };
        attributes[4] = FeatureOperations.link(name(AttributeConvention.GEOMETRY_PROPERTY), attributes[defaultGeometry]);
        attributes[5] = FeatureOperations.envelope(name("bounds"), null, attributes);
        return new DefaultFeatureType(name("school"), false, null, attributes);
    }

    /**
     * Creates a map of identification properties containing only an entry for the given name.
     */
    private static Map<String,?> name(final Object name) {
        return Collections.singletonMap(DefaultAttributeType.NAME_KEY, name);
    }

    /**
     * Tests the constructor. The set of attributes on which the operation depends shall include
     * "classes", "climbing wall" and "gymnasium" but not "name" since the later does not contain
     * a geometry. Furthermore the default CRS shall be {@code HardCodedCRS.WGS84}, not
     * {@code HardCodedCRS.WGS84_φλ}, because this test uses "gymnasium" as the default geometry.
     *
     * @throws FactoryException if an error occurred while searching for the coordinate operations.
     */
    @Test
    public void testConstruction() throws FactoryException {
        final PropertyType property = school(3).getProperty("bounds");
        assertInstanceOf("bounds", EnvelopeOperation.class, property);
        final EnvelopeOperation op = (EnvelopeOperation) property;
        assertSame("crs", HardCodedCRS.WGS84, op.crs);
        assertSetEquals(Arrays.asList("classes", "climbing wall", "gymnasium"), op.getDependencies());
    }

    /**
     * Implementation of the test methods.
     */
    private static void run(final AbstractFeature feature) {
        assertNull("Before a geometry is set", feature.getPropertyValue("bounds"));
        GeneralEnvelope expected;

        // Set one geometry
        Polygon classes = new Polygon();
        classes.startPath(10, 20);
        classes.lineTo(10, 30);
        classes.lineTo(15, 30);
        classes.lineTo(15, 20);
        feature.setPropertyValue("classes", classes);
        expected = new GeneralEnvelope(HardCodedCRS.WGS84_φλ);
        expected.setRange(0, 10, 15);
        expected.setRange(1, 20, 30);
        assertEnvelopeEquals(expected, (Envelope) feature.getPropertyValue("bounds"));

        // Set second geometry
        Point wall = new Point(18, 40);
        feature.setPropertyValue("climbing wall", wall);
        expected = new GeneralEnvelope(HardCodedCRS.WGS84_φλ);
        expected.setRange(0, 10, 18);
        expected.setRange(1, 20, 40);
        assertEnvelopeEquals(expected, (Envelope) feature.getPropertyValue("bounds"));

        // Set third geometry. This geometry has CRS axis order reversed.
        Polygon gymnasium = new Polygon();
        gymnasium.startPath(-5, -30);
        gymnasium.lineTo(-6, -30);
        gymnasium.lineTo(-6, -31);
        gymnasium.lineTo(-5, -31);
        feature.setPropertyValue("gymnasium", gymnasium);
        expected = new GeneralEnvelope(HardCodedCRS.WGS84_φλ);
        expected.setRange(0, -31, 18);
        expected.setRange(1,  -6, 40);
        assertEnvelopeEquals(expected, (Envelope) feature.getPropertyValue("bounds"));
    }

    /**
     * Tests a dense type with operations.
     *
     * @throws FactoryException if an error occurred while searching for the coordinate operations.
     */
    @Test
    @DependsOnMethod("testConstruction")
    public void testDenseFeature() throws FactoryException {
        run(new DenseFeature(school(1)));
    }

    /**
     * Tests a sparse feature type with operations.
     *
     * @throws FactoryException if an error occurred while searching for the coordinate operations.
     */
    @Test
    @DependsOnMethod("testConstruction")
    public void testSparseFeature() throws FactoryException {
        run(new SparseFeature(school(2)));
    }

    /**
     * If no characteristic is defined on properties, but geometries define different ones, we should return an
     * error, because it is an ambiguous case (Note: In the future, we could try to push them all in a
     * {@link CRS#suggestCommonTarget(GeographicBoundingBox, CoordinateReferenceSystem...) common space}.
     */
    @Test
    public void no_characteristic_but_different_geometry_crs() {
        try {
            final Envelope env = new CRSManagementUtil().test(HardCodedCRS.WGS84, HardCodedCRS.NTF);
            fail("Ambiguity in CRS should have caused an error,  a value has been returned: "+env);
        } catch (IllegalStateException e) {
            // Expected behavior
        }
    }

    /**
     * When CRS is not in characteristics, but can be found on geometries, returned envelope should match it.
     */
    @Test
    public void same_crs_on_geometries() {
        final Envelope env = new CRSManagementUtil().test(HardCodedCRS.WGS84, HardCodedCRS.WGS84);
        assertEquals(HardCodedCRS.WGS84, env.getCoordinateReferenceSystem());
    }

    /**
     * When referencing is defined neither in characteristics nor on geometries, we should assume all geometries are
     * expressed in the same space. Therefore, an envelope with no CRS should be returned.
     */
    @Test
    public void no_crs_defined() {
        Envelope env = new CRSManagementUtil().test(null, null);
        assertNull(env.getCoordinateReferenceSystem());
    }

    /**
     * Ensure that returned envelope CRS is the default one specified by property type characteristics if no geometry
     * defines its CRS.
     */
    @Test
    public void feature_type_characteristic_defines_crs() {
        final Envelope env = new CRSManagementUtil(HardCodedCRS.WGS84, false, HardCodedCRS.WGS84, false)
                .test(null, null);
        assertEquals(HardCodedCRS.WGS84, env.getCoordinateReferenceSystem());
    }

    @Test
    public void feature_characteristic_define_crs() {
        final CRSManagementUtil environment = new CRSManagementUtil(null, true, null, true);
        Envelope env = environment
                .test(HardCodedCRS.WGS84, true, HardCodedCRS.WGS84, true);
        assertEquals(HardCodedCRS.WGS84, env.getCoordinateReferenceSystem());

        try {
            env = environment.test(HardCodedCRS.WGS84, true, HardCodedCRS.NTF, true);
            fail("Envelope should not be computed due to different CRS in geometries: "+env);
        } catch (IllegalStateException e) {
            // expected behavior
        }
    }

    private static class CRSManagementUtil {
        final FeatureType type;

        CRSManagementUtil() {
            this(null, false, null, false);
        }

        /**
         * Create a feature type containing two geometric fields. If given CRS are non null, they will be specified as
         * default CRS of each field through property type CRS characteristic.
         * @param defaultCrs1 Default CRS of first property
         * @param forceCharacteristic1 True if we want a CRS characteristic even with a null CRS. False to omit
         *                             characteristic i defaultCrs1 is null.
         * @param defaultCrs2 Default CRS for second property
         * @param forceCharacteristic2 True if we want a CRS characteristic even with a null CRS. False to omit
         *                             characteristic i defaultCrs2 is null.
         */
        CRSManagementUtil(final CoordinateReferenceSystem defaultCrs1, boolean forceCharacteristic1, final CoordinateReferenceSystem defaultCrs2, boolean forceCharacteristic2) {
            final FeatureTypeBuilder builder = new FeatureTypeBuilder().setName("test");
            final AttributeTypeBuilder<GeometryWrapper> g1 = builder.addAttribute(GeometryWrapper.class).setName("g1");
            if (defaultCrs1 != null || forceCharacteristic1) g1.setCRS(defaultCrs1);
            g1.addRole(AttributeRole.DEFAULT_GEOMETRY);

            final AttributeTypeBuilder<GeometryWrapper> g2 = builder.addAttribute(GeometryWrapper.class).setName("g2");
            if (defaultCrs2 != null || forceCharacteristic2) g2.setCRS(defaultCrs2);

            type = builder.build();
        }

        /**
         * Compute the envelope of this feature, and ensure that lower/upper coordinates are well-defined.
         * The result is returned, so user can check the coordinate reference system on it.
         *
         * @param c1 CRS to put on the first geometry (not on property characteristic, but geometry itself)
         * @param c2 CRS to put on the second geometry (not on property characteristic, but geometry itself)
         * @return A non null envelope, result of the envelope operation.
         */
        Envelope test(final CoordinateReferenceSystem c1, final CoordinateReferenceSystem c2) {
            return test(c1, false, c2, false);
        }

        Envelope test(final CoordinateReferenceSystem c1, final boolean c1AsCharacteristic, final CoordinateReferenceSystem c2, final boolean c2AsCharacteristic) {
            final GeometryWrapper g1 = Geometries.wrap(new Point(4, 4))
                    .orElseThrow(() -> new IllegalStateException("Cannot load ESRI binding"));
            final GeometryWrapper g2 = Geometries.wrap(new Polyline(new Point(2, 2), new Point(3, 3)))
                    .orElseThrow(() -> new IllegalStateException("Cannot load ESRI binding"));

            Feature f = type.newInstance();
            set(f, "g1", g1, c1, c1AsCharacteristic);
            set(f, "g2", g2, c2, c2AsCharacteristic);

            Object result = f.getPropertyValue("sis:envelope");
            assertNotNull(result);
            assertTrue(result instanceof Envelope);
            Envelope env = (Envelope) result;
            assertArrayEquals(new double[]{2, 2}, env.getLowerCorner().getCoordinate(), 1e-4);
            assertArrayEquals(new double[]{4, 4}, env.getUpperCorner().getCoordinate(), 1e-4);
            return env;
        }

        private void set(final Feature target, final String propertyName, final GeometryWrapper geometry, final CoordinateReferenceSystem crs, final boolean asCharacteristic) {
            if (asCharacteristic) {
                final Attribute g1p = (Attribute) target.getProperty(propertyName);
                final Attribute<CoordinateReferenceSystem> crsCharacteristic = CRS_CHARACTERISTIC.newInstance();
                crsCharacteristic.setValue(crs);
                g1p.characteristics().put(AttributeConvention.CRS_CHARACTERISTIC.toString(), crsCharacteristic);
                g1p.setValue(geometry);
            } else {
                geometry.setCoordinateReferenceSystem(crs);
                target.setPropertyValue(propertyName, geometry);
            }
        }
    }
}
