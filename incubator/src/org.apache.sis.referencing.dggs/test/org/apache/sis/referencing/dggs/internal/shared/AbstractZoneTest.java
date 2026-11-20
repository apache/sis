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
package org.apache.sis.referencing.dggs.internal.shared;

import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link AbstractZone} through S2 zones.
 *
 * @author Johann Sorel (Geomatys)
 */
public class AbstractZoneTest {

    /**
     * Tests {@link AbstractZone#isAncestorOf(Object, int)} and {@link AbstractZone#isDescendantOf(Object, int)}
     * with a maximum depth equal to or greater than the actual depth.
     */
    @Test
    public void testAncestorAndDescendant() throws Exception {
        final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
        final Zone face = dggrs.getGridSystem().getHierarchy().getGrids().get(0).getZones().findFirst().orElseThrow();
        final Zone child = face.getChildren().iterator().next();
        final Zone grandChild = child.getChildren().iterator().next();

        assertTrue (face.isAncestorOf(child, 1));
        assertTrue (face.isAncestorOf(child, 5));
        assertFalse(face.isAncestorOf(grandChild, 1));
        assertTrue (face.isAncestorOf(grandChild, 2));
        assertTrue (face.isAncestorOf(grandChild, 5));
        assertFalse(child.isAncestorOf(face, 5));
        assertTrue (grandChild.isDescendantOf(face, 5));
    }
}
