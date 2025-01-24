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
package org.apache.sis.gui.internal.io;

import java.util.List;
import java.util.ListIterator;
import java.util.EnumMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.collection.RangeSet;
import org.apache.sis.io.stream.ByteRangeChannel;


/**
 * A table row with bars showing which parts of a file have been loaded.
 * This is a row in the table shown by {@link FileAccessView} table.
 *
 * @author  Martin Desruisseaux (Geomatys)
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
     * The access mode. Rendering are done in enumeration order.
     */
    private enum Mode {
        /** Cache a range of bytes. */ CACHE(Color.LIGHTGRAY),
        /** Read  a range of bytes. */ READ(Color.LIGHTSEAGREEN),
        /** Write a range of bytes. */ WRITE(Color.LIGHTCORAL);

        /** The color to use for rendering the rectangle. */
        private final Color border, fill;

        /** Creates a new enumeration value. */
        private Mode(final Color fill) {
            this.fill = fill;
            border = fill.darker();
        }

        /** Sets the colors of the given rectangle for representing this mode. */
        final void colorize(final Rectangle r) {
            r.setStroke(border);
            r.setFill(fill);
        }
    }

    /**
     * Range of bytes on which read or write operations have been performed.
     */
    private final EnumMap<Mode, RangeSet<Long>> accessRanges;

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
        accessRanges  = new EnumMap<>(Mode.class);
        staticView.setAutoSizeChildren(false);
        /*
         * Background rectangle.
         */
        final Rectangle background = new Rectangle();
        background.setY(MARGIN_TOP);
        background.setHeight(HEIGHT);
        background.setStroke(Mode.READ.fill.brighter());
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
     * @param  lower  offset of the first byte read or written.
     * @param  count  offset after the last byte read or written.
     * @param  mode   whether a read, write or cache operation is performed.
     *
     * @see #addRangeLater(long, long, Mode)
     */
    private void addRange(final long lower, final long upper, final Mode mode) {
        cursorPosition = lower;
        /*
         * Add the range for the specified mode and remove it for all other modes.
         * Consequently the visual component will show the last access mode for
         * the specified range of bytes.
         */
        RangeSet<Long> ranges = accessRanges.get(mode);
        if (ranges == null) {
            ranges = RangeSet.create(Long.class, true, false);
            accessRanges.put(mode, ranges);
        }
        boolean add = ranges.add(lower, upper);
        for (final RangeSet<Long> other : accessRanges.values()) {
            if (other != null && other != ranges) {
                add |= other.remove(lower, upper);
            }
        }
        /*
         * Update the visual component showing the position of last operation.
         * An animation effect is used.
         */
        final double scale = columnWidth / fileSize;
        if (Double.isFinite(scale)) {
            if (add) {
                adjustSizes(scale, false);
            }
            if (mode != Mode.CACHE) {
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
                r.setX(Math.max(0, Math.min(scale*lower - CURSOR_WIDTH/2, columnWidth - CURSOR_WIDTH)));
                cursor.playFromStart();
            }
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
     * <h4>Implementation note</h4>
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
        for (final EnumMap.Entry<Mode, RangeSet<Long>> entry : accessRanges.entrySet()) {
            final Mode mode = entry.getKey();
            for (final Range<Long> range : entry.getValue()) {
                final long min = range.getMinValue();
                final long max = range.getMaxValue();
                final double x = scale * min;
                final double width = scale * (max - min);
                if (bars.hasNext()) {
                    final Rectangle r = (Rectangle) bars.next();
                    if (resized || r.getX() + r.getWidth() >= x) {
                        r.setX(x);
                        r.setWidth(width);
                        mode.colorize(r);
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
                mode.colorize(r);
                bars.add(r);
            }
        }
        // Remove all remaining children, if any.
        staticGroup.remove(bars.nextIndex(), staticGroup.size());
    }

    /**
     * A range of bytes determined from the background thread and to be consumed in the JavaFX thread.
     * This range can be updated as long as it has not been consumed. Those modifications reduce the
     * number of events to be consumed by the JavaFX thread.
     */
    private final class NextAddRange implements Runnable {
        /** Whether the range of bytes has been read, written or cached. */
        private final Mode mode;

        /** The range of bytes, modifiable as long as the event has not been consumed. */
        long lower, upper;

        /** Creates a new range of bytes for the given access mode. */
        NextAddRange(final Mode mode) {
            this.mode = mode;
        }

        /** Invoked in the JavaFX thread for saving the range in {@link #accessRanges}. */
        @Override public void run() {
            synchronized (FileAccessItem.this) {
                if (next == this) {
                    next = null;
                }
            }
            addRange(lower, upper, mode);
        }
    }

    /**
     * The next range of bytes to be merged into {@link #accessRanges}.
     * Accesses to this field must be synchronized on {@code this}.
     * The instance is created in a background thread and consumed in the JavaFX thread.
     */
    private NextAddRange next;

    /**
     * Reports a read or write operation on a range of bytes.
     * This method should be invoked from a background thread.
     *
     * @param  lower  offset of the first byte read or written.
     * @param  count  number of bytes read or written.
     * @param  mode   whether a read, write or cache operation is performed.
     */
    private synchronized void addRangeLater(long lower, final long count, final Mode mode) {
        long upper = Numerics.saturatingAdd(lower, count);
        if (next != null && next.mode == mode && next.upper >= lower && next.lower <= upper) {
            lower = Math.min(next.lower, lower);
            upper = Math.max(next.upper, upper);
        } else {
            next = new NextAddRange(mode);
            Platform.runLater(next);
        }
        next.lower = lower;
        next.upper = upper;
    }

    /**
     * Wrapper around a {@link SeekableByteChannel} which will observe the ranges of bytes read or written.
     */
    final class Observer extends ByteRangeChannel implements SeekableByteChannel {
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
         * Specifies a range of bytes which is expected to be read.
         * This method may do nothing if the backed channel does not support this operation.
         *
         * @param  lower  position (inclusive) of the first byte to be requested.
         * @param  upper  position (exclusive) of the last byte to be requested.
         */
        @Override
        public void rangeOfInterest(final long lower, final long upper) {
            if (channel instanceof ByteRangeChannel) {
                ((ByteRangeChannel) channel).rangeOfInterest(lower, upper);
            }
        }

        /**
         * Forwards to the wrapped channel and report the range of bytes read.
         */
        @Override
        public int read(final ByteBuffer dst) throws IOException {
            final long position = position();
            final int count = channel.read(dst);
            addRangeLater(position, count, Mode.READ);
            return count;
        }

        /**
         * Forwards to the wrapped channel and report the range of bytes written.
         */
        @Override
        public int write(final ByteBuffer src) throws IOException {
            final long position = position();
            final int count = channel.write(src);
            addRangeLater(position, count, Mode.WRITE);
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
     * Wrapper around an {@link InputStream} which will observe the ranges of bytes read.
     * It can be used directly when the input is an {@link InputStream}, or indirectly
     * when the channel observed by {@link Observer} is itself wrapping an input stream.
     * In such case, the bytes read from the input stream are typically cached in some temporary file.
     *
     * <h2>Implementation note</h2>
     * We do not extend {@link java.io.FilterInputStream} because we override almost all methods anyway.
     * This implementation avoids using a non-final {@code volatile} field for the wrapped input stream.
     */
    final class InputObserver extends InputStream {
        /** The source input stream. */
        private final InputStream in;

        /** The mode, either read or cache. */
        private final Mode mode;

        /** Position of the stream, current and marked. */
        private long position, mark;

        /** Creates a new observer for the given input stream. */
        InputObserver(final InputStream in) {
            this.in   = in;
            this.mode = Mode.READ;
        }

        /** Creates a new observer for the given input stream used as a cache. */
        InputObserver(final InputStream in, final long start) {
            this.in   = in;
            this.mode = Mode.CACHE;
            position  = start;
        }

        /**
         * Declares that a range of bytes has been read.
         * This method update the rectangles in the JavaFX view.
         *
         * @param  count  number of bytes that have been read.
         * @param  mode   the mode, usually {@link #mode}.
         */
        private void range(final long count, final Mode mode) {
            if (count > 0) {
                addRangeLater(position, count, mode);
                if (position > (position += count)) {
                    position = Long.MAX_VALUE;
                }
            }
        }

        /**
         * Declares that a range of bytes has been read.
         * This method update the rectangles in the JavaFX view.
         *
         * @param  count  number of bytes that have been read.
         */
        private void range(final long count) {
            range(count, mode);
        }

        /** Returns the next byte or -1 on EOF. */
        @Override public int read() throws IOException {
            final int b = in.read();
            if (b >= 0) range(1);
            return b;
        }

        /** Stores a sequence of bytes in the specified array. */
        @Override public int read(final byte[] b, final int off, int len) throws IOException {
            range(len = in.read(b, off, len));
            return len;
        }

        /** Stores a sequence of bytes in a newly allocated array. */
        @Override public byte[] readNBytes(final int len) throws IOException {
            final byte[] b = in.readNBytes(len);
            range(b.length);
            return b;
        }

        /** Stores a sequence of bytes in the specified output stream. */
        @Override public long transferTo(final OutputStream out) throws IOException {
            final long n = in.transferTo(out);
            range(n);
            return n;
        }

        /** Skips some bytes without reporting them as read. */
        @Override public long skip(long n) throws IOException {
            range(n = in.skip(n), Mode.CACHE);
            return n;
        }

        /** Returns an estimate of the number of bytes that can be read. */
        @Override public int available() throws IOException {
            return in.available();
        }

        /** Tells whether the input stream supports marks. */
        @Override public boolean markSupported() {
            return in.markSupported();
        }

        /** Marks the current position in this input stream. */
        @Override public void mark(final int readlimit) {
            in.mark(readlimit);
            mark = position;
        }

        /** Repositions this stream to the position of the last mark. */
        @Override public void reset() throws IOException {
            in.reset();
            position = mark;
        }

        /** Closes the wrapped input stream. */
        @Override public void close() throws IOException {
            if (mode == Mode.READ) {
                Platform.runLater(FileAccessItem.this);
                // Otherwise will be removed by `Observer`.
            }
            in.close();
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
