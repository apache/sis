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

import java.util.Set;
import java.util.EnumSet;
import java.io.File;
import java.net.URL;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import net.jcip.annotations.Immutable;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;


/**
 * Handles conversions between {@link Path}, {@link File}, {@link URI} and {@link URL} objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.01)
 * @version 0.3
 * @module
 */
@Immutable
abstract class PathConverter<S,T> extends SystemConverter<S,T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2150865427977735620L;

    /**
     * Creates a path converter from the given source class to the given  target class.
     */
    PathConverter(final Class<S> sourceClass, final Class<T> targetClass) {
        super(sourceClass, targetClass);
    }

    /**
     * Returns a predefined instance for the given target class, or {@code null} if none.
     * This method does not create any new instance.
     *
     * @param  <T> The target class.
     * @param  targetClass The target class.
     * @return An instance for the given target class, or {@code null} if none.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    static <S,T> PathConverter<S,T> getInstance(final Class<S> sourceClass, final Class<T> targetClass) {
        if (sourceClass == File.class) {
            if (targetClass == URI.class) return (PathConverter<S,T>) FileURI.INSTANCE;
            if (targetClass == URL.class) return (PathConverter<S,T>) FileURL.INSTANCE;
        } else if (sourceClass == URL.class) {
            if (targetClass == File.class) return (PathConverter<S,T>) URLFile.INSTANCE;
            if (targetClass == URI .class) return (PathConverter<S,T>) URL_URI.INSTANCE;
        } else if (sourceClass == URI.class) {
            if (targetClass == File.class) return (PathConverter<S,T>) URIFile.INSTANCE;
            if (targetClass == URL .class) return (PathConverter<S,T>) URI_URL.INSTANCE;
        }
        return null;
    }

    /**
     * Returns the singleton instance on deserialization, if any.
     */
    @Override
    public final ObjectConverter<S, T> unique() {
        final PathConverter<S,T> instance = getInstance(sourceClass, targetClass);
        return (instance != null) ? instance : super.unique();
    }

    /**
     * Returns the properties of this converter.
     */
    @Override
    public final Set<FunctionProperty> properties() {
        return EnumSet.of(FunctionProperty.INJECTIVE, FunctionProperty.INVERTIBLE);
    }

    /**
     * Converts the given path to the target type of this converter.
     * This method verifies that the given path is non-null,
     * then delegates to {@link #doConvert(S)}.
     *
     * @param  source The path to convert, or {@code null}.
     * @return The converted value, or {@code null} if the given path was null.
     * @throws UnconvertibleObjectException If an error occurred during the conversion.
     */
    @Override
    public final T convert(final S source) throws UnconvertibleObjectException {
        if (source == null) {
            return null;
        }
        try {
            return doConvert(source);
        } catch (Exception e) {
            throw new UnconvertibleObjectException(formatErrorMessage(source), e);
        }
    }

    /**
     * Invoked by {@link #convert(S)} for converting the given path to the target
     * type of this converter.
     *
     * @param  source The path to convert, guaranteed to be non-null.
     * @return The converted path.
     * @throws Exception If an error occurred during the conversion.
     */
    abstract T doConvert(S source) throws Exception;

    /**
     * Converter from {@link File} to {@link URI}.
     * This converter changes relative paths to absolute paths.
     */
    private static final class FileURI extends PathConverter<File,URI> {
        private static final long serialVersionUID = 1032598133849975567L;
        static final FileURI INSTANCE = new FileURI();
        private FileURI() {super(File.class, URI.class);}

        @Override public ObjectConverter<URI,File> inverse() {return URIFile.INSTANCE;}
        @Override public URI doConvert(final File source) {
            return source.toURI();
        }
    }

    /**
     * Converter from {@link File} to {@link URL}.
     */
    private static final class FileURL extends PathConverter<File,URL> {
        private static final long serialVersionUID = 621496099287330756L;
        static final FileURL INSTANCE = new FileURL();
        private FileURL() {super(File.class, URL.class);}

        @Override public ObjectConverter<URL,File> inverse() {return URLFile.INSTANCE;}
        @Override public URL doConvert(final File source) throws MalformedURLException {
            return source.toURI().toURL();
        }
    }

    /**
     * Converter from {@link URL} to {@link File}.
     */
    private static final class URLFile extends PathConverter<URL,File> {
        private static final long serialVersionUID = 1228852836485762335L;
        static final URLFile INSTANCE = new URLFile();
        private URLFile() {super(URL.class, File.class);}

        @Override public ObjectConverter<File,URL> inverse() {return FileURL.INSTANCE;}
        @Override public File doConvert(final URL source) throws URISyntaxException {
            return new File(source.toURI());
        }
    }

    /**
     * Converter from {@link URL} to {@link File}.
     */
    private static final class URIFile extends PathConverter<URI,File> {
        private static final long serialVersionUID = 5289256237146366469L;
        static final URIFile INSTANCE = new URIFile();
        private URIFile() {super(URI.class, File.class);}

        @Override public ObjectConverter<File,URI> inverse() {return FileURI.INSTANCE;}
        @Override public File doConvert(final URI source) throws IllegalArgumentException {
            return new File(source);
        }
    }

    /**
     * Converter from {@link URL} to {@link URI}.
     */
    @Immutable
    static final class URL_URI extends PathConverter<URL,URI> {
        private static final long serialVersionUID = -1653233667050600894L;
        static final URL_URI INSTANCE = new URL_URI();
        private URL_URI() {super(URL.class, URI.class);}

        @Override public ObjectConverter<URI, URL> inverse() {return URI_URL.INSTANCE;}
        @Override public URI doConvert(final URL source) throws URISyntaxException {
            return source.toURI();
        }
    }

    /**
     * Converter from {@link URI} to {@link URL}.
     */
    static final class URI_URL extends PathConverter<URI,URL> {
        private static final long serialVersionUID = -7866572007304228474L;
        static final URI_URL INSTANCE = new URI_URL();
        private URI_URL() {super(URI.class, URL.class);}

        @Override public ObjectConverter<URL, URI> inverse() {return URL_URI.INSTANCE;}
        @Override public URL doConvert(final URI source) throws MalformedURLException {
            return source.toURL();
        }
    }
}
