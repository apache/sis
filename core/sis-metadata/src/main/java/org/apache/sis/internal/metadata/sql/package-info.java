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
 * A set of helper classes for SQL handling in the Apache SIS implementation.
 *
 * <strong>Do not use!</strong>
 *
 * This package is for internal use by SIS only. Classes in this package
 * may change in incompatible ways in any future version without notice.
 *
 * <div class="section">Declaring the data source in a web container</div>
 * The {@link org.apache.sis.internal.metadata.sql.Initializer#getDataSource()} method gets the unique,
 * SIS-wide, data source for JDBC connection to the {@code $SIS_DATA/Databases/SpatialMetadata}.
 * The method Javadoc describes the steps for fetching that data source.
 * When used in a JavaEE container, the data source can be configured as below:
 *
 * <ol class="verbose">
 *   <li><p>Make the JDBC driver available to the web container and its applications. On Tomcat, this
 *     is accomplished by installing the driver's JAR files into the {@code $CATALINA_HOME/lib} directory
 *     (<a href="https://tomcat.apache.org/tomcat-9.0-doc/jndi-resources-howto.html">source</a>).</p></li>
 *
 *   <li><p>If using Derby, copy {@code derby.war} into the {@code $CATALINA_HOME/webapps} directory
 *     and specify the directory where the Derby databases are located:</p>
 *     {@preformat text
 *       export JAVA_OPTS=-Dderby.system.home=$SIS_DATA/Databases
 *     }
 *   </li>
 *
 *   <li><p>Declare the JNDI name in application {@code WEB-INF/web.xml} file:</p>
 *     {@preformat xml
 *       <resource-ref>
 *         <description>EPSG dataset and other metadata used by Apache SIS.</description>
 *         <res-ref-name>jdbc/SpatialMetadata</res-ref-name>
 *         <res-type>javax.sql.DataSource</res-type>
 *         <res-auth>Container</res-auth>
 *       </resource-ref>
 *     }
 *   </li>
 *
 *   <li><p>Configure the data source in {@code $CATALINA_HOME/conf/context.xml} or in application
 *     {@code META-INF/context.xml} file (change attribute values as needed for the chosen JDBC driver):</p>
 *     {@preformat xml
 *       <Context crossContext="true">
 *         <WatchedResource>WEB-INF/web.xml</WatchedResource>
 *         <Resource name            = "jdbc/SpatialMetadata"
 *                   auth            = "Container"
 *                   type            = "javax.sql.DataSource"
 *                   username        = "sa"
 *                   password        = "sa"
 *                   driverClassName = "org.apache.derby.jdbc.EmbeddedDriver"
 *                   url             = "jdbc:derby:SpatialMetadata"/>
 *       </Context>
 *     }
 *   </li>
 *
 *   <li><p>If using Derby, go to {@code localhost:8080/derby/derbynet}.</p></li>
 * </ol>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
package org.apache.sis.internal.metadata.sql;
