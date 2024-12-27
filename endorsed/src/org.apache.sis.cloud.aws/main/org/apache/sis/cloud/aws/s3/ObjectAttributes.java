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
import java.util.HashMap;
import java.time.Instant;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.BasicFileAttributeView;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.core.exception.SdkException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;


/**
 * Basic attributes (creation time, is directory, …) for an Amazon S3 object.
 * The basic attributes are mandatory according NIO specification.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ObjectAttributes implements BasicFileAttributeView {
    /**
     * The name of this set of attributes.
     * We currently support only the basic set.
     */
    static final String NAME = "basic";

    /**
     * The path for which to provide attributes.
     */
    private final KeyPath path;

    /**
     * Creates a new set of attributes.
     */
    ObjectAttributes(final KeyPath path) {
        this.path = path;
    }

    /**
     * Returns the name of this attribute view, which is fixed to {@value #NAME}.
     */
    @Override
    public String name() {
        return NAME;
    }

    /**
     * Reads the basic file attributes as a bulk operation.
     * The values may change between different invocations of this method.
     */
    @Override
    public BasicFileAttributes readAttributes() throws IOException {
        try {
            final S3Object metadata = path.metadata();
            if (metadata != null) {
                return new Snapshot(path.bucket(), metadata);
            }
            if (path.isDirectory) {
                final Bucket bucket = path.bucket();
                if (bucket != null) {
                    return new Snapshot(bucket);
                }
            }
        } catch (SdkException e) {
            throw FileService.failure(path, e);
        }
        throw new IOException(Resources.format(Resources.Keys.MustBeAbsolutePath));
    }

    /**
     * Returns the attributes as a map.
     *
     * @see java.nio.file.Files#readAttributes(Path, String, java.nio.file.LinkOption...)
     */
    final Map<String,Object> toMap(final String attributes) throws IOException {
        return ((Snapshot) readAttributes()).toMap(attributes);
    }


    /**
     * Updates the file's timestamp attributes. If any argument value is {@code null},
     * the corresponding time stamp is not changed.
     */
    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * A snapshot of the information provided by the AWS {@link Bucket} at the time this method is invoked.
     */
    private static final class Snapshot implements BasicFileAttributes {
        /**
         * Creation time or modification time of the S3 object. Should not be null.
         */
        private final Instant creationTime, lastModifiedTime;

        /**
         * The size (in bytes) of the file, or 0 if the object does not exist
         * (including the case of pseudo-directory).
         */
        private long size;

        /**
         * Whether this object is considered a directory.
         */
        private final boolean isDirectory;

        /**
         * An object that uniquely identifies the file, or {@code null} if none.
         */
        private String fileKey;

        /**
         * Creates a new set of attributes with the values available at the time this constructor is invoked.
         *
         * @param  bucket  metadata about the root. Cannot be null.
         */
        Snapshot(final Bucket bucket) {
            creationTime     = orDefault(bucket.creationDate());
            lastModifiedTime = creationTime;
            isDirectory      = true;
        }

        /**
         * Creates a new set of attributes with the values available at the time this constructor is invoked.
         *
         * @param  bucket  metadata about the root, or {@code null} if none.
         * @param  object  metadata about the object. Cannot be null.
         */
        Snapshot(final Bucket bucket, final S3Object object) {
            Instant t;
            lastModifiedTime = orDefault(object.lastModified());
            creationTime     = (bucket != null && (t = bucket.creationDate()) != null) ? t : lastModifiedTime;
            fileKey          = object.key();
            isDirectory      = false;
            Long s = object.size();
            if (s != null) size = s;
        }

        /**
         * Returns the given instant if non-null, or a default value otherwise.
         * This is used because {@link BasicFileAttributes} does not seem to allow null values.
         */
        private static Instant orDefault(final Instant time) {
            return (time != null) ? time : Instant.now();
        }

        /**
         * Returns the creation time.
         */
        @Override
        public FileTime creationTime() {
            return FileTime.from(creationTime);
        }

        /**
         * Returns the time of last modification.
         * If not supported, fallbacks on creation time.
         */
        @Override
        public FileTime lastModifiedTime() {
            return FileTime.from(lastModifiedTime);
        }

        /**
         * Returns the time of last access.
         * If not supported, fallbacks on modification time.
         */
        @Override
        public FileTime lastAccessTime() {
            return lastModifiedTime();
        }

        /**
         * Tells whether the file is a regular file with opaque content.
         */
        @Override
        public boolean isRegularFile() {
            return !isDirectory && size != 0;
        }

        /**
         * Tells whether the file is a directory.
         */
        @Override
        public boolean isDirectory() {
            return isDirectory;
        }

        /**
         * Tells whether the file is a symbolic link. S3 does not support directly symbolic links,
         * but may allow many keys to reference the same object with User-defined object metadata.
         * Current version of S3 wrappers does not parse those metadata.
         */
        @Override
        public boolean isSymbolicLink() {
            return false;
        }

        /**
         * Tells whether the file is something other than a regular file, directory, or symbolic link.
         * Current version of S3 wrappers does not emulate anything else than files and directories.
         */
        @Override
        public boolean isOther() {
            return false;
        }

        /**
         * Returns the size (in bytes) of the file.
         */
        @Override
        public long size() {
            return size;
        }

        /**
         * Returns an object that uniquely identifies the file, or {@code null} if none.
         */
        @Override
        public Object fileKey() {
            return fileKey;
        }

        /**
         * Returns the attributes as a map.
         */
        final Map<String,Object> toMap(final String attributes) {
            /*
             * Keep only attributes in the "basic" category, remove everything else.
             */
            String[] keys = (String[]) CharSequences.split(attributes, ',');
            boolean isBasic = true;
            int count = 0;
            for (final String key : keys) {
                final int s = key.indexOf(':');
                if (s >= 0) {
                    isBasic = NAME.regionMatches(true, 0, key, 0, s);
                    if (isBasic) keys[count++] = key.substring(s+1);
                } else if (isBasic) {
                    keys[count++] = key;
                }
            }
            keys = ArraysExt.resize(keys, count);
            if (ArraysExt.contains(keys, "*")) {
                keys = new String[] {
                    "creationTime", "lastModifiedTime", "size"
                };
            }
            /*
             * Now copy in the map all requested attributes.
             */
            final var map = new HashMap<String,Object>(8);
            for (final String key : keys) {
                final Object value;
                switch (key) {
                    case "creationTime": {
                        if (!isDirectory && creationTime == lastModifiedTime) continue;
                        value = creationTime();
                        break;
                    }
                    case "lastModifiedTime": {
                        if (isDirectory) continue;
                        value = lastModifiedTime();
                        break;
                    }
                    case "size": {
                        if (isDirectory) continue;
                        value = size;
                        break;
                    }
                    default: continue;
                }
                map.put(key, value);
            }
            return map;
        }
    }
}
