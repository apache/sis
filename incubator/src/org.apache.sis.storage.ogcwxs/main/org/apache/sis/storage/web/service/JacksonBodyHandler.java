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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpResponse.BodySubscribers.html#mapping(java.net.http.HttpResponse.BodySubscriber,java.util.function.Function)
 */
public final class JacksonBodyHandler<W> implements HttpResponse.BodyHandler<Supplier<W>> {

    private final ObjectMapper mapper;
    private final JavaType ref;

    public JacksonBodyHandler(ObjectMapper mapper, Class<W> valueClass) {
        this(mapper, (JavaType)((valueClass == null) ? null : mapper.constructType(valueClass)));
    }

    public JacksonBodyHandler(ObjectMapper mapper, TypeReference<W> ref) {
        this(mapper, (JavaType)((ref == null) ? null : mapper.constructType(ref)));
    }

    public JacksonBodyHandler(ObjectMapper mapper, JavaType ref) {
        this.mapper = mapper;
        this.ref = ref;
    }

    @Override
    public HttpResponse.BodySubscriber<Supplier<W>> apply(HttpResponse.ResponseInfo responseInfo) {
        final String contentEncoding = responseInfo.headers().firstValue("Content-Encoding").orElse("");
        return asJSON(contentEncoding);
    }

    private BodySubscriber<Supplier<W>> asJSON(String contentEncoding) {
        final BodySubscriber<InputStream> upstream = BodySubscribers.ofInputStream();

        return BodySubscribers.mapping(upstream,
            (InputStream is) -> () -> {
                try (InputStream in = is) {
                    InputStream di = in;
                    if ("gzip".equals(contentEncoding)) {
                        di = new GZIPInputStream(di);
                    }
                    final W body;
                    if (ref == null) {
                        // Drain the InputStream
                        while (in.read() != -1) {
                            // Ignore
                        }
                        body = null;
                    } else {
                        byte[] blob = di.readAllBytes();
                        final String str = new String(blob, StandardCharsets.UTF_8);
                        body = mapper.readValue(str, ref);
                    }
                    return body;

                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
    }

}
