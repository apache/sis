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
package org.apache.sis.gui;

import java.util.Locale;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.sis.util.Locales;
import org.apache.sis.util.resources.Errors;


/**
 * All command-line options allowed by the launcher.
 * The name used on the command-line is the lower-cases variant of the enumeration name.
 *
 * <p>This is a simplified version of {@link org.apache.sis.console.Option}.
 * More options may be ported here as needed.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
enum Option {
    /**
     * The locale for the application.
     *
     * @see org.apache.sis.console.Option#LOCALE
     */
    LOCALE;

    /**
     * The code given to {@link System#exit(int)} when the program failed because of a unknown option.
     *
     * @see org.apache.sis.console.Command#INVALID_OPTION_EXIT_CODE
     */
    private static final int INVALID_OPTION_EXIT_CODE = 2;

    /**
     * Parses the options and removes them from the collection backing the iterator.
     * After this method returned, the remaining elements in the collections can be
     * interpreted as files to open. This method exists the <abbr>JVM</abbr> with an
     * error code if an unknown option is found.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    static void parse(final Iterator<String> it) {
        while (it.hasNext()) {
            String label = it.next().trim();
            if (label.startsWith("--")) {
                final Option option;
                String value = null;
                try {
                    final int s = label.indexOf('=');
                    if (s >= 0) {
                        value = label.substring(s+1).trim();
                        label = label.substring(0, s).trim();
                    }
                    option = valueOf(label.substring(2).toUpperCase());
                    it.remove();
                    if (value == null) {
                        value = it.next().trim();
                        it.remove();
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println(Errors.format(Errors.Keys.UnknownOption_1, label));
                    System.exit(INVALID_OPTION_EXIT_CODE);
                    throw e;
                } catch (NoSuchElementException e) {
                    System.err.println(Errors.format(Errors.Keys.MissingValueForOption_1, label));
                    System.exit(INVALID_OPTION_EXIT_CODE);
                    throw e;
                }
                try {
                    switch (option) {
                        case LOCALE: Locale.setDefault(Locales.parse(value)); break;
                    }
                } catch (RuntimeException e) {
                    System.err.println(Errors.format(Errors.Keys.IllegalOptionValue_2, label, value));
                    System.exit(INVALID_OPTION_EXIT_CODE);
                    throw e;
                }
            }
        }
    }
}
