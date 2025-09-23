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
package org.apache.sis.util.resources;

import java.net.URI;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.lang.reflect.Modifier;
import javax.measure.Unit;
import org.opengis.util.CodeList;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.system.Loggers;
import org.apache.sis.system.Configuration;
import org.apache.sis.util.internal.AutoMessageFormat;
import org.apache.sis.util.internal.shared.MetadataServices;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.measure.RangeFormat;
import org.apache.sis.measure.Range;


/**
 * {@link ResourceBundle} implementation accepting integers instead of strings for resource keys.
 * Using integers allow implementations to avoid adding large string constants into their
 * {@code .class} files and runtime images. Developers still have meaningful labels in their
 * code (e.g. {@code MismatchedDimension}) through a set of constants defined in {@code Keys}
 * inner classes, with the side-effect of compile-time safety. Because integer constants are
 * inlined right into class files at compile time, the declarative classes is not loaded at run time.
 *
 * <p>Localized resources are fetched by calls to {@link #getString(short)}.
 * Arguments can optionally be provided by calls to {@link #getString(short, Object) getString(short, Object, ...)}.
 * If arguments are present, then the string will be formatted using {@link MessageFormat},
 * completed by some special cases handled by this class. Roughly speaking:</p>
 *
 * <ul>
 *   <li>{@link Number}, {@link java.util.Date}, {@link CodeList} and {@link InternationalString} instances
 *       are localized using the current {@code ResourceBundle} locale.</li>
 *   <li>Long {@link CharSequence} instances are shortened by {@link CharSequences#shortSentence(CharSequence, int)}.</li>
 *   <li>{@link Class} and {@link Throwable} instances are summarized.</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * The same {@code IndexedResourceBundle} instance can be safely used by many threads without synchronization
 * on the part of the caller. Subclasses should make sure that any overridden methods remain safe to call from
 * multiple threads.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public abstract class IndexedResourceBundle extends ResourceBundle implements Localized {
    /**
     * The logger for localization events.
     */
    public static final Logger LOGGER = Logger.getLogger(Loggers.LOCALIZATION);

    /**
     * Key used in properties map for localizing some aspects of the operation being executed.
     * The {@code forProperties(Map<?,?>)} methods defined in some sub-classes will look for this property.
     *
     * @see org.apache.sis.referencing.AbstractIdentifiedObject#LOCALE_KEY
     */
    public static final String LOCALE_KEY = "locale";

    /**
     * Maximum string length for text inserted into another text. This parameter is used by {@link #toArray(Object)}.
     * Resource strings are never cut to this length. However, text replacing {@code "{0}"} in a string like
     * {@code "Parameter name is {0}"} will be cut to this length.
     */
    @Configuration
    private static final int MAX_STRING_LENGTH = 200;

    /**
     * First valid key index.
     * We start at 1 rather than 0 in order to keep value 0 available for meaning "no localized message".
     */
    static final int FIRST = 1;

    /**
     * The array of resources. Keys are an array index plus {@value #FIRST}.
     * For example, the value for key "14" is {@code values[13]}.
     *
     * This array will be loaded only when first needed. We should not load it at construction time,
     * because some {@code ResourceBundle} instances will never ask for values. In particular, parent
     * of {@code Resources_en}, {@code Resources_fr}, <i>etc.</i> which will only be queries if a key
     * has not been found in the child resources.
     *
     * @see #ensureLoaded(String)
     */
    @SuppressWarnings("VolatileArrayField")     // Okay because we set this field only after the array has been fully constructed.
    private volatile String[] values;

    /**
     * The object to use for formatting messages. This object
     * will be constructed only when first needed.
     */
    private transient AutoMessageFormat format;

    /**
     * The key of the last resource requested. If the same resource is requested multiple times,
     * knowing its key allows us to avoid invoking the costly {@link MessageFormat#applyPattern}
     * method.
     */
    private transient short lastKey;

    /**
     * Constructs a new resource bundle loading data from a UTF file derived from the class name.
     */
    protected IndexedResourceBundle() {
    }

    /**
     * Returns the given locale if non-null, or the default locale otherwise.
     *
     * @param  locale  the user-specified locale.
     * @return the locale to use for fetching a resource bundle.
     */
    protected static Locale nonNull(final Locale locale) {
        return (locale != null) ? locale : Locale.getDefault();
    }

    /**
     * Opens the binary file containing the localized resources to load.
     * Subclasses shall implement this method as below:
     *
     * {@snippet lang="java" :
     *     return getClass().getResourceAsStream(name);
     *     }
     *
     * Above implementation needs to be provided by subclasses because the
     * call to {@link Class#getResourceAsStream(String)} is caller-sensitive.
     *
     * @param  name  name of the UTF file to open.
     * @return a stream opened on the given file.
     */
    protected abstract InputStream getResourceAsStream(String name);

    /**
     * Returns a handler for the constants declared in the inner {@code Keys} class.
     *
     * @return a handler for the constants declared in the inner {@code Keys} class.
     */
    protected abstract KeyConstants getKeyConstants();

    /**
     * Returns an enumeration of the keys.
     *
     * @return all keys in this resource bundle.
     */
    @Override
    public final Enumeration<String> getKeys() {
        return new KeyEnum(getKeyConstants().getKeyNames());
    }

    /**
     * The keys as an enumeration. This enumeration needs to skip null values, which
     * may occur if the resource bundle is incomplete for that particular locale.
     */
    private static final class KeyEnum implements Enumeration<String> {
        /** The keys to return.          */ private final String[] keys;
        /** Index of next key to return. */ private int next;

        /** Creates a new enum for the given array of keys. */
        KeyEnum(final String[] keys) {
            this.keys = keys;
        }

        /** Returns {@code true} if there is at least one more non-null key. */
        @Override public boolean hasMoreElements() {
            while (next < keys.length) {
                if (keys[next] != null) {
                    return true;
                }
                next++;
            }
            return false;
        }

        /** Returns the next key. */
        @Override public String nextElement() {
            while (next < keys.length) {
                final String key = keys[next++];
                if (key != null) {
                    return key;
                }
            }
            throw new NoSuchElementException();
        }
    }

    /**
     * Lists resources to the specified stream. If a resource has more than one line, only
     * the first line will be written. This method is used mostly for debugging purposes.
     *
     * @param  out  the destination stream.
     * @throws IOException if an output operation failed.
     */
    @Debug
    public final void list(final Appendable out) throws IOException {
        int keyLength = 0;
        final String[] keys = getKeyConstants().getKeyNames();
        for (final String key : keys) {
            if (key != null) {
                keyLength = Math.max(keyLength, key.length());
            }
        }
        final String lineSeparator = System.lineSeparator();
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final String[] values = ensureLoaded(null);
        for (int i=0; i < values.length; i++) {
            final String key   = keys  [i];
            final String value = values[i];
            if (key != null && value != null) {
                int indexCR = value.indexOf('\r'); if (indexCR < 0) indexCR = value.length();
                int indexLF = value.indexOf('\n'); if (indexLF < 0) indexLF = value.length();
                final String number = String.valueOf(i);
                out.append(CharSequences.spaces(5 - number.length()))
                   .append(number)
                   .append(": ")
                   .append(key)
                   .append(CharSequences.spaces(keyLength - key.length()))
                   .append(" = ")
                   .append(value, 0, Math.min(indexCR, indexLF))
                   .append(lineSeparator);
            }
        }
    }

    /**
     * Ensures that resource values are loaded. If they are not, loads them immediately.
     *
     * @param  key  key for the requested resource, or {@code null} if all resources
     *         are requested. This key is used mostly for constructing messages.
     * @return the resources.
     * @throws MissingResourceException if this method failed to load resources.
     */
    private String[] ensureLoaded(final String key) throws MissingResourceException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        String[] values = this.values;
        if (values == null) synchronized (this) {
            values = this.values;
            if (values == null) {
                final InputStream resources = getResourceAsStream(getClass().getSimpleName() + ".utf");
                /*
                 * If there are no explicit resources for this instance, inherit the resources
                 * from the parent. Note that this IndexedResourceBundle instance may still
                 * differ from its parent in the way dates and numbers are formatted.
                 */
                if (resources == null) {
                    /*
                     * If we get a NullPointerException or ClassCastException here,
                     * it would be a bug in the way we create the chain of parents.
                     */
                    values = ((IndexedResourceBundle) parent).ensureLoaded(key);
                } else {
                    /*
                     * Prepares a log record.  We will wait for successful loading before
                     * posting this record.  If loading fails, the record will be changed
                     * into an error record. Note that the message must be logged outside
                     * the synchronized block, otherwise there is dead locks!
                     */
                    final Locale    locale     = getLocale();                         // Sometimes null with IBM's JDK.
                    final String    baseName   = getClass().getCanonicalName();
                    final String    methodName = (key != null) ? "getObject" : "getKeys";
                    final LogRecord record     = new LogRecord(Level.FINER, "Loaded resources for {0} from bundle \"{1}\".");
                    /*
                     * Loads resources from the UTF file.
                     */
                    try (DataInputStream input = new DataInputStream(new BufferedInputStream(resources))) {
                        values = new String[input.readInt()];
                        for (int i=0; i<values.length; i++) {
                            values[i] = input.readUTF();
                            if (values[i].isEmpty()) {
                                values[i] = null;
                            }
                        }
                    } catch (IOException exception) {
                        record.setLevel  (Level.WARNING);
                        record.setMessage(exception.getMessage());              // For administrator, use system locale.
                        record.setThrown (exception);
                        Logging.completeAndLog(LOGGER, IndexedResourceBundle.class, methodName, record);
                        throw (MissingResourceException) new MissingResourceException(
                                Exceptions.getLocalizedMessage(exception, locale),   // For users, use requested locale.
                                baseName, key).initCause(exception);
                    }
                    /*
                     * Now, logs the message. This message is provided only in English.
                     * Note that Locale.getDisplayName() may return different string on
                     * different Java implementation, but it doesn't matter here since
                     * we use the result only for logging purpose.
                     */
                    if (LOGGER.isLoggable(record.getLevel())) {
                        String language = null;
                        if (locale != null) {
                            language = locale.getDisplayName(Locale.US);
                        }
                        if (Strings.isNullOrEmpty(language)) {
                            language = "<root>";
                        }
                        record.setParameters(new String[] {language, baseName});
                        Logging.completeAndLog(LOGGER, IndexedResourceBundle.class, methodName, record);
                    }
                }
                this.values = values;
            }
        }
        return values;
    }

    /**
     * Gets an object for the given key from this resource bundle.
     * Returns null if this resource bundle does not contain an
     * object for the given key.
     *
     * @param  key  the key for the desired object
     * @throws NullPointerException if {@code key} is {@code null}
     * @return the object for the given key, or null
     */
    @Override
    protected final Object handleGetObject(final String key) {
        /*
         * Note: Synchronization is performed by 'ensureLoaded'
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final String[] values = ensureLoaded(key);
        int keyID;
        try {
            keyID = Short.parseShort(key);
        } catch (NumberFormatException exception) {
            /*
             * Maybe the full key name has been specified instead. We do that for localized
             * LogRecords, for easier debugging if the message has not been properly formatted.
             */
            try {
                keyID = getKeyConstants().getKeyValue(key);
            } catch (ReflectiveOperationException e) {
                e.addSuppressed(exception);
                Logging.recoverableException(LOGGER, getClass(), "handleGetObject", e);
                return null;                // This is okay as of 'handleGetObject' contract.
            }
        }
        keyID -= FIRST;
        return (keyID >= 0 && keyID < values.length) ? values[keyID] : null;
    }

    /**
     * Returns {@code arguments} as an array, and convert some types that are not recognized
     * by {@link MessageFormat}. If {@code arguments} is already an array, then that array or
     * a copy of that array will be returned. If {@code arguments} is not an array, it will be
     * placed in an array of length 1.
     *
     * <p>All the array elements will be checked for {@link CharSequence}, {@link InternationalString},
     * {@link CodeList}, {@link Throwable} or {@link Class} instances.
     * All {@code InternationalString} instances will be localized according this resource bundle locale.
     * Any characters sequences of length greater than {@link #MAX_STRING_LENGTH} will be shortened using
     * the {@link CharSequences#shortSentence(CharSequence, int)} method.</p>
     *
     * <h4>Note for maintainers</h4>
     * If more cases are added, remember to update class and package javadoc.
     *
     * @param  arguments  the object to check.
     * @return {@code arguments} as an array, eventually with some elements replaced.
     */
    final Object[] toArray(final Object arguments) {
        Object[] array;
        if (arguments instanceof Object[]) {
            array = (Object[]) arguments;
        } else {
            array = new Object[] {arguments};
        }
        for (int i=0; i<array.length; i++) {
            final Object element = array[i];
            if (element == null) continue;
            Object replacement = element;
            if (element instanceof CharSequence) {
                CharSequence text = (CharSequence) element;
                if (text instanceof InternationalString) {
                    text = ((InternationalString) element).toString(getLocale());
                }
                replacement = CharSequences.shortSentence(text, MAX_STRING_LENGTH);
            } else if (element instanceof URI) {
                replacement = ((URI) element).getSchemeSpecificPart();      // For decoding encoded characters.
            } else if (element instanceof Class<?>) {
                replacement = Classes.getShortName(getPublicType((Class<?>) element));
            } else if (element instanceof CodeList<?>) {
                replacement = MetadataServices.getInstance().getCodeTitle((CodeList<?>) element, getLocale());
            } else if (element instanceof Range<?>) {
                final Range<?> range = (Range<?>) element;
                replacement = new RangeFormat(getLocale(), range.getElementType()).format(range);
            } else if (element instanceof Unit<?>) {
                String s = element.toString();
                if (s.isEmpty()) s = "1";
                replacement = s;
            } else if (element.getClass().isArray()) {
                replacement = Utilities.deepToString(element);
            } else if (element instanceof Throwable) {
                String message = Exceptions.getLocalizedMessage((Throwable) element, getLocale());
                if (message == null) {
                    message = Classes.getShortClassName(element);
                }
                replacement = message;
            }
            /*
             * No need to check for Numbers or Dates instances, since they are
             * properly formatted in the ResourceBundle locale by MessageFormat.
             */
            if (replacement != element) {
                if (array == arguments) {
                    array = Arrays.copyOf(array, array.length, Object[].class);
                }
                array[i] = replacement;
            }
        }
        return array;
    }

    /**
     * If the given class is not public, returns the first public interface or the first public super-class.
     * This is for avoiding confusing the user with private class in message like "Value cannot be instance
     * of XYZ".
     */
    private static Class<?> getPublicType(Class<?> c) {
        while (!Modifier.isPublic(c.getModifiers())) {
            for (final Class<?> type : c.getInterfaces()) {
                if (Modifier.isPublic(type.getModifiers()) && !type.getName().startsWith("java")) {
                    return type;
                }
            }
            c = c.getSuperclass();
        }
        return c;
    }

    /**
     * Writes the localized string identified by the given key followed by a colon.
     * The way to write the colon depends on the language.
     *
     * <h4>API note</h4>
     * We do not provide a method with {@link StringBuilder} argument and without {@link IOException} clause
     * because it is not needed by Apache SIS in practice. We found that codes invoking this method with a
     * {@link StringBuilder} happen in contexts where an {@link IOException} is thrown elsewhere anyway.
     *
     * @param  key         the key for the desired string.
     * @param  toAppendTo  where to write the localized string followed by a colon.
     * @throws IOException if an error occurred while writing to the given destination.
     */
    public final void appendLabel(final short key, final Appendable toAppendTo) throws IOException {
        toAppendTo.append(getString(key));
        final String colon = colon();
        if (colon != null) {
            toAppendTo.append(colon);
        } else {
            toAppendTo.append(':');
        }
    }

    /**
     * Returns the given string followed by a colon.
     *
     * @param  text  the text to follow be a colon.
     * @return the given text followed by a colon.
     */
    public final String toLabel(final String text) {
        return text.concat(colon());
    }

    /**
     * Returns the localized string identified by the given key followed by a colon.
     * This is the same functionality as {@link #appendLabel(short, Appendable)} but
     * without temporary buffer.
     *
     * @param  key  the key for the desired string.
     * @return localized string followed by a colon.
     */
    public final String getLabel(final short key) {
        final String text = getString(key);
        final String colon = colon();
        return (colon != null) ? (text + colon) : (text + ':');
    }

    /**
     * Returns the colon to write after localized text.
     *
     * @todo Should be a localized resource by itself.
     */
    private String colon() {
        return Locale.FRENCH.getLanguage().equals(getLocale().getLanguage()) ? "\u00A0:" : null;
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its parents.
     *
     * @param  key  the key for the desired string.
     * @return the string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public final String getString(final short key) throws MissingResourceException {
        return getString(String.valueOf(key));
    }

    /**
     * Gets a string for the given key and formats it with the specified argument. The message is
     * formatted using {@link MessageFormat}. Calling this method is approximately equivalent to
     * calling:
     *
     * {@snippet lang="java" :
     *     String pattern = getString(key);
     *     Format f = new MessageFormat(pattern);
     *     return f.format(arg0);
     *     }
     *
     * If {@code arg0} is not already an array, it will be placed into an array of length 1. Using
     * {@link MessageFormat}, all occurrences of "{0}", "{1}", "{2}" in the resource string will be
     * replaced by {@code arg0[0]}, {@code arg0[1]}, {@code arg0[2]}, etc.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  a single object or an array of objects to be formatted and substituted.
     * @return the string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     *
     * @see #getString(String)
     * @see #getString(short,Object,Object)
     * @see #getString(short,Object,Object,Object)
     * @see MessageFormat
     */
    public final String getString(final short key, final Object arg0) throws MissingResourceException {
        final String pattern = getString(key);
        final Object[] arguments = toArray(arg0);
        synchronized (this) {
            if (format == null) {
                /*
                 * Constructs a new MessageFormat for formatting the arguments.
                 */
                format  = new AutoMessageFormat(pattern, getLocale());
                lastKey = key;
            } else if (key != lastKey) {
                /*
                 * Method MessageFormat.applyPattern(…) is costly! We will avoid
                 * calling it again if the format already has the right pattern.
                 */
                format.applyPattern(pattern);
                lastKey = key;
            }
            try {
                format.configure(arguments);
                return format.format(arguments);
            } catch (RuntimeException e) {
                /*
                 * Safety against badly implemented toString() method
                 * in libraries that we do not control.
                 */
                return "[Unformattable message: " + e + ']';
            }
        }
    }

    /**
     * Gets a string for the given key and replaces all occurrences of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}, etc.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  value to substitute for "{0}".
     * @param  arg1  value to substitute for "{1}".
     * @return the formatted string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public final String getString(final short  key,
                                  final Object arg0,
                                  final Object arg1) throws MissingResourceException
    {
        return getString(key, new Object[] {arg0, arg1});
    }

    /**
     * Gets a string for the given key and replaces all occurrences of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}, etc.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  value to substitute for "{0}".
     * @param  arg1  value to substitute for "{1}".
     * @param  arg2  value to substitute for "{2}".
     * @return the formatted string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public final String getString(final short  key,
                                  final Object arg0,
                                  final Object arg1,
                                  final Object arg2) throws MissingResourceException
    {
        return getString(key, new Object[] {arg0, arg1, arg2});
    }

    /**
     * Gets a string for the given key and replaces all occurrences of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}, etc.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  value to substitute for "{0}".
     * @param  arg1  value to substitute for "{1}".
     * @param  arg2  value to substitute for "{2}".
     * @param  arg3  value to substitute for "{3}".
     * @return the formatted string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public final String getString(final short  key,
                                  final Object arg0,
                                  final Object arg1,
                                  final Object arg2,
                                  final Object arg3) throws MissingResourceException
    {
        return getString(key, new Object[] {arg0, arg1, arg2, arg3});
    }

    /**
     * Gets a string for the given key and replaces all occurrences of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}, etc.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  value to substitute for "{0}".
     * @param  arg1  value to substitute for "{1}".
     * @param  arg2  value to substitute for "{2}".
     * @param  arg3  value to substitute for "{3}".
     * @param  arg4  value to substitute for "{4}".
     * @return the formatted string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public final String getString(final short  key,
                                  final Object arg0,
                                  final Object arg1,
                                  final Object arg2,
                                  final Object arg3,
                                  final Object arg4) throws MissingResourceException
    {
        return getString(key, new Object[] {arg0, arg1, arg2, arg3, arg4});
    }

    /**
     * Creates a new log record with the localized message identified by the given key.
     * The logger name, source class and source method are initially {@code null}.
     *
     * @param  level  the log record level.
     * @param  key    the resource key.
     * @return the log record.
     */
    public final LogRecord createLogRecord(final Level level, final short key) {
        final var record = new LogRecord(level, getKeyConstants().getKeyName(key));
        record.setResourceBundleName(getClass().getName());
        record.setResourceBundle(this);
        return record;
    }

    /**
     * Creates a new log record with the localized message identified by the given key.
     * The logger name, source class and source method are initially {@code null}.
     *
     * @param  level  the log record level.
     * @param  key    the resource key.
     * @param  arg0   the parameter for the log message, which may be an array.
     * @return the log record.
     */
    public final LogRecord createLogRecord(final Level level, final short key,
                                           final Object arg0)
    {
        final LogRecord record = createLogRecord(level, key);
        record.setParameters(toArray(arg0));
        return record;
    }

    /**
     * Creates a new log record with the localized message identified by the given key.
     * The logger name, source class and source method are initially {@code null}.
     *
     * @param  level  the log record level.
     * @param  key    the resource key.
     * @param  arg0   the first parameter.
     * @param  arg1   the second parameter.
     * @return the log record.
     */
    public final LogRecord createLogRecord(final Level level, final short key,
                                           final Object arg0,
                                           final Object arg1)
    {
        return createLogRecord(level, key, new Object[] {arg0, arg1});
    }

    /**
     * Creates a new log record with the localized message identified by the given key.
     * The logger name, source class and source method are initially {@code null}.
     *
     * @param  level  the log record level.
     * @param  key    the resource key.
     * @param  arg0   the first parameter.
     * @param  arg1   the second parameter.
     * @param  arg2   the third parameter.
     * @return the log record.
     */
    public final LogRecord createLogRecord(final Level level, final short key,
                                           final Object arg0,
                                           final Object arg1,
                                           final Object arg2)
    {
        return createLogRecord(level, key, new Object[] {arg0, arg1, arg2});
    }

    /**
     * Creates a new log record with the localized message identified by the given key.
     * The logger name, source class and source method are initially {@code null}.
     *
     * @param  level  the log record level.
     * @param  key    the resource key.
     * @param  arg0   the first parameter.
     * @param  arg1   the second parameter.
     * @param  arg2   the third parameter.
     * @param  arg3   the fourth parameter.
     * @return the log record.
     */
    public final LogRecord createLogRecord(final Level level, final short key,
                                           final Object arg0,
                                           final Object arg1,
                                           final Object arg2,
                                           final Object arg3)
    {
        return createLogRecord(level, key, new Object[] {arg0, arg1, arg2, arg3});
    }

    /**
     * Returns the locale specified in the given map, or {@code null} if none.
     * Value of unexpected type are ignored.
     *
     * @param  properties  the map of properties, or {@code null} if none.
     * @return the locale found in the given map, or {@code null} if none.
     */
    public static Locale getLocale(final Map<?,?> properties) {
        if (properties != null) {
            final Object candidate = properties.get(LOCALE_KEY);
            if (candidate instanceof Locale) {
                return (Locale) candidate;
            }
        }
        return null;
    }

    /**
     * Concatenates two sentences. The concatenation order is locale-sensitive.
     * Current implementation ignores the locale and always concatenate the sentence from left to right.
     * This method is defined for centralizing the places where such concatenations are done, for making
     * easier to change this order if a future Apache SIS version supports right to left writing systems.
     *
     * @param  first   the first sentence, or {@code null} or empty.
     * @param  second  the second sentence, or {@code null} or empty.
     * @return the concatenated sentence.
     */
    public static String concatenate(final String first, final String second) {
        if (first  == null ||  first.isBlank()) return second;
        if (second == null || second.isBlank()) return first;
        return first + ' ' + second;
    }

    /**
     * Returns a string representation of this object.
     * This method is for debugging purposes only.
     *
     * @return a string representation of this resources bundle.
     */
    @Override
    public synchronized String toString() {
        return Strings.bracket(getClass(), getLocale());
    }
}
