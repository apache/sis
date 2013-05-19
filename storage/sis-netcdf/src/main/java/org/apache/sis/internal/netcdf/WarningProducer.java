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
package org.apache.sis.internal.netcdf;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.logging.Logging;


/**
 * Base class of NetCDF classes which may produce warnings.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.08)
 * @version 0.3
 * @module
 */
public class WarningProducer {
    /**
     * Where to send the warnings, or {@code null} if none.
     */
    private final WarningProducer parent;

    /**
     * Creates a new instance.
     *
     * @param parent Where to send the warnings, or {@code null} if none.
     */
    protected WarningProducer(final WarningProducer parent) {
        this.parent = parent;
    }

    /**
     * Reports a warning represented by the given log record. The current implementation just logs the warning.
     * However if we want to implement a listener mechanism in a future version, this could be done here.
     *
     * @param record The warning as a log record.
     */
    private void warning(final LogRecord record) {
        if (parent != null) {
            parent.warning(record);
        } else {
            final Logger logger = Logging.getLogger(WarningProducer.class);
            record.setLoggerName(logger.getName());
            logger.log(record);
        }
    }

    /**
     * Reports a warning represented by the given exception.
     *
     * @param methodName The name of the method in which the warning occurred.
     * @param exception  The exception to log.
     */
    protected final void warning(final String methodName, final Exception exception) {
        final LogRecord record = new LogRecord(Level.WARNING, Exceptions.formatChainedMessages(null, null, exception));
        record.setSourceClassName(getClass().getCanonicalName());
        record.setSourceMethodName(methodName);
        warning(record);
    }
}
