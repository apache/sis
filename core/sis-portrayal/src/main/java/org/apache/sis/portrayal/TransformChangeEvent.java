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
package org.apache.sis.portrayal;

import java.beans.PropertyChangeEvent;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * A change in the zoom, pan or translation applied for viewing a map. All events fired by
 * {@link Canvas} for the {@value Canvas#OBJECTIVE_TO_DISPLAY_PROPERTY} property are of this kind.
 * This specialization provides a method for computing the difference between the old and new state.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @see Canvas#OBJECTIVE_TO_DISPLAY_PROPERTY
 *
 * @since 1.3
 * @module
 */
public class TransformChangeEvent extends PropertyChangeEvent {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4065626270969827867L;

    /**
     * The change in display coordinates, computed when first needed.
     * This field may be precomputed by the code that fired this event.
     *
     * @see #getChangeInDisplayCoordinates()
     */
    LinearTransform change;

    /**
     * Creates a new event for a change of the "objective to display" property.
     * The old and new transforms should not be null, except for lazy computation:
     * a null {@code newValue} means to take the value from {@link Canvas#getObjectiveToDisplay()} when needed.
     *
     * @param  source    the canvas that fired the event.
     * @param  oldValue  the old "objective to display" transform.
     * @param  newValue  the new transform, or {@code null} for lazy computation.
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     */
    public TransformChangeEvent(final Canvas source, final LinearTransform oldValue, final LinearTransform newValue) {
        super(source, Canvas.OBJECTIVE_TO_DISPLAY_PROPERTY, oldValue, newValue);
    }

    /**
     * Returns the canvas on which this event initially occurred.
     *
     * @return the canvas on which this event initially occurred.
     */
    @Override
    public Canvas getSource() {
        return (Canvas) source;
    }

    /**
     * Gets the old "objective to display" transform.
     *
     * @return the old "objective to display" transform.
     */
    @Override
    public LinearTransform getOldValue() {
        return (LinearTransform) super.getOldValue();
    }

    /**
     * Gets the new "objective to display" transform.
     * It should be the current value of {@link Canvas#getObjectiveToDisplay()}.
     *
     * @return the new "objective to display" transform.
     */
    @Override
    public LinearTransform getNewValue() {
        LinearTransform transform = (LinearTransform) super.getNewValue();
        if (transform == null) {
            transform = getSource().getObjectiveToDisplay();
        }
        return transform;
    }

    /**
     * Returns the change in display coordinates from the old state to the new state.
     * If the "objective to display" transform changed because the users did a zoom,
     * pan or translation, this is the transform representing that change in display
     * coordinates.
     *
     * @return the change in display coordinates, or {@code null} if the old or new transform is missing.
     * @throws NoninvertibleTransformException if a singular matrix prevent the change to be computed.
     */
    public LinearTransform getChangeInDisplayCoordinates() throws NoninvertibleTransformException {
        if (change == null) {
            final LinearTransform oldValue = getOldValue();
            if (oldValue != null) {
                final LinearTransform newValue = getNewValue();
                if (newValue != null) {
                    change = (LinearTransform) MathTransforms.concatenate(oldValue.inverse(), newValue);
                }
            }
        }
        return change;
    }
}
