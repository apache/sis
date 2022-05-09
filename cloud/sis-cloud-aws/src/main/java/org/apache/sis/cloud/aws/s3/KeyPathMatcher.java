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
package org.apache.sis.cloud.aws.s3;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.sis.util.resources.Errors;


/**
 * Matches the string representation of {@link KeyPath} against a pattern.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class KeyPathMatcher implements PathMatcher {
    /**
     * Supported pattern syntax. The "glob" one is only partially supported.
     */
    private static final String REGEX = "regex", GLOB = "glob";

    /**
     * The regular expression pattern to match.
     */
    private final Pattern pattern;

    /**
     * Creates a new matcher.
     *
     * @param  syntaxAndPattern  filtering criteria of the {@code syntax:pattern}.
     * @param  separator         the {@link ClientFileSystem#separator} value.
     * @throws PatternSyntaxException if the pattern is invalid.
     * @throws UnsupportedOperationException if the pattern syntax is not known to this implementation.
     */
    KeyPathMatcher(final String syntaxAndPattern, final String separator) {
        final int s = syntaxAndPattern.indexOf(':');
        if (s < 0) {
            throw new IllegalArgumentException();
        }
        final String p;
        if (syntaxAndPattern.regionMatches(true, 0, REGEX, 0, s)) {
            p = syntaxAndPattern.substring(s + 1);
        } else if (syntaxAndPattern.regionMatches(true, 0, GLOB, 0, s)) {
            final int length = syntaxAndPattern.length();
            final StringBuilder sb = new StringBuilder(length);
            boolean quote = false;     // Whether we are inside a "\Q … \E" string.
loop:       for (int i = s; ++i < length;) {
                final char c = syntaxAndPattern.charAt(i);
                switch (c) {
                    default: {
                        if (!quote) sb.append("\\Q");
                        sb.append(c);
                        quote = true;
                        continue;
                    }
                    case '\\': {
                        if (++i >= length) break loop;
                        sb.append(syntaxAndPattern.charAt(i+1));
                        continue;
                    }
                    case '?': {
                        if (quote) sb.append("\\E");
                        sb.append('.');
                        quote = false;
                        continue;
                    }
                    case '*': {     // Handles also the "**" case.
                        final boolean multi = (i+1 < length && syntaxAndPattern.charAt(i+1) == '*');
                        if (multi) i++;
                        if (quote) sb.append("\\E");
                        if (multi) {
                            sb.append(".*");
                        } else if (separator.length() == 1) {
                            sb.append("[^").append(separator).append("]*");
                        } else {
                            throw unsupportedSyntax(syntaxAndPattern, s);
                        }
                        quote = false;
                        continue;
                    }
                    case ']': case '}': // Fall through (note: we could provide an "unexpected character" error message instead).
                    case '[': case '{': throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnsupportedOperation_1, c));
                }
            }
            if (quote) sb.append("\\E");
            p = sb.toString();
        } else {
            throw unsupportedSyntax(syntaxAndPattern, s);
        }
        pattern = Pattern.compile(p);
    }

    /**
     * Returns the exception to throw for an unsupported syntax.
     */
    private static UnsupportedOperationException unsupportedSyntax(final String syntaxAndPattern, final int s) {
        return new UnsupportedOperationException(Errors.format(
                Errors.Keys.IllegalArgumentValue_2, "syntax", syntaxAndPattern.substring(0, s)));
    }

    /**
     * Tells if given path matches the regular expression pattern.
     */
    @Override
    public boolean matches(final Path path) {
        return pattern.matcher(path.toString()).matches();
    }

    /**
     * Returns a string expression of this matcher.
     */
    @Override
    public String toString() {
        return "PathMatcher[regex=\"" + pattern + "\"]";
    }
}
