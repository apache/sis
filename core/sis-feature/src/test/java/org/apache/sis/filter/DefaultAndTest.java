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
package org.apache.sis.filter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static org.apache.sis.test.Assert.assertSerializedEquals;
import org.apache.sis.test.TestCase;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

/**
 * Tests {@link DefaultAnd}.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class DefaultAndTest extends TestCase {
    /**
     * Test factory.
     */
    @Test
    public void testConstructor() {
        final FilterFactory2 factory = new DefaultFilterFactory();
        final Literal literal = factory.literal("text");
        final Filter filter = factory.isNull(literal);

        assertNotNull(factory.and(filter, filter));
        assertNotNull(factory.and(Arrays.asList(filter, filter, filter)));

        try {
            factory.and(null, null);
            Assert.fail("Creation of an AND with a null child filter must raise an exception");
        } catch (Exception ex) {}
        try {
            factory.and(filter, null);
            Assert.fail("Creation of an AND with a null child filter must raise an exception");
        } catch (Exception ex) {}
        try {
            factory.and(null, filter);
            Assert.fail("Creation of an AND with a null child filter must raise an exception");
        } catch (Exception ex) {}
        try {
            factory.and(Arrays.asList(filter));
            Assert.fail("Creation of an AND with less then two children filters must raise an exception");
        } catch (Exception ex) {}

    }

    /**
     * Tests evaluation.
     */
    @Test
    public void testEvaluate() {
        final FilterFactory2 factory = new DefaultFilterFactory();
        final PropertyName literalNotNull = factory.property("attNotNull");
        final PropertyName literalNull = factory.property("attNull");
        final Filter filterTrue = factory.isNull(literalNull);
        final Filter filterFalse = factory.isNull(literalNotNull);

        final Map<String,String> feature = new HashMap();
        feature.put("attNotNull", "text");

        assertEquals(true, new DefaultAnd(Arrays.asList(filterTrue, filterTrue)).evaluate(feature));
        assertEquals(false, new DefaultAnd(Arrays.asList(filterFalse, filterTrue)).evaluate(feature));
        assertEquals(false, new DefaultAnd(Arrays.asList(filterTrue, filterFalse)).evaluate(feature));
        assertEquals(false, new DefaultAnd(Arrays.asList(filterFalse, filterFalse)).evaluate(feature));
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialize() {
        final FilterFactory2 factory = new DefaultFilterFactory();
        final Literal literal = factory.literal("text");
        final Filter filter = factory.isNull(literal);
        assertSerializedEquals(new DefaultAnd(Arrays.asList(filter,filter)));
    }

}
