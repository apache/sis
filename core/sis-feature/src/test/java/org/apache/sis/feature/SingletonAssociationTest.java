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
package org.apache.sis.feature;

import java.util.Map;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests {@link SingletonAssociation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.5
 */
@DependsOn({
    DefaultAssociationRoleTest.class,
    DenseFeatureTest.class
})
public final class SingletonAssociationTest extends TestCase {
    /**
     * Returns an association to use for testing purpose.
     *
     * <blockquote>“The earliest known town twinning in Europe was between Paderborn, Germany
     * and Le Mans, France in 836.” — source: Wikipedia</blockquote>
     */
    static AbstractAssociation twinTown() {
        final var twinTown = DefaultFeatureTypeTest.city().newInstance();
        twinTown.setPropertyValue("city", "Le Mans");
        twinTown.setPropertyValue("population", 143240);        // In 2011.
        final var association = new SingletonAssociation(DefaultAssociationRoleTest.twinTown(false));
        association.setValue(twinTown);
        return association;
    }

    /**
     * Tests attempt to set a value of the wrong type.
     */
    @Test
    public void testWrongValue() {
        final var association  = twinTown();
        final var population   = association.getRole().getValueType().getProperty("population");
        final var otherFeature = new DefaultFeatureType(Map.of(DefaultFeatureType.NAME_KEY, "Population"), false, null, population).newInstance();
        try {
            association.setValue(otherFeature);
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("twin town"));
            assertTrue(message, message.contains("Population"));
            assertTrue(message, message.contains("City"));
        }
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        final AbstractAssociation twinTown = twinTown();
        assertNotSame(twinTown, assertSerializedEquals(twinTown));
    }

    /**
     * Tests {@link SingletonAssociation#toString()}.
     */
    @Test
    public void testToString() {
        final AbstractAssociation twinTown = twinTown();
        assertEquals("FeatureAssociation[“twin town” : City] = Le Mans", twinTown.toString());
    }
}
