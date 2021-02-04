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
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.awt.image.ImagingOpException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.feature.Resources;


/**
 * Action to perform when errors occurred while reading or writing some tiles in an image.
 * The most typical actions are {@linkplain #THROW throwing an exception} or {@linkplain #LOG logging a warning}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public interface ErrorHandler {
    /**
     * Exceptions are wrapped in an {@link ImagingOpException} and thrown.
     * In such case, no result is available. This is the default handler.
     */
    ErrorHandler THROW = ErrorAction.THROW;

    /**
     * Exceptions are wrapped in a {@link LogRecord} and logged at {@link java.util.logging.Level#WARNING}.
     * Only one log record is created for all tiles that failed for the same operation on the same image.
     * A partial result may be available.
     *
     * <p>Users are encouraged to use {@link #THROW} or to specify their own {@link ErrorHandler}
     * instead than using this error action, because not everyone read logging records.</p>
     */
    ErrorHandler LOG = ErrorAction.LOG;

    /**
     * Invoked after errors occurred in one or many tiles. This method may be invoked an arbitrary
     * time after the error occurred, and may aggregate errors that occurred in more than one tile.
     *
     * @param  details  information about errors.
     */
    void handle(Report details);

    /**
     * Information about errors that occurred while reading or writing tiles in an image.
     * A single {@code Report} may be generated for failures in more than one tiles.
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
         * Error reports can be added by calls to {@code add(Throwable, …)} methods.
         */
        public Report() {
        }

        /**
         * Returns {@code true} if no error has been reported.
         * This is true only if no {@code add(Throwable, …)} method had been invoked.
         *
         * @return whether this report is empty.
         */
        public boolean isEmpty() {
            return length == 0 && description == null;
        }

        /**
         * Returns {@code true} if the given exceptions are of the same class, have the same message
         * and the same stack trace. The cause and the suppressed exceptions are ignored.
         */
        private static boolean equals(final Throwable t1, final Throwable t2) {
            return t1.getClass() == t2.getClass()
                    && Objects.equals(t1.getMessage(),    t2.getMessage())
                    &&  Arrays.equals(t1.getStackTrace(), t2.getStackTrace());
        }

        /**
         * Adds the {@code more} exception to the list if suppressed exceptions if not already present.
         */
        private static void addSuppressed(final Throwable error, final Throwable more) {
            if (equals(error, more)) return;
            for (final Throwable s : error.getSuppressed()) {
                if (equals(s, more)) return;
            }
            error.addSuppressed(more);
        }

        /**
         * Reports an error that occurred while computing an image property.
         * This method can be invoked many times on the same {@code Report} instance.
         *
         * <h4>Logging information</h4>
         * {@code Report} creates a {@link LogRecord} the first time that an {@code add(…)} method is invoked.
         * The record will have its
         * {@linkplain LogRecord#getLevel() level},
         * {@linkplain LogRecord#getMessage() message} and
         * {@linkplain LogRecord#getThrown() exception} properties initialized. But the
         * {@linkplain LogRecord#getSourceClassName() source class name},
         * {@linkplain LogRecord#getSourceMethodName() source method name} and
         * {@linkplain LogRecord#getLoggerName() logger name} may be undefined;
         * they may need to be completed by the caller.
         *
         * @param  error     the error that occurred.
         * @param  property  name of the property which was computed, or {@code null} if none.
         * @return {@code true} if this is the first time that an error is reported
         *         (in which case a {@link LogRecord} instance has been created),
         *         or {@code false} if a {@link LogRecord} already exists.
         */
        public boolean addPropertyError(final Throwable error, final String property) {
            ArgumentChecks.ensureNonNull("error", error);
            if (description == null) {
                if (property != null) {
                    description = Errors.getResources((Locale) null)
                            .getLogRecord(Level.WARNING, Errors.Keys.CanNotCompute_1, property);
                } else {
                    description = new LogRecord(Level.WARNING, error.toString());
                }
                description.setThrown(error);
                return true;
            } else {
                addSuppressed(description.getThrown(), error);
                return false;
            }
        }

        /**
         * Reports an error that occurred while computing an image tile.
         * This method can be invoked many times on the same {@code Report} instance.
         *
         * <h4>Logging information</h4>
         * {@code Report} creates a {@link LogRecord} the first time that an {@code add(…)} method is invoked.
         * The record will have its
         * {@linkplain LogRecord#getLevel() level},
         * {@linkplain LogRecord#getMessage() message} and
         * {@linkplain LogRecord#getThrown() exception} properties initialized. But the
         * {@linkplain LogRecord#getSourceClassName() source class name},
         * {@linkplain LogRecord#getSourceMethodName() source method name} and
         * {@linkplain LogRecord#getLoggerName() logger name} may be undefined;
         * they may need to be completed by the caller.
         *
         * @param  error  the error that occurred.
         * @param  tx     column index of the tile where the error occurred.
         * @param  ty     row index of the tile where the error occurred.
         * @return {@code true} if this is the first time that an error is reported
         *         (in which case a {@link LogRecord} instance has been created),
         *         or {@code false} if a {@link LogRecord} already exists.
         */
        public boolean addTileError(final Throwable error, final int tx, final int ty) {
            ArgumentChecks.ensureNonNull("error", error);
            if (indices == null) {
                indices = new int[8];
            } else if (length >= indices.length) {
                indices = Arrays.copyOf(indices, indices.length * 2);
            }
            indices[length++] = tx;
            indices[length++] = ty;
            if (description == null) {
                description = Resources.forLocale(null)
                        .getLogRecord(Level.WARNING, Resources.Keys.CanNotProcessTile_2, tx, ty);
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
        public Point[] getTileIndices() {
            final Point[] p = new Point[length >>> 1];
            for (int i=0; i<length;) {
                p[i >>> 1] = new Point(indices[i++], indices[i++]);
            }
            return p;
        }

        /**
         * Returns a description of errors as a log record.
         * The exception can be obtained by {@link LogRecord#getThrown()}.
         * The return value is never null unless this report {@linkplain #isEmpty() is empty}.
         *
         * @return errors description, or {@code null} if this report is empty.
         */
        public LogRecord getDescription() {
            return description;
        }
    }
}
