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
}
