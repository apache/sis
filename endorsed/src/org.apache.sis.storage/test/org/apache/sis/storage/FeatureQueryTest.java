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
package org.apache.sis.storage;

import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import org.apache.sis.feature.Features;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.util.iso.Names;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSetEquals;
import static org.apache.sis.test.Assertions.assertSingleton;
import static org.apache.sis.test.Assertions.assertMessageContains;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.MatchAction;
import org.opengis.filter.SortOrder;
import org.opengis.filter.SortProperty;


/**
 * Tests {@link FeatureQuery} and (indirectly) {@link FeatureSubset}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class FeatureQueryTest extends TestCase {
    /**
     * An arbitrary number of features, all of the same type.
     */
    private Feature[] features;

    /**
     * The {@link #features} array wrapped in a in-memory feature set.
     */
    private FeatureSet featureSet;

    /**
     * The query to be executed.
     */
    private final FeatureQuery query;

    /**
     * Creates a new test case.
     */
    public FeatureQueryTest() {
        query = new FeatureQuery();
    }

    /**
     * Creates a simple feature with a property flagged as an identifier.
     */
    private void createFeatureWithIdentifier() {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder().setName("Test");
        ftb.addAttribute(String.class).setName("id").addRole(AttributeRole.IDENTIFIER_COMPONENT);
        final FeatureType type = ftb.build();
        features = new Feature[] {
            type.newInstance()
        };
        features[0].setPropertyValue("id", "id-0");
        featureSet = new MemoryFeatureSet(null, type, Arrays.asList(features));
    }

    /**
     * Creates a set of features common to most tests.
     * The feature type is composed of two attributes and one association.
     */
    private void createFeaturesWithAssociation() {
        FeatureTypeBuilder ftb;

        // A dependency of the test feature type.
        ftb = new FeatureTypeBuilder().setName("Dependency");
        ftb.addAttribute(Integer.class).setName("value3");
        final FeatureType dependency = ftb.build();

        // Test feature type with attributes and association.
        ftb = new FeatureTypeBuilder().setName("Test");
        ftb.addAttribute(Integer.class).setName("value1");
        ftb.addAttribute(Integer.class).setName("value2");
        ftb.addAssociation(dependency).setName("dependency");
        final FeatureType type = ftb.build();
        features = new Feature[] {
            feature(type, null,       3, 1,  0),
            feature(type, null,       2, 2,  0),
            feature(type, dependency, 2, 1, 25),
            feature(type, dependency, 1, 1, 18),
            feature(type, null,       4, 1,  0)
        };
        featureSet = new MemoryFeatureSet(null, type, Arrays.asList(features));
    }

    /**
     * Creates an instance of the test feature type with the given values.
     * The {@code value3} is stored only if {@code dependency} is non-null.
     */
    private static Feature feature(final FeatureType type, final FeatureType dependency,
                                   final int value1, final int value2, final int value3)
    {
        final Feature f = type.newInstance();
        f.setPropertyValue("value1", value1);
        f.setPropertyValue("value2", value2);
        if (dependency != null) {
            final Feature d = dependency.newInstance();
            d.setPropertyValue("value3", value3);
            f.setPropertyValue("dependency", d);
        }
        return f;
    }

    /**
     * Configures the query for returning a single instance and returns that instance.
     */
    private Feature executeAndGetFirst() throws DataStoreException {
        query.setLimit(1);
        final FeatureSet subset = query.execute(featureSet);
        return assertSingleton(subset.features(false).collect(Collectors.toList()));
    }

    /**
     * Executes the query and verify that the result is equal to the features at the given indices.
     *
     * @param  indices  indices of expected features.
     * @throws DataStoreException if an error occurred while executing the query.
     */
    private void verifyQueryResult(final int... indices) throws DataStoreException {
        final FeatureSet fs = query.execute(featureSet);
        final List<Feature> result = fs.features(false).collect(Collectors.toList());
        assertEquals(indices.length, result.size());
        for (int i=0; i<indices.length; i++) {
            final Feature expected = features[indices[i]];
            final Feature actual   = result.get(i);
            if (!expected.equals(actual)) {
                fail(String.format("Unexpected feature at index %d%n"
                                 + "Expected:%n%s%n"
                                 + "Actual:%n%s%n", i, expected, actual));
            }
        }
    }

    /**
     * Verifies that the XPath set contains all the given elements.
     *
     * @param expected the expected XPaths.
     */
    private void assertXPathsEqual(final String... expected) {
        assertSetEquals(Arrays.asList(expected), query.getXPaths());
    }

    /**
     * Verifies the effect of {@link FeatureQuery#setLimit(long)}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testLimit() throws DataStoreException {
        createFeaturesWithAssociation();
        query.setLimit(2);
        assertXPathsEqual();
        verifyQueryResult(0, 1);
    }

    /**
     * Verifies the effect of {@link FeatureQuery#setOffset(long)}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testOffset() throws DataStoreException {
        createFeaturesWithAssociation();
        query.setOffset(2);
        assertXPathsEqual();
        verifyQueryResult(2, 3, 4);
    }

    /**
     * Verifies the effect of {@link FeatureQuery#setSortBy(SortProperty[])}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testSortBy() throws DataStoreException {
        createFeaturesWithAssociation();
        final FilterFactory<Feature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setSortBy(ff.sort(ff.property("value1", Integer.class), SortOrder.ASCENDING),
                        ff.sort(ff.property("value2", Integer.class), SortOrder.DESCENDING));
        assertXPathsEqual();
        verifyQueryResult(3, 1, 2, 0, 4);
    }

    /**
     * Verifies the effect of {@link FeatureQuery#setSelection(Filter)}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testSelection() throws DataStoreException {
        createFeaturesWithAssociation();
        final FilterFactory<Feature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setSelection(ff.equal(ff.property("value1", Integer.class),
                                    ff.literal(2), true, MatchAction.ALL));
        assertXPathsEqual("value1");
        verifyQueryResult(1, 2);
    }

    /**
     * Tests {@link FeatureQuery#setSelection(Filter)} on complex features
     * with a filter that follows associations.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testSelectionThroughAssociation() throws DataStoreException {
        createFeaturesWithAssociation();
        final FilterFactory<Feature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setSelection(ff.equal(ff.property("dependency/value3"), ff.literal(18)));
        assertXPathsEqual("dependency/value3");
        verifyQueryResult(3);
    }

    /**
     * Verifies the effect of {@link FeatureQuery#setProjection(FeatureQuery.Column[])}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testProjection() throws DataStoreException {
        createFeaturesWithAssociation();
        final FilterFactory<Feature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setProjection(new FeatureQuery.NamedExpression(ff.property("value1", Integer.class), (String) null),
                            new FeatureQuery.NamedExpression(ff.property("value1", Integer.class), "renamed1"),
                            new FeatureQuery.NamedExpression(ff.literal("a literal"), "computed"));
        assertXPathsEqual("value1");

        // Check result type.
        final Feature instance = executeAndGetFirst();
        final FeatureType resultType = instance.getType();
        assertEquals("Test", resultType.getName().toString());
        assertEquals(3, resultType.getProperties(true).size());
        final PropertyType pt1 = resultType.getProperty("value1");
        final PropertyType pt2 = resultType.getProperty("renamed1");
        final PropertyType pt3 = resultType.getProperty("computed");
        assertTrue(pt1 instanceof AttributeType);
        assertTrue(pt2 instanceof AttributeType);
        assertTrue(pt3 instanceof AttributeType);
        assertEquals(Integer.class, ((AttributeType) pt1).getValueClass());
        assertEquals(Integer.class, ((AttributeType) pt2).getValueClass());
        assertEquals(String.class,  ((AttributeType) pt3).getValueClass());

        // Check feature instance.
        assertEquals(3, instance.getPropertyValue("value1"));
        assertEquals(3, instance.getPropertyValue("renamed1"));
        assertEquals("a literal", instance.getPropertyValue("computed"));
    }

    /**
     * Verifies the effect of {@link FeatureQuery#setProjection(String[])}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testProjectionByNames() throws DataStoreException {
        createFeaturesWithAssociation();
        query.setProjection("value2");
        assertXPathsEqual("value2");
        final Feature instance = executeAndGetFirst();
        final PropertyType p = assertSingleton(instance.getType().getProperties(true));
        assertEquals("value2", p.getName().toString());
    }

    /**
     * Tests the creation of default column names when no alias where explicitly specified.
     * Note that the string representations of default names shall be unlocalized.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testDefaultColumnName() throws DataStoreException {
        createFeaturesWithAssociation();
        final FilterFactory<Feature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setLimit(1);
        query.setProjection(
                ff.add(ff.property("value1", Number.class), ff.literal(1)),
                ff.add(ff.property("value2", Number.class), ff.literal(1)));
        assertXPathsEqual("value1", "value2");
        final FeatureSet subset = featureSet.subset(query);
        final FeatureType type = subset.getType();
        final Iterator<? extends PropertyType> properties = type.getProperties(true).iterator();
        assertEquals("Unnamed #1", properties.next().getName().toString());
        assertEquals("Unnamed #2", properties.next().getName().toString());
        assertFalse(properties.hasNext());

        final Feature instance = assertSingleton(subset.features(false).collect(Collectors.toList()));
        assertSame(type, instance.getType());
    }

    /**
     * Tests {@link FeatureQuery#setProjection(FeatureQuery.NamedExpression...)} on an abstract feature type.
     * We expect the column to be defined even if the property name is undefined on the feature type.
     * This case happens when the {@link FeatureSet} contains features with inherited types.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testProjectionOfAbstractType() throws DataStoreException {
        createFeaturesWithAssociation();
        final FilterFactory<Feature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setProjection(new FeatureQuery.NamedExpression(ff.property("value1"),  (String) null),
                            new FeatureQuery.NamedExpression(ff.property("/*/unknown"), "unexpected"));
        assertXPathsEqual("value1", "/*/unknown");

        // Check result type.
        final Feature instance = executeAndGetFirst();
        final FeatureType resultType = instance.getType();
        assertEquals("Test", resultType.getName().toString());
        assertEquals(2, resultType.getProperties(true).size());
        final PropertyType pt1 = resultType.getProperty("value1");
        final PropertyType pt2 = resultType.getProperty("unexpected");
        assertTrue(pt1 instanceof AttributeType<?>);
        assertTrue(pt2 instanceof AttributeType<?>);
        assertEquals(Integer.class, ((AttributeType<?>) pt1).getValueClass());
        assertEquals(Object.class,  ((AttributeType<?>) pt2).getValueClass());

        // Check feature property values.
        assertEquals(3,    instance.getPropertyValue("value1"));
        assertEquals(null, instance.getPropertyValue("unexpected"));
    }

    /**
     * Tests {@link FeatureQuery#setProjection(FeatureQuery.NamedExpression...)} on complex features
     * with a filter that follows associations.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testProjectionThroughAssociation() throws DataStoreException {
        createFeaturesWithAssociation();
        final FilterFactory<Feature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setProjection(new FeatureQuery.NamedExpression(ff.property("value1"),  (String) null),
                            new FeatureQuery.NamedExpression(ff.property("dependency/value3"), "value3"));
        assertXPathsEqual("value1", "dependency/value3");
        query.setOffset(2);
        final Feature instance = executeAndGetFirst();
        assertEquals( 2, instance.getPropertyValue("value1"));
        assertEquals(25, instance.getPropertyValue("value3"));
    }

    /**
     * Tests {@link FeatureQuery#setProjection(FeatureQuery.NamedExpression...)} on a field
     * which is a link, ensuring that the link name is preserved.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testProjectionOfLink() throws DataStoreException {
        createFeatureWithIdentifier();
        query.setProjection(AttributeConvention.IDENTIFIER);
        assertXPathsEqual(AttributeConvention.IDENTIFIER);
        final Feature instance = executeAndGetFirst();
        assertEquals("id-0", instance.getPropertyValue(AttributeConvention.IDENTIFIER));
    }

    /**
     * Shortcut for creating expression for a projection computed on-the-fly.
     */
    private static FeatureQuery.NamedExpression virtualProjection(final Expression<Feature, ?> expression, final String alias) {
        return new FeatureQuery.NamedExpression(expression, Names.createLocalName(null, null, alias), FeatureQuery.ProjectionType.COMPUTING);
    }

    /**
     * Verifies the effect of virtual projections.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testVirtualProjection() throws DataStoreException {
        createFeaturesWithAssociation();
        final FilterFactory<Feature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setProjection(
                new FeatureQuery.NamedExpression(ff.property("value1", Integer.class), (String) null),
                virtualProjection(ff.property("value1", Integer.class), "renamed1"),
                virtualProjection(ff.literal("a literal"), "computed"));
        assertXPathsEqual("value1");

        // Check result type.
        final Feature instance = executeAndGetFirst();
        final FeatureType resultType = instance.getType();
        assertEquals("Test", resultType.getName().toString());
        assertEquals(3, resultType.getProperties(true).size());
        final PropertyType pt1 = resultType.getProperty("value1");
        final PropertyType pt2 = resultType.getProperty("renamed1");
        final PropertyType pt3 = resultType.getProperty("computed");
        assertTrue(pt1 instanceof AttributeType<?>);
        assertTrue(pt2 instanceof Operation);
        assertTrue(pt3 instanceof Operation);
        final IdentifiedType result2 = ((Operation) pt2).getResult();
        final IdentifiedType result3 = ((Operation) pt3).getResult();
        assertEquals(Integer.class, ((AttributeType<?>) pt1).getValueClass());
        assertTrue(result2 instanceof AttributeType<?>);
        assertTrue(result3 instanceof AttributeType<?>);
        assertEquals(Integer.class, ((AttributeType<?>) result2).getValueClass());
        assertEquals(String.class,  ((AttributeType<?>) result3).getValueClass());

        // Check feature instance.
        assertEquals(3, instance.getPropertyValue("value1"));
        assertEquals(3, instance.getPropertyValue("renamed1"));
        assertEquals("a literal", instance.getPropertyValue("computed"));

        // The `ValueReference` operation should have been optimized as a link.
        assertEquals("value1", Features.getLinkTarget(pt2).get());
        assertTrue(Features.getLinkTarget(pt1).isEmpty());
        assertTrue(Features.getLinkTarget(pt3).isEmpty());
    }

    /**
     * Verifies that a virtual projection on a missing field causes an exception.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testIncorrectVirtualProjection() throws DataStoreException {
        createFeaturesWithAssociation();
        final FilterFactory<Feature,?,?> ff = DefaultFilterFactory.forFeatures();
        query.setProjection(new FeatureQuery.NamedExpression(ff.property("value1", Integer.class), (String) null),
                            virtualProjection(ff.property("valueMissing", Integer.class), "renamed1"));
        assertXPathsEqual("value1", "valueMissing");

        var exception = assertThrows(UnsupportedQueryException.class, this::executeAndGetFirst);
        assertMessageContains(exception);
    }
}
