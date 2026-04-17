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

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;


/**
 * A pane for evanescent shapes shown in a {@link MapCanvas}. The nodes in this pane should use
 * an animation such as {@link javafx.animation.FadeTransition} and be removed after a few seconds.
 *
 * <p>This pane is used when it is not worth to update the shapes coordinates after user's navigation
 * (zooms, pans, rotations) because those shapes will disappear soon anyway. Instead, a transform will
 * be added the whole pane when needed.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class EvanescentPane extends Pane implements ListChangeListener<Node> {
    /**
     * The object that created this pane.
     */
    final MapCanvas.StaticGraphics owner;

    /**
     * Whether this pane has been added in {@link MapCanvas#floatingPane}.
     */
    private boolean added;

    /**
     * Creates an initially empty pane with no shape and no transform.
     *
     * @param  owner  the object that created this pane.
     */
    private EvanescentPane(final MapCanvas.StaticGraphics owner) {
        this.owner = owner;
    }

    /**
     * Creates an initially empty pane with no shape and no transform.
     *
     * <p>Note that the returned pane has an identity transform at the time when
     * this method is invoked, but that transform may become non-identity later
     * if the user navigates on the map (e.g. zoom or pan events).</p>
     *
     * @param  owner  the object that created this pane.
     * @return a pane with an identity transform at the time when this method is invoked.
     */
    static EvanescentPane create(final MapCanvas.StaticGraphics owner) {
        final var pane = new EvanescentPane(owner);
        pane.getChildren().addListener(pane);
        return pane;
    }

    /**
     * Invoked after the list of evanescent children changed.
     * If the last child has been removed, removes this pane from the map canvas.
     */
    @Override
    public void onChanged(Change<? extends Node> change) {
        if (getChildren().isEmpty() == added) {
            final ObservableList<Node> siblings = owner.floatingPane.getChildren();
            if (added) {
                siblings.remove(this);
                added = false;
            } else {
                siblings.add(this);
                added = true;
            }
        }
    }
}
