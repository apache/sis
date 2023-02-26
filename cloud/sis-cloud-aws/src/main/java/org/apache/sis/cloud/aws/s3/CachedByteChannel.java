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

import java.util.List;
import java.io.IOException;
import org.apache.sis.internal.storage.io.FileCacheByteChannel;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.Abortable;


/**
 * A seekable byte channel which copies S3 data to a temporary file for caching purposes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.2
 */
final class CachedByteChannel extends FileCacheByteChannel {
    /**
     * Path to the S3 file to open.
     */
    private final KeyPath path;

    /**
     * Creates a new channel for the S3 file identified by the given path.
     * The connection will be opened when first needed.
     *
     * @param  path  path to the S3 file to open.
     * @throws IOException if the temporary file can not be created.
     */
    CachedByteChannel(final KeyPath path) throws IOException {
        super("S3-");
        this.path = path;
    }

    /**
     * Returns the filename to use in error messages.
     */
    @Override
    protected String filename() {
        return path.getFileName().toString();
    }

    /**
     * Creates an input stream which provides the bytes to read starting at the specified position.
     *
     * @param  start  position of the first byte to read (inclusive).
     * @param  end    position of the last byte to read with the returned stream (inclusive),
     *                or {@link Long#MAX_VALUE} for end of stream.
     * @return contains the input stream providing the bytes to read starting at the given start position.
     */
    @Override
    protected Connection openConnection(final long start, final long end) throws IOException {
        final ResponseInputStream<GetObjectResponse> stream;
        try {
            GetObjectRequest.Builder builder = GetObjectRequest.builder().bucket(path.bucket).key(path.key);
            final String range = Connection.formatRange(start, end);
            if (range != null) {
                builder = builder.range(range);
            }
            stream = path.fs.client().getObject(builder.build());
            final GetObjectResponse response = stream.response();
            final String contentRange = response.contentRange();
            final String acceptRanges = response.acceptRanges();
            final List<String> rangeUnits = (acceptRanges != null) ? List.of(acceptRanges) : List.of();
            try {
                if (contentRange == null) {
                    final Long contentLength = response.contentLength();
                    final long length = (contentLength != null) ? contentLength : -1;
                    return new Connection(this, stream, length, rangeUnits);
                } else {
                    return new Connection(this, stream, contentRange, rangeUnits);
                }
            } catch (IllegalArgumentException e) {
                throw new IOException(e);
            }
        } catch (SdkException e) {
            throw FileService.failure(path, e);
        }
    }

    /**
     * Invoked when this channel is no longer interested in reading bytes from the specified connection.
     *
     * @param  connection  contains the input stream to eventually close.
     * @return whether the input stream has been closed by this method.
     * @throws IOException if an error occurred while closing the stream or preparing for next read operations.
     */
    @Override
    protected boolean abort(final Connection connection) throws IOException {
        if (connection.rawInput instanceof Abortable) {
            ((Abortable) connection.rawInput).abort();
            return true;
        } else {
            return super.abort(connection);
        }
    }
}
