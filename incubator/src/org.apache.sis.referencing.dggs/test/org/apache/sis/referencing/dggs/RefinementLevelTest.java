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
package org.apache.sis.referencing.dggs;

import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.referencing.dggs.smc.SmcDggrs;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link RefinementLevel}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class RefinementLevelTest {

    /**
     * Tests that levels are equal when they have the same reference system and the same number.
     */
    @Test
    public void testEquals() {
        final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
        final RefinementLevel level = new RefinementLevel(dggrs, 2);
        assertEquals(level, new RefinementLevel(dggrs, 2));
        assertEquals(level.hashCode(), new RefinementLevel(dggrs, 2).hashCode());
        assertNotEquals(level, new RefinementLevel(dggrs, 3));
        assertNotEquals(level, new RefinementLevel(new SmcDggrs(), 2));
        assertTrue(level.getChildren().contains(new RefinementLevel(dggrs, 3)));
        assertTrue(level.getParents().contains(new RefinementLevel(dggrs, 1)));
    }

    /**
     * Tests that the owner of a level is the owner of the reference system.
     */
    @Test
    public void testOwner() {
        final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
        assertSame(dggrs.getOverallOwner(), new RefinementLevel(dggrs, 0).getOwner());
    }
}
