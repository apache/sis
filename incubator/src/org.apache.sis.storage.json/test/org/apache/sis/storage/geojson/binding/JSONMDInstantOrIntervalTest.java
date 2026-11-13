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
package org.apache.sis.storage.geojson.binding;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalPrimitive;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link JSONMDInstantOrInterval}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class JSONMDInstantOrIntervalTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testInstant() throws Exception {
        final JSONMDInstantOrInterval value = mapper.readValue("\"2020-01-15\"", JSONMDInstantOrInterval.class);
        assertFalse(value.isInterval());
        assertEquals(LocalDate.of(2020, 1, 15), value.getBegin().getValue());
        assertEquals("\"2020-01-15\"", mapper.writeValueAsString(value));

        final TemporalPrimitive primitive = value.toTemporal();
        final Instant instant = assertInstanceOf(Instant.class, primitive);
        assertEquals(LocalDate.of(2020, 1, 15), instant.getPosition());
    }

    @Test
    public void testInterval() throws Exception {
        final JSONMDInstantOrInterval value = mapper.readValue("[\"2020-01\", \"2020-06\"]", JSONMDInstantOrInterval.class);
        assertTrue(value.isInterval());
        assertEquals("[\"2020-01\",\"2020-06\"]", mapper.writeValueAsString(value));

        final Period period = assertInstanceOf(Period.class, value.toTemporal());
        assertEquals(value.getBegin().getValue(), period.getBeginning().getPosition());
        assertEquals(value.getEnd().getValue(),   period.getEnding().getPosition());
    }

    @Test
    public void testOpenInterval() throws Exception {
        final JSONMDInstantOrInterval value = mapper.readValue("[\"2020-01-01\", \"..\"]", JSONMDInstantOrInterval.class);
        assertTrue(value.isInterval());
        assertNull(value.getEnd());
        assertEquals(List.of("2020-01-01", JSONMDInstantOrInterval.OPEN), value.toJSON());

        final JSONMDInstantOrInterval open = new JSONMDInstantOrInterval(null, null);
        assertNull(open.toTemporal());
    }

    @Test
    public void testInvalid() {
        assertThrows(Exception.class, () -> mapper.readValue("[\"2020-01-01\"]", JSONMDInstantOrInterval.class));
        assertThrows(Exception.class, () -> mapper.readValue("12", JSONMDInstantOrInterval.class));
    }
}
