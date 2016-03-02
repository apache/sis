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
 * Factories for geodetic objects defined in a SQL database, typically the EPSG dataset.
 * This package provides support for all codes prefixed by {@code "EPSG:"} in the Apache SIS's
 * <a href="http://sis.apache.org/book/tables/CoordinateReferenceSystems.html">list of authority codes</a>.
 * The main class in this package is {@link org.apache.sis.referencing.factory.sql.EPSGFactory},
 * which requires a {@link javax.sql.DataSource} providing connections to an EPSG database.
 *
 *
 * <div class="section">Connection to the database</div>
 * By default Apache SIS uses Apache Derby (a.k.a. JavaDB in Oracle JDK),
 * but the database can also be PostgreSQL or MS-Access.
 * The database connection is obtained by the first of the following data sources which is found:
 *
 * <ol>
 *   <li>If a {@linkplain javax.naming.InitialContext JNDI context} exists,
 *       the {@link javax.sql.DataSource} registered under the {@code "java:comp/env/jdbc/SpatialMetadata"} name.</li>
 *   <li>If the {@code SIS_DATA} {@linkplain java.lang.System#getenv(String) environment variable} is defined,
 *       a JDBC connection for the {@code "jdbc:derby:$SIS_DATA/Databases/SpatialMetadata"} URL.</li>
 *   <li>If the {@code "derby.system.home"} {@linkplain java.lang.System#getProperty(String) property} is defined,
 *       a JDBC connection for the {@code "jdbc:derby:SpatialMetadata"} URL.</li>
 * </ol>
 *
 * In choice 1, the JDBC driver must be provided by the application container (e.g. Apache Tomcat).
 * In choice 2 and 3, Apache SIS tries to use the JavaDB driver in the JDK installation directory
 * (included in Oracle's distribution of Java) if no Apache Derby driver is found on the classpath.
 *
 *
 * <div class="section">The EPSG dataset</div>
 * A widely-used factory is the <a href="http://www.epsg.org">EPSG geodetic dataset</a>.
 * EPSG codes are numerical identifiers.
 * For example {@code "EPSG:4326"} is the EPSG identifier for the <cite>"WGS 84"</cite> geographic CRS.
 * As an extension, the Apache SIS implementation accepts names as well as numeric identifiers.
 * For example the two following method calls fetch the same object:
 *
 * <ul>
 *   <li>{@code createProjectedCRS("27572")}</li>
 *   <li>{@code createProjectedCRS("NTF (Paris) / Lambert zone II")}</li>
 * </ul>
 *
 * <div class="note"><b>Note:</b> names may be ambiguous since the same name may be used for more than one object.
 * This is the case of <cite>"WGS 84"</cite> for example. If such an ambiguity is found, an exception will be thrown.
 * For more determinism, the numerical codes are preferred.</div>
 *
 *
 * <div class="section">How deprecated entries are handled</div>
 * When an error is discovered in a Coordinate Reference System (CRS) definition, the EPSG group does not apply the
 * correction directly on the erroneous object (unless the correction is very minor).
 * Instead, the erroneous object is deprecated and a new one is created.
 * Apache SIS handles deprecated objects as below:
 *
 * <ul>
 *   <li>Deprecated objects are not listed in the collection returned by the
 *       {@link org.apache.sis.referencing.factory.sql.EPSGDataAccess#getAuthorityCodes EPSGDataAccess.getAuthorityCodes(…)} method.</li>
 *   <li>All method expecting an EPSG code in argument accept also the codes of deprecated objects.</li>
 *   <li>If a deprecated object is created by a call to {@code EPSGDataAccess.createFoo(…)},
 *       a warning will be logged with a message proposing a replacement.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Yann Cézard (IRD)
 * @author  Rueben Schulz (UBC)
 * @author  Matthias Basler
 * @author  Andrea Aime (TOPP)
 * @author  Jody Garnett (Refractions)
 * @author  Didier Richard (IGN)
 * @author  John Grange
 * @version 0.7
 * @since   0.7
 * @module
 */
package org.apache.sis.referencing.factory.sql;
