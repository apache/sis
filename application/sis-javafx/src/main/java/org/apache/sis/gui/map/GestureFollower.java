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
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.Optional;
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
import org.apache.sis.portrayal.TransformChangeEvent;
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
    private static final DropShadow CURSOR_EFFECT = new DropShadow(BlurType.ONE_PASS_BOX, Color.DEEPPINK, 5, 0, 0, 0);

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
     * Whether the {@link #cursorSourcePosition} is valid.
     * If {@code true}, then {@link #cursor} shall be non-null and should be visible.
     */
    private boolean cursorSourceValid;

    /**
     * Cursor position of the mouse over source canvas, expressed in coordinates of the source and target canvas.
     */
    private final Point2D.Double cursorSourcePosition, cursorTargetPosition;

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
        cursorSourcePosition = new Point2D.Double();
        cursorTargetPosition = new Point2D.Double();
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
                cursor.setStrokeWidth(3);
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
            pane.addEventHandler(MouseEvent.MOUSE_DRAGGED, this);
            cursorSourceValid = true;
        } else {
            cursorSourceValid = false;
            pane.removeEventHandler(MouseEvent.MOUSE_ENTERED, this);
            pane.removeEventHandler(MouseEvent.MOUSE_EXITED,  this);
            pane.removeEventHandler(MouseEvent.MOUSE_MOVED,   this);
            pane.removeEventHandler(MouseEvent.MOUSE_DRAGGED, this);
            if (cursor != null) {
                (((MapCanvas) target).floatingPane).getChildren().remove(cursor);
            }
        }
    }

    /**
     * Returns the position for the mouse cursor in the source canvas if that position is known.
     * This information is used when the source and target canvases do not use the same CRS.
     * {@code GestureFollower} tries to transform the canvas views in such a way that the
     * "real world" change is the same for both canvas at that location.
     *
     * <p>The returned value is "live"; it may change with mouse and gesture events.
     * Callers should not modify that value, and copy it if they need to keep it.</p>
     *
     * @return mouse position in source canvas where displacements, zooms and rotations
     *         applied on the source canvas should be mirrored exactly on the target canvas.
     */
    @Override
    public Optional<Point2D> getSourceDisplayPOI() {
        if (cursorSourceValid) {
            return Optional.of(cursorSourcePosition);
        }
        return super.getSourceDisplayPOI();
    }

    /**
     * Invoked when the mouse position changed. This method should be invoked only if
     * {@link #cursorEnabled} is {@code true} (this is not verified by this method).
     *
     * @param  event  the enter, exit or move event.
     */
    @Override
    public void handle(final MouseEvent event) {
        cursorSourcePosition.x = event.getX();
        cursorSourcePosition.y = event.getY();
        cursorSourceValid = true;
        final EventType<? extends MouseEvent> type = event.getEventType();
        if (type == MouseEvent.MOUSE_MOVED) {
            updateCursorPosition();
        } else if (type == MouseEvent.MOUSE_ENTERED) {
            cursor.setVisible(true);
            updateCursorPosition();
        } else if (type == MouseEvent.MOUSE_EXITED) {
            cursor.setVisible(false);
        }
    }

    /**
     * Sets the cursor location in the target canvas to a position computed from current value
     * of {@link #cursorSourcePosition}.
     */
    private void updateCursorPosition() {
        final MathTransform2D tr = getDisplayTransform().orElse(null);
        if (tr != null) try {
            final Point2D  p = tr.transform(cursorSourcePosition, cursorTargetPosition);
            cursor.setTranslateX(p.getX());
            cursor.setTranslateY(p.getY());
        } catch (TransformException e) {
            cursorSourceValid = false;
            cursor.setVisible(false);
            Logging.recoverableException(Logger.getLogger(Modules.APPLICATION), GestureFollower.class, "handle", e);
        }
    }

    /**
     * Returns {@code true} if this listener should replicate the following changes on the target canvas.
     * This implementation returns {@code true} if the transform reason is {@link TransformChangeEvent.Reason#INTERM}.
     * It allows immediate feedback to users without waiting for the background thread to complete rendering.
     *
     * @param  event  a transform change event that occurred on the source canvas.
     * @return  whether to replicate that change on the target canvas.
     */
    @Override
    protected boolean filter(final TransformChangeEvent event) {
        return event.getReason() == TransformChangeEvent.Reason.INTERIM;
    }

    /**
     * Invoked after the source "objective to display" transform has been updated.
     * This implementation adjusts the cursor position for compensating the relative change in mouse position.
     *
     * <div class="note"><b>Details:</b>
     * If the map moved in the {@linkplain #source source} canvas without a change of mouse cursor position
     * (for example if the user navigates using the keyboard), then the mouse position changed relatively to
     * the map, so the cursor position on the {@linkplain #target target} canvas needs to be updated accordingly.
     * This is a temporary change applied until the next {@link MouseEvent} gives us new mouse coordinates relative
     * to the map.</div>
     */
    @Override
    protected void transformedSource(final TransformChangeEvent event) {
        super.transformedSource(event);
        if (cursorSourceValid) {
            final AffineTransform change = event.getDisplayChange2D().orElse(null);
            if (change == null) {
                cursorSourceValid = false;
                cursor.setVisible(false);
            } else if (event.getReason() != TransformChangeEvent.Reason.INTERIM) {
                change.transform(cursorSourcePosition, cursorSourcePosition);
                updateCursorPosition();
            } else try {
                change.inverseTransform(cursorSourcePosition, cursorSourcePosition);
                updateCursorPosition();
            } catch (NoninvertibleTransformException e) {
                cursorSourceValid = false;
                cursor.setVisible(false);
            }
        }
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
