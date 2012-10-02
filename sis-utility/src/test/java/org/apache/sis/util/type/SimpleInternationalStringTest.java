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
package org.apache.sis.util.type;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Validators.validate;
import static org.apache.sis.test.Assert.*;
import static org.apache.sis.util.type.DefaultInternationalStringTest.MESSAGE;


/**
 * Tests the {@link SimpleInternationalString} implementation.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
public final strictfp class SimpleInternationalStringTest extends TestCase {
    /**
     * Tests the {@link SimpleInternationalString} implementation.
     */
    @Test
    public void testSimple() {
        final SimpleInternationalString toTest = new SimpleInternationalString(MESSAGE);
        assertSame(MESSAGE, toTest.toString());
        assertSame(MESSAGE, toTest.toString(null));
        validate(toTest);
    }

    /**
     * Tests the {@link SimpleInternationalString} serialization.
     */
    @Test
    public void testSerialization() {
        final SimpleInternationalString before = new SimpleInternationalString(MESSAGE);
        final SimpleInternationalString after  = assertSerializedEquals(before);
        assertEquals(MESSAGE, after.toString());
        assertEquals(MESSAGE, after.toString(null));
        validate(after);
    }
}
