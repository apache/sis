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
 * Tests for {@link org.apache.sis.internal.style.GraphicStroke}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class GraphicStrokeTest extends AbstractStyleTests {

    /**
     * Test of InitialGap methods.
     */
    @Test
    public void testInitialGap() {
        GraphicStroke cdt = new GraphicStroke();

        //check default
        assertEquals(null, cdt.getInitialGap());

        //check get/set
        cdt.setInitialGap(EXP_DOUBLE);
        assertEquals(EXP_DOUBLE, cdt.getInitialGap());
    }

    /**
     * Test of Gap methods.
     */
    @Test
    public void testGap() {
        GraphicStroke cdt = new GraphicStroke();

        //check default
        assertEquals(null, cdt.getGap());

        //check get/set
        cdt.setGap(EXP_DOUBLE);
        assertEquals(EXP_DOUBLE, cdt.getGap());
    }

}
