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
 * <h2>Implementation notes</h2>
 * Feature type analysis is done through {@link org.apache.sis.internal.sql.feature.Analyzer} class.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.2
 * @since   1.0
 * @module
 */
package org.apache.sis.internal.sql.feature;
