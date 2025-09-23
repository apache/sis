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
package org.apache.sis.metadata.sql.internal.shared;

import java.sql.Types;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link TypeMapper}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TypeMapperTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public TypeMapperTest() {
    }

    /**
     * Tests {@link TypeMapper#toJavaType(int)}.
     */
    @Test
    public void testToJavaType() {
        assertEquals(Integer.class, TypeMapper.toJavaType(Types.INTEGER));
        assertEquals(Boolean.class, TypeMapper.toJavaType(Types.BOOLEAN));
        assertNull  (               TypeMapper.toJavaType(Types.LONGVARCHAR));
    }
}
