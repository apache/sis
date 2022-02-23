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
package org.apache.sis.internal.style;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link org.apache.sis.internal.style.Displacement}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DisplacementTest extends AbstractStyleTests {

    /**
     * Test of DisplacementXY methods.
     */
    @Test
    public void testGetDisplacementXY() {
        Displacement cdt = new Displacement();

        //check defaults
        assertEquals(FF.literal(0.0), cdt.getDisplacementX());
        assertEquals(FF.literal(0.0), cdt.getDisplacementY());

        //check get/set
        cdt.setDisplacementX(EXP_DOUBLE);
        cdt.setDisplacementY(EXP_DOUBLE_2);
        assertEquals(EXP_DOUBLE, cdt.getDisplacementX());
        assertEquals(EXP_DOUBLE_2, cdt.getDisplacementY());
    }

}
