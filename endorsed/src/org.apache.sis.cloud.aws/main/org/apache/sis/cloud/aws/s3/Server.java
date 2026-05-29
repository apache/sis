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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;


/**
 * Information about the server to connect to.
 * This is used as a key for identifying the file systems stored in {@link FileService#fileSystems}.
 *
 * @author  Quentin Bialota (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Server {
    /**
     * Default value when user did not specified explicitly a protocol.
     * In such case, the default protocol is <abbr>HTTPS</abbr>.
     *
     * <p>Protocol can also be defined with:</p>
     * <ul>
     *   <li>{@value FileService#IS_HTTPS} configuration properties.</li>
     * </ul>
     */
    static final boolean DEFAULT_IS_HTTPS = true;

    /**
     * An arbitrary value when the user did not specified explicitly a port.
     * In such case, no port is assigned and the default is chosen by Java.
     * Note that "no port" is not the same as "default port" for this class,
     * since the default ports for <abbr>HTTP</abbr> and <abbr>HTTPS</abbr> are hard-coded
     * to {@value FileService#HTTP_PORT} and {@value FileService#HTTPS_PORT} respectively.
     *
     * <p>Port can also be defined with:</p>
     * <ul>
     *   <li>{@value FileService#PORT} configuration properties.</li>
     * </ul>
     */
    static final int NO_PORT = -1;

    /**
     * The <abbr>AWS</abbr> S3 access key, or {@code null} if none.
     * In the latter case, the default mechanism documented in <abbr>AWS</abbr> <abbr>SDK</abbr> is used.
     * In preference order:
     *
     * <ul>
     *   <li>{@code aws.accessKeyId} and {@code aws.secretAccessKey} Java system properties.</li>
     *   <li>{@code AWS_ACCESS_KEY_ID} and {@code AWS_SECRET_ACCESS_KEY} environment variables.</li>
     *   <li>{@code ~/.aws/credentials} or {@code ~/.aws/conﬁg} files.</li>
     * </ul>
     */
    final String accessKey;

    /**
     * The S3 host (if not stored on Amazon <abbr>AWS</abbr> Infrastructure).
     * May be {@code null} if unspecified.
     * If and only if non-null, the {@link #port} shall be specified.
     */
    final String host;

    /**
     * The S3 port (if not stored on Amazon <abbr>AWS</abbr> Infrastructure).
     * May be {@link #NO_PORT} if unspecified.
     * The port shall be specified if and only if {@link #host} is non-null.
     */
    final int port;

    /**
     * Whether the S3 <abbr>HTTP</abbr> Protocol is secure.
     * Default is {@code true}.
     */
    final boolean isHttps;

    /**
     * Creates a new server for the given access key.
     *
     * @param accessKey  the S3 access key or {@code null} for using <abbr>AWS</abbr> configuration.
     */
    Server(final String accessKey) {
        this.accessKey = accessKey;
        this.host      = null;
        this.port      = NO_PORT;
        this.isHttps   = DEFAULT_IS_HTTPS;
    }

    /**
     * Creates a new server for the given access key, host, port and protocol (secure or not secure).
     *
     * @param accessKey   the S3 access key or {@code null} for using <abbr>AWS</abbr> configuration.
     * @param host        the host or {@code null} for using <abbr>AWS</abbr> configuration.
     * @param port        the port or {@link #NO_PORT} for using <abbr>AWS</abbr> configuration.
     * @param isHttps     whether the protocol is secure.
     * @param uri         <abbr>URI</abbr> to use for completing missing information, or {@code null} if none.
     */
    Server(final String accessKey, String host, int port, final boolean isHttps, final URI uri) {
        if (uri != null) {
            if (uri.getHost() == null) {
                /*
                 * The host is null if the authority contains characters that are invalid for a host name.
                 * For example if the host contains underscore character ('_'), then it is considered invalid.
                 * We could use the authority instead, but that authority may contain a user name, port number, etc.
                 * Current version does not try to parse that string.
                 */
                String bucket = uri.getAuthority();
                if (bucket == null) bucket = uri.toString();
                throw new IllegalArgumentException(Resources.format(Resources.Keys.InvalidBucketName_1, bucket));
            }
            /*
             * Host and port number specified in the property map have precedence over the URI.
             * If these information were not found anywhere, the AWS SDK will fetch them itself.
             */
            if (port < 0) {
                port = uri.getPort();
                if (port < 0) {
                    if (host != null) {
                        // Use default value only if the host was specified in the properties map.
                        // If the host and the port were specified by the URI, assume that the URI was complete.
                        port = isHttps ? FileService.HTTPS_PORT : FileService.HTTP_PORT;
                    }
                } else if (host == null) {
                    host = uri.getHost();
                }
            }
        }
        this.accessKey = accessKey;
        this.host      = host;
        this.port      = port;
        this.isHttps   = isHttps;
    }

    /**
     * Returns the protocol, host and port number as an <abbr>URI</abbr>.
     * The access key is ignored.
     *
     * @return <abbr>URI</abbr> to the server, ignoring the access key.
     * @throws URISyntaxException if an error occurred while creating the <abbr>URI</abbr.
     */
    final URI toURI() throws URISyntaxException {
        return new URI(isHttps ? "https" : "http", null, host, port, "", null, null);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param  obj  the object with which to compare.
     * @return {@code true} if this object is equal to {@code obj}, {@code false} otherwise.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Server) {
            final var that = (Server) obj;
            return Objects.equals(accessKey, that.accessKey) &&
                   Objects.equals(host, that.host) &&
                   port == that.port &&
                   isHttps == that.isHttps;
        }
        return false;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(accessKey, host, port, isHttps);
    }

    /**
     * Returns a string representation of the path to the server.
     * The string uses the <abbr>URI</abbr> syntax.
     * This is used for error message.
     *
     * @return  path to the server.
     */
    @Override
    public String toString() {
        if (host == null) {
            return accessKey;
        }
        return toString(null).toString();
    }

    /**
     * Returns a string representation of the path to the given bucket on this server.
     *
     * @param  bucket  the bucket for which to get the path, or {@code null}.
     * @return path to the given bucket.
     */
    final StringBuilder toString(final String bucket) {
        final var sb = new StringBuilder();
        if (bucket != null || host != null) {
            sb.append(KeyPath.SCHEME).append(KeyPath.SCHEME_SEPARATOR);
            if (accessKey != null) {
                sb.append(accessKey).append('@');
            }
            if (host != null) {
                sb.append(host);
                if (port >= 0) {
                    sb.append(':').append(port);
                }
                sb.append('/');
            }
            if (bucket != null) {
                sb.append(bucket);
            }
        }
        return sb;
    }
}
