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
package org.apache.sis.storage.dggs;

import java.util.List;
import org.apache.sis.coverage.grid.IncompleteGridGeometryException;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.storage.rs.internal.shared.CodeTransforms;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link DiscreteGlobalGridGeometry}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DiscreteGlobalGridGeometryTest {

    private final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();

    /**
     * Tests a geometry without zone list, which accepts any zone.
     */
    @Test
    public void testUndefinedZones() throws Exception {
        final DiscreteGlobalGridGeometry geometry = DiscreteGlobalGridGeometry.unstructured(dggrs, null, null);
        assertNull(geometry.getEnvelope());
        assertNull(geometry.getEnvelope(CommonCRS.WGS84.geographic()));
        assertThrows(IncompleteGridGeometryException.class, geometry::getRefinementLevel);
    }

    /**
     * Tests a geometry with an empty zone list.
     */
    @Test
    public void testEmptyZones() throws Exception {
        final DiscreteGlobalGridGeometry geometry = DiscreteGlobalGridGeometry.unstructured(
                dggrs, null, CodeTransforms.toTransform(dggrs, List.of()), null);
        assertNull(geometry.getEnvelope());
        assertNull(geometry.getEnvelope(CommonCRS.WGS84.geographic()));
        assertThrows(IncompleteGridGeometryException.class, geometry::getRefinementLevel);
    }
}
