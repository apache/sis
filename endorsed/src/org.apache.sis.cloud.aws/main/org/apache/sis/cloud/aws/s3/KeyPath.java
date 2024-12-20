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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.InvalidPathException;
import java.util.Iterator;
import java.util.Objects;
import java.util.NoSuchElementException;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;


/**
 * Wraps a single key for an S3 object as a path.
 * The path contains the bucket as its root.
 *
 * <p>AWS S3 has no concept of file directories.
 * Instead a bucket is more like a big {@link java.util.HashMap} with arbitrary {@link String} keys.
 * Those keys may contain the {@code "/"} character, but AWS S3 gives no special meaning to it.
 * The interpretation of {@link ClientFileSystem#separator} as a path separator is done by this class.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class KeyPath implements Path {
    /**
     * The case-insensitive scheme for the URI represented by this path.
     */
    static final String SCHEME = "S3";

    /**
     * Separator between {@value #SCHEME} and the bucket name.
     */
    private static final String SCHEME_SEPARATOR = "://";

    /**
     * Length of the {@code "S3://"} header.
     */
    private static final int SCHEME_LENGTH = SCHEME.length() + SCHEME_SEPARATOR.length();

    /**
     * The file system that created this path. This file system gives access to the
     * {@link software.amazon.awssdk.services.s3.S3Client} instance from AWS SDK.
     *
     * @see ClientFileSystem#client()
     */
    final ClientFileSystem fs;

    /**
     * Name of the bucket which is the root of this path, or {@code null} if this path is relative.
     * At least one of {@code bucket} and {@link #key} shall be non-null.
     *
     * @see #bucket()
     */
    final String bucket;

    /**
     * The key for locating the S3 object (shall not be empty), or {@code null} if this path is the root.
     * If the key contains {@link ClientFileSystem#separator}, it will be interpreted as a list of path components.
     * However, the separator characters have no special meaning for S3; this is an interpretation added by this wrapper.
     */
    final String key;

    /**
     * Whether this path should be considered as a directory.
     * Amazon S3 has no concept of directories; this is a concept added by this wrapper.
     */
    final boolean isDirectory;

    /**
     * Metadata about the S3 bucket, or {@code null} if none or unknown. This field is not used directly
     * by {@code KeyPath} but is made available as a cache for other classes in this package.
     *
     * @see #bucket()
     */
    private Bucket bucketMetadata;

    /**
     * Metadata about the S3 object, or {@code null} if none or unknown. Always null for directories.
     * This field is not used directly by {@code KeyPath} but is made available as a cache for other
     * classes in this package.
     *
     * @see #metadata()
     */
    private S3Object objectMetadata;

    /**
     * Creates an absolute path for a root defined by a S3 bucket.
     * This is used when listing the roots in a file system.
     *
     * @param fs      the file system which is creating the root.
     * @param bucket  the bucket.
     *
     * @see ClientFileSystem#getRootDirectories()
     */
    KeyPath(final ClientFileSystem fs, final Bucket bucket) {
        this.fs        = fs;
        this.bucket    = bucket.name();
        bucketMetadata = bucket;
        isDirectory    = true;
        key            = null;
    }

    /**
     * Creates an absolute path for an object in the S3 storage.
     * This is used when iterating over the files in a pseudo-directory.
     *
     * <p>When using this constructor, it may happen that the path ends with the {@link ClientFileSystem#separator}
     * character if the S3 object really has that name, and this {@code KeyPath} still have the {@link #isDirectory}
     * flag set to {@code false}.
     * We keep it that way because it describes what is really on the S3 file system, even if confusing.</p>
     *
     * @param root      a path from which to inherit the file system and the root.
     * @param metadata  metadata about the S3 object.
     *
     * @see PathIterator#next()
     */
    KeyPath(final KeyPath root, final S3Object metadata) {
        this(root, metadata.key(), false);
        objectMetadata = metadata;
    }

    /**
     * Creates a new path with the same root as the given path.
     * This is used for deriving root, parent, subpath and resolving path.
     *
     * @param root         a path from which to inherit the file system and the root.
     * @param key          key for locating the S3 object, or {@code null} if this path is the root.
     * @param isDirectory  whether this path should be flagged as a directory.
     */
    KeyPath(final KeyPath root, final String key, final boolean isDirectory) {
        bucketMetadata   = root.bucketMetadata;
        this.bucket      = root.bucket;
        this.fs          = root.fs;
        this.key         = key;
        this.isDirectory = isDirectory;
        assert key == null || !key.isEmpty();
        // Do not copy `objectMetadata` because it is not for the same object.
    }

    /**
     * Creates a relative path from the given key.
     * This is used for filename, subpath and relativizing path.
     *
     * @param fs           the file system that created this path.
     * @param key          key for locating the S3 object.
     * @param isDirectory  whether this path should be flagged as a directory.
     */
    KeyPath(final ClientFileSystem fs, final String key, final boolean isDirectory) {
        this.fs          = fs;
        this.bucket      = null;
        this.key         = key;
        this.isDirectory = isDirectory;
        assert !key.isEmpty();
    }

    /**
     * Creates a new path by parsing the given components.
     * This is used for creating from character strings or URI.
     * This constructor accepts the following strings, where {@code key} can be a path
     * with any number of occurrences of the {@link ClientFileSystem#separator} separator:
     *
     * <ul>
     *   <li>{@code "S3://bucket/file"} (note that {@code "accessKey@bucket"} is not accepted)</li>
     *   <li>{@code "/bucket/file"} (absolute path)</li>
     *   <li>{@code "key"} (relative path)</li>
     * </ul>
     *
     * @param  fs          the file system that created this path.
     * @param  first       the path string or initial part of the path string.
     * @param  more        additional strings to be joined to form the path string.
     * @param  isAbsolute  whether to force the path to an absolute path.
     *                     If {@code false}, will be determined automatically.
     * @throws InvalidPathException if the path uses a protocol other than {@value #SCHEME}.
     */
    KeyPath(final ClientFileSystem fs, final String first, final String[] more, boolean isAbsolute) {
        /*
         * Verify if the path start with "S3://" or "/" prefix. In both cases the path is considered absolute
         * and the prefix is skipped. The `start` variable is the index of the first character after prefix,
         * which means that `start = 0` for relative path and `start > 0` for absolute path.
         */
        int start = 0;
        final int ps = first.indexOf(SCHEME_SEPARATOR);
        if (ps >= 0) {
            if (!first.regionMatches(true, 0, SCHEME, 0, ps)) {
                String reason = Resources.format(Resources.Keys.UnexpectedProtocol_1, first.substring(0, ps));
                throw new InvalidPathException(first, reason, 0);
            }
            start = SCHEME_LENGTH;
            isAbsolute = true;
        } else if (first.isEmpty()) {
            throw emptyPath(first, 0);
        } else if (first.startsWith(fs.separator)) {
            isAbsolute = true;
            start = 1;
        }
        this.fs = fs;       // At this point we have validated that `ClientFileSystem` is suitable for the path.
        /*
         * Now separate the bucket name from the S3 object key. The first path component is the bucket name
         * if and only if the path is absolute. The bucket name is the string before the first '/' character
         * and everything else is the S3 object key. The "everything else" is stored in the `StringBuilder`.
         */
        final int separatorLength = fs.separator.length();
        StringBuilder buffer = null;
        if (isAbsolute) {
            int end = first.indexOf(fs.separator, start);
            if (end < 0) {
                bucket = first.substring(start);
            } else {
                bucket = first.substring(start, end);       // Take characters before the first '/'.
                start  = end + separatorLength;             // Remaining path after the first '/'.
                end    = first.length();
                while (start < end) {
                    if (!first.startsWith(fs.separator, end - separatorLength)) {
                        buffer = new StringBuilder(end - start).append(first, start, end);
                        break;
                    }
                    end -= separatorLength;                 // Skip trailing '/' characters.
                }
            }
            start = first.length();     // Tells that there is nothing more from `start` to append.
            if (bucket.isEmpty()) {
                throw emptyPath(first, (ps >= 0) ? SCHEME_LENGTH : 0);
            }
        } else {
            /*
             * Relative path: no bucket. If there is no more component, as a slight optimization
             * we try to use the `first` string directly without copying to a temporary buffer.
             * We need to make sure that there is no "//" character sequences because they would
             * be interpreted as empty paths, which is illegal in this implementation.
             */
            bucket = null;
            if (more.length == 0) {
                String path = first;
                int end = path.length();
                do path = path.replace(fs.duplicatedSeparator, fs.separator);
                while (end > (end = path.length()));
                if ((end -= separatorLength) < 0) {
                    throw emptyPath(first, 0);
                }
                isDirectory = path.endsWith(fs.separator);
                key = isDirectory ? path.substring(0, end) : path;
                return;
            }
        }
        /*
         * At this point, we finished to parse the `first` component. Some parts of the path may be
         * in the `StringBuilder`. Now append all remaining components. We replace some (but not all)
         * extraneous '/' characters.
         */
        for (final String component : more) {
            final int end = component.length();
            for (int i=0; i<end; i++) {
                if (!component.startsWith(fs.separator, i)) {
                    if (buffer == null) {
                        buffer = new StringBuilder(first.substring(start));
                    }
                    if (buffer.length() != 0) {
                        buffer.append(fs.separator);
                    }
                    buffer.append(component, i, end);
                    break;
                }
            }
        }
        /*
         * At this point, we finished to join components in a S3 object key. Replace all "//" by "/" because
         * the former would be interpreted as an empty path, which is not supported by this implementation.
         * If the path ends with "/", that character needs to be removed as well and the path is flagged as
         * a directory.
         */
        if (buffer != null) {
            int i = buffer.length();
            while ((i = buffer.lastIndexOf(fs.duplicatedSeparator, i)) >= 0) {
                buffer.delete(i, i + separatorLength);
            }
            i = buffer.length() - separatorLength;
            if (i >= 0) {
                isDirectory = CharSequences.regionMatches(buffer, i, fs.separator);
                if (isDirectory) {
                    if (i == 0) {
                        if (bucket == null) {
                            throw emptyPath(first, 0);
                        }
                        key = null;
                        return;
                    }
                    buffer.setLength(i);
                }
                key = buffer.toString();
                return;
            }
        }
        if (bucket == null) {
            throw emptyPath(first, 0);
        }
        isDirectory = true;
        key = null;
    }

    /**
     * Returns the exception to throw for an empty path.
     */
    private static InvalidPathException emptyPath(final String path, final int index) {
        return new InvalidPathException(path, Resources.format(Resources.Keys.EmptyPath), index);
    }

    /**
     * Returns a new path with the same file system as this path.
     */
    private KeyPath newPath(final String other) {
        return new KeyPath(fs, Objects.requireNonNull(other, "other"), CharSequences.EMPTY_ARRAY, false);
    }

    /**
     * Returns metadata about the bucket, or {@code null} if none.
     * Created when first needed, then cached.
     */
    final synchronized Bucket bucket() {
        if (bucketMetadata == null && bucket != null) {
            // TODO: we should get the bucket from `S3Client`.
            // FileService.checkAccess(…) uses the absence of creation date as a sentinal value.
            bucketMetadata = Bucket.builder().name(bucket).build();
        }
        return bucketMetadata;
    }

    /**
     * Returns metadata about the object, or {@code null} if none.
     * Created when first needed, then cached.
     */
    final synchronized S3Object metadata() {
        if (objectMetadata == null && bucket != null && key != null && !isDirectory) {
            for (final S3Object candidate : fs.client().listObjectsV2(request().maxKeys(1).build()).contents()) {
                if (key.equals(candidate.key())) {
                    return objectMetadata = candidate;
                }
            }
            // FileService.checkAccess(…) uses the absence of size as a sentinal value.
            objectMetadata = S3Object.builder().key(key).build();
        }
        return objectMetadata;
    }

    /**
     * Creates a builder for a request to be sent to AWS S3 server. AWS limits the response to 1000 elements.
     * Consequently, this method may need to be invoked more than once in order to get the next elements.
     * For all continuation requests, {@code request.continuationToken(String)} needs to be invoked.
     */
    final ListObjectsV2Request.Builder request() {
        ListObjectsV2Request.Builder request = ListObjectsV2Request.builder().bucket(bucket).delimiter(fs.getSeparator());
        if (key != null) {
            request = request.prefix(isDirectory ? key.concat(fs.separator) : key);
        }
        return request;
    }

    /**
     * Returns the file system that created this object.
     */
    @Override
    public FileSystem getFileSystem() {
        return fs;
    }

    /**
     * Tells whether this path is complete. An absolute path does not need
     * to be combined with other path information in order to locate a file.
     */
    @Override
    public boolean isAbsolute() {
        return bucket != null;
    }

    /**
     * Returns the root component of this path as a {@code Path} object,
     * or {@code null} if this path does not have a root component.
     */
    @Override
    public Path getRoot() {
        if (bucket == null) return null;
        if (key    == null) return this;
        return new KeyPath(this, null, true);
    }

    /**
     * Returns the farthest element from the root in the directory hierarchy.
     * The returned element is a relative path.
     */
    @Override
    public Path getFileName() {
        if (key != null) {
            final int i = key.lastIndexOf(fs.separator);
            if (bucket != null || i >= 0) {
                final String name = (i >= 0) ? key.substring(i + fs.separator.length()) : key;
                return new KeyPath(fs, name, isDirectory);
            }
        }
        return this;
    }

    /**
     * Returns the root (if any) and each element in the path except for the farthest from the root.
     * This method does not eliminate special names such as {@code "."} and {@code ".."}.
     * If this path has no parent, then this method returns {@code null}.
     */
    @Override
    public Path getParent() {
        if (key != null) {
            final int i = key.lastIndexOf(fs.separator);
            if (i >= 0) {
                return new KeyPath(this, key.substring(0, i), true);
            }
            return getRoot();
        }
        return null;
    }

    /**
     * Returns the number of name elements in the path.
     */
    @Override
    public int getNameCount() {
        int count = CharSequences.count(key, fs.separator);
        if (key    != null) count++;
        if (bucket != null) count++;
        return count;
    }

    /**
     * Returns a name element of this path at the given index.
     *
     * @throws IllegalArgumentException if the given index is invalid.
     */
    @Override
    public Path getName(int index) {
        return subpath(index, index + 1);
    }

    /**
     * Returns a relative {@code Path} that is a subsequence of the name elements of this path.
     *
     * @param  beginIndex  index of the first element, inclusive.
     * @param  endIndex    index of the last element, exclusive.
     * @throws IllegalArgumentException if a given index is invalid.
     */
    @Override
    public Path subpath(final int beginIndex, final int endIndex) {
        int count = endIndex - beginIndex;
        if (beginIndex >= 0 && count > 0) {
            int skip = beginIndex;
            boolean includeRoot = false;
            if (bucket != null) {
                includeRoot = (beginIndex == 0);
                if (includeRoot) {
                    if (endIndex == 1) {
                        return getRoot();
                    }
                    count--;
                }
                skip--;
            }
            final int separatorLength = fs.separator.length();
search:     if (key != null) {
                int start = 0;
                while (--skip >= 0) {
                    start = key.indexOf(fs.separator, start);
                    if (start < 0) break search;
                    start += separatorLength;
                }
                int stop = start;
                if (--count >= 0) {
                    stop = key.indexOf(fs.separator, start);
                    while (--count >= 0) {
                        if (stop < 0) break search;
                        stop = key.indexOf(fs.separator, stop + separatorLength);
                    }
                }
                boolean isParent = (stop >= 0);
                if (!isParent && beginIndex == 0) {
                    assert endIndex == getNameCount() : endIndex;
                    return this;
                }
                final String part = isParent ? key.substring(start, stop) : key.substring(start);
                isParent |= isDirectory;
                if (includeRoot) {
                    return new KeyPath(this, part, isParent);
                } else {
                    return new KeyPath(fs, part, isParent);
                }
            }
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalRange_2, beginIndex, endIndex));
    }

    /**
     * Comparison modes for {@link #compare(Object, int)}.
     */
    private static final int EQUALS = 0, STARTS_WITH = 1, ENDS_WITH = 2;

    /**
     * Implementation of {@link #startsWith(Path)}, {@link #endsWith(Path)} and {@link #equals(Object)} methods.
     * This method verifies that the two paths are of the same class, have the same file system, the same root
     * and finally that their key is either null, or both non-null and satisfying the given condition.
     *
     * @param  other  the other object to compare with this path.
     * @param  mode   one of {@link #EQUALS}, {@link #STARTS_WITH} or {@link #ENDS_WITH} constants.
     * @return whether the comparison of {@code this} and {@code other} passes all conditions.
     */
    private boolean compare(final Object other, int mode) {
        if (other instanceof KeyPath) {
            final KeyPath part = (KeyPath) other;
            if (fs.equals(part.fs)) {
                if (mode == ENDS_WITH && part.bucket != null) {
                    mode = EQUALS;
                }
                if (mode == ENDS_WITH || Objects.equals(bucket, part.bucket)) {
                    if (key == null) {
                        return part.key == null;
                    } else if (part.key != null) {
                        switch (mode) {
                            case EQUALS: {
                                return key.equals(part.key);
                            }
                            case STARTS_WITH: {
                                if (key.startsWith(part.key)) {
                                    final int end = part.key.length();
                                    return end == key.length() || key.startsWith(fs.separator, end);
                                }
                                break;
                            }
                            case ENDS_WITH: {
                                if (key.endsWith(part.key)) {
                                    final int start = key.length() - part.key.length();
                                    return start == 0 || key.startsWith(fs.separator, start - fs.separator.length());
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the given path has the same file system as this path, the same root (possibly none),
     * and a key which is a prefix of this path key. The prefix must be complete component name.
     * For example, {@code "foo/b"} is <em>not</em> a prefix of {@code "foo/bar"}.
     */
    @Override
    public boolean startsWith(final Path other) {
        return compare(other, STARTS_WITH);
    }

    /**
     * Delegates to {@link #startsWith(Path)}.
     */
    @Override
    public boolean startsWith(String other) {
        return startsWith(newPath(other));
    }

    /**
     * Returns {@code true} if the given path has the same file system as this path and a key which is
     * a suffix of this path key. The suffix must be complete component name. For example, {@code "oo/bar"}
     * is <em>not</em> a suffix of {@code "foo/bar"}.
     */
    @Override
    public boolean endsWith(final Path other) {
        return compare(other, ENDS_WITH);
    }

    /**
     * Delegates to {@link #endsWith(Path)}.
     */
    @Override
    public boolean endsWith(String other) {
        return endsWith(newPath(other));
    }

    /**
     * Returns a path that is this path with redundant name elements eliminated.
     * Current implementation does nothing because S3 key names are not paths,
     * so the {@code "."} and {@code ".."} characters have no special meaning.
     */
    @Override
    public Path normalize() {
        return this;
    }

    /**
     * Resolves the given path against this path. If the given path is absolute, then it is returned.
     * Otherwise this method returns the concatenation of this path with the given path.
     */
    @Override
    public Path resolve(final Path other) {
        if (other.isAbsolute()) {
            return other;
        }
        String part = other.normalize().toString();
        if (part.isEmpty()) {
            return this;
        }
        part = part.replace(other.getFileSystem().getSeparator(), fs.getSeparator());
        if (key != null) {      // If non-null, shall not be empty.
            if (key.endsWith(fs.separator) || part.startsWith(fs.separator)) {
                part = key + part;
            } else {
                part = key + fs.separator + part;
            }
        }
        return new KeyPath(this, part, (other instanceof KeyPath) && ((KeyPath) other).isDirectory);
    }

    /**
     * Delegates to {@link #resolve(Path)}.
     */
    @Override
    public Path resolve(final String other) {
        return resolve(newPath(other));
    }

    /**
     * @todo Remove on JDK17 or replace by optimized implementation.
     */
    @Override
    public Path resolveSibling(final Path other) {
        Objects.requireNonNull(other);
        final Path parent = getParent();
        return (parent == null) ? other : parent.resolve(other);
    }

    /**
     * Delegates to {@link #resolveSibling(Path)}.
     */
    @Override
    public Path resolveSibling(String other) {
        return resolveSibling(newPath(other));
    }

    /**
     * Attempts to construct a relative path that when resolved against this path,
     * yields a path that locates the same file as the given path.
     * The two paths must either has no root or the same root.
     *
     * @throws IllegalArgumentException if the other path cannot be relativized against this path.
     */
    @Override
    public Path relativize(final Path other) {
        if (other instanceof KeyPath) {
            final KeyPath kp = (KeyPath) other;
            if (kp.startsWith(this) && key != null) {
                final String suffix = kp.key.substring(key.length() + fs.separator.length());
                if (!suffix.isEmpty()) {
                    return new KeyPath(kp.fs, suffix, kp.isDirectory);
                }
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns an URI with the {@value #SCHEME} scheme if the path is absolute, or a relative URI otherwise.
     *
     * <p>Note: {@link Path#toUri()} specification mandate an absolute URI.
     * But we cannot provide an absolute URI if this path is not already absolute.</p>
     *
     * @see #toString()
     */
    @Override
    public URI toUri() {
        String path = key;
        if (path != null) {
            final StringBuilder sb = new StringBuilder(path.length() + 2).append('/').append(path);
            if (isDirectory) sb.append('/');
            path = sb.toString();
        }
        try {
            return new URI(SCHEME, fs.accessKey, bucket, -1, path, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Returns a string representation of this path.
     */
    @Override
    public String toString() {
        if (bucket == null && !isDirectory) {
            return key;
        }
        final StringBuilder sb = new StringBuilder();
        if (bucket != null) {
            sb.append(SCHEME).append(SCHEME_SEPARATOR);
            if (fs.accessKey != null) {
                sb.append(fs.accessKey).append('@');
            }
            sb.append(bucket);
        }
        if (key != null) {
            if (bucket != null) {
                sb.append(fs.separator);
            }
            sb.append(key);
        }
        return sb.toString();
    }

    /**
     * Returns this path if it is already absolute.
     * Current implementation cannot change a relative path to an absolute path.
     */
    @Override
    public Path toAbsolutePath() {
        if (bucket != null) return this;
        throw new UnsupportedOperationException(Resources.format(Resources.Keys.CanNotChangeToAbsolutePath));
    }

    /**
     * Returns this path as an absolute path if possible.
     * Current implementation cannot change a relative path to an absolute path,
     * and does not verify if an object exists in the S3 bucket for the {@linkplain #key}.
     */
    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        if (bucket != null) return this;
        throw new IOException(Resources.format(Resources.Keys.CanNotChangeToAbsolutePath));
    }

    /**
     * @todo Remove on JDK17.
     */
    @Override
    public File toFile() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) {
        throw new UnsupportedOperationException();
    }

    /**
     * @todo Remove on JDK17.
     */
    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an iterator over the name elements of this path.
     */
    @Override
    public Iterator<Path> iterator() {
        return new Iter();
    }

    /**
     * Implementation of the iterator returned by {@link #iterator()}.
     */
    private final class Iter implements Iterator<Path> {
        /**
         * Index of the first character to use in the next path, or -1 if iteration is finished.
         * The sequence of characters are from {@code start} inclusive to {@link #end} exclusive.
         */
        private int start;

        /**
         * Index after the last character to use in the next path, or -1 for using the remaining of the key.
         * By convention, a value of 0 means that the next element should be the root.
         */
        private int end;

        /**
         * Creates a new iterator.
         */
        Iter() {
            if (bucket == null) {
                end = key.indexOf(fs.separator);        // If `bucket` is null, then `key` shall be non-null.
            }
        }

        /**
         * Returns {@code true} if there is more elements to iterator.
         */
        @Override public boolean hasNext() {
            return (start >= 0);
        }

        /**
         * Returns the next path element.
         */
        @Override public Path next() {
            if (end == 0) {
                // Set `start` and `end` to the values needed for next element (after this one).
                if (key != null) {
                    end = key.indexOf(fs.separator);
                } else {
                    start = -1;
                }
                return getRoot();
            }
            if (start >= 0) {
                final String name;
                final boolean dir;
                if (end >= 0) {
                    name  = key.substring(start, end);
                    dir   = true;
                    start = end + fs.separator.length();
                    end   = key.indexOf(fs.separator, start);
                } else {
                    name  = key.substring(start);
                    dir   = isDirectory;
                    start = -1;
                }
                return new KeyPath(fs, name, dir);
            }
            throw new NoSuchElementException();
        }
    }

    /**
     * Compares two paths lexicographically, with implementation-dependent criterion.
     *
     * @param   other  the path compared to this path.
     * @throws  ClassCastException if the paths are associated with different providers.
     */
    @Override
    public int compareTo(final Path other) {
        final KeyPath kp = (KeyPath) other;
        boolean present = (bucket != null);
        if (present != (kp.bucket != null)) {
            return present ? -1 : +1;           // Paths with root are first.
        }
        if (present) {
            final int c = bucket.compareTo(kp.bucket);
            if (c != 0) return c;
        }
        present = (key != null);
        if (present != (kp.key != null)) {
            return present ? +1 : -1;           // Paths without empty key (considered as empty key) are first.
        }
        if (present) {
            final int c = key.compareTo(kp.key);
            if (c != 0) return c;
        }
        if (isDirectory == kp.isDirectory) return 0;
        return isDirectory ? -1 : +1;           // Directories sorted before files.
    }

    /**
     * Compares this path with the given object for equality. The given object is considered equal
     * if it is an instance of the same class, has the same file system, equal root and equal key.
     */
    @Override
    public boolean equals(final Object obj) {
        return compare(obj, EQUALS) && isDirectory == ((KeyPath) obj).isDirectory;
    }

    /**
     * Returns a hash code value for this path.
     */
    @Override
    public int hashCode() {
        return Objects.hash(fs, bucket, key, isDirectory);
    }
}
