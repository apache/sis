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
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Stream;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.PropertyIsEqualTo;


/**
 * Tests {@link JoinFeatureSet}.
 *
 * @author  Johann Sorel (Geomatys)
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
     * Creates a new test case.
     */
    public JoinFeatureSetTest() {
        FeatureTypeBuilder builder = new FeatureTypeBuilder().setName("Type1");
        builder.addAttribute( String.class).setName(AttributeConvention.IDENTIFIER_PROPERTY);
        builder.addAttribute( String.class).setName("myNameSpace", "att1");
        builder.addAttribute(Integer.class).setName("myNameSpace", "att2");
        final FeatureType type1 = builder.build();

        List<Feature> features = new ArrayList<>(5);

        Feature f = type1.newInstance();
        f.setPropertyValue(AttributeConvention.IDENTIFIER, "fid_1_0");
        f.setPropertyValue("att1", "str1");
        f.setPropertyValue("att2", 1);
        features.add(f);

        f = type1.newInstance();
        f.setPropertyValue(AttributeConvention.IDENTIFIER, "fid_1_1");
        f.setPropertyValue("att1", "str2");
        f.setPropertyValue("att2", 2);
        features.add(f);

        f = type1.newInstance();
        f.setPropertyValue(AttributeConvention.IDENTIFIER, "fid_1_2");
        f.setPropertyValue("att1", "str3");
        f.setPropertyValue("att2", 3);
        features.add(f);

        f = type1.newInstance();
        f.setPropertyValue(AttributeConvention.IDENTIFIER, "fid_1_3");
        f.setPropertyValue("att1", "str50");
        f.setPropertyValue("att2", 50);
        features.add(f);

        f = type1.newInstance();
        f.setPropertyValue(AttributeConvention.IDENTIFIER, "fid_1_4");
        f.setPropertyValue("att1", "str51");
        f.setPropertyValue("att2", 51);
        features.add(f);

        featureSet1 = new MemoryFeatureSet(null, null, type1, features);

        // ----------------------------------------------------------------------

        builder = new FeatureTypeBuilder().setName("Type2");
        builder.addAttribute( String.class).setName(AttributeConvention.IDENTIFIER_PROPERTY);
        builder.addAttribute(Integer.class).setName("otherNameSpace", "att3");
        builder.addAttribute( Double.class).setName("otherNameSpace", "att4");
        final FeatureType type2 = builder.build();

        features = new ArrayList<>(6);

        f = type2.newInstance();
        f.setPropertyValue(AttributeConvention.IDENTIFIER, "fid_2_0");
        f.setPropertyValue("att3", 1);
        f.setPropertyValue("att4", 10d);
        features.add(f);

        f = type2.newInstance();
        f.setPropertyValue(AttributeConvention.IDENTIFIER, "fid_2_1");
        f.setPropertyValue("att3", 2);
        f.setPropertyValue("att4", 20d);
        features.add(f);

        f = type2.newInstance();
        f.setPropertyValue(AttributeConvention.IDENTIFIER, "fid_2_2");
        f.setPropertyValue("att3", 2);
        f.setPropertyValue("att4", 30d);
        features.add(f);

        f = type2.newInstance();
        f.setPropertyValue(AttributeConvention.IDENTIFIER, "fid_2_3");
        f.setPropertyValue("att3", 3);
        f.setPropertyValue("att4", 40d);
        features.add(f);

        f = type2.newInstance();
        f.setPropertyValue(AttributeConvention.IDENTIFIER, "fid_2_4");
        f.setPropertyValue("att3", 60);
        f.setPropertyValue("att4", 60d);
        features.add(f);

        f = type2.newInstance();
        f.setPropertyValue(AttributeConvention.IDENTIFIER, "fid_2_5");
        f.setPropertyValue("att3", 61);
        f.setPropertyValue("att4", 61d);
        features.add(f);

        featureSet2 = new MemoryFeatureSet(null, null, type2, features);
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
     * Returns the identifier of the given feature.
     */
    private static String getId(final Feature feature) {
        return String.valueOf(feature.getPropertyValue(AttributeConvention.IDENTIFIER));
    }

    /**
     * Test inner join feature set.
     *
     * @throws DataStoreException if an error occurred while creating the feature set.
     */
    @Test
    public void testInnerJoin() throws DataStoreException {
        final FeatureSet col = create(JoinFeatureSet.Type.INNER);
        try (Stream<Feature> stream = col.features(false)) {
            final Iterator<Feature> ite = stream.iterator();
            int count = 0;
            while (ite.hasNext()) {
                count++;
                final Feature f  = ite.next();
                final Feature c1 = (Feature) f.getPropertyValue("s1");
                final Feature c2 = (Feature) f.getPropertyValue("s2");
                switch (getId(f)) {
                    case "fid_1_0 fid_2_0": {
                        assertEquals("str1", c1.getProperty("att1").getValue());
                        assertEquals( 1,     c1.getProperty("att2").getValue());
                        assertEquals( 1,     c2.getProperty("att3").getValue());
                        assertEquals(10d,    c2.getProperty("att4").getValue());
                        break;
                    }
                    case "fid_1_1 fid_2_1": {
                        assertEquals("str2", c1.getProperty("att1").getValue());
                        assertEquals( 2,     c1.getProperty("att2").getValue());
                        assertEquals( 2,     c2.getProperty("att3").getValue());
                        assertEquals(20d,    c2.getProperty("att4").getValue());
                        break;
                    }
                    case "fid_1_1 fid_2_2": {
                        assertEquals("str2", c1.getProperty("att1").getValue());
                        assertEquals( 2,     c1.getProperty("att2").getValue());
                        assertEquals( 2,     c2.getProperty("att3").getValue());
                        assertEquals(30d,    c2.getProperty("att4").getValue());
                        break;
                    }
                    case "fid_1_2 fid_2_3": {
                        assertEquals("str3", c1.getProperty("att1").getValue());
                        assertEquals( 3,     c1.getProperty("att2").getValue());
                        assertEquals( 3,     c2.getProperty("att3").getValue());
                        assertEquals(40d,    c2.getProperty("att4").getValue());
                        break;
                    }
                    default: {
                        fail("unexpected feature");
                        break;
                    }
                }
            }
            assertEquals("Unexpected amount of features.", 4, count);
        }
    }

    /**
     * Test outer join feature set.
     *
     * @throws DataStoreException if an error occurred while creating the feature set.
     */
    @Test
    public void testOuterLeft() throws DataStoreException {
        final FeatureSet col = create(JoinFeatureSet.Type.LEFT_OUTER);
        try (Stream<Feature> stream = col.features(false)) {
            final Iterator<Feature> ite = stream.iterator();
            boolean foundStr1  = false;
            boolean foundStr20 = false;
            boolean foundStr21 = false;
            boolean foundStr3  = false;
            boolean foundStr50 = false;
            boolean foundStr51 = false;
            int count = 0;
            while (ite.hasNext()) {
                final Feature f   = ite.next();
                final Feature c1  = (Feature) f.getPropertyValue("s1");
                final Feature c2  = (Feature) f.getPropertyValue("s2");
                final String att1 = c1.getProperty("att1").getValue().toString();
                switch (att1) {
                    case "str1": {
                        foundStr1 = true;
                        assertEquals( 1,  c1.getProperty("att2").getValue());
                        assertEquals( 1,  c2.getProperty("att3").getValue());
                        assertEquals(10d, c2.getProperty("att4").getValue());
                        break;
                    }
                    case "str2": {
                        assertEquals(2, c1.getProperty("att2").getValue());
                        assertEquals(2, c2.getProperty("att3").getValue());
                        double att4 = (Double) c2.getProperty("att4").getValue();
                        foundStr20 |= (att4 == 20);
                        foundStr21 |= (att4 == 30);
                        break;
                    }
                    case "str3": {
                        foundStr3 = true;
                        assertEquals( 3,  c1.getProperty("att2").getValue());
                        assertEquals( 3,  c2.getProperty("att3").getValue());
                        assertEquals(40d, c2.getProperty("att4").getValue());
                        break;
                    }
                    case "str50": {
                        foundStr50 = true;
                        assertEquals(50, c1.getProperty("att2").getValue());
                        assertNull(c2);
                        break;
                    }
                    case "str51": {
                        foundStr51 = true;
                        assertEquals(51, c1.getProperty("att2").getValue());
                        assertNull(c2);
                        break;
                    }
                    default: {
                        fail("unexpected feature");
                        break;
                    }
                }
                count++;
            }
            assertTrue(foundStr1);
            assertTrue(foundStr20);
            assertTrue(foundStr21);
            assertTrue(foundStr3);
            assertTrue(foundStr50);
            assertTrue(foundStr51);
            assertEquals("Unexpected amount of features.", 6, count);
        }
    }

    /**
     * Test outer join feature set.
     *
     * @throws DataStoreException if an error occurred while creating the feature set.
     */
    @Test
    public void testOuterRight() throws DataStoreException {
        final FeatureSet col = create(JoinFeatureSet.Type.RIGHT_OUTER);
        try (Stream<Feature> stream = col.features(false)) {
            final Iterator<Feature> ite = stream.iterator();
            boolean foundStr1  = false;
            boolean foundStr20 = false;
            boolean foundStr21 = false;
            boolean foundStr3  = false;
            boolean foundStr60 = false;
            boolean foundStr61 = false;
            int count = 0;
            while (ite.hasNext()) {
                final Feature f  = ite.next();
                final Feature c1 = (Feature) f.getPropertyValue("s1");
                final Feature c2 = (Feature) f.getPropertyValue("s2");
                if (c1 != null) {
                    switch (c1.getProperty("att1").getValue().toString()) {
                        case "str1": {
                            foundStr1 = true;
                            assertEquals( 1,  c1.getProperty("att2").getValue());
                            assertEquals( 1,  c2.getProperty("att3").getValue());
                            assertEquals(10d, c2.getProperty("att4").getValue());
                            break;
                        }
                        case "str2": {
                            assertEquals(2, c1.getProperty("att2").getValue());
                            assertEquals(2, c2.getProperty("att3").getValue());
                            double att4 = (Double) c2.getProperty("att4").getValue();
                            foundStr20 |= (att4 == 20d);
                            foundStr21 |= (att4 == 30d);
                            break;
                        }
                        case "str3": {
                            foundStr3 = true;
                            assertEquals( 3, c1.getProperty("att2").getValue());
                            assertEquals( 3, c2.getProperty("att3").getValue());
                            assertEquals(40d,c2.getProperty("att4").getValue());
                            break;
                        }
                    }
                } else {
                    int att3 = (Integer) c2.getProperty("att3").getValue();
                    if (att3 == 60) {
                        assertEquals(c2.getProperty("att4").getValue(), 60d);
                        foundStr60 = true;
                    } else if (att3 == 61) {
                        assertEquals(c2.getProperty("att4").getValue(), 61d);
                        foundStr61 = true;
                    }
                }
                count++;
            }
            assertTrue(foundStr1);
            assertTrue(foundStr20);
            assertTrue(foundStr21);
            assertTrue(foundStr3);
            assertTrue(foundStr60);
            assertTrue(foundStr61);
            assertEquals(6, count);
        }
    }
}
