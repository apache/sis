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
package org.apache.sis.gui.map;

import java.beans.PropertyChangeEvent;
import javafx.scene.transform.Transform;
import javafx.geometry.Point2D;


/**
 * Event sent when a rendering finished, either successfully or with errors.
 *
 * <h2><abbr>API</abbr> design note</h2>
 * This event is redundant with {@link MapCanvas#renderingProperty()}, but nevertheless added because we need
 * to provide the {@linkplain #change} information, which is not easily representable as an old a new value.
 * Because of this redundancy, and because the class is not serializable, we do not provide this event in the
 * public <abbr>API</abbr> yet. We should try harder to retrofit in existing <abbr>API</abbr>.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see MapCanvas#renderingProperty()
 * @see MapCanvas#runAfterRendering(Runnable)
 */
@SuppressWarnings("serial")     // Not intended to be serialized.
final class RenderingCompletedEvent extends PropertyChangeEvent {
    /**
     * Name of this property event.
     */
    static final String NAME = "isRendering";

    /**
     * The change in the "objective to display" transform between the previous rendering and the new rendering.
     * This is a snapshot of {@link MapCanvas#transform} at the time when {@link RenderingTask} started its work.
     * This field is {@code null} if the rendering was interrupted by an error before completion.
     *
     * <p>If this field is non-null, then this change has already been appended to the {@link MapCanvas}
     * "objective to display" transform at the time when this {@code RenderingCompletedEvent} is fired.
     * Note however that the "objective to display" transform may also contain additional changes
     * if the user continued to navigate on the map (zooms and pans) after the rendering.</p>
     *
     * @see RenderingTask#changeInProgress
     */
    private final Transform change;

    /**
     * Creates a new event for a change in the "objective to display" transform between two renderings.
     *
     * @param  source  the canvas that fired the event.
     * @param  change  the change in the transform between previous rendering and new rendering, or {@code null}.
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     */
    public RenderingCompletedEvent(final MapCanvas source, final Transform change) {
        super(source, NAME, Boolean.TRUE, Boolean.FALSE);
        this.change = change;
    }

    /**
     * Transforms a display coordinate from the old rendering space to the new rendering space.
     * If the change specified at construction time was {@code null}, then this method returns {@code null}.
     *
     * @param  x  the <var>x</var> coordinate in display units of the old rendering.
     * @param  y  the <var>y</var> coordinate in display units of the old rendering.
     * @return the (<var>x</var>, <var>y</var>) coordinates in display units of the new rendering, or {@code null}.
     */
    public final Point2D updateDisplayCoordinates(final double x, final double y) {
        if (change != null) {
            return change.transform(x, y);
        } else {
            return null;
        }
    }
}
