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

import java.util.Set;
import java.util.Collections;
import java.util.regex.PatternSyntaxException;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.InvalidPathException;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.internal.shared.Strings;


/**
 * File system wrapping a S3 client. This class wraps an Amazon {@link S3Client},
 * which is kept ready-to-use until the file system is {@linkplain #close closed}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ClientFileSystem extends FileSystem {
    /**
     * The default separator.
     */
    static final String DEFAULT_SEPARATOR = "/";

    /**
     * The AWS S3 access key, or {@code null} if none.
     * Also used as key of this file system in the {@link FileService#fileSystems} map.
     */
    final String accessKey;

    /**
     * The provider of this file system.
     */
    private final FileService provider;

    /**
     * The service client for accessing Amazon S3, or {@code null} if this file system is closed.
     * Note that AWS SDK objects are thread-safe.
     */
    private volatile S3Client client;

    /**
     * The character used as a separator in path component.
     * This is usually "/".
     */
    final String separator;

    /**
     * The {@link #separator} repeated twice. Used for detecting empty paths.
     */
    final String duplicatedSeparator;

    /**
     * Creates a file system with default credential and default separator.
     */
    ClientFileSystem(final FileService provider, final S3Client client) {
        this.provider  = provider;
        this.client    = client;
        this.accessKey = null;
        this.separator = DEFAULT_SEPARATOR;
        duplicatedSeparator = DEFAULT_SEPARATOR + DEFAULT_SEPARATOR;
    }

    /**
     * Creates a new file system with the specified credential.
     *
     * @param provider    the provider creating this file system.
     * @param region      the AWS region, or {@code null} for default.
     * @param accessKey   the AWS S3 access key for this file system.
     * @param secret      the password.
     * @param separator   the separator in paths, or {@code null} for the default value.
     */
    ClientFileSystem(final FileService provider, final Region region, final String accessKey, final String secret,
                     String separator)
    {
        if (separator == null) {
            separator = DEFAULT_SEPARATOR;
        }
        ArgumentChecks.ensureNonEmpty("separator", separator);
        this.provider  = provider;
        this.accessKey = accessKey;
        S3ClientBuilder builder = S3Client.builder().credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secret)));
        if (region != null) {
            builder = builder.region(region);
        }
        client = builder.build();
        this.separator = separator;
        duplicatedSeparator = separator.concat(separator);
    }

    /**
     * Returns the provider that created this file system.
     */
    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    /**
     * Returns the object from AWS SDK for accessing S3.
     *
     * @throws ClosedFileSystemException if this file system has been closed.
     */
    final S3Client client() {
        final S3Client c = client;
        if (c != null) return c;
        throw new ClosedFileSystemException();
    }

    /**
     * Closes all channels and other closeable objects associated with the file system and releases resources.
     * If the file system is already closed then invoking this method has no effect.
     */
    @Override
    public synchronized void close() throws IOException {
        final S3Client c = client;
        client = null;
        if (c != null) try {
            provider.dispose(accessKey);
            c.close();
        } catch (SdkException e) {
            throw new IOException(e);
        }
    }

    /**
     * Tells whether this file system is open. The value become {@code false} after a call to {@link #close()}.
     */
    @Override
    public boolean isOpen() {
        return client != null;
    }

    /**
     * Tells whether this file system allows only read-only access to its file stores.
     * Current implementation returns {@code true}. Write support may be added in a future version.
     *
     * @see BucketStore#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    /**
     * Returns the name separator used to separate names in a path string.
     */
    @Override
    public String getSeparator() {
        return separator;
    }

    /**
     * Returns an object to iterate over the paths of the root directories.
     * Each S3 bucket is considered as a root directory.
     * The order of the elements is not defined.
     */
    @Override
    public Iterable<Path> getRootDirectories() {
        return Containers.derivedList(client().listBuckets().buckets(), (root) -> new KeyPath(this, root));
    }

    /**
     * Returns an object to iterate over the underlying file stores.
     * Each S3 bucket is considered as a file store.
     * The order of the elements is not defined.
     *
     * @see FileService#getFileStore(Path)
     */
    @Override
    public Iterable<FileStore> getFileStores() {
        return Containers.derivedList(client().listBuckets().buckets(), BucketStore::new);
    }

    /**
     * Returns the names of file attribute views supported by this file system.
     * The {@link BasicFileAttributeView} is mandatory.
     *
     * @see BucketStore#supportsFileAttributeView(String)
     */
    @Override
    public Set<String> supportedFileAttributeViews() {
        return Collections.singleton(ObjectAttributes.NAME);
    }

    /**
     * Converts a path string to a {@link KeyPath}.
     * Paths starting with {@code '/'} character are considered as absolute paths.
     * Paths ending with {@code '/'} character are considered as directories.
     * Empty paths are not allowed.
     *
     * @param  first  the path string or initial part of the path string.
     * @param  more   additional strings to be joined to form the path string.
     * @return the resulting {@link KeyPath}.
     * @throws InvalidPathException if the path string cannot be converted.
     */
    @Override
    public Path getPath(final String first, final String... more) {
        return new KeyPath(this, first, more, false);
    }

    /**
     * Returns a filter that matches {@link KeyPath} string representation against a given pattern.
     *
     * @param  syntaxAndPattern  filtering criteria of the {@code syntax:pattern}.
     * @return a {@link KeyPath} matcher for the given pattern.
     * @throws PatternSyntaxException if the pattern is invalid.
     * @throws UnsupportedOperationException if the pattern syntax is not known to this implementation.
     */
    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        return new KeyPathMatcher(syntaxAndPattern, separator);
    }

    /**
     * Unsupported, this wrapper does not provide a lookup service yet.
     */
    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported, this wrapper does not provide a watch service yet.
     */
    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a string representation of this file system for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "accessKey", accessKey);
    }
}
