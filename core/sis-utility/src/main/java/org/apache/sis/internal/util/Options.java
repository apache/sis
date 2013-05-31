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
package org.apache.sis.internal.util;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import org.apache.sis.util.Static;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.io.TableAppender;
import org.apache.sis.setup.OptionKey;


/**
 * Utility methods working with {@link OptionKey}. The user needs to hold a {@code Map<OptionKey<?>,Object>} field.
 * The methods in this class needs only that field value for performing their work.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class Options extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Options() {
    }

    /**
     * Returns the option value for the given key, or {@code null} if none.
     *
     * @param  <T>     The type of option value.
     * @param  options The map where to search for the value, or {@code null} if not yet created.
     * @param  key     The option for which to get the value.
     * @return The current value for the given option, or {@code null} if none.
     */
    public static <T> T get(final Map<?,?> options, final OptionKey<T> key) {
        ArgumentChecks.ensureNonNull("key", key);
        return (options != null) ? key.getElementType().cast(options.get(key)) : null;
    }

    /**
     * Sets the option value for the given key.
     *
     * @param  <T>     The type of option value.
     * @param  options The map where to set the value, or {@code null} if not yet created.
     * @param  key     The option for which to set the value.
     * @param  value   The new value for the given option, or {@code null} for removing the value.
     * @return The map of options, as a new map if the given map was null.
     */
    public static <T> Map<OptionKey<?>,Object> set(Map<OptionKey<?>,Object> options, final OptionKey<T> key, final T value) {
        ArgumentChecks.ensureNonNull("key", key);
        ArgumentChecks.ensureCanCast("value", key.getElementType(), value);
        if (value != null) {
            if (options == null) {
                options = new HashMap<>();
            }
            options.put(key, value);
        } else if (options != null) {
            options.remove(key);
        }
        return options;
    }

    /**
     * Lists the options in the given string builder.
     * This method is used for {@link Object#toString()} implementations.
     *
     * @param  options The map of options, or {@code null} if none.
     * @param  header  String to append if the given map is non-null.
     * @param  buffer  The buffer where to write the option values.
     */
    public static void list(final Map<? extends OptionKey<?>,?> options, final String header, final StringBuilder buffer) {
        if (options != null) {
            buffer.append(header).append("options={").append(System.lineSeparator());
            final TableAppender table = new TableAppender(buffer, "");
            table.setMultiLinesCells(true);
            for (final Map.Entry<? extends OptionKey<?>,?> entry : options.entrySet()) {
                table.append("    ").append(entry.getKey().getName());
                table.nextColumn();
                table.append(" = ").append(String.valueOf(entry.getValue()));
                table.nextLine();
            }
            try {
                table.flush();
            } catch (IOException e) {
                throw new AssertionError(e);
            }
            buffer.append('}');
        }
    }
}
