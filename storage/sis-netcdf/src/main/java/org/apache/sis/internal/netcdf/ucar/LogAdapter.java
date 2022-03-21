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
package org.apache.sis.internal.netcdf.ucar;

import java.util.logging.Level;
import org.apache.sis.util.CharSequences;
import org.apache.sis.storage.event.StoreListeners;


/**
 * Forwards netCDF logging to the Apache SIS warning listeners.
 * NetCDF sends message to a user-specified {@link java.util.Formatter} with one message per line.
 * This class intercepts the characters and send them to the {@link StoreListeners} every time
 * that a complete line has been received.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   0.8
 * @module
 */
final class LogAdapter implements Appendable {
    /**
     * Temporary buffer where to append the netCDF logging messages.
     */
    private final StringBuilder buffer = new StringBuilder();

    /**
     * Where to sends the warning messages.
     */
    private final StoreListeners listeners;

    /**
     * Creates a new adapter which will send lines to the given listeners.
     */
    LogAdapter(final StoreListeners listeners) {
        this.listeners = listeners;
    }

    /**
     * Appends the given message and forwards to the listeners after we completed a line.
     */
    @Override
    public Appendable append(final CharSequence message) {
        if (message.length() != 0) {
            final CharSequence[] sp = CharSequences.splitOnEOL(message);
            int count = sp.length;
            /*
             * If the last line does not ends with a EOL character, we will not send it to the listeners.
             * Instead we will copy it to the buffer for concatenation with the next characters appended.
             */
            final char c = message.charAt(message.length() - 1);
            if (c != '\r' && c != '\n') {
                count--;
            }
            /*
             * Send complete lines to the warning listeners.
             */
            for (int i=0; i<count; i++) {
                final CharSequence line = sp[i];
                if (buffer.length() != 0) {
                    log(buffer.append(line));
                    buffer.setLength(0);
                } else {
                    log(line);
                }
            }
            /*
             * If the last line has not been sent to the warning listeners, copy in the buffer
             * for next iteration.
             */
            if (count != sp.length) {
                buffer.append(sp[count]);
            }
        }
        return this;
    }

    /**
     * Appends the given message and forwards to the listeners after we completed a line.
     */
    @Override
    public Appendable append(CharSequence message, int start, int end) {
        if (message == null) message = "null";              // For compliance with Appendable specification.
        return append(message.subSequence(start, end));
    }

    /**
     * Appends the given character and forwards to the listeners after we completed a line.
     */
    @Override
    public Appendable append(char c) {
        if (c != '\r' && c != '\n') {
            buffer.append(c);
        } else if (buffer.length() != 0) {
            log(buffer);
            buffer.setLength(0);
        }
        return this;
    }

    /**
     * Sends the given message to the listeners if the message is non-white.
     */
    private void log(CharSequence message) {
        message = CharSequences.trimWhitespaces(message);
        if (message.length() != 0) {
            listeners.warning(Level.FINE, message.toString(), null);
        }
    }
}
