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
package org.apache.sis.internal.setup;

import java.util.logging.LogManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.apache.sis.internal.storage.io.IOUtilities.CURRENT_DIRECTORY_SYMBOL;


/**
 * Loads {@code conf/logging.properties} file and filter the {@code %p} pattern
 * before to delegate to Java logging system. The filtering replaces {@code "%p"}
 * by the parent directory of configuration file.
 *
 * <p>This class should not use any SIS classes (except constants inlined at compile-time)
 * because it may be invoked early while the application is still initializing.</p>
 *
 * <p>This class is not referenced directly by other Java code.
 * Instead, it is specified at JVM startup time like below:</p>
 *
 * {@preformat shell
 *     java -Djava.util.logging.config.class="org.apache.sis.internal.setup.LoggingConfiguration"
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class LoggingConfiguration {
    /**
     * The property to filter.
     */
    private static final String PROPERTY = "java.util.logging.FileHandler.pattern";

    /**
     * The pattern to replace.
     */
    private static final String PATTERN = "%p";

    /**
     * Invoked by Java if the {@code java.util.logging.config.class} property is set
     * to this class name. This constructor filters {@code conf/logging.properties},
     * then delegate to Java logging system.
     *
     * @throws IOException if an error occurred while reading the configuration file.
     */
    public LoggingConfiguration() throws IOException {
        final String file = System.getProperty("java.util.logging.config.file");
        if (file != null) {
            final Path path = Paths.get(file).normalize();
            final StringBuilder buffer = new StringBuilder(600);
            for (String line : Files.readAllLines(path)) {
                if (!(line = line.trim()).isEmpty() && line.charAt(0) != '#') {
                    final int base = buffer.length();
                    buffer.append(line).append('\n');
                    if (line.startsWith(PROPERTY)) {
                        final int i = buffer.indexOf(PATTERN, base + PROPERTY.length());
                        if (i >= 0) {
                            Path parent = path;
                            for (int j=Math.min(parent.getNameCount(), 2); --j >= 0;) {
                                parent = parent.getParent();
                            }
                            String replacement = (parent != null) ? parent.toString() : CURRENT_DIRECTORY_SYMBOL;
                            replacement = replacement.replace(File.separatorChar, '/');
                            buffer.replace(i, i + PATTERN.length(), replacement);
                        }
                    }
                }
            }
            LogManager.getLogManager().readConfiguration​(new ByteArrayInputStream(buffer.toString().getBytes()));
        }
    }
}
