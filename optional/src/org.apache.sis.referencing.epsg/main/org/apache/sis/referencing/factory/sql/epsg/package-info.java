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
 * SQL scripts for EPSG geodetic data set installation.
 * The <a href="https://epsg.org/">EPSG</a> geodetic dataset provides definitions for thousands of
 * {@linkplain org.opengis.referencing.crs.CoordinateReferenceSystem Coordinate Reference Systems} (<abbr>CRS</abbr>),
 * together with parameter values for thousands of {@linkplain org.opengis.referencing.operation.ConcatenatedOperation
 * Coordinate Operations} between various <abbr>CRS</abbr> pairs.
 * This module contains the SQL scripts for creating a local copy of EPSG geodetic dataset.
 *
 * <h2>Licensing</h2>
 * EPSG is maintained by the <a href="https://www.iogp.org/">International Association of Oil and Gas Producers</a>
 * (IOGP) Surveying &amp; Positioning Committee and is subject to <a href="https://epsg.org/terms-of-use.html">EPSG
 * terms of use</a>. This module is not included in the Apache <abbr>SIS</abbr> distribution of convenience binaries,
 * and the source code contains only the Java classes without the <abbr>EPSG</abbr> data. For use in an application,
 * see <a href="https://sis.apache.org/epsg.html">How to use EPSG geodetic dataset</a> on the <abbr>SIS</abbr> web site.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.7
 */
package org.apache.sis.referencing.factory.sql.epsg;
