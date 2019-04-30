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


/**
 * Collection of features that share a common set of attributes or properties.
 * Features may be organized in coverages, but not necessarily.
 * The common set of properties is described by {@linkplain org.apache.sis.feature.DefaultFeatureType feature types},
 * grid geometries or sample dimensions, depending on the {@code DataSet} subtype.
 * The actual values are provided by methods defined in {@code DataSet} subtypes.
 *
 * <div class="note"><b>Example:</b>
 * the features contained in a {@code DataSet} could be all bridges in a city. A {@code DataSet} can be associated to
 * one {@code FeatureType} which specifies that all bridges shall have {@code "construction date"} and {@code "height"}
 * attributes, and an arbitrary amount of {@code Feature} instances which contains the actual values for all bridges in
 * the dataset.</div>
 *
 * <div class="section">Metadata</div>
 * Datasets should have {@link #getMetadata() metadata} /
 * {@link org.apache.sis.metadata.iso.DefaultMetadata#getMetadataScopes() metadataScope} /
 * {@link org.apache.sis.metadata.iso.DefaultMetadataScope#getResourceScope() resourceScope} sets to
 * {@link org.opengis.metadata.maintenance.ScopeCode#DATASET}.
 * If this datasets is part of a series or an {@link Aggregate}, the aggregate name should be declared
 * as the {@linkplain org.apache.sis.metadata.iso.DefaultMetadata#getParentMetadata() parent metadata}.
 * That parent metadata is often the same instance than {@link DataStore#getMetadata()}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public interface DataSet extends Resource {
    /**
     * Returns the spatiotemporal extent of this resource in its most natural coordinate reference system.
     * The following relationship to {@linkplain #getMetadata()} should hold (departures may exist):
     *
     * <ul>
     *   <li>The envelope should be contained in the union of all geographic, vertical or temporal extents
     *       described by {@code metadata} /
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getIdentificationInfo() identificationInfo} /
     *       {@link org.apache.sis.metadata.iso.identification.AbstractIdentification#getExtents() extent}.</li>
     *   <li>The coordinate reference system should be one of the instances returned by
     *       {@link org.apache.sis.metadata.iso.DefaultMetadata#getReferenceSystemInfo() referenceSystemInfo}.</li>
     * </ul>
     *
     * The envelope should use the coordinate reference system (CRS)
     * that most closely matches the geometry of the resource storage. It is often a
     * {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS projected CRS}, but other types like
     * {@linkplain org.apache.sis.referencing.crs.DefaultEngineeringCRS engineering CRS} are also allowed.
     * If this resource uses many different CRS with none of them covering all data, then the envelope should use a
     * global system (typically a {@linkplain org.apache.sis.referencing.crs.DefaultGeocentricCRS geographic CRS}).
     *
     * <p>The returned envelope is not necessarily the smallest bounding box encompassing all data.
     * If the smallest envelope is too costly to compute, this method may conservatively return a larger envelope.</p>
     *
     * @return the spatiotemporal resource extent. Should not be {@code null} (but may happen if too costly to compute).
     * @throws DataStoreException if an error occurred while reading or computing the envelope.
     */
    Envelope getEnvelope() throws DataStoreException;
}
