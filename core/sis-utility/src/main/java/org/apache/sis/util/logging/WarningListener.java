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
package org.apache.sis.util.logging;

import java.util.EventListener;
import java.util.logging.Level;
import java.util.logging.LogRecord;


/**
 * Intercepts non-fatal error messages logged by {@link org.apache.sis.storage.DataStore} or other SIS objects.
 * Warnings are encapsulated in {@link LogRecord} objects and logged at {@link Level#WARNING} if the emitter does not
 * have registered any {@code WarningListener}. This listener allows applications to intercept warning records for
 * displaying them in a dialog (or any other action that the application may choose) instead than logging them.
 *
 * <div class="note"><b>Comparison with alternative approaches:</b>
 * it is also possible to listen to login events by registering a custom {@link java.util.logging.Handler} to the logger.
 * But {@code Handler} instances are registered on a per-logger basis and receive all messages sent to that logger
 * regardless their emitter. By contrast, {@code WarningListener} instances are registered on a per-{@code DataStore}
 * basis (or any other emitter) and receive all messages sent by that emitter regardless the destination logger.
 * The emitter is part of the information given to the {@link #warningOccured(Object, LogRecord)} method,
 * in addition to the log record.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.3
 *
 * @param <S>  the base type of objects that emit warnings (the <cite>source</cite>).
 *
 * @see WarningListeners
 * @see org.apache.sis.storage.DataStore#addWarningListener(WarningListener)
 * @see org.apache.sis.storage.event.StoreListener
 *
 * @since 0.3
 * @module
 */
public interface WarningListener<S> extends EventListener {
    /**
     * Returns the type of objects that emit warnings of interest for this listener.
     * This is typically, but not necessarily, the class having the name returned by
     * {@link LogRecord#getSourceClassName()}, or one of its parent classes.
     *
     * @return the base type of objects that emit warnings (the <cite>source</cite>).
     */
    Class<S> getSourceClass();

    /**
     * Reports the occurrence of a non-fatal error. The emitter process (often a
     * {@link org.apache.sis.storage.DataStore} in the midst of a reading process)
     * will continue following the call to this method.
     *
     * <p>The {@code LogRecord} provides the warning {@linkplain LogRecord#getMessage() message} together with
     * programmatic information like the {@linkplain LogRecord#getSourceClassName() source class name} and
     * {@linkplain LogRecord#getSourceMethodName() method name} where the warning occurred. The log record
     * may optionally contains the exception which has been {@linkplain LogRecord#getThrown() thrown}.</p>
     *
     * <p>Applications may choose to ignore the warning, display a dialog or take any other action they choose.
     * Applications do not need to log the warning, since logging will be done automatically if the emitter has
     * no registered warning listeners.</p>
     *
     * @param source   the object that emitted a warning, or {@code null} if not available.
     * @param warning  the warning message together with programmatic information.
     */
    void warningOccured(S source, LogRecord warning);
}
