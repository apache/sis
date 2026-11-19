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
package org.apache.sis.map.service.common;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;


/**
 * Places labels without overlaps, then draws them on top of everything else.
 * The labels are collected while the layers are portrayed, by groups such as the labels of a feature.
 *
 * <ul>
 *   <li>Labels are placed by decreasing priority, and in the order they were added for equal priorities.</li>
 *   <li>A label is omitted if its footprint, enlarged by the padding, intersects a label of another group already placed.
 *       The labels of a same group do not exclude each other.</li>
 *   <li>The labels are drawn in placement order.</li>
 *   <li>This class is not thread-safe.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class LabelPlacer {
    /**
     * Margin in pixels added around a label when searching for overlaps with the labels already placed.
     */
    private final double padding;

    /**
     * The labels at their candidate positions, in the order they were added.
     */
    private final List<Entry> candidates;

    /**
     * Number of groups added so far, used for identifying the groups.
     */
    private int groupCount;

    /**
     * A candidate label with the group it belongs to.
     *
     * @param candidate  the candidate label.
     * @param group      the group of the label.
     */
    private record Entry(LabelCandidate candidate, int group) {
        /**
         * Returns whether this label, enlarged by the padding, intersects the given label of another group.
         */
        boolean conflicts(final Entry placed, final double padding) {
            if (placed.group == group) {
                return false;
            }
            for (final LabelBox box : candidate.footprint()) {
                final LabelBox enlarged = box.expand(padding);
                for (final LabelBox other : placed.candidate.footprint()) {
                    if (enlarged.intersects(other)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Creates a placer without labels.
     *
     * @param  padding  margin in pixels added around a label when searching for overlaps.
     */
    public LabelPlacer(final double padding) {
        this.padding = padding;
        candidates = new ArrayList<>();
    }

    /**
     * Adds a label in a group of its own.
     *
     * @param  candidate  the label at its candidate position.
     */
    public void add(final LabelCandidate candidate) {
        candidates.add(new Entry(candidate, groupCount++));
    }

    /**
     * Adds a group of labels which do not exclude each other, for example the repetitions of a label along a line.
     *
     * @param  group  the labels at their candidate positions.
     */
    public void add(final Collection<LabelCandidate> group) {
        final int id = groupCount++;
        for (final LabelCandidate candidate : group) {
            candidates.add(new Entry(candidate, id));
        }
    }

    /**
     * Places the collected labels without overlaps, then draws them.
     * The candidates are discarded after this method call.
     *
     * @param  graphics  where to draw, in display coordinates.
     */
    public void paint(final Graphics2D graphics) {
        candidates.sort(Comparator.comparingDouble((Entry e) -> e.candidate().priority()).reversed());
        final List<Entry> placed = new ArrayList<>();
        for (final Entry entry : candidates) {
            if (placed.stream().noneMatch((other) -> entry.conflicts(other, padding))) {
                placed.add(entry);
            }
        }
        candidates.clear();
        for (final Entry entry : placed) {
            entry.candidate().drawing().paint(graphics);
        }
    }
}
