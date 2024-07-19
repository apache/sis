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
package org.apache.sis.referencing.internal;

import java.util.Map;
import java.util.Iterator;
import org.opengis.metadata.quality.Result;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.metadata.quality.QuantitativeResult;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.referencing.operation.CoordinateOperation;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link PositionalAccuracyConstant} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class PositionalAccuracyConstantTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public PositionalAccuracyConstantTest() {
    }

    /**
     * Tests {@link PositionalAccuracyConstant#equals(Object)}.
     */
    @Test
    public void testEquals() {
        assertEquals(PositionalAccuracyConstant.DATUM_SHIFT_APPLIED,
                     PositionalAccuracyConstant.DATUM_SHIFT_APPLIED);

        assertEquals(PositionalAccuracyConstant.DATUM_SHIFT_OMITTED,
                     PositionalAccuracyConstant.DATUM_SHIFT_OMITTED);

        assertNotSame(PositionalAccuracyConstant.DATUM_SHIFT_APPLIED,
                      PositionalAccuracyConstant.DATUM_SHIFT_OMITTED);

        assertNotEquals(PositionalAccuracyConstant.DATUM_SHIFT_APPLIED,
                        PositionalAccuracyConstant.DATUM_SHIFT_OMITTED);
    }

    /**
     * Verifies the property values of some {@link PositionalAccuracyConstant} constants.
     */
    @Test
    public void testQualitativeResults() {
        final Iterator<? extends Result> appliedResults = PositionalAccuracyConstant.DATUM_SHIFT_APPLIED.getResults().iterator();
        final Iterator<? extends Result> omittedResults = PositionalAccuracyConstant.DATUM_SHIFT_OMITTED.getResults().iterator();
        final var applied = assertInstanceOf(ConformanceResult.class, appliedResults.next());
        final var omitted = assertInstanceOf(ConformanceResult.class, omittedResults.next());
        assertNotSame(applied, omitted);
        assertTrue (applied.pass(), "DATUM_SHIFT_APPLIED");
        assertFalse(omitted.pass(), "DATUM_SHIFT_OMITTED");
        assertNotEquals(applied, omitted);
        assertNotEquals(appliedResults, omittedResults);

       assertNotEquals(assertInstanceOf(QuantitativeResult.class, appliedResults.next()),
                       assertInstanceOf(QuantitativeResult.class, omittedResults.next()));

       assertFalse(appliedResults.hasNext());
       assertFalse(omittedResults.hasNext());
    }

    /**
     * tests {@link PositionalAccuracyConstant#getLinearAccuracy(CoordinateOperation)}.
     */
    @Test
    public void testQuantitativeResults() {
        assertLinearAccuracyEquals(PositionalAccuracyConstant.DATUM_SHIFT_APPLIED,
                                   PositionalAccuracyConstant.DATUM_SHIFT_ACCURACY);
        assertLinearAccuracyEquals(PositionalAccuracyConstant.DATUM_SHIFT_OMITTED,
                                   PositionalAccuracyConstant.UNKNOWN_ACCURACY);
        assertLinearAccuracyEquals(PositionalAccuracyConstant.INDIRECT_SHIFT_APPLIED,
                                   PositionalAccuracyConstant.INDIRECT_SHIFT_ACCURACY);
    }

    /**
     * Asserts that the numerical accuracy associated to the given metadata is the expected value.
     *
     * @param metadata  the metadata to test.
     * @param expected  the expected accuracy value.
     */
    private static void assertLinearAccuracyEquals(final PositionalAccuracy metadata, final double expected) {
        var properties = Map.of(CoordinateOperation.NAME_KEY, "Dummy",
                                CoordinateOperation.COORDINATE_OPERATION_ACCURACY_KEY, metadata);
        var operation = new AbstractCoordinateOperation(properties, null, null, null, null);
        assertEquals(expected, PositionalAccuracyConstant.getLinearAccuracy(operation));
    }
}
