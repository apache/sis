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

import java.util.Objects;

/**
 * File System Key stored in {@link FileService#fileSystems}.
 *
 * @author  Quentin Bialota (Geomatys)
 */
final class ClientFileSystemKey {
    /**
     * The S3 access key.
     */
    final String accessKey;

    /**
     * The S3 host (if not stored on Amazon AWS Infrastructure).
     */
    final String host;

    /**
     * The S3 port (if not stored on Amazon AWS Infrastructure).
     */
    final int port;

    /**
     * Is the S3 HTTP Protocol secure (if not stored on Amazon AWS Infrastructure).
     */
    final boolean isHttps;

    /**
     * Creates a new file system key for the {@code FileService} with access key, host, port and protocol (secure or not secure).
     *
     * @param accessKey   the S3 access key for this file system.
     * @param host        the host or {@code null} for AWS request.
     * @param port        the port or {@code -1} for AWS request.
     * @param isHttps     the protocol is secure or not
     */
    public ClientFileSystemKey(String accessKey, String host, int port, boolean isHttps) {
        this.accessKey = accessKey;
        this.host = host;
        this.port = port;
        this.isHttps = isHttps;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o The reference object with which to compare.
     * @return {@code true} if this object is the same as the o argument; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientFileSystemKey)) return false;
        ClientFileSystemKey that = (ClientFileSystemKey) o;
        return Objects.equals(accessKey, that.accessKey) && Objects.equals(host, that.host) && port == that.port && isHttps == that.isHttps;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(accessKey, host, port);
    }
}
