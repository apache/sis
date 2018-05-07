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

import org.opengis.feature.Feature;


/**
 * Definition of filtering to apply for fetching a resource subset.
 * Filtering can happen in two domains:
 *
 * <ol>
 *   <li>By filtering the {@link Feature} instances.</li>
 *   <li>By filtering the {@linkplain org.apache.sis.feature.DefaultFeatureType#getProperty properties}
 *       in each feature instance.</li>
 * </ol>
 *
 * Compared to Java functional interfaces, the first domain is equivalent to using
 * <code>{@linkplain java.util.function.Predicate}&lt;{@linkplain Feature}&gt;</code>
 * while the second domain is equivalent to using
 * <code>{@linkplain java.util.function.UnaryOperator}&lt;{@linkplain Feature}&gt;</code>.
 *
 * <div class="note"><b>Note:</b>
 * it is technically possible to use {@code Query} for performing more generic feature transformations,
 * for example inserting new properties computed from other properties, but such {@code Query} usages
 * should be rare since transformations (or more generic processing) are the topic of another package.
 * Queries are rather descriptive objects used by {@link FeatureSet} to optimize search operations
 * as much as possible on the resource, using for example caches and indexes.</div>
 *
 * Compared to the SQL language, {@code Query} contains the information in the {@code SELECT} and
 * {@code WHERE} clauses of a SQL statement. A {@code Query} typically contains filtering capabilities
 * and (sometime) simple attribute transformations. Well known query languages include SQL and CQL.
 *
 * @author Johann Sorel (Geomatys)
 * @version 0.8
 *
 * @see FeatureSet#subset(Query)
 *
 * @since 0.8
 * @module
 */
public interface Query {

}
