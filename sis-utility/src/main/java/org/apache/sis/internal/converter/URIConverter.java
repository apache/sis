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
import java.net.MalformedURLException;
import java.net.URI;
import net.jcip.annotations.Immutable;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;


/**
 * Handles conversions from {@link java.net.URI} to various objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.01)
 * @version 0.3
 * @module
 */
@Immutable
abstract class URIConverter<T> extends InjectiveConverter<URI,T> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5419481828621160876L;

    /**
     * For inner classes only.
     */
    URIConverter() {
    }

    /**
     * Returns the source class, which is always {@link URI}.
     */
    @Override
    public final Class<URI> getSourceClass() {
        return URI.class;
    }

    /**
     * Converter from {@link URI} to {@link java.lang.String}.
     * This converter does not encode the string, i.e. the conversion is performed with
     * the {@link URI#toString()} method rather than {@link URI#toASCIIString()}. We do
     * that in order to avoid too many transformations if we convert back and forward to
     * {@code String}.
     */
    @Immutable
    static final class String extends URIConverter<java.lang.String> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -1745990349642467147L;
        /** The unique, shared instance. */ static final String INSTANCE = new String();
        /** For {@link #INSTANCE} only.  */ private String() {}

        @Override public Class<java.lang.String> getTargetClass() {
            return java.lang.String.class;
        }

        @Override public java.lang.String convert(final URI source) {
            return (source != null) ? source.toString() : null;
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.lang.String, URI> inverse() {
            return StringConverter.URI.INSTANCE;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link URI} to {@link java.io.File}.
     */
    @Immutable
    static final class File extends URIConverter<java.io.File> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 5289256237146366469L;
        /** The unique, shared instance. */ static final File INSTANCE = new File();
        /** For {@link #INSTANCE} only.  */ private File() {}

        @Override public Class<java.io.File> getTargetClass() {
            return java.io.File.class;
        }

        @Override public java.io.File convert(final URI source) throws UnconvertibleObjectException {
            if (source == null) {
                return null;
            }
            try {
                return new java.io.File(source);
            } catch (IllegalArgumentException e) {
                throw new UnconvertibleObjectException(formatErrorMessage(source), e);
            }
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.io.File, URI> inverse() {
            return FileConverter.URI.INSTANCE;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link URI} to {@link java.net.URL}.
     */
    @Immutable
    static final class URL extends URIConverter<java.net.URL> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -7866572007304228474L;
        /** The unique, shared instance. */ static final URL INSTANCE = new URL();
        /** For {@link #INSTANCE} only.  */ private URL() {}

        @Override public Class<java.net.URL> getTargetClass() {
            return java.net.URL.class;
        }

        @Override public java.net.URL convert(final URI source) throws UnconvertibleObjectException {
            if (source == null) {
                return null;
            }
            try {
                return source.toURL();
            } catch (MalformedURLException e) {
                throw new UnconvertibleObjectException(formatErrorMessage(source), e);
            }
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.net.URL, URI> inverse() {
            return URLConverter.URI.INSTANCE;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }
}
