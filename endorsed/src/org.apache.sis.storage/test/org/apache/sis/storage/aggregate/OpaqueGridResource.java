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
package org.apache.sis.storage.aggregate;

import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.MemoryGridCoverageResource;
import org.apache.sis.storage.base.GridResourceWrapper;


/**
 * Wraps a grid coverage resource without changing anything. This is used for disabling optimizations,
 * in order to test different code paths that what would be used with {@code MemoryGridCoverageResource}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class OpaqueGridResource extends GridResourceWrapper {
    /**
     * The resource to hide.
     */
    private final GridCoverageResource source;

    /**
     * Creates a new wrapper for the given coverage.
     */
    OpaqueGridResource(final GridCoverage source) {
        this.source = new MemoryGridCoverageResource(null, source, null);
    }

    /**
     * Creates a new wrapper for the given resource.
     */
    OpaqueGridResource(final GridCoverageResource source) {
        this.source = source;
    }

    @Override protected Object getSynchronizationLock()     {return source;}
    @Override protected GridCoverageResource createSource() {return source;}
}
