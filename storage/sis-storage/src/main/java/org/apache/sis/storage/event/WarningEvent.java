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
package org.apache.sis.storage.event;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.ArgumentChecks;


/**
 * Describes non-fatal errors that occurred in a resource or a data store.
 * The warning message is encapsulated in a {@link LogRecord} object, which allows the storage of various information
 * ({@linkplain LogRecord#getThrown() stack trace}, {@linkplain LogRecord#getThreadID() thread identifier},
 * {@linkplain LogRecord#getInstant() log time}, <i>etc.</i>) in addition of warning message.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   1.0
 * @version 1.0
 * @module
 */
public class WarningEvent extends StoreEvent {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3825327888379868663L;

    /**
     * The warning message together with its severity level, source method/class name,
     * stack trace, thread identifier, <i>etc</i>.
     */
    private final LogRecord description;

    /**
     * Constructs an event for a warning that occurred in the given resource.
     *
     * @param  source       the resource on which the warning initially occurred.
     * @param  description  log record containing warning message, stack trace (if any) and other information.
     * @throws IllegalArgumentException if the given source is null.
     * @throws NullPointerException if the given description is null.
     */
    public WarningEvent(final Resource source, final LogRecord description) {
        super(source);
        ArgumentChecks.ensureNonNull("description", description);
        this.description = description;
    }

    /**
     * Returns the warning message together with stack trace (if any) and other information.
     *
     * @return the log record containing warning message, stack trace and other information.
     */
    public LogRecord getDescription() {
        return description;
    }

    /**
     * Returns a string representation of this warning for debugging purpose.
     *
     * @return a string representation of this warning.
     */
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        final Level level = description.getLevel();
        if (level != null) {
            b.append(level.getLocalizedName()).append(": ");
        }
        b.append(description.getMessage());
        final Throwable cause = description.getThrown();
        if (cause != null) {
            b.append(System.lineSeparator()).append("Caused by ").append(cause);
        }
        return b.toString();
    }
}
