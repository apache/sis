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
package org.apache.sis.internal.gui.io;

import java.util.List;
import java.util.ListIterator;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import org.apache.sis.measure.Range;
import org.apache.sis.util.collection.RangeSet;


/**
 * A table row with bars showing which parts of a file have been loaded.
 * This is a row in the table shown by {@link FileAccessView} table.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class FileAccessItem implements Runnable, EventHandler<ActionEvent> {
    /**
     * The height of bars in pixels.
     */
    private static final int HEIGHT = 16;

    /**
     * Number of pixels between cell top border and background border.
     */
    private static final int MARGIN_TOP = 1;

    /**
     * Number of pixels between cell right border and background border.
     */
    private static final int MARGIN_RIGHT = 6;

    /**
     * Color to use for filling the rectangles.
     */
    private static final Color FILL_COLOR = Color.LIGHTSEAGREEN;

    /**
     * Color to use for rectangles border.
     */
    private static final Color BORDER_COLOR = FILL_COLOR.darker();

    /**
     * Width of the cursor in pixels.
     */
    private static final int CURSOR_WIDTH = 10;

    /**
     * The amount of time to keep cursor visible for showing that
     * a read or write operation is in progress.
     */
    private static final Duration CURSOR_DURATION = Duration.seconds(4);

    /**
     * The amount of time to keep seek positions before to let them fade away.
     */
    private static final Duration SEEK_DURATION = Duration.minutes(1);

    /**
     * The list of rows shown by the table.
     * This is used for removing this item when the file is closed.
     */
    final List<FileAccessItem> owner;

    /**
     * The text to show in the "File" column.
     */
    final String filename;

    /**
     * Range of bytes on which a read or write operation has been performed.
     */
    private final RangeSet<Long> accessRanges;

    /**
     * Visual representation of {@link #accessRanges}.
     * The first child is the background rectangle and must be always present.
     * All other children are rectangles built from {@link #accessRanges}.
     */
    final Pane accessView;

    /**
     * The group of rectangles to keep showing after they have been added.
     * Some rectangles may be merged together, but the visual effect is that fill area are only added.
     */
    private final ObservableList<Node> staticGroup;

    /**
     * The group of lines showing seek positions.
     */
    private final ObservableList<Node> seeksGroup;

    /**
     * An animation containing a rectangle showing the current position of read or write operation.
     * The rectangle is drawn on top of {@link #staticView} and fades away after access stopped.
     * This field is reset to {@code null} after the animation stopped.
     */
    private FadeTransition cursor;

    /**
     * Position (in bytes) of the cursor.
     */
    private long cursorPosition;

    /**
     * Size of the file in bytes.
     */
    private long fileSize;

    /**
     * Width in pixels of the column were to draw the boxes.
     */
    private double columnWidth;

    /**
     * Creates a new row in the table of files.
     *
     * @param  owner     list where this rows will be added by the caller.
     * @param  filename  text to show in the "File" column.
     */
    FileAccessItem(final List<FileAccessItem> owner, final String filename) {
        final Group staticView, seeksView;
        this.owner    = owner;
        this.filename = filename;
        staticView    = new Group();
        seeksView     = new Group();
        staticGroup   = staticView.getChildren();
        seeksGroup    = seeksView .getChildren();
        accessView    = new Pane(staticView, seeksView);
        accessRanges  = RangeSet.create(Long.class, true, false);
        staticView.setAutoSizeChildren(false);
        /*
         * Background rectangle.
         */
        final Rectangle background = new Rectangle();
        background.setY(MARGIN_TOP);
        background.setHeight(HEIGHT);
        background.setStroke(FILL_COLOR.brighter());
        background.setFill(Color.TRANSPARENT);
        background.setStrokeType(StrokeType.INSIDE);
        staticGroup.add(background);
        accessView.widthProperty().addListener((p,o,n) -> resize(n.doubleValue()));
    }

    /**
     * Sets a new total width (in pixels) for the bars to draw in {@link #accessView}.
     * This method adjust the sizes of all bars and the positions of cursor and seeks.
     */
    private void resize(final double width) {
        final double old = columnWidth;
        columnWidth = width - MARGIN_RIGHT;
        final double scale = columnWidth / fileSize;
        if (Double.isFinite(scale)) {
            adjustSizes(scale, true);
            if (cursor != null) {
                final Rectangle r = (Rectangle) cursor.getNode();
                r.setX(Math.max(0, Math.min(scale*cursorPosition - CURSOR_WIDTH/2, columnWidth - CURSOR_WIDTH)));
            }
            final double ratio = columnWidth / old;
            for (final Node node : seeksGroup) {
                final Line line = (Line) node;
                final double x = line.getStartX() * ratio;
                line.setStartX(x);
                line.setEndX(x);
            }
        }
    }

    /**
     * Adds a seek position. The position will be kept for some time before to fade away.
     */
    private void addSeek(final long position) {
        final double x = position * (columnWidth / fileSize);
        final Line line = new Line(x, MARGIN_TOP, x, MARGIN_TOP + HEIGHT);
        line.setStroke(Color.DARKBLUE);
        seeksGroup.add(line);
        final FadeTransition t = new FadeTransition(CURSOR_DURATION, line);
        t.setDelay(SEEK_DURATION);
        t.setFromValue(1);
        t.setToValue(0);
        t.setOnFinished(this);
        t.play();
    }

    /**
     * Reports a read or write operation on a range of bytes.
     * This method must be invoked from JavaFX thread.
     *
     * @param  position  offset of the first byte read or written.
     * @param  count     number of bytes read or written.
     * @param  write     {@code false} for a read operation, or {@code true} for a write operation.
     */
    private void addRange(final long position, final int count, final boolean write) {
        cursorPosition = position;
        final boolean add = accessRanges.add(position, position + count);
        final double scale = columnWidth / fileSize;
        if (Double.isFinite(scale)) {
            if (add) {
                adjustSizes(scale, false);
            }
            final Rectangle r;
            if (cursor == null) {
                r = new Rectangle(0, MARGIN_TOP, CURSOR_WIDTH, HEIGHT);
                r.setArcWidth(CURSOR_WIDTH/2 - 1);
                r.setArcHeight(HEIGHT/2 - 2);
                r.setStroke(Color.ORANGE);
                r.setFill(Color.YELLOW);
                accessView.getChildren().add(r);
                cursor = new FadeTransition(CURSOR_DURATION, r);
                cursor.setOnFinished(this);
                cursor.setFromValue(1);
                cursor.setToValue(0);
            } else {
                r = (Rectangle) cursor.getNode();
            }
            r.setX(Math.max(0, Math.min(scale*position - CURSOR_WIDTH/2, columnWidth - CURSOR_WIDTH)));
            cursor.playFromStart();
        }
    }

    /**
     * Invoked when an animation effect finished.
     * This method discards the animation and geometry objects for letting GC do its work.
     */
    @Override
    public void handle(final ActionEvent event) {
        final FadeTransition animation = (FadeTransition) event.getSource();
        final ObservableList<Node> list;
        if (animation == cursor) {
            cursor = null;
            list = accessView.getChildren();
        } else {
            list = seeksGroup;
        }
        final boolean removed = list.remove(animation.getNode());
        assert removed : animation;
    }

    /**
     * Recomputes all rectangles from current {@link #columnWidth} and {@link #accessRanges}.
     *
     * <h4>Implementation note:</h4>
     * This method is inefficient as it iterates over all ranges instead of only the ranges that changed.
     * It should be okay in the common case where file accesses happens often on consecutive blocks,
     * in which case ranges get merged together and the total number of elements in {@link #accessRanges}
     * stay stable or even reduce.
     *
     * @param  resized  {@code true} if this method is invoked because of a change of column width
     *         with (presumably) no change in {@link #accessRanges}, or {@code false} if invoked
     *         after a new range has been added with (presumably) no change in {@link #columnWidth}.
     */
    final void adjustSizes(final double scale, final boolean resized) {
        final ListIterator<Node> bars = staticGroup.listIterator();
        ((Rectangle) bars.next()).setWidth(columnWidth);                    // Background.
        /*
         * Adjust the position and width of all rectangles.
         */
        for (final Range<Long> range : accessRanges) {
            final long min = range.getMinValue();
            final long max = range.getMaxValue();
            final double x = scale * min;
            final double width = scale * (max - min);
            if (bars.hasNext()) {
                final Rectangle r = (Rectangle) bars.next();
                if (resized || r.getX() + r.getWidth() >= x) {
                    r.setX(x);
                    r.setWidth(width);
                    continue;
                }
                /*
                 * Newly added range may have merged two or more ranges in a single one.
                 * Discard all ranges that are fully on the left side of current range.
                 * This is not really mandatory, but we do that in an effort to keep the
                 * most "relevant" rectangles (before change) for the new set of ranges.
                 */
                bars.remove();
            }
            final Rectangle r = new Rectangle(x, MARGIN_TOP, width, HEIGHT);
            r.setStrokeType(StrokeType.INSIDE);
            r.setStroke(BORDER_COLOR);
            r.setFill(FILL_COLOR);
            bars.add(r);
        }
        // Remove all remaining children, if any.
        staticGroup.remove(bars.nextIndex(), staticGroup.size());
    }

    /**
     * Wrapper around a {@link SeekableByteChannel} which will observe the ranges of bytes read or written.
     */
    final class Observer implements SeekableByteChannel {
        /**
         * The channel doing the actual read or write operations.
         */
        private final SeekableByteChannel channel;

        /**
         * Creates a new wrapper around the given channel.
         */
        Observer(final SeekableByteChannel channel) throws IOException {
            this.channel = channel;
            fileSize = channel.size();
        }

        /**
         * Forwards to the wrapped channel and report the range of bytes read.
         */
        @Override
        public int read(final ByteBuffer dst) throws IOException {
            final long position = position();
            final int count = channel.read(dst);
            Platform.runLater(() -> addRange(position, count, false));
            return count;
        }

        /**
         * Forwards to the wrapped channel and report the range of bytes written.
         */
        @Override
        public int write(final ByteBuffer src) throws IOException {
            final long position = position();
            final int count = channel.write(src);
            Platform.runLater(() -> addRange(position, count, true));
            return count;
        }

        /**
         * Forwards to the wrapper channel.
         */
        @Override
        public long position() throws IOException {
            return channel.position();
        }

        /**
         * Forwards to the wrapper channel.
         */
        @Override
        public SeekableByteChannel position(final long position) throws IOException {
            channel.position(position);
            Platform.runLater(() -> addSeek(position));
            return this;
        }

        /**
         * Forwards to the wrapper channel.
         */
        @Override
        public long size() throws IOException {
            return channel.size();
        }

        /**
         * Forwards to the wrapper channel.
         */
        @Override
        public SeekableByteChannel truncate(final long size) throws IOException {
            fileSize = channel.truncate(size).size();
            return this;
        }

        /**
         * Forwards to the wrapper channel.
         */
        @Override
        public boolean isOpen() {
            return channel.isOpen();
        }

        /**
         * Forwards to the wrapper channel and remove the enclosing row from the table.
         */
        @Override
        public void close() throws IOException {
            Platform.runLater(FileAccessItem.this);
            channel.close();
        }
    }

    /**
     * Invoked in JavaFX thread for removing this row from the table.
     */
    @Override
    public void run() {
        owner.remove(this);
    }
}
