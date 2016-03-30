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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.logging.*;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.OS;
import org.apache.sis.internal.util.X364;
import org.apache.sis.io.IO;
import org.apache.sis.io.LineAppender;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Configuration;
import org.apache.sis.util.Debug;

// Related to JDK7
import org.apache.sis.internal.jdk7.JDK7;


/**
 * A formatter writing log messages on a single line. Compared to the JDK {@link SimpleFormatter},
 * this formatter uses only one line per message instead of two. For example messages formatted by
 * {@code MonolineFormatter} may look like:
 *
 * <blockquote><table style="color:white; background:black" class="compact" summary="Logging example.">
 * <tr><td><code>00:01</code></td><td style="background:blue"><code>CONFIG</code></td>
 *     <td><code><b>[MyApplication]</b> Read configuration from “my-application/setup.xml”.</code></td></tr>
 * <tr><td><code>00:03</code></td><td style="background:green"><code>INFO</code></td>
 *     <td><code><b>[EPSGFactory]</b> Connected to the EPSG database version 6.9 on JavaDB 10.8.</code></td></tr>
 * <tr><td><code>00:12</code></td><td style="background:goldenrod"><code>WARNING</code></td>
 *     <td><code><b>[DefaultTemporalExtent]</b> This operation requires the “sis-temporal” module.</code></td></tr>
 * </table></blockquote>
 *
 * By default, {@code MonolineFormatter} shows only the level and the message. One or two additional
 * fields can be inserted between the level and the message if the {@link #setTimeFormat(String)} or
 * {@link #setSourceFormat(String)} methods are invoked with o non-null argument. Examples:
 *
 * <ul>
 *   <li>{@code setTimeFormat("HH:mm:ss")} for formatting the time like {@code 00:00:04"},
 *       as time elapsed since the {@code MonolineFormatter} creation time.</li>
 *   <li>{@code setSourceFormat("logger:long")} for formatting the full logger name
 *       (e.g. {@code "org.apache.sis.storage.netcdf"}).</li>
 *   <li>{@code setSourceFormat("class:short")} for formatting the short class name,
 *       without package (e.g. {@code "NetcdfStore"}).</li>
 * </ul>
 *
 * <div class="section">Configuration from {@code logging.properties}</div>
 * The format can also be set from the {@code jre/lib/logging.properties} file.
 * For example, user can cut and paste the following properties into {@code logging.properties}:
 *
 * {@preformat text
 *     ###########################################################################
 *     # Properties for the apache.sis.org's MonolineFormatter.
 *     # By default, the monoline formatter display only the level
 *     # and the message. Additional fields can be specified here:
 *     #
 *     #  time:   If set, writes the time elapsed since the initialization.
 *     #          The argument specifies the output pattern. For example, the
 *     #          pattern "HH:mm:ss.SSSS" displays the hours, minutes, seconds
 *     #          and milliseconds.
 *     #
 *     #  source: If set, writes the source logger name or the source class name.
 *     #          Valid argument values are "none", "logger:short", "logger:long",
 *     #          "class:short", "class:long" and "class.method".
 *     ###########################################################################
 *     org.apache.sis.util.logging.MonolineFormatter.time = HH:mm:ss.SSS
 *     org.apache.sis.util.logging.MonolineFormatter.source = class:short
 * }
 *
 * See {@link #setTimeFormat(String)} and {@link #setSourceFormat(String)} for more information about the
 * above {@code time} and {@code source} properties. Encoding and logging level are configured separately,
 * typically on the JDK {@link ConsoleHandler} like below:
 *
 * {@preformat text
 *     java.util.logging.ConsoleHandler.encoding = UTF-8
 *     java.util.logging.ConsoleHandler.level = FINE
 * }
 *
 * <div class="section">Thread safety</div>
 * The same {@code MonolineFormatter} instance can be safely used by many threads without synchronization
 * on the part of the caller. Subclasses should make sure that any overridden methods remain safe to call
 * from multiple threads.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 *
 * @see SimpleFormatter
 * @see Handler#setFormatter(Formatter)
 */
public class MonolineFormatter extends Formatter {
    /** Do not format source class name.       */ private static final int NO_SOURCE    = 0;
    /** Format the source logger without base. */ private static final int LOGGER_SHORT = 1;
    /** Format the source logger only.         */ private static final int LOGGER_LONG  = 2;
    /** Format the class name without package. */ private static final int CLASS_SHORT  = 3;
    /** Format the fully qualified class name. */ private static final int CLASS_LONG   = 4;
    /** Format the class name and method name. */ private static final int METHOD       = 5;

    /**
     * The label to use in the {@code logging.properties} for setting the source format.
     */
    private static final String[] FORMAT_LABELS = new String[6];
    static {
        FORMAT_LABELS[LOGGER_SHORT] = "logger:short";
        FORMAT_LABELS[LOGGER_LONG ] = "logger:long";
        FORMAT_LABELS[ CLASS_SHORT] = "class:short";
        FORMAT_LABELS[ CLASS_LONG ] = "class:long";
        FORMAT_LABELS[METHOD      ] = "class.method";
    }

    /**
     * Log records at level below this threshold will be printed in faint color.
     * Logs records at this level or above will be printed in normal color.
     * This threshold is set to the default level of console handlers.
     */
    private static final Level LEVEL_THRESHOLD = Level.INFO;

    /**
     * A comparator for logging level. This comparator sorts finest levels first and severe levels last.
     */
    private static final Comparator<Level> COMPARATOR = new Comparator<Level>() {
        @Override public int compare(final Level l1, final Level l2) {
            // We can't just return (i1 - i2) because some levels are
            // Integer.MIN_VALUE or Integer.MAX_VALUE, which cause overflow.
            final int i1 = l1.intValue();
            final int i2 = l2.intValue();
            if (i1 < i2) return -1;
            if (i1 > i2) return +1;
            return 0;
        }
    };

    /**
     * Whether the logging level should be visible or not.
     * We do not provide the option to hide the levels for now.
     */
    private static final boolean SHOW_LEVEL = true;

    /**
     * Minimal number of stack trace elements to print before and after the "interesting" elements.
     * The "interesting" elements are the first stack trace elements, and the element which point
     * to the method that produced the log record.
     *
     * @see #printAbridged(Throwable, Appendable, String, String, String)
     */
    private static final int CONTEXT_STACK_TRACE_ELEMENTS = 2;

    /**
     * The string to write on the left side of the first line of every log records.
     * The default value is an empty string. This field can not be null.
     *
     * @see #getHeader()
     * @see #setHeader(String)
     */
    private String header = "";

    /**
     * The colors to apply, or {@code null} if none.
     *
     * @see #colors()
     */
    private SortedMap<Level,X364> colors;

    /**
     * {@code true} if the faint X.364 code is supported.
     * On MacOS, faint colors produce bad output.
     */
    private final boolean faintSupported;

    /**
     * Numerical values of levels for which to apply colors in increasing order.
     * Computed from {@link #colors} when first needed or when the color map changes.
     */
    private transient int[] colorLevels;

    /**
     * X3.64 sequences of colors matching the levels declared in {@link #colorLevels}.
     * Computed from {@link #colors} when first needed or when the color map changes.
     */
    private transient String[] colorSequences;

    /**
     * The minimum amount of characters to use for writing logging level before the message.
     * If the logging level is shorter, remaining characters will be padded with spaces.
     * This is used in order to align the messages.
     *
     * @see #levelWidth(Level)
     */
    private final int levelWidth;

    /**
     * Time of {@code MonolineFormatter} creation, in milliseconds elapsed since January 1, 1970.
     */
    private final long startMillis;

    /**
     * The format to use for formatting elapsed time, or {@code null} if there is none.
     */
    private SimpleDateFormat timeFormat;

    /**
     * The message format, or {@code null} if not yet created.
     */
    private transient MessageFormat messageFormat;

    /**
     * Value of the last call to {@link MessageFormat#applyPattern(String)}. Saved in order to avoid
     * calling {@code applyPattern(String)} in the common case where the same message is logged many
     * times with different arguments.
     */
    private transient String messagePattern;

    /**
     * One of the following constants: {@link #NO_SOURCE}, {@link #LOGGER_SHORT},
     * {@link #LOGGER_LONG}, {@link #CLASS_SHORT}, {@link #CLASS_LONG} or {@link #METHOD}.
     */
    private int sourceFormat = NO_SOURCE;

    /**
     * Buffer for formatting messages. We will reuse this buffer in order to reduce memory allocations.
     * This is the buffer used internally by {@link #writer}.
     *
     * <p>This buffer is also arbitrarily chosen as our synchronization lock. The rational is that all
     * operations on {@code StringBuffer} are synchronized anyway. So by reusing it for our lock, we
     * will take only one monitor instead of two.</p>
     */
    private final StringBuffer buffer;

    /**
     * The line writer. This object transforms all {@code "\r"}, {@code "\n"} or {@code "\r\n"}
     * occurrences into a single line separator. This line separator will include space for the
     * left margin, if needed.
     */
    private final LineAppender writer;

    /**
     * The printer wrapping the {@link #writer}. This is used for {@link Throwable#printStackTrace(PrintWriter)} calls.
     * We don't use the printer for other usage in order to avoid unnecessary indirections and synchronizations.
     */
    private final PrintWriter printer;

    /**
     * Constructs a default {@code MonolineFormatter}.
     *
     * <div class="section">Auto-configuration from the handler</div>
     * Formatters are often associated to a particular handler. If this handler is known, giving it at
     * construction time can help this formatter to configure itself. This handler is only a hint - it
     * will not be modified, and no reference to that handler will be kept by this constructor.
     *
     * @param handler The handler to be used with this formatter, or {@code null} if unknown.
     *
     * @see Handler#setFormatter(Formatter)
     */
    public MonolineFormatter(final Handler handler) {
        this.startMillis = System.currentTimeMillis();
        /*
         * The length of the widest standard level name that may be displayed according current handler setting.
         * If a larger label is to be printed, this class will adjust itself but the visual alignment with
         * previous or next record may be broken.
         */
        levelWidth = levelWidth((handler != null) ? handler.getLevel() : null);
        /*
         * Configures this formatter according the properties, if any.
         */
        final LogManager manager = LogManager.getLogManager();
        final String classname = MonolineFormatter.class.getName();
        header = manager.getProperty(classname + ".header");
        if (header == null) {
            header = "";
        }
        try {
            timeFormat(manager.getProperty(classname + ".time"));
        } catch (IllegalArgumentException exception) {
            Logging.configurationException(Logging.getLogger(Modules.UTILITIES), MonolineFormatter.class, "<init>", exception);
        }
        try {
            sourceFormat(manager.getProperty(classname + ".source"));
        } catch (IllegalArgumentException exception) {
            Logging.configurationException(Logging.getLogger(Modules.UTILITIES), MonolineFormatter.class, "<init>", exception);
        }
        /*
         * Applies the default set of colors only if the handler is writing to the console.
         * Note that we do not check for a non-null System.console() because we are writing
         * to System.err, which may be the console even when System.console() returns null.
         * Even in the case where System.err is redirected to a file, this is typically for
         * printing in an other console (e.g. using the Unix "tail" command).
         */
        if (handler instanceof ConsoleHandler && X364.isAnsiSupported()) {
            resetLevelColors();
        }
        faintSupported = OS.current() != OS.MAC_OS;
        /*
         * Creates the buffer and the printer. We will expand the tabulations with 4 characters.
         * This apply to the stack trace formatted by Throwable.printStackTrace(PrintWriter);
         * The default (8 characters) is a little bit too wide...
         */
        final StringWriter str = new StringWriter();
        writer  = new LineAppender(str, JDK7.lineSeparator(), true);
        buffer  = str.getBuffer().append(header);
        printer = new PrintWriter(IO.asWriter(writer));
        writer.setTabulationWidth(4);
    }

    /**
     * Returns the length of the widest level name, taking in account only the standard levels
     * equals or greater then the given threshold.
     */
    static int levelWidth(final Level threshold) {
        int levelWidth = 0;
loop:   for (int i=0; ; i++) {
            final Level c;
            switch (i) {
                case 0: c = Level.SEVERE;  break;
                case 1: c = Level.WARNING; break;
                case 2: c = Level.INFO;    break;
                case 3: c = Level.CONFIG;  break;
                case 4: c = Level.FINE;    break;
                case 5: c = Level.FINER;   break;
                case 6: c = Level.FINEST;  break;
                default: break loop;
            }
            if (threshold != null && c.intValue() < threshold.intValue()) {
                break;
            }
            final int length = c.getLocalizedName().length();
            if (length > levelWidth) levelWidth = length;
        }
        return levelWidth;
    }

    /**
     * Returns the string to write on the left side of the first line of every log records, or {@code null} if none.
     * This is a string to be shown just before the level.
     *
     * @return The string to write on the left side of the first line of every log records, or {@code null} if none.
     */
    public String getHeader() {
        final String header;
        synchronized (buffer) {
            header = this.header;
        }
        // All other properties in MonolineFormatter are defined in such a way
        // that null means "none", so we do the same here for consistency.
        return header.isEmpty() ? null : header;
    }

    /**
     * Sets the string to write on the left side of the first line of every log records.
     *
     * @param header The string to write on the left side of the first line of every log records,
     *        or {@code null} if none.
     */
    public void setHeader(String header) {
        if (header == null) {                           // See comment in getHeader().
            header = "";
        }
        synchronized (buffer) {
            this.header = header;
        }
    }

    /**
     * Returns the format for elapsed time, or {@code null} if the time is not shown.
     * This method returns the pattern specified by the last call to the
     * {@link #setTimeFormat(String)} method, or the patten specified by the
     * {@code org.apache.sis.util.logging.MonolineFormatter.time} property in the
     * {@code jre/lib/logging.properties} file.
     *
     * @return The time pattern, or {@code null} if elapsed time is not formatted.
     */
    public String getTimeFormat() {
        synchronized (buffer) {
            return (timeFormat != null) ? timeFormat.toPattern() : null;
        }
    }

    /**
     * Sets the format for elapsed time, or hides the time field. The pattern must matches the
     * format specified in {@link SimpleDateFormat}, but for the time part only (no date).
     *
     * <div class="note"><b>Example:</b>
     * The {@code "HH:mm:ss.SSS"} pattern will display the elapsed time in hours, minutes, seconds
     * and milliseconds.</div>
     *
     * @param  pattern The time pattern, or {@code null} to disable time formatting.
     * @throws IllegalArgumentException If the given pattern is invalid.
     */
    public void setTimeFormat(final String pattern) throws IllegalArgumentException {
        synchronized (buffer) {
            timeFormat(pattern);
        }
    }

    /**
     * Implementation of {@link #setTimeFormat(String)}, to be invoked also by the constructor.
     */
    private void timeFormat(String pattern) throws IllegalArgumentException {
        if (pattern == null) {
            timeFormat = null;
        } else if (timeFormat == null) {
            timeFormat = new SimpleDateFormat(pattern);
            timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        } else {
            timeFormat.applyPattern(pattern);
        }
    }

    /**
     * Returns the format for the source, or {@code null} is the source is not shown.
     * This method returns the source format specified by the last call to the
     * {@link #setSourceFormat(String)} method, or the format specified by the
     * {@code org.apache.sis.util.logging.MonolineFormatter.source} property in the
     * {@code jre/lib/logging.properties} file.
     *
     * @return The source format, or {@code null} if source is not formatted.
     */
    public String getSourceFormat() {
        synchronized (buffer) {
            return FORMAT_LABELS[sourceFormat];
        }
    }

    /**
     * Sets the format for displaying the source, or hides the source field.
     * The given format can be any of the following values, from more verbose to less verbose:
     *
     * <ul>
     *   <li>{@code null} for hiding the source field.</li>
     *   <li>{@code "class:long"}   for the {@linkplain LogRecord#getSourceClassName() source class name}</li>
     *   <li>{@code "logger:long"}  for the {@linkplain LogRecord#getLoggerName() logger name}</li>
     *   <li>{@code "class:short"}  for the source class name without the package part.</li>
     *   <li>{@code "logger:short"} for the logger name without the package part.</li>
     *   <li>{@code "class.method"} for the short class name followed by the
     *       {@linkplain LogRecord#getSourceMethodName() source method name}</li>
     * </ul>
     *
     * The source class name usually contains the logger name since (by convention) logger
     * names are package names, but this is not mandatory neither enforced.
     *
     * @param  format The format for displaying the source, or {@code null} if the source shall not be formatted.
     * @throws IllegalArgumentException If the given argument is not one of the recognized format names.
     */
    public void setSourceFormat(final String format) throws IllegalArgumentException {
        synchronized (buffer) {
            sourceFormat(format);
        }
    }

    /**
     * Implementation of {@link #setSourceFormat(String)}, to be invoked also by the constructor.
     */
    private void sourceFormat(String format) throws IllegalArgumentException {
        if (format == null) {
            sourceFormat = NO_SOURCE;
            return;
        }
        format = CharSequences.trimWhitespaces(format).toLowerCase(Locale.US);
        for (int i=0; i<FORMAT_LABELS.length; i++) {
            if (format.equals(FORMAT_LABELS[i])) {
                sourceFormat = i;
                return;
            }
        }
        throw new IllegalArgumentException(format);
    }

    /**
     * Returns the color used for the given level, or {@code null} if none.
     * The current set of supported colors are {@code "red"}, {@code "green"}, {@code "yellow"}, {@code "blue"},
     * {@code "magenta"}, {@code "cyan"} and {@code "gray"}. This set may be extended in any future SIS version.
     *
     * @param  level The level for which to get the color.
     * @return The color for the given level, or {@code null} if none.
     */
    public String getLevelColor(final Level level) {
        synchronized (buffer) {
            if (colors != null) {
                final X364 code = colors.get(level);
                if (code != null) {
                    return code.color;
                }
            }
        }
        return null;
    }

    /**
     * Sets the color to use for the given level, or {@code null} for removing colorization.
     * This method should be invoked only if this formatter is associated to a {@link Handler}
     * writing to a terminal supporting <cite>ANSI escape codes</cite>
     * (a.k.a. ECMA-48, ISO/IEC 6429 and X3.64 standards).
     *
     * <p>The given {@code color} argument shall be one of the values documented in the
     * {@link #getLevelColor(Level)} method.</p>
     *
     * @param  level The level for which to set a new color.
     * @param  color The case-insensitive new color, or {@code null} if none.
     * @throws IllegalArgumentException If the given color is not one of the recognized values.
     */
    public void setLevelColor(final Level level, final String color) throws IllegalArgumentException {
        boolean changed = false;
        synchronized (buffer) {
            if (color != null) {
                final X364 code = X364.forColorName(color).background();
                changed = (colors().put(level, code) != code);
            } else if (colors != null) {
                changed = (colors.remove(level) != null);
                if (colors.isEmpty()) {
                    colors = null;
                }
            }
            if (changed) {
                colorLevels = null;
                colorSequences = null;
            }
        }
    }

    /**
     * Returns the {@link #colors} map, creating it if needed.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private SortedMap<Level,X364> colors() {
        if (colors == null) {
            colors = new TreeMap<Level,X364>(COMPARATOR);
        }
        return colors;
    }

    /**
     * Resets the colors to the default values. This method does not check if <cite>ANSI escape codes</cite>
     * are supported or not - this check must be done by the caller.
     */
    private void resetLevelColors() {
        final SortedMap<Level,X364> colors = colors();
        colors.clear();
        colors.put(Level.ALL,     X364.BACKGROUND_GRAY);
        colors.put(Level.CONFIG,  X364.BACKGROUND_BLUE);
        colors.put(Level.INFO,    X364.BACKGROUND_GREEN);
        colors.put(Level.WARNING, X364.BACKGROUND_YELLOW);
        colors.put(Level.SEVERE,  X364.BACKGROUND_RED);
        colors.put(PerformanceLevel.PERFORMANCE, X364.BACKGROUND_CYAN);
    }

    /**
     * Resets the colors setting to its default value.
     *
     * <ul>
     *   <li>If {@code enabled} is {@code true}, then this method defines a default set of colors.</li>
     *   <li>If {@code enabled} is {@code false}, then this method resets the formatting to plain text.</li>
     * </ul>
     *
     * This method does not check if <cite>ANSI escape codes</cite> are supported or not.
     * This check must be done by the caller.
     *
     * @param enabled {@code true} for defining a default set of colors, or {@code false} for removing all colors.
     */
    public void resetLevelColors(final boolean enabled) {
        synchronized (buffer) {
            if (enabled) {
                resetLevelColors();
            } else {
                colors = null;
                colorLevels = null;
                colorSequences = null;
            }
        }
    }

    /**
     * Gets the color for the given level. If there is no explicit color for the given level,
     * returns the color of the first level below the given one for which a color is specified.
     */
    private String colorAt(final Level level) {
        if (colorSequences == null) {
            colorSequences = new String[colors.size()];
            colorLevels = new int[colorSequences.length];
            int i = 0;
            for (final SortedMap.Entry<Level,X364> entry : colors.entrySet()) {
                colorSequences[i] = entry.getValue().background().sequence();
                colorLevels[i++]  = entry.getKey().intValue();
            }
        }
        int i = Arrays.binarySearch(colorLevels, level.intValue());
        if (i < 0) {
            i = Math.max((~i)-1, 0);                    // Really tild, not minus sign.
        }
        return colorSequences[i];
    }

    /**
     * Formats the given log record and return the formatted string.
     * See the <a href="#overview">class javadoc</a> for information on the log format.
     *
     * @param  record The log record to be formatted.
     * @return A formatted log record.
     */
    @Override
    public String format(final LogRecord record) {
        final Level level = record.getLevel();
        final StringBuffer buffer = this.buffer;
        synchronized (buffer) {
            final boolean colors  = (this.colors != null);
            final boolean emphase = !faintSupported || (level.intValue() >= LEVEL_THRESHOLD.intValue());
            buffer.setLength(header.length());
            /*
             * Appends the time (e.g. "00:00:12.365"). The time pattern can be set either
             * programmatically by a call to 'setTimeFormat(…)', or in logging.properties
             * file with the "org.apache.sis.util.logging.MonolineFormatter.time" property.
             */
            if (timeFormat != null) {
                Date time = new Date(Math.max(0, record.getMillis() - startMillis));
                timeFormat.format(time, buffer, new FieldPosition(0));
                buffer.append(' ');
            }
            /*
             * Appends the level (e.g. "FINE"). We do not provide the option to turn level off for now.
             * This level will be formatted with a colorized background if ANSI escape sequences are enabled.
             */
            int margin = buffer.length();
            String levelColor = "", levelReset = "";
            if (SHOW_LEVEL) {
                if (colors) {
                    levelColor = colorAt(level);
                    levelReset = X364.BACKGROUND_DEFAULT.sequence();
                }
                final int offset = buffer.append(levelColor).length();
                buffer.append(level.getLocalizedName())
                      .append(CharSequences.spaces(levelWidth - (buffer.length() - offset)));
                margin += buffer.length() - offset;
                buffer.append(levelReset).append(' ');
            }
            /*
             * Appends the logger name or source class name, in long of short form.
             * The name may be formatted in bold characters if ANSI escape sequences are enabled.
             */
            String source;
            switch (sourceFormat) {
                case LOGGER_SHORT: // Fall through
                case LOGGER_LONG:  source = record.getLoggerName(); break;
                case METHOD:       // Fall through
                case CLASS_SHORT:  // Fall through
                case CLASS_LONG:   source = record.getSourceClassName(); break;
                default:           source = null; break;
            }
            if (source != null) {
                switch (sourceFormat) {
                    case METHOD:       // Fall through
                    case LOGGER_SHORT: // Fall through
                    case CLASS_SHORT: {
                        // Works even if there is no '.' since we get -1 as index.
                        source = source.substring(source.lastIndexOf('.') + 1);
                        break;
                    }
                }
                if (sourceFormat == METHOD) {
                    source = source + '.' + record.getSourceMethodName();
                }
                if (colors && emphase) {
                    buffer.append(X364.BOLD.sequence());
                }
                buffer.append('[').append(source).append(']');
                if (colors && emphase) {
                    buffer.append(X364.NORMAL.sequence());
                }
                buffer.append(' ');
            }
            /*
             * Now prepare the LineAppender for the message. We set a line separator prefixed by some
             * amount of spaces in order to align message body on the column after the level name.
             */
            String bodyLineSeparator = writer.getLineSeparator();
            final String lineSeparator = JDK7.lineSeparator();
            if (bodyLineSeparator.length() != lineSeparator.length() + margin + 1) {
                bodyLineSeparator = lineSeparator + levelColor + CharSequences.spaces(margin) + levelReset + ' ';
                writer.setLineSeparator(bodyLineSeparator);
            }
            if (colors && !emphase) {
                buffer.append(X364.FAINT.sequence());
            }
            final Throwable exception = record.getThrown();
            String message = formatMessage(record);
            int length = 0;
            if (message != null) {
                length = CharSequences.skipTrailingWhitespaces(message, 0, message.length());
            }
            /*
             * Up to this point, we wrote directly in the StringBuilder for performance reasons.
             * Now for the message part, we need to use the LineAppender in order to replace EOL
             * and tabulations.
             */
            try {
                if (message != null) {
                    writer.append(message, 0, length);
                }
                if (exception != null) {
                    if (message != null) {
                        writer.append("\nCaused by: ");     // LineAppender will replace '\n' by the system EOL.
                    }
                    if (level.intValue() >= LEVEL_THRESHOLD.intValue()) {
                        exception.printStackTrace(printer);
                    } else {
                        printAbridged(exception, writer, record.getLoggerName(),
                                record.getSourceClassName(), record.getSourceMethodName());
                    }
                }
                writer.flush();
            } catch (IOException e) {
                throw new AssertionError(e);
            }
            buffer.setLength(CharSequences.skipTrailingWhitespaces(buffer, 0, buffer.length()));
            if (colors && !emphase) {
                buffer.append(X364.NORMAL.sequence());
            }
            buffer.append(lineSeparator);
            return buffer.toString();
        }
    }

    /**
     * Returns the localized message from the given log record.
     * First this method gets the {@linkplain LogRecord#getMessage() raw message} from the given record.
     * Then there is choices:
     *
     * <ul>
     *   <li>If the given record specifies a {@linkplain LogRecord#getResourceBundle() resource bundle},
     *       then the message is used as a key for fetching the localized resources in the given bundle.</li>
     *   <li>If the given record specifies one or more {@linkplain LogRecord#getParameters() parameters}
     *       and if the message seems to use the {@link MessageFormat} syntax, then the message is formatted
     *       by {@code MessageFormat}.</li>
     * </ul>
     *
     * @param  record The log record from which to get a localized message.
     * @return The localized message.
     */
    @Override
    public String formatMessage(final LogRecord record) {
        /*
         * Same work than java.util.logging.Formatter.formatMessage(LogRecord) except for the synchronization lock,
         * the reuse of existing MessageFormat and StringBuffer instances, and not catching formatting exceptions
         * (we want to know if our messages have a problem).
         */
        String message = record.getMessage();
        ResourceBundle resources = record.getResourceBundle();
        if (resources != null) {
            message = resources.getString(message);
        }
        final Object parameters[] = record.getParameters();
        if (parameters != null && parameters.length != 0) {
            int i = message.indexOf('{');
            if (i >= 0 && ++i < message.length()) {
                final char c = message.charAt(i);
                if (c >= '0' && c <= '9') {
                    synchronized (buffer) {
                        if (messageFormat == null) {
                            messageFormat = new MessageFormat(message);
                        } else if (!message.equals(messagePattern)) {
                            messageFormat.applyPattern(message);
                        }
                        messagePattern = message;
                        final int base = buffer.length();
                        try {
                            message = messageFormat.format(parameters, buffer, new FieldPosition(0)).substring(base);
                        } finally {
                            buffer.setLength(base);
                        }
                    }
                }
            }
        }
        return message;
    }

    /**
     * Prints an abridged stack trace. This method is invoked when the record is logged at
     * at low logging level (typically less than {@link Level#INFO}).
     *
     * @param exception         The exception to be logged.
     * @param writer            Where to print the stack trace.
     * @param loggerName        The name of the logger when the log will be sent.
     * @param sourceClassName   The name of the class that emitted the log.
     * @param sourceMethodName  The name of the method that emitted the log.
     */
    private static void printAbridged(Throwable exception, final Appendable writer,
            final String loggerName, final String sourceClassName, final String sourceMethodName) throws IOException
    {
        StackTraceElement previous = null;
        // Arbitrary limit of 10 causes to format.
        for (int numCauses=0; numCauses<10; numCauses++) {
            final StackTraceElement[] trace = exception.getStackTrace();
            /*
             * Find the index of the stack trace element where the log has been produced.
             * If no exact match is found, some heuristic is applied (the first element
             * from the same class, or the first element from the logger package). If no
             * approximative match is found, then the default value is the last element.
             */
            int logProducer = trace.length;
            boolean useLoggerName = (loggerName != null), useClassName = true;
            for (int i=0; i<trace.length; i++) {
                final StackTraceElement element = trace[i];
                final String classname = element.getClassName();
                if (classname != null) {
                    if (useLoggerName && classname.startsWith(loggerName)) {
                        logProducer = i;
                        useLoggerName = false;
                    }
                    if (classname.contains(sourceClassName)) {
                        final String m = element.getMethodName();
                        if (m != null && m.equals(sourceMethodName)) {
                            logProducer = i;
                            break;
                        }
                        if (useClassName) {
                            logProducer = i;
                            useClassName = false;
                        }
                        useLoggerName = false;
                    }
                }
            }
            /*
             * If the stack trace element pointed by 'logProducer' is the same one than
             * during the previous iteration, it is not worth to print those elements again.
             */
            int stopIndex = trace.length;
            if (logProducer < stopIndex) {
                final StackTraceElement element = trace[logProducer];
                if (element.equals(previous)) {
                    stopIndex = CONTEXT_STACK_TRACE_ELEMENTS;
                }
                previous = element;
            }
            stopIndex = Math.min(logProducer + (CONTEXT_STACK_TRACE_ELEMENTS + 1), stopIndex);
            /*
             * Now format the exception, then redo the loop for the cause (if any).
             */
            writer.append(String.valueOf(exception)).append('\n'); // LineAppender will replace '\n' by the system EOL.
            for (int i=0; i<stopIndex; i++) {
                if (i == CONTEXT_STACK_TRACE_ELEMENTS) {
                    final int numToSkip = (logProducer - 2*CONTEXT_STACK_TRACE_ELEMENTS);
                    if (numToSkip > 1) {
                        more(writer, numToSkip, true);
                        i += numToSkip;
                    }
                }
                if (i == logProducer) {
                    writer.append("  →");
                }
                writer.append("\tat ").append(String.valueOf(trace[i])).append('\n');
            }
            more(writer, trace.length - stopIndex, false);
            exception = exception.getCause();
            if (exception == null) break;
            writer.append("Caused by: ");
        }
    }

    /**
     * Formats the number of stack trace elements that where skipped.
     */
    private static void more(final Appendable writer, final int numToSkip, final boolean con) throws IOException {
        if (numToSkip > 0) {
            writer.append("... ").append(String.valueOf(numToSkip)).append(" more");
            if (con) {
                writer.append(" ...");
            }
            writer.append('\n');                    // LineAppender will replace '\n' by the system EOL.
        }
    }

    /**
     * Installs a {@code MonolineFormatter} for the root logger, or returns the existing instance if any.
     * This method performs the following choices:
     *
     * <ul>
     *   <li>If a {@link ConsoleHandler} is associated to the root logger, then:
     *     <ul>
     *       <li>If that handler already uses a {@code MonolineFormatter}, then the existing formatter is returned.</li>
     *       <li>Otherwise the {@code ConsoleHandler} formatter is replaced by a new {@code MonolineFormatter} instance,
     *           and that new instance is returned. We perform this replacement in order to avoid sending twice the same
     *           records to the console.</li>
     *     </ul></li>
     *   <li>Otherwise a new {@code ConsoleHandler} using a new {@code MonolineFormatter} is created and added to the
     *       root logger.</li>
     * </ul>
     *
     * <div class="note"><b>Implementation note:</b>
     * The current implementation does not check for duplicated {@code ConsoleHandler} instances,
     * and does not check if any child logger has a {@code ConsoleHandler}.</div>
     *
     * @return The new or existing {@code MonolineFormatter}. The formatter output can be configured
     *         using the {@link #setTimeFormat(String)} and {@link #setSourceFormat(String)} methods.
     * @throws SecurityException If this method does not have the permission to install the formatter.
     */
    @Configuration
    public static MonolineFormatter install()  throws SecurityException {
        return install(Logging.getLogger(""), null);
    }

    /**
     * Installs a {@code MonolineFormatter} for the specified logger, or returns the existing instance if any.
     * This method performs the following steps:
     *
     * <ul>
     *   <li>If a {@link ConsoleHandler} is associated to the given logger, then:
     *     <ul>
     *       <li>If that handler already uses a {@code MonolineFormatter}, then the existing formatter is returned.</li>
     *       <li>Otherwise the {@code ConsoleHandler} formatter is replaced by a new {@code MonolineFormatter} instance,
     *           and that new instance is returned. We perform this replacement in order to avoid sending twice the same
     *           records to the console.</li>
     *     </ul></li>
     *   <li>Otherwise:
     *     <ul>
     *       <li>The {@link Logger#setUseParentHandlers(boolean)} flag is set to {@code false} for avoiding duplicated
     *           loggings if a {@code ConsoleHandler} instance exists in the parent handlers.</li>
     *       <li>Parent handlers that are not {@code ConsoleHandler} instances are added to the given logger in
     *           order to preserve similar behavior as before the call to {@code setUseParentHandlers(false)}.</li>
     *       <li>A new {@code ConsoleHandler} using a new {@code MonolineFormatter} is created and added to the
     *           given logger.</li>
     *     </ul></li>
     * </ul>
     *
     * <div class="note"><b>Implementation note:</b>
     * The current implementation does not check for duplicated {@code ConsoleHandler} instances,
     * and does not check if any child logger has a {@code ConsoleHandler}.</div>
     *
     * <div class="section">Specifying a log level</div>
     * This method can opportunistically set the handler level. If the given level is non-null,
     * then the {@link ConsoleHandler} using the {@code MonolineFormatter} will be set to that level.
     * This is mostly a convenience for temporary increase of logging verbosity for debugging purpose.
     * This functionality should not be used in production environment, since it overwrite user's level setting.
     *
     * @param  logger The base logger to apply the change on.
     * @param  level The desired level, or {@code null} if no level should be set.
     * @return The new or existing {@code MonolineFormatter}. The formatter output can be configured
     *         using the {@link #setTimeFormat(String)} and {@link #setSourceFormat(String)} methods.
     * @throws SecurityException If this method does not have the permission to install the formatter.
     */
    @Debug
    @Configuration
    public static MonolineFormatter install(final Logger logger, final Level level) throws SecurityException {
        ArgumentChecks.ensureNonNull("logger", logger);
        MonolineFormatter monoline = null;
        for (final Handler handler : logger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                /*
                 * Get or replace the formatter of the first ConsoleHandler found, then stop the search.
                 * We do not search for duplicated ConsoleHandler instances. If such duplicated values exist,
                 * we presume that the user know what he is doing and will avoid messing more with his configuration.
                 */
                final Formatter formatter = handler.getFormatter();
                if (formatter instanceof MonolineFormatter) {
                    monoline = (MonolineFormatter) formatter;
                } else {
                    monoline = new MonolineFormatter(handler);
                    handler.setFormatter(monoline);
                }
                if (level != null) {
                    handler.setLevel(level);
                }
                break;
            }
        }
        /*
         * If we didn't found any ConsoleHandler, then we will need to create a new one. This usually happen if
         * the logger given in argument to this method was not the root logger. For example the user may want to
         * configure only the "org.apache.sis" logger. But before to create the new ConsoleHandler, we will need
         * to stop using the parent handlers because we don't want to inherit the original ConsoleHandler which
         * is likely to exist in the root package. In order to preserve functionalities of other loggers, we copy
         * a snapshot of all other handlers.
         */
        if (monoline == null) {
            logger.setUseParentHandlers(false);
            for (Logger parent=logger; parent.getUseParentHandlers();) {
                parent = parent.getParent();
                if (parent == null) {
                    break;
                }
                for (final Handler handler : parent.getHandlers()) {
                    if (!(handler instanceof ConsoleHandler)) {
                        logger.addHandler(handler);
                    }
                }
            }
            final Handler handler = new ConsoleHandler();
            if (level != null) {
                handler.setLevel(level);                    // Shall be before MonolineFormatter creation.
            }
            monoline = new MonolineFormatter(handler);
            handler.setFormatter(monoline);
            logger.addHandler(handler);
        }
        return monoline;
    }
}
