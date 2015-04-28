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
package org.apache.sis.internal.referencing;

import java.util.Collection;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.metadata.quality.Result;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link PositionalAccuracyConstant} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class PositionalAccuracyConstantTest extends TestCase {
    /**
     * Tests {@link PositionalAccuracyConstant} constants.
     */
    @Test
    public void testPositionalAccuracy() {
        assertEquals("Identity comparison",
                     PositionalAccuracyConstant.DATUM_SHIFT_APPLIED,
                     PositionalAccuracyConstant.DATUM_SHIFT_APPLIED);

        assertEquals("Identity comparison",
                     PositionalAccuracyConstant.DATUM_SHIFT_OMITTED,
                     PositionalAccuracyConstant.DATUM_SHIFT_OMITTED);

        assertNotSame(PositionalAccuracyConstant.DATUM_SHIFT_APPLIED,
                      PositionalAccuracyConstant.DATUM_SHIFT_OMITTED);

        final Collection<? extends Result> appliedResults = PositionalAccuracyConstant.DATUM_SHIFT_APPLIED.getResults();
        final Collection<? extends Result> omittedResults = PositionalAccuracyConstant.DATUM_SHIFT_OMITTED.getResults();
        final ConformanceResult applied = (ConformanceResult) TestUtilities.getSingleton(appliedResults);
        final ConformanceResult omitted = (ConformanceResult) TestUtilities.getSingleton(omittedResults);
        assertNotSame(applied, omitted);
        assertTrue ("DATUM_SHIFT_APPLIED", applied.pass());
        assertFalse("DATUM_SHIFT_OMITTED", omitted.pass());
        assertFalse(applied.equals(omitted));
        assertFalse(appliedResults.equals(omittedResults));
        assertFalse(PositionalAccuracyConstant.DATUM_SHIFT_APPLIED.equals(
                    PositionalAccuracyConstant.DATUM_SHIFT_OMITTED));
    }
}
