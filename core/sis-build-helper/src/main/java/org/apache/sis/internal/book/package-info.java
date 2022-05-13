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
 * Takes the source HTML files in the {@code sis-site/main/source/developer-guide/} directory
 * and assembles them in a single file to be published in the {@code asf-staging/book/} directory.
 *
 * <p>The main class in this package is {@link org.apache.sis.internal.book.Assembler}.
 * Other classes are helper classes that should be ignored. Assuming the following directory layout:</p>
 *
 * <pre>&lt;current directory&gt;
 * ├─ master
 * │   └─ core
 * │       └─ sis-build-helper
 * └─ site
 *     ├─ main
 *     │   └─ source
 *     └─ asf-staging
 *         └─ book
 * </pre>
 *
 * Then the command can be used as below on Unix systems:
 *
 * <pre>java -classpath master/core/sis-build-helper/target/classes org.apache.sis.internal.book.Assembler site</pre>
 *
 * <h2>Future evolution</h2>
 * We may replace (at least partially) this tools by some more advanced open-source alternatives.
 * Known candidates are:
 *
 * <ul>
 *   <li><a href="http://www.xmlmind.com/ebookc/">XMLmind Ebook Compiler</a></li>
 * </ul>
 *
 * A goal is to keep HTML5 as the language of source files, not DocBook or AsciiDoc or others.
 * See <a href="http://www.xmlmind.com/tutorials/HTML5Books/HTML5Books.html">HTML5 as an alternative to DITA and DocBook</a>.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.7
 */
package org.apache.sis.internal.book;
