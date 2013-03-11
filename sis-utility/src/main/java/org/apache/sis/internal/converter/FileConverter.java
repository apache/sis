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

import java.io.File;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.net.MalformedURLException;
import net.jcip.annotations.Immutable;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;


/**
 * Handles conversions from {@link File} to various objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.01)
 * @version 0.3
 * @module
 */
@Immutable
abstract class FileConverter<T> extends InjectiveConverter<File,T> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2150865427977735620L;

    /**
     * For inner classes only.
     */
    FileConverter() {
    }

    /**
     * Returns the source class, which is always {@link File}.
     */
    @Override
    public final Class<File> getSourceClass() {
        return File.class;
    }

    /**
     * Converter from {@link File} to {@link java.net.URI}.
     * Note that this converter change relative paths to absolute paths.
     */
    @Immutable
    static final class URI extends FileConverter<java.net.URI> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 1032598133849975567L;
        /** The unique, shared instance. */ static final URI INSTANCE = new URI();
        /** For {@link #INSTANCE} only.  */ private URI() {}

        @Override public Class<java.net.URI> getTargetClass() {
            return java.net.URI.class;
        }

        @Override public java.net.URI convert(final File source) {
            return (source != null) ? source.toURI() : null;
        }

        @Override public ObjectConverter<java.net.URI, File> inverse() {
            return URIConverter.File.INSTANCE;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link File} to {@link java.net.URL}.
     */
    @Immutable
    static final class URL extends FileConverter<java.net.URL> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 621496099287330756L;
        /** The unique, shared instance. */ static final URL INSTANCE = new URL();
        /** For {@link #INSTANCE} only.  */ private URL() {}

        @Override public Class<java.net.URL> getTargetClass() {
            return java.net.URL.class;
        }

        @Override public java.net.URL convert(final File source) throws UnconvertibleObjectException {
            if (source == null) {
                return null;
            }
            try {
                return source.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new UnconvertibleObjectException(formatErrorMessage(source), e);
            }
        }

        @Override public ObjectConverter<java.net.URL, File> inverse() {
            return URLConverter.File.INSTANCE;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }
}
