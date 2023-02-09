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

import java.util.logging.LogManager;
import java.util.logging.FileHandler;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Loads a logging configuration file using Java logging syntax augmented with Apache SIS extensions.
 * The {@code "java.util.logging.config.file"} {@linkplain System#getProperty(String) system property}
 * must be set to the path of a {@linkplain java.util.Properties properties} file containing configuration
 * in the format described by {@link LogManager}.
 * This class applies the following filtering on the configuration file:
 *
 * <ul>
 *   <li>Changes in {@code "java.util.logging.FileHandler.pattern"} property value:
 *     <ul>
 *       <li>Replace {@code %p} by the parent directory of the logging configuration file.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Example</h2>
 * Given an application with the following directory structure:
 *
 * <pre>application
 * ├─ conf/
 * │  └─ logging.properties
 * └─ log/</pre>
 *
 * If the {@code logging.properties} contains the following line:
 *
 * <pre>java.util.logging.FileHandler.pattern = <b>%p</b>/log/myapp.log</pre>
 *
 * Then the {@code %p} characters of that property value will be replaced by the path to the
 * {@code application} directory, thus allowing the application to log in the {@code log/} sub-directory.
 * Other special components such as {@code %t}, {@code %h}, {@code %g} or {@code %u} are handled as usual
 * (i.e. as documented by {@link FileHandler}).
 *
 * <h2>Usage</h2>
 * This class should not referenced directly by other Java code.
 * Instead, it should be specified at JVM startup time like below:
 *
 * <pre>java -Djava.util.logging.config.class=org.apache.sis.util.logging.Initializer \
 *     -Djava.util.logging.config.file=<i>path/to/my/application/conf/logging.properties</i></pre>
 *
 * See for example the {@code bin/sis} shell script in Apache SIS binary distribution.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @see FileHandler
 *
 * @since 1.3
 */
public class Initializer {
    /*
     * WARNING: This class shall not use any SIS classes (except constants inlined at compile-time)
     *          because it may be invoked early while the application is still initializing.
     */

    /**
     * The property for which to replace the {@value #PATTERN} string.
     */
    private static final String PROPERTY = "java.util.logging.FileHandler.pattern";

    /**
     * The pattern to replace.
     */
    private static final String PATTERN = "%p";

    /**
     * Configures Java logging using a filtered configuration file.
     * This constructor gets the configuration file referenced by
     * the {@code "java.util.logging.config.file"} system property,
     * applies the filtering described in class javadoc,
     * then gives the filtered configuration to {@link LogManager#readConfiguration​(InputStream)}.
     *
     * <p>This constructor should not be invoked directly.
     * See class javadoc for usage example.</p>
     *
     * @throws IOException if an error occurred while reading the configuration file.
     */
    public Initializer() throws IOException {
        final String file = System.getProperty("java.util.logging.config.file");
        if (file != null) {
            final Path path = Path.of(file).normalize();
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
                            String replacement = (parent != null) ? parent.toString() : ".";
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
