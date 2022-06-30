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
import javafx.scene.Cursor;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.control.skin.VirtualContainerBase;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Font;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import org.apache.sis.internal.gui.MouseDrags;
import org.apache.sis.internal.gui.Styles;


/**
 * The {@link GridView} renderer as a virtualized and scrollable content.
 * The primary direction of virtualization is vertical (rows will stack vertically on top of each other).
 *
 * <p>Relationships:</p>
 * <ul>
 *   <li>This is created by {@link GridView#createDefaultSkin()}.</li>
 *   <li>The {@link GridView} which own this skin is given by {@link #getSkinnable()}.</li>
 *   <li>This {@code GridViewSkin} contains an arbitrary amount of {@link GridRow} children.
 *       It should be limited to the number of children that are visible at the same time,
 *       not the total number of rows in the image.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
final class GridViewSkin extends VirtualContainerBase<GridView, GridRow> implements EventHandler<MouseEvent> {
    /**
     * The cells that we put in the header row on the top of the view. The children list is initially empty;
     * new elements are added or removed when first needed and when the view size changed.
     */
    private final HBox headerRow;

    /**
     * Background of the header row (top side) and header column (left side) of the view.
     */
    private final Rectangle topBackground, leftBackground;

    /**
     * Zero-based index of the first column visible in the view, ignoring the header column.
     * This is equal to the {@link RenderedImage} <var>x</var> index if the image coordinates
     * also start at zero (i.e. {@link RenderedImage#getMinX()} = 0).
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
     * Width of the region where to write the text in a cell. Should be equal or slightly smaller
     * than {@link #cellWidth}. We use a smaller width for leaving a small margin between cells.
     *
     * <p>This field is written by {@link #layoutChildren(double, double, double, double)}.
     * All other accesses (especially from outside of this class) should be read-only.</p>
     */
    double cellInnerWidth;

    /**
     * Whether a new image has been set, in which case we should recompute everything
     * including the labels in header row.
     */
    private boolean layoutAll;

    /**
     * Whether the grid view contains at least one tile that we failed to fetch.
     */
    private boolean hasErrors;

    /**
     * A rectangle around selected the cell in the content area or in the row/column header.
     */
    private final Rectangle selection, selectedRow, selectedColumn;

    /**
     * {@code true} if a drag event is in progress.
     *
     * @see #onDrag(MouseEvent)
     */
    private boolean isDragging;

    /**
     * Cursor position at the time of previous pan event.
     * This is used for computing the translation to apply during drag events.
     *
     * @see #onDrag(MouseEvent)
     */
    private double xPanPrevious, yPanPrevious;

    /**
     * Creates a new skin for the specified view.
     */
    GridViewSkin(final GridView view) {
        super(view);
        headerRow = new HBox();
        /*
         * Main content where sample values will be shown.
         */
        final VirtualFlow<GridRow> flow = getVirtualFlow();
        flow.setCellFactory(GridRow::new);
        flow.setFocusTraversable(true);
        flow.setFixedCellSize(GridView.getSizeValue(view.cellHeight));
        view.cellHeight .addListener((p,o,n) -> cellHeightChanged(n));
        view.cellWidth  .addListener((p,o,n) -> cellWidthChanged(n, true));
        view.headerWidth.addListener((p,o,n) -> cellWidthChanged(n, false));
        /*
         * Rectangles for filling the background of the cells in the header row and header column.
         * Those rectangles will be resized and relocated by the `layout(…)` method.
         */
        topBackground  = new Rectangle();
        leftBackground = new Rectangle();
        leftBackground.fillProperty().bind(view.headerBackground);
        topBackground .fillProperty().bind(view.headerBackground);
        /*
         * Rectangle around the selected cell (for example the cell below mouse position).
         * Become visible only when the mouse enter in the widget area. The rectangles are
         * declared unmanaged for avoiding relayout of the whole widget every time that a
         * rectangle position changed.
         */
        selection      = new Rectangle();
        selectedRow    = new Rectangle();
        selectedColumn = new Rectangle();
        selection     .setFill(Styles.SELECTION_BACKGROUND);
        selectedRow   .setFill(Color.SILVER);
        selectedColumn.setFill(Color.SILVER);
        selection     .setVisible(false);
        selectedRow   .setVisible(false);
        selectedColumn.setVisible(false);
        selection     .setManaged(false);
        selectedRow   .setManaged(false);
        selectedColumn.setManaged(false);
        flow.setOnMouseExited((e) -> hideSelection());
        /*
         * The list of children is initially empty. We need to add the virtual flow
         * (together with headers, selection, etc.), otherwise nothing will appear.
         */
        getChildren().addAll(topBackground, leftBackground, selectedColumn,
                             selectedRow, headerRow, selection, flow);
        /*
         * Keyboard and drag events for moving the viewed bounds.
         */
        view.addEventHandler(KeyEvent.KEY_PRESSED, this::onKeyTyped);
        MouseDrags.setHandlers(view, this::onDrag);
    }

    /**
     * Invoked when the mouse is moving over the cells. This method computes cell indices
     * and draws the selection rectangle around that cell. Then, listeners are notified.
     *
     * <p>This listener is registered for each {@link GridRow} instances.
     * It is not designed for other kinds of event source.</p>
     *
     * @see #onDrag(MouseEvent)
     */
    @Override
    public final void handle(final MouseEvent event) {
        double x = event.getX() - (leftPosition + headerWidth);
        boolean visible = (x >= 0);
        if (visible) {
            final double column = Math.floor(x / cellWidth);
            visible = (column >= 0);
            if (visible) {
                final GridRow row = (GridRow) event.getSource();
                double y = row.getLayoutY();
                visible = y < ((Flow) getVirtualFlow()).getVisibleHeight();
                if (visible) {
                    x  = column * cellWidth + leftBackground.getWidth() + row.getLayoutX();
                    y += topBackground.getHeight();
                    selection.setX(x);                  // NOT equivalent to `relocate(x,y)`.
                    selection.setY(y);
                    selectedRow.setY(y);
                    selectedColumn.setX(x);
                    getSkinnable().formatCoordinates(firstVisibleColumn + (int) column, row.getIndex());
                }
            }
        }
        selection     .setVisible(visible);
        selectedRow   .setVisible(visible);
        selectedColumn.setVisible(visible);
        if (!visible) {
            getSkinnable().hideCoordinates();
        }
    }

    /**
     * Invoked when the user presses the button, drags the grid and releases the button.
     * The position of the selection rectangles become invalid has a result of the drag.
     * Instead of bothering to adjust it, we hide it. It may also be less confusing for
     * the user, because it shows that we are not exploring values of different cells.
     *
     * @see #handle(MouseEvent)
     */
    private void onDrag(final MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            final double x = event.getX() - leftBackground.getWidth();
            final double y = event.getY() - topBackground.getHeight();
            final Flow flow = (Flow) getVirtualFlow();
            if (x >= 0 && y >= 0 && y < flow.getVisibleHeight()) {
                final GridView view = getSkinnable();
                final EventType<? extends MouseEvent> type = event.getEventType();
                if (type == MouseEvent.MOUSE_PRESSED) {
                    view.setCursor(Cursor.CLOSED_HAND);
                    view.requestFocus();
                    xPanPrevious = x;
                    yPanPrevious = y;
                    isDragging = true;
                    hideSelection();
                } else if (isDragging) {
                    if (type == MouseEvent.MOUSE_RELEASED) {
                        view.setCursor(Cursor.DEFAULT);
                        isDragging = false;
                    }
                    xPanPrevious -= flow.scrollHorizontal(xPanPrevious - x);
                    yPanPrevious -= flow.scrollPixels(yPanPrevious - y);
                }
                event.consume();
            }
        }
    }

    /**
     * Invoked when the user presses a key. This handler provides navigation in the direction of arrow keys.
     * The selection rectangles are hidden because otherwise the user may be surprised to see the whole grid
     * scrolling instead of the selection rectangle moving.
     */
    private void onKeyTyped(final KeyEvent event) {
        double tx=0, ty=0;
        switch (event.getCode()) {
            case RIGHT: case KP_RIGHT: tx =  1; break;
            case LEFT:  case KP_LEFT:  tx = -1; break;
            case DOWN:  case KP_DOWN:  ty = +1; break;
            case UP:    case KP_UP:    ty = -1; break;
            default: return;
        }
        if (event.isShiftDown()) {
            tx *= 10;
            ty *= 10;
        }
        final Flow flow = (Flow) getVirtualFlow();
        flow.scrollPixels(flow.getFixedCellSize() * ty);
        flow.scrollHorizontal(cellWidth * tx);
        hideSelection();
        event.consume();
    }

    /**
     * Hides the selection when the mouse moved outside the grid view area,
     * or when a drag or scrolling action is performed.
     */
    private void hideSelection() {
        selection     .setVisible(false);
        selectedRow   .setVisible(false);
        selectedColumn.setVisible(false);
        getSkinnable().hideCoordinates();
    }

    /**
     * Invoked when the value of {@link GridView#cellHeight} property changed.
     * This method copies the new value into {@link VirtualFlow#fixedCellSizeProperty()} after bounds check.
     */
    private void cellHeightChanged(Number newValue) {
        final Flow flow = (Flow) getVirtualFlow();
        double value = newValue.doubleValue();
        if (!(value >= GridView.MIN_CELL_SIZE)) {           // Use ! for catching NaN values.
            value = GridView.MIN_CELL_SIZE;
        }
        flow.setFixedCellSize(value);
        selection.setVisible(false);
        selection.setHeight(value);
        contentChanged(false);
    }

    /**
     * Invoked when the cell width or header width changed.
     * This method notifies all children about the new width.
     *
     * @param  cell  {@code true} if modifying the width of cells, or
     *               {@code false} if modifying the width of headers.
     */
    private void cellWidthChanged(Number newValue, final boolean cell) {
        final GridView view = getSkinnable();
        final double width = view.getContentWidth();
        for (final Node child : getChildren()) {
            if (child instanceof GridRow) {             // The first instances are not a GridRow.
                ((GridRow) child).setPrefWidth(width);
            }
        }
        if (cell) {
            selection.setVisible(false);
            selection.setWidth(newValue.doubleValue());
        }
        layoutAll = true;
        contentChanged(false);
    }

    /**
     * Invoked when an error occurred while fetching a tile. The given {@link GridError} node is added as last
     * child (i.e. will be drawn on top of everything else). That child will be removed if a new image is set.
     */
    final void errorOccurred(final GridError error) {
        hasErrors = true;
        getChildren().add(error);
    }

    /**
     * Removes the given error. This method is invoked when the user wants to try again to fetch a tile.
     * Callers is responsible for invoking {@link GridTile#clear()}.
     */
    final void removeError(final GridError error) {
        final ObservableList<Node> children = getChildren();
        children.remove(error);
        // The list should never be empty, so IndexOutOfBoundsException here would be a bug.
        hasErrors = children.get(children.size() - 1) instanceof GridError;
        contentChanged(false);
    }

    /**
     * Invoked when the content may have changed. If {@code all} is {@code true}, then everything
     * may have changed including the number of rows and columns. If {@code all} is {@code false}
     * then the number of rows and columns is assumed the same.
     *
     * <p>This method is invoked by {@link GridView} when the image has changed ({@code all=true}),
     * or the band in the image to show has changed ({@code all=false}).</p>
     *
     * @see GridView#contentChanged(boolean)
     */
    final void contentChanged(final boolean all) {
        if (all) {
            updateItemCount();
            getChildren().removeIf((node) -> (node instanceof GridError));
            layoutAll = true;
            hasErrors = false;
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
    private static final class Flow extends VirtualFlow<GridRow> implements ChangeListener<Number> {
        /**
         * Creates a new flow for the given view. This method registers listeners
         * on the properties that may require a redrawn of the full view port.
         */
        @SuppressWarnings("ThisEscapedInObjectConstruction")
        Flow(final GridView view) {
            setPannable(false);         // We will use our own pan listeners.
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
         * Attempts to scroll horizontally the view by the given amount of pixels.
         *
         * @param  delta  the amount of pixels to scroll.
         * @return the number of pixels actually moved.
         */
        final double scrollHorizontal(final double delta) {
            final ScrollBar bar = getHbar();
            final double previous = bar.getValue();
            final double value = Math.max(bar.getMin(), Math.min(bar.getMax(), previous + delta));
            bar.setValue(value);
            return value - previous;
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
        final Flow   flow         = (Flow) getVirtualFlow();
        final double cellHeight   = flow.getFixedCellSize();
        final double headerHeight = cellHeight + 2*cellSpacing;
        final double dataY        = y + headerHeight;
        final double dataHeight   = height - headerHeight;
        layoutAll |= (flow.getWidth() != width) || (flow.getHeight() != dataHeight);
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
        firstVisibleColumn  = (int) (leftPosition / cellWidth);     // Zero-based column index in the image.
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
        selection     .setWidth (cellWidth);
        selectedRow   .setWidth (headerWidth);
        selectedColumn.setWidth (cellWidth);
        selection     .setHeight(cellHeight);
        selectedRow   .setHeight(cellHeight);
        selectedColumn.setHeight(headerHeight);
        selectedRow   .setX(x);
        selectedColumn.setY(y);
        if (cellSpacing < headerWidth) {
            headerWidth  -= cellSpacing;
            leftPosition += cellSpacing;
        }
        /*
         * Reformat the row header if its content changed. It may be because a horizontal scroll has been
         * detected (in which case values changed), or because the view size changed (in which case cells
         * may need to be added or removed).
         */
        if (layoutAll || oldPos != leftPosition) {
            layoutInArea(headerRow, x, y, width, headerHeight,
                         Node.BASELINE_OFFSET_SAME_AS_HEIGHT, HPos.LEFT, VPos.TOP);
            final ObservableList<Node> children = headerRow.getChildren();
            final int count   = children.size();
            final int missing = (int) Math.ceil((width - headerWidth) / cellWidth) - count;
            if (missing != 0) {
                if (missing < 0) {
                    children.remove(missing + count, count);        // Too many children. Remove the extra ones.
                } else {
                    final GridCell[] more = new GridCell[missing];
                    final Font font = Font.font(null, FontWeight.BOLD, -1);
                    for (int i=0; i<missing; i++) {
                        final GridCell cell = new GridCell();
                        cell.setFont(font);
                        more[i] = cell;
                    }
                    children.addAll(more);             // Single addAll(…) operation for sending only one event.
                }
            }
            double pos = x + headerWidth;
            int column = firstVisibleColumn;
            for (final Node cell : children) {
                ((GridCell) cell).setText(view.formatHeaderValue(column++, false));
                layoutInArea(cell, pos, y, cellWidth, headerHeight, Node.BASELINE_OFFSET_SAME_AS_HEIGHT, HPos.RIGHT, VPos.CENTER);
                pos += cellWidth;
            }
            /*
             * For a mysterious reason, all row headers except the first one (0) are invisible on the first time
             * that the grid is shown. I have been unable to identify the reason; all `GridCell` are created and
             * received a non-empty text string. Doing a full layout again makes them appear. So as a workaround
             * we request the next layout to be full again if it seems that we have done the initial layout. The
             * very first layout create one cell (count = 0 & missing = 1), the next layout create missing cells
             * (count = 1 & missing = 18) — this is where we want to force a third layout — then the third layout
             * is stable (count = 19 & missing = 0).
             */
            layoutAll = (count <= missing);
        }
        /*
         * Update position of the highlights at mouse cursor position. Usually the correction computed below is
         * zero and this block does not change any position (but it may change the geographic coordinates shown
         * in status bar). However if the user was scrolling and reached the end of the virtial flow, the last
         * scrolling action may have caused a displacement which is a fractional amount of cells, in which case
         * the highlights appear misaligned if we do not apply the correction below.
         */
        if (selection.isVisible()) {
            GridRow row = flow.getFirstVisibleCell();
            if (row != null) {
                double sy = selection.getY() - (row.getLayoutY() + headerHeight);
                final int i = ((int) Math.rint(sy / cellHeight)) + row.getIndex();
                row = flow.getCell(i);                      // Empty cell if beyond the range.
                sy  = row.getLayoutY() + headerHeight;      // Usually same as `selection.y` (see above comment).
                final double offset = row.getLayoutX() + leftBackground.getWidth();
                final double column = Math.rint((selection.getX() - offset) / cellWidth);
                final double sx     = column * cellWidth + offset;
                selection.setX(sx);
                selection.setY(sy);
                selectedRow.setY(sy);
                selectedColumn.setX(sx);
                getSkinnable().formatCoordinates(firstVisibleColumn + (int) column, i);
            }
        }
        if (hasErrors) {
            computeErrorBounds(flow);
        }
    }

    /**
     * If an error exists somewhere, computes as estimation of the visible region
     * as zero-based column and row indices. We use an AWT rectangle instead of
     * JavaFX object because this rectangle will be intersected with AWT rectangle.
     */
    private void computeErrorBounds(final Flow flow) {
        final java.awt.Rectangle viewArea = new java.awt.Rectangle();
        final GridRow first = flow.getFirstVisibleCell();
        int firstVisibleRow = 0;
        if (first != null) {
            viewArea.x      = firstVisibleColumn;
            viewArea.y      = firstVisibleRow = first.getIndex();
            viewArea.width  = (int) ((flow.getWidth() - leftBackground.getWidth()) / cellWidth);
            viewArea.height = (int) (flow.getVisibleHeight() / flow.getFixedCellSize());
        }
        final ObservableList<Node> children = getChildren();
        for (int i=children.size(); --i >= 0;) {
            final Node node = children.get(i);
            if (!(node instanceof GridError)) break;
            final GridError error = (GridError) node;
            boolean visible = false;
            final java.awt.Rectangle area = error.getVisibleRegion(viewArea);
            if (!area.isEmpty()) {
                final double cellHeight = flow.getFixedCellSize();
                final double width  = area.width  * cellWidth;
                final double height = area.height * cellHeight;
                layoutInArea​(error,
                        cellWidth  * (area.x - firstVisibleColumn) + leftBackground.getWidth(),
                        cellHeight * (area.y - firstVisibleRow)    + topBackground.getHeight(),
                        width, height, Node.BASELINE_OFFSET_SAME_AS_HEIGHT, HPos.CENTER, VPos.CENTER);
                /*
                 * If after layout the error message size appears too large for the remaining space, hide it.
                 * The intent is to avoid having the message and buttons on top of cell values in valid tiles.
                 * It does not seem to work fully however (some overlaps can still happen), so we added some
                 * inset to mitigate the problem and also for more aerated presentation.
                 */
                visible = error.getHeight() <= height && error.getWidth() <= width;
            }
            error.setVisible(visible);
        }
    }
}
