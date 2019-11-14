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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URL;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;


/**
 * Handles conversions between {@link Path}, {@link File}, {@link URI} and {@link URL} objects.
 *
 * <h2>Immutability and thread safety</h2>
 * This base class and all inner classes are immutable, and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.7
 * @since   0.3
 * @module
 */
abstract class PathConverter<S,T> extends SystemConverter<S,T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4361503025580262022L;

    /**
     * Creates a path converter from the given source class to the given  target class.
     */
    PathConverter(final Class<S> sourceClass, final Class<T> targetClass) {
        super(sourceClass, targetClass);
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
     * then delegates to {@link #doConvert(Object)}.
     *
     * @param  source  the path to convert, or {@code null}.
     * @return the converted value, or {@code null} if the given path was null.
     * @throws UnconvertibleObjectException if an error occurred during the conversion.
     */
    @Override
    public final T apply(final S source) throws UnconvertibleObjectException {
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
     * Invoked by {@link #apply(Object)} for converting the given path to the target
     * type of this converter.
     *
     * @param  source  the path to convert, guaranteed to be non-null.
     * @return the converted path.
     * @throws Exception if an error occurred during the conversion.
     */
    abstract T doConvert(S source) throws Exception;

    /**
     * Converter from {@link Path} to {@link URI}.
     */
    public static final class PathURI extends PathConverter<Path,URI> {
        private static final long serialVersionUID = 740202123888081482L;
        static final PathURI INSTANCE = new PathURI();
        public PathURI() {super(Path.class, URI.class);}        // Instantiated by ServiceLoader.

        @Override public ObjectConverter<Path,URI> unique()  {return INSTANCE;}
        @Override public ObjectConverter<URI,Path> inverse() {return URIPath.INSTANCE;}
        @Override public URI doConvert(final Path source) {
            return source.toUri();
        }
    }

    /**
     * Converter from {@link Path} to {@link URL}.
     */
    public static final class PathURL extends PathConverter<Path,URL> {
        private static final long serialVersionUID = -289518201451769080L;
        static final PathURL INSTANCE = new PathURL();
        public PathURL() {super(Path.class, URL.class);}        // Instantiated by ServiceLoader.

        @Override public ObjectConverter<Path,URL> unique()  {return INSTANCE;}
        @Override public ObjectConverter<URL,Path> inverse() {return URLPath.INSTANCE;}
        @Override public URL doConvert(final Path source) throws MalformedURLException {
            return source.toUri().toURL();
        }
    }

    /**
     * Converter from {@link Path} to {@link File}.
     */
    public static final class PathFile extends PathConverter<Path,File> {
        private static final long serialVersionUID = 452241851474627778L;
        static final PathFile INSTANCE = new PathFile();
        public PathFile() {super(Path.class, File.class);}      // Instantiated by ServiceLoader.

        @Override public ObjectConverter<Path,File> unique()  {return INSTANCE;}
        @Override public ObjectConverter<File,Path> inverse() {return FilePath.INSTANCE;}
        @Override public File doConvert(final Path source) throws UnsupportedOperationException {
            return source.toFile();
        }
    }

    /**
     * Converter from {@link File} to {@link Path}.
     */
    public static final class FilePath extends PathConverter<File,Path> {
        private static final long serialVersionUID = 6420947028493989549L;
        static final FilePath INSTANCE = new FilePath();
        public FilePath() {super(File.class, Path.class);}      // Instantiated by ServiceLoader.

        @Override public ObjectConverter<File,Path> unique()  {return INSTANCE;}
        @Override public ObjectConverter<Path,File> inverse() {return PathFile.INSTANCE;}
        @Override public Path doConvert(final File source) {
            return source.toPath();
        }
    }

    /**
     * Converter from {@link File} to {@link URI}.
     * This converter changes relative paths to absolute paths.
     */
    public static final class FileURI extends PathConverter<File,URI> {
        private static final long serialVersionUID = 1122784850124333991L;
        static final FileURI INSTANCE = new FileURI();
        public FileURI() {super(File.class, URI.class);}        // Instantiated by ServiceLoader.

        @Override public ObjectConverter<File,URI> unique()  {return INSTANCE;}
        @Override public ObjectConverter<URI,File> inverse() {return URIFile.INSTANCE;}
        @Override public URI doConvert(final File source) {
            return source.toURI();
        }
    }

    /**
     * Converter from {@link File} to {@link URL}.
     */
    public static final class FileURL extends PathConverter<File,URL> {
        private static final long serialVersionUID = 2191394598748096966L;
        static final FileURL INSTANCE = new FileURL();
        public FileURL() {super(File.class, URL.class);}        // Instantiated by ServiceLoader.

        @Override public ObjectConverter<File,URL> unique()  {return INSTANCE;}
        @Override public ObjectConverter<URL,File> inverse() {return URLFile.INSTANCE;}
        @Override public URL doConvert(final File source) throws MalformedURLException {
            return source.toURI().toURL();
        }
    }

    /**
     * Converter from {@link URL} to {@link Path}.
     */
    public static final class URLPath extends PathConverter<URL,Path> {
        private static final long serialVersionUID = 4030502499990629230L;
        static final URLPath INSTANCE = new URLPath();
        public URLPath() {super(URL.class, Path.class);}        // Instantiated by ServiceLoader.

        @Override public ObjectConverter<URL,Path> unique()  {return INSTANCE;}
        @Override public ObjectConverter<Path,URL> inverse() {return PathURL.INSTANCE;}
        @Override public Path doConvert(final URL source) throws URISyntaxException {
            return Paths.get(source.toURI());
        }
    }

    /**
     * Converter from {@link URI} to {@link Path}.
     */
    public static final class URIPath extends PathConverter<URI,Path> {
        private static final long serialVersionUID = 5935532794533554151L;
        static final URIPath INSTANCE = new URIPath();
        public URIPath() {super(URI.class, Path.class);}        // Instantiated by ServiceLoader.

        @Override public ObjectConverter<URI,Path> unique()  {return INSTANCE;}
        @Override public ObjectConverter<Path,URI> inverse() {return PathURI.INSTANCE;}
        @Override public Path doConvert(final URI source) throws IllegalArgumentException {
            return Paths.get(source);
        }
    }

    /**
     * Converter from {@link URL} to {@link File}.
     */
    public static final class URLFile extends PathConverter<URL,File> {
        private static final long serialVersionUID = 3669726699184691997L;
        static final URLFile INSTANCE = new URLFile();
        public URLFile() {super(URL.class, File.class);}        // Instantiated by ServiceLoader.

        @Override public ObjectConverter<URL,File> unique()  {return INSTANCE;}
        @Override public ObjectConverter<File,URL> inverse() {return FileURL.INSTANCE;}
        @Override public File doConvert(final URL source) throws URISyntaxException {
            return new File(source.toURI());
        }
    }

    /**
     * Converter from {@link URI} to {@link File}.
     */
    public static final class URIFile extends PathConverter<URI,File> {
        private static final long serialVersionUID = 5070991554943811760L;
        static final URIFile INSTANCE = new URIFile();
        public URIFile() {super(URI.class, File.class);}        // Instantiated by ServiceLoader.

        @Override public ObjectConverter<URI,File> unique()  {return INSTANCE;}
        @Override public ObjectConverter<File,URI> inverse() {return FileURI.INSTANCE;}
        @Override public File doConvert(final URI source) throws IllegalArgumentException {
            return new File(source);
        }
    }

    /**
     * Converter from {@link URL} to {@link URI}.
     */
    public static final class URL_URI extends PathConverter<URL,URI> {
        private static final long serialVersionUID = 6327568235014244008L;
        static final URL_URI INSTANCE = new URL_URI();
        public URL_URI() {super(URL.class, URI.class);}         // Instantiated by ServiceLoader.

        @Override public ObjectConverter<URL,URI> unique()  {return INSTANCE;}
        @Override public ObjectConverter<URI,URL> inverse() {return URI_URL.INSTANCE;}
        @Override public URI doConvert(final URL source) throws URISyntaxException {
            return source.toURI();
        }
    }

    /**
     * Converter from {@link URI} to {@link URL}.
     */
    public static final class URI_URL extends PathConverter<URI,URL> {
        private static final long serialVersionUID = 5478354821309176895L;
        static final URI_URL INSTANCE = new URI_URL();
        public URI_URL() {super(URI.class, URL.class);}         // Instantiated by ServiceLoader.

        @Override public ObjectConverter<URI,URL> unique()  {return INSTANCE;}
        @Override public ObjectConverter<URL,URI> inverse() {return URL_URI.INSTANCE;}
        @Override public URL doConvert(final URI source) throws MalformedURLException {
            return source.toURL();
        }
    }
}
