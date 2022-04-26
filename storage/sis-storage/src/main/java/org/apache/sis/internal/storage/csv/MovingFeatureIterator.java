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

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import java.util.logging.LogRecord;
import java.time.Instant;
import java.time.DateTimeException;
import java.io.IOException;

// Branch-dependent imports
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;


/**
 * An extension of the feature iterator that merge the line segments in a single trajectory.
 * Line segments can be specified on a single line in the CSV file. All lines for the same
 * feature are combined together in a single trajectory. For example iteration over the following
 * file will produce 3 {@code Feature} instances instead of 4, because the two lines of features "a"
 * will be merged in a single feature instance:
 *
 * {@preformat text
 *    a,  10, 150, 11.0 2.0 12.0 3.0
 *    b,  10, 190, 10.0 2.0 11.0 3.0
 *    a, 150, 190, 12.0 3.0 10.0 3.0
 *    c,  10, 190, 12.0 1.0 10.0 2.0 11.0 3.0
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class MovingFeatureIterator extends FeatureIterator implements Consumer<LogRecord> {
    /**
     * Identifier of the feature in process of being parsed.
     */
    private String identifier;

    /**
     * Where to store the property values and the trajectory of the feature in process of being parsed.
     */
    private MovingFeatureBuilder builder;

    /**
     * All builders by feature name (not only the one being parsed).
     */
    private final Map<String,MovingFeatureBuilder> builders;

    /**
     * Creates a new iterator.
     */
    MovingFeatureIterator(final Store store) {
        super(store);
        builders = new LinkedHashMap<>();
    }

    /**
     * Creates all moving features.
     * This method can only be invoked after {@link #readMoving(Consumer, boolean)} completion.
     * This method is ignored if the CSV file contains only static features.
     */
    Feature[] createMovingFeatures() {
        int n = 0;
        final int np = values.length - TRAJECTORY_COLUMN;
        final Feature[] features = new Feature[builders.size()];
        for (final Map.Entry<String,MovingFeatureBuilder> entry : builders.entrySet()) {
            features[n++] = createMovingFeature(entry.getKey(), entry.getValue(), np);
        }
        return features;
    }

    /**
     * Creates the moving feature of the given name.
     * This method can only be invoked after {@link #readMoving(Consumer, boolean)}.
     *
     * @param  featureName  name of the feature to create.
     * @param  np           number of properties, ignoring the ones before the trajectory column.
     */
    @SuppressWarnings("unchecked")
    private Feature createMovingFeature(final String featureName, final MovingFeatureBuilder mf, final int np) {
        final Feature feature = store.featureType.newInstance();
        feature.setPropertyValue(propertyNames[0], featureName);
        mf.storeTimeRange(propertyNames[1], propertyNames[2], feature);
        int column = 0;
        if (store.hasTrajectories()) {
            mf.storeGeometry(featureName, column, store.spatialDimensionCount(), store.geometries,
                    (Attribute) feature.getProperty(propertyNames[TRAJECTORY_COLUMN]), this);
            column++;
        }
        while (column < np) {
            mf.storeAttribute(column, (Attribute<?>) feature.getProperty(propertyNames[TRAJECTORY_COLUMN + column]));
            column++;
        }
        return feature;
    }

    /**
     * Executes the given action for the next moving feature or for all remaining moving features.
     * This method assumes that the 4 first columns are as documented in the code inside constructor.
     *
     * @param  action  the action to execute as soon as the {@code mfidref} change, or {@code null} if none.
     * @param  all     {@code true} for executing the given action on all remaining features.
     * @return {@code false} if there are no remaining features after this method call.
     * @throws IOException if an I/O error occurred while reading a feature.
     * @throws IllegalArgumentException if parsing of a number failed, or other error.
     * @throws DateTimeException if parsing of a date failed.
     */
    boolean readMoving(final Consumer<? super Feature> action, final boolean all) throws IOException {
        final FixedSizeList elements = new FixedSizeList(values);
        final int np = values.length - TRAJECTORY_COLUMN;
        String line;
        while ((line = store.readLine()) != null) {
            Store.split(line, elements);
            int n = elements.size();
            for (int i=0; i<n; i++) {
                values[i] = converters[i].apply((String) values[i]);
            }
            final String  mfIdRef   =  (String)  values[0];
            final long    startTime = ((Instant) values[1]).toEpochMilli();
            final long    endTime   = ((Instant) values[2]).toEpochMilli();
            String        publish   = null;
            if (!mfIdRef.equals(identifier)) {
                publish    = identifier;
                identifier = mfIdRef;
                builder    = builders.computeIfAbsent(mfIdRef, (k) -> new MovingFeatureBuilder(builder, np));
            }
            builder.addTimeRange(startTime, endTime);
            for (int i=0; i<np; i++) {
                builder.addValue(i, startTime, endTime, values[i + TRAJECTORY_COLUMN]);
            }
            /*
             * If we started a new feature and the features are stored in sequential order,
             * we can publish the previous one right away.
             */
            if (publish != null && action != null) {
                action.accept(createMovingFeature(publish, builders.remove(publish), np));
                if (!all) return true;
            }
            elements.clear();
        }
        return false;
    }

    /**
     * Invoked when a warning occurred while computing the geometry.
     */
    @Override
    public void accept(final LogRecord warning) {
        store.log(warning);
    }
}
