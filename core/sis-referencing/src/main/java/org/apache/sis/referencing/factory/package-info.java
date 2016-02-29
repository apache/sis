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
 * Creates Coordinate Reference System (CRS) objects from given properties or authority codes.
 * This package provides two kinds of factories:
 *
 * <ul class="verbose">
 *   <li>{@link org.apache.sis.referencing.factory.GeodeticAuthorityFactory}
 *     creates geodetic objects from codes defined by an authority.
 *     An <cite>authority</cite> is an organization that maintains definitions of authority codes.
 *     An <cite>authority code</cite> is a compact string defined by an authority to reference a particular spatial reference object.
 *     A frequently used set of authority codes is the <a href="http://www.epsg.org">EPSG geodetic dataset</a>,
 *     a database of coordinate systems and other spatial referencing objects where each object has a code number ID.</li>
 *
 *   <li>{@link org.apache.sis.referencing.factory.GeodeticObjectFactory}
 *     creates complex objects that can not be created by the authority factories.
 *     Allows also <cite>inversion of control</cite> when used with the
 *     {@linkplain org.apache.sis.referencing.factory.sql.EPSGFactory EPSG authority factory}
 *     or with the {@linkplain org.apache.sis.io.wkt.WKTFormat WKT parser}.</li>
 * </ul>
 *
 * Authority factories available in Apache SIS are listed below.
 * Factories defined in this package do not require any configuration or external resources.
 * Factories defined in the {@link org.apache.sis.referencing.factory.sql sql} sub-package require a connection to a database.
 *
 * <table class="sis">
 *   <caption>Authority factory implementations</caption>
 *   <tr>
 *     <th>Authorities</th>
 *     <th>Implementation class</th>
 *     <th>Conditions</th>
 *   </tr><tr>
 *     <td>{@code CRS}, {@code AUTO} and {@code AUTO2}</td>
 *     <td>{@link org.apache.sis.referencing.factory.CommonAuthorityFactory}</td>
 *     <td>None.</td>
 *   </tr><tr>
 *     <td>{@code EPSG}</td>
 *     <td>{@link org.apache.sis.referencing.factory.sql.EPSGFactory}</td>
 *     <td>Requires installation of EPSG dataset.</td>
 *   </tr>
 * </table>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.7
 * @since   0.6
 * @module
 */
package org.apache.sis.referencing.factory;
