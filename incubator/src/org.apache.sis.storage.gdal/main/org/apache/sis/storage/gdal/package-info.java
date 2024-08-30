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
 * This package assumes that <abbr>GDAL</abbr> is preinstalled.
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
 * <p>The current implementation can read rasters.
 * Write operations and support of vector data will be provided in a future version.</p>
 *
 * @author  Quentin Bialota (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
package org.apache.sis.storage.gdal;
