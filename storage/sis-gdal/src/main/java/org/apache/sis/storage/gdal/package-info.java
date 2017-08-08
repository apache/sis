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
 * Extensions to referencing services as wrapper around the C/C++ {@literal Proj.4} library.
 * Current version wraps only referencing services, but future versions are expected to wrap more GDAL functionalities.
 * Unless otherwise specified, this optional module requires the native (C/C++) <a href="http://proj.osgeo.org/">Proj.4</a>
 * library to be installed on the local machine. This package allows to:
 * <ul>
 *   <li>{@linkplain org.apache.sis.storage.gdal.Proj4#createCRS Create a Coordinate Reference System instance from a Proj.4 definition string}.</li>
 *   <li>Conversely, {@linkplain org.apache.sis.storage.gdal.Proj4#definition get a Proj.4 definition string from a Coordinate Reference System}.</li>
 *   <li>{@linkplain org.apache.sis.storage.gdal.Proj4#createOperation Create a coordinate operation backed by Proj.4 between two arbitrary CRS}.</li>
 * </ul>
 *
 * Note that Apache SIS provides its own map projection engine in pure Java, so this module is usually not needed.
 * This module is useful when a map projection is not yet available in Apache SIS, or when an application needs to
 * reproduce the exact same numerical results than Proj.4. But some Apache SIS features like
 * {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform#derivative transform derivatives}
 * are not available through the Proj.4 wrappers.
 *
 * <p>When this optional module is available, the {@link org.apache.sis.referencing.CRS#forCode CRS.forCode(String)}
 * method accepts Proj.4 definition strings prefixed by {@code "Proj4::"}. Example:</p>
 *
 * {@preformat java
 *     CoordinateReferenceSystem crs = CRS.forCode("Proj4::+init=epsg:3395");
 * }
 *
 * Calls to {@link org.apache.sis.referencing.CRS#findOperation CRS.findOperation(…)} will delegate the coordinate
 * transformation to Proj.4 if {@code sourceCRS} and {@code targetCRS} were both obtained from a code in {@code "Proj4"}
 * namespace or by a method in this package. Otherwise, Apache SIS will use its own referencing engine.
 * The backing referencing engine can be seen by printing the {@code MathTransform}:
 * a transform backed by Proj.4 have a <cite>Well Known Text 1</cite> representation like below:
 *
 * {@preformat wkt
 *   PARAM_MT["pj_transform",
 *     PARAMETER["srcdefn", "+proj=…"],
 *     PARAMETER["dstdefn", "+proj=…"]]
 * }
 *
 * <div class="section">Note on Proj.4 definition strings</div>
 * Proj.4 unconditionally requires 3 letters for the {@code "+axis="} parameter — for example {@code "neu"} for
 * <cite>North</cite>, <cite>East</cite> and <cite>Up</cite> respectively — regardless the number of dimensions
 * in the CRS to create. Apache SIS makes the vertical direction optional in order to specify whether the CRS to
 * create shall contain a vertical axis or not:
 *
 * <ul>
 *   <li>If the vertical direction is present (as in {@code "neu"}), a three-dimensional CRS is created.</li>
 *   <li>If the vertical direction is absent (as in {@code "ne"}), a two-dimensional CRS is created.</li>
 * </ul>
 *
 * <div class="note"><b>Examples:</b>
 * <ul>
 *   <li>{@code "+init=epsg:4326"} (<strong>not</strong> equivalent to the standard EPSG::4326 definition)</li>
 *   <li>{@code "+proj=latlong +datum=WGS84 +ellps=WGS84 +towgs84=0,0,0"} (default to two-dimensional CRS)</li>
 *   <li>{@code "+proj=latlon +a=6378137.0 +b=6356752.314245179 +pm=0.0 +axis=ne"} (explicitely two-dimensional)</li>
 *   <li>{@code "+proj=latlon +a=6378137.0 +b=6356752.314245179 +pm=0.0 +axis=neu"} (three-dimensional)</li>
 * </ul>
 * </div>
 *
 * <b>Warning:</b> despite the {@code "epsg"} word, coordinate reference systems created by {@code "+init=epsg:"}
 * syntax are not necessarily compliant with EPSG definitions. In particular, the axis order is often different.
 * Units of measurement may also differ.
 *
 * <div class="section">Installation</div>
 * The Proj.4 library needs to be reachable on a platform-dependent library path.
 * For example the operating system may search in {@code /usr/lib}, {@code /opt/local/lib} and other directories.
 * One of those directories shall contain the {@code proj} or {@code libproj} file with platform-specific suffix
 * (e.g. {@code .so}, {@code .dylib} or {@code .dll}).
 * An easy way to install the library in appropriate directory is to use the package manager provided by the platform.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
package org.apache.sis.storage.gdal;
