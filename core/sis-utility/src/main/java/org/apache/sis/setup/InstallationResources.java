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


/**
 * Resources needed for installation of third-party or optional components.
 * {@code InstallationResources} can be used for downloading large files that may not be of interest
 * to every users, or data that are subject to more restricting terms of use than the Apache license.
 *
 * <div class="note"><b>Examples:</b><ul>
 * <li>The NADCON grid files provide <cite>datum shifts</cite> data for North America.
 *     Since those files are in the public domain, they could be bundled in Apache SIS.
 *     But the weight of those files (about 2.4 Mb) is unnecessary for users who do not live in North America.</li>
 * <li>On the other hand, the <a href="http://www.epsg.org/">EPSG geodetic dataset</a> is important for most users.
 *     Codes like {@code "EPSG:4326"} became a <i>de-facto</i> standard in various places like <cite>Web Map Services</cite>,
 *     images encoded in GeoTIFF format, <i>etc</i>. But the <a href="http://www.epsg.org/TermsOfUse">EPSG terms of use</a>
 *     are more restrictive than the Apache license and require that we inform the users about those conditions.</li>
 * </ul></div>
 *
 * Some classes that depend on installation resources are:
 * {@link org.apache.sis.referencing.factory.sql.EPSGFactory}.
 * In order to allow those classes to discover which resources are available,
 * {@code InstallationResources} implementations shall be declared in the following file:
 *
 * {@preformat text
 *     META-INF/services/org.apache.sis.setup.InstallationResources
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
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
     * <ul>
     *   <li>{@code "EPSG"} for the EPSG geodetic dataset.</li>
     * </ul>
     *
     * This method may return an empty set if this {@code InstallationResources} instance did not find the
     * resources (for example because of files not found) or does not have the permission to distribute them.
     *
     * @return Identifiers of resources that this instance can distribute.
     */
    public abstract Set<String> getAuthorities();

    /**
     * Returns the terms of use of the resources distributed by the specified authority, or {@code null} if none.
     * The terms of use can be returned in either plain text or HTML.
     *
     * <div class="note"><b>Example:</b>
     * For the {@code "EPSG"} authority, this method may return a copy of the
     * <a href="http://www.epsg.org/TermsOfUse">http://www.epsg.org/TermsOfUse</a> page.
     * </div>
     *
     * @param  authority One of the values returned by {@link #getAuthorities()}.
     * @param  locale    The preferred locale for the terms of use.
     * @param  mimeType  Either {@code "text/plain"} or {@code "text/html"}.
     * @return The terms of use in plain text or HTML, or {@code null} if none.
     * @throws IllegalArgumentException if the given {@code authority} argument is not one of the expected values.
     * @throws IOException if an error occurred while reading the license file.
     */
    public abstract String getLicense(String authority, Locale locale, String mimeType) throws IOException;

    /**
     * Returns the names of all resources of the specified authority that are distributed by this instance.
     * The resources will be used in the order they appear in the array.
     *
     * <div class="note"><b>Example:</b>
     * for the {@code "EPSG"} authority, this method may return the filenames of all SQL scripts to execute.
     * One of the first script creates tables, followed by a script that populates tables with data,
     * followed by a script that creates foreigner keys.
     * </div>
     *
     * @param  authority One of the values returned by {@link #getAuthorities()}.
     * @return The names of all resources of the given authority that are distributed by this instance.
     * @throws IllegalArgumentException if the given {@code authority} argument is not one of the expected values.
     * @throws IOException if fetching the resource names required an I/O operation and that operation failed.
     */
    public abstract String[] getResourceNames(String authority) throws IOException;

    /**
     * Returns a reader for the resources at the given index.
     * The resource may be a SQL script or any other resources readable as a text.
     * The returned {@link BufferedReader} instance shall be closed by the caller.
     *
     * @param  authority One of the values returned by {@link #getAuthorities()}.
     * @param  resource Index of the script to open, from 0 inclusive to
     *         <code>{@linkplain #getResourceNames(String) getResourceNames}(authority).length</code> exclusive.
     * @return A reader for the installation script content.
     * @throws IllegalArgumentException if the given {@code authority} argument is not one of the expected values.
     * @throws IndexOutOfBoundsException if the given {@code resource} argument is out of bounds.
     * @throws IOException if an error occurred while creating the reader.
     */
    public abstract BufferedReader openScript(String authority, int resource) throws IOException;
}
