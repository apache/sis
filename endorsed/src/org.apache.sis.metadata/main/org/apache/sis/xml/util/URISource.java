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
package org.apache.sis.xml.util;

import java.net.URI;
import java.io.InputStream;
import javax.xml.transform.stream.StreamSource;
import org.apache.sis.util.internal.Strings;


/**
 * A source of XML document which is read from an URL.
 * This class should be handled as a standard {@link StreamSource} by Java API,
 * but allows Apache SIS to keep a reference to the original {@link URI} object.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class URISource extends StreamSource {
    /**
     * URL of the XML document.
     */
    final URI source;

    /**
     * Creates a source from a URL.
     *
     * @param source  URL of the XML document.
     */
    URISource(final URI source) {
        this.source = source;
    }

    /**
     * Creates a new source.
     *
     * @param input   stream of the XML document.
     * @param source  URL of the XML document.
     */
    private URISource(final InputStream input, final URI source) {
        super(input);
        this.source = source;
    }

    /**
     * Creates a new source.
     *
     * @param  input   stream of the XML document.
     * @param  source  URL of the XML document, or {@code null} if none.
     * @return the given input stream as a source.
     */
    public static StreamSource create(final InputStream input, final URI source) {
        if (source != null) {
            return new URISource(input, source);
        } else {
            return new StreamSource(input);
        }
    }

    /**
     * Gets the system identifier derived from the URI.
     * The system identifier is the URL encoded in ASCII, computed when first needed.
     */
    @Override
    public String getSystemId() {
        String systemId = super.getSystemId();
        if (systemId == null) {
            systemId = source.toASCIIString();
            setSystemId(systemId);
        }
        return systemId;
    }

    /**
     * {@return a string representation of this source for debugging purposes}.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "source", source, "inputStream", getInputStream());
    }
}
