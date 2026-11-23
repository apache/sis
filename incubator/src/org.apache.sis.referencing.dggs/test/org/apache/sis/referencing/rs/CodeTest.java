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
package org.apache.sis.referencing.rs;

import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link Code}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CodeTest {

    /**
     * Tests {@link Code#toDirectPosition()} with a two-dimensional component followed by a temporal component.
     */
    @Test
    public void testToDirectPositionWithMultiDimensionalComponent() throws Exception {
        final ReferenceSystem rs = ReferenceSystems.createCompound(CommonCRS.WGS84.normalizedGeographic(), CommonCRS.Temporal.JAVA.crs());
        final DirectPosition position = new Code(rs, 10.0, 20.0, 1000.0).toDirectPosition();
        assertArrayEquals(new double[] {10, 20, 1000}, position.getCoordinates());
        assertEquals(3, position.getCoordinateReferenceSystem().getCoordinateSystem().getDimension());
    }
}
