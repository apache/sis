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

import static org.apache.sis.test.Assert.assertSerializedEquals;
import org.apache.sis.test.TestCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.opengis.filter.FilterFactory2;

/**
 * Tests {@link DefaultMultiply}.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class DefaultMultiplyTest extends TestCase {
    /**
     * Test factory.
     */
    @Test
    public void testConstructor() {
        final FilterFactory2 factory = new DefaultFilterFactory();
        assertNotNull(factory.multiply(factory.literal(1), factory.literal(2)));
    }

    /**
     * Tests evaluation.
     */
    @Test
    public void testEvaluate() {
        final FilterFactory2 factory = new DefaultFilterFactory();

        assertEquals(200.0, new DefaultMultiply(factory.literal(10), factory.literal(20)).evaluate(null));
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialize() {
        final FilterFactory2 factory = new DefaultFilterFactory();
        assertSerializedEquals(new DefaultMultiply(factory.literal(1), factory.literal(2)));
    }

}
