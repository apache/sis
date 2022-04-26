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
package org.apache.sis.internal.gui;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;


/**
 * Utility methods for handling mouse dragging events.
 * Current version does not offer much service.
 * But this class provides a place where future versions may implement "smooth dragging".
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public final class MouseDrags {
    /**
     * Do not allow (for now) instantiation of this class.
     */
    private MouseDrags() {
    }

    /**
     * Sets the given handle on "mouse pressed", "mouse dragged" and "mouse released" listeners.
     * This convenience method is uses as a way to register the same lambda for the 3 kinds of listener.
     * By contrast, repeating a lambda expression such as {@code this::onDrag} three times results in 3
     * different lambda instances to be created.
     *
     * @param  view     the view on which to register the listener.
     * @param  handler  the listener to register on the specified view.
     */
    public static void setHandlers(final Region view, final EventHandler<? super MouseEvent> handler) {
        view.setOnMousePressed (handler);
        view.setOnMouseDragged (handler);
        view.setOnMouseReleased(handler);
    }
}
