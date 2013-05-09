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
package org.apache.sis.util.collection;

import java.util.Locale;
import org.opengis.util.InternationalString;
import org.junit.Test;
import org.apache.sis.test.TestCase;

import static org.apache.sis.test.Assert.*;
import org.apache.sis.test.foreigner.SerializableTableColumn;
import static org.apache.sis.util.collection.TableColumn.*;


/**
 * Tests the {@link TableColumn}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class TableColumnTest extends TestCase {
    /**
     * Test the header of some constants.
     */
    @Test
    public void testConstantHeader() {
        InternationalString i18n = NAME.getHeader();
        assertEquals("Name", i18n.toString(Locale.ROOT));
        assertEquals("Name", i18n.toString(Locale.ENGLISH));
        assertEquals("Nom",  i18n.toString(Locale.FRENCH));
        assertSame("Test caching", i18n, NAME.getHeader());

        i18n = TYPE.getHeader();
        assertEquals("Type", i18n.toString(Locale.ROOT));
        assertEquals("Type", i18n.toString(Locale.ENGLISH));
        assertEquals("Type", i18n.toString(Locale.FRENCH));
        assertSame("Test caching", i18n, TYPE.getHeader());
    }

    /**
     * Tests the serialization of predefined constants.
     */
    @Test
    public void testConstantSerialization() {
        assertSame(NAME, assertSerializedEquals(NAME));
        assertSame(TYPE, assertSerializedEquals(TYPE));
    }

    /**
     * Tests the serialization of custom constants declared in a foreigner package.
     */
    @Test
    public void testCustomSerialization() {
        assertSame(SerializableTableColumn.LATITUDE,  assertSerializedEquals(SerializableTableColumn.LATITUDE));
        assertSame(SerializableTableColumn.LONGITUDE, assertSerializedEquals(SerializableTableColumn.LONGITUDE));
    }
}
