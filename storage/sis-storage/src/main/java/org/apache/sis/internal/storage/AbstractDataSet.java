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

import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.util.logging.WarningListeners;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.identification.Identification;

/**
 * Base implementation of data sets contained in data stores.
 *
 * @author Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public abstract class AbstractDataSet extends AbstractResource implements DataSet {
 /**
     * Creates a new resource.
     *
     * @param store      the data store which contains this resource.
     * @param listeners  the set of registered warning listeners for the data store.
     */
    protected AbstractDataSet(final DataStore store, final WarningListeners<DataStore> listeners) {
        super(store, listeners);
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
                if (identification != null) {                                               // Paranoiac check.
                    for (final Extent extent : identification.getExtents()) {
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

}
