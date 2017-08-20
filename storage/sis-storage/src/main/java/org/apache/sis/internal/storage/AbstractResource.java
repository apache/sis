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
package org.apache.sis.internal.storage;

import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.identification.Identification;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.logging.WarningListeners;

// Branch-dependent imports
import org.opengis.metadata.identification.DataIdentification;


/**
 * Base implementation of resources contained in data stores.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 *
 * @todo this class may be removed if we refactor {@link Resource} as an abstract class.
 */
public abstract class AbstractResource implements Resource {
    /**
     * The data store which contains this resource.
     */
    protected final DataStore store;

    /**
     * The set of registered warning listeners for the data store.
     *
     * @todo Remove this field if we move {@code AbstractResource} to the {@code org.apache.sis.storage} package,
     *       since we would be able to access {@code DataStore.listeners} directly.
     */
    private final WarningListeners<DataStore> listeners;

    /**
     * Creates a new resource.
     *
     * @param store      the data store which contains this resource.
     * @param listeners  the set of registered warning listeners for the data store.
     */
    protected AbstractResource(final DataStore store, final WarningListeners<DataStore> listeners) {
        this.store     = store;
        this.listeners = listeners;
    }

    /**
     * Returns the spatio-temporal envelope of this resource.
     * The default implementation computes the union of all {@link GeographicBoundingBox} in the resource metadata,
     * assuming the {@linkplain org.apache.sis.referencing.CommonCRS#defaultGeographic() default geographic CRS}
     * (usually WGS 84).
     *
     * @return the spatio-temporal resource extent.
     * @throws DataStoreException if an error occurred while reading or computing the envelope.
     */
    @Override
    public Envelope getEnvelope() throws DataStoreException {
        final Metadata metadata = getMetadata();
        GeneralEnvelope bounds = null;
        if (metadata != null) {
            for (final Identification identification : metadata.getIdentificationInfo()) {
                if (identification instanceof DataIdentification) {
                    for (final Extent extent : ((DataIdentification) identification).getExtents()) {
                        if (extent != null) {                                               // Paranoiac check.
                            for (final GeographicExtent ge : extent.getGeographicElements()) {
                                if (ge instanceof GeographicBoundingBox) {
                                    final GeneralEnvelope env = new GeneralEnvelope((GeographicBoundingBox) ge);
                                    if (bounds == null) {
                                        bounds = env;
                                    } else {
                                        bounds.add(env);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return bounds;
    }

    /**
     * The set of registered warning listeners for the data store.
     *
     * @return the registered warning listeners for the data store.
     */
    protected final WarningListeners<DataStore> listeners() {
        return listeners;
    }
}
