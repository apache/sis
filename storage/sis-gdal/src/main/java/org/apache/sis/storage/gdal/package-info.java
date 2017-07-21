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
 * Referencing services as wrapper around the C/C++ <a href="http://proj.osgeo.org/">{@literal Proj.4}</a> library.
 * Unless otherwise specified, this optional module requires the native (C/C++) Proj.4 library to be installed
 * on the local machine. This package allows to:
 * <ul>
 *   <li>{@linkplain org.apache.sis.storage.gdal.Proj4#createCRS Create a Coordinate Reference System instance from a Proj.4 definition string}.</li>
 *   <li>Conversely, {@linkplain org.apache.sis.storage.gdal.Proj4#definition get a Proj.4 definition string from a Coordinate Reference System}.</li>
 *   <li>{@linkplain org.apache.sis.storage.gdal.Proj4#createOperation Create a coordinate operation backed by Proj.4 between two arbitrary CRS}.</li>
 * </ul>
 *
 * Note that Apache SIS provides its own map projection engine in pure Java, so this module is usually not needed.
 * This module is useful when a desired map projection is not yet available in Apache SIS, or when an application
 * needs to reproduce the exact same numerical results than Proj.4. But some Apache SIS features like
 * {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform#derivative transform derivatives}
 * are not available through the Proj.4 wrappers.
 *
 * <div class="section">Installation</div>
 * The Proj.4 library needs to be reachable on a platform-dependent library path.
 * For example the operating system may search in the {@code /opt/local/lib} and other directories.
 * One of those directories shall contain a {@code libproj.so}, {@code libproj.dylib} or {@code proj.dll} file,
 * depending on the platform. An easy way to install the library in appropriate directory is to use the package
 * manager provided by the platform.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
package org.apache.sis.storage.gdal;
