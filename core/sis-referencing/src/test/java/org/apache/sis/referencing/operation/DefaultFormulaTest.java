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
package org.apache.sis.referencing.operation;

import org.apache.sis.io.wkt.Convention;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests {@link DefaultFormula}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.5
 * @module
 */
public final strictfp class DefaultFormulaTest extends TestCase {
    /**
     * Tests {@link DefaultFormula#toWKT()}.
     */
    @Test
    public void testWKT() {
        final DefaultFormula formula = new DefaultFormula("Mercator");
        assertWktEquals(Convention.WKT2_SIMPLIFIED, "Formula[“Mercator”]", formula);
        assertWktEquals(Convention.WKT2, "FORMULA[“Mercator”]", formula);
    }
}
