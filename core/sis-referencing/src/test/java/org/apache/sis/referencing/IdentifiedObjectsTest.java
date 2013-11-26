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
package org.apache.sis.referencing;

import org.opengis.util.GenericName;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.test.mock.IdentifiedObjectMock;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link IdentifiedObjects}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class IdentifiedObjectsTest extends TestCase {
    /**
     * Tests {@link IdentifiedObjects#nameMatches(IdentifiedObject, IdentifiedObject)}.
     */
    @Test
    public void testNameMatches() {
        final GenericName name = DefaultFactories.SIS_NAMES.createGenericName(null, "myScope", "myName");
        IdentifiedObjectMock object = new IdentifiedObjectMock("myCode ", name); // Intentional trailing space.

        // Test the code.
        assertFalse(IdentifiedObjects.nameMatches(object, "other"));
        assertTrue (IdentifiedObjects.nameMatches(object, "myCode"));
        assertTrue (IdentifiedObjects.nameMatches(object, " my_code "));
        assertFalse(IdentifiedObjects.nameMatches(object, "testmyCode"));
        assertFalse(IdentifiedObjects.nameMatches(object, "other:myCode"));
        assertFalse(IdentifiedObjects.nameMatches(object, "test"));

        // Test the alias.
        assertTrue (IdentifiedObjects.nameMatches(object, "myName"));
        assertTrue (IdentifiedObjects.nameMatches(object, " My_name "));
        assertFalse(IdentifiedObjects.nameMatches(object, "myScope"));
        assertFalse(IdentifiedObjects.nameMatches(object, "other:myName"));
        assertFalse(IdentifiedObjects.nameMatches(object, "myScope:other"));
        assertFalse(IdentifiedObjects.nameMatches(object, "other:myScope:myName"));

        // Test non-letter and non-digits characters.
        object = new IdentifiedObjectMock("Mercator (1SP)", name);
        assertTrue (IdentifiedObjects.nameMatches(object, "Mercator (1SP)"));
        assertTrue (IdentifiedObjects.nameMatches(object, "Mercator_1SP"));
        assertFalse(IdentifiedObjects.nameMatches(object, "Mercator_2SP"));

        // Test diacritical signs
        object = new IdentifiedObjectMock("Réunion", name);
        assertTrue (IdentifiedObjects.nameMatches(object, "Réunion"));
        assertTrue (IdentifiedObjects.nameMatches(object, "Reunion"));
    }
}
