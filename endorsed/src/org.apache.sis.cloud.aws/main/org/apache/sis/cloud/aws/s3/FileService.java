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
package org.apache.sis.cloud.aws.s3;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.CopyOption;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.AccessDeniedException;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.nio.channels.SeekableByteChannel;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.internal.shared.Strings;


/**
 * A provider of file system services as wrappers around Amazon Simple Storage Service (AWS S3).
 * This provider accepts URIs of the following forms:
 *
 * <ul>
 *   <li>{@code S3://bucket/file}</li>
 *   <li>{@code S3://host:port/bucket/file}</li>
 *   <li>{@code S3://accessKey@bucket/file} (password not allowed)</li>
 *   <li>{@code S3://accessKey@host:port/bucket/key} (password not allowed)</li>
 * </ul>
 *
 * "Files" are S3 keys interpreted as paths with components separated by the {@code '/'} separator.
 * The password and the region can be specified at {@linkplain #newFileSystem file system initialization time}.
 * The endpoint (e.g. {@code "s3.eu-central-1.amazonaws.com"}) shall <em>not</em> be specified in the <abbr>URI</abbr>.
 * In particular the region ({@code "eu-central-1"} in the above example) can depend on the server location
 * instead of the data to access, and can be a global configuration for the server.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Quentin Bialota (Geomatys)
 * @version 1.7
 * @since   1.2
 */
public class FileService extends FileSystemProvider {
    /**
     * The default port number for the <abbr>HTTP</abbr> protocol.
     */
    static final int HTTP_PORT = 80;

    /**
     * The default port number for the <abbr>HTTPS</abbr> protocol.
     */
    static final int HTTPS_PORT = 443;

    /**
     * The property for the secret access key (password).
     * Values shall be instances of {@link String}.
     * If not specified, the AWS SDK default mechanism searches for the first of the following:
     *
     * <ul>
     *   <li>{@code AWS_SECRET_ACCESS_KEY} environment variable.</li>
     *   <li>{@code ~/.aws/credentials} and {@code ~/.aws/conﬁg} files.</li>
     * </ul>
     *
     * @see #newFileSystem(URI, Map)
     */
    public static final String AWS_SECRET_ACCESS_KEY = "aws.secretAccessKey";

    /**
     * The property for the region.
     * Values shall should be instances of {@link Region} or
     * strings {@linkplain Region#of(String) convertible} to region.
     * If not specified, the <abbr>AWS</abbr> <abbr>SDK</abbr> default
     * mechanism searches for the first of the following:
     *
     * <ul>
     *   <li>{@code AWS_REGION} environment variable.</li>
     *   <li>{@code ~/.aws/credentials} and {@code ~/.aws/conﬁg} files.</li>
     * </ul>
     *
     * @see #newFileSystem(URI, Map)
     */
    public static final String AWS_REGION = "aws.region";

    /**
     * The property for the host (mandatory if not using <abbr>AWS</abbr> S3).
     * Values shall be instances of {@link String}.
     *
     * @see #newFileSystem(URI, Map)
     * @since 1.7
     */
    public static final String HOST_URL = "hostURL";

    /**
     * The property for the port (mandatory if not using <abbr>AWS</abbr> S3).
     * Values shall be instances of {@link Integer}.
     *
     * @see #newFileSystem(URI, Map)
     * @since 1.7
     */
    public static final String PORT = "port";

    /**
     * The property for the protocol (optional).
     * Values shall be instances of {@link Boolean}.
     * The default value is {@code true} (<abbr>HTTPS</abbr>).
     *
     * @see #newFileSystem(URI, Map)
     * @since 1.7
     */
    public static final String IS_HTTPS = "isHttps";

    /**
     * The property for the name-separator characters.
     * The default value is "/" for simulating Unix paths.
     * The separator must contain at least one character.
     * They usually have only one character, but longer separators are accepted.
     * The separator can contain any characters which are valid in a S3 object name.
     */
    public static final String SEPARATOR = "separator";

    /**
     * All file systems created by this provider. Keys are <abbr>AWS</abbr> S3 access keys.
     */
    private final ConcurrentMap<Server, ClientFileSystem> fileSystems;

    /**
     * Creates a new provider of file systems for Amazon S3.
     */
    public FileService() {
        fileSystems = new ConcurrentHashMap<>();
    }

    /**
     * Returns the <abbr>URI</abbr> scheme that identifies this provider, which is {@code "S3"}.
     *
     * @return the {@code "S3"} <abbr>URI</abbr> scheme.
     */
    @Override
    public String getScheme() {
        return KeyPath.SCHEME;
    }

    /**
     * Returns the key used in the cache of file systems, or {@code null} if none.
     * Keys are created from the URI scheme and user info.
     * The remaining parts of the URI (path, query, <i>etc.</i>) are ignored.
     *
     * @throws IllegalArgumentException if the URI scheme is not "S3" (ignoring case).
     */
    private String getAccessKey(final URI uri) {
        final String scheme = uri.getScheme();
        if (scheme != null && !getScheme().equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.UnexpectedProtocol_1, scheme));
        }
        return uri.getUserInfo();
    }

    /**
     * Initializes and returns a new file system identified by a <abbr>URI</abbr>.
     * The given <abbr>URI</abbr> shall have the following pattern:
     *
     * <pre class="text">S3://accessKey@bucket/file</pre>
     *
     * In current version all path components after {@code accessKey} are ignored.
     * A future version may allow finer grain control.
     *
     * <h4>Authentication</h4>
     * The access key is a kind of login (not a password).
     * The password (also called "secret access key") shall not be specified in the URI;
     * syntax like {@code accessKey:password} will <em>not</em> be parsed by this package.
     * Instead the password can be specified in the given map as a {@link String} value
     * associated to the {@value #AWS_SECRET_ACCESS_KEY} key.
     *
     * <h4>Recognized properties</h4>
     * The following properties are accepted:
     *
     * <ul>
     *   <li>{@value #AWS_SECRET_ACCESS_KEY} with {@link String} value.</li>
     *   <li>{@value #AWS_REGION} with {@link Region} value or a string
     *     {@linkplain Region#of(String) convertible} to region.</li>
     *   <li>{@value #HOST_URL} with {@link String} value.</li>
     *   <li>{@value #PORT} with {@link Integer} value.</li>
     *   <li>{@value #IS_HTTPS} with {@link Boolean} value.</li>
     * </ul>
     *
     * <p>The <abbr>URI</abbr> can be of the form {@code "s3://accessKey@bucket/file"} (<abbr>AWS</abbr> S3)
     * or {@code "s3://accessKey@host:port/bucket/file"} (self-hosted S3).
     * In the latter case, the host <em>and</em> the port are mandatory.</p>
     *
     * @param  uri an <abbr>URI</abbr> of the form {@code "s3://accessKey@[host:port/]bucket/file"}.
     * @param  properties  properties to configure the file system, or {@code null} if none.
     * @return the new file system.
     * @throws IllegalArgumentException if the <abbr>URI</abbr> or the map contains invalid values.
     * @throws IOException if an I/O error occurs while creating the file system.
     * @throws FileSystemAlreadyExistsException if a file system has already been created
     *         for the given URI and has not yet been closed.
     */
    @Override
    public FileSystem newFileSystem(final URI uri, final Map<String,?> properties) throws IOException {
        final String accessKey = getAccessKey(uri);
        final String secret;
        if (accessKey == null || (secret = Containers.property(properties, AWS_SECRET_ACCESS_KEY, String.class)) == null) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.MissingAccessKey_2, (accessKey == null) ? 0 : 1, uri));
        }
        final String  separator = Containers.property(properties, SEPARATOR, String.class);
        final Region  region    = property(properties, AWS_REGION, Region.class, null, Region::of);
        /*
         * Host and port number specified in the property map have precedence over the URI.
         * If these information were not found anywhere, the AWS SDK will fetch them itself.
         */
        final var server = new Server(accessKey,
                Containers.property(properties, HOST_URL, String.class),
                property(properties, PORT, Integer.class, Server.NO_PORT, Integer::valueOf),
                property(properties, IS_HTTPS, Boolean.class, Server.DEFAULT_IS_HTTPS, Strings::parseBoolean),
                uri);
        /*
         * The following class is for checking if a `ClientFileSystem` exists before to create one,
         * in one atomic concurrent hash map operation. The standard `Map.computeIfAbsent(…)` method
         * does not tell us whether the returned value was the existing one or a new one.
         * We need a flag for differentiating the two cases.
         */
        final class Creator implements Function<Server, ClientFileSystem> {
            /** Whether this function has been invoked.  */ boolean invoked;
            /** If the operation failed, the reason why. */ URISyntaxException exception;

            @Override
            public ClientFileSystem apply(final Server key) {
                invoked = true;
                try {
                    return new ClientFileSystem(FileService.this, region, key, secret, separator);
                } catch (URISyntaxException e) {
                    exception = e;
                    return null;
                }
            }
        }
        final var c = new Creator();
        final ClientFileSystem fs = fileSystems.computeIfAbsent(server, c);
        if (c.exception != null) {
            final var e = new MalformedURLException(Resources.format(Resources.Keys.CannotConnectTo_1, server));
            e.initCause(c.exception);
            throw e;
        } else if (c.invoked) {
            return fs;
        }
        throw new FileSystemAlreadyExistsException(Resources.format(Resources.Keys.FileSystemInitialized_2, 1, accessKey));
    }

    /**
     * Returns a value from the map specified in the constructor.
     * The value can be parsed from a string representation.
     *
     * @param  <T>           compile-time value of the {@code type} argument.
     * @param  properties    map from which to get a property value.
     * @param  key           key of the property to get.
     * @param  type          type of the property to get.
     * @param  defaultValue  default value if the key is not associated to a non-null value.
     * @param  parser        function to invoke for converting a text to a value.
     * @return the property value for the given key cast to the given type, or {@code defaultValue} if none.
     * @throws IllegalArgumentException if the value is not of the expected type.
     */
    private static <T> T property(final Map<String,?> properties, final String key,
                                  final Class<T> type, final T defaultValue,
                                  final Function<String, T> parser)
    {
        final Object value = properties.get(key);
        if (value == null) {
            return defaultValue;
        } else if (value instanceof CharSequence) {
            T c = parser.apply((String) value);
            if (c != null) return c;
        }
        try {
            return type.cast(value);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(Errors.forProperties(properties)
                    .getString(Errors.Keys.IllegalPropertyValueClass_3, key, type, value.getClass()), e);
        }
    }

    /**
     * Removes the given file system from the cache.
     * This method is invoked after the file system has been closed.
     */
    final void dispose(final Server server) {
        fileSystems.remove(server);
    }

    /**
     * Returns a reference to a file system that was created by the {@link #newFileSystem(URI, Map)} method.
     * If the file system has not been created or has been closed,
     * then this method throws {@link FileSystemNotFoundException}.
     *
     * <p>The <abbr>URI</abbr> scheme should be the value returned by {@link #getScheme()}, which is usually {@code "S3"}.
     * The <abbr>AWS</abbr> S3 service may be implemented on top of <abbr>HTTP</abbr> or <abbr>HTTPS</abbr> protocol.
     * This method detects automatically which protocol was specified in the call to {@link #newFileSystem(URI, Map)}.
     * If the two protocols have been used for the same host, then this method uses the port number for disambiguation:
     * port 80 is mapped to <abbr>HTTP</abbr> and port 443 is mapped to <abbr>HTTPS</abbr>.</p>
     *
     * @param  uri  an <abbr>URI</abbr> of the form {@code "s3://accessKey@bucket/file"} or {@code "s3://accessKey@host:port/bucket/key"}.
     * @return the file system previously created by {@link #newFileSystem(URI, Map)}.
     * @throws IllegalArgumentException if the <abbr>URI</abbr> is not supported by this provider.
     * @throws FileSystemNotFoundException if the file system does not exist or has been closed.
     */
    @Override
    public FileSystem getFileSystem(final URI uri) {
        return getFileSystem(uri, false);
    }

    /**
     * Implementation of {@link #getFileSystem(URI)} with the option of creating the file system instead
     * of throwing an exception.
     *
     * @param  uri     an <abbr>URI</abbr> of the form {@code "s3://accessKey@bucket/file"} or {@code "s3://accessKey@host:port/bucket/key"}.
     * @param  create  {@code true} for creating the file system if it does not already exist.
     * @return the file system previously created by {@link #newFileSystem(URI, Map)}.
     * @throws FileSystemNotFoundException if the file system does not exist and {@code create} is {@code false}.
     */
    private ClientFileSystem getFileSystem(final URI uri, final boolean create) {
        final String accessKey = getAccessKey(uri);
        boolean isHttps = Server.DEFAULT_IS_HTTPS;
        int port = uri.getPort();
        final String host = (port >= 0) ? uri.getHost() : null;
        switch (port) {
            case HTTP_PORT:  isHttps = false; break;
            case HTTPS_PORT: isHttps = true;  break;
        }
        /*
         * Try the following combinations, in order
         * (the logic is to give precedence to explicit parameters, then to security):
         *
         *   - specified port (may be -1),   isHttps
         *   - specified port (may be -1),  !isHttps
         *   - default port for  `isHttps`,  isHttps
         *   - default port for `!isHttps`, !isHttps
         *   - default port for `!isHttps`,  isHttps  (unusual, possibly a user's error.
         *   - default port for  `isHttps`, !isHttps  (unusual, possibly a user's error.
         */
        boolean tryDefaultPorts  = false;
        boolean tryOppositePorts = false;
        Server first = null;
        for (;;) {
            if (tryDefaultPorts) {
                port = (tryOppositePorts ^ isHttps) ? HTTPS_PORT : HTTP_PORT;
            }
            final Server server = new Server(accessKey, host, port, isHttps, uri);
            final ClientFileSystem fs = fileSystems.get(server);
            if (fs != null) {
                return fs;
            }
            if (first == null) {
                first = server;     // Will be used for the error message if we cannot find a file system.
            }
            isHttps = !isHttps;
            if (isHttps == first.isHttps) {
                if (!tryDefaultPorts) {
                    if (port >= 0) break;       // Do not try default ports if a port was explicitly specified.
                    tryDefaultPorts = true;
                } else if (!tryOppositePorts) {
                    tryOppositePorts = true;
                } else {
                    break;      // We tried all combinations.
                }
            }
        }
        /*
         * No existing file system found. Create if we are allowed to do so.
         */
        if (create) {
            return fileSystems.computeIfAbsent(first, (key) -> {
                try {
                    return new ClientFileSystem(this, null, key, null, null);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException(Resources.format(Resources.Keys.CannotConnectTo_1, key), e);
                }
            });
        } else {
            throw new FileSystemNotFoundException(Resources.format(
                    Resources.Keys.FileSystemInitialized_2, 0, first));
        }
    }

    /**
     * Return a {@code Path} object by converting the given {@code URI}.
     * The resulting {@code Path} is associated with a {@link FileSystem}
     * that already exists or is constructed automatically.
     *
     * <p>The <abbr>URI</abbr> can be of the form {@code "s3://accessKey@bucket/file"} (<abbr>AWS</abbr> S3)
     * or {@code "s3://accessKey@host:port/bucket/file"} (self-hosted S3).
     * In the latter case, the host <em>and</em> the port are mandatory.</p>
     *
     * @param  uri an <abbr>URI</abbr> of the form {@code "s3://accessKey@[host:port/]bucket/file"}.
     * @return the resulting {@code Path}.
     * @throws IllegalArgumentException if the URI is not supported by this provider.
     * @throws FileSystemNotFoundException if the file system does not exist and cannot be created automatically.
     */
    @Override
    public Path getPath(final URI uri) {
        /*
         * In case of custom host, bucket name will be the first element of the "uri.getPath()".
         * We want:
         *
         *   - `host` as the S3 bucket name.
         *   - `path` as the path in above bucket.
         */
        String path = uri.getPath();
        String host = uri.getHost();
        if (host != null) {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            String[] parts = path.split("/", 2);
            if (parts.length >= 2) {
                // Bucket + Path specified. Example: "/bucket/path/to/folder"
                host = parts[0];
                path = "/" + parts[1];
            } else if (parts.length == 1) {
                // Bucket specified without path. Example: "/bucket"
                host = parts[0];
                path = null;
            }
        }
        final ClientFileSystem fs = getFileSystem(uri, true);
        return new KeyPath(fs, host, (path != null) ? new String[] {path} : CharSequences.EMPTY_ARRAY, true);
    }

    /**
     * Returns the given path as an absolute S3 path with a key component,
     * or throws an exception otherwise.
     */
    private static KeyPath toAbsolute(final Path path, final boolean requireKey) {
        short cause = Resources.Keys.MustBeAbsolutePath;
        if (path instanceof KeyPath) {
            final KeyPath kp = (KeyPath) path;
            if (kp.bucket != null) {
                if (!requireKey || kp.key != null) {
                    return kp;
                }
                cause = Resources.Keys.MustHaveKeyComponent;
            }
        }
        throw new IllegalArgumentException(Resources.format(cause));
    }

    /**
     * Tests if two paths locate the same file.
     *
     * @param  path1  one path to the file.
     * @param  path2  the other path.
     * @return {@code true} if the two paths locate the same file.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public boolean isSameFile(final Path path1, final Path path2) throws IOException {
        return toAbsolute(path1, false).equals(path2);
    }

    /**
     * Tells whether a file is considered hidden.
     * Current implementation always return {@code false}.
     *
     * @param  path  the path to the file to test.
     * @return {@code true} if the file is considered hidden.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public boolean isHidden(Path path) throws IOException {
        return false;
    }

    /**
     * Returns the store where a file is located.
     *
     * @param  path  the path to the file.
     * @return the store where the file is stored.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public FileStore getFileStore(final Path path) throws IOException {
        return new BucketStore(toAbsolute(path, false).bucket());
    }

    /**
     * Opens a directory and returns a {@code DirectoryStream} to iterate over its entries.
     * S3 does not formally has directories. Instead directories are simulated by handling
     * the {@code '/'} character in a special way.
     *
     * @param  directory  the path to the directory.
     * @param  filter     the directory stream filter.
     * @return a new and open stream on directory entries.
     * @throws NotDirectoryException if the file is not a directory.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path directory,
            final DirectoryStream.Filter<? super Path> filter) throws IOException
    {
        final KeyPath kp = toAbsolute(directory, false);
        ArgumentChecks.ensureNonNull("filter", filter);
        if (kp.isDirectory) try {
            return new PathIterator(kp, filter);
        } catch (SdkException e) {
            throw failure(directory, e);
        } else {
            throw new NotDirectoryException(kp.toString());
        }
    }

    /**
     * Creates a new pseudo-directory.
     * S3 does not formally has directories, so the current operation does nothing.
     * Instead a pseudo-directory will appear after the first file or sub-directory is added in the directory.
     *
     * @param  directory   the directory to create.
     * @param  attributes  an optional list of file attributes to set when creating the directory.
     * @throws IOException if an I/O error occurs or the parent directory does not exist.
     */
    @Override
    public void createDirectory(final Path directory, final FileAttribute<?>... attributes) throws IOException {
        if (attributes.length != 0) {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnsupportedArgumentValue_1, attributes[0]));
        }
    }

    /**
     * Copies a file.
     * This method is not yet supported.
     *
     * @param  source   the path to the file to copy.
     * @param  target   the path to the target file.
     * @param  options  options specifying how the copy should be done
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void copy(final Path source, final Path target, final CopyOption... options) throws IOException {
        throw new IOException("Not supported yet.");
    }

    /**
     * Copies a file.
     * This method is not yet supported.
     *
     * @param  source   the path to the file to move.
     * @param  target   the path to the target file.
     * @param  options  options specifying how the move should be done
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void move(final Path source, final Path target, final CopyOption... options) throws IOException {
        throw new IOException("Not supported yet.");
    }

    /**
     * Deletes a file.
     * This method is not yet supported.
     *
     * @param  path  the path to the file to delete.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void delete(final Path path) throws IOException {
        throw new IOException("Not supported yet.");
    }

    /**
     * Opens an input stream to read from the path.
     * It is important to close the input stream after usage for avoiding exhaustion of connection pool.
     *
     * @param  path     the path to the file to open.
     * @param  options  options specifying how the file is opened.
     * @return a new input stream for the specified path.
     * @throws UnsupportedOperationException if an unsupported option is specified.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public InputStream newInputStream(final Path path, final OpenOption... options) throws IOException {
        ensureSupported(options);
        final KeyPath kp = toAbsolute(path, true);
        final ResponseInputStream<GetObjectResponse> stream;
        try {
            stream = kp.fs.client().getObject(GetObjectRequest.builder().bucket(kp.bucket).key(kp.key).build());
        } catch (SdkException e) {
            throw failure(path, e);
        }
        return stream;
    }

    /**
     * Returns a seekable byte channel to access a file.
     *
     * @param  path        the path to the file to open.
     * @param  options     options specifying how the file is opened.
     * @param  attributes  an optional list of file attributes to set when creating the file.
     * @return a new seekable byte channel for the specified path.
     * @throws UnsupportedOperationException if an unsupported open option is specified.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options,
            final FileAttribute<?>... attributes) throws IOException
    {
        ensureSupported(options.toArray(OpenOption[]::new));
        return new CachedByteChannel(toAbsolute(path, true));
    }

    /**
     * Ensures that the given array of options does not contain an unsupported option.
     */
    private static void ensureSupported(final OpenOption[] options) {
        for (final OpenOption opt : options) {
            if (opt == StandardOpenOption.APPEND || opt == StandardOpenOption.WRITE) {
                throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnsupportedArgumentValue_1, opt));
            }
        }
    }

    /**
     * Checks the existence, and optionally the accessibility, of a file.
     *
     * @param  path   the path to the file to check.
     * @param  modes  the access modes to check; may have zero elements.
     * @throws NoSuchFileException if a file does not exist (optional specific exception).
     * @throws AccessDeniedException the requested access would be denied.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        final KeyPath kp = toAbsolute(path, false);
        if (!kp.isDirectory || kp.key == null) try {
            final boolean exists;
            if (kp.isDirectory) {
                final Bucket bucket = kp.bucket();
                exists = (bucket != null) && (bucket.creationDate() != null);
            } else {
                final S3Object metadata = kp.metadata();
                exists = (metadata != null) && (metadata.size() != null);
            }
            if (!exists) {
                throw new NoSuchFileException(path.toString());
            }
        } catch (SdkException e) {
            throw failure(path, e);
        }
        for (final AccessMode mode : modes) {
            switch (mode) {
                case READ: break;
                case WRITE:
                case EXECUTE: throw new AccessDeniedException(path.toString());
                default: throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnsupportedArgumentValue_1, mode));
            }
        }
    }

    /**
     * Returns a read-only or updatable view of a set of file attributes.
     *
     * @param   <V>      the view type.
     * @param   path     the path to the file.
     * @param   type     the class of the file attribute view.
     * @param   options  options indicating how symbolic links are handled.
     * @return  a file attribute view of the specified type, or {@code null} if not available.
     */
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        if (type.isAssignableFrom(BasicFileAttributeView.class)) {
            return type.cast(new ObjectAttributes(toAbsolute(path, false)));
        }
        return null;
    }

    /**
     * Reads a file's attributes as a bulk operation.
     *
     * @param   <A>      the {@code BasicFileAttributes} type.
     * @param   path     the path to the file.
     * @param   type     the class of the file attributes to read.
     * @param   options  options indicating how symbolic links are handled.
     * @return  the file attributes.
     * @throws  UnsupportedOperationException if attributes of the given type are not supported.
     * @throws  IOException if an I/O error occurs.
     */
    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        if (type.isAssignableFrom(BasicFileAttributes.class)) {
            return type.cast(new ObjectAttributes(toAbsolute(path, false)).readAttributes());
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Reads a set of file attributes as a bulk operation.
     * See {@linkplain java.nio.file.Files#readAttributes(Path, String, LinkOption...) NIO javadoc}
     * for details about the {@code attributes} argument.
     *
     * @param   path        the path to the file.
     * @param   attributes  the attributes to read.
     * @param   options     options indicating how symbolic links are handled
     * @return  a map of the attributes returned; may be empty. The map's keys
     *          are the attribute names, its values are the attribute values.
     * @throws  IOException if an I/O error occurs.
     *
     * @see java.nio.file.Files#readAttributes(Path, String, LinkOption...)
     */
    @Override
    public Map<String,Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return new ObjectAttributes(toAbsolute(path, false)).toMap(attributes);
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Wraps an Amazon exception into Java I/O exception.
     */
    static IOException failure(final Path path, final SdkException cause) {
        final IOException ex;
        if (cause instanceof NoSuchBucketException) {
            ex = new NoSuchFileException(path.toString());
        } else {
            return new IOException(cause);
        }
        return (IOException) ex.initCause(cause);
    }
}
