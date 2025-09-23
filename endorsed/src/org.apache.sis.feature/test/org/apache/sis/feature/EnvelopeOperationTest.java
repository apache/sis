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

import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.Geometry;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.referencing.Assertions.assertEnvelopeEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;


/**
 * Tests {@link EnvelopeOperation}.
 * This test uses a feature with two geometric properties, named "g1" and "g2",
 * optionally associated with a default CRS declared in attribute characteristics.
 * This class tests different ways to declare the CRS and tests the case where the
 * CRSs are not the same.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class EnvelopeOperationTest extends TestCase {
    /**
     * The description of a feature with two geometric properties. The properties are named "g1" and "g2"
     * and may or may not have default CRS, depending which {@code initialize(…)} method is invoked.
     *
     * @see #initialize()
     * @see #initialize(CoordinateReferenceSystem, boolean, CoordinateReferenceSystem, boolean)
     */
    private FeatureType type;

    /**
     * The feature created by a test method. Saved for allowing additional checks or operations.
     */
    private Feature feature;

    /**
     * Creates a new test case.
     */
    public EnvelopeOperationTest() {
    }

    /**
     * Creates the feature type with two geometric properties without default CRS.
     *
     * @see #initialize(CoordinateReferenceSystem, boolean, CoordinateReferenceSystem, boolean)
     */
    private void initialize() {
        initialize(null, false, null, false);
    }

    /**
     * Creates a feature type containing two geometric properties in the specified CRSs, which may be null.
     * They will be specified as default CRS of each property through property type CRS characteristic only
     * if the corresponding {@code declareCRS} flag is true. The first geometry will be the default one.
     *
     * @param defaultCRS1        default CRS of first property (may be {@code null}).
     * @param defaultCRS2        default CRS of second property (may be {@code null}).
     * @param asCharacteristic1  whether to declare CRS 1 as a characteristic of first property.
     * @param asCharacteristic2  whether to declare CRS 2 as a characteristic of second property.
     */
    private void initialize(final CoordinateReferenceSystem defaultCRS1, boolean asCharacteristic1,
                            final CoordinateReferenceSystem defaultCRS2, boolean asCharacteristic2)
    {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder().setName("test");
        final AttributeTypeBuilder<?> g1 = builder.addAttribute(GeometryWrapper.class).setName("g1");
        final AttributeTypeBuilder<?> g2 = builder.addAttribute(GeometryWrapper.class).setName("g2");
        if (asCharacteristic1) g1.setCRS(defaultCRS1);
        if (asCharacteristic2) g2.setCRS(defaultCRS2);
        g1.addRole(AttributeRole.DEFAULT_GEOMETRY);
        type = builder.build();
    }

    /**
     * Sets the two properties to arbitrary geometries in given CRS, then computes the envelope.
     * The CRS are set directly on the geometry objects, not in the attribute characteristics.
     * The two geometries are:
     *
     * <ul>
     *   <li>A point at (4 7)</li>
     *   <li>A polyline in envelope from lower corner (12 15) to upper corner (17 15).
     * </ul>
     *
     * @param  crs1  CRS to associate to the first geometry  (not on property characteristic, but geometry itself).
     * @param  crs2  CRS to associate to the second geometry (not on property characteristic, but geometry itself).
     * @return a non null envelope, result of the envelope operation.
     */
    private Envelope compute(final CoordinateReferenceSystem crs1,
                             final CoordinateReferenceSystem crs2)
    {
        return compute(crs1, false, crs2, false);
    }

    /**
     * Sets the two properties to arbitrary geometries in given CRS, then computes the envelope.
     * The CRS are set either on geometry objects or on the attribute characteristics, depending
     * on the {@code asCharacteristic} flags.
     *
     * @param  crs1  CRS to associate to the first geometry  (either directly or indirectly).
     * @param  crs2  CRS to associate to the second geometry (either directly or indirectly).
     * @return a non null envelope, result of the envelope operation.
     */
    private Envelope compute(final CoordinateReferenceSystem crs1, final boolean asCharacteristic1,
                             final CoordinateReferenceSystem crs2, final boolean asCharacteristic2)
    {
        feature = type.newInstance();
        set("g1", crs1, asCharacteristic1, new Point(4, 7));
        set("g2", crs2, asCharacteristic2, new Polyline(new Point(12, 15), new Point(17, 14)));
        final Object result = feature.getPropertyValue("sis:envelope");
        assertInstanceOf(Envelope.class, result, "sis:envelope");
        return (Envelope) result;
    }

    /**
     * Sets a geometric property value together with its CRS, either directly or indirectly through
     * attribute characteristic.
     *
     * @param  propertyName      name of the property on which to set the CRS.
     * @param  crs               the CRS to set on the geometry.
     * @param  asCharacteristic  whether to associate the CRS as a characteristic or directly on the geometry.
     * @param  geometry          the ESRI geometry value to store in the property.
     */
    private void set(final String propertyName, final CoordinateReferenceSystem crs,
                     final boolean asCharacteristic, final Geometry geometry)
    {
        final GeometryWrapper wrapper = Geometries.wrap(geometry).orElseThrow(
                    () -> new IllegalStateException("Cannot load ESRI binding"));

        if (asCharacteristic) {
            @SuppressWarnings("unchecked")
            final var property = (Attribute<GeometryWrapper>) feature.getProperty(propertyName);
            final var crsCharacteristic = Features.cast(
                    property.getType().characteristics().get(AttributeConvention.CRS),
                    CoordinateReferenceSystem.class).newInstance();
            crsCharacteristic.setValue(crs);
            property.characteristics().put(AttributeConvention.CRS, crsCharacteristic);
            property.setValue(wrapper);
        } else {
            wrapper.setCoordinateReferenceSystem(crs);
            feature.setPropertyValue(propertyName, wrapper);
        }
    }

    /**
     * Verifies that two geometries using the same CRS, without any CRS declared as the default one, can be combined.
     * The CRS is not declared in characteristics but can be found on geometries, so returned envelope should use it.
     * The expected envelope is {@code BOX(4 7, 17 15)}
     */
    @Test
    public void same_crs_on_geometries() {
        initialize();
        final Envelope result = compute(HardCodedCRS.WGS84, HardCodedCRS.WGS84);
        final Envelope expected = new Envelope2D(HardCodedCRS.WGS84, 4, 7, 13, 8);
        assertSame(HardCodedCRS.WGS84, result.getCoordinateReferenceSystem());
        assertEnvelopeEquals(expected, result);
    }

    /**
     * Verifies that two geometries using the same CRS, specified as characteristics, can be combined.
     * This tests ensures that envelope CRS is the default one specified by property type characteristics.
     */
    @Test
    public void same_crs_on_characteristic() {
        initialize(HardCodedCRS.WGS84, true, HardCodedCRS.WGS84, true);
        final Envelope result = compute(null, null);
        final Envelope expected = new Envelope2D(HardCodedCRS.WGS84, 4, 7, 13, 8);
        assertSame(HardCodedCRS.WGS84, result.getCoordinateReferenceSystem());
        assertEnvelopeEquals(expected, result);
    }

    /**
     * Verifies that two geometries using different CRS, without any CRS declared as the default one,
     * are combined using a common CRS. The difference between the two CRS is only a change of axis order.
     * The expected envelope is {@code BOX(4 7, 15 17)} where the upper corner (15 17) was (17 15) in
     * the original geometry (before the change of CRS has been applied).
     */
    @Test
    public void different_crs_on_geometries() {
        initialize();
        final Envelope result = compute(HardCodedCRS.WGS84, HardCodedCRS.WGS84_LATITUDE_FIRST);
        final Envelope expected = new Envelope2D(HardCodedCRS.WGS84, 4, 7, 11, 10);
        assertSame(HardCodedCRS.WGS84, result.getCoordinateReferenceSystem());
        assertEnvelopeEquals(expected, result);
    }

    /**
     * Verifies that two geometries using different CRS, specified as characteristics, can be combined.
     */
    @Test
    public void different_crs_on_characteristic() {
        initialize(null, true, null, true);
        final Envelope result = compute(HardCodedCRS.WGS84, true, HardCodedCRS.WGS84, true);
        final Envelope expected = new Envelope2D(HardCodedCRS.WGS84, 4, 7, 13, 8);
        assertSame(HardCodedCRS.WGS84, result.getCoordinateReferenceSystem());
        assertEnvelopeEquals(expected, result);
    }

    /**
     * Verifies attempts to compute envelope when geometries have unspecified CRS.
     * If the feature has two or more geometries, the operation should fail because of ambiguity.
     * If the feature has only one geometry, its envelope should be returned with a null CRS.
     */
    @Test
    public void unspecified_crs() {
        initialize();
        var exception = assertThrows(FeatureOperationException.class, () -> compute(null, null));
        assertMessageContains(exception);
        feature.setPropertyValue("g2", null);
        final Envelope result = (Envelope) feature.getPropertyValue("sis:envelope");
        assertNull(result.getCoordinateReferenceSystem());
        assertEnvelopeEquals(new Envelope2D(null, 4, 7, 0, 0), result);
    }

    /**
     * Verifies attempts to compute envelope when only one geometry has unspecified CRS.
     * The operation should fail because of ambiguity.
     */
    @Test
    public void partially_unspecified_crs() {
        initialize();
        var exception = assertThrows(FeatureOperationException.class, () -> compute(null, HardCodedCRS.WGS84));
        assertMessageContains(exception);
    }
}
