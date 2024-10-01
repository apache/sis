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
package org.apache.sis.storage.gdal;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.foreign.MemorySegment;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.InternalDataStoreException;
import org.apache.sis.storage.panama.NativeFunctions;


/**
 * A callback which is invoked by <abbr>GDAL</abbr> when a warning or error occurred.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ErrorHandler {
    /**
     * Context in which a <abbr>GDAL</abbr> method is invoked.
     */
    private static final ThreadLocal<List<ErrorHandler>> CURRENT = new ThreadLocal<>();

    /**
     * The {@code CE_Failure} value of the <abbr>GDAL</abbr> {@code CPLErr} codes.
     */
    private static final int FAILURE = 3;

    /**
     * <abbr>GDAL</abbr> Common Portability Library error code ({@code CPLErr}) of the message.
     * This code will be mapped to a {@link Level} at logging time. The possible values are:
     *
     * <ul>
     *   <li>{@code CE_None}    = 0: No error or warning occurred.</li>
     *   <li>{@code CE_Debug}   = 1: The result contains debug information that users can ignore.</li>
     *   <li>{@code CE_Warning} = 2: The result contains informational warning.</li>
     *   <li>{@code CE_Failure} = 3: The action failed.</li>
     *   <li>{@code CE_Fatal}   = 4: A fatal error has occurred and <abbr>GDAL</abbr> should not be used anymore.
     *       The default GDAL behavior is to report errors to {@code stderr} and to abort the application.</li>
     * </ul>
     */
    private final int err;

    /**
     * The error message, or {@code null} if none.
     */
    private final String message;

    /**
     * Creates a record for a <abbr>GDAL</abbr> error message. The use of this class as a record is an
     * {@code ErrorHandler} implementation details. Code outside this class see only the static methods.
     */
    private ErrorHandler(final int err, final String message) {
        this.err     = err;
        this.message = message;
    }

    /**
     * Returns the handle for the method to invoke from <abbr>GDAL</abbr>.
     * Used only at {@linkplain GDAL#setErrorHandler initialization time}
     * after the <abbr>GDAL</abbr> library has been loaded.
     */
    static MethodHandle getMethod() {
        try {
            return MethodHandles.lookup().findStatic(ErrorHandler.class, "errorOccurred",
                    MethodType.methodType(Void.TYPE, Integer.TYPE, Integer.TYPE, MemorySegment.class));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);        // Should never happen.
        }
    }

    /**
     * Invoked by <abbr>GDAL</abbr> native code when a warning or an error occurred.
     * This method merely adds the message in a queue for logging at a later stage.
     * No logging is performed here for avoiding the risk that a user's code throws
     * an exception.
     *
     * @param  err      the <abbr>GDAL</abbr> Common Portability Library error level ({@code CPLErr}).
     * @param  code     the <abbr>GDAL</abbr> {@code CPLErrorNum} error code.
     * @param  message  a message describing the error, or {@link MemorySegment#NULL} if none.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public static void errorOccurred(final int err, final int code, final MemorySegment message) {
        try {
            List<ErrorHandler> messages = CURRENT.get();
            if (messages == null) {
                messages = new ArrayList<>();
                CURRENT.set(messages);
            }
            String text = NativeFunctions.toString(message);
            if (text == null || text.isBlank()) {
                text = "GDAL error #" + code;
            } else {
                /*
                 * GDAL puts line separator in the middle of messages, maybe for console output.
                 * Remove them as line feeds are handled either by JavaFX or by our log formatter.
                 */
                text = text.replace(System.lineSeparator(), " ");
            }
            messages.add(new ErrorHandler(err, text));
        } catch (Throwable e) {
            /*
             * Should never occurs. If it occurs anyway, we cannot let any exception to be thrown by this method,
             * because this method is invoked from native code. Even logging may be dangerous, because logger may
             * perform complex operations. The less risky is to only print the stack trace.
             */
            e.printStackTrace();
        }
    }

    /**
     * Remembers the {@code CPLError} value returned by a <abbr>GDAL</abbr> function in context where that value
     * cannot be consumed immediately. This is a safety in case a warning with a more descriptive message has not
     * been added to the {@link #CURRENT} list, and is used for remembering that an exception should be thrown.
     */
    static void errorOccurred(final int err) {
        List<ErrorHandler> messages = CURRENT.get();
        if (messages == null) {
            messages = new ArrayList<>();
            CURRENT.set(messages);
        }
        messages.add(new ErrorHandler(err, null));
    }

    /**
     * Invoked after the execution of <abbr>GDAL</abbr> functions when failure are considered warnings.
     * This method should be used as below.
     *
     * {@snippet lang="java" :
     *     public void myPublicMethod() throws DataStoreException {
     *         try {
     *             handle.invokeExact(…);
     *         } catch (Throwable e) {
     *             throws GDAL.propagate(e);
     *         } finally {
     *             ErrorHandler.report(store, "myPublicMethod");
     *         =
     *     }
     *     }
     *
     * @param store   the store where to redirect the <abbr>GDAL</abbr> warnings, or {@code null} if none.
     * @param method  name of the {@code GDALStore} method to declare as the source of the warning, or {@code null}.
     */
    static void report(final GDALStore store, final String method) {
        final List<ErrorHandler> messages = CURRENT.get();
        if (messages != null) {
            CURRENT.remove();
            for (final ErrorHandler m : messages) {
                final Level level;
                switch (m.err) {
                    default: {
                        level = Level.SEVERE;
                        if (store != null) {
                            store.getProvider().fatalError();
                        }
                        break;
                    }
                    case FAILURE: // Fall through
                    case 2: level = Level.WARNING; break;
                    case 1: level = Level.FINE;    break;
                    case 0: return;
                }
                /*
                 * The message may be null only when the warning has been reported by `errorOccurred(int)`,
                 * in which case a more descriptive message has probably been already reported in a prior
                 * instance of `ErrorHandler`.
                 */
                if (m.message != null) {
                    final var r = new LogRecord(level, m.message);
                    if (store != null) {
                        store.warning(method, r);
                    } else {
                        Class<?> src = (method != null) ? GDALStore.class : null;
                        Logging.completeAndLog(GDALStoreProvider.LOGGER, src, method, r);
                    }
                }
            }
        }
    }

    /**
     * Invoked after the execution of <abbr>GDAL</abbr> functions when failures are considered errors.
     *
     * @param store   the store where to redirect the <abbr>GDAL</abbr> warnings, or {@code null} if none.
     * @param method  name of the {@code GDALStore} method to declare as the source of the warning, or {@code null}.
     */
    static void throwOnFailure(final GDALStore store, final String method) throws DataStoreException {
        final List<ErrorHandler> messages = CURRENT.get();
        if (messages != null) {
            String error = null;
            boolean hasError = false;
            for (int i = messages.size(); --i >= 0;) {
                final ErrorHandler m = messages.get(i);
                if (m.err >= FAILURE) {
                    error = m.message;          // Message may be null.
                    if (m.err == FAILURE) {     // Keep logging of fatal errors.
                        messages.remove(i);
                    }
                    hasError = true;
                    if (error != null) break;
                }
            }
            report(store, method);
            if (hasError) {
                throw new DataStoreException(error);
            }
        }
    }

    /**
     * Handles a <abbr>GDAL</abbr> Common Portability Library error code ({@code CPLErr}).
     * The possible values are:
     *
     * <ul>
     *   <li>{@code CE_None}    = 0: No error or warning occurred.</li>
     *   <li>{@code CE_Debug}   = 1: The result contains debug information that users can ignore.</li>
     *   <li>{@code CE_Warning} = 2: The result contains informational warning.</li>
     *   <li>{@code CE_Failure} = 3: The action failed.</li>
     *   <li>{@code CE_Fatal}   = 4: A fatal error has occurred and <abbr>GDAL</abbr> should not be used anymore.
     *       The default GDAL behavior is to report errors to {@code stderr} and to abort the application.</li>
     * </ul>
     *
     * @param  err  the {@code CPLErr} enumeration value returned by <abbr>GDAL</abbr>.
     * @return whether the operation was successful, possibly with warnings.
     * @throws DataStoreException if the error category is {@code CE_Fatal} or higher.
     */
    static boolean checkCPLErr(final int err) throws DataStoreException {
        if (err > FAILURE) {
            throw new InternalDataStoreException("GDAL fatal error.");
        }
        return err < FAILURE;
    }
}
