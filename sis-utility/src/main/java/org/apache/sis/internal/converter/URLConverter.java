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
package org.apache.sis.internal.converter;

import java.io.Serializable;
import java.io.ObjectStreamException;
import java.net.URISyntaxException;
import java.net.URL;
import net.jcip.annotations.Immutable;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;


/**
 * Handles conversions from {@link java.net.URL} to various objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.01)
 * @version 0.3
 * @module
 */
@Immutable
abstract class URLConverter<T> extends InjectiveConverter<URL,T> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4843540356265851861L;

    /**
     * For inner classes only.
     */
    URLConverter() {
    }

    /**
     * Returns the source class, which is always {@link URL}.
     */
    @Override
    public final Class<URL> getSourceClass() {
        return URL.class;
    }

    /**
     * Converter from {@link URL} to {@link java.io.File}.
     */
    @Immutable
    static final class File extends URLConverter<java.io.File> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 1228852836485762335L;
        /** The unique, shared instance. */ static final File INSTANCE = new File();
        /** For {@link #INSTANCE} only.  */ private File() {}

        @Override public Class<java.io.File> getTargetClass() {
            return java.io.File.class;
        }

        @Override public java.io.File convert(final URL source) throws UnconvertibleObjectException {
            if (source == null) {
                return null;
            }
            try {
                return new java.io.File(source.toURI());
            } catch (URISyntaxException | IllegalArgumentException e) {
                throw new UnconvertibleObjectException(formatErrorMessage(source), e);
            }
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.io.File, URL> inverse() {
            return FileConverter.URL.INSTANCE;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link URL} to {@link java.net.URI}.
     */
    @Immutable
    static final class URI extends URLConverter<java.net.URI> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -1653233667050600894L;
        /** The unique, shared instance. */ static final URI INSTANCE = new URI();
        /** For {@link #INSTANCE} only.  */ private URI() {}

        @Override public Class<java.net.URI> getTargetClass() {
            return java.net.URI.class;
        }

        @Override public java.net.URI convert(final URL source) throws UnconvertibleObjectException {
            if (source == null) {
                return null;
            }
            try {
                return source.toURI();
            } catch (URISyntaxException e) {
                throw new UnconvertibleObjectException(formatErrorMessage(source), e);
            }
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.net.URI, URL> inverse() {
            return URIConverter.URL.INSTANCE;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }
}
