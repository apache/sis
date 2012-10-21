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

/**
 * Extensions to standard Java {@link java.io.Reader} and {@link java.io.Writer} for I/O operations.
 * Many classes defined in this package are actually filters applying on-the-fly formatting while
 * writing text to the output device. For example {@link org.apache.sis.io.IndentedLineFormatter}
 * adds indentation at the beginning of every new line, and {@link org.apache.sis.io.TableFormatter}
 * replaces all occurrence of {@code '\t'} by the amount of spaces needed for producing a tabular
 * output.
 *
 * {@section Unicode characters}
 * Some formatters in this package make extensive use of Unicode characters. This may produce
 * unexpected results in a Windows console, unless the underlying output stream uses the correct
 * encoding (e.g. {@code OutputStreamWriter(System.out, "Cp437")}). To display the appropriate
 * code page for a Windows console, type <code>chcp</code> on the command line.
 *
 * {@section Supplementary Unicode characters}
 * This package can handle the {@linkplain java.lang.Character#isSupplementaryCodePoint(int)
 * Unicode supplementary characters}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-1.0)
 * @version 0.3
 * @module
 */
package org.apache.sis.io;
