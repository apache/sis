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
package org.apache.sis.pending.jdk;

import static org.apache.sis.pending.jdk.JDK18.ceilDiv;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link JDK18} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class JDK18Test extends TestCase {
    /**
     * Creates a new test case.
     */
    public JDK18Test() {
    }

    /**
     * Tests {@link JDK18#ceilDiv(int, int)} and {@link JDK18#ceilDiv(long, long)}.
     */
    @Test
    public void testCeilDiv() {
        assertEquals( 4,  ceilDiv( 12,  3 ));
        assertEquals( 4L, ceilDiv( 12L, 3L));
        assertEquals( 3,  ceilDiv(  8,  3 ));
        assertEquals( 3L, ceilDiv(  8L, 3L));
        assertEquals(-4,  ceilDiv(-12,  3 ));
        assertEquals(-4L, ceilDiv(-12L, 3L));
        assertEquals(-2,  ceilDiv( -8,  3 ));
        assertEquals(-2L, ceilDiv( -8L, 3L));
    }
}
