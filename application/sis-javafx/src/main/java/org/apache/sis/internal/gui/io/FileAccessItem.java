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
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
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
final class FileAccessItem implements Runnable {
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
        this.owner    = owner;
        this.filename = filename;
        accessRanges = RangeSet.create(Long.class, true, false);
        accessView = new Pane();
        /*
         * Background rectangle.
         */
        final Rectangle background = new Rectangle();
        background.setY(MARGIN_TOP);
        background.setHeight(HEIGHT);
        background.setStroke(FILL_COLOR.brighter());
        background.setFill(Color.TRANSPARENT);
        background.setStrokeType(StrokeType.INSIDE);
        accessView.getChildren().add(background);
        accessView.widthProperty().addListener((p,o,n) -> {
            columnWidth = n.doubleValue() - MARGIN_RIGHT;
            adjustSizes(true);
        });
    }

    /**
     * Reports a read or write operation on a range of bytes.
     * This method is invoked by the {@link Observer} wrapper.
     *
     * @param  position  offset of the first byte read or written.
     * @param  count     number of bytes read or written.
     * @param  write     {@code false} for a read operation, or {@code true} for a write operation.
     */
    private void addRange(final long position, final int count, final boolean write) {
        Platform.runLater(() -> {
            if (accessRanges.add(position, position + count)) {
                adjustSizes(false);
            }
        });
    }

    /**
     * Recomputes all rectangles from current {@link #columnWidth} and {@link #accessRanges}.
     *
     * @param  resized  {@code true} if this method is invoked because of a change of column width
     *         with (presumably) no change in {@link #accessRanges}, or {@code false} if invoked
     *         after a new range has been added with (presumably) no change in {@link #columnWidth}.
     */
    final void adjustSizes(final boolean resized) {
        final double scale = columnWidth / fileSize;
        if (!Double.isFinite(scale)) {
            return;
        }
        final ObservableList<Node> children = accessView.getChildren();
        final ListIterator<Node> bars = children.listIterator();
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
        children.remove(bars.nextIndex(), children.size());
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
            addRange(position, count, false);
            return count;
        }

        /**
         * Forwards to the wrapped channel and report the range of bytes written.
         */
        @Override
        public int write(final ByteBuffer src) throws IOException {
            final long position = position();
            final int count = channel.write(src);
            addRange(position, count, true);
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
        public SeekableByteChannel position(final long newPosition) throws IOException {
            channel.position(newPosition);
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
