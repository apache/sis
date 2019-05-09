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
package org.apache.sis.metadata.iso;

import java.util.Arrays;
import java.util.List;
import java.util.Iterator;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import org.opengis.metadata.MetadataScope;
import org.apache.sis.internal.metadata.legacy.LegacyPropertyAdapter;


/**
 * A specialization of {@link LegacyPropertyAdapter} which will try to merge the
 * {@code "hierarchyLevel"} and {@code "hierarchyLevelName"} properties in the same
 * {@link DefaultMetadataScope} instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.5
 * @module
 */
abstract class MetadataScopeAdapter<L> extends LegacyPropertyAdapter<L,MetadataScope> {
    /**
     * @param scopes Value of {@link DefaultMetadata#getMetadataScopes()}.
     */
    MetadataScopeAdapter(final Collection<MetadataScope> scopes) {
        super(scopes);
    }

    /**
     * Invoked (indirectly) by JAXB when adding a new scope code or scope name. This implementation searches
     * for an existing {@link MetadataScope} instance with a free slot for the new value before to create a
     * new {@link DefaultMetadataScope} instance.
     */
    @Override
    public boolean add(final L newValue) {
        int n = 0;
        final Iterator<MetadataScope> it = elements.iterator();
        while (it.hasNext()) {
            MetadataScope scope = it.next();
            if (unwrap(scope) != null) {
                n++;
                continue;
            }
            /*
             * We found a free slot. If the slot is a metadata that we can modify (which is the usual case),
             * we will be allowed to update that instance directly - no change needed in the backing collection.
             * But if the metadata is not modifiable, then we will need to clone it and replaces the element in
             * the collection.
             */
            if (!(scope instanceof DefaultMetadataScope) ||
                    ((DefaultMetadataScope) scope).state() == DefaultMetadataScope.State.FINAL)
            {
                scope = new DefaultMetadataScope(scope);
                if (elements instanceof List<?>) {
                    ((List<MetadataScope>) elements).set(n, scope);
                } else {
                    /*
                     * Not a list. Delete all the remaining parts, substitute the value
                     * and reinsert everything in the same order.
                     */
                    final MetadataScope[] remaining = new MetadataScope[elements.size() - n];
                    remaining[0] = scope;
                    n = 1;
                    it.remove();
                    while (it.hasNext()) {
                        remaining[n++] = it.next();
                        it.remove();
                    }
                    if (n != remaining.length) { // Paranoiac check.
                        throw new ConcurrentModificationException();
                    }
                    elements.addAll(Arrays.asList(remaining));
                }
            }
            return update(scope, newValue);
        }
        return super.add(newValue);
    }
}
