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

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Iterator;
import java.util.Collection;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link AbstractElement}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.3
 */
public final class AbstractElementTest extends TestCase {
    /**
     * Tests {@link Element#getDates()}.
     */
    @Test
    public void testGetDates() {
        final Instant   startTime = Instant.parse("2009-05-08T14:10:00Z");
        final Instant     endTime = Instant.parse("2009-05-12T21:45:00Z");
        final DefaultEvaluationMethod method = new DefaultEvaluationMethod();
        method.setDates(List.of(startTime, endTime));
        final AbstractElement element = new AbstractElement();
        element.setEvaluationMethod(method);

        @SuppressWarnings("deprecation")
        final Collection<? extends Date> dates = element.getDates();
        assertEquals(2, dates.size());
        final Iterator<? extends Date> it = dates.iterator();
        assertEquals(startTime, it.next().toInstant());
        assertEquals(endTime,   it.next().toInstant());
        assertFalse (it.hasNext());
    }
}
