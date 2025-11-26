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
 * A data store that can read and write GeoTIFF files as grid coverages.
 * This module also maps GeoTIFF tags to <abbr>ISO</abbr> 19115 metadata.
 *
 * <p>References:</p>
 * <ul>
 *   <li><a href="https://www.adobe.io/content/dam/udp/en/open/standards/tiff/TIFF6.pdf">TIFF specification 6.0</a>: baseline.</li>
 *   <li><a href="https://www.adobe.io/content/dam/udp/en/open/standards/tiff/TIFFPM6.pdf">TIFF Tecnical Notes</a>: add new tags.</li>
 *   <li><a href="https://www.awaresystems.be/imaging/tiff/tifftags.html">TIFF Tag Reference</a></li>
 *   <li><a href="https://www.iso.org/standard/34342.html">ISO 12639:2004 — Tag image file format for image technology (TIFF/IT)</a></li>
 *   <li><a href="http://docs.opengeospatial.org/is/19-008r4/19-008r4.html">OGC GeoTIFF standard</a></li>
 *   <li><a href="https://gdal.org/proj_list/">GeoTIFF projections list</a></li>
 * </ul>
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @author  Minh Chinh Vu (VNSC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 * @since   0.8
 */
package org.apache.sis.storage.geotiff;
