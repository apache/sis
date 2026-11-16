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
package org.apache.sis.storage.web.openapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Supplier;
import org.apache.sis.storage.web.service.AbstractService;
import org.apache.sis.storage.web.service.ServiceRequest;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractOpenApi extends AbstractService {

    protected final ObjectMapper objectMapper;

    public AbstractOpenApi(OpenApiConfiguration apiClient) {
        super(apiClient);
        objectMapper = apiClient.getObjectMapper();
    }

    @Override
    public OpenApiConfiguration getConfiguration() {
        return (OpenApiConfiguration) super.getConfiguration();
    }

    protected <T> ServiceRequest<Supplier<T>> requestAsJson(HttpRequest.Builder request, Class<T> valueClass) {
        return super.requestAsJson(request, objectMapper, valueClass);
    }

    protected <T> ServiceRequest<Supplier<T>> requestAsJson(HttpRequest.Builder request, TypeReference<T> valueClass) {
        return super.requestAsJson(request, objectMapper, valueClass);
    }

    protected <T> ServiceRequest<Supplier<T>> requestAsJson(HttpRequest.Builder request, JavaType valueClass) {
        return super.requestAsJson(request, objectMapper, valueClass);
    }

    /**
     * Deserializes a specific named field from the JSON response object.
     * Use when the API returns a thin envelope (e.g. {@code {"versions":[...]}})
     * and the caller needs only the inner value as a standard Java type.
     *
     * @param <T>       target type
     * @param request   prepared request builder
     * @param fieldName JSON field name to extract from the root object
     * @param valueType Jackson TypeReference for the target type
     * @return configured service request
     */
    protected <T> ServiceRequest<Supplier<T>> requestAsJsonField(HttpRequest.Builder request, String fieldName, TypeReference<T> valueType) {
        final HttpResponse.BodyHandler<Supplier<T>> bodyHandler = responseInfo ->
            HttpResponse.BodySubscribers.mapping(
                HttpResponse.BodySubscribers.ofInputStream(),
                is -> {
                    try {
                        final T value = objectMapper.convertValue(
                            objectMapper.readTree(is).get(fieldName),
                            valueType);
                        return () -> value;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        return request(request, bodyHandler);
    }
}
