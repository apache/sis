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

import java.util.Locale;
import java.io.IOException;


/**
 * Resources needed for installation of third-party or optional components.
 * {@code InstallationResources} can be used for downloading large files that may not be of interest
 * to every users, or data that are subject to more restricting terms of use than the Apache license.
 *
 * <div class="note"><b>Examples:</b>
 * the NADCON grid files provide <cite>datum shifts</cite> data for North America.
 * Since those files are in the public domain, they could be bundled in Apache SIS.
 * But the weight of those files (about 2.4 Mb) is unnecessary for users who do not live in North America.
 *
 * <p>On the other hand, the <a href="http://www.epsg.org/">EPSG geodetic dataset</a> is important for most users.
 * Codes like {@code "EPSG:4326"} became a <i>de-facto</i> standard in various places like <cite>Web Map Services</cite>,
 * images encoded in GeoTIFF format, <i>etc</i>. But the <a href="http://www.epsg.org/TermsOfUse">EPSG terms of use</a>
 * are more restrictive than the Apache license and require that we inform the users about those conditions.</p>
 * </div>
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
     * Returns the terms of use of the resources, or {@code null} if none.
     * The terms of use can be returned in either plain text or HTML.
     *
     * @param  locale The preferred locale for the terms of use.
     * @param  mimeType Either {@code "text/plain"} or {@code "text/html"}.
     * @return The terms of use in plain text or HTML, or {@code null} if none.
     * @throws IOException if an error occurred while reading the license file.
     */
    public abstract String getLicense(Locale locale, String mimeType) throws IOException;
}
