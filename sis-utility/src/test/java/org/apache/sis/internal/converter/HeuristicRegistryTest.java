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
package org.apache.sis.internal.converter;

import org.apache.sis.util.ObjectConverter;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;
import static org.apache.sis.internal.converter.HeuristicRegistry.SYSTEM;


/**
 * Tests the {@link HeuristicRegistry#SYSTEM} constant.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
@DependsOn(ConverterRegistryTest.class)
public final strictfp class HeuristicRegistryTest extends TestCase {
    /**
     * Tests the creation of {@link NumberConverter}.
     */
    @Test
    public void testSystem() {
        final ObjectConverter<Float,Double> c1 = SYSTEM.findExact(Float.class, Double.class);
        final ObjectConverter<Double,Float> c2 = SYSTEM.findExact(Double.class, Float.class);
        assertInstanceOf("Double ← Float", NumberConverter.class, c1);
        assertInstanceOf("Float ← Double", NumberConverter.class, c2);
        assertSame("inverse()", c2, c1.inverse());
        assertSame("inverse()", c1, c2.inverse());
    }
}
