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

import java.util.Locale;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.Utilities;


/**
 * A container for a list of elements grouped by their CRS. The CRS comparisons ignore metadata.
 *
 * <h2>Usage for coverage aggregation</h2>
 * {@code GroupByCRS} contains an arbitrary number of {@link GroupByTransform} instances,
 * which in turn contain an arbitrary number of {@link GridSlice} instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <E>  type of objects in this group.
 */
final class GroupByCRS<E> extends Group<E> {
    /**
     * The coordinate reference system of this group, or {@code null}.
     * All {@linkplain #members} of this group use this CRS,
     * possibly with ignorable differences in metadata.
     */
    private final CoordinateReferenceSystem crs;

    /**
     * Creates a new group of objects associated to the given CRS.
     *
     * @param  parent  the parent group in which this group is a child.
     * @param  crs  coordinate reference system of this group, or {@code null}.
     */
    GroupByCRS(final GroupBySample parent, final CoordinateReferenceSystem crs) {
        super(parent);
        this.crs = crs;
    }

    /**
     * Returns whether an object having the given <abbr>CRS</abbr> can be a member of this group.
     */
    final boolean accepts(final CoordinateReferenceSystem candidate) {
        return Utilities.equalsIgnoreMetadata(crs, candidate);
    }

    /**
     * Creates a name for this group for use in metadata (not a persistent identifier).
     * This is used as the resource name if an aggregated resource needs to be created.
     * The name distinguishes the group by their CRS name.
     */
    @Override
    final String createName(final Locale locale) {
        return IdentifiedObjects.getDisplayName(crs, locale);
    }
}
