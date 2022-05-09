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
package org.apache.sis.setup;

import java.util.Set;
import java.util.Locale;
import java.io.IOException;
import java.io.BufferedReader;
import org.apache.sis.internal.util.URLs;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.MetadataServices;


/**
 * Resources needed for installation of third-party or optional components.
 * {@code InstallationResources} can be used for downloading large files that may not be of interest
 * to every users, or data that are subject to more restricting terms of use than the Apache license.
 *
 * <div class="note"><b>Examples:</b><ul class="verbose">
 * <li>The NADCON grid files provide <cite>datum shifts</cite> data for North America.
 *     Since those files are in the public domain, they could be bundled in Apache SIS.
 *     But the weight of those files (about 2.4 Mb) is unnecessary for users who do not live in North America.</li>
 * <li>On the other hand, the <a href="https://epsg.org/">EPSG geodetic dataset</a> is important for most users.
 *     Codes like {@code "EPSG:4326"} became a <i>de-facto</i> standard in various places like <cite>Web Map Services</cite>,
 *     images encoded in GeoTIFF format, <i>etc</i>. But the <a href="https://epsg.org/terms-of-use.html">EPSG terms of use</a>
 *     are more restrictive than the Apache license and require that we inform the users about those conditions.</li>
 * </ul></div>
 *
 * Some authorities implemented in Apache SIS modules are listed below.
 * In this list, {@code "Embedded"} is a pseudo-authority for an embedded database containing EPSG and other data.
 * The embedded database is provided as a convenience for avoiding the need to define a {@code SIS_DATA} directory
 * on the local machine.
 *
 * <table class="sis">
 *   <caption>Authorities supported by Apache SIS</caption>
 *   <tr><th>Authority</th>          <th>Provided by Maven module</th>                          <th>Used by class</th></tr>
 *   <tr><td>{@code "EPSG"}</td>     <td>{@code org.apache.sis.non-free:sis-epsg}</td>          <td>{@link org.apache.sis.referencing.factory.sql.EPSGFactory}</td></tr>
 *   <tr><td>{@code "Embedded"}</td> <td>{@code org.apache.sis.non-free:sis-embedded-data}</td> <td>All the above</td></tr>
 * </table>
 *
 * In order to allow those classes to discover which resources are available,
 * {@code InstallationResources} implementations shall be declared in the following file:
 *
 * {@preformat text
 *     META-INF/services/org.apache.sis.setup.InstallationResources
 * }
 *
 * Above registration is usually done automatically when extension modules are added on the classpath.
 * For example adding the {@code org.apache.sis.non-free:sis-epsg} Maven dependency as documented on
 * the <a href="https://sis.apache.org/epsg.html">Apache SIS web site</a> is the only step needed for
 * allowing Apache SIS to read the EPSG scripts (however SIS still needs an installation directory
 * for writing the database; see above-cited web page for more information).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   0.7
 * @module
 */
public abstract class InstallationResources {
    /**
     * For subclass constructors.
     */
    protected InstallationResources() {
    }

    /**
     * Returns identifiers of the resources provided by this instance.
     * The values recognized by SIS are listed below
     * (note that this list may be expanded in any future SIS versions):
     *
     * <table class="sis">
     *   <caption>Authorities supported by Apache SIS</caption>
     *   <tr><th>Authority</th>          <th>Resources</th></tr>
     *   <tr><td>{@code "EPSG"}</td>     <td>SQL installation scripts for EPSG geodetic dataset.</td></tr>
     *   <tr><td>{@code "Embedded"}</td> <td>Data source of embedded database containing EPSG and other resources.</td></tr>
     * </table>
     *
     * <div class="note"><b>Note:</b>
     * {@code "Embedded"} is a pseudo-authority for an embedded database containing EPSG and other data.
     * This embedded database is provided by the {@code org.apache.sis.non-free:sis-embedded-data} module
     * as a convenience for avoiding the need to define a {@code SIS_DATA} directory on the local machine.
     * In this particular case, the resource is more for execution than for installation.
     * </div>
     *
     * This method may return an empty set if this {@code InstallationResources} instance did not find the
     * resources (for example because of files not found) or does not have the permission to distribute them.
     *
     * @return identifiers of resources that this instance can distribute.
     */
    public abstract Set<String> getAuthorities();

    /**
     * Returns a URL where users can get more information about the installation process.
     *
     * @return URL to installation instruction, or {@code null} if none.
     *
     * @since 1.2
     */
    public String getInstructionURL() {
        final Set<String> authorities = getAuthorities();
        if (authorities.contains(Constants.EPSG) || authorities.contains(MetadataServices.EMBEDDED)) {
            return URLs.EPSG_INSTALL;
        }
        return null;
    }

    /**
     * Returns the terms of use of the resources distributed by the specified authority, or {@code null} if none.
     * The terms of use can be returned in either plain text or HTML.
     *
     * <table class="sis">
     *   <caption>Licenses for some supported authorities</caption>
     *   <tr>
     *     <th>Authority</th>
     *     <th>License</th>
     *   </tr><tr>
     *     <td>{@code "EPSG"}</td>
     *     <td>A copy of the <a href="https://epsg.org/terms-of-use.html">https://epsg.org/terms-of-use.html</a> page.</td>
     *   </tr><tr>
     *     <td>{@code "Embedded"}</td>
     *     <td>Above EPSG license.</td>
     *   </tr>
     * </table>
     *
     * @param  authority  one of the values returned by {@link #getAuthorities()}.
     * @param  locale     the preferred locale for the terms of use.
     * @param  mimeType   either {@code "text/plain"} or {@code "text/html"}.
     * @return the terms of use in plain text or HTML, or {@code null} if none.
     * @throws IllegalArgumentException if the given {@code authority} argument is not one of the expected values.
     * @throws IOException if an error occurred while reading the license file.
     */
    public abstract String getLicense(String authority, Locale locale, String mimeType) throws IOException;

    /**
     * Returns the names of all resources of the specified authority that are distributed by this instance.
     * The resources will be used in the order they appear in the array.
     * Examples:
     *
     * <ul class="verbose">
     *   <li><b>{@code "EPSG"} authority:</b>
     *     the resource names are the filenames of all SQL scripts to execute. One of the first script creates tables,
     *     followed by a script that populates tables with data, followed by a script that creates foreigner keys.
     *   </li>
     *   <li><b>{@code "Embedded"} pseudo-authority:</b>
     *     the database name, which is {@code "SpatialMetadata"}.
     *     When embedded, this database is read-only.
     *   </li>
     * </ul>
     *
     * @param  authority  one of the values returned by {@link #getAuthorities()}.
     * @return the names of all resources of the given authority that are distributed by this instance.
     * @throws IllegalArgumentException if the given {@code authority} argument is not one of the expected values.
     * @throws IOException if fetching the resource names required an I/O operation and that operation failed.
     */
    public abstract String[] getResourceNames(String authority) throws IOException;

    /**
     * Returns an installation resource for the given authority, or {@code null} if not available.
     * The return value may be an instance of any type, at implementation choice.
     * This may be for example a {@link java.net.URL} referencing the actual resource,
     * or a {@link javax.sql.DataSource} for an embedded database.
     *
     * <p>The default implementation returns {@code null}. A null value means that the resource is fetched by
     * {@link #openScript(String, int)} instead of this method. We do not return {@link java.net.URL} to text
     * files in order to ensure that the file is opened with proper character encoding.</p>
     *
     * @param  authority  one of the values returned by {@link #getAuthorities()}.
     * @param  index      index of the resource to get, from 0 inclusive to
     *         <code>{@linkplain #getResourceNames(String) getResourceNames}(authority).length</code> exclusive.
     * @return the resource as an URL or any other type (at implementation choice), or {@code null} if not available.
     * @throws IllegalArgumentException if the given {@code authority} argument is not one of the expected values.
     * @throws IndexOutOfBoundsException if the given {@code resource} argument is out of bounds.
     * @throws IOException if an error occurred while fetching the resource.
     *
     * @see ClassLoader#getResource(String)
     *
     * @since 0.8
     */
    public Object getResource(String authority, int index) throws IOException {
        return null;
    }

    /**
     * Returns a reader for the resources at the given index.
     * The resource may be a SQL script or any other resources readable as a text.
     * The returned {@link BufferedReader} instance shall be closed by the caller.
     *
     * @param  authority  one of the values returned by {@link #getAuthorities()}.
     * @param  resource   index of the script to open, from 0 inclusive to
     *         <code>{@linkplain #getResourceNames(String) getResourceNames}(authority).length</code> exclusive.
     * @return a reader for the installation script content.
     * @throws IllegalArgumentException if the given {@code authority} argument is not one of the expected values.
     * @throws IndexOutOfBoundsException if the given {@code resource} argument is out of bounds.
     * @throws IOException if an error occurred while creating the reader.
     */
    public abstract BufferedReader openScript(String authority, int resource) throws IOException;
}
