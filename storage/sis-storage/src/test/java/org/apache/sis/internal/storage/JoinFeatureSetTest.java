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
package org.apache.sis.internal.storage;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.PropertyIsEqualTo;
import org.apache.sis.filter.DefaultFilterFactory;


/**
 * Tests {@link JoinFeatureSet}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class JoinFeatureSetTest extends TestCase {
    /**
     * The set of features to be joined together.
     */
    private final FeatureSet featureSet1, featureSet2;

    /**
     * {@code true} if testing parallel execution, or {@code false} for testing sequential execution.
     * Parallel execution will be tested only if all sequential execution succeed.
     */
    private boolean parallel;

    /**
     * Creates a new test case.
     */
    public JoinFeatureSetTest() {
        FeatureTypeBuilder builder = new FeatureTypeBuilder().setName("Type1");
        builder.addAttribute( String.class).setName(AttributeConvention.IDENTIFIER_PROPERTY);
        builder.addAttribute( String.class).setName("myNameSpace", "att1");
        builder.addAttribute(Integer.class).setName("myNameSpace", "att2");
        final FeatureType type1 = builder.build();
        featureSet1 = new MemoryFeatureSet(null, null, type1, Arrays.asList(
                newFeature1(type1, "fid_1_0", "str1",   1),
                newFeature1(type1, "fid_1_1", "str2",   2),
                newFeature1(type1, "fid_1_2", "str3",   3),
                newFeature1(type1, "fid_1_3", "str50", 50),
                newFeature1(type1, "fid_1_4", "str51", 51)));

        builder = new FeatureTypeBuilder().setName("Type2");
        builder.addAttribute( String.class).setName(AttributeConvention.IDENTIFIER_PROPERTY);
        builder.addAttribute(Integer.class).setName("otherNameSpace", "att3");
        builder.addAttribute( Double.class).setName("otherNameSpace", "att4");
        final FeatureType type2 = builder.build();
        featureSet2 = new MemoryFeatureSet(null, null, type2, Arrays.asList(
                newFeature2(type2, "fid_2_0",  1, 10),
                newFeature2(type2, "fid_2_1",  2, 20),
                newFeature2(type2, "fid_2_2",  2, 30),
                newFeature2(type2, "fid_2_3",  3, 40),
                newFeature2(type2, "fid_2_4", 60, 60),
                newFeature2(type2, "fid_2_5", 61, 61)));
    }

    /**
     * Creates a new feature of type 1 with the given identifier and attribute values.
     * This is a helper method for the constructor only.
     */
    private static Feature newFeature1(final FeatureType type, final String id, final String att1, final int att2) {
        final Feature f = type.newInstance();
        f.setPropertyValue(AttributeConvention.IDENTIFIER, id);
        f.setPropertyValue("att1", att1);
        f.setPropertyValue("att2", att2);
        return f;
    }

    /**
     * Creates a new feature of type 2 with the given identifier and attribute values.
     * This is a helper method for the constructor only.
     */
    private static Feature newFeature2(final FeatureType type, final String id, final int att3, final double att4) {
        final Feature f = type.newInstance();
        f.setPropertyValue(AttributeConvention.IDENTIFIER, id);
        f.setPropertyValue("att3", att3);
        f.setPropertyValue("att4", att4);
        return f;
    }

    /**
     * Creates a new join feature set of the given type using the {@link #featureSet1} and {@link #featureSet2}.
     */
    private FeatureSet create(final JoinFeatureSet.Type type) throws DataStoreException {
        final FilterFactory factory = new DefaultFilterFactory();
        final PropertyIsEqualTo condition = factory.equals(factory.property("att2"), factory.property("att3"));
        final Map<String,Object> properties = new HashMap<>(4);
        assertNull(properties.put("name", "JoinSet"));
        assertNull(properties.put("identifierDelimiter", " "));
        return new JoinFeatureSet(null, featureSet1, "s1", featureSet2, "s2", type, condition, properties);
    }

    /**
     * Creates a stream over the features from the given set. If parallelization is enabled,
     * then this method copies the features in a temporary list using parallelized paths
     * before to return the stream of that list.
     */
    private Stream<Feature> stream(final FeatureSet col) throws DataStoreException {
        if (parallel) {
            return col.features(true).collect(Collectors.toList()).stream();
        } else {
            return col.features(false);
        }
    }

    /**
     * Returns the identifier of the given feature.
     */
    private static String getId(final Feature feature) {
        return String.valueOf(feature.getPropertyValue(AttributeConvention.IDENTIFIER));
    }

    /**
     * Tests inner join feature set.
     *
     * @throws DataStoreException if an error occurred while creating the feature set.
     */
    @Test
    public void testInnerJoin() throws DataStoreException {
        final FeatureSet col = create(JoinFeatureSet.Type.INNER);
        try (Stream<Feature> stream = stream(col)) {
            final Iterator<Feature> ite = stream.iterator();
            int count = 0;
            while (ite.hasNext()) {
                count++;
                final Feature f = ite.next();
                final String att1;          // Expected value of "att1".
                final int    join;          // Expected value of "att2" and "att3", on which the join operation is done.
                final double att4;          // Expected value of "att4".
                switch (getId(f)) {
                    case "fid_1_0 fid_2_0":  att1 = "str1"; join = 1; att4 = 10; break;
                    case "fid_1_1 fid_2_1":  att1 = "str2"; join = 2; att4 = 20; break;
                    case "fid_1_1 fid_2_2":  att1 = "str2"; join = 2; att4 = 30; break;
                    case "fid_1_2 fid_2_3":  att1 = "str3"; join = 3; att4 = 40; break;
                    default: fail("unexpected feature"); continue;
                }
                final Feature c1 = (Feature) f.getPropertyValue("s1");
                final Feature c2 = (Feature) f.getPropertyValue("s2");
                assertEquals("att1", att1, c1.getProperty("att1").getValue());
                assertEquals("att2", join, c1.getProperty("att2").getValue());
                assertEquals("att3", join, c2.getProperty("att3").getValue());
                assertEquals("att4", att4, c2.getProperty("att4").getValue());
            }
            assertEquals("Unexpected amount of features.", 4, count);
        }
    }

    /**
     * Tests outer join feature set.
     *
     * @throws DataStoreException if an error occurred while creating the feature set.
     */
    @Test
    public void testOuterLeft() throws DataStoreException {
        final FeatureSet col = create(JoinFeatureSet.Type.LEFT_OUTER);
        testOuter(col, 1, 0);
    }

    /**
     * Tests outer join feature set.
     *
     * @throws DataStoreException if an error occurred while creating the feature set.
     */
    @Test
    public void testOuterRight() throws DataStoreException {
        final FeatureSet col = create(JoinFeatureSet.Type.RIGHT_OUTER);
        testOuter(col, 0, 1);
    }

    /**
     * Implementation of {@link #testOuterLeft()} and {@link #testOuterRight()}.
     *
     * @param  nl  1 if testing outer left,  0 otherwise.
     * @param  nr  1 if testing outer right, 0 otherwise.
     */
    private void testOuter(final FeatureSet col, final int nl, final int nr) throws DataStoreException {
        try (Stream<Feature> stream = stream(col)) {
            final Iterator<Feature> ite = stream.iterator();
            int foundStr1 = 0, foundStr20 = 0, foundStr50 = 0, foundStr60 = 0,
                foundStr3 = 0, foundStr21 = 0, foundStr51 = 0, foundStr61 = 0, count = 0;
            while (ite.hasNext()) {
                final Feature f  = ite.next();
                final Feature c1 = (Feature) f.getPropertyValue("s1");
                final Feature c2 = (Feature) f.getPropertyValue("s2");
                if (c1 != null) {
                    switch ((String) c1.getProperty("att1").getValue()) {
                        case "str1": {
                            assertEquals("att2",  1,  c1.getProperty("att2").getValue());
                            assertEquals("att3",  1,  c2.getProperty("att3").getValue());
                            assertEquals("att4", 10d, c2.getProperty("att4").getValue());
                            foundStr1++;
                            break;
                        }
                        case "str2": {
                            assertEquals("att2", 2, c1.getProperty("att2").getValue());
                            assertEquals("att3", 2, c2.getProperty("att3").getValue());
                            double att4 = (Double)  c2.getProperty("att4").getValue();
                            if (att4 == 20) foundStr20++;
                            if (att4 == 30) foundStr21++;
                            break;
                        }
                        case "str3": {
                            assertEquals("att2",  3,  c1.getProperty("att2").getValue());
                            assertEquals("att3",  3,  c2.getProperty("att3").getValue());
                            assertEquals("att4", 40d, c2.getProperty("att4").getValue());
                            foundStr3++;
                            break;
                        }
                        case "str50": {
                            assertEquals("att2", 50, c1.getProperty("att2").getValue());
                            assertNull("right", c2);
                            foundStr50++;
                            break;
                        }
                        case "str51": {
                            assertEquals("att2", 51, c1.getProperty("att2").getValue());
                            assertNull("right", c2);
                            foundStr51++;
                            break;
                        }
                        default: {
                            fail("unexpected feature");
                            break;
                        }
                    }
                } else {
                    switch ((Integer) c2.getProperty("att3").getValue()) {
                        case 60: {
                            assertEquals(c2.getProperty("att4").getValue(), 60d);
                            foundStr60++;
                            break;
                        }
                        case 61: {
                            assertEquals(c2.getProperty("att4").getValue(), 61d);
                            foundStr61++;
                            break;
                        }
                        default: {
                            fail("unexpected feature");
                            break;
                        }
                    }
                }
                count++;
            }
            assertEquals("str1",  1,  foundStr1);
            assertEquals("str2",  1,  foundStr20);
            assertEquals("str2",  1,  foundStr21);
            assertEquals("str3",  1,  foundStr3);
            assertEquals("str50", nl, foundStr50);
            assertEquals("str51", nl, foundStr51);
            assertEquals("str60", nr, foundStr60);
            assertEquals("str61", nr, foundStr61);
            assertEquals("Unexpected amount of features.", 6, count);
        }
    }

    /**
     * Tests inner join, outer left and outer right using parallelized paths. This will test the
     * {@code Spliterator.trySplit()} / {@code forEachRemaining(Consumer)} implementations of
     * {@link JoinFeatureSet}.
     *
     * @throws DataStoreException if an error occurred while creating the feature set.
     */
    @Test
    @DependsOnMethod({"testInnerJoin", "testOuterLeft", "testOuterRight"})
    public void testParallelization() throws DataStoreException {
        parallel = true;
        testInnerJoin();
        testOuterLeft();
        testOuterRight();
    }
}
