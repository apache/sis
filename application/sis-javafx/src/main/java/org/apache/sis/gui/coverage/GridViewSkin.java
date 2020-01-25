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
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.control.skin.VirtualContainerBase;
import javafx.scene.layout.HBox;
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
     * The cells that we put in the header row on the top of the view. This list is initially empty;
     * new elements are added or removed when first needed and when the view size changed.
     */
    private final ObservableList<Node> headerRow;

    /**
     * Background of the header row (top side) and header column (left side) of the view.
     */
    private final Rectangle topBackground, leftBackground;

    /**
     * Image index of the first column visible in the view, ignoring the header column.
     * This is a {@link RenderedImage} <var>x</var> index (with an arbitrary origin),
     * not necessarily the same value than the zero-based index used in JavaFX views.
     *
     * <p>This field is written by {@link #layoutChildren(double, double, double, double)}.
     * All other accesses (especially from outside of this class) should be read-only.</p>
     */
    int firstVisibleColumn;

    /**
     * Horizontal position in the virtual flow where to start writing the text of the header column.
     * This value changes during horizontal scrolls, even if the cells continue to start at the same
     * visual position on the screen. The position of the column showing {@link #firstVisibleColumn}
     * sample values is {@code leftPosition} + {@link #headerWidth}, and that position is incremented
     * by {@link #cellWidth} for all other columns.
     *
     * <p>This field is written by {@link #layoutChildren(double, double, double, double)}.
     * All other accesses (especially from outside of this class) should be read-only.</p>
     */
    double leftPosition;

    /**
     * Horizontal position where to stop rendering the cells.
     * This is {@link #leftPosition} + the view width.
     *
     * <p>This field is written by {@link #layoutChildren(double, double, double, double)}.
     * All other accesses (especially from outside of this class) should be read-only.</p>
     */
    double rightPosition;

    /**
     * Width of the header column ({@code headerWidth}) and of all other columns ({@code cellWidth}).
     * Must be greater than zero, otherwise infinite loop may happen.
     *
     * <p>This field is written by {@link #layoutChildren(double, double, double, double)}.
     * All other accesses (especially from outside of this class) should be read-only.</p>
     */
    double headerWidth, cellWidth;

    /**
     * Width of the region where to write the text in a cell. Should be equals or slightly smaller
     * than {@link #cellWidth}. We use a smaller width for leaving a small margin between cells.
     *
     * <p>This field is written by {@link #layoutChildren(double, double, double, double)}.
     * All other accesses (especially from outside of this class) should be read-only.</p>
     */
    double cellInnerWidth;

    /**
     * Creates a new skin for the specified view.
     */
    GridViewSkin(final GridView view) {
        super(view);
        final HBox header = new HBox();
        headerRow = header.getChildren();
        /*
         * Main content where sample values will be shown.
         */
        final VirtualFlow<GridRow> flow = getVirtualFlow();
        flow.setCellFactory(GridRow::new);
        flow.setFocusTraversable(true);
        flow.setFixedCellSize(GridView.getSizeValue(view.cellHeight));
        view.cellHeight .addListener(this::cellHeightChanged);
        view.cellWidth  .addListener(this::cellWidthChanged);
        view.headerWidth.addListener(this::cellWidthChanged);
        /*
         * Rectangles for filling the background of the cells in the header row and header column.
         * Those rectangles will be resized and relocated in `layout(…)` method.
         */
        topBackground  = new Rectangle();
        leftBackground = new Rectangle();
        leftBackground.fillProperty().bind(view.headerBackground);
        topBackground .fillProperty().bind(view.headerBackground);
        /*
         * The list of children is initially empty. We need to
         * add the virtual flow, otherwise nothing will appear.
         */
        getChildren().addAll(topBackground, leftBackground, header, flow);
    }

    /**
     * Invoked when the value of {@link GridView#cellHeight} property changed.
     * This method copies the new value into {@link VirtualFlow#fixedCellSizeProperty()} after bounds check.
     */
    private void cellHeightChanged(ObservableValue<? extends Number> property, Number oldValue, Number newValue) {
        final Flow flow = (Flow) getVirtualFlow();
        final double value = newValue.doubleValue();
        flow.setFixedCellSize(value >= GridView.MIN_CELL_SIZE ? value : GridView.MIN_CELL_SIZE);
    }

    /**
     * Invoked when the cell width or header width changed.
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
     * <p>This method is invoked by {@link GridView} when the image has changed,
     * or the band in the image  to show has changed.</p>
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
     * There is two main properties that we want:
     *
     * <ul>
     *   <li>{@link #getHorizontalPosition()} for the position of the horizontal scroll bar.</li>
     *   <li>{@link #getWidth()} for the width of the visible region.
     * </ul>
     *
     * Those two properties are used for creating the minimal amount
     * of {@link GridCell}s needed for rendering the {@link GridRow}.
     */
    static final class Flow extends VirtualFlow<GridRow> implements ChangeListener<Number> {
        /**
         * Creates a new flow for the given view. This method registers listeners
         * on the properties that may require a redrawn of the full view port.
         */
        @SuppressWarnings("ThisEscapedInObjectConstruction")
        Flow(final GridView view) {
            getHbar().valueProperty().addListener(this);
            view.bandProperty.addListener(this);
            view.cellSpacing .addListener(this);
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
         * Returns the height of the view area, not counting the horizontal scroll bar.
         * This height does not include the row header neither, because it is managed by
         * a separated node ({@link #headerRow}).
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
        final GridView view = getSkinnable();
        double cellSpacing  = Math.min(view.cellSpacing.get(), cellWidth);
        if (!(cellSpacing  >= 0)) cellSpacing = 0;          // Use ! for catching NaN (can not use Math.max).
        /*
         * Do layout of the flow first because it may cause scroll bars to appear or disappear,
         * which may change the size calculations done after that. The flow is located below the
         * header row, so we adjust y and height accordingly.
         */
        final Flow    flow         = (Flow) getVirtualFlow();
        final double  headerHeight = flow.getFixedCellSize() + 2*cellSpacing;
        final double  dataY        = y + headerHeight;
        final double  dataHeight   = height - headerHeight;
        final boolean resized      = (flow.getWidth() != width) || (flow.getHeight() != dataHeight);
        flow.resizeRelocate(x, dataY, width, dataHeight);
        /*
         * Recompute all values which will be needed by GridRowSkin. They are mostly information about
         * the horizontal dimension, because the vertical dimension is already managed by VirtualFlow.
         * We compute here for avoiding to recompute the same values in each GridRowSkin instance.
         */
        final double oldPos = leftPosition;
        headerWidth         = GridView.getSizeValue(view.headerWidth);
        cellWidth           = GridView.getSizeValue(view.cellWidth);
        cellInnerWidth      = cellWidth - cellSpacing;
        leftPosition        = flow.getHorizontalPosition();         // Horizontal position in the virtual view.
        rightPosition       = leftPosition + width;                 // Horizontal position where to stop.
        firstVisibleColumn  = (int) (leftPosition / cellWidth);     // Column index in the RenderedImage.
        /*
         * Set the rectangle position before to do final adjustment on cell position,
         * because the background to fill should include the `cellSpacing` margin.
         */
        topBackground .setX(x);                                     // As a matter of principle, but should be zero.
        topBackground .setY(y);
        topBackground .setWidth(width);
        topBackground .setHeight(headerHeight);
        leftBackground.setX(x);
        leftBackground.setY(dataY);
        leftBackground.setWidth(headerWidth);
        leftBackground.setHeight(flow.getVisibleHeight());
        if (cellSpacing < headerWidth) {
            headerWidth  -= cellSpacing;
            leftPosition += cellSpacing;
        }
        /*
         * Reformat the row header if its content changed. It may be because a horizontal scroll has been
         * detected (in which case values changed), or because the view size changed (in which case cells
         * may need to be added or removed).
         */
        if (resized || oldPos != leftPosition) {
            final int count   = headerRow.size();
            final int missing = (int) Math.ceil((width - headerWidth) / cellWidth) - count;
            if (missing != 0) {
                if (missing < 0) {
                    headerRow.remove(missing + count, count);       // Too many children. Remove the extra ones.
                } else {
                    final GridCell[] more = new GridCell[missing];
                    for (int i=0; i<missing; i++) {
                        more[i] = new GridCell();
                    }
                    headerRow.addAll(more);             // Single addAll(…) operation for sending only one event.
                }
            }
            double pos = x + headerWidth;
            int column = firstVisibleColumn;
            for (final Node cell : headerRow) {
                ((GridCell) cell).setText(view.formatHeaderValue(column++, false));
                layoutInArea(cell, pos, y, cellWidth, headerHeight, 0, HPos.CENTER, VPos.CENTER);
                pos += cellWidth;
            }
        }
    }
}