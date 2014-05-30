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

import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link DefaultAssociationRole}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(DefaultFeatureTypeTest.class)
public final strictfp class DefaultAssociationRoleTest extends TestCase {
    /**
     * Creates an association to a twin town. We arbitrarily fix the maximum number
     * of occurrences to 1, even if in reality some cities have many twin towns.
     */
    static DefaultAssociationRole twinTown() {
        return new DefaultAssociationRole(singletonMap(DefaultAssociationRole.NAME_KEY, "twin town"),
                DefaultFeatureTypeTest.city(), 0, 1);
    }

    /**
     * Returns a City feature type which may have a twin town.
     */
    static DefaultFeatureType twinTownCity() {
        final DefaultAssociationRole twinTown = twinTown();
        return new DefaultFeatureType(singletonMap(DefaultFeatureType.NAME_KEY, "Twin town"), false,
                new DefaultFeatureType[] {twinTown.getValueType()}, twinTown);
    }

    /**
     * Tests serialization of an {@link DefaultAssociationRole} instance.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(twinTown());
    }

    /**
     * Tests {@link DefaultAssociationRole#getTitleProperty(FeatureAssociationRole)}.
     */
    @Test
    public void testGetTitleProperty() {
        final DefaultAssociationRole twinTown = twinTown();
        assertEquals("city", DefaultAssociationRole.getTitleProperty(twinTown));
    }

    /**
     * Tests {@link DefaultAssociationRole#toString()}.
     */
    @Test
    public void testToString() {
        final DefaultAssociationRole twinTown = twinTown();
        assertEquals("FeatureAssociationRole[“twin town” : City]", twinTown.toString());
    }
}
