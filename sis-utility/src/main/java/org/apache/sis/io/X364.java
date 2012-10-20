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
package org.apache.sis.io;

import org.apache.sis.util.StringBuilders;


/**
 * Escape codes from ANSI X3.64 standard (aka ECMA-48 and ISO/IEC 6429).
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 *
 * @see <a href="http://en.wikipedia.org/wiki/ANSI_escape_code">ANSI escape codes</a>
 * @see org.apache.sis.io.wkt.Colors
 */
public enum X364 {
    /** Reset all attributes to their default value. */ RESET               ((byte)  0),
    /** Normal intensity (not {@link #BOLD}).        */ NORMAL              ((byte) 22),
    /** Bold intensity.                              */ BOLD                ((byte)  1),
    /** Faint intensity.                             */ FAINT               ((byte)  2),
    /** Red foreground color, normal intensity.      */ FOREGROUND_RED      ((byte) 31),
    /** Green foreground color, normal intensity.    */ FOREGROUND_GREEN    ((byte) 32),
    /** Yellow foreground color, normal intensity.   */ FOREGROUND_YELLOW   ((byte) 33),
    /** Blue foreground color, normal intensity.     */ FOREGROUND_BLUE     ((byte) 34),
    /** Magenta foreground color, normal intensity.  */ FOREGROUND_MAGENTA  ((byte) 35),
    /** Cyan foreground color, normal intensity.     */ FOREGROUND_CYAN     ((byte) 36),
    /** Gray foreground color, normal intensity.     */ FOREGROUND_GRAY     ((byte) 37),
    /** Reset the foreground color.                  */ FOREGROUND_DEFAULT  ((byte) 39),
    /** Red background color, normal intensity.      */ BACKGROUND_RED      (FOREGROUND_RED),
    /** Green background color, normal intensity.    */ BACKGROUND_GREEN    (FOREGROUND_GREEN),
    /** Yellow background color, normal intensity.   */ BACKGROUND_YELLOW   (FOREGROUND_YELLOW),
    /** Blue background color, normal intensity.     */ BACKGROUND_BLUE     (FOREGROUND_BLUE),
    /** Magenta background color, normal intensity.  */ BACKGROUND_MAGENTA  (FOREGROUND_MAGENTA),
    /** Cyan background color, normal intensity.     */ BACKGROUND_CYAN     (FOREGROUND_CYAN),
    /** Gray background color, normal intensity.     */ BACKGROUND_GRAY     (FOREGROUND_GRAY),
    /** Reset the background color.                  */ BACKGROUND_DEFAULT  (FOREGROUND_DEFAULT);

    /**
     * The beginning of escape sequences.
     */
    private static final String START = "\u001B[";

    /**
     * The end of escape sequences.
     */
    private static char END = 'm';

    /**
     * The X3.64 code.
     */
    private final byte code;

    /**
     * The X3.64 escape sequence. Created only when first needed.
     */
    private transient String sequence;

    /**
     * Foreground or background flavors of this enum.
     */
    private X364 foreground, background;

    /**
     * Creates a new code.
     *
     * @param code The X.364 code.
     */
    private X364(final byte code) {
        this.code  = code;
        foreground = this;
        background = this;
    }

    /**
     * Creates a new background code.
     *
     * @param code The X.364 code.
     */
    private X364(final X364 foreground) {
        this((byte) (foreground.code + 10));
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
     * Returns the X3.64 escape sequence.
     *
     * @return The X3.64 escape sequence.
     */
    public String sequence() {
        if (sequence == null) {
            sequence = START + code + END;
        }
        return sequence;
    }

    /**
     * Replaces escape codes in the given string by HTML {@code <font>} instructions.
     * If no HTML instruction is associated to the given escape code, then the escape
     * sequence is removed.
     *
     * @param  text The text with X3.64 sequences.
     * @return The text with HTML {@code <font>} instructions.
     */
    public static String toHTML(final String text) {
        final StringBuilder buffer = new StringBuilder(text);
        StringBuilders.replace(buffer, "&", "&amp;");
        StringBuilders.replace(buffer, "<", "&lt;");
        StringBuilders.replace(buffer, ">", "&gt;");
        boolean fontApplied = false;
        StringBuilder tmp = null;
        for (int i=buffer.indexOf(START); i>=0; i=buffer.indexOf(START, i)) {
            int lower  = i + START.length();
            int upper  = lower;
            int length = buffer.length();
            while (upper < length) {
                if (buffer.charAt(upper++) == END) {
                    break;
                }
            }
            final int code;
            try {
                code = Integer.parseInt(buffer.substring(lower, upper-1));
            } catch (NumberFormatException e) {
                buffer.delete(i, upper);
                continue;
            }
            final String color;
            switch (code) {
                case 31: color="red";     break;
                case 32: color="green";   break;
                case 33: color="olive";   break; // "yellow" is too bright.
                case 34: color="blue";    break;
                case 35: color="magenta"; break;
                case 36: color="teal";    break; // "cyan" is not in HTML 4, while "teal" is.
                case 37: color="gray";    break;
                case 39: // Fall through
                case 0:  color=null; break;
                default: {
                    buffer.delete(i, upper);
                    continue;
                }
            }
            if (tmp == null) {
                tmp = new StringBuilder(24);
            }
            if (fontApplied) {
                tmp.append("</font>");
                fontApplied = false;
            }
            if (color != null) {
                tmp.append("<font color=\"").append(color).append("\">");
                fontApplied = true;
            }
            buffer.replace(i, upper, tmp.toString());
            tmp.setLength(0);
        }
        final String result = buffer.toString();
        return result.equals(text) ? text : result;
    }

    /**
     * Removes all escape codes from the given string.
     *
     * @param  text The string which may contains escape codes.
     * @return Text without the escape codes, or the given {@code text} reference if
     *         it didn't contained any escape codes.
     */
    public static String plain(final String text) {
        int i = text.indexOf(START);
        if (i >= 0) {
            StringBuilder buffer = null;
            int last = 0;
search:     do {
                final int start = i;
                i += START.length();
                final int end = text.indexOf(END, i);
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
                    buffer = new StringBuilder(text.length() - last);
                }
                buffer.append(text, last, start);
                last = ++i; // The ++ is for skipping the END character.
            } while ((i = text.indexOf(START, i)) >= 0);
            if (buffer != null) {
                return buffer.append(text, last, text.length()).toString();
            }
        }
        return text;
    }

    /**
     * Returns the length of the given string without the ANSI escape codes.
     * This is equivalent to <code>{@linkplain #plain plain}(text).length()</code>
     * without the cost of creating a temporary string.
     *
     * @param  text The string which may contains escape codes.
     * @return The length of the given string without escape codes.
     */
    public static int lengthOfPlain(final String text) {
        int i = text.indexOf(START);
        if (i < 0) {
            return text.length();
        }
        int last   = 0;
        int length = 0;
search: do {
            final int start = i;
            i += START.length();
            final int end = text.indexOf(END, i);
            if (end < 0) {
                break;
            }
            while (i < end) {
                final char c = text.charAt(i++);
                if (c < '0' || c > '9') {
                    continue search;
                }
            }
            length += start - last;
            last = ++i; // The ++ is for skipping the END character.
        } while ((i = text.indexOf(START, i)) >= 0);
        length += text.length() - last;
        assert plain(text).length() == length : text;
        return length;
    }

    /**
     * Returns {@code true} if we think that the operating system supports X3.64 sequences.
     * This method performs a very naive and approximative check. Result is just a hint and
     * may be wrong.
     *
     * <p>This method does not check if a {@linkplain java.io.Console console} is attached to the
     * running JVM because the caller may want to write to the {@linkplain System#err standard
     * error stream} instead than the {@linkplain System#out standard output stream}, in which
     * case the console information is not applicable.</p>
     *
     * @return {@code true} if we think that the operating system supports X3.64.
     *         This method may conservatively returns {@code false} in case of doubt.
     */
    public static boolean isSupported() {
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
