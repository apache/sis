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
 * Reads netCDF files conforming to the <a href="http://www.cfconventions.org">Climate and Forecast (CF)</a>.
 * The netCDF attributes recognized by this package are listed in the
 * {@link org.apache.sis.storage.netcdf.AttributeNames} class.
 *
 * <h2>Note on the definition of terms</h2>
 * The UCAR library sometimes uses the same words as the ISO/OGC standards for different things.
 * In particular the words <q>domain</q> and <q>range</q> can be applied to arbitrary functions,
 * and the UCAR library chooses to apply it to the function that converts grid indices to geodetic coordinates.
 * This is a different usage than ISO 19123 which uses the <i>domain</i> and <i>range</i> words
 * for the coverage function. More specifically:
 *
 * <ul>
 *   <li>UCAR <q>coordinate system</q> is actually a mix of <i>coordinate system</i>,
 *       <i>coordinate reference system</i> and <i>grid geometry</i> in OGC sense.</li>
 *   <li>UCAR coordinate system <q>domain</q> is not equivalent to ISO 19123 coverage domain,
 *       but is rather related to <i>grid envelope</i>.</li>
 *   <li>ISO 19123 coverage <i>domain</i> is related to UCAR coordinate system <q>range</q>.</li>
 *   <li>ISO 19123 coverage <i>range</i> is not equivalent to UCAR <q>range</q>,
 *       but is rather related to the netCDF variable's minimum and maximum values.</li>
 * </ul>
 *
 * Care must be taken for avoiding confusion when using SIS and UCAR libraries together.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.6
 * @since   0.3
 */
package org.apache.sis.storage.netcdf;
