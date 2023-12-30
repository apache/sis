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
package org.apache.sis.xml.test;

import java.net.URI;
import org.apache.sis.xml.XLink;
import org.apache.sis.xml.MarshalContext;
import org.apache.sis.xml.ReferenceResolver;


/**
 * The reference resolver to use for testing purpose.
 * This resolver blocks attempts to download documents at test (non-real) URLs.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class TestReferenceResolver extends ReferenceResolver {
    /**
     * A dummy URI to not try to load.
     */
    private final URI ignore;

    /**
     * Creates a default {@code ReferenceResolver}.
     *
     * @param  dummyURI  the dummy URL to ignore.
     */
    TestReferenceResolver(final String dummyURI) {
        ignore = URI.create(dummyURI);
    }

    /**
     * Returns an object of the given type for the given {@code xlink} attribute, except if it is a test URL.
     */
    @Override
    public <T> T resolve(final MarshalContext context, final Class<T> type, final XLink link) {
        if (ignore.equals(link.getHRef())) {
            return null;
        }
        return super.resolve(context, type, link);
    }
}
