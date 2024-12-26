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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;


/**
 * A provider of file system services as wrappers around Amazon Simple Storage Service (AWS S3).
 * This provider accepts URIs of the following forms:
 *
 * <ul>
 *   <li>{@code S3://bucket/file}</li>
 *   <li>{@code S3://accessKey@bucket/file} (password not allowed)</li>
 * </ul>
 *
 * "Files" are S3 keys interpreted as paths with components separated by the {@code '/'} separator.
 * The password and the region can be specified at {@linkplain #newFileSystem file system initialization time}.
 * The endpoint (e.g. {@code "s3.eu-central-1.amazonaws.com"}) shall <em>not</em> be specified in the <abbr>URI</abbr>.
 * In particular the region ({@code "eu-central-1"} in the above example) can depend on the server location
 * instead of the data to access, and can be a global configuration for the server.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.2
 */
public class FileService extends FileSystemProvider {
    /**
     * An arbitrary string used as key in the {@link #fileSystems} map
     * when the user did not specified explicitly an access key.
     * In such case, the default mechanism documented in AWS SDK is used.
     * In preference order:
     *
     * <ul>
     *   <li>{@code aws.accessKeyId} and {@code aws.secretAccessKey} Java system properties.</li>
     *   <li>{@code AWS_ACCESS_KEY_ID} and {@code AWS_SECRET_ACCESS_KEY} environment variables.</li>
     *   <li>{@code ~/.aws/credentials} or {@code ~/.aws/conﬁg} files.</li>
     * </ul>
     */
    private static final String DEFAULT_ACCESS_KEY = "";

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
     * The property for the secret access key (password).
     * Values shall should be instances of {@link Region} or
     * strings {@linkplain Region#of(String) convertible} to region.
     * If not specified, the AWS SDK default mechanism searches for the first of the following:
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
     * The property for the name-separator characters.
     * The default value is "/" for simulating Unix paths.
     * The separator must contain at least one character.
     * They usually have only one character, but longer separators are accepted.
     * The separator can contain any characters which are valid in a S3 object name.
     */
    public static final String SEPARATOR = "separator";

    /**
     * All file systems created by this provider. Keys are AWS S3 access keys.
     */
    private final ConcurrentMap<String, ClientFileSystem> fileSystems;

    /**
     * Creates a new provider of file systems for Amazon S3.
     */
    public FileService() {
        fileSystems = new ConcurrentHashMap<>();
    }

    /**
     * Returns the URI scheme that identifies this provider, which is {@code "S3"}.
     *
     * @return the {@code "S3"} URI scheme.
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
     * </ul>
     *
     * @param  uri         a <abbr>URI</abbr> of the form {@code "s3://accessKey@bucket/file"}.
     * @param  properties  properties to configure the file system, or {@code null} if none.
     * @return the new file system.
     * @throws IllegalArgumentException if the URI or the map contains invalid values.
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
        final String separator = Containers.property(properties, SEPARATOR, String.class);
        final Region region;
        Object value = properties.get(AWS_REGION);
        if (value instanceof String) {
            region = Region.of((String) value);
        } else {
            region = Containers.property(properties, AWS_REGION, Region.class);
        }
        final class Creator implements Function<String, ClientFileSystem> {
            /** Identifies if a new file system is created. */ boolean created;

            /** Invoked if the map does not already contains the file system. */
            @Override public ClientFileSystem apply(final String key) {
                created = true;
                return new ClientFileSystem(FileService.this, region, key, secret, separator);
            }
        }
        final Creator c = new Creator();
        final ClientFileSystem fs = fileSystems.computeIfAbsent(accessKey, c);
        if (c.created) {
            return fs;
        }
        throw new FileSystemAlreadyExistsException(Resources.format(Resources.Keys.FileSystemInitialized_2, 1, accessKey));
    }

    /**
     * Removes the given file system from the cache.
     * This method is invoked after the file system has been closed.
     */
    final void dispose(String identifier) {
        if (identifier == null) {
            identifier = DEFAULT_ACCESS_KEY;
        }
        fileSystems.remove(identifier);
    }

    /**
     * Returns the file system associated to the {@link #DEFAULT_ACCESS_KEY}.
     *
     * @throws SdkException if the file system cannot be created.
     */
    private ClientFileSystem getDefaultFileSystem() {
        return fileSystems.computeIfAbsent(DEFAULT_ACCESS_KEY, (key) -> new ClientFileSystem(this, S3Client.create()));
    }

    /**
     * Returns a reference to a file system that was created by the {@link #newFileSystem(URI, Map)} method.
     * If the file system has not been created or has been closed,
     * then this method throws {@link FileSystemNotFoundException}.
     *
     * @param  uri  a <abbr>URI</abbr> of the form {@code "s3://accessKey@bucket/file"}.
     * @return the file system previously created by {@link #newFileSystem(URI, Map)}.
     * @throws IllegalArgumentException if the URI is not supported by this provider.
     * @throws FileSystemNotFoundException if the file system does not exist or has been closed.
     */
    @Override
    public FileSystem getFileSystem(final URI uri) {
        final String accessKey = getAccessKey(uri);
        if (accessKey == null) {
            return getDefaultFileSystem();
        }
        final ClientFileSystem fs = fileSystems.get(accessKey);
        if (fs != null) {
            return fs;
        }
        throw new FileSystemNotFoundException(Resources.format(Resources.Keys.FileSystemInitialized_2, 0, accessKey));
    }

    /**
     * Return a {@code Path} object by converting the given {@code URI}.
     * The resulting {@code Path} is associated with a {@link FileSystem}
     * that already exists or is constructed automatically.
     *
     * @param  uri  a <abbr>URI</abbr> of the form {@code "s3://accessKey@bucket/file"}.
     * @return the resulting {@code Path}.
     * @throws IllegalArgumentException if the URI is not supported by this provider.
     * @throws FileSystemNotFoundException if the file system does not exist and cannot be created automatically.
     */
    @Override
    public Path getPath(final URI uri) {
        final String accessKey = getAccessKey(uri);
        final ClientFileSystem fs;
        if (accessKey == null) {
            fs = getDefaultFileSystem();
        } else {
            // TODO: we may need a way to get password here.
            fs = fileSystems.computeIfAbsent(accessKey, (key) -> new ClientFileSystem(FileService.this, null, key, null, null));
        }
        String host = uri.getHost();
        if (host == null) {
            /*
             * The host is null if the authority contains characters that are invalid for a host name.
             * For example if the host contains underscore character ('_'), then it is considered invalid.
             * We could use the authority instead, but that authority may contain a user name, port number, etc.
             * Current version do not try to parse that string.
             */
            host = uri.getAuthority();
            if (host == null) host = uri.toString();
            throw new IllegalArgumentException(Resources.format(Resources.Keys.InvalidBucketName_1, host));
        }
        final String path = uri.getPath();
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
