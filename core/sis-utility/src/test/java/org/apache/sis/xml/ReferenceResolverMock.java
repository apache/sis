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
package org.apache.sis.xml;

import java.util.UUID;
import org.apache.sis.internal.jaxb.Context;


/**
 * A dummy implementation of {@link ReferenceResolver} which authorizes the replacement of metadata objects
 * by their {@link XLink} or {@link UUID} references. This resolver is used in test cases wanting to verify
 * identifier substitutions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class ReferenceResolverMock extends ReferenceResolver {
    /**
     * Creates a new reference resolver.
     */
    private ReferenceResolverMock() {
    }

    /**
     * Creates a new SIS context using a {@code ReferenceResolverMock}.
     * Callers shall use this method in a {@code try} … {@code finally} block as below:
     *
     * {@preformat java
     *     final Context context = ReferenceResolverMock.begin(true);
     *     try {
     *         // So some test
     *     } finally {
     *         context.finish();
     *     }
     * }
     *
     * @param  marshalling {@code true} for marshalling, or {@code false} for unmarshalling.
     * @return The (un)marshalling context.
     */
    public static Context begin(final boolean marshalling) {
        return new Context(null, new ReferenceResolverMock(), null, null, null, null,
                marshalling ? Context.MARSHALLING : 0);
    }

    /**
     * Unconditionally returns {@code true}.
     */
    @Override
    public <T> boolean canSubstituteByReference(final MarshalContext context, final Class<T> type, final T object, final UUID uuid) {
        return true;
    }

    /**
     * Unconditionally returns {@code true}.
     */
    @Override
    public <T> boolean canSubstituteByReference(final MarshalContext context, final Class<T> type, final T object, final XLink link) {
        return true;
    }
}
