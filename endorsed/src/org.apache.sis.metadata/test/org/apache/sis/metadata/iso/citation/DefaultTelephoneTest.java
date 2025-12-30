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
package org.apache.sis.metadata.iso.citation;

import java.util.List;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;


/**
 * Tests {@link DefaultTelephone}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public class DefaultTelephoneTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultTelephoneTest() {
    }

    /**
     * Tests {@link DefaultTelephone#identityEquals(Iterator, Iterator)}.
     */
    @Test
    public void testIdentityEquals() {
        final List<String> c1 = List.of("A", "B", "C");
        final List<String> c2 = List.of("A", "B");
        assertFalse(DefaultTelephone.identityEquals(c1.iterator(), c2.iterator()));
        assertFalse(DefaultTelephone.identityEquals(c2.iterator(), c1.iterator()));
        assertTrue(DefaultTelephone.identityEquals(c1.iterator(), List.of("A", "B", "C").iterator()));
    }
}
