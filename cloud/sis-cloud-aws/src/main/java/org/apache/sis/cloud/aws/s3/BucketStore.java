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

import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import software.amazon.awssdk.services.s3.model.Bucket;


/**
 * Wraps a single Amazon {@link Bucket} as a NIO {@link FileStore}.
 * Instances of this class are not cached because this in only a thin wrapper.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class BucketStore extends FileStore {
    /**
     * The Amazon bucket to wrap as a file store.
     */
    private final Bucket bucket;

    /**
     * Creates a new store wrapping the given Amazon bucket.
     */
    BucketStore(final Bucket bucket) {
        this.bucket = bucket;
    }

    /**
     * Returns the name of the bucket.
     */
    @Override
    public String name() {
        return bucket.name();
    }

    /**
     * Returns implementation-specific indication of the type of this file store.
     */
    @Override
    public String type() {
        return "Amazon S3 bucket";
    }

    /**
     * Tells whether this file store is read-only.
     * Current implementation returns {@code true}.
     * Write support may be added in a future version.
     *
     * @see ClientFileSystem#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    /**
     * Returns the size (in bytes) of the file store.
     * Current implementation returns {@link Long#MAX_VALUE} because the actual size is unknown.
     */
    @Override
    public long getTotalSpace() {
        return Long.MAX_VALUE;
    }

    /**
     * Returns an estimation of the number of bytes available to this Java virtual machine on the file store.
     * Current implementation returns {@link Long#MAX_VALUE} because the actual size is unknown.
     */
    @Override
    public long getUsableSpace() {
        return Long.MAX_VALUE;
    }

    /**
     * Returns an estimation of the number of unallocated bytes in the file store.
     * Current implementation returns {@link Long#MAX_VALUE} because the actual size is unknown.
     */
    @Override
    public long getUnallocatedSpace() {
        return Long.MAX_VALUE;
    }

    /**
     * Tells whether or not this file store supports the file attributes identified by the given file attribute view.
     * Current version does not support any attribute other than the mandatory basic set.
     */
    @Override
    public boolean supportsFileAttributeView(final Class<? extends FileAttributeView> type) {
        return BasicFileAttributeView.class.isAssignableFrom(type);
    }

    /**
     * Tells whether or not this file store supports the file attributes identified by the given file attribute view.
     * Current version does not support any attribute other than the mandatory basic set.
     *
     * @see ClientFileSystem#supportedFileAttributeViews()
     */
    @Override
    public boolean supportsFileAttributeView(final String name) {
        return ObjectAttributes.NAME.equalsIgnoreCase(name);
    }

    /**
     * Returns an attribute of the given type. This is a type-safe version of {@link #getAttribute(String)}.
     * The given type should be a class for which {@link #supportsFileAttributeView(Class)} returned {@code true}.
     *
     * @param  type  the class of the attribute view to get.
     * @return the attribute view of the specified type, or {@code null} if the attribute view is not available.
     */
    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(final Class<V> type) {
        return null;
    }

    /**
     * Reads the value of a file store attribute.
     *
     * @param  attribute  the attribute to read in a "view:attribute-name" syntax (e.g. {@code "zfs:compression"}).
     * @return the attribute value ({@code null} is a valid value).
     * @throws UnsupportedOperationException if the attribute value is not available.
     */
    @Override
    public Object getAttribute(final String attribute) {
        throw new UnsupportedOperationException();
    }

    /**
     * Compares this file store with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof BucketStore) && bucket.equals(((BucketStore) obj).bucket);
    }

    /**
     * Returns a hash code value for this file store.
     */
    @Override
    public int hashCode() {
        return ~bucket.hashCode();
    }

    /**
     * Returns a string representation of the bucket.
     */
    @Override
    public String toString() {
        return bucket.toString();
    }
}
