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
package org.apache.sis.internal.storage.aggregate;

import java.util.List;
import java.util.Locale;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.util.Utilities;


/**
 * A container for a list of elements grouped by their CRS. The CRS comparisons ignore metadata.
 *
 * <h2>Usage for coverage aggregation</h2>
 * {@code GroupByCRS} contains an arbitrary amount of {@link GroupByTransform} instances,
 * which in turn contain an arbitrary amount of {@link GridSlice} instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @param  <E>  type of objects in this group.
 *
 * @since 1.3
 * @module
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
     * @param  crs  coordinate reference system of this group, or {@code null}.
     */
    private GroupByCRS(final CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    /**
     * Returns a name for this group.
     */
    @Override
    final String getName(final Locale locale) {
        return IdentifiedObjects.getDisplayName(crs, locale);
    }

    /**
     * Returns the group of objects associated to the given grid geometry.
     * The CRS comparisons ignore metadata.
     * This method takes a synchronization lock on the given list.
     *
     * @param  <E>       type of objects in groups.
     * @param  groups    the list where to search for a group.
     * @param  geometry  geometry of the grid coverage or resource.
     * @return group of objects associated to the given CRS (never null).
     */
    static <E> GroupByCRS<E> getOrAdd(final List<GroupByCRS<E>> groups, final GridGeometry geometry) {
        return getOrAdd(groups, geometry.isDefined(GridGeometry.CRS) ? geometry.getCoordinateReferenceSystem() : null);
    }

    /**
     * Returns the group of objects associated to the given CRS.
     * The CRS comparisons ignore metadata.
     * This method takes a synchronization lock on the given list.
     *
     * @param  <E>     type of objects in groups.
     * @param  groups  the list where to search for a group.
     * @param  crs     coordinate reference system of the desired group, or {@code null}.
     * @return group of objects associated to the given CRS (never null).
     */
    private static <E> GroupByCRS<E> getOrAdd(final List<GroupByCRS<E>> groups, final CoordinateReferenceSystem crs) {
        synchronized (groups) {
            for (final GroupByCRS<E> c : groups) {
                if (Utilities.equalsIgnoreMetadata(crs, c.crs)) {
                    return c;
                }
            }
            final GroupByCRS<E> c = new GroupByCRS<>(crs);
            groups.add(c);
            return c;
        }
    }
}
