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
package org.apache.sis.referencing.gazetteer;

import java.util.Map;
import java.util.HashMap;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link ReferencingByIdentifiers}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@DependsOn(LocationTypeTest.class)
public final strictfp class ReferencingByIdentifiersTest extends TestCase {
    /**
     * Creates the example given in annex B of ISO 19112:2003.
     *
     * @param  inherit  {@code false} for defining all properties of all location types explicitely even
     *                  in case of redundancy, or {@code true} for relying on inheritance when possible.
     */
    private static ReferencingByIdentifiers create(final boolean inherit) {
        final Map<String,Object> properties = new HashMap<>();
        assertNull(properties.put(ReferencingByIdentifiers.NAME_KEY, "UK property addressing"));
        assertNull(properties.put(ReferencingByIdentifiers.DOMAIN_OF_VALIDITY_KEY, new DefaultExtent("UK", null, null, null)));
        assertNull(properties.put(ReferencingByIdentifiers.THEME_KEY, "property"));
        assertNull(properties.put(ReferencingByIdentifiers.OVERALL_OWNER_KEY, new DefaultOrganisation("Office for National Statistics", null, null, null)));
        return new ReferencingByIdentifiers(properties, LocationTypeTest.create(inherit));
    }

    /**
     * Tests the equality and hash code value computation.
     */
    @Test
    public void testEquals() {
        final ReferencingByIdentifiers t1 = create(false);
        final ReferencingByIdentifiers t2 = create(true);
        assertEquals("hashCode", t1.hashCode(), t2.hashCode());
        assertEquals("equals", t1, t2);
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testEquals")
    public void testSerialization() {
        assertSerializedEquals(create(true));
    }
}
