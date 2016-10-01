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

import java.util.Map;
import java.util.HashMap;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.apache.sis.util.iso.Names;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link DefaultPropertyName}.
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final strictfp class DefaultPropertyNameTest extends TestCase {
    /**
     * Test factory.
     */
    @Test
    public void testConstructor() {
        final FilterFactory2 FF = new DefaultFilterFactory();
        assertNotNull(FF.property(Names.parseGenericName(null, null, "type")));
        assertNotNull(FF.property("type"));
    }

    /**
     * Tests evaluation.
     */
    @Test
    public void testEvaluate() {
        final Map<String,String> candidate = new HashMap<>();

        final PropertyName prop = new DefaultPropertyName("type");
        assertEquals("type", prop.getPropertyName());

        assertEquals(null, prop.evaluate(candidate));
        assertEquals(null, prop.evaluate(null));

        candidate.put("type", "road");
        assertEquals("road", prop.evaluate(candidate));
        assertEquals("road", prop.evaluate(candidate,String.class));

        candidate.put("type", "45.1");
        assertEquals("45.1", prop.evaluate(candidate));
        assertEquals("45.1", prop.evaluate(candidate, Object.class));
        assertEquals("45.1", prop.evaluate(candidate, String.class));
        assertEquals( 45.1,  prop.evaluate(candidate, Double.class), STRICT);
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialize() {
        assertSerializedEquals(new DefaultPropertyName("type"));
    }
}
