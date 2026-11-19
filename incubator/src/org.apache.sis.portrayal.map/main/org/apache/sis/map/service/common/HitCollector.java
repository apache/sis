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

import java.awt.Shape;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.sis.map.Presentation;


/**
 * Collects the presentations whose drawings intersect an area, such as a mouse selection.
 * A presentation may have several drawings: it is collected once, when its first intersecting drawing is added.
 * This class is thread-safe, so that presentations can be tested in parallel.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see org.apache.sis.map.service.StylePainter#intersects(org.apache.sis.map.service.Scene2D, org.apache.sis.map.MapLayer, Shape)
 */
public final class HitCollector {
    /**
     * The area to test, in display coordinates.
     */
    private final Shape mask;

    /**
     * The presentations of the drawings intersecting the area, in the order of the first hit.
     * All accesses are synchronized on this set.
     */
    private final Set<Presentation> hits;

    /**
     * Creates a collector for the given area.
     *
     * @param  mask  the area to test, in display coordinates.
     */
    public HitCollector(final Shape mask) {
        this.mask = Objects.requireNonNull(mask, "mask");
        hits = new LinkedHashSet<>();
    }

    /**
     * Tests a drawing of a presentation, and collects the presentation if the drawing intersects the area.
     * The drawing is not tested if the presentation has already been collected.
     *
     * @param  presentation  the presentation of the drawing.
     * @param  drawing       the drawing to test, or {@code null} if none.
     * @return whether the presentation has been collected by this method call.
     */
    public boolean add(final Presentation presentation, final Drawing drawing) {
        if (drawing == null) {
            return false;
        }
        synchronized (hits) {
            if (hits.contains(presentation)) {
                return false;
            }
        }
        if (!drawing.intersects(mask)) {
            return false;
        }
        synchronized (hits) {
            return hits.add(presentation);
        }
    }

    /**
     * Returns the collected presentations, in the order of their first hit.
     *
     * @return the presentations whose drawings intersect the area.
     */
    public Stream<Presentation> hits() {
        synchronized (hits) {
            return List.copyOf(hits).stream();
        }
    }
}
