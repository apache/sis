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
 * The API used internally by Apache SIS for fetching all data from netCDF files.
 * We use this API for isolating Apache SIS from the library used for reading the
 * netCDF file: it can be either the UCAR library, or our own internal library.
 *
 * <p>We do not use systematically the UCAR library because it is quite large (especially when including
 * all dependencies) while SIS uses only a fraction of it. This is because the UCAR library provides some
 * features like referencing services which overlap with SIS services. In addition, SIS often needs "raw"
 * data instead of "high level" data. For example, we need the minimal and maximal values of a variable in
 * its raw format, while the UCAR high level API provides the values converted by the offset and scale
 * factors.</p>
 *
 * <p>A side effect of this isolation layer is also to adapt netCDF vocabulary to Apache SIS one.
 * For example, what netCDF calls <q>coordinate system</q> is actually a mix of what OGC/ISO
 * specifications call <q>coordinate system</q>, <q>coordinate reference system</q>
 * and <q>grid geometry</q>. The netCDF coordinate system <q>range</q> is closer to
 * ISO 19123 <q>domain</q>, the netCDF coordinate system <q>domain</q> is closer to
 * ISO 19123 <q>grid envelope</q> and the ISO 19123 <q>range</q> is rather related
 * to the netCDF variable minimum and maximum values. Trying to use OGC/ISO and netCDF objects in
 * the same code appears to be very confusing. This isolation layer allows our code to use a more
 * consistent vocabulary (more similar to the rest of Apache SIS).</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
package org.apache.sis.storage.netcdf.base;
