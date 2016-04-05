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

import org.apache.sis.internal.feature.FeatureTypeBuilder;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.iso.Names;
import org.junit.Test;

import static org.junit.Assert.*;
import org.opengis.feature.PropertyType;


/**
 * Tests {@link AggregateOperation}.
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    AbstractOperationTest.class,
    DenseFeatureTest.class
})
public final strictfp class AggregateOperationTest extends TestCase {
    /**
     * Creates a feature type with an aggregation operation.
     * The feature contains the following properties:
     *
     * <ul>
     *   <li>{@code name} as a  {@link String}</li>
     *   <li>{@code age} as an {@link Integer}</li>
     *   <li>{@code summary} as aggregation of {@code name} and {@code age} attributes.</li>
     * </ul>
     *
     * @return The feature for a city.
     */
    private static DefaultFeatureType person() {
        //Create type with an aggregation
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("person");
        final PropertyType nameType = ftb.addProperty("name", String.class);
        final PropertyType ageType = ftb.addProperty("age", Integer.class);
        ftb.addProperty(FeatureOperations.aggregate(Names.parseGenericName(null, ":", 
                "summary"), "prefix:", ":suffix","/",nameType.getName(), ageType.getName()));
        return ftb.build();
    }

    /**
     * Implementation of the test methods.
     */
    private static void run(final AbstractFeature feature) {

        //test feature
        assertEquals("prefix:null/null:suffix", feature.getPropertyValue("summary"));
        feature.setPropertyValue("name", "marc");
        assertEquals("prefix:marc/null:suffix", feature.getPropertyValue("summary"));
        feature.setPropertyValue("age", 21);
        assertEquals("prefix:marc/21:suffix", feature.getPropertyValue("summary"));

        //test setting value
        feature.setPropertyValue("summary", "prefix:emile/37:suffix");
        assertEquals("emile", feature.getPropertyValue("name"));
        assertEquals(37, feature.getPropertyValue("age"));
        assertEquals("prefix:emile/37:suffix", feature.getPropertyValue("summary"));

    }

    /**
     * Tests a dense type with operations.
     */
    @Test
    public void testDenseFeature() {
        run(new DenseFeature(person()));
    }

    /**
     * Tests a sparse feature type with operations.
     */
    @Test
    public void testSparseFeature() {
        run(new SparseFeature(person()));
    }
}
