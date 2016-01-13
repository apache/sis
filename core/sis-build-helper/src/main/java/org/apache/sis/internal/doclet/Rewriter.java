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
package org.apache.sis.internal.doclet;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;


/**
 * Rewrites some HTML files after they have been generated.
 * The {@link #hyphenation()} method scans for {@code <code>...</code>} elements (without attributes)
 * in the HTML files and performs the following changes in any occurrences found:
 *
 * <ul>
 *   <li>For any Java identifier, insert soft-hyphens between lower-case letters followed by an upper-case letter.</li>
 *   <li>Between any Java identifiers separated by a {@code '.'} character, insert a zero-width space before the dot.</li>
 * </ul>
 *
 * The intend is to avoid large amount of white spaces in Javadoc when a line content long code.
 *
 * <p>Current version does not try to extends the standard doclet because the later is under revision in JDK 9.
 * We may revisit when JDK 9 become available, if their new standard doclet API is public.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class Rewriter {
    /**
     * The encoding to use for reading and writing HTML files.
     */
    private static final String ENCODING = "UTF-8";

    /**
     * The HTML element to search. Only text inside those elements will be processed.
     */
    private static final String OPEN = "<code>";

    /**
     * The closing HTML element.
     */
    private static final String CLOSE = "</code>";

    /**
     * The file being processed.
     */
    private File file;

    /**
     * The full content of {@link #file}. Hyphenation will be done directly in this string builder.
     */
    private final StringBuilder content;

    /**
     * A temporary buffer for loading files.
     */
    private final char[] buffer;

    /**
     * {@code true} if the {@linkplain #content} has been modified an need to be saved.
     */
    private boolean modified;

    /**
     * Creates a new {@code Rewriter}.
     */
    Rewriter() throws IOException {
        content = new StringBuilder(256 * 1024);
        buffer = new char[4096];
    }

    /**
     * Processes recursively all HTML files in the given directory.
     */
    void processDirectory(final File directory) throws IOException {
        for (final File file : directory.listFiles()) {
            if (!file.isHidden()) {
                final String name = file.getName();
                if (file.isDirectory()) {
                    if (!name.startsWith("class-use") && !name.startsWith("doc-files")) {
                        processDirectory(file);
                    }
                } else if (name.endsWith(".html")) {
                    if (name.startsWith("package-")) {
                        // Skip package-frame, package-tree, package-use, everything except package-summary.
                        if (!name.startsWith("package-summary")) {
                            continue;
                        }
                    }
                    load(file);
                    hyphenation();
                    save();
                }
            }
        }
    }

    /**
     * Loads the given file.
     */
    private void load(final File file) throws IOException {
        modified = false;
        this.file = file;
        content.setLength(0);
        final Reader in = new InputStreamReader(new FileInputStream(file), ENCODING);
        try {
            int n;
            while ((n = in.read(buffer)) >= 0) {
                content.append(buffer, 0, n);
            }
        } finally {
            in.close();
        }
    }

    /**
     * Saves the files after processing, provided that the content has changed.
     * This method does nothing if the content did not changed.
     */
    private void save() throws IOException {
        if (modified) {
            final Writer out = new OutputStreamWriter(new FileOutputStream(file), ENCODING);
            try {
                out.append(content);
            } finally {
                out.close();
            }
        }
    }

    /**
     * For any Java identifier inside {@code <code>...</code>} elements, inserts soft-hyphens or zero-width spaces
     * where this method decides that the code could be wrapped on the next line.
     */
    private void hyphenation() {
        int i = 0;
        while ((i = content.indexOf(OPEN, i)) >= 0) {
            int stopAt = content.indexOf(CLOSE, i += OPEN.length());
            /*
             * At this point, we are inside a <code>...</code> element. The text begins at 'i',
             * which will be incremented until we reach the "</code>" string at 'stopAt'.
             * The 'c' variable will contain the code point at position 'i'.
             *
             * Code below assumes that the HTML is well formed. In particular, we do not check
             * for IndexOutOfBoundsException when a well formed HTML would have more content.
             */
process:    while (i < stopAt) {
                int c = content.codePointAt(i);
                /*
                 * We need to skip every other HTML elements inside <code>...</code>. For example if we have
                 * <code><a href="...">...</a></code>, we do not want to process the text inside <a>.
                 */
                while (c == '<') {
                    int n = 1;
                    do {
                        switch (content.charAt(++i)) {
                            case '<': n++; break;
                            case '>': n--; break;
                        }
                    } while (n != 0);
                    stopAt = content.indexOf(CLOSE, i);
                    if (++i >= stopAt) {
                        break process;
                    }
                    c = content.codePointAt(i);
                }
                /*
                 * At this point, we know that we are inside a <code>...</code> element and not inside
                 * another HTML sub-element.
                 */
                if ((i += Character.charCount(c)) >= stopAt) {
                    break;
                }
                if (Character.isJavaIdentifierStart(c)) {
                    int previous = c;
                    c = content.codePointAt(i);                     // 'i' is already on the next character.
                    while (Character.isJavaIdentifierPart(c)) {
                        if (Character.isUpperCase(c) && Character.isLowerCase(previous)) {
                            modified = true;
                            content.insert(i++, '­');               // Soft hyphen (U+00AD)
                            stopAt++;
                        }
                        if ((i += Character.charCount(c)) >= stopAt) {
                            break process;
                        }
                        previous = c;
                        c = content.codePointAt(i);
                    }
                    /*
                     * At this point, we are right after a Java identifier. If the character is a dot
                     * followed immediately by another Java identifier, allow line break before the dot.
                     * We may need to skip the "()" in method names.
                     *
                     * NOTE: this code does not handle the <code><a.../a>.<a.../a></code> case.
                     *       We may revisit this issue when porting the doclet to JDK 9.
                     */
                    if (c == '(') c = content.codePointAt(++i);
                    if (c == ')') c = content.codePointAt(++i);
                    if (c == '.' && Character.isJavaIdentifierStart(content.codePointAt(i+1))) {
                        content.insert(i++, '\u200B');              // Zero width space.
                        stopAt++;
                    }
                }
            }
        }
    }
}
