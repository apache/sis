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
import com.esri.core.geometry.Polygon;
import org.opengis.geometry.Envelope;
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


/**
 * Tests {@link EnvelopeOperation}.
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(LinkOperationTest.class)
public final strictfp class EnvelopeOperationTest extends TestCase {
    /**
     * Creates a feature type with a bounds operation.
     * The feature contains the following properties:
     *
     * <ul>
     *   <li>{@code name} as a {@link String}</li>
     *   <li>{@code classes} as a {@link Polygon}</li>
     *   <li>{@code climbing wall} as a {@link Point}</li>
     *   <li>{@code gymnasium} as a {@link Polygon}</li>
     *   <li>{@code @geometry} as a link to the default geometry</li>
     *   <li>{@code bounds} as the feature envelope attribute.</li>
     * </ul>
     *
     * @param  defaultGeometry 1 for using "classes" as the default geometry, or 3 for "gymnasium".
     * @return the feature for a school.
     */
    private static DefaultFeatureType school(final int defaultGeometry) throws FactoryException {
        final DefaultAttributeType<?> standardCRS = new DefaultAttributeType<CoordinateReferenceSystem>(
                name(AttributeConvention.CRS_CHARACTERISTIC), CoordinateReferenceSystem.class, 1, 1, HardCodedCRS.WGS84_φλ);

        final DefaultAttributeType<?> normalizedCRS = new DefaultAttributeType<CoordinateReferenceSystem>(
                name(AttributeConvention.CRS_CHARACTERISTIC), CoordinateReferenceSystem.class, 1, 1, HardCodedCRS.WGS84);

        final AbstractIdentifiedType[] attributes = {
            new DefaultAttributeType<String> (name("name"),          String.class,  1, 1, null),
            new DefaultAttributeType<Polygon>(name("classes"),       Polygon.class, 1, 1, null, standardCRS),
            new DefaultAttributeType<Point>  (name("climbing wall"), Point.class,   1, 1, null, standardCRS),
            new DefaultAttributeType<Polygon>(name("gymnasium"),     Polygon.class, 1, 1, null, normalizedCRS),
            null,
            null
        };
        attributes[4] = FeatureOperations.link(name(AttributeConvention.DEFAULT_GEOMETRY_PROPERTY), attributes[defaultGeometry]);
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
        final AbstractIdentifiedType property = school(3).getProperty("bounds");
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
}
