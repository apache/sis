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
package org.apache.sis.storage.earthobservation;

import org.apache.sis.storage.StorageConnector;


/**
 * The provider of {@link LandsatStore} instances. Given a {@link StorageConnector} input,
 * this class tries to instantiate a {@code LandsatStore}.
 *
 * <h2>Thread safety</h2>
 * The same {@code LandsatStoreProvider} instance can be safely used by many threads without synchronization on
 * the part of the caller. However the {@link LandsatStore} instances created by this factory are not thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.8
 * @module
 *
 * @deprecated Moved to the {@link org.apache.sis.storage.landsat} package.
 */
@Deprecated
public class LandsatStoreProvider extends org.apache.sis.storage.landsat.LandsatStoreProvider {
    /**
     * Creates a new provider.
     */
    public LandsatStoreProvider() {
    }
}
