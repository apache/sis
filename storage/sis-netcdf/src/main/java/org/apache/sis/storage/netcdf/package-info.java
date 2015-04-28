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
 * Maps ISO metadata elements from/to the <a href="http://www.cfconventions.org">Climate and Forecast (CF)</a>
 * attributes in a NetCDF file. The mapping is defined in the following web pages:
 *
 * <ul>
 *   <li><a href="http://wiki.esipfed.org/index.php/Category:Attribute_Conventions_Dataset_Discovery">NetCDF
 *       Attribute Convention for Dataset Discovery</a> version 1.0.</li>
 *   <li><a href="http://ngdc.noaa.gov/metadata/published/xsl/nciso2.0/UnidataDD2MI.xsl">UnidataDD2MI.xsl</a> file.</li>
 * </ul>
 *
 * The NetCDF attributes recognized by this package are listed in the
 * {@link org.apache.sis.storage.netcdf.AttributeNames} class.
 *
 * <div class="section">Note on the definition of terms</div>
 * The UCAR library sometime uses the same words than the ISO/OGC standards for different things.
 * In particular the words <cite>"domain"</cite> and <cite>"range"</cite> can be applied to arbitrary functions,
 * and the UCAR library chooses to apply it to the function that converts grid indices to geodetic coordinates.
 * The ISO 19123 standard on the other hand considers coverage as a function, and applies those <cite>domain</cite>
 * and <cite>range</cite> words to that function. More specifically:
 *
 * <ul>
 *   <li>UCAR <cite>"coordinate system"</cite> is actually a mix of <cite>coordinate system</cite>,
 *       <cite>coordinate reference system</cite> and <cite>grid geometry</cite> in OGC sense.</li>
 *   <li>UCAR coordinate system <cite>"domain"</cite> is not equivalent to ISO 19123 coverage domain,
 *       but is rather related to <cite>grid envelope</cite>.</li>
 *   <li>ISO 19123 coverage <cite>domain</cite> is related to UCAR coordinate system <cite>"range"</cite>.</li>
 *   <li>ISO 19123 coverage <cite>range</cite> is not equivalent to UCAR <cite>"range"</cite>,
 *       but is rather related to the NetCDF variable's minimum and maximum values.</li>
 * </ul>
 *
 * Care must be taken for avoiding confusion when using SIS and UCAR libraries together.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
package org.apache.sis.storage.netcdf;
