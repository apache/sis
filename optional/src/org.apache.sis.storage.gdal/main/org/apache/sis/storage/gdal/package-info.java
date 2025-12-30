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
 * Bridge to the <abbr>GDAL</abbr> library for reading rasters.
 * This package assumes that <abbr>GDAL</abbr> 3.0 or later is preinstalled.
 * The <abbr>GDAL</abbr> C/C++ functions are invoked by using the {@link java.lang.foreign} package.
 * Running this package requires user's authorization to perform native accesses.
 * See the module Javadoc for more information.
 *
 * <p>Having this module on the module-path is sufficient for allowing Apache <abbr>SIS</abbr> to
 * try <abbr>GDAL</abbr> when {@link org.apache.sis.storage.DataStores#open(Object)} is invoked.
 * Pure Java implementations are tried first, and <abbr>GDAL</abbr> is tried as a fallback when
 * no Java implementation can decode a file. When first needed, this module searches on the
 * library path for a {@code gdal.dll} file on Windows or a {@code libgdal.so} file on Unix.
 * If a different <abbr>GDAL</abbr> library is desired, it can be specified explicitly
 * to the {@link org.apache.sis.storage.gdal.GDALStoreProvider} constructor.</p>
 *
 * <h2>Limitations</h2>
 * The current implementation can only read vector data and two-dimensional rasters.
 * It cannot yet write any data,
 * and does not yet use the multi-dimensional raster <abbr>API</abbr> of <abbr>GDAL</abbr>.
 * Those operations will be added progressively in future versions of this module.
 *
 * <p>For any <abbr>GDAL</abbr> layer, only one {@link java.util.stream.Stream} returned by
 * {@link org.apache.sis.storage.FeatureSet#features(boolean)} can be executed at a given time.
 * This is because the <abbr>GDAL</abbr> {@code OGRLayerH} C/C++ <abbr>API</abbr> provides only one cursor.
 * If two iterations are executed in same time on the feature instances of the same {@code FeatureSet},
 * then a {@link org.apache.sis.storage.ConcurrentReadException} is thrown on the second iteration.</p>
 *
 * @author  Quentin Bialota (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.6
 * @since   1.5
 */
package org.apache.sis.storage.gdal;
