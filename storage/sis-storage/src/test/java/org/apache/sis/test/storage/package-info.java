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
 * Helper classes for testing {@link org.apache.sis.storage.DataStore} implementations.
 * The {@link org.apache.sis.test.storage.CoverageReadConsistency} class reads a coverage fully,
 * then requests various sub-regions. Sub-regions are than compared to the corresponding regions
 * in the full image. It is not a proof that data values are correct, but it shows at least that
 * read operations are consistent for the test data set.
 *
 * <p>The classes in this package can be used by other modules (netCDF, GeoTIFF, <i>etc.</i>).
 * Each module will need to provide a test data file in the format to be tested.
 * That test data shall be small enough for fitting in memory.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
package org.apache.sis.test.storage;
