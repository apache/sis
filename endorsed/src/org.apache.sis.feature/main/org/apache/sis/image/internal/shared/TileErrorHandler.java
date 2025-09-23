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
package org.apache.sis.image.internal.shared;

import java.util.logging.LogRecord;
import java.awt.image.RenderedImage;
import java.awt.image.ImagingOpException;
import org.apache.sis.image.ErrorHandler;
import org.apache.sis.system.Modules;


/**
 * A convenience class for reporting an error during computation of a tile.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TileErrorHandler {
    /**
     * Exceptions are wrapped in an {@link ImagingOpException} and thrown.
     * In such case, no result is available. This is the default handler.
     */
    public static final TileErrorHandler THROW = new TileErrorHandler(ErrorHandler.THROW, null, null);

    /**
     * Where to report exceptions, or {@link ErrorHandler#THROW} for throwing them.
     */
    final ErrorHandler handler;

    /**
     * The class to declare in {@link LogRecord} in an error occurred during calculation.
     * If non-null, then {@link #sourceMethod} should also be non-null.
     */
    private final Class<?> sourceClass;

    /**
     * Name of the method to declare in {@link LogRecord} in an error occurred during calculation.
     * If non-null, then {@link #sourceClass} should also be non-null.
     */
    private final String sourceMethod;

    /**
     * Creates a new tile error handler.
     *
     * @param  handler        where to report exceptions, or {@link ErrorHandler#THROW} for throwing them.
     * @param  sourceClass    the class to declare in {@link LogRecord} in an error occurred during calculation.
     * @param  sourceMethod   name of the method to declare in {@link LogRecord} in an error occurred during calculation.
     */
    public TileErrorHandler(final ErrorHandler handler, final Class<?> sourceClass, final String sourceMethod) {
        this.handler      = handler;
        this.sourceClass  = sourceClass;
        this.sourceMethod = sourceMethod;
    }

    /**
     * Returns {@code true} if the error handler is {@link ErrorHandler#THROW}.
     * In such case there is not need to configure the {@link LogRecord} since
     * nothing will be logged.
     */
    final boolean isThrow() {
        return handler == ErrorHandler.THROW;
    }

    /**
     * If the given report is non-empty, sends it to the error handler.
     * This method sets the logger, source class and source method name
     * on the {@link LogRecord} instance before to publish it.
     *
     * @param  report  the error report to send if non-empty.
     */
    public void publish(final ErrorHandler.Report report) {
        synchronized (report) {
            if (report.isEmpty()) {
                return;
            }
            if (!isThrow()) {
                final LogRecord record = report.getDescription();
                if (sourceClass != null) {
                    record.setSourceClassName(sourceClass.getCanonicalName());
                }
                if (sourceMethod != null) {
                    record.setSourceMethodName(sourceMethod);
                }
                record.setLoggerName(Modules.RASTER);
            }
        }
        handler.handle(report);
    }

    /**
     * An object executing actions in a way where errors occurring during tile computation
     * are reported to an error handler instead of causing the whole operation to fail.
     *
     * <p>This interface is currently used as a workaround for accessing
     * {@link org.apache.sis.image.PrefetchedImage} without making that class public.</p>
     */
    public interface Executor {
        /**
         * Executes the given action in a mode where errors occurring in {@link RenderedImage#getTile(int, int)}
         * are reported to the given handler instead of stopping the operation.
         *
         * @param  action        the action to execute (for example drawing the image).
         * @param  errorHandler  the handler to notify if errors occur.
         */
        void execute(Runnable action, TileErrorHandler errorHandler);
    }
}
