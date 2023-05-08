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
package org.apache.sis.storage;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.storage.ProbeResult.*;

import static org.junit.Assert.*;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests {@link ProbeResult}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.4
 * @since   0.3
 */
public final class ProbeResultTest extends TestCase {
    /**
     * Tests serialization of predefined constants.
     */
    @Test
    public void testConstantSerialization() {
        assertSame("SUPPORTED",           SUPPORTED,           assertSerializedEquals(SUPPORTED));
        assertSame("UNSUPPORTED_STORAGE", UNSUPPORTED_STORAGE, assertSerializedEquals(UNSUPPORTED_STORAGE));
        assertSame("INSUFFICIENT_BYTES",  INSUFFICIENT_BYTES,  assertSerializedEquals(INSUFFICIENT_BYTES));
        assertSame("UNDETERMINED",        UNDETERMINED,        assertSerializedEquals(UNDETERMINED));
    }
}
