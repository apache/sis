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
package org.apache.sis.internal.referencing.provider;

import java.util.List;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.internal.util.LazySet;


/**
 * The provider of coordinate operations. All operations are read from the information provided
 * in the {@code META-INF/services/org.opengis.referencing.operation.OperationMethod} files.
 *
 * <p>This class is <strong>not</strong> thread-safe. Synchronization are user's responsibility.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class Providers extends LazySet<OperationMethod> {
    /**
     * Creates new set of provider.
     */
    public Providers() {
        super(OperationMethod.class);
    }

    /**
     * Caches a new element, possibly substituting the created instance by a previously created instance.
     *
     * @param element The element to add to the cache.
     */
    @Override
    protected void cache(OperationMethod element) {
        if (element instanceof GeodeticOperation) {
            final Class<?> variant3D = ((GeodeticOperation) element).variant3D();
            if (variant3D != null) {
                final List<OperationMethod> cached = cached();
                for (int i=cached.size(); --i >= 0;) {
                    final OperationMethod m = cached.get(i);
                    if (m.getClass() == variant3D) {
                        final GeodeticOperation candidate = ((GeodeticOperation) m).redimensioned[0];
                        if (candidate != null) {            // Should not be null, but let be safe.
                            assert candidate.getClass() == element.getClass() : variant3D;
                            element = candidate;
                            break;
                        }
                    }
                }
            }
        }
        super.cache(element);
    }
}
