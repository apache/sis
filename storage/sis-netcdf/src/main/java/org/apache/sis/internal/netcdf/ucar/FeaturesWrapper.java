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
import org.apache.sis.internal.netcdf.DiscreteSampling;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.storage.DataStore;
import org.apache.sis.util.logging.WarningListeners;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.util.GenericName;
import ucar.nc2.ft.FeatureCollection;


/**
 * A wrapper around the UCAR {@code ucar.nc2.ft} package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class FeaturesWrapper extends DiscreteSampling {
    /**
     * The feature dataset provided by the UCAR library.
     */
    private final FeatureCollection features;

    /**
     * Creates a new discrete sampling parser.
     *
     * @param  factory    the library for geometric objects, or {@code null} for the default.
     * @param  listeners  the set of registered warning listeners for the data store.
     * @throws IllegalArgumentException if the given library is non-null but not available.
     */
    FeaturesWrapper(final FeatureCollection features, final GeometryLibrary factory, final WarningListeners<DataStore> listeners) {
        super(factory, listeners);
        this.features = features;
    }

    @Override
    public GenericName getIdentifier() {
        throw new UnsupportedOperationException();      // TODO
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
