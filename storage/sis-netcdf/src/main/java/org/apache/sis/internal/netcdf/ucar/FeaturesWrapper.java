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
package org.apache.sis.internal.netcdf.ucar;

import java.util.stream.Stream;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.internal.netcdf.DiscreteSampling;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.DataStore;
import ucar.nc2.ft.DsgFeatureCollection;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;


/**
 * A wrapper around the UCAR {@code ucar.nc2.ft} package.
 * Created by {@link DecoderWrapper#getDiscreteSampling(Object)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.8
 * @module
 *
 * @todo we do not yet have an example of file that {@link ucar.nc2.ft.FeatureDatasetFactoryManager} can handle
 *       (maybe we don't use that class correctly).
 */
final class FeaturesWrapper extends DiscreteSampling {
    /**
     * The feature dataset provided by the UCAR library.
     */
    private final DsgFeatureCollection features;

    /**
     * Creates a new discrete sampling parser.
     *
     * @param  factory    the library for geometric objects, or {@code null} for the default.
     * @param  listeners  the set of registered warning listeners for the data store.
     * @param  lock       the lock to use in {@code synchronized(lock)} statements.
     * @throws IllegalArgumentException if the given library is non-null but not available.
     */
    FeaturesWrapper(final DsgFeatureCollection features, final GeometryLibrary factory, final StoreListeners listeners,
                    final DataStore lock)
    {
        super(factory, listeners, lock);
        this.features = features;
    }

    @Override
    public FeatureType getType() {
        throw new UnsupportedOperationException();      // TODO
    }

    /**
     * Returns the stream of features.
     */
    @Override
    public Stream<Feature> features(boolean parallel) {
        throw new UnsupportedOperationException();      // TODO
    }
}
