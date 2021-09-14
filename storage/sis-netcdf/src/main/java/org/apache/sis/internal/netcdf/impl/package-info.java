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
 * Implementation of the {@link org.apache.sis.internal.netcdf} API as a standalone library.
 * This is useful only for the netCDF binary format (no NcML, no GRIB, no BUFR).
 * This package works with channels instead of files, which is a little bit easier to use
 * in some environments.
 *
 * <h2>Reference</h2>
 * <ul>
 *   <li><a href="https://www.ogc.org/standards/netcdf">NetCDF standards on OGC web site</a></li>
 *   <li><a href="https://portal.ogc.org/files/?artifact_id=43734">NetCDF Classic and 64-bit Offset Format (1.0)</a></li>
 *   <li><a href="https://www.unidata.ucar.edu/software/netcdf/docs/file_format_specifications.html">NetCDF on UCAR web site.</a></li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
package org.apache.sis.internal.netcdf.impl;
