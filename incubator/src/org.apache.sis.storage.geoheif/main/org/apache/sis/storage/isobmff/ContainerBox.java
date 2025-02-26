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
package org.apache.sis.storage.isobmff;

import java.io.IOException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.base.MetadataBuilder;


/**
 * Box whose sole purpose is to contain and group a set of related boxes.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public abstract class ContainerBox extends Box {
    /**
     * All boxes contained in this container box. This array may or may not accept null elements,
     * depending on the {@code indexed} argument given at construction time:
     *
     * <ul>
     *   <li>If {@code indexed} was {@code false}, all array elements are non-null but indexes in the array
     *     may not correspond to the indexes in the file. This is because unknown boxes were skipped.</li>
     *   <li>If {@code indexed} was {@code true}, the indexes in the array correspond to the indexes in the
     *     file, but the array may contain null elements in place of unknown boxes.</li>
     * </ul>
     */
    public final Box[] children;

    /**
     * Creates a new box and loads the payload using the given box registry.
     * A custom registry is specified for filtering the type of boxes to accept.
     * Boxes of unknown types are ignored and skipped.
     *
     * @param  reader    the reader from which to read the payload.
     * @param  registry  the registry to use for box instantiations, or {@code null} for the main registry.
     * @param  indexed   whether index matter. If {@code true}, then the children array may contain null elements.
     * @throws IOException if an error occurred while reading the payload.
     * @throws DataStoreContentException if the <abbr>HEIF</abbr> file is malformed.
     * @throws DataStoreException if the reading failed for another reason.
     */
    protected ContainerBox(final Reader reader, final BoxRegistry registry, final boolean indexed)
            throws IOException, DataStoreException
    {
        children = reader.readChildren(registry, indexed).toArray(Box[]::new);
    }

    /**
     * Converts node properties of all children to <abbr>ISO</abbr> 19115 metadata.
     *
     * @param  builder  the builder where to set metadata information.
     */
    @Override
    public void metadata(final MetadataBuilder builder) {
        for (final Box child : children) {
            if (child != null) {
                child.metadata(builder);
            }
        }
    }
}
