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
package org.apache.sis.metadata.iso.extent;

import java.time.Instant;
import org.opengis.referencing.operation.TransformException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link DefaultTemporalExtent}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultTemporalExtentTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultTemporalExtentTest() {
    }

    /**
     * Tests {@link DefaultTemporalExtent#intersect(TemporalExtent)}.
     *
     * @throws TransformException if the transformation failed.
     */
    @Test
    public void testTemporalIntersection() throws TransformException {
        final var e1 = new DefaultTemporalExtent();
        final var e2 = new DefaultTemporalExtent();
        final Instant t1 = Instant.parse("2016-12-05T19:45:20Z");
        final Instant t2 = Instant.parse("2017-02-18T02:12:50Z");
        final Instant t3 = Instant.parse("2017-11-30T23:50:00Z");
        final Instant t4 = Instant.parse("2018-05-20T12:30:45Z");
        e1.setBounds(t1, t3);
        e2.setBounds(t2, t4);
        e1.intersect(e2);
        assertEquals(t2, e1.getBeginning().orElseThrow(), "beginning");
        assertEquals(t3, e1.getEnding().orElseThrow(), "ending");
    }
}
