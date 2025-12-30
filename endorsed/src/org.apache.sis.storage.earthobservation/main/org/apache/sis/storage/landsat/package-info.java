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
 * Reader of Landsat 8 level 1-2 data.
 * Those data are produced by the U.S. Geological Survey (USGS).
 * This reader takes in input the directory containing the files for a Landsat scene.
 * Such directory is made of metadata files in text or XML formal and GeoTIFF files.
 * The text file is parsed and <a href="doc-files/MetadataMapping.html">mapped to ISO 19115 metadata</a>.
 * The GeoTIFF files are read using {@link org.apache.sis.storage.geotiff.GeoTiffStore}
 * with each band offered as a Landsat resource.
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @author  Minh Chinh Vu (VNSC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 * @since   1.1
 */
package org.apache.sis.storage.landsat;
