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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.Comparator;
import java.util.logging.*;
import net.jcip.annotations.ThreadSafe;
import org.apache.sis.internal.system.OS;
import org.apache.sis.internal.util.X364;
import org.apache.sis.io.IO;
import org.apache.sis.util.CharSequences;
import org.apache.sis.io.LineAppender;

// Related to JDK7
import java.util.Objects;


/**
 * A formatter writing log messages on a single line. Compared to {@link SimpleFormatter}, this
 * formatter uses only one line per message instead of two. For example a message formatted by
 * {@code MonolineFormatter} looks like:
 *
 * {@preformat text
 *     FINE   A log message logged with level FINE from the "org.apache.sis.util" logger.
 * }
 *
 * By default, {@code MonolineFormatter} displays only the level and the message. Additional
 * fields can be formatted if {@link #setTimeFormat(String)} or {@link #setSourceFormat(String)}
 * methods are invoked with a non-null argument. The format can also be set from the
 * {@code jre/lib/logging.properties} file. For example, user can cut and paste the following
 * properties into {@code logging.properties}:
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
 *     #          "class:short" and "class:long".
 *     ###########################################################################
 *     org.apache.sis.util.logging.MonolineFormatter.time = HH:mm:ss.SSS
 *     org.apache.sis.util.logging.MonolineFormatter.source = class:short
 * }
 *
 * The example below sets the {@code MonolineFormatter} for the whole system with level {@code FINE}
 * and {@code "Cp850"} page encoding (which is appropriate for some DOS console on old Windows).
 *
 * {@preformat text
 *     java.util.logging.ConsoleHandler.formatter = org.apache.sis.util.logging.MonolineFormatter
 *     java.util.logging.ConsoleHandler.encoding = Cp850
 *     java.util.logging.ConsoleHandler.level = FINE
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.0)
 * @version 0.3
 * @module
 */
@ThreadSafe
public class MonolineFormatter extends Formatter {
    /**
     * The string to write before any log message.
     */
    private static final String MARGIN = "";

    /** Do not format source class name.       */ private static final int NO_SOURCE    = 0;
    /** Explicit value for 'none'.             */ private static final int NO_SOURCE_EX = 1;
    /** Format the source logger without base. */ private static final int LOGGER_SHORT = 2;
    /** Format the source logger only.         */ private static final int LOGGER_LONG  = 3;
    /** Format the class name without package. */ private static final int CLASS_SHORT  = 4;
    /** Format the fully qualified class name. */ private static final int CLASS_LONG   = 5;

    /**
     * The label to use in the {@code logging.properties} for setting the source format.
     */
    private static final String[] FORMAT_LABELS = new String[6];
    static {
        FORMAT_LABELS[NO_SOURCE_EX] = "none";
        FORMAT_LABELS[LOGGER_SHORT] = "logger:short";
        FORMAT_LABELS[LOGGER_LONG ] = "logger:long";
        FORMAT_LABELS[ CLASS_SHORT] = "class:short";
        FORMAT_LABELS[ CLASS_LONG ] = "class:long";
    }

    /**
     * Logs at level below this threshold will be printed in faint color.
     * Logs at this level or above will be printed in normal color. This
     * threshold is set to the default level of console handlers.
     */
    private static final Level LEVEL_THRESHOLD = Level.INFO;

    /**
     * A comparator for logging level. This comparator sorts finest levels first
     * and severe levels last.
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
     * Minimal number of stack trace elements to print before and after the "interesting".
     * elements. The "interesting" elements are the first stack trace elements, and the
     * element which point to the method that produced the log record.
     *
     * @see #printAbridged(Throwable, PrintWriter, String, String, String)
     */
    private static final int CONTEXT_STACK_TRACE_ELEMENTS = 2;

    /**
     * The colors to apply, or {@code null} if none.
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
     */
    private int levelWidth;

    /**
     * Time of {@code MonolineFormatter} creation, in milliseconds elapsed since January 1, 1970.
     */
    private final long startMillis;

    /**
     * The format to use for formatting elapsed time, or {@code null} if there is none.
     */
    private SimpleDateFormat timeFormat = null;

    /**
     * One of the following constants: {@link #NO_SOURCE}, {@link #LOGGER_SHORT},
     * {@link #LOGGER_LONG}, {@link #CLASS_SHORT} or {@link #CLASS_LONG}.
     */
    private int sourceFormat = NO_SOURCE;

    /**
     * Buffer for formatting messages. We will reuse this buffer in order to reduce memory
     * allocations. This is the buffer used internally by {@link #writer}.
     */
    private final StringBuffer buffer;

    /**
     * The line writer. This object transforms all {@code "\r"}, {@code "\n"} or {@code "\r\n"}
     * occurrences into a single line separator. This line separator will include space for the
     * margin, if needed.
     */
    private final LineAppender writer;

    /**
     * Constructs a default {@code MonolineFormatter}.
     *
     * {@section Auto-configuration from the handler}
     * Formatters are often associated to a particular handler. If this handler is known, giving it at
     * construction time can help this formatter to configure itself. This handler is only a hint - no
     * reference to this handler will be kept.
     *
     * @param handler The handler to be used with this formatter, or {@code null} if unknown.
     */
    public MonolineFormatter(final Handler handler) {
        this.startMillis = System.currentTimeMillis();
        /*
         * Sets the "levelWidth" field to the largest label that may be displayed,
         * according current handler setting. In the case where a larger label is
         * to be printed, this class will adjust itself but the visual alignment
         * with previous or next record may be broken.
         */
        final Level level = (handler != null) ? handler.getLevel() : null;
loop:   for (int i=0; ; i++) {
            final Level c;
            switch (i) {
                case 0: c = Level.FINEST;  break;
                case 1: c = Level.FINER;   break;
                case 2: c = Level.FINE;    break;
                case 3: c = Level.CONFIG;  break;
                case 4: c = Level.INFO;    break;
                case 5: c = Level.WARNING; break;
                case 6: c = Level.SEVERE;  break;
                default: break loop;
            }
            if (level == null || c.intValue() >= level.intValue()) {
                final int length = c.getLocalizedName().length();
                if (length > levelWidth) levelWidth = length;
            }
        }
        /*
         * Creates the buffer and the printer. We will expand the tabulations with 4 characters.
         * This apply to the stack trace formatted by Throwable.printStackTrace(PrintWriter);
         * The default (8 characters) is a bit wide...
         */
        final StringWriter str = new StringWriter();
        writer = new LineAppender(str, System.lineSeparator(), true);
        buffer = str.getBuffer().append(MARGIN);
        writer.setTabulationWidth(4);
        /*
         * Configures this formatter according the properties, if any.
         */
        final LogManager manager = LogManager.getLogManager();
        final String classname = MonolineFormatter.class.getName();
        try {
            setTimeFormat(manager.getProperty(classname + ".time"));
        } catch (IllegalArgumentException exception) {
            // Can't use the logging framework, since we are configuring it.
            // Display the exception name only, not the trace.
            System.err.println(exception);
        }
        try {
            setSourceFormat(manager.getProperty(classname + ".source"));
        } catch (IllegalArgumentException exception) {
            System.err.println(exception);
        }
        /*
         * Applies the default set of colors only if the handler is writing to the console.
         * Note that we do not check for a non-null System.console() because we are writing
         * to System.err, which may be the console even when System.console() returns null.
         * Even in the case where System.err is redirected to a file, this is typically for
         * printing in an other console (e.g. using the Unix "tail" command).
         */
        if (handler instanceof ConsoleHandler && X364.isAnsiSupported()) {
            colors = new TreeMap<>(COMPARATOR);
            final SortedMap<Level,X364> colors = this.colors;
            colors.put(Level.ALL,     X364.BACKGROUND_GRAY);
            colors.put(Level.CONFIG,  X364.BACKGROUND_BLUE);
            colors.put(Level.INFO,    X364.BACKGROUND_GREEN);
            colors.put(Level.WARNING, X364.BACKGROUND_YELLOW);
            colors.put(Level.SEVERE,  X364.BACKGROUND_RED);
            colors.put(PerformanceLevel.PERFORMANCE, X364.BACKGROUND_CYAN);
        }
        faintSupported = OS.current() != OS.MAC_OS;
    }

    /**
     * Returns the format for displaying elapsed time. This is the pattern specified
     * to the last call to {@link #setTimeFormat}, or the patten specified in the
     * {@code org.apache.sis.util.logging.MonolineFormatter.time} property in the
     * {@code jre/lib/logging.properties} file.
     *
     * @return The time pattern, or {@code null} if time is not formatted.
     */
    public synchronized String getTimeFormat() {
        return (timeFormat != null) ? timeFormat.toPattern() : null;
    }

    /**
     * Sets the format for displaying elapsed time. The pattern must matches the format specified
     * in {@link SimpleDateFormat}, but for the time part only (not the date). For example, the
     * pattern {@code "HH:mm:ss.SSS"} will display the elapsed time in hours, minutes, seconds
     * and milliseconds.
     *
     * @param pattern The time patter, or {@code null} to disable time formatting.
     */
    public synchronized void setTimeFormat(final String pattern) {
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
     * Returns the format for displaying the source. This is the pattern specified
     * to the last call to {@link #setSourceFormat}, or the patten specified in the
     * {@code org.apache.sis.util.logging.MonolineFormatter.source} property in the
     * {@code jre/lib/logging.properties} file.
     *
     * @return The source pattern, or {@code null} if source is not formatted.
     */
    public synchronized String getSourceFormat() {
        return FORMAT_LABELS[sourceFormat];
    }

    /**
     * Sets the format for displaying the source. The pattern can be {@code null}, {@code "none"},
     * {@code "logger:short"}, {@code "class:short"}, {@code "logger:long"} or {@code "class:long"}.
     * The 4 last choices are made of two parts separated by a {@code ':'} character:
     *
     * <ol>
     *   <li>{@code "logger"} for the {@linkplain LogRecord#getLoggerName logger name}, or
     *       {@code "class"} for the {@linkplain LogRecord#getSourceClassName source class name}.
     *       The source class name usually contains the logger name since (by convention) logger
     *       names are package names, but this is not mandatory neither enforced.</li>
     *
     *   <li>{@code "long"} for the full logger or class name, or {@code "short"} for only
     *       the part following the last dot character.</li>
     * </ol>
     *
     * The difference between a {@code null} and {@code "none"} is that {@code null}
     * may be replaced by a default value, while {@code "none"} means that the caller
     * explicitly requested no source.
     *
     * @param format The format for displaying the source.
     */
    public synchronized void setSourceFormat(String format) {
        if (format != null) {
            format = format.trim().toLowerCase();
        }
        for (int i=0; i<FORMAT_LABELS.length; i++) {
            if (Objects.equals(FORMAT_LABELS[i], format)) {
                sourceFormat = i;
                return;
            }
        }
        throw new IllegalArgumentException(format);
    }

    /**
     * Returns the color used for the given level. By default there is no color for any level.
     * Colors should be used only if this formatter is associated to a {@link Handler} writing
     * to an ANSI X3.64 compatible terminal.
     *
     * @param  level The level for which to get the color.
     * @return The color for the given level, or {@code null} if none.
     *
     * @todo Not yet public because X364 is not a public enumeration.
     */
    final synchronized X364 getLevelColor(final Level level) {
        return (colors != null) ? colors.get(level) : null;
    }

    /**
     * Sets the color to use for the given level. This method should be invoked only if this
     * formatter is associated to a {@link Handler} writing to an ANSI X3.64 compatible terminal.
     *
     * @param level The level for which to set a new color.
     * @param color The new color, or {@code null} if none.
     *
     * @todo Not yet public because X364 is not a public enumeration.
     */
    final synchronized void setLevelColor(final Level level, final X364 color) {
        boolean changed = false;
        if (color != null) {
            if (colors == null) {
                colors = new TreeMap<>(COMPARATOR);
            }
            changed = (colors.put(level, color) != color);
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

    /**
     * Clears all colors setting. If this formatter was inserting X3.64 escape sequences
     * for colored output, invoking this method will force the formatting of plain text.
     *
     * @todo Not yet public because X364 is not a public enumeration.
     */
    final synchronized void clearLevelColors() {
        colors = null;
        colorLevels = null;
        colorSequences = null;
    }

    /**
     * Interpolates the color for the given level. If there is no color specified explicitly for the given color,
     * returns the color of the first level below the given one for which a color is specified.
     */
    private String interpolateColor(final Level level) {
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
            i = Math.max((~i)-1, 0);  // Really tild, not minus sign.
        }
        return colorSequences[i];
    }

    /**
     * Formats the given log record and return the formatted string.
     *
     * @param  record The log record to be formatted.
     * @return A formatted log record.
     */
    @Override
    public synchronized String format(final LogRecord record) {
        final Level level = record.getLevel();
        final boolean colors  = (this.colors != null);
        final boolean emphase = !faintSupported || (level.intValue() >= LEVEL_THRESHOLD.intValue());
        final StringBuffer buffer = this.buffer;
        buffer.setLength(MARGIN.length());
        /*
         * Formats the time (e.g. "00:00:12.365"). The time pattern can be set either
         * programmatically by a call to setTimeFormat(...), or in logging.properties
         * file with the "org.apache.sis.util.logging.MonolineFormatter.time" property.
         */
        if (timeFormat != null) {
            Date time = new Date(Math.max(0, record.getMillis() - startMillis));
            timeFormat.format(time, buffer, new FieldPosition(0));
            buffer.append(' ');
        }
        /*
         * Formats the level (e.g. "FINE"). We do not provide
         * the option to turn level off for now.
         */
        int margin = buffer.length();
        if (true) {
            if (colors) {
                buffer.append(interpolateColor(level));
            }
            final int offset = buffer.length();
            buffer.append(level.getLocalizedName());
            buffer.append(CharSequences.spaces(levelWidth - (buffer.length() - offset)));
            margin += buffer.length() - offset;
            if (colors) {
                buffer.append(X364.BACKGROUND_DEFAULT.sequence());
            }
            buffer.append(' ');
            margin++;
        }
        /*
         * Adds the source. It may be either the source logger or the source class name.
         */
        String source;
        switch (sourceFormat) {
            case LOGGER_SHORT: // Fall through
            case LOGGER_LONG:  source = record.getLoggerName(); break;
            case CLASS_SHORT:  // Fall through
            case CLASS_LONG:   source = record.getSourceClassName(); break;
            default:           source = null; break;
        }
        if (source != null) {
            switch (sourceFormat) {
                case LOGGER_SHORT: // Fall through
                case CLASS_SHORT: {
                    // Works even if there is no '.' since we get -1 as index.
                    source = source.substring(source.lastIndexOf('.') + 1);
                    break;
                }
            }
            source = source.replace('$', '.');
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
         * Now format the message. We will use a line separator made of the
         * usual EOL ("\r", "\n", or "\r\n", which is plateform specific)
         * following by some amout of space in order to align message body.
         */
        String bodyLineSeparator = writer.getLineSeparator();
        final String lineSeparator = System.lineSeparator();
        if (bodyLineSeparator.length() != lineSeparator.length() + margin) {
            bodyLineSeparator = lineSeparator + CharSequences.spaces(margin);
            writer.setLineSeparator(bodyLineSeparator);
        }
        if (colors && !emphase) {
            buffer.append(X364.FAINT.sequence());
        }
        final Throwable exception = record.getThrown();
        String message = formatMessage(record);
        if (message != null) {
            message = message.substring(0, trim(message));
        }
        try {
            if (exception != null) {
                // If there is no message, print directly the exception.
                if (message != null) {
                    writer.append(message).append(lineSeparator);
                    writer.append("Caused by: ");
                }
                if (level.intValue() >= LEVEL_THRESHOLD.intValue()) {
                    exception.printStackTrace(new PrintWriter(IO.asWriter(writer)));
                } else {
                    printAbridged(exception, writer, record.getLoggerName(),
                            record.getSourceClassName(), record.getSourceMethodName(), lineSeparator);
                }
            } else {
                // If there is no message, print "null".
                writer.append(message);
            }
            writer.flush();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        buffer.setLength(trim(buffer));
        if (colors && !emphase) {
            buffer.append(X364.NORMAL.sequence());
        }
        buffer.append(lineSeparator);
        return buffer.toString();
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
            final String loggerName, final String sourceClassName, final String sourceMethodName,
            final String lineSeparator) throws IOException
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
            writer.append(String.valueOf(exception)).append(lineSeparator);
            for (int i=0; i<stopIndex; i++) {
                if (i == CONTEXT_STACK_TRACE_ELEMENTS) {
                    final int numToSkip = (logProducer - 2*CONTEXT_STACK_TRACE_ELEMENTS);
                    if (numToSkip > 1) {
                        more(writer, numToSkip, true, lineSeparator);
                        i += numToSkip;
                    }
                }
                if (i == logProducer) {
                    writer.append("  \u2192"); // Right arrow
                }
                writer.append("\tat ").append(String.valueOf(trace[i])).append(lineSeparator);
            }
            more(writer, trace.length - stopIndex, false, lineSeparator);
            exception = exception.getCause();
            if (exception == null) break;
            writer.append("Caused by: ");
        }
    }

    /**
     * Formats the number of stack trace elements that where skipped.
     */
    private static void more(final Appendable writer, final int numToSkip, final boolean con,
            final String lineSeparator) throws IOException
    {
        if (numToSkip > 0) {
            writer.append("... ").append(String.valueOf(numToSkip)).append(" more");
            if (con) {
                writer.append(" ...");
            }
            writer.append(lineSeparator);
        }
    }

    /**
     * Returns the length of the given characters sequences without the trailing spaces
     * or line feed.
     */
    private static int trim(final CharSequence message) {
        int length = message.length();
        while (length != 0 && Character.isWhitespace(message.charAt(length-1))) {
            length--;
        }
        return length;
    }

    /**
     * Setups a {@code MonolineFormatter} for the specified logger and its children. This method
     * searches for all instances of {@link ConsoleHandler} using the {@link SimpleFormatter}. If
     * such instances are found, they are replaced by a single instance of {@code MonolineFormatter}.
     * If no such {@link ConsoleHandler} are found, then a new one is created with a new
     * {@code MonolineFormatter}.
     *
     * <p>In addition, this method can set the handler levels. If the level is non-null, then every
     * {@link Handler}s using the monoline formatter may be set to the specified level. Whatever
     * the given level is used or not depends on current configuration. The choice is based on
     * heuristic rules that may change in any future version. Developers are encouraged to avoid
     * non-null level except for debugging purpose, since a user trying to configure his logging
     * properties file may find confusing to see his setting ignored.</p>
     *
     * @param  logger The base logger to apply the change on.
     * @param  level The desired level, or {@code null} if no level should be set.
     * @return The registered {@code MonolineFormatter}, or {@code null} if the registration failed.
     *         If non-null, the formatter output can be configured using the {@link #setTimeFormat}
     *         and {@link #setSourceFormat} methods.
     */
    public static MonolineFormatter configureConsoleHandler(final Logger logger, final Level level) {
        MonolineFormatter monoline = null;
        boolean foundConsoleHandler = false;
        Handler[] handlers = logger.getHandlers();
        for (int i=0; i<handlers.length; i++) {
            final Handler handler = handlers[i];
            if (handler.getClass() == ConsoleHandler.class) {
                foundConsoleHandler = true;
                final Formatter formatter = handler.getFormatter();
                if (formatter instanceof MonolineFormatter) {
                    /*
                     * A MonolineFormatter already existed. Sets the level only for the first
                     * instance (only one instance should exists anyway) for consistency with
                     * the fact that this method returns only one MonolineFormatter for further
                     * configuration.
                     */
                    if (monoline == null) {
                        monoline = (MonolineFormatter) formatter;
                        setLevel(handler, level);
                    }
                } else if (formatter.getClass() == SimpleFormatter.class) {
                    /*
                     * A ConsoleHandler using the SimpleFormatter has been found. Replaces
                     * the SimpleFormatter by MonolineFormatter, creating it if necessary.
                     * If the handler setting fail with an exception, then we will continue
                     * to use the old J2SE handler instead.
                     */
                    try {
                        setLevel(handler, level);
                    } catch (SecurityException exception) {
                        unexpectedException(exception);
                    }
                    if (monoline == null) {
                        monoline = new MonolineFormatter(handler);
                    }
                    try {
                        handler.setFormatter(monoline);
                    } catch (SecurityException exception) {
                        unexpectedException(exception);
                    }
                }
            }
        }
        /*
         * If the logger uses parent handlers, copy them to the logger that we are initializing,
         * because we will not use parent handlers anymore at the end of this method.
         */
        for (Logger parent=logger; parent.getUseParentHandlers();) {
            parent = parent.getParent();
            if (parent == null) {
                break;
            }
            handlers = parent.getHandlers();
            for (int i=0; i<handlers.length; i++) {
                Handler handler = handlers[i];
                if (handler.getClass() == ConsoleHandler.class) {
                    if (!foundConsoleHandler) {
                        // We have already set a ConsoleHandler and we don't want a second one.
                        continue;
                    }
                    foundConsoleHandler = true;
                    final Formatter formatter = handler.getFormatter();
                    if (formatter.getClass() == SimpleFormatter.class) {
                        monoline = addHandler(logger, level);
                        continue;
                    }
                }
                logger.addHandler(handler);
            }
        }
        logger.setUseParentHandlers(false);
        if (!foundConsoleHandler) {
            monoline = addHandler(logger, level);
        }
        return monoline;
    }

    /**
     * Adds to the specified logger a {@link Handler} using a {@code MonolineFormatter}
     * set at the specified level. The formatter is returned for convenience.
     */
    private static MonolineFormatter addHandler(final Logger logger, final Level level) {
        MonolineFormatter monoline = null;
        try {
            final Handler handler = new ConsoleHandler();
            monoline = new MonolineFormatter(handler);
            handler.setFormatter(monoline);
            setLevel(handler, level);
            logger.addHandler(handler);
        } catch (SecurityException exception) {
            unexpectedException(exception);
            /*
             * Returns without any change to the J2SE configuration. It will not
             * prevent to program to work; just produces different logging outputs.
             */
        }
        return monoline;
    }

    /**
     * Sets the level of the given handler. This method tries to find a balance between user's
     * setting and desired level using heuristic rules that may change in any future version.
     */
    private static void setLevel(final Handler handler, final Level level) {
        if (level != null) {
            final int desired = level.intValue();
            final int current = handler.getLevel().intValue();
            if (desired < LEVEL_THRESHOLD.intValue() ? desired < current : desired > current) {
                handler.setLevel(level);
            }
        }
    }

    /**
     * Invoked when an error occurs during the initialization.
     */
    private static void unexpectedException(final Exception exception) {
        Logging.unexpectedException(MonolineFormatter.class, "configureConsoleHandler", exception);
    }
}
