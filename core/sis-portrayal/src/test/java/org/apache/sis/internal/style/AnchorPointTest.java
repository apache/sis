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
 * Tests for {@link org.apache.sis.internal.style.AnchorPoint}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class AnchorPointTest extends AbstractStyleTests {

    /**
     * Test of AnchorPointXY methods.
     */
    @Test
    public void testAnchorPointXY() {
        AnchorPoint cdt = new AnchorPoint();

        //check defaults
        assertEquals(FF.literal(0.5), cdt.getAnchorPointX());
        assertEquals(FF.literal(0.5), cdt.getAnchorPointY());

        //check get/set
        cdt.setAnchorPointX(EXP_DOUBLE);
        cdt.setAnchorPointY(EXP_DOUBLE_2);
        assertEquals(EXP_DOUBLE, cdt.getAnchorPointX());
        assertEquals(EXP_DOUBLE_2, cdt.getAnchorPointY());
    }

}
