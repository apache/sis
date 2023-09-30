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
package org.apache.sis.portrayal;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.sis.map.ListChangeEvent;
import org.apache.sis.measure.NumberRange;
import org.junit.Test;
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;


/**
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class MapLayersTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public MapLayersTest() {
    }

    /**
     * Test the maplayers components list events.
     */
    @Test
    public void testListEvents() {

        final MapLayers layers = new MapLayers();
        final MapLayer layer1 = new MapLayer();
        final MapLayer layer2 = new MapLayer();

        final AtomicInteger eventNum = new AtomicInteger();
        layers.addPropertyChangeListener(MapLayers.COMPONENTS_PROPERTY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                assertTrue(evt instanceof ListChangeEvent<?>);
                final ListChangeEvent<?> levt = (ListChangeEvent<?>) evt;
                assertEquals(layers.getComponents(), levt.getOldValue());
                assertEquals(layers.getComponents(), levt.getNewValue());
                assertEquals(MapLayers.COMPONENTS_PROPERTY, levt.getPropertyName());
                assertEquals(layers, levt.getSource());
                int eventId = eventNum.incrementAndGet();
                switch (eventId) {
                    case 1 :
                        assertEquals(ListChangeEvent.Type.ADDED, levt.getType());
                        assertEquals(NumberRange.create(0, true, 0, true), levt.getRange());
                        assertEquals(1, levt.getItems().size());
                        assertEquals(layer1, levt.getItems().get(0));
                        break;
                    case 2 :
                        assertEquals(ListChangeEvent.Type.ADDED, levt.getType());
                        assertEquals(NumberRange.create(1, true, 1, true), levt.getRange());
                        assertEquals(1, levt.getItems().size());
                        assertEquals(layer2, levt.getItems().get(0));
                        break;
                    case 3 :
                        assertEquals(ListChangeEvent.Type.REMOVED, levt.getType());
                        assertEquals(NumberRange.create(1, true, 1, true), levt.getRange());
                        assertEquals(1, levt.getItems().size());
                        assertEquals(layer2, levt.getItems().get(0));
                        break;
                    case 4 :
                        assertEquals(ListChangeEvent.Type.CHANGED, levt.getType());
                        assertEquals(null, levt.getRange());
                        assertEquals(null, levt.getItems());
                        break;
                }

            }
        });

        layers.getComponents().add(layer1);
        assertEquals(1, eventNum.get());

        layers.getComponents().add(layer2);
        assertEquals(2, eventNum.get());

        layers.getComponents().remove(layer2);
        assertEquals(3, eventNum.get());

        layers.getComponents().set(0, layer2);
        assertEquals(4, eventNum.get());
    }

}
