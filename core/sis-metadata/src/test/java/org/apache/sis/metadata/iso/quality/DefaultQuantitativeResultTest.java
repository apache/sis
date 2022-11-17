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
package org.apache.sis.metadata.iso.quality;

import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link DefaultQuantitativeResult}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
public final strictfp class DefaultQuantitativeResultTest extends TestCase {
    /**
     * Tests {@link DefaultQuantitativeResult#isEmpty()}. The {@code isEmpty()} method needs a special check
     * for the deprecated {@code "errorStatistic"} property because, contrarily to other deprecated properties,
     * that one has no replacement. Consequently no non-deprecated property is set as a result of redirection.
     * Because by default {@code isEmpty()} ignores deprecated properties,
     * it can cause {@link DefaultQuantitativeResult} to be wrongly considered as empty.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testIsEmpty() {
        final DefaultQuantitativeResult r = new DefaultQuantitativeResult();
        assertTrue(r.isEmpty());
        r.setErrorStatistic(new SimpleInternationalString("a description"));
        assertFalse(r.isEmpty());
    }
}
