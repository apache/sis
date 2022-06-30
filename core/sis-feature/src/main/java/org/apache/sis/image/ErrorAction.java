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

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.awt.image.ImagingOpException;
import org.apache.sis.internal.system.Modules;

import static java.util.logging.Logger.getLogger;


/**
 * Some common ways to handle exceptions occurring during tile calculation.
 * This class provides the implementations of {@link ErrorHandler} static constants.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
enum ErrorAction implements ErrorHandler {
    /**
     * Exceptions are wrapped in an {@link ImagingOpException} and thrown.
     * In such case, no result is available. This is the default action.
     */
    THROW,

    /**
     * Exceptions are wrapped in a {@link LogRecord} and logged at {@link java.util.logging.Level#WARNING}.
     * Only one log record is created for all tiles that failed for the same operation on the same image.
     * A partial result may be available.
     *
     * <p>Users are encouraged to use {@link #THROW} or to specify their own {@link ErrorHandler}
     * instead of using this error action, because not everyone read logging records.</p>
     */
    LOG;

    /**
     * Logs the given record or throws its exception, depending on {@code this} enumeration value.
     * This method is implemented as a matter of principle but not invoked for {@code ErrorAction}
     * enumeration values.
     */
    @Override
    public void handle(final Report details) {
        synchronized (details) {
            final LogRecord record = details.getDescription();
            if (record != null) {
                if (this == LOG) {
                    String logger = record.getLoggerName();
                    if (logger == null) {
                        logger = Modules.RASTER;
                        record.setLoggerName(logger);
                    }
                    getLogger(logger).log(record);
                } else {
                    final Throwable ex = record.getThrown();
                    if (ex instanceof Error) {
                        throw (Error) ex;
                    } else if (ex instanceof ImagingOpException) {
                        throw (ImagingOpException) ex;
                    } else {
                        final String message = new SimpleFormatter().formatMessage(record);
                        throw (ImagingOpException) new ImagingOpException(message).initCause(ex);
                    }
                }
            }
        }
    }
}
