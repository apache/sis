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
package org.apache.sis.image;

import java.awt.Point;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.awt.image.ImagingOpException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.feature.internal.Resources;


/**
 * Action to perform when errors occurred while reading or writing some tiles in an image.
 * The most typical actions are {@linkplain #THROW throwing an exception} or {@linkplain #LOG logging a warning}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 */
public interface ErrorHandler {
    /**
     * Exceptions are wrapped in an {@link ImagingOpException} and thrown.
     * In such case, no result is available. This is the default handler.
     */
    ErrorHandler THROW = ErrorAction.THROW;

    /**
     * Exceptions are wrapped in a {@link LogRecord} and logged, usually at {@link Level#WARNING}.
     * Only one log record is created for all tiles that failed for the same operation on the same image.
     * A partial result may be available.
     *
     * <p>Users are encouraged to use {@link #THROW} or to specify their own {@link ErrorHandler}
     * instead of using this error action, because not everyone read logging records.</p>
     */
    ErrorHandler LOG = ErrorAction.LOG;

    /**
     * Invoked after errors occurred in one or many tiles. This method may be invoked an arbitrary
     * time after the error occurred, and may aggregate errors that occurred in more than one tile.
     *
     * <h4>Multi-threading</h4>
     * If the image processing was split between many worker threads, this method may be invoked
     * from any of those threads. However, the invocation should happen after all threads terminated,
     * either successfully or with an error reported in {@code details}.
     *
     * @param  details  information about the first error. If more than one error occurred, the other
     *         errors are reported as {@linkplain Throwable#getSuppressed() suppressed exceptions}.
     */
    void handle(Report details);

    /**
     * Information about errors that occurred while reading or writing tiles in an image.
     * A single {@code Report} may be generated for failures in more than one tiles.
     *
     * <h2>Multi-threading</h2>
     * This class is safe for use in multi-threading. The synchronization lock is {@code this}.
     * However, the {@link LogRecord} instance returned by {@link #getDescription()} is not thread-safe.
     * Operations applied on the {@code LogRecord} should be inside a block synchronized on the
     * {@code Report.this} lock.
     */
    class Report {
        /**
         * The tile indices as (x,y) tuples where errors occurred, or {@code null} if none.
         */
        private int[] indices;

        /**
         * Number of valid elements in {@link #indices}.
         */
        private int length;

        /**
         * Description of the error that occurred, or {@code null} if none.
         */
        private LogRecord description;

        /**
         * Creates an initially empty report.
         * Error reports can be added by calls to {@link #add add(…)}.
         */
        public Report() {
        }

        /**
         * Returns {@code true} if no error has been reported.
         * This is true only if the {@link #add add(…)} method has never been invoked.
         *
         * @return whether this report is empty.
         */
        public synchronized boolean isEmpty() {
            return length == 0 && description == null;
        }

        /**
         * Returns {@code true} if the given exceptions are of the same class, have the same message
         * and the same stack trace. The cause and the suppressed exceptions are ignored.
         */
        private static boolean equals(final Throwable t1, final Throwable t2) {
            if (t1 == t2) return true;
            return t1 != null && t2 != null && t1.getClass() == t2.getClass()
                    && Objects.equals(t1.getMessage(),    t2.getMessage())
                    &&  Arrays.equals(t1.getStackTrace(), t2.getStackTrace());
        }

        /**
         * Adds the {@code more} exception to the list if suppressed exceptions if not already present.
         */
        private static void addSuppressed(final Throwable error, final Throwable more) {
            if (equals(error, more) || equals(error.getCause(), more)) {
                return;
            }
            for (final Throwable s : error.getSuppressed()) {
                if (equals(s, more)) return;
            }
            error.addSuppressed(more);
        }

        /**
         * Reports an error that occurred while computing an image tile.
         * This method can be invoked many times on the same {@code Report} instance.
         *
         * <h4>Logging information</h4>
         * {@code Report} creates a {@link LogRecord} the first time that this {@code add(…)} method is invoked.
         * The log record is created using the given supplier if non-null. That supplier should set the log
         * {@linkplain LogRecord#getLevel() level},
         * {@linkplain LogRecord#getMessage() message},
         * {@linkplain LogRecord#getSourceClassName() source class name},
         * {@linkplain LogRecord#getSourceMethodName() source method name} and
         * {@linkplain LogRecord#getLoggerName() logger name}.
         * The {@linkplain LogRecord#getThrown() exception} property will be set by this method.
         *
         * @param  tile    column (x) and row (y) indices of the tile where the error occurred, or {@code null} if unknown.
         * @param  error   the error that occurred.
         * @param  record  the record supplier, invoked only when this method is invoked for the first time.
         *                 If {@code null}, a default {@link LogRecord} will be created.
         * @return {@code true} if this is the first time that an error is reported
         *         (in which case a {@link LogRecord} instance has been created),
         *         or {@code false} if a {@link LogRecord} already exists.
         */
        public synchronized boolean add(final Point tile, final Throwable error, final Supplier<LogRecord> record) {
            ArgumentChecks.ensureNonNull("error", error);
            if (tile != null) {
                if (indices == null) {
                    indices = new int[8];
                } else if (length >= indices.length) {
                    indices = Arrays.copyOf(indices, indices.length * 2);
                }
                indices[length++] = tile.x;
                indices[length++] = tile.y;
            }
            if (description == null) {
                if (record != null) {
                    description = record.get();
                }
                if (description == null) {
                    if (tile != null) {
                        description = Resources.forLocale(null)
                                .createLogRecord(Level.WARNING, Resources.Keys.CanNotProcessTile_2, tile.x, tile.y);
                    } else {
                        description = new LogRecord(Level.WARNING, error.toString());
                    }
                }
                description.setThrown(error);
                return true;
            } else {
                addSuppressed(description.getThrown(), error);
                return false;
            }
        }

        /**
         * Returns indices of all tiles where an error has been reported.
         *
         * @return indices of all tiles in error, or an empty array if none.
         */
        public synchronized Point[] getTileIndices() {
            final Point[] p = new Point[length >>> 1];
            for (int i=0; i<length;) {
                p[i >>> 1] = new Point(indices[i++], indices[i++]);
            }
            return p;
        }

        /**
         * Returns a description of the first error as a log record.
         * The exception can be obtained by {@link LogRecord#getThrown()}.
         * If more than one error occurred, the other errors are reported
         * as {@linkplain Throwable#getSuppressed() suppressed exceptions}.
         * The return value is never null unless this report {@linkplain #isEmpty() is empty}.
         *
         * @return errors description, or {@code null} if this report is empty.
         */
        public synchronized LogRecord getDescription() {
            return description;
        }
    }
}
