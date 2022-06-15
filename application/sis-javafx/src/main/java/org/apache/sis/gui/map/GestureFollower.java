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

import java.awt.geom.Point2D;
import java.util.logging.Logger;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.VLineTo;
import javafx.scene.shape.PathElement;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.event.EventType;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.portrayal.CanvasFollower;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.logging.Logging;


/**
 * A listener of mouse or keyboard events in a source canvas which can be reproduced in a target canvas.
 * This listener can reproduce the "real world" displacements documented in {@linkplain CanvasFollower parent class}.
 * In addition, this class can also follow mouse movements in source canvas and move a cursor in the target canvas
 * at the same "real world" position.
 *
 * <h2>Listeners</h2>
 * {@code GestureFollower} listeners need to be registered explicitly by a call to the {@link #initialize()} method.
 * The {@link #dispose()} convenience method is provided for unregistering all those listeners.
 *
 * <h2>Multi-threading</h2>
 * This class is <strong>not</strong> thread-safe.
 * All events should be processed in the JavaFX thread.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
public class GestureFollower extends CanvasFollower implements EventHandler<MouseEvent> {
    /**
     * Distance from cursor origin (0,0) to the nearest (inner) or farthest (outer) point of the shape
     * drawing the cursor. Fractional part should be .5 for locating the lines at pixel center.
     */
    private static final double CURSOR_INNER_RADIUS = 7.5, CURSOR_OUTER_RADIUS = 20.5;

    /**
     * The path elements that describes the shape of the cursor.
     * The same elements are shared by all {@link #cursor} instances.
     * The cursor center is at (0.5, 0.5), which maps to a pixel center in JavaFX coordinate system.
     */
    private static final PathElement[] CURSOR_SHAPE = {
        new MoveTo(0.5, -CURSOR_OUTER_RADIUS), new VLineTo(-CURSOR_INNER_RADIUS),
        new MoveTo(0.5, +CURSOR_OUTER_RADIUS), new VLineTo(+CURSOR_INNER_RADIUS),
        new MoveTo(-CURSOR_OUTER_RADIUS, 0.5), new HLineTo(-CURSOR_INNER_RADIUS),
        new MoveTo(+CURSOR_OUTER_RADIUS, 0.5), new HLineTo(+CURSOR_INNER_RADIUS)
    };

    /**
     * The effect applied on the cursor. The intend is to make it more visible if the cursor color
     * is close to the color of features rendered on the map.
     */
    private static final DropShadow CURSOR_EFFECT = new DropShadow(BlurType.ONE_PASS_BOX, Color.BLACK, 5, 0, 0, 0);

    /**
     * Whether changes in the "objective to display" transforms should be propagated from source to target canvas.
     * The default value is {@code false}; this property needs to be enabled explicitly by caller if desired.
     */
    public final BooleanProperty transformEnabled;

    /**
     * Whether mouse position in source canvas should be shown by a cursor in the target canvas.
     * The default value is {@code false}; this property needs to be enabled explicitly by caller if desired.
     */
    public final BooleanProperty cursorEnabled;

    /**
     * Cursor position of the mouse over source canvas, expressed in coordinates of the target canvas.
     */
    private final Point2D.Double cursorPosition;

    /**
     * The shape used for drawing a cursor on the target canvas. Constructed when first requested.
     *
     * @see #followCursor(boolean)
     */
    private Path cursor;

    /**
     * Creates a new listener for synchronizing "objective to display" transform changes and cursor position
     * between the specified canvas. This is a unidirectional binding: changes in source are applied on target,
     * but not the converse.
     *
     * <p>Caller needs to register listeners by a call to the {@link #initialize()} method.
     * This is not done automatically by this constructor for allowing users to control
     * when to start listening to changes.</p>
     *
     * @param  source  the canvas which is the source of zoom, pan or rotation events.
     * @param  target  the canvas on which to apply the changes of zoom, pan or rotation.
     */
    public GestureFollower(final MapCanvas source, final MapCanvas target) {
        super(source, target);
        super.setDisabled(true);
        cursorPosition   = new Point2D.Double();
        transformEnabled = new SimpleBooleanProperty(this, "transformEnabled");
        cursorEnabled    = new SimpleBooleanProperty(this, "cursorEnabled");
        transformEnabled.addListener((p,o,n) -> setDisabled(!n));
        cursorEnabled   .addListener((p,o,n) -> followCursor(n));
    }

    /**
     * Invoked when the {@link #cursorEnabled} property value changed.
     *
     * @param  enabled  the new property value.
     */
    private void followCursor(final boolean enabled) {
        final Pane pane = ((MapCanvas) source).floatingPane;
        if (enabled) {
            if (cursor == null) {
                cursor = new Path(CURSOR_SHAPE);
                cursor.setStroke(Color.LIGHTPINK);
                cursor.setEffect(CURSOR_EFFECT);
                cursor.setMouseTransparent(true);
                cursor.setManaged(false);
                cursor.setSmooth(false);
                cursor.setCache(true);
            }
            (((MapCanvas) target).floatingPane).getChildren().add(cursor);
            pane.addEventHandler(MouseEvent.MOUSE_ENTERED, this);
            pane.addEventHandler(MouseEvent.MOUSE_EXITED,  this);
            pane.addEventHandler(MouseEvent.MOUSE_MOVED,   this);
        } else {
            pane.removeEventHandler(MouseEvent.MOUSE_ENTERED, this);
            pane.removeEventHandler(MouseEvent.MOUSE_EXITED,  this);
            pane.removeEventHandler(MouseEvent.MOUSE_MOVED,   this);
            if (cursor != null) {
                (((MapCanvas) target).floatingPane).getChildren().remove(cursor);
            }
        }
    }

    /**
     * Invoked when the mouse position changed. This method should be invoked only if
     * {@link #cursorEnabled} is {@code true} (this is not verified by this method).
     *
     * @param  event  the enter, exit or move event.
     */
    @Override
    public void handle(final MouseEvent event) {
        final EventType<? extends MouseEvent> type = event.getEventType();
        if (type == MouseEvent.MOUSE_MOVED || type == MouseEvent.MOUSE_ENTERED) {
            final MathTransform2D tr = getDisplayTransform().orElse(null);
            if (tr != null) try {
                cursorPosition.x = event.getX();
                cursorPosition.y = event.getY();
                final Point2D  p = tr.transform(cursorPosition, cursorPosition);
                cursor.setTranslateX(p.getX());
                cursor.setTranslateY(p.getY());
                if (type == MouseEvent.MOUSE_ENTERED) {
                    cursor.setVisible(true);
                }
                return;
            } catch (TransformException e) {
                Logging.recoverableException(Logger.getLogger(Modules.APPLICATION), GestureFollower.class, "handle", e);
                // Handle as a mouse exit.
            }
        } else if (type != MouseEvent.MOUSE_EXITED) {
            return;
        }
        cursor.setVisible(false);
    }

    /**
     * Removes all listeners registered by this {@code GestureFollower} instance.
     * This method should be invoked when {@code GestureFollower} is no longer needed,
     * in order to avoid memory leak.
     */
    @Override
    public void dispose() {
        followCursor(false);
        super.dispose();
    }
}
