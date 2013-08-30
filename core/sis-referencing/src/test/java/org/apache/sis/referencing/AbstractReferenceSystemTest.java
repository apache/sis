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

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import org.opengis.test.Validators;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link AbstractReferenceSystem}.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
@DependsOn(AbstractIdentifiedObjectTest.class)
public final strictfp class AbstractReferenceSystemTest extends TestCase {
    /**
     * Tests {@link AbstractReferenceSystem}.
     */
    @Test
    public void testCreateFromMap() {
        final Map<String,Object> properties = new HashMap<>();
        assertNull(properties.put("name",       "This is a name"));
        assertNull(properties.put("scope",      "This is a scope"));
        assertNull(properties.put("scope_fr",   "Valide dans ce domaine"));
        assertNull(properties.put("remarks",    "There is remarks"));
        assertNull(properties.put("remarks_fr", "Voici des remarques"));

        final AbstractReferenceSystem reference = new AbstractReferenceSystem(properties);
        Validators.validate(reference);

        assertEquals("name",       "This is a name",         reference.getName()   .getCode());
        assertEquals("scope",      "This is a scope",        reference.getScope()  .toString(Locale.ROOT));
        assertEquals("scope_fr",   "Valide dans ce domaine", reference.getScope()  .toString(Locale.FRENCH));
        assertEquals("remarks",    "There is remarks",       reference.getRemarks().toString(Locale.ENGLISH));
        assertEquals("remarks_fr", "Voici des remarques",    reference.getRemarks().toString(Locale.FRENCH));
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testCreateFromMap")
    public void testSerialization() {
        final Map<String,Object> properties = new HashMap<>(8);
        assertNull(properties.put("code",       "4326"));
        assertNull(properties.put("codeSpace",  "EPSG"));
        assertNull(properties.put("scope",      "This is a scope"));
        assertNull(properties.put("remarks",    "There is remarks"));
        assertNull(properties.put("remarks_fr", "Voici des remarques"));

        final AbstractReferenceSystem object = new AbstractReferenceSystem(properties);
        Validators.validate(object);

        assertNotSame(object, assertSerializedEquals(object));
    }
}
