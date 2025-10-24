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

import org.apache.sis.storage.Resource;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.event.StoreListeners;
import org.opengis.metadata.Metadata;


/**
 * A pseudo-resource used as a way to specify listeners to the {@link AbstractResource} constructor.
 * Instances of this class are short-lived: no reference should be stored.
 * This pseudo-resource should never be visible to users.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class PseudoResource implements Resource {
    /**
     * The listeners that {@link AbstractResource} should take. May be {@code null}.
     */
    public final StoreListeners listeners;

    /**
     * Whether the children of this pseudo-resource should be hidden.
     * If {@code false} (the recommended value), then the children will have their own set of listeners
     * and each child will be the {@linkplain StoreListeners#getSource() source of their own events}.
     * It will be possible to add and remove listeners independently from the set of parent listeners.
     * Conversely if {@code true}, then the {@linkplain #listeners} will be used directly and the
     * children resource will not appear as the source of any event.
     *
     * <p>In any cases, the listeners of all parents (ultimately the data store that created the resource)
     * will always be notified, either directly if {@code childrenAreHidden} is {@code true} or indirectly
     * if {@code childrenAreHidden} is {@code false}.</p>
     */
    public final boolean childrenAreHidden;

    /**
     * Creates a new instance wrapping the given resources.
     *
     * @param  listeners          the listeners that {@link AbstractResource} should take, or {@code null}.
     * @param  childrenAreHidden  whether the children of this pseudo-resource should be hidden.
     */
    public PseudoResource(final StoreListeners listeners, final boolean childrenAreHidden) {
        this.listeners = listeners;
        this.childrenAreHidden = childrenAreHidden;
    }

    /**
     * Ignored.
     *
     * @return {@code null}.
     */
    @Override
    public Metadata getMetadata() {
        return null;
    }
}
