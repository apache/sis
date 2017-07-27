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
package org.apache.sis.internal.jdk8;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link java.time.OffsetTime} placeholder.
 */
public final strictfp class OffsetTimeTest extends TestCase {
    /**
     * Tests {@link OffsetDateTime#parse(CharSequence)}.
     */
    @Test
    public void testParse() {
        assertEquals("millis", ((3*60 + 2)*60 + 1)*1000 + 90, OffsetTime.parse("03:02:01.09Z").millis);
    }
}
