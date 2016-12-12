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
package org.apache.sis.internal.gpx;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.citation.OnlineResource;


/**
 * Link to a resource in a GPX file.
 * This element provides 3 properties:
 *
 * <ul>
 *   <li>The {@linkplain #uri}, which is the only mandatory property.</li>
 * </ul>
 *
 * Those properties can be read or modified directly. All methods defined in this class are bridges to
 * the ISO 19115 metadata model and can be ignored if the user only wants to manipulate the GPX model.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final class Link implements OnlineResource {
    /**
     * The link value.
     */
    public URI uri;

    /**
     * Creates an initially empty instance.
     * Callers should set at least the {@link #uri} field after construction.
     */
    public Link() {
    }

    /**
     * Creates a new instance initialized to the given URI.
     *
     * @param  uri  the URI.
     * @throws URISyntaxException if the given URI is invalid.
     */
    public Link(final String uri) throws URISyntaxException {
        this.uri = new URI(uri);
    }

    /**
     * Returns the link value.
     *
     * @return {@link #uri}.
     */
    @Override
    public URI getLinkage() {
        return uri;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     *
     * @return connection protocol to be used.
     */
    @Override
    public String getProtocol() {
        return null;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     *
     * @return application profile that can be used with the online resource.
     */
    @Override
    public String getApplicationProfile() {
        return null;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     *
     * @return name of the online resource.
     */
    @Override
    public InternationalString getName() {
        return null;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     *
     * @return text description of what the online resource is/does.
     */
    @Override
    public InternationalString getDescription() {
        return null;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     *
     * @return function performed by the online resource.
     */
    @Override
    public OnLineFunction getFunction() {
        return null;
    }

    /**
     * ISO 19115 metadata property not specified by GPX.
     *
     * @return request used to access the resource, or {@code null}.
     */
    @Override
    public String getProtocolRequest() {
        return null;
    }

    /**
     * Compares this {@code Link} with the given object for equality.
     *
     * @param  obj  the object to compare with this {@code Link}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Link) {
            final Link that = (Link) obj;
            return Objects.equals(this.uri, that.uri);
        }
        return false;
    }

    /**
     * Returns a hash code value for this {@code Link}.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(uri);
    }

    /**
     * Returns the string representation of {@link #uri}, or {@code null} if {@code uri} is null.
     * This method is intended to allow direct usage of {@code Link} in code that format arbitrary {@link Object}.
     *
     * @return the URI, or {@code null}.
     */
    @Override
    public String toString() {
        return (uri != null) ? uri.toString() : null;
    }
}
