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

import java.util.function.UnaryOperator;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.StorageConnector;


/**
 * {@link StorageConnector} options not part of public API.
 * Some of those options may move to public API in the future if useful.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @param  <T>  the type of option values.
 *
 * @since 1.2
 * @module
 */
public final class InternalOptionKey<T> extends OptionKey<T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1786137598411493790L;

    /**
     * Wraps readable or writable channels on creation. Wrappers can be used for example
     * in order to listen to read events or for transforming bytes on the fly.
     */
    @SuppressWarnings("unchecked")
    public static final InternalOptionKey<UnaryOperator<ChannelFactory>> CHANNEL_FACTORY_WRAPPER =
            (InternalOptionKey) new InternalOptionKey<>("READ_CHANNEL_WRAPPER", UnaryOperator.class);

    /**
     * Creates a new key of the given name.
     */
    public InternalOptionKey(final String name, final Class<T> type) {
        super(name, type);
    }
}
