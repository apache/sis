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

import static org.apache.sis.internal.style.StyleFactory.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link org.apache.sis.internal.style.Halo}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class HaloTest extends AbstractStyleTests {

    /**
     * Test of Fill methods.
     */
    @Test
    public void testFill() {
        Halo cdt = new Halo();

        //check default
        assertEquals(new Fill(null, LITERAL_WHITE, DEFAULT_FILL_OPACITY), cdt.getFill());

        //check get/set
        cdt.setFill(new Fill(null, EXP_COLOR, EXP_DOUBLE));
        assertEquals(new Fill(null, EXP_COLOR, EXP_DOUBLE), cdt.getFill());
    }

    /**
     * Test of Radius methods.
     */
    @Test
    public void testRadius() {
        Halo cdt = new Halo();

        //check default
        assertEquals(FF.literal(1.0), cdt.getRadius());

        //check get/set
        cdt.setRadius(EXP_DOUBLE);
        assertEquals(EXP_DOUBLE, cdt.getRadius());
    }

}
