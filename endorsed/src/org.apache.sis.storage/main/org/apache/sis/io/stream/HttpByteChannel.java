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
package org.apache.sis.io.stream;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.file.AccessDeniedException;
import java.lang.ref.WeakReference;
import java.util.List;
import org.apache.sis.storage.internal.Resources;


/**
 * A seekable byte channel on a HTTP connection.
 * This implementation use HTTP range for reading bytes at an arbitrary position.
 * A temporary file is used for caching the bytes that have been read.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
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
     * @throws IOException if the temporary file cannot be created.
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
        switch (response.statusCode()) {
            default:  throw new IOException(cannotConnect(response));
            case 204: // No Content
            case 404: throw new FileNotFoundException(cannotConnect(response));
            case 451: // Unavailable For Legal Reasons
            case 401: // Unauthorized
            case 403: throw new AccessDeniedException(cannotConnect(response));
            case 203: // Non-Authoritative Information
            case 200: break;    // OK
            case 206: break;    // Partial content
        }
        final InputStream stream  = response.body();
        final HttpHeaders headers = response.headers();
        range = headers.firstValue("Content-Range").orElse(null);
        final List<String> rangeUnits = headers.allValues("Accept-Ranges");
        try {
            if (range == null) {
                final long length = headers.firstValueAsLong("Content-Length").orElse(-1);
                return new Connection(this, stream, length, rangeUnits);
            } else {
                return new Connection(this, stream, range, rangeUnits);
            }
        } catch (IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    /**
     * Returns an error message for an HTTP connection that failed.
     */
    private String cannotConnect(final HttpResponse<?> response) {
        return Resources.format(Resources.Keys.CanNotConnectHTTP_2, filename, response.statusCode());
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
        connection.input.close();
        return true;
    }
}
