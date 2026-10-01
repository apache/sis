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
 * Extensions to standard Java I/O and formatting <abbr>API</abbr>.
 * This package provides subtypes or utility methods for the
 * {@link java.io.Reader}, {@link java.io.Writer}, {@link java.lang.Appendable} and {@link java.text.Format} classes.
 * Some subclasses are filters applying on-the-fly formatting while writing text to the output stream.
 * For example, {@link org.apache.sis.io.LineAppender} can wrap lines to some maximal line length (e.g. 80 characters),
 * and {@link org.apache.sis.io.TableAppender} replaces all occurrence of {@code '\t'} by the number of spaces needed
 * for producing a tabular output.
 *
 * <h2>Unicode characters usage in <abbr>SIS</abbr></h2>
 * Some classes in this package make extensive use of Unicode characters, in particular for the formatting of trees and tables.
 * Outputs printed to {@link java.lang.System#out} may not appear correctly if the character encoding of the console is not the
 * character encoding specified by the {@code stdout.encoding} system property (usually <abbr>UTF</abbr>-8).
 *
 * <p>This package, like most of Apache <abbr>SIS</abbr> library, can handle the
 * {@linkplain java.lang.Character#isSupplementaryCodePoint(int) Unicode supplementary characters}.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.7
 * @since   0.3
 */
package org.apache.sis.io;
