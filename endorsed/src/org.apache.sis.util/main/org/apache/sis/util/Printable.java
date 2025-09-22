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
package org.apache.sis.util;


/**
 * Object providing a {@code print()} method for sending a string representation to the standard output stream.
 * A call to {@code object.print()} is often (but not necessarily) equivalent to {@code System.out.println(object)},
 * except that <i>ANSI escape codes</i> (a.k.a. ECMA-48, ISO/IEC 6429 and X3.64 standards) may be used for
 * syntax coloring if the terminal supports it. The character encoding may also be more suitable on some platforms.
 * Finally, some implementations may be more verbose than {@code toString()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public interface Printable {
    /**
     * Prints a string representation of this object to the {@linkplain System#out standard output stream}.
     * If a {@linkplain java.io.Console console} is attached to the running JVM (i.e. if the application is
     * run from the command-line and the output is not redirected to a file) and if Apache SIS thinks that
     * the console supports the ANSI escape codes (a.k.a. X3.64), then a syntax coloring may be applied.
     *
     * <p>This is a convenience method for debugging purpose and for console applications.</p>
     */
    @Debug
    void print();
}
