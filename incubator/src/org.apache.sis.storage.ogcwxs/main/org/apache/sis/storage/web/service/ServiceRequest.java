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
package org.apache.sis.storage.web.service;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * A request configured.
 * It can be customized using the builder method before sending it.
 *
 * @author Johann Sorel (Geomatys)
 * @param <T> request response type.
 */
public final class ServiceRequest<T> {

    private final AbstractService service;
    private final HttpRequest.Builder builder;
    private final HttpResponse.BodyHandler<T> bodyHandler;

    public ServiceRequest(AbstractService service, HttpRequest.Builder builder, HttpResponse.BodyHandler<T> bodyHandler) {
        this.service = service;
        this.builder = builder;
        this.bodyHandler = bodyHandler;
    }

    /**
     * Get the request builder, can be modified before sending the request.
     *
     * @return request builder.
     */
    public HttpRequest.Builder builder() {
        return builder;
    }

    /**
     * Send request and block until response is parsed.
     *
     * @return HttpResponse
     * @throws ServiceException if fails to make API call
     */
    public HttpResponse<T> send() throws ServiceException {
        return service.send(builder.build(), bodyHandler);
    }

    /**
     * Send request in an asynchrone way, method does not block the current thread.
     * @return a future HttpResponse
     */
    public CompletableFuture<HttpResponse<T>> sendAsync() {
        return service.sendAsync(builder.build(), bodyHandler);
    }
}
