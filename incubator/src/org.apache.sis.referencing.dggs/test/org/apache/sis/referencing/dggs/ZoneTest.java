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
package org.apache.sis.referencing.dggs;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.opengis.referencing.gazetteer.LocationType;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests the default methods of {@link Zone}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ZoneTest {

    /**
     * A reference system whose hierarchy has no long identifiers.
     */
    private static final class TextOnlyDggrs extends DiscreteGlobalGridReferenceSystem {
        TextOnlyDggrs() {
            super(Map.of(NAME_KEY, "text only"), new LocationType[0]);
        }

        @Override public List<String>            getKeywords()     {return List.of();}
        @Override public URI                     getUri()          {return null;}
        @Override public ZonalReferenceSystem    getZonalSystem()  {return null;}
        @Override public SubZoneOrder            getSubZoneOrder() {return null;}
        @Override public Coder                   createCoder()     {return null;}

        @Override
        public DiscreteGlobalGridSystem getGridSystem() {
            final DiscreteGlobalGridHierarchy hierarchy = proxy(DiscreteGlobalGridHierarchy.class, (p, method, args) ->
                    method.getName().equals("supportLongIdentifiers") ? Boolean.FALSE : null);
            return proxy(DiscreteGlobalGridSystem.class, (p, method, args) ->
                    method.getName().equals("getHierarchy") ? hierarchy : null);
        }
    }

    /**
     * Creates an implementation of the given interface which delegates to the given handler.
     */
    private static <T> T proxy(final Class<T> type, final InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(ZoneTest.class.getClassLoader(), new Class<?>[] {type}, handler));
    }

    /**
     * Creates a zone at level 0 with the given text identifier, which has no long identifier.
     */
    private static Zone textZone(final DiscreteGlobalGridReferenceSystem dggrs, final String identifier) {
        return proxy(Zone.class, (zone, method, args) -> {
            switch (method.getName()) {
                case "getLocationType":   return new RefinementLevel(dggrs, 0);
                case "getTextIdentifier": return identifier;
                case "getLongIdentifier": throw new UnsupportedOperationException();
                default: return method.isDefault() ? InvocationHandler.invokeDefault(zone, method, args) : null;
            }
        });
    }

    /**
     * Tests {@link Zone#compareTo(Zone)} on zones without long identifiers.
     * The zones shall be sorted by their text identifiers.
     */
    @Test
    public void testCompareToWithTextIdentifiers() {
        final DiscreteGlobalGridReferenceSystem dggrs = new TextOnlyDggrs();
        final Zone a = textZone(dggrs, "a");
        final Zone b = textZone(dggrs, "b");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(textZone(dggrs, "a")));
    }

    /**
     * Tests {@link Zone#compareTo(Zone)} on zones of different levels: the coarsest level comes first.
     */
    @Test
    public void testCompareToLevels() throws Exception {
        final DiscreteGlobalGridReferenceSystem dggrs = new S2Dggrs();
        final Zone face = dggrs.getGridSystem().getHierarchy().getGrids().get(0).getZones().findFirst().orElseThrow();
        final Zone child = face.getChildren().iterator().next();
        assertTrue(face.compareTo(child) < 0);
        assertTrue(child.compareTo(face) > 0);
    }
}
