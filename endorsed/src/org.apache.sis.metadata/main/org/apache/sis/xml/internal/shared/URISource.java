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
package org.apache.sis.xml.internal.shared;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.transform.stream.StreamSource;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.system.DataURI;


/**
 * A source of XML document which is read from an URL.
 * This class should be handled as a standard {@link StreamSource} by Java API,
 * but allows Apache SIS to keep a reference to the original {@link URI} object.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class URISource extends StreamSource {
    /**
     * If the URI has been resolved relatively to some base directory or sibling file, that base.
     * Otherwise {@code null}.
     */
    private final URI base;

    /**
     * Normalized URI of the XML document, without the fragment part if the document will be read from this URL.
     * The URI is normalized for making possible to use it as a key in a cache of previously loaded documents.
     */
    public final URI document;

    /**
     * The fragment part of the original URI, or {@code null} if none of if the fragment is in the document URI.
     * The latter case happen when the XML will need to be read from the input stream rather than from the URI,
     * in which case we do not know if the input stream is not already for the fragment.
     */
    public final String fragment;

    /**
     * Creates a source from an URI. This constructor separates the fragment from the path.
     * The URI stored by this constructor in {@link #document} excludes the fragment part.
     *
     * @param  base    the base directory from which to resolve the source, or {@code null} if none.
     * @param  source  URI to the XML document.
     * @throws URISyntaxException if an error occurred while normalizing the URI.
     */
    URISource(URI base, URI source) throws URISyntaxException {
        if (base != null) {
            if (source == (source = base.resolve(source))) {
                base = null;    // The base could not be used.
            }
        }
        this.base = base;
        source = source.normalize();
        fragment = Strings.trimOrNull(source.getFragment());
        // Build a new URI unconditionally because it also decodes escaped characters.
        source = new URI(source.getScheme(), source.getSchemeSpecificPart(), null);
        if (source.isOpaque() && "file".equalsIgnoreCase(source.getScheme())) {
            /*
             * If the URI is "file:something" without "/" or "///" characters, resolve as an absolute path.
             * This special case happens if `IOUtilities.toFileOrURI(String)` did not converted a string to
             * a `java.io.File` because it contains a fragment. Since this constructor removed the fragment,
             * we can now attempt this conversion again. The result will be an absolute path. This is needed
             * for `URI.resolve(URI)` to work.
             */
            source = new File(source.getSchemeSpecificPart()).toURI();
        }
        document = source;
    }

    /**
     * Creates a new source from the given input stream.
     * The input should not be null, unless it will be specified later
     * by a call to {@code setInputStream(…)} or {@code setReader(…)}.
     * This method never returns {@code null} since the input may be set after this method call.
     *
     * @param  input   stream of the XML document, or {@code null} if none.
     * @param  base    the base directory from which to resolve the source, or {@code null} if none.
     * @param  source  URL of the XML document, or {@code null} if none.
     * @return the given input stream as a source, never null even if all arguments were null.
     * @throws URISyntaxException if an error occurred while normalizing the URI.
     */
    public static StreamSource create(final InputStream input, final URI base, final URI source)
            throws URISyntaxException
    {
        if (source != null) {
            var s = new URISource(base, source);
            s.setInputStream(input);
            return s;
        } else {
            return new StreamSource(input);
        }
    }

    /**
     * Returns whether the document URL is in the same directory or in a sub-directory
     * of the base given at construction time. This is used for access control.
     *
     * @return whether the document URL has been resolved relatively to the base.
     */
    public boolean isChildOfBase() {
        return (base != null) && DataURI.isPathInDirectory(base, document);
    }

    /**
     * If this source is defined only by URI (no input stream), returns that URI.
     * Otherwise returns {@code null}.
     *
     * @return the URI, or {@code null} if not applicable for reading the document.
     */
    public URI getReadableURI() {
        return (getInputStream() == null && getReader() == null) ? document : null;
    }

    /**
     * Gets the system identifier derived from the URI.
     * The system identifier is the URL encoded in ASCII, computed when first needed.
     */
    @Override
    public String getSystemId() {
        String systemId = super.getSystemId();
        if (systemId == null) {
            systemId = document.toASCIIString();
            setSystemId(systemId);
        }
        return systemId;
    }

    /**
     * Returns a string representation of this source for debugging purposes.
     *
     * @return string representation for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "document", document, "fragment", fragment,
                                "inputStream", getInputStream(), "reader", getReader());
    }
}
