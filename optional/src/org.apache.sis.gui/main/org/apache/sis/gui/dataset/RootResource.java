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
package org.apache.sis.gui.dataset;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import javafx.scene.control.TreeItem;
import org.opengis.metadata.Metadata;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.Resource;


/**
 * The root pseudo-resource for allowing the tree to contain more than one resource.
 * This root node should be hidden in the {@link ResourceTree}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class RootResource implements Aggregate {
    /**
     * The children to expose as an unmodifiable list of components.
     */
    private final List<TreeItem<Resource>> components;

    /**
     * Creates a new aggregate which is going to be wrapped in the given node.
     * Caller shall invoke {@code group.setValue(root)} after this constructor.
     *
     * @param  group     the new tree root which will contain "real" resources.
     * @param  previous  the previous root, to be added in the new group.
     */
    RootResource(final TreeItem<Resource> group, final TreeItem<Resource> previous) {
        components = group.getChildren();
        if (previous != null) {
            components.add(previous);
        }
    }

    /**
     * Checks whether this root contains the given resource as a direct child.
     * This method does not search recursively in sub-trees.
     *
     * @param  resource  the resource to search.
     * @param  remove    whether to remove the resource if found.
     * @return the resource wrapper, or {@code null} if not found.
     */
    TreeItem<Resource> contains(final Resource resource, final boolean remove) {
        for (int i=components.size(); --i >= 0;) {
            final TreeItem<Resource> item = components.get(i);
            if (((ResourceItem) item).contains(resource)) {
                return remove ? components.remove(i) : item;
            }
        }
        return null;
    }

    /**
     * Adds the given resource if not already present.
     * This is invoked when new resources are opened and listed in {@link ResourceTree}.
     *
     * @param  resource  the resource to add.
     * @return whether the given resource has been added.
     *
     * @see ResourceTree#addResource(Resource)
     */
    boolean add(final Resource resource) {
        for (int i = components.size(); --i >= 0;) {
            if (((ResourceItem) components.get(i)).contains(resource)) {
                return false;
            }
        }
        return components.add(new ResourceItem(resource));
    }

    /**
     * Returns a read-only view of the components. This method is not used directly by {@link ResourceTree}
     * but is defined in case a user invoke {@link ResourceTree#getResource()}. For this reason, it is not
     * worth to cache the list created in this method.
     */
    @Override
    public Collection<Resource> components() {
        return new AbstractList<Resource>() {
            @Override public int size() {
                return components.size();
            }

            @Override public Resource get(final int index) {
                return components.get(index).getValue();
            }
        };
    }

    /**
     * Returns null since this resource has no metadata. Returning null is normally
     * not allowed for this method, but {@link ResourceTree} is robust to this case.
     */
    @Override
    public Metadata getMetadata() {
        return null;
    }
}
