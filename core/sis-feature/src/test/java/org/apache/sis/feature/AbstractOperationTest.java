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

import java.util.Map;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests {@link AbstractOperation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.6
 */
@DependsOn(SingletonAttributeTest.class)
public final class AbstractOperationTest extends TestCase {
    /**
     * Returns an operation that found new cities.
     */
    static AbstractOperation foundCity() {
        final ParameterBuilder builder = new ParameterBuilder();
        final ParameterDescriptor<?>[] parameters = {
            builder.addName("founder").create(String.class, null)
        };
        return new NoOperation(Map.of(AbstractOperation.NAME_KEY, "new city"),
                builder.addName("create").createGroup(parameters),
                DefaultAttributeTypeTest.city());
    }

    /**
     * Tests serialization of {@link AbstractOperation}.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(foundCity());
    }

    /**
     * Tests {@link AbstractOperation#toString()}.
     */
    @Test
    public void testToString() {
        assertEquals("NoOperation[“new city” : String] = create(founder)", foundCity().toString());
    }
}
