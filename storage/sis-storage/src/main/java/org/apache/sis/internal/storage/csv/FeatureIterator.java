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
package org.apache.sis.internal.storage.csv;

import java.util.Collection;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.DateTimeException;
import java.io.IOException;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.collection.BackingStoreException;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;


/**
 * Base implementation of iterators returned by {@link Store#features(boolean)}. This base class returns one feature
 * per line. For example iteration over the following file will produce 4 {@code Feature} instances, even if there is
 * actually only three distinct instances because the feature "a" is splitted on 2 lines:
 *
 * {@preformat text
 *    a,  10, 150, 11.0 2.0 12.0 3.0
 *    b,  10, 190, 10.0 2.0 11.0 3.0
 *    a, 150, 190, 12.0 3.0 10.0 3.0
 *    c,  10, 190, 12.0 1.0 10.0 2.0 11.0 3.0
 * }
 *
 * <b>Multi-threading:</b> {@code Iter} is not thread-safe.
 * However many {@code Iter} instances can be used concurrently for the same {@link Store} instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 */
class FeatureIterator implements Spliterator<Feature> {
    /**
     * Index of the column containing trajectory coordinates.
     * Columns before the trajectory are Moving Feature identifier {@code mfIdRef}, start time and end time.
     */
    static final int TRAJECTORY_COLUMN = 3;

    /**
     * Connection to the CSV file.
     */
    final Store store;

    /**
     * Name of the property where to store a value.
     * This array be considered unmodifiable and may be shared between many {@code Iter} instances.
     */
    final String[] propertyNames;

    /**
     * Converters from string representations to the values to store in the {@link #values} array.
     * This array be considered unmodifiable and may be shared between many {@code Iter} instances.
     */
    final ObjectConverter<String,?>[] converters;

    /**
     * All values found in a row. We need to remember those values between different executions
     * of the {@link #tryAdvance(Consumer)} method because the Moving Feature Specification said:
     * "If the value equals the previous value, the text for the value can be omitted."
     */
    final Object[] values;

    /**
     * Number of calls to {@link #trySplit()}. Created only if needed.
     */
    private AtomicInteger splitCount;

    /**
     * Creates a new iterator.
     */
    @SuppressWarnings({"unchecked", "rawtypes", "fallthrough"})
    FeatureIterator(final Store store) {
        this.store = store;
        final Collection<? extends PropertyType> properties = store.featureType.getProperties(true);
        converters    = new ObjectConverter[properties.size()];
        values        = new Object[converters.length];
        propertyNames = new String[converters.length];
        int i = -1;
        for (final PropertyType p : properties) {
            propertyNames[++i] = p.getName().tip().toString();
            /*
             * According Moving Features specification:
             *   Column 0 is the feature identifier (mfidref). There is nothing special to do here.
             *   Column 1 is the start time.
             *   Column 2 is the end time.
             *   Column 3 is the trajectory.
             *   Columns 4+ are custom attributes.
             */
            final ObjectConverter<String,?> c;
            switch (i) {
                case 1: // Fall through
                case 2: {
                    final TimeEncoding timeEncoding = store.timeEncoding();
                    if (timeEncoding != null) {
                        c = timeEncoding;
                        break;
                    }
                    /*
                     * If there are no time columns, then this column may be the trajectory (note that allowing
                     * CSV files without time is obviously a departure from Moving Features specification.
                     * The intent is to have a CSV format applicable to other features than moving ones).
                     * Fall through in order to process trajectory.
                     */
                }
                case TRAJECTORY_COLUMN: {
                    if (store.hasTrajectories()) {
                        c = GeometryParser.INSTANCE;
                        break;
                    }
                    /*
                     * If there are no trajectory columns, than this column is a custum attribute.
                     * CSV files without trajectories are not compliant with Moving Feature spec.,
                     * but we try to keep this reader a little bit more generic.
                     */
                }
                default: {
                    c = ObjectConverters.find(String.class, ((AttributeType) p).getValueClass());
                    break;
                }
            }
            converters[i] = c;
        }
    }

    /**
     * Creates a new iterator using the same configuration than the given iterator.
     * This constructor is for {@link #trySplit()} implementation only.
     */
    private FeatureIterator(final FeatureIterator other) {
        store         = other.store;
        splitCount    = other.splitCount;
        converters    = other.converters;
        propertyNames = other.propertyNames;
        values        = new Object[converters.length];
    }

    /**
     * If this spliterator can be partitioned, returns a {@code Spliterator} covering elements.
     * This method does not make any guarantees about iteration order; i.e. the returned iterator
     * is not guaranteed to cover a strict prefix of the elements.
     */
    @Override
    public Spliterator<Feature> trySplit() {
        if (splitCount == null) {
            splitCount = new AtomicInteger();
        }
        if (splitCount.incrementAndGet() < 8) {        // Arbitrary limit.
            return new FeatureIterator(this);
        }
        return null;
    }

    /**
     * Executes the given action only on the next feature, if any.
     */
    @Override
    public boolean tryAdvance(final Consumer<? super Feature> action) {
        try {
            return read(action, false);
        } catch (IOException | IllegalArgumentException | DateTimeException e) {
            throw new BackingStoreException(store.canNotParseFile(), e);
        }
    }

    /**
     * Executes the given action on all remaining features.
     */
    @Override
    public void forEachRemaining(final Consumer<? super Feature> action) {
        try {
            read(action, true);
        } catch (IOException | IllegalArgumentException | DateTimeException e) {
            throw new BackingStoreException(store.canNotParseFile(), e);
        }
    }

    /**
     * Executes the given action for the next feature or for all remaining features.
     * The features are assumed static, with one feature per line.
     * This method is for {@link #tryAdvance(Consumer)} and {@link #forEachRemaining(Consumer)} implementations.
     *
     * <h4>Multi-threading</h4>
     * {@code Iter} does not need to be thread-safe, so we do not perform synchronization for its {@link #values}.
     * Accesses to {@code Store} fields need to be thread-safe, but this method uses only immutable or thread-safe
     * objects from {@link Store}, so there is no need for {@code synchronize(Store.this)} statement.
     * The only object that need synchronization is {@link Store#source}, which is already synchronized.
     *
     * @param  action  the action to execute.
     * @param  all     {@code true} for executing the given action on all remaining features.
     * @return {@code false} if there are no remaining features after this method call.
     * @throws IOException if an I/O error occurred while reading a feature.
     * @throws IllegalArgumentException if parsing of a number failed, or other error.
     * @throws DateTimeException if parsing of a date failed.
     */
    private boolean read(final Consumer<? super Feature> action, final boolean all) throws IOException {
        final FixedSizeList elements = new FixedSizeList(values);
        String line;
        while ((line = store.readLine()) != null) {
            Store.split(line, elements);
            final Feature feature = store.featureType.newInstance();
            int i, n = elements.size();
            for (i=0; i<n; i++) {
                values[i] = converters[i].apply((String) values[i]);
                feature.setPropertyValue(propertyNames[i], values[i]);
            }
            n = values.length;
            for (; i<n; i++) {
                // For omitted elements, reuse previous value.
                feature.setPropertyValue(propertyNames[i], values[i]);
            }
            action.accept(feature);
            if (!all) return true;
            elements.clear();
        }
        return false;
    }

    /**
     * We do not know the number of features.
     */
    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    /**
     * Returns the characteristics of the iteration over feature instances.
     * The iteration is {@link #NONNULL} (i.e. {@link #tryAdvance(Consumer)} is not allowed
     * to return null value) and {@link #IMMUTABLE} (i.e. we do not support modification of
     * the CSV file while an iteration is in progress).
     * The iteration is not declared {@link #ORDERED} because {@link #trySplit()} does not
     * return a strict prefix of the elements.
     *
     * @return characteristics of iteration over the features in the CSV file.
     */
    @Override
    public int characteristics() {
        return NONNULL | IMMUTABLE;
    }
}
