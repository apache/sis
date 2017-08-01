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
package org.apache.sis.storage;

import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;

/**
 * A resource is an accessor to geospatial data.
 * The user should test if the resource is a {@code CoverageResource} or {@code FeatureResource}.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Resource {

    /**
     * Gets resource metadata object.
     *
     * @return metadata about the resource, never null.
     * @throws DataStoreException if an I/O error occurs.
     */
    Metadata getMetadata() throws DataStoreException;

    /**
     * Returns the spatio-temporal envelope of this resource.
     *
     * @return the spatio-temporal envelope, never null.
     * @throws DataStoreException if an I/O or decoding error occurs.
     */
    Envelope getEnvelope() throws DataStoreException;

}
