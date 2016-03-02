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

import java.util.Arrays;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * A limited set of color and font attributes assignable to characters at formatting time.
 * Those attributes are used by SIS formatters for providing some syntax coloring,
 * for example in the {@link org.apache.sis.io.wkt} package.
 *
 * <p>This enumeration is restricted to a subset of the <cite>ANSI escape codes</cite> (a.k.a.
 * ECMA-48, ISO/IEC 6429 and X3.64 standards) because SIS uses them mostly for syntax coloring in
 * console outputs. However those attributes can also occasionally be used for HTML rendering.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 *
 * @see <a href="http://en.wikipedia.org/wiki/ANSI_escape_code">Wikipedia: ANSI escape codes</a>
 * @see org.apache.sis.io.wkt.Colors
 */
public enum X364 {
    /** Reset all attributes to their default value. */ RESET               ((byte)  0, null),
    /** Normal intensity (not {@link #BOLD}).        */ NORMAL              ((byte) 22, null),
    /** Bold intensity.                              */ BOLD                ((byte)  1, null),
    /** Faint intensity.                             */ FAINT               ((byte)  2, null),
    /** Single underline.                            */ UNDERLINE           ((byte)  4, null),
    /** No underline.                                */ NO_UNDERLINE        ((byte) 24, null),
    /** Red foreground color, normal intensity.      */ FOREGROUND_RED      ((byte) 31, "red"),
    /** Green foreground color, normal intensity.    */ FOREGROUND_GREEN    ((byte) 32, "green"),
    /** Yellow foreground color, normal intensity.   */ FOREGROUND_YELLOW   ((byte) 33, "yellow"),
    /** Blue foreground color, normal intensity.     */ FOREGROUND_BLUE     ((byte) 34, "blue"),
    /** Magenta foreground color, normal intensity.  */ FOREGROUND_MAGENTA  ((byte) 35, "magenta"),
    /** Cyan foreground color, normal intensity.     */ FOREGROUND_CYAN     ((byte) 36, "cyan"),
    /** Gray foreground color, normal intensity.     */ FOREGROUND_GRAY     ((byte) 37, "gray"),
    /** Reset the foreground color.                  */ FOREGROUND_DEFAULT  ((byte) 39, null),
    /** Red background color, normal intensity.      */ BACKGROUND_RED      (FOREGROUND_RED),
    /** Green background color, normal intensity.    */ BACKGROUND_GREEN    (FOREGROUND_GREEN),
    /** Yellow background color, normal intensity.   */ BACKGROUND_YELLOW   (FOREGROUND_YELLOW),
    /** Blue background color, normal intensity.     */ BACKGROUND_BLUE     (FOREGROUND_BLUE),
    /** Magenta background color, normal intensity.  */ BACKGROUND_MAGENTA  (FOREGROUND_MAGENTA),
    /** Cyan background color, normal intensity.     */ BACKGROUND_CYAN     (FOREGROUND_CYAN),
    /** Gray background color, normal intensity.     */ BACKGROUND_GRAY     (FOREGROUND_GRAY),
    /** Reset the background color.                  */ BACKGROUND_DEFAULT  (FOREGROUND_DEFAULT);

    /**
     * The list of codes having a non-null {@linkplain #color} name.
     * They are the codes in the range 31 to 37 inclusive.
     *
     * @see #forColorName(String)
     */
    private static final X364[] NAMED;
    static {
        NAMED = Arrays.copyOfRange(values(), 6, 13);
    }

    /**
     * The first character of the {@link #START} escape string.
     */
    public static final char ESCAPE = '\u001B';

    /**
     * The second character of the {@link #START} escape string.
     */
    public static final char BRACKET = '[';

    /**
     * The Control Sequence Introducer (CSI).
     * Must be the concatenation of {@link #ESCAPE} with {@link #BRACKET}.
     */
    private static final String START = "\u001B[";

    /**
     * The end of escape sequences. Fixed to {@code 'm'} for now, but a wider range
     * of letters actually exists for different operations.
     */
    private static final char END = 'm';

    /**
     * The X3.64 code.
     */
    private final transient byte code;

    /**
     * The X3.64 escape sequence, built when first needed.
     */
    private transient String sequence;

    /**
     * Foreground or background flavors of this enum.
     */
    private transient X364 foreground, background;

    /**
     * The color name, or {@code null} if none.
     */
    public final String color;

    /**
     * Creates a new code.
     *
     * @param code  The X.364 numerical code.
     * @param color The color name, or {@code null} if none.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    private X364(final byte code, final String color) {
        this.code  = code;
        this.color = color;
        foreground = this;
        background = this;
    }

    /**
     * Creates a new background code.
     *
     * @param foreground The X.364 code for a foreground color.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    private X364(final X364 foreground) {
        this((byte) (foreground.code + 10), foreground.color);
        this.foreground = foreground;
        this.background = foreground.background = this;
    }

    /**
     * Returns the enum for the foreground color.
     *
     * @return The foreground color, or {@code this} if this enum is already a foreground color.
     */
    public X364 foreground() {
        return foreground;
    }

    /**
     * Returns the enum for the background color.
     *
     * @return The background color, or {@code this} if this enum is already a background color.
     */
    public X364 background() {
        return background;
    }

    /**
     * Returns the ANSI escape sequence.
     *
     * @return The ANSI escape sequence.
     */
    public String sequence() {
        if (sequence == null) {
            sequence = (START + code + END).intern();
            // We used the string.intern() method in order to avoid worrying about memory barrier
            // (synchronization or volatile variable) since intern() does its own synchronization.
            // The String will live for the whole library lifetime anyway, and if there is other
            // X3.64 libraries on the JVM we may share the strings with them as a side effect.
        }
        return sequence;
    }

    /**
     * Removes all escape codes from the given string.
     *
     * @param  text      The string which may contains escape codes.
     * @param  fromIndex The index from which to start the process.
     * @param  toIndex   The index after the last character to process.
     * @return Text without the escape codes, or the given {@code text} reference if
     *         it didn't contained any escape codes.
     */
    public static CharSequence plain(final CharSequence text, int fromIndex, final int toIndex) {
        int i = CharSequences.indexOf(text, START, fromIndex, toIndex);
        if (i >= 0) {
            StringBuilder buffer = null;
search:     do {
                final int start = i;
                i += START.length();
                final int end = CharSequences.indexOf(text, END, i, toIndex);
                if (end < 0) {
                    break;
                }
                while (i < end) {
                    final char c = text.charAt(i++);
                    if (c < '0' || c > '9') {
                        continue search;
                    }
                }
                if (buffer == null) {
                    buffer = new StringBuilder(toIndex - fromIndex);
                }
                buffer.append(text, fromIndex, start);
                fromIndex = ++i; // The ++ is for skipping the END character.
            } while ((i = CharSequences.indexOf(text, START, i, toIndex)) >= 0);
            if (buffer != null) {
                return buffer.append(text, fromIndex, toIndex);
            }
        }
        return text.subSequence(fromIndex, toIndex);
    }

    /**
     * Returns the number of Unicode code points in the given string without the ANSI escape codes.
     * This is equivalent to <code>{@linkplain CharSequences#codePointCount(CharSequence)
     * CharSequences.codePointCount}({@linkplain #plain plain}(text))</code> without the
     * cost of creating a temporary string.
     *
     * @param  text      The string which may contains escape codes.
     * @param  fromIndex The index from which to start the computation.
     * @param  toIndex   The index after the last character to take in account.
     * @return The length of the given string without escape codes.
     */
    public static int lengthOfPlain(final CharSequence text, final int fromIndex, final int toIndex) {
        int i = CharSequences.indexOf(text, START, fromIndex, toIndex);
        if (i < 0) {
            return CharSequences.codePointCount(text, fromIndex, toIndex);
        }
        int last   = fromIndex;
        int length = 0;
search: do {
            final int start = i;
            i += START.length();
            final int end = CharSequences.indexOf(text, END, i, toIndex);
            if (end < 0) {
                break;
            }
            while (i < end) {
                final char c = text.charAt(i++);
                if (c < '0' || c > '9') {
                    continue search; // Not an X.364 sequence.
                }
            }
            length += CharSequences.codePointCount(text, last, start);
            last = ++i; // The ++ is for skipping the END character.
        } while ((i = CharSequences.indexOf(text, START, i, toIndex)) >= 0);
        length += CharSequences.codePointCount(text, last, toIndex);
        assert CharSequences.codePointCount(plain(text, fromIndex, toIndex)) == length : text.subSequence(fromIndex, toIndex);
        return length;
    }

    /**
     * Returns the enumeration value for the given color name.
     * The search is case-insensitive.
     *
     * @param  color The color name.
     * @return The code for the given color name.
     * @throws IllegalArgumentException If no code has been found for the given color name.
     */
    public static X364 forColorName(String color) throws IllegalArgumentException {
        color = CharSequences.trimWhitespaces(color);
        ArgumentChecks.ensureNonEmpty("color", color);
        for (final X364 code : NAMED) {
            if (color.equalsIgnoreCase(code.color)) {
                return code;
            }
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "color", color));
    }

    /**
     * Returns {@code true} if we think that the operating system supports ANSI sequences.
     * This method performs a very naive and approximative check. Result is just a hint and
     * may be wrong.
     *
     * <p>This method does not check if a {@linkplain java.io.Console console} is attached to the
     * running JVM because the caller may want to write to the {@linkplain System#err standard
     * error stream} instead than the {@linkplain System#out standard output stream}, in which
     * case the console information is not applicable.</p>
     *
     * @return {@code true} if we think that the operating system supports ANSI codes.
     *         This method may conservatively returns {@code false} in case of doubt.
     */
    public static boolean isAnsiSupported() {
        String terminal;
        try {
            terminal = System.getenv("COLORTERM");
            if (terminal != null) {
                // Non-numerical value - don't try to parse that.
                return true;
            }
            terminal = System.getenv("CLICOLOR");
        } catch (SecurityException e) {
            return false; // Okay according javadoc.
        }
        if (terminal != null) try {
            return Integer.parseInt(terminal) != 0;
        } catch (NumberFormatException e) {
            // Okay to ignore according our javadoc.
        }
        return false;
    }
}
