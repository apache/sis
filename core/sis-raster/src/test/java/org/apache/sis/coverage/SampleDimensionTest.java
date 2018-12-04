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
package org.apache.sis.coverage;

import java.util.Set;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.operation.transform.TransferFunction;
import org.apache.sis.measure.Units;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link SampleDimension}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class SampleDimensionTest extends TestCase {
    /**
     * Tests the creation of a sample dimensions using builder, then verifies the dimension properties.
     */
    @Test
    public void testBuilder() {
        final int    lower  = 10;
        final int    upper  = 200;
        final double scale  = 0.1;
        final double offset = 5.0;
        final SampleDimension test = new SampleDimension.Builder()
                .addQualitative(null,      0)           // Default to "No data" name, potentially locale.
                .addQualitative("Clouds",  1)
                .addQualitative("Lands", 255)
                .addQuantitative("Temperature", lower, upper, scale, offset, Units.CELSIUS)
                .build();

        final Set<Number> nodataValues = test.getNoDataValues();
        assertArrayEquals(new Integer[] {0, 1, 255}, nodataValues.toArray());

        final TransferFunction tr = test.getTransferFunctionFormula().get();
        assertFalse ("identity",  test.getTransferFunction().get().isIdentity());
        assertFalse ("identity",  tr.getTransform().isIdentity());
        assertEquals("scale",     scale,  tr.getScale(),  STRICT);
        assertEquals("offset",    offset, tr.getOffset(), STRICT);

        NumberRange<?> range = test.getSampleRange().get();
        assertEquals("minimum",   0,   range.getMinDouble(), STRICT);
        assertEquals("maximum",   255, range.getMaxDouble(), STRICT);

        range = test.getMeasurementRange().get();
        assertEquals("minimum", lower*scale+offset, range.getMinDouble(true),  CategoryTest.EPS);
        assertEquals("maximum", upper*scale+offset, range.getMaxDouble(false), CategoryTest.EPS);

        assertEquals("units", Units.CELSIUS, test.getUnits().get());
    }
}
