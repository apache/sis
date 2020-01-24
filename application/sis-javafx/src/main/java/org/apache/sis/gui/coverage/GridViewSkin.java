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
package org.apache.sis.gui.coverage;

import java.awt.image.RenderedImage;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.control.skin.VirtualContainerBase;
import javafx.scene.shape.Rectangle;


/**
 * The {@link GridView} renderer as a virtualized and scrollable content.
 * The primary direction of virtualization is vertical (rows will stack vertically on top of each other).
 *
 * <p>Relationships:</p>
 * <ul>
 *   <li>This is created by {@link GridView#createDefaultSkin()}.</li>
 *   <li>The {@link GridView} which own this skin is given by {@link #getSkinnable()}.</li>
 *   <li>This {@code GridViewSkin} contains an arbitrary amount of {@link GridRow} children.
 *       It should be limited to the number of children that are visible in same time,
 *       not the total number of rows in the image.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class GridViewSkin extends VirtualContainerBase<GridView, GridRow> {
    /**
     * Creates a new skin for the specified view.
     */
    GridViewSkin(final GridView view) {
        super(view);
        final VirtualFlow<GridRow> flow = getVirtualFlow();
        flow.setCellFactory(GridRow::new);
        flow.setFocusTraversable(true);
        flow.setFixedCellSize(Math.max(GridView.MIN_CELL_SIZE, view.cellHeight.get()));
        view.cellHeight .addListener(this::cellHeightChanged);
        view.cellWidth  .addListener(this::cellWidthChanged);
        view.headerWidth.addListener(this::cellWidthChanged);
        /*
         * Rectangles for filling the background of the cells in the header row and header column.
         * Those rectangles will be resized when the GridView size changes or cells size changes.
         */
        final Rectangle topBackground  = new Rectangle();
        final Rectangle leftBackground = new Rectangle();
        topBackground.setHeight(view.cellHeight.get());
        leftBackground.setY(topBackground.getHeight());
        leftBackground.widthProperty().bind(view.headerWidth);
        leftBackground.fillProperty() .bind(view.headerBackground);
        topBackground .fillProperty() .bind(view.headerBackground);
        /*
         * The list of children is initially empty. We need to
         * add the virtual flow, otherwise nothing will appear.
         */
        getChildren().addAll(topBackground, leftBackground, flow);
        flow.widthProperty() .addListener(this::gridSizeChanged);
        flow.heightProperty().addListener(this::gridSizeChanged);
    }

    /**
     * Invoked when the width or height of {@link GridView} changed. This method recomputes the size of
     * the rectangles used for painting backgrounds. We listen to changes in width and height together
     * because a change of width may show or hide the horizontal scroll bar, which change the height
     * (and conversely for the vertical scroll bar).
     */
    private void gridSizeChanged(ObservableValue<? extends Number> property, Number oldValue, Number newValue) {
        final Flow flow = (Flow) getVirtualFlow();
        final ObservableList<Node> children = getChildren();
        Rectangle r;
        r = (Rectangle) children.get(0); r.setWidth (flow.getVisibleWidth()  - r.getX());
        r = (Rectangle) children.get(1); r.setHeight(flow.getVisibleHeight() - r.getY());
    }

    /**
     * Invoked when the value of {@link GridView#cellHeight} property changed. This method copies the new value
     * into {@link VirtualFlow#fixedCellSizeProperty()} after bounds check, then adjusts the size and position
     * of rectangles filling the header background.
     */
    private void cellHeightChanged(ObservableValue<? extends Number> property, Number oldValue, Number newValue) {
        final Flow flow = (Flow) getVirtualFlow();
        final ObservableList<Node> children = getChildren();
        final double height = Math.max(GridView.MIN_CELL_SIZE, newValue.doubleValue());
        flow.setFixedCellSize(height);
        Rectangle r;
        r = (Rectangle) children.get(0); r.setHeight(height);
        r = (Rectangle) children.get(1); r.setHeight(flow.getVisibleHeight() - height); r.setY(height);
    }

    /**
     * Invoked when the cell width or cell spacing changed.
     * This method notifies all children about the new width.
     */
    private void cellWidthChanged(ObservableValue<? extends Number> property, Number oldValue, Number newValue) {
        final double width = getSkinnable().getContentWidth();
        for (final Node child : getChildren()) {
            if (child instanceof GridRow) {             // The first instances are not a GridRow.
                ((GridRow) child).setPrefWidth(width);
            }
        }
    }

    /**
     * Invoked when the content may have changed. If {@code all} is {@code true}, then everything
     * may have changed including the number of rows and columns. If {@code all} is {@code false}
     * then the number of rows and columns is assumed the same.
     *
     * @see GridView#contentChanged(boolean)
     */
    final void contentChanged(final boolean all) {
        if (all) {
            updateItemCount();
        }
        /*
         * Following call may be redundant with `updateItemCount()` except if the number of
         * rows did not changed, in which case `updateItemCount()` may have sent no event.
         */
        ((Flow) getVirtualFlow()).changed(null, null, null);
    }

    /**
     * Creates the virtual flow used by this {@link GridViewSkin}. The virtual flow
     * created by this method registers a listener for horizontal scroll bar events.
     */
    @Override
    protected VirtualFlow<GridRow> createVirtualFlow() {
        return new Flow(getSkinnable());
    }

    /**
     * The virtual flow used by {@link GridViewSkin}. We define that class
     * mostly for getting access to the protected {@link #getHbar()} method.
     */
    static final class Flow extends VirtualFlow<GridRow> implements ChangeListener<Number> {
        /**
         * Creates a new flow for the given view. This method registers listeners
         * on the properties that may require a redrawn of the full view port.
         */
        @SuppressWarnings("ThisEscapedInObjectConstruction")
        Flow(final GridView view) {
            getHbar().valueProperty().addListener(this);
            view.bandProperty .addListener(this);
            view.cellSpacing  .addListener(this);
            // Other listeners are registered by enclosing class.
        }

        /**
         * The position of the horizontal scroll bar. This is a value between 0 and
         * the width that the {@link GridView} would have if we were showing it fully.
         */
        final double getHorizontalPosition() {
            return getHbar().getValue();
        }

        /**
         * Returns the width of the view port area, not counting the vertical scroll bar.
         */
        final double getVisibleWidth() {
            double width = getWidth();
            final ScrollBar bar = getVbar();
            if (bar.isVisible()) {
                width -= bar.getWidth();
            }
            return width;
        }

        /**
         * Returns the height of the view port area, not counting the horizontal scroll bar.
         */
        final double getVisibleHeight() {
            double height = getHeight();
            final ScrollBar bar = getHbar();
            if (bar.isVisible()) {
                height -= bar.getHeight();
            }
            return height;
        }

        /**
         * Invoked when the content to show changed because of a change in a property.
         * The most important event is a change in the position of horizontal scroll bar,
         * which is handled as a change of content because we will need to change values
         * shown by the cells (because we reuse a small number of cells in visible region).
         * But this method is also invoked for real changes of content like changes in the
         * index of the band to show, provided that the number of rows and columns is the same.
         *
         * @param  property  the property that changed (ignored).
         * @param  oldValue  the old value (ignored).
         * @param  newValue  the new value (ignored).
         */
        @Override
        public void changed(ObservableValue<? extends Number> property, Number oldValue, Number newValue) {
            // Inform VirtualFlow that a layout pass should be done, but no GridRows have been added or removed.
            reconfigureCells();
        }
    }

    /**
     * Invoked when it is possible that the item count has changed. JavaFX may invoke this method
     * when scrolling has occurred, the control has resized, <i>etc.</i>, but for {@link GridView}
     * the count will change only if a new {@link RenderedImage} has been specified.
     */
    @Override
    protected void updateItemCount() {
        /*
         * VirtualFlow.setCellCount(int) indicates the number of cells that should be in the flow.
         * When the cell count changes, VirtualFlow responds by updating the visuals. If the items
         * backing the cells change but the count has not changed, then reconfigureCells() should
         * be invoked instead. This is done by the `Flow` inner class above.
         */
        getVirtualFlow().setCellCount(getItemCount());      // Fires event only if count changed.
    }

    /**
     * Returns the total number of image rows, including those that are currently hidden because
     * they are out of view. The returned value is (indirectly) {@link RenderedImage#getHeight()}.
     */
    @Override
    public int getItemCount() {
        return getSkinnable().getImageHeight();
    }

    /**
     * Called during the layout pass of the scene graph. The (x,y) coordinates are usually zero
     * and the (width, height) are the size of the control as shown (not the full content size).
     * Current implementation sets the virtual flow size to the given size.
     */
    @Override
    protected void layoutChildren(final double x, final double y, final double width, final double height) {
        /*
         * Super-class only invokes `updateItemCount()` if needed.
         * It does not perform any layout by itself in this method.
         */
        super.layoutChildren(x, y, width, height);
        getVirtualFlow().resizeRelocate(x, y, width, height);
    }
}
