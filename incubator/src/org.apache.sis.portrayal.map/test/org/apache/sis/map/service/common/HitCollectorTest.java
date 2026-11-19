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
package org.apache.sis.map.service.common;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.sis.map.Presentation;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link HitCollector}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class HitCollectorTest {
    /**
     * A presentation identified by a number, for the tests.
     */
    private static final class Numbered extends Presentation {
        /** The identifier. */
        final int id;

        /** Creates a presentation of the given identifier. */
        Numbered(final int id) {
            this.id = id;
        }

        /** Compares the identifiers. */
        @Override public boolean equals(final Object other) {
            return (other instanceof Numbered n) && n.id == id;
        }

        /** Returns the identifier. */
        @Override public int hashCode() {
            return id;
        }
    }

    /**
     * Creates a new test case.
     */
    public HitCollectorTest() {
    }

    /**
     * Returns a filled square of 2×2 pixels at the given location.
     */
    private static Drawing square(final int x, final int y) {
        return new DrawingShape(new Rectangle(x, y, 2, 2)).setFill(Color.RED, 1);
    }

    /**
     * Tests the collection of the presentations whose drawings intersect the area, without duplicates.
     */
    @Test
    public void testCollect() {
        final HitCollector collector = new HitCollector(new Rectangle(10, 10, 5, 5));
        final Presentation a = new Numbered(1);
        final Presentation b = new Numbered(2);
        assertFalse(collector.add(a, square(0, 0)),   "Outside the area.");
        assertTrue (collector.add(a, square(11, 11)), "Inside the area.");
        assertFalse(collector.add(a, square(12, 12)), "Already collected.");
        assertFalse(collector.add(b, null),           "No drawing.");
        assertTrue (collector.add(b, square(14, 14)));
        assertEquals(List.of(a, b), collector.hits().collect(Collectors.toList()));
    }

    /**
     * Tests the collection from parallel threads.
     */
    @Test
    public void testParallel() {
        final HitCollector collector = new HitCollector(new Rectangle(0, 0, 100, 100));
        final List<Presentation> presentations = IntStream.range(0, 200).mapToObj(Numbered::new)
                .collect(Collectors.toList());
        final long count = presentations.parallelStream()
                .filter((p) -> collector.add(p, square(((Numbered) p).id, 0)))
                .count();
        assertEquals(100, count, "Squares at x < 100 intersect the area.");
        assertEquals(100, collector.hits().count());
    }
}
