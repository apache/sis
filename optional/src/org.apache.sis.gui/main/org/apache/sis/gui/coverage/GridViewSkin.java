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

import java.util.List;
import java.util.Arrays;
import java.awt.image.RenderedImage;
import javafx.geometry.Orientation;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Cursor;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SkinBase;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import org.apache.sis.gui.internal.MouseDrags;
import org.apache.sis.gui.internal.Styles;
import org.apache.sis.util.internal.shared.Numerics;


/**
 * The {@link GridView} renderer as a scrollable content.
 * This is created by {@link GridView#createDefaultSkin()}.
 * The {@link GridView} which owns this skin is given by {@link #getSkinnable()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class GridViewSkin extends SkinBase<GridView> implements EventHandler<MouseEvent> {
    /**
     * Margin to add to the header row or header column.
     */
    private static final int HEADER_MARGIN = 9;

    /**
     * Minimum cell width and height. Must be greater than zero, otherwise infinite loops may happen.
     *
     * @see #toValidCellSize(double)
     */
    private static final int MIN_CELL_SIZE = 4;

    /**
     * Minimal width and height of the error box for showing it.
     */
    private static final int MIN_ERROR_BOX_SIZE = 140;

    /**
     * The cells that we put in the header row on the top of the view.
     * The length of this array is the number of columns that are visible.
     * This array is recreated when the view's width changed.
     *
     * @see #topBackground
     */
    private Text[] headerRow;

    /**
     * The cells that we put in the header column on the left of the view.
     * The length of this array is the number of rows that are visible.
     * This array is recreated when the view's height changed.
     *
     * @see #leftBackground
     */
    private Text[] headerColumn;

    /**
     * All grid cells (row major) containing the pixel values of the selected band.
     * The length of this array shall be {@code headerRow.length * headerColumn.length}.
     * This array is recreated when the view's size changed.
     *
     * @see #valuesRegionX()
     * @see #valuesRegionY()
     */
    private Text[] valueCells;

    /**
     * Background of the header row (top side) of the view. The bottom coordinate of this
     * rectangle is the <var>y</var> coordinate where to start drawing the value cells.
     *
     * @see #headerRow
     * @see #valuesRegionY()
     */
    private final Rectangle topBackground;

    /**
     * Background of the header header column (left side) of the view. The right coordinate of
     * this rectangle is the <var>x</var> coordinate where to start drawing the value cells.
     *
     * @see #headerColumn
     * @see #valuesRegionX()
     */
    private final Rectangle leftBackground;

    /**
     * Width of all columns other than the header column.
     * This is the {@link GridView#cellWidth} value forced to at least {@value #MIN_CELL_SIZE}.
     */
    private double cellWidth;

    /**
     * Height of all cells other than the header row.
     * This is the {@link GridView#cellHeight} value forced to at least {@value #MIN_CELL_SIZE}.
     */
    private double cellHeight;

    /**
     * A rectangle behind the selected cell in the content area or in the row/column header.
     * Used to highlight which cells is below the mouse cursor.
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
     * The coordinate units are related to {@link RenderedImage} coordinates.
     * This is used for computing the translation to apply during drag events.
     *
     * @see #onDrag(MouseEvent)
     */
    private double xPanPrevious, yPanPrevious;

    /**
     * Horizontal and vertical scroll bars. Units are {@link RenderedImage} pixel coordinates.
     * The minimum value is the minimum image pixel coordinate, which is not necessarily zero.
     */
    private final ScrollBar xScrollBar, yScrollBar;

    /**
     * Range if indexes of the children which is an instance of {@link GridError}.
     */
    private int indexOfFirstError, indexAfterLastError;

    /**
     * The clip to apply on the view.
     */
    private final Rectangle clip;

    /**
     * Creates a new skin for the specified view.
     */
    GridViewSkin(final GridView view) {
        super(view);
        headerRow = new Text[0];
        headerColumn = headerRow;
        valueCells   = headerRow;
        /*
         * Main content where sample values will be shown.
         */
        cellWidth  = toValidCellSize(view.cellWidth.get());
        cellHeight = toValidCellSize(view.cellHeight.get());
        /*
         * Rectangles for filling the background of the cells in the header row and header column.
         * Those rectangles will be resized and relocated by the `layoutChildren(…)` method.
         */
        final double valuesRegionX = toValidCellSize(view.headerWidth.get());
        final double valuesRegionY = cellHeight + HEADER_MARGIN;     // No independent property yet.
        leftBackground = new Rectangle(valuesRegionX, 0);
        topBackground  = new Rectangle(0, valuesRegionY);
        /*
         * Rectangle around the selected cell (for example the cell below mouse position).
         * They become visible only when the mouse enter in the widget area.
         */
        selection      = new Rectangle(valuesRegionX, valuesRegionY, cellWidth, cellHeight);
        selectedRow    = new Rectangle(0, valuesRegionY, valuesRegionX, cellHeight);
        selectedColumn = new Rectangle(valuesRegionX, 0, cellWidth, valuesRegionY);
        selection     .setFill(Styles.SELECTION_BACKGROUND);
        selectedRow   .setFill(Color.SILVER);
        selectedColumn.setFill(Color.SILVER);
        selection     .setVisible(false);
        selectedRow   .setVisible(false);
        selectedColumn.setVisible(false);
        /*
         * Scroll bars.
         */
        xScrollBar = new ScrollBar();
        yScrollBar = new ScrollBar();
        yScrollBar.setOrientation(Orientation.VERTICAL);
        view.setClip(clip = new Rectangle());
    }

    /**
     * Registers the listeners. This is done outside the constructor because this
     * method creates references from {@link GridView} to this {@code GridViewSkin}.
     */
    @Override
    public void install() {
        super.install();
        xScrollBar.valueProperty().addListener((p,o,n) -> positionChanged(o, n, false));
        yScrollBar.valueProperty().addListener((p,o,n) -> positionChanged(o, n, true));

        final GridView view = getSkinnable();
        topBackground .fillProperty().bind(view.headerBackground);
        leftBackground.fillProperty().bind(view.headerBackground);
        view.headerWidth.addListener((p,o,n) -> cellSizeChanged(o, n, HEADER_PROPERTY));
        view.cellWidth  .addListener((p,o,n) -> cellSizeChanged(o, n, WIDTH_PROPERTY));
        view.cellHeight .addListener((p,o,n) -> cellSizeChanged(o, n, HEIGHT_PROPERTY));
        view.addEventHandler(KeyEvent.KEY_PRESSED, this::onKeyTyped);
        view.setOnMouseExited((e) -> hideSelection());
        view.setOnMouseMoved(this);
        MouseDrags.setHandlers(view, this::onDrag);
    }

    /**
     * Removes the listeners installed by {@link #install()}.
     *
     * <h4>Limitations</h4>
     * We don't have an easy way to remove the listeners on properties.
     * But it should not be an issue, because this method is defined mostly as
     * a matter of principle since we don't expect users to remove this skin.
     */
    @Override
    public void dispose() {
        final GridView view = getSkinnable();
        topBackground .fillProperty().unbind();
        leftBackground.fillProperty().unbind();
        view.setOnMouseExited(null);
        view.setOnMouseMoved(null);
        MouseDrags.setHandlers(view, null);
        super.dispose();
    }

    /**
     * Invoked when the mouse is moving over the cells. This method computes cell indices
     * and draws the selection rectangle around that cell. Then, listeners are notified.
     *
     * @see #onDrag(MouseEvent)
     */
    @Override
    public final void handle(final MouseEvent event) {
        final double valuesRegionX = valuesRegionX();
        double x = (event.getX() - valuesRegionX) / cellWidth;
        boolean visible = (x >= 0 && x < headerRow.length);
        if (visible) {
            final double valuesRegionY = valuesRegionY();
            double y = (event.getY() - valuesRegionY) / cellHeight;
            visible = (y >= 0 && y < headerColumn.length);
            if (visible) {
                final double xminOfValues = xScrollBar.getValue();
                final double yminOfValues = yScrollBar.getValue();
                x = Math.floor(x + xminOfValues);
                y = Math.floor(y + yminOfValues);
                final double xpos = valuesRegionX + (x - xminOfValues) * cellWidth;
                final double ypos = valuesRegionY + (y - yminOfValues) * cellHeight;
                selection.setX(xpos);      // NOT equivalent to `relocate(x,y)`.
                selection.setY(ypos);
                selectedRow.setY(ypos);
                selectedColumn.setX(xpos);
                final GridControls controls = getSkinnable().controls;
                if (controls != null) {
                    controls.status.setLocalCoordinates(x, y);
                }
            }
        }
        selection.setVisible(visible);
        selectedRow.setVisible(visible);
        selectedColumn.setVisible(visible);
        if (!visible) {
            getSkinnable().hideCoordinates();
        }
    }

    /**
     * Invoked when the user presses the button, drags the grid and releases the button.
     *
     * @see #handle(MouseEvent)
     */
    private void onDrag(final MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            final double x = (event.getX() - valuesRegionX()) / cellWidth;
            if (x >= 0 && x < headerRow.length) {
                final double y = (event.getY() - valuesRegionY()) / cellHeight;
                if (y >= 0 && y < headerColumn.length) {
                    final GridView view = getSkinnable();
                    final EventType<? extends MouseEvent> type = event.getEventType();
                    if (type == MouseEvent.MOUSE_PRESSED) {
                        view.setCursor(Cursor.CLOSED_HAND);
                        view.requestFocus();
                        isDragging = true;
                    } else if (isDragging) {
                        if (type == MouseEvent.MOUSE_RELEASED) {
                            view.setCursor(Cursor.DEFAULT);
                            isDragging = false;
                        }
                        shift(xScrollBar, xPanPrevious - x, false);
                        shift(yScrollBar, yPanPrevious - y, false);
                    } else {
                        return;
                    }
                    xPanPrevious = x;
                    yPanPrevious = y;
                    event.consume();
                }
            }
        }
    }

    /**
     * Invoked when the user presses a key. This handler provides navigation in the direction of arrow keys.
     * The selection rectangles are hidden because otherwise the user may be surprised to see the whole grid
     * scrolling instead of the selection rectangle moving.
     */
    private void onKeyTyped(final KeyEvent event) {
        int tx=0, ty=0;
        switch (event.getCode()) {
            case RIGHT: case KP_RIGHT: tx =  1; break;
            case LEFT:  case KP_LEFT:  tx = -1; break;
            case DOWN:  case KP_DOWN:  ty = +1; break;
            case UP:    case KP_UP:    ty = -1; break;
            case PAGE_DOWN: ty = Math.max(headerColumn.length - 3,  1); break;
            case PAGE_UP:   ty = Math.min(3 - headerColumn.length, -1); break;
            default: return;
        }
        if (tx != 0) shift(xScrollBar, tx, true);
        if (ty != 0) shift(yScrollBar, ty, true);
        event.consume();
    }

    /**
     * Increments or decrements the value of the given scroll bar by the given amount.
     *
     * @param  bar    the scroll bar for which to modify the position.
     * @param  shift  the increment to add, in units of {@link RenderedImage} coordinates.
     */
    private static void shift(final ScrollBar bar, final double shift, final boolean snap) {
        double value = bar.getValue() + shift;
        if (snap) {
            value = Math.rint(value);
        }
        bar.setValue(Math.max(bar.getMin(), Math.min(bar.getMax(), value)));
    }

    /**
     * Invoked when the scroll bar changed its position. This is not only in reaction to direct interaction
     * with the scroll bar, but may also be in response to a key pressed on the keyboard or a drag event.
     */
    private void positionChanged(final Number oldValue, final Number newValue, final boolean vertical) {
        final double shift = newValue.doubleValue() - oldValue.doubleValue();
        if (vertical) {
            final double ypos = selection.getY() - shift * cellHeight;
            selection.setY(ypos);
            selectedRow.setY(ypos);
        } else {
            final double xpos = selection.getX() - shift * cellWidth;
            selection.setX(xpos);
            selectedColumn.setX(xpos);
        }
        updateCellValues();
    }

    /**
     * Identification of which size property has changed.
     * Used as argument in {@link #cellSizeChanged(Number, int)}.
     */
    private static final int WIDTH_PROPERTY = 0, HEIGHT_PROPERTY = 1, HEADER_PROPERTY = 2;

    /**
     * Returns the given value inside the range expected by this class for cell values.
     * We use this method instead of {@link Math#max(double, double)} because we want
     * {@link Double#NaN} values to be replaced by {@value #MIN_CELL_SIZE}.
     */
    private static double toValidCellSize(final double value) {
        return (value >= MIN_CELL_SIZE) ? value : MIN_CELL_SIZE;
    }

    /**
     * Invoked when a cell width or cell height changed.
     *
     * @param  oldValue  the old cell width or cell height.
     * @param  newValue  the new cell width or cell height.
     * @param  property  one of the {@code *_PROPERTY} constants.
     */
    private void cellSizeChanged(final Number oldValue, final Number newValue, final int property) {
        double value = toValidCellSize(newValue.doubleValue());
        switch (property) {
            case HEADER_PROPERTY: {
                leftBackground.setWidth(value);
                selectedRow.setWidth(value);
                selectedColumn.setX(value);
                break;
            }
            case WIDTH_PROPERTY: {
                cellWidth = value;
                selection.setWidth(value);
                selectedColumn.setWidth(value);
                if (value > oldValue.doubleValue()) {
                    // Maybe there is enough space for the full pattern now.
                    getSkinnable().cellFormat.restorePattern();
                }
                break;
            }
            case HEIGHT_PROPERTY: {
                cellHeight = value;
                selection.setHeight(value);
                selectedRow.setHeight(value);
                value += HEADER_MARGIN;     // No independent property yet.
                selectedColumn.setHeight(value);
                topBackground.setWidth(value);
                break;
            }
        }
        hideSelection();
        getSkinnable().requestLayout();
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
     * Invoked when an error occurred while fetching a tile. The given {@link GridError}
     * node will be added after the value cells, in order to be drawn on top of them.
     * That child will be removed if a new image is set.
     */
    final void errorOccurred(final GridError error) {
        getChildren().add(indexAfterLastError, error);
        indexAfterLastError++;  // Increment only after success.
    }

    /**
     * Returns all {@link GridError} instances. This is a view over the children of this skin.
     */
    private List<Node> errors() {
        return getChildren().subList(indexOfFirstError, indexAfterLastError);
    }

    /**
     * Removes the given error. This method is invoked when the user wants to try again to fetch a tile.
     * Callers is responsible for invoking {@link GridTile#clear()}.
     */
    final void removeError(final GridError error) {
        if (errors().remove(error)) {
            indexAfterLastError--;
        }
    }

    /**
     * Removes all {@link GridError} instances.
     */
    final void clear() {
        errors().clear();
    }

    /**
     * Resizes the given array of cells. If the array become longer, new labels are created.
     * This is a helper method for {@link #layoutChildren(double, double, double, double)}.
     *
     * @param  cells   the array to resize.
     * @param  count   the desired number of elements.
     * @param  header  whether the cells are for a header row or column.
     * @return the resized array.
     */
    private static Text[] resize(Text[] cells, final int count, final boolean header) {
        int i = cells.length;
        if (i != count) {
            cells = Arrays.copyOf(cells, count);
            if (count > i) {
                final Font font = header ? Font.font(null, FontWeight.BOLD, -1) : null;
                do {
                    final var cell = new Text();
                    if (header) {
                        cell.setFont(font);
                    }
                    cells[i] = cell;
                } while (++i < count);
            }
        }
        return cells;
    }

    /**
     * Returns the leftmost coordinate where value cells are rendered. This is in units of the
     * coordinates given to the {@link #layoutChildren(double, double, double, double)} method,
     * with {@code xmin} assumed to be zero.
     */
    private double valuesRegionX() {
        return leftBackground.getWidth();
    }

    /**
     * Returns the topmost coordinate where value cells are rendered. This is in units of the
     * coordinates given to the {@link #layoutChildren(double, double, double, double)} method,
     * with {@code ymin} assumed to be zero.
     */
    private double valuesRegionY() {
        return topBackground.getHeight();
    }

    /**
     * Called during the layout pass of the scene graph. The (x,y) coordinates are usually zero
     * and the (width, height) are the size of the control as shown (not the full content size).
     * Current implementation assume that the visible part is the given size.
     *
     * <h4>Assumptions</h4>
     * This implementation ignores {@code xmin} and {@code ymin} on the assumption that they are always zero.
     * If this assumption is false, we need to add those values in pretty much everything that set a position
     * in this class.
     */
    @Override
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    protected void layoutChildren(final double xmin, final double ymin, final double width, final double height) {
        // Do not invoke `super.layoutChildren(…)` because we manage all children outselves.
        clip.setX(xmin);
        clip.setY(xmin);
        clip.setWidth(width);
        clip.setHeight(height);

        final double sy = height - Styles.SCROLLBAR_HEIGHT;
        xScrollBar.resizeRelocate(0, sy, width, Styles.SCROLLBAR_HEIGHT);
        yScrollBar.resizeRelocate(width - Styles.SCROLLBAR_WIDTH, 0, Styles.SCROLLBAR_WIDTH, sy);
        leftBackground.setHeight(height);
        topBackground .setWidth (width);

        final double valuesRegionX = valuesRegionX();
        final double valuesRegionY = valuesRegionY();
        final int nx = Math.max(0, (int) Math.ceil((width  - valuesRegionX - Styles.SCROLLBAR_WIDTH)  / cellWidth  + 1));
        final int ny = Math.max(0, (int) Math.ceil((height - valuesRegionY - Styles.SCROLLBAR_HEIGHT) / cellHeight + 1));
        final int n  = Math.multiplyExact(nx, ny);

        // Intentionally use `GridError` instead of `Node` for detecting errors.
        final GridError[] errors = errors().toArray(GridError[]::new);
        final Text[] headerRow    = resize(this.headerRow,    nx, true);
        final Text[] headerColumn = resize(this.headerColumn, ny, true);
        final Text[] valueCells   = resize(this.valueCells,   n, false);
        final Node[] all = new Node[headerRow.length + headerColumn.length + valueCells.length + errors.length + 7];

        // Order matter: nodes added last will hide nodes added first.
        int i = 0;
        all[i++] = selection;
        System.arraycopy(valueCells, 0, all, i, n);
        final int indexOfFirstError = i += n;
        System.arraycopy(errors, 0, all, i, errors.length);
        final int indexAfterLastError = i += errors.length;
        all[i++] = topBackground;
        all[i++] = selectedColumn;
        all[i++] = leftBackground;
        all[i++] = selectedRow;
        System.arraycopy(headerRow,    0, all, i, nx); i += nx;
        System.arraycopy(headerColumn, 0, all, i, ny); i += ny;
        all[i++] = xScrollBar;
        all[i++] = yScrollBar;
        if (i != all.length) {
            throw new AssertionError(i);
        }
        /*
         * It is important to ensure that all nodes are unmanaged, otherwise adding the nodes
         * causes a new layout attempt, which causes infinite enqueued calls of this method
         * in the JavaFX events thread (the application is not frozen, but wastes CPU).
         */
        for (Node node : all) {
            node.setManaged(false);
        }
        getChildren().setAll(all);
        this.headerRow           = headerRow;       // Save only after the above succeeded.
        this.headerColumn        = headerColumn;
        this.valueCells          = valueCells;
        this.indexOfFirstError   = indexOfFirstError;
        this.indexAfterLastError = indexAfterLastError;
        final GridView view = getSkinnable();
        view.scaleScrollBar(xScrollBar, nx, false);
        view.scaleScrollBar(yScrollBar, ny, true);
        updateCellValues();
    }

    /**
     * Formats the values in all cells. This method is invoked when a new image is set or after scrolling.
     * It can also be invoked when the band to show has changed.
     */
    final void updateCellValues() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final double cellWidth  = this.cellWidth,
                     cellHeight = this.cellHeight;

        final double valuesRegionX = valuesRegionX();
        final double valuesRegionY = valuesRegionY();
        double xminOfValues = xScrollBar.getValue();
        double yminOfValues = yScrollBar.getValue();
        double xposOfValues = valuesRegionX - (xminOfValues - (xminOfValues = Math.floor(xminOfValues))) * cellWidth;
        double yposOfValues = valuesRegionY - (yminOfValues - (yminOfValues = Math.floor(yminOfValues))) * cellHeight;
        final long xmin = (long) xminOfValues;  // Those `double` values are already integers at this point.
        final long ymin = (long) yminOfValues;
        /*
         * Render the header column. That column has its own width,
         * different than the width of all other cells.
         */
        double cellInnerWidth = toValidCellSize(valuesRegionX - HEADER_MARGIN);
        final GridView view = getSkinnable();
        Text[] cells = headerColumn;
        final int ny = cells.length;
        for (int y=0; y<ny; y++) {
            final double ypos = yposOfValues + cellHeight*y;
            setRightAlignedText(null, cells[y], view.formatCoordinateValue(ymin + y), 0, ypos, cellInnerWidth, cellHeight);
        }
        /*
         * Compute the width of all cells other than the header column,
         * then render the header row. Finally, render the cell values.
         */
        cellInnerWidth = cellWidth;
        double cellSpacing = view.cellSpacing.get();
        if (cellSpacing > 0) {
            cellInnerWidth = toValidCellSize(cellInnerWidth - cellSpacing);
        }
        cells = headerRow;
        final int nx = cells.length;
        for (int x=0; x<nx; x++) {
            final double xpos = xposOfValues + cellWidth*x;
            setRightAlignedText(null, cells[x], view.formatCoordinateValue(xmin + x), xpos, 0, cellInnerWidth, valuesRegionY);
        }
        cells = valueCells;
redo:   for (;;) {
            for (int i=0; i<cells.length; i++) {
                final int x = i % nx;
                final int y = i / nx;
                final double xpos = xposOfValues + x * cellWidth;
                final double ypos = yposOfValues + y * cellHeight;
                if (setRightAlignedText(view, cells[i], view.formatSampleValue(xmin+x, ymin+y),
                                        xpos, ypos, cellInnerWidth, cellHeight))
                {
                    // The format pattern has been made shorter. Rewrite previous cells with the new pattern.
                    continue redo;
                }
            }
            break;
        }
        /*
         * Relocate the error boxes. Usually, there is none.
         */
        if (indexOfFirstError != indexAfterLastError) {
            final var viewArea = new java.awt.Rectangle(Numerics.clamp(xmin), Numerics.clamp(ymin), nx-1, ny-1);
            for (Node node : errors()) {
                boolean visible = false;
                final var error = (GridError) node;
                final var area = error.getVisibleRegion(viewArea);
                if (!area.isEmpty()) {
                    error.resizeRelocate(
                            (area.x - xmin) * cellWidth  + xposOfValues,
                            (area.y - ymin) * cellHeight + yposOfValues,
                             area.width     * cellWidth,
                             area.height    * cellHeight);
                    /*
                     * If after layout the error message size appears too small, hide it.
                     */
                    visible = error.getHeight() >= MIN_ERROR_BOX_SIZE
                           && error.getWidth()  >= MIN_ERROR_BOX_SIZE;
                }
                error.setVisible(visible);
            }
        }
    }

    /**
     * Sets the text and sets its position for making it aligned on the right.
     * If the text does not fit in the space specified by {@code width}, then
     * this method tries to format the number using a shorter pattern.
     * If this method cannot use a shorter pattern, then the text is truncated.
     *
     * <p>This method returns {@code true} if it has shortened the pattern.
     * In such case, the caller should rewrite all cells in order to use a
     * consistent pattern.</p>
     *
     * @param view    the view for which cells are rendered, or {@code null} for not shortening the pattern.
     * @param cell    the cell where to set the text.
     * @param value   the text to set.
     * @param x       horizontal position of the cell.
     * @param y       vertical position of the cell.
     * @param width   width of the cell.
     * @param height  height of the cell.
     */
    private static boolean setRightAlignedText(GridView view, final Text cell, String value,
            double x, double y, final double width, final double height)
    {
        boolean redo = false;
        cell.setText(value);
        if (value != null) {
            double dx, dy;
            for (;;) {
                Bounds bounds = cell.getLayoutBounds();
                dy = height - bounds.getHeight();
                dx = width  - bounds.getWidth();
                if (dx >= 0) break;
                if (view != null) {
                    if (view.cellFormat.shorterPattern()) {
                        redo = true;
                        break;
                    }
                    view = null;
                }
                int cut = value.length() - 2;
                if (cut < 0) break;
                value = value.substring(0, cut) + '…';
                cell.setText(value);
            }
            x += dx;
            y += dy * 0.5;
        }
        cell.relocate(x, y);
        return redo;
    }
}
