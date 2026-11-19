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

import java.util.List;
import java.util.Objects;


/**
 * A label at a candidate position, drawn if it does not overlap the labels placed before it.
 *
 * @param drawing    the label in display coordinates.
 * @param footprint  the area occupied by the label: one box for a straight label, or one box per glyph along a line.
 * @param priority   the placement priority, higher values being placed first.
 *
 * @author Johann Sorel (Geomatys)
 */
public record LabelCandidate(Drawing drawing, List<LabelBox> footprint, double priority) {
    /**
     * Creates a candidate label.
     *
     * @param drawing    the label in display coordinates.
     * @param footprint  the area occupied by the label.
     * @param priority   the placement priority, higher values being placed first. NaN is replaced by 0.
     */
    public LabelCandidate {
        Objects.requireNonNull(drawing, "drawing");
        footprint = List.copyOf(footprint);
        if (Double.isNaN(priority)) {
            priority = 0;
        }
    }
}
