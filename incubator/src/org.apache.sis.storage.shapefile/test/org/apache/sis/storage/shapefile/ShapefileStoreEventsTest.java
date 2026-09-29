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
package org.apache.sis.storage.shapefile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.internal.shared.ContentEvent;
import org.apache.sis.storage.internal.shared.FeatureSetContentEvent;
import org.apache.sis.storage.internal.shared.FeatureSetModelEvent;
import org.apache.sis.storage.internal.shared.ModelEvent;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.ResourceId;


/**
 * Tests the {@link FeatureSetModelEvent} and {@link FeatureSetContentEvent} sent by
 * {@link ShapefileStore} when the shapefile is created or its features are modified.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class ShapefileStoreEventsTest {

    private static final FilterFactory<Feature,Object,Object> FF = DefaultFilterFactory.forFeatures();

    public ShapefileStoreEventsTest() {
    }

    /**
     * Collects the events sent to a listener, so that a test can assert what was sent and in
     * which order.
     */
    private static final class Recorder<E extends StoreEvent> implements StoreListener<E> {
        /**
         * All events received so far, in the order they were received.
         */
        final List<E> events = new ArrayList<>();

        @Override
        public void eventOccurred(final E event) {
            events.add(event);
        }

        /**
         * Returns the only event received, failing if a different number of events was received.
         */
        E single() {
            assertEquals(1, events.size(), () -> "Expected exactly one event but got " + events);
            return events.get(0);
        }

        /**
         * Asserts that no event at all was received.
         */
        void assertEmpty() {
            assertTrue(events.isEmpty(), () -> "Expected no events but received: " + events);
        }
    }

    /**
     * Creates a store for a shapefile which does not exist yet.
     */
    private static ShapefileStore create(final Path folder) throws DataStoreException {
        return new ShapefileStore(null, new StorageConnector(folder.resolve("test.shp")));
    }

    /**
     * Creates a store holding the given number of features, from one to three.
     * The features are numbered from one, as are the record numbers they are written to,
     * so the feature identifiers are {@code "test.1"} to {@code "test.3"}.
     */
    private static ShapefileStore createPopulated(final Path folder, final int count) throws DataStoreException {
        final ShapefileStore store = create(folder);
        store.updateType(ShapefileStoreTest.createType());
        final FeatureType type = store.getType();
        final var features = new ArrayList<Feature>(3);
        if (count >= 1) features.add(ShapefileStoreTest.createFeature1(type));
        if (count >= 2) features.add(ShapefileStoreTest.createFeature2(type));
        if (count >= 3) features.add(ShapefileStoreTest.createFeature3(type));
        store.add(features.iterator());
        return store;
    }

    /**
     * Returns the identifiers designated by the filter of a content event.
     * The filter is a single {@link ResourceId} when only one feature was modified,
     * and the union of the identifiers of all the modified features otherwise.
     */
    private static Set<String> identifiers(final Filter<Feature> filter) {
        assertNotNull(filter, "Event carries no identifier.");
        final var ids = new LinkedHashSet<String>();
        collect(filter, ids);
        return ids;
    }

    /**
     * Adds to the given set the identifiers designated by the given filter.
     */
    private static void collect(final Filter<?> filter, final Set<String> ids) {
        if (filter instanceof ResourceId<?> rid) {
            assertTrue(ids.add(rid.getIdentifier()), () -> "Duplicated identifier: " + rid.getIdentifier());
        } else if (filter instanceof LogicalOperator<?> op) {
            for (final Filter<?> operand : op.getOperands()) {
                collect(operand, ids);
            }
        } else {
            fail("Filter is neither a ResourceId nor a union of them: " + filter);
        }
    }

    /**
     * Asserts that the filter of a content event selects exactly the given features of the store,
     * which is what makes the filter usable by the listener. Applicable only to the features which
     * still exist after the modification, therefore not to a deletion.
     */
    private static void assertSelects(final FeatureSet data, final Filter<Feature> filter,
            final String... expected) throws DataStoreException
    {
        final var selected = new ArrayList<String>();
        try (Stream<Feature> features = data.features(false)) {
            features.forEach((feature) -> {
                if (filter.test(feature)) {
                    selected.add(feature.getPropertyValue(AttributeConvention.IDENTIFIER).toString());
                }
            });
        }
        assertArrayEquals(expected, selected.toArray(String[]::new), "Features selected by the event filter");
    }

    /**
     * Tests the model event sent when a shapefile is created by {@code updateType(…)}.
     * The type did not exist before, so the event is an addition.
     */
    @Test
    public void testCreateSendsModelEvent(@TempDir final Path folder) throws DataStoreException {
        try (ShapefileStore store = create(folder)) {
            final var model   = new Recorder<FeatureSetModelEvent>();
            final var content = new Recorder<FeatureSetContentEvent>();
            store.addListener(FeatureSetModelEvent.class, model);
            store.addListener(FeatureSetContentEvent.class, content);

            store.updateType(ShapefileStoreTest.createType());

            final FeatureSetModelEvent event = model.single();
            assertSame(store, event.getSource(), "The store is the resource the users hold.");
            assertEquals(FeatureSetModelEvent.Type.ADD, event.getType());
            assertNull(event.getOldFeatureType(), "Nothing existed before the creation.");
            assertEquals(store.getType(), event.getNewFeatureType(),
                         "The event must carry the effective type, as read back from the files.");
            content.assertEmpty();
        }
    }

    /**
     * Tests the content event sent when features are added.
     */
    @Test
    public void testAddSendsContentEvent(@TempDir final Path folder) throws DataStoreException {
        try (ShapefileStore store = create(folder)) {
            store.updateType(ShapefileStoreTest.createType());
            final FeatureType type = store.getType();
            final var content = new Recorder<FeatureSetContentEvent>();
            final var model   = new Recorder<FeatureSetModelEvent>();
            store.addListener(FeatureSetContentEvent.class, content);
            store.addListener(FeatureSetModelEvent.class, model);

            store.add(List.of(ShapefileStoreTest.createFeature1(type),
                              ShapefileStoreTest.createFeature2(type)).iterator());

            final FeatureSetContentEvent event = content.single();
            assertSame(store, event.getSource());
            assertEquals(FeatureSetContentEvent.Type.ADD, event.getType());
            assertEquals(Set.of("test.1", "test.2"), identifiers(event.getIds()));
            assertSelects(store, event.getIds(), "test.1", "test.2");
            model.assertEmpty();
        }
    }

    /**
     * Verifies that the identifiers reported for added features are the record numbers actually
     * used, which are not a simple count when the file holds records marked as deleted.
     */
    @Test
    public void testAddAfterRemoveReportsRealIdentifiers(@TempDir final Path folder) throws DataStoreException {
        try (ShapefileStore store = createPopulated(folder, 2)) {
            store.removeIf(FF.equal(FF.property("id"), FF.literal(1)));
            final FeatureType type = store.getType();
            final var content = new Recorder<FeatureSetContentEvent>();
            store.addListener(FeatureSetContentEvent.class, content);

            store.add(List.of(ShapefileStoreTest.createFeature3(type)).iterator());

            final FeatureSetContentEvent event = content.single();
            assertEquals(FeatureSetContentEvent.Type.ADD, event.getType());
            assertEquals(Set.of("test.3"), identifiers(event.getIds()),
                         "The deleted record keeps its slot, so the new feature is the third one.");
            assertSelects(store, event.getIds(), "test.3");
        }
    }

    /**
     * Tests the content event sent when features are removed.
     */
    @Test
    public void testRemoveSendsContentEvent(@TempDir final Path folder) throws DataStoreException {
        try (ShapefileStore store = createPopulated(folder, 3)) {
            final var content = new Recorder<FeatureSetContentEvent>();
            store.addListener(FeatureSetContentEvent.class, content);

            store.removeIf(FF.equal(FF.property("id"), FF.literal(2)));

            final FeatureSetContentEvent event = content.single();
            assertSame(store, event.getSource());
            assertEquals(FeatureSetContentEvent.Type.DELETE, event.getType());
            assertEquals(Set.of("test.2"), identifiers(event.getIds()));
        }
    }

    /**
     * Tests the content event sent when features are updated.
     */
    @Test
    public void testReplaceSendsContentEvent(@TempDir final Path folder) throws DataStoreException {
        try (ShapefileStore store = createPopulated(folder, 3)) {
            final var content = new Recorder<FeatureSetContentEvent>();
            store.addListener(FeatureSetContentEvent.class, content);

            store.replaceIf(FF.equal(FF.property("id"), FF.literal(1)), (feature) -> {
                feature.setPropertyValue("text", "modified");
                return feature;
            });

            final FeatureSetContentEvent event = content.single();
            assertEquals(FeatureSetContentEvent.Type.UPDATE, event.getType());
            assertEquals(Set.of("test.1"), identifiers(event.getIds()));
            assertSelects(store, event.getIds(), "test.1");
        }
    }

    /**
     * Verifies that a replacement whose operator returns {@code null} is reported as a deletion,
     * which is what it does to the file.
     */
    @Test
    public void testReplaceByNullSendsDeleteEvent(@TempDir final Path folder) throws DataStoreException {
        try (ShapefileStore store = createPopulated(folder, 3)) {
            final var content = new Recorder<FeatureSetContentEvent>();
            store.addListener(FeatureSetContentEvent.class, content);

            store.replaceIf(FF.equal(FF.property("id"), FF.literal(3)), (feature) -> null);

            final FeatureSetContentEvent event = content.single();
            assertEquals(FeatureSetContentEvent.Type.DELETE, event.getType());
            assertEquals(Set.of("test.3"), identifiers(event.getIds()));
        }
    }

    /**
     * Tests the content event sent by a compaction, which renumbers every surviving record and
     * therefore changes every identifier. No filter can express that mapping, so none is sent.
     */
    @Test
    public void testCompactSendsContentEvent(@TempDir final Path folder) throws DataStoreException {
        try (ShapefileStore store = createPopulated(folder, 3)) {
            store.removeIf(FF.equal(FF.property("id"), FF.literal(2)));
            final var content = new Recorder<FeatureSetContentEvent>();
            store.addListener(FeatureSetContentEvent.class, content);

            store.compact();

            final FeatureSetContentEvent event = content.single();
            assertSame(store, event.getSource());
            assertEquals(FeatureSetContentEvent.Type.UPDATE, event.getType());
            assertNull(event.getIds(), "Every identifier changed, no filter can designate them.");
        }
    }

    /**
     * Verifies that the operations which change nothing send nothing.
     */
    @Test
    public void testNoChangeSendsNoEvent(@TempDir final Path folder) throws DataStoreException {
        try (ShapefileStore store = createPopulated(folder, 2)) {
            final var content = new Recorder<FeatureSetContentEvent>();
            store.addListener(FeatureSetContentEvent.class, content);

            store.add(Collections.emptyIterator());
            content.assertEmpty();

            store.removeIf(FF.equal(FF.property("id"), FF.literal(999)));
            content.assertEmpty();

            store.replaceIf(FF.equal(FF.property("id"), FF.literal(999)), (feature) -> feature);
            content.assertEmpty();

            store.compact();
            content.assertEmpty();
        }
    }

    /**
     * Verifies that a listener registered for a parent event type receives the events,
     * which is how a listener interested in any change of the resource registers itself.
     */
    @Test
    public void testListenerOnParentEventType(@TempDir final Path folder) throws DataStoreException {
        try (ShapefileStore store = create(folder)) {
            final var model   = new Recorder<ModelEvent>();
            final var content = new Recorder<ContentEvent>();
            store.addListener(ModelEvent.class, model);
            store.addListener(ContentEvent.class, content);

            store.updateType(ShapefileStoreTest.createType());
            final FeatureType type = store.getType();
            store.add(List.of(ShapefileStoreTest.createFeature1(type)).iterator());

            assertInstanceOf(FeatureSetModelEvent.class, model.single());
            assertInstanceOf(FeatureSetContentEvent.class, content.single());
        }
    }

    /**
     * Verifies that a removed listener stops receiving the events.
     */
    @Test
    public void testRemovedListener(@TempDir final Path folder) throws DataStoreException {
        try (ShapefileStore store = createPopulated(folder, 2)) {
            final var content = new Recorder<FeatureSetContentEvent>();
            store.addListener(FeatureSetContentEvent.class, content);
            store.removeListener(FeatureSetContentEvent.class, content);

            store.removeIf(FF.equal(FF.property("id"), FF.literal(1)));
            content.assertEmpty();
        }
    }

}
