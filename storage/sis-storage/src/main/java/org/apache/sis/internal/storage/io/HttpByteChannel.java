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
package org.apache.sis.internal.storage.io;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.InputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;


/**
 * A seekable byte channel on a HTTP connection.
 * This implementation use HTTP range for reading bytes at an arbitrary position.
 * A temporary file is used for caching the bytes that have been read.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.4
 * @module
 */
final class HttpByteChannel extends FileCacheByteChannel {
    /**
     * Data store name to report in case of failure.
     */
    private final String filename;

    /**
     * The request to be sent to the client, without "Range" header.
     * This builder contains the {@linkplain #path} to the file.
     */
    private final HttpRequest.Builder request;

    /**
     * The client where to send HTTP requests.
     */
    private final HttpClient client;

    /**
     * The singleton client used for HTTP connections.
     */
    private static WeakReference<HttpClient> sharedClient;

    /**
     * Gets or create the singleton client used for HTTP connections.
     */
    private static synchronized HttpClient sharedClient() {
        if (sharedClient != null) {
            HttpClient client = sharedClient.get();
            if (client != null) return client;
        }
        HttpClient client = HttpClient.newHttpClient();
        sharedClient = new WeakReference<>(client);
        return client;
    }

    /**
     * Creates a new channel for a file at the given URI.
     *
     * @param  name  data store name to report in case of failure.
     * @param  path  URL to the file to read.
     * @throws IOException if the temporary file can not be created.
     */
    public HttpByteChannel(final String name, final URI path) throws IOException {
        super("http-");
        filename = name;
        request  = HttpRequest.newBuilder(path);
        client   = sharedClient();
    }

    /**
     * Returns the data store name to report in case of failure.
     */
    @Override
    protected String filename() {
        return filename;
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
        HttpRequest.Builder r = request;
        String range = Connection.formatRange(start, end);
        if (range != null) {
            r = r.copy().setHeader("Range", range);
        }
        final HttpResponse<InputStream> response;
        try {
            response = client.send(r.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        final InputStream stream  = response.body();
        final HttpHeaders headers = response.headers();
        range = headers.firstValue("Content-Range").orElse(null);
        final List<String> rangeUnits = headers.allValues("Accept-Ranges");
        try {
            final long length = headers.firstValueAsLong("Content-Length").orElse(-1);
            return new Connection(stream, range, length, rangeUnits);
        } catch (IllegalArgumentException e) {
            throw new IOException(e);
        }
    }
}
