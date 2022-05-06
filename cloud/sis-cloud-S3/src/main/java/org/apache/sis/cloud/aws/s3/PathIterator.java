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

import java.util.Iterator;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.ClosedDirectoryStreamException;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;


/**
 * Iterator over the objects in a S3 buckets. Each object if given as a {@link KeyPath}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class PathIterator implements DirectoryStream<Path>, Iterator<Path> {
    /**
     * The directory to scan.
     */
    private final KeyPath directory;

    /**
     * The filter to apply. Shall never be null.
     */
    private final DirectoryStream.Filter<? super Path> filter;

    /**
     * The S3 response to the request for entries, or {@code null} if the directory stream has been closed.
     */
    private ListObjectsV2Response response;

    /**
     * Iterator over prefixes to consider as directories, or {@code null} if that iteration is finished.
     * After iteration on directories is finished, iteration continues with {@link #contents}.
     */
    private Iterator<CommonPrefix> directories;

    /**
     * Iterator over the S3 objects in the response, or {@code null} if iteration did not started.
     */
    private Iterator<S3Object> contents;

    /**
     * The next path to return, or {@code null} if {@link #hasNext()} has not been invoked yet.
     */
    private KeyPath next;

    /**
     * A temporary buffer for building the {@link KeyPath#key} values.
     * The characters for the parent directory are pre-filled and should not be modified.
     */
    private final StringBuilder buffer;

    /**
     * Number of characters for the parent directory in the {@link #buffer}.
     */
    private final int parentLength;

    /**
     * Creates a new iterator over directory entries.
     */
    PathIterator(final KeyPath directory, DirectoryStream.Filter<? super Path> filter) throws SdkException {
        this.directory = directory;
        this.filter    = filter;
        this.response  = directory.fs.client().listObjectsV2(directory.request().build());
        this.buffer    = new StringBuilder();
        if (directory.key != null) {
            buffer.append(directory.key).append(directory.fs.separator);
        }
        parentLength = buffer.length();
    }

    /**
     * Returns the iterator over directory entries.
     * As specified by {@link DirectoryStream} contract, this method can be invoked only once.
     */
    @Override
    public Iterator<Path> iterator() {
        if (response == null) throw new ClosedDirectoryStreamException();
        if (contents != null) throw new IllegalStateException();
        directories = response.commonPrefixes().iterator();
        contents = response.contents().iterator();
        return this;
    }

    /**
     * Initializes {@link #next} to the {@link KeyPath} instance for the next directory.
     *
     * @return whether the path has been created.
     */
    private boolean nextDirectory() {
        while (directories.hasNext()) {
            String path = directories.next().prefix();
            int length = path.length();
            if (length > 0) {
                if (path.endsWith(directory.fs.separator)) {
                    if (--length == 0) {
                        continue;
                    }
                }
                path = buffer.append(path, 0, length).toString();
                buffer.setLength(parentLength);
                next = new KeyPath(directory, path, true);
                return true;
            }
        }
        directories = null;
        return false;
    }

    /**
     * Returns {@code true} if there is more path elements to return.
     */
    @Override
    public boolean hasNext() throws DirectoryIteratorException {
        if (response == null) {
            throw new ClosedDirectoryStreamException();
        }
        if (next != null) {
            return true;
        }
        /*
         * Iterate over all directories first. After the iteration over directories
         * is finished, we continue with the iteration over the actual S3 objects.
         * Some paths may be discarded if a filter has been specified.
         */
        try {
verify:     do if (directories == null || !nextDirectory()) {
                while (!contents.hasNext()) try {
                    if (!response.isTruncated()) {
                        next = null;
                        return false;
                    }
                    /*
                     * If iteration is finished but the server truncated the result,
                     * ask for the next bunch of data.
                     */
                    final String token = response.nextContinuationToken();
                    response    = directory.fs.client().listObjectsV2(directory.request().continuationToken(token).build());
                    directories = response.commonPrefixes().iterator();
                    contents    = response.contents().iterator();
                    if (nextDirectory()) continue verify;           // Will test if filter accepts the path.
                } catch (SdkException e) {
                    throw FileService.failure(directory, e);
                }
                final S3Object object = contents.next();
                next = new KeyPath(directory, object);
            } while (!accept());
        } catch (IOException e) {
            throw new DirectoryIteratorException(e);
        }
        return true;
    }

    /**
     * Returns whether the given path can be accepted. We need to exclude empty paths (not supported by our wrappers)
     * and the path with the same name than {@link #directory} (for avoiding never-empty loop if an S3 object of that
     * name exists). User-specified filter is tested last.
     */
    private boolean accept() throws IOException {
        final String path = next.key;
        if (path != null) {
            final String separator = directory.fs.separator;
            final int separatorLength = separator.length();
            int last = path.length();
            do if ((last -= separatorLength) < 0) return false;
            while (path.startsWith(separator, last));
            if (path.regionMatches(0, directory.key, 0, last + separatorLength)) {
                return false;
            }
        }
        return filter.accept(next);
    }

    /**
     * Returns the next path element.
     */
    @Override
    public Path next() {
        if (next == null && !hasNext()) {
            throw new NoSuchElementException();
        }
        Path p = next;
        next = null;
        return p;
    }

    /**
     * Closes the iterator.
     */
    @Override
    public void close() throws IOException {
        next        = null;
        response    = null;
        directories = null;
        contents    = Collections.emptyIterator();
    }
}
