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
import java.util.StringJoiner;
import org.apache.sis.coverage.SampleDimension;


/**
 * A container for a list of elements grouped by their sample dimensions.
 *
 * <h2>Usage for coverage aggregation</h2>
 * {@code GroupBySample} contains an arbitrary amount of {@link GroupByCRS} instances,
 * which in turn contain an arbitrary amount of {@link GroupByTransform} instances,
 * which in turn contain an arbitrary amount of {@link GridSlice} instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
final class GroupBySample extends Group<GroupByCRS<GroupByTransform>> {
    /**
     * The sample dimensions of this group.
     */
    private final List<SampleDimension> ranges;

    /**
     * Creates a new group of objects associated to the list of sample dimensions.
     *
     * @param  ranges  the sample dimensions of this group.
     */
    private GroupBySample(final List<SampleDimension> ranges) {
        this.ranges = ranges;       // TODO: use List.copyOf(…) with JDK10.
    }

    /**
     * Returns a name for this group.
     */
    @Override
    final String getName(final Locale locale) {
        final StringJoiner name = new StringJoiner(", ");
        for (final SampleDimension range : ranges) {
            name.add(range.getName().toInternationalString().toString(locale));
        }
        return name.toString();
    }

    /**
     * Returns the group of objects associated to the given ranges.
     * This method takes a synchronization lock on the given list.
     *
     * @param  <E>     type of objects in groups.
     * @param  groups  the list where to search for a group.
     * @param  ranges  sample dimensions of the desired group.
     * @return group of objects associated to the given ranges (never null).
     */
    static GroupBySample getOrAdd(final List<GroupBySample> groups, final List<SampleDimension> ranges) {
        synchronized (groups) {
            for (final GroupBySample c : groups) {
                if (ranges.equals(c.ranges)) {
                    return c;
                }
            }
            final GroupBySample c = new GroupBySample(ranges);
            groups.add(c);
            return c;
        }
    }

    /**
     * Creates sub-aggregates for each member of this group and add them to the given aggregate.
     *
     * @param  destination  where to add sub-aggregates.
     */
    final void createComponents(final GroupAggregate destination) {
        destination.sampleDimensions = ranges;
        destination.fillWithChildAggregates(this, (byCRS,child) -> {
            child.fillWithCoverageComponents(byCRS.members, ranges);
            child.sampleDimensions = ranges;
        });
    }
}
