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


/**
 * Build {@link org.opengis.feature.FeatureType}s by inspection of database schemas.
 * The work done here is similar to reverse engineering.
 *
 * <STRONG>Do not use!</STRONG>
 *
 * This package is for internal use by SIS only. Classes in this package
 * may change in incompatible ways in any future version without notice.
 *
 * @implNote Feature type analysis is done through {@link org.apache.sis.internal.sql.feature.Analyzer} class.
 * It relies on internal {@link org.apache.sis.internal.sql.feature.SQLTypeSpecification} API to fetch SQL schema
 * information, and build {@link org.apache.sis.internal.sql.feature.FeatureAdapter an adapter to feature model from it}.
 *
 * This package provides two main {@link org.apache.sis.storage.FeatureSet feature set} implementations:
 * <ul>
 *     <li>{@link org.apache.sis.internal.sql.feature.QueryFeatureSet}: execute a prepared SQL query, then interpret its result as Simple Feature collection.</li>
 *     <li>{@link org.apache.sis.internal.sql.feature.Table}: Analysis of SQL Table to provide a complex feature type modeling associations.</li>
 * </ul>
 *
 * TODO: a lot of code could be factorized to reduce splitting of code base for both use cases above. Notably, all
 * association management is done specifically in table implementation, but should be deported in {@link org.apache.sis.internal.sql.feature.FeatureAdapter}.
 * With that, we could reduce feature set implementations to only QueryFeatureSet, and delegating model analysis upstream.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
package org.apache.sis.internal.sql.feature;
