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
package org.apache.sis.storage.base;

import java.util.List;
import java.util.Collection;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;


/**
 * An aggregate with a list of components determined in advance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class SimpleAggregate extends AbstractResource implements Aggregate {
    /**
     * Components of this aggregate as an unmodifiable collection.
     */
    private final List<Resource> components;

    /**
     * Creates a new resource, potentially as a child of another resource.
     *
     * @param  parent      the parent resource, or {@code null} if none.
     * @param  components  components of this aggregate. This collection is copied.
     */
    public SimpleAggregate(final Resource parent, final Collection<? extends Resource> components) {
        super(parent);
        this.components = UnmodifiableArrayList.wrap(components.toArray(Resource[]::new));
    }

    /**
     * Returns the components of this aggregate.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Collection<Resource> components() {
        return components;
    }
}
