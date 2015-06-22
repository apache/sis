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
package org.apache.sis.referencing.cs;

import java.util.Collections;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link DefaultCompoundCS} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
@DependsOn(org.apache.sis.internal.metadata.AxisDirectionsTest.class)
public final strictfp class DefaultCompoundCSTest extends TestCase {
    /**
     * Tests {@link DefaultCompoundCS} construction.
     */
    @Test
    public void testConstruction() {
        final DefaultCompoundCS cs = new DefaultCompoundCS(
                HardCodedCS.PROJECTED,
                new DefaultVerticalCS(Collections.singletonMap(DefaultVerticalCS.NAME_KEY,
                        HardCodedAxes.HEIGHT_cm.getName()), HardCodedAxes.HEIGHT_cm),
                HardCodedCS.DAYS
        );
        assertEquals("Compound CS: East (m), North (m), Up (cm), Future (d).", cs.getName().getCode());
    }
}
