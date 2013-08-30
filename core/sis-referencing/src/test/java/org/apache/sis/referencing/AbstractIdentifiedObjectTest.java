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
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link AbstractIdentifiedObject}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
@DependsOn(NamedIdentifierTest.class)
public final strictfp class AbstractIdentifiedObjectTest extends TestCase {
    /**
     * Tests the {@link AbstractIdentifiedObject#AbstractIdentifiedObject(Map)} constructor.
     */
    @Test
    public void testCreateFromMap() {
        final Map<String,Object> properties = new HashMap<>(10);
        assertNull(properties.put("name",             "This is a name"));
        assertNull(properties.put("remarks",          "There is remarks"));
        assertNull(properties.put("remarks_fr",       "Voici des remarques"));
        assertNull(properties.put("anchorPoint",      "Anchor point"));
        assertNull(properties.put("realizationEpoch", "Realization epoch"));
        assertNull(properties.put("validArea",        "Valid area"));

        final AbstractIdentifiedObject reference = new AbstractIdentifiedObject(properties);
        Validators.validate(reference);

        assertEquals("name",       "This is a name",      reference.getName().getCode());
        assertNull  ("codeSpace",                         reference.getName().getCodeSpace());
        assertNull  ("version",                           reference.getName().getVersion());
        assertEquals("remarks",    "There is remarks",    reference.getRemarks().toString(Locale.ENGLISH));
        assertEquals("remarks_fr", "Voici des remarques", reference.getRemarks().toString(Locale.FRENCH));
        assertTrue  ("identifiers",                       reference.getIdentifiers().isEmpty());
        assertNull  ("identifier",                        reference.getIdentifier());
        assertTrue  ("aliases",                           reference.getAlias().isEmpty());
    }
}
