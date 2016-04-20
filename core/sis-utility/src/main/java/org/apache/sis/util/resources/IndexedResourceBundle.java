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

import java.net.URL;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.lang.reflect.Modifier;
import javax.measure.unit.Unit;
import org.opengis.util.CodeList;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.PatchedUnitFormat;

// Related to JDK7
import org.apache.sis.internal.jdk7.JDK7;


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
 * <div class="section">Thread safety</div>
 * The same {@code IndexedResourceBundle} instance can be safely used by many threads without synchronization
 * on the part of the caller. Subclasses should make sure that any overridden methods remain safe to call from
 * multiple threads.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public class IndexedResourceBundle extends ResourceBundle implements Localized {
    /**
     * Key used in properties map for localizing some aspects of the operation being executed.
     * The {@code getResources(Map<?,?>)} methods defined in some sub-classes will look for this property.
     *
     * @see org.apache.sis.referencing.AbstractIdentifiedObject#LOCALE_KEY
     */
    public static final String LOCALE_KEY = "locale";

    /**
     * Maximum string length for text inserted into another text. This parameter is used by {@link #toArray(Object)}.
     * Resource strings are never cut to this length. However, text replacing {@code "{0}"} in a string like
     * {@code "Parameter name is {0}"} will be cut to this length.
     */
    private static final int MAX_STRING_LENGTH = 200;

    /**
     * The path of the binary file containing resources, or {@code null} if there is no resources
     * or if the resources have already been loaded. The resources may be a file or an entry in a
     * JAR file.
     */
    private URL resources;

    /**
     * The array of resources. Keys are an array index. For example, the value for key "14" is
     * {@code values[14]}. This array will be loaded only when first needed. We should not load
     * it at construction time, because some {@code ResourceBundle} objects will never ask for
     * values. This is particularly the case for parent resources of {@code Resources_fr_CA},
     * {@code Resources_en}, {@code Resources_de}, etc., which will only be used if a key has
     * not been found in the child resources.
     *
     * @see #ensureLoaded(String)
     */
    @SuppressWarnings("VolatileArrayField")     // Okay because we set this field only after the array has been fully constructed.
    private volatile String[] values;

    /**
     * The object to use for formatting messages. This object
     * will be constructed only when first needed.
     */
    private transient MessageFormat format;

    /**
     * The key of the last resource requested. If the same resource is requested multiple times,
     * knowing its key allows us to avoid invoking the costly {@link MessageFormat#applyPattern}
     * method.
     */
    private transient short lastKey;

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param resources The path of the binary file containing resources, or {@code null} if
     *        there is no resources. The resources may be a file or an entry in a JAR file.
     */
    protected IndexedResourceBundle(final URL resources) {
        this.resources = resources;
    }

    /**
     * Returns a resource bundle of the specified class.
     *
     * @param  <T>     The resource bundle class.
     * @param  base    The resource bundle class.
     * @param  locale  The locale, or {@code null} for the default locale.
     * @return Resources in the given locale.
     * @throws MissingResourceException if resources can't be found.
     *
     * @see Vocabulary#getResources(Locale)
     * @see Errors#getResources(Locale)
     */
    protected static <T extends IndexedResourceBundle> T getBundle(Class<T> base, Locale locale)
            throws MissingResourceException
    {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        // No caching; we rely on the one implemented in ResourceBundle.
        return base.cast(getBundle(base.getName(), locale, base.getClassLoader(), Loader.INSTANCE));
    }

    /**
     * Returns a handler for the constants declared in the inner {@code Keys} class.
     * Subclasses defined in the {@code org.apache.sis.util.resources} package
     * override this method for efficiency. However the default implementation
     * should work for other cases (we don't want to expose too much internal API).
     *
     * @return A handler for the constants declared in the inner {@code Keys} class.
     */
    KeyConstants getKeyConstants() {
        Class<?> keysClass = KeyConstants.class;
        for (final Class<?> inner : getClass().getClasses()) {
            if ("Keys".equals(inner.getSimpleName())) {
                keysClass = inner;
                break;
            }
        }
        return new KeyConstants(keysClass);
    }

    /**
     * Returns an enumeration of the keys.
     *
     * @return All keys in this resource bundle.
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
     * @param  out The destination stream.
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
        final String lineSeparator = JDK7.lineSeparator();
        final String[] values = ensureLoaded(null);
        for (int i=0; i<values.length; i++) {
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
     * @param  key Key for the requested resource, or {@code null} if all resources
     *         are requested. This key is used mostly for constructing messages.
     * @return The resources.
     * @throws MissingResourceException if this method failed to load resources.
     */
    private String[] ensureLoaded(final String key) throws MissingResourceException {
        String[] values = this.values;
        if (values == null) synchronized (this) {
            values = this.values;
            if (values == null) {
                /*
                 * If there is no explicit resources for this instance, inherit the resources
                 * from the parent. Note that this IndexedResourceBundle instance may still
                 * differ from its parent in the way dates and numbers are formatted.
                 */
                if (resources == null) {
                    // If we get a NullPointerException or ClassCastException here,
                    // it would be a bug in the way we create the chain of parents.
                    values = ((IndexedResourceBundle) parent).ensureLoaded(key);
                } else {
                    /*
                     * Prepares a log record.  We will wait for successful loading before
                     * posting this record.  If loading fails, the record will be changed
                     * into an error record. Note that the message must be logged outside
                     * the synchronized block, otherwise there is dead locks!
                     */
                    final Locale    locale     = getLocale(); // Sometime null with IBM's JDK.
                    final String    baseName   = getClass().getCanonicalName();
                    final String    methodName = (key != null) ? "getObject" : "getKeys";
                    final LogRecord record     = new LogRecord(Level.FINER, "Loaded resources for {0} from bundle \"{1}\".");
                    record.setLoggerName(Loggers.LOCALIZATION);
                    /*
                     * Loads resources from the UTF file.
                     */
                    try {
                        DataInputStream input = new DataInputStream(new BufferedInputStream(resources.openStream()));
                        values = new String[input.readInt()];
                        for (int i=0; i<values.length; i++) {
                            values[i] = input.readUTF();
                            if (values[i].isEmpty()) {
                                values[i] = null;
                            }
                        }
                        input.close();
                    } catch (IOException exception) {
                        record.setLevel  (Level.WARNING);
                        record.setMessage(exception.getMessage()); // For administrator, use system locale.
                        record.setThrown (exception);
                        Logging.log(IndexedResourceBundle.class, methodName, record);
                        final MissingResourceException error = new MissingResourceException(
                                Exceptions.getLocalizedMessage(exception, locale), // For users, use requested locale.
                                baseName, key);
                        error.initCause(exception);
                        throw error;
                    }
                    /*
                     * Now, logs the message. This message is provided only in English.
                     * Note that Locale.getDisplayName() may return different string on
                     * different Java implementation, but it doesn't matter here since
                     * we use the result only for logging purpose.
                     */
                    String language = null;
                    if (locale != null) {
                        language = locale.getDisplayName(Locale.US);
                    }
                    if (language == null || language.isEmpty()) {
                        language = "<root>";
                    }
                    record.setParameters(new String[] {language, baseName});
                    Logging.log(IndexedResourceBundle.class, methodName, record);
                    resources = null; // Not needed anymore, let GC do its job.
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
     * @param  key the key for the desired object
     * @throws NullPointerException if {@code key} is {@code null}
     * @return the object for the given key, or null
     */
    @Override
    protected final Object handleGetObject(final String key) {
        // Synchronization performed by 'ensureLoaded'
        final String[] values = ensureLoaded(key);
        short keyID;
        try {
            keyID = Short.parseShort(key);
        } catch (NumberFormatException exception) {
            /*
             * Maybe the full key name has been specified instead. We do that for localized
             * LogRecords, for easier debugging if the message has not been properly formatted.
             */
            try {
                keyID = getKeyConstants().getKeyValue(key);
            } catch (Exception e) {
                Logging.recoverableException(Logging.getLogger(Loggers.LOCALIZATION), getClass(), "handleGetObject", e);
                return null; // This is okay as of 'handleGetObject' contract.
            }
        }
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
     * <div class="note"><b>Note:</b>
     * If more cases are added, remember to update class and package javadoc.</div>
     *
     * @param  arguments The object to check.
     * @return {@code arguments} as an array, eventually with some elements replaced.
     */
    private Object[] toArray(final Object arguments) {
        Object[] array;
        if (arguments instanceof Object[]) {
            array = (Object[]) arguments;
        } else {
            array = new Object[] {arguments};
        }
        for (int i=0; i<array.length; i++) {
            final Object element = array[i];
            Object replacement = element;
            if (element instanceof CharSequence) {
                CharSequence text = (CharSequence) element;
                if (text instanceof InternationalString) {
                    text = ((InternationalString) element).toString(getLocale());
                }
                replacement = CharSequences.shortSentence(text, MAX_STRING_LENGTH);
            } else if (element instanceof Throwable) {
                String message = Exceptions.getLocalizedMessage((Throwable) element, getLocale());
                if (message == null) {
                    message = Classes.getShortClassName(element);
                }
                replacement = message;
            } else if (element instanceof Class<?>) {
                replacement = Classes.getShortName(getPublicType((Class<?>) element));
            } else if (element instanceof CodeList<?>) {
                replacement = Types.getCodeTitle((CodeList<?>) element).toString(getLocale());
            } else if (element instanceof Unit<?>) {
                replacement = PatchedUnitFormat.toString((Unit<?>) element);
            }
            // No need to check for Numbers or Dates instances, since they are
            // properly formatted in the ResourceBundle locale by MessageFormat.
            if (replacement != element) {
                if (array == arguments) {
                    array = array.clone();                  // Protect the user-provided array from change.
                }
                array[i] = replacement;
            }
        }
        return array;
    }

    /**
     * If the given class is not public, returns the first public interface or the first public super-class.
     * This is for avoiding confusing the user with private class in message like "Value can not be instance
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
     * Gets a string for the given key and appends "…" to it.
     * This method is typically used for creating menu items.
     *
     * @param  key The key for the desired string.
     * @return The string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public final String getMenuLabel(final short key) throws MissingResourceException {
        return getString(key) + '…';
    }

    /**
     * Gets a string for the given key and appends ":" to it.
     * A space may or may not be added before ":", depending on the locale.
     * No space is added after the string; it is up to the caller to add such space if needed.
     *
     * @param  key The key for the desired string.
     * @return The string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public final String getLabel(final short key) throws MissingResourceException {
        String label = getString(key);
        if (Locale.FRENCH.getLanguage().equals(getLocale().getLanguage())) {
            label += "\u00A0:";
        } else {
            label += ':';
        }
        return label;
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its parents.
     *
     * @param  key The key for the desired string.
     * @return The string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public final String getString(final short key) throws MissingResourceException {
        return getString(String.valueOf(key));
    }

    /**
     * Gets a string for the given key and formats it with the specified argument. The message is
     * formatted using {@link MessageFormat}. Calling this method is approximately equivalent to
     * calling:
     *
     * {@preformat java
     *     String pattern = getString(key);
     *     Format f = new MessageFormat(pattern);
     *     return f.format(arg0);
     * }
     *
     * If {@code arg0} is not already an array, it will be placed into an array of length 1. Using
     * {@link MessageFormat}, all occurrences of "{0}", "{1}", "{2}" in the resource string will be
     * replaced by {@code arg0[0]}, {@code arg0[1]}, {@code arg0[2]}, etc.
     *
     * @param  key The key for the desired string.
     * @param  arg0 A single object or an array of objects to be formatted and substituted.
     * @return The string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
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
                format = new MessageFormat(pattern, getLocale());
                lastKey = key;
            } else if (key != lastKey) {
                /*
                 * Method MessageFormat.applyPattern(...) is costly! We will avoid
                 * calling it again if the format already has the right pattern.
                 */
                format.applyPattern(pattern);
                lastKey = key;
            }
            try {
                return format.format(arguments);
            } catch (RuntimeException e) {
                /*
                 * Safety against badly implemented toString() method
                 * in libraries that we don't control.
                 */
                return "[Unformattable message: " + e + ']';
            }
        }
    }

    /**
     * Gets a string for the given key and replaces all occurrences of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}, etc.
     *
     * @param  key The key for the desired string.
     * @param  arg0 Value to substitute for "{0}".
     * @param  arg1 Value to substitute for "{1}".
     * @return The formatted string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
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
     * @param  key The key for the desired string.
     * @param  arg0 Value to substitute for "{0}".
     * @param  arg1 Value to substitute for "{1}".
     * @param  arg2 Value to substitute for "{2}".
     * @return The formatted string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
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
     * @param  key The key for the desired string.
     * @param  arg0 Value to substitute for "{0}".
     * @param  arg1 Value to substitute for "{1}".
     * @param  arg2 Value to substitute for "{2}".
     * @param  arg3 Value to substitute for "{3}".
     * @return The formatted string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
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
     * @param  key The key for the desired string.
     * @param  arg0 Value to substitute for "{0}".
     * @param  arg1 Value to substitute for "{1}".
     * @param  arg2 Value to substitute for "{2}".
     * @param  arg3 Value to substitute for "{3}".
     * @param  arg4 Value to substitute for "{4}".
     * @return The formatted string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
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
     * Gets a localized log record.
     *
     * @param  level The log record level.
     * @param  key   The resource key.
     * @return The log record.
     */
    public final LogRecord getLogRecord(final Level level, final short key) {
        final LogRecord record = new LogRecord(level, getKeyConstants().getKeyName(key));
        record.setResourceBundleName(getClass().getName());
        record.setResourceBundle(this);
        return record;
    }

    /**
     * Gets a localized log record.
     *
     * @param  level The log record level.
     * @param  key   The resource key.
     * @param  arg0  The parameter for the log message, which may be an array.
     * @return The log record.
     */
    public final LogRecord getLogRecord(final Level level, final short key,
                                        final Object arg0)
    {
        final LogRecord record = getLogRecord(level, key);
        record.setParameters(toArray(arg0));
        return record;
    }

    /**
     * Gets a localized log record.
     *
     * @param  level The log record level.
     * @param  key   The resource key.
     * @param  arg0  The first parameter.
     * @param  arg1  The second parameter.
     * @return The log record.
     */
    public final LogRecord getLogRecord(final Level level, final short key,
                                        final Object arg0,
                                        final Object arg1)
    {
        return getLogRecord(level, key, new Object[] {arg0, arg1});
    }

    /**
     * Gets a localized log record.
     *
     * @param  level The log record level.
     * @param  key   The resource key.
     * @param  arg0  The first parameter.
     * @param  arg1  The second parameter.
     * @param  arg2  The third parameter.
     * @return The log record.
     */
    public final LogRecord getLogRecord(final Level level, final short key,
                                        final Object arg0,
                                        final Object arg1,
                                        final Object arg2)
    {
        return getLogRecord(level, key, new Object[] {arg0, arg1, arg2});
    }

    /**
     * Gets a localized log record.
     *
     * @param  level The log record level.
     * @param  key   The resource key.
     * @param  arg0  The first parameter.
     * @param  arg1  The second parameter.
     * @param  arg2  The third parameter.
     * @param  arg3  The fourth parameter.
     * @return The log record.
     */
    public final LogRecord getLogRecord(final Level level, final short key,
                                        final Object arg0,
                                        final Object arg1,
                                        final Object arg2,
                                        final Object arg3)
    {
        return getLogRecord(level, key, new Object[] {arg0, arg1, arg2, arg3});
    }

    /**
     * Returns the locale specified in the given map, or {@code null} if none.
     * Value of unexpected type are ignored.
     *
     * @param  properties The map of properties, or {@code null} if none.
     * @return The locale found in the given map, or {@code null} if none.
     *
     * @since 0.4
     */
    static Locale getLocale(final Map<?,?> properties) {
        if (properties != null) {
            final Object candidate = properties.get(LOCALE_KEY);
            if (candidate instanceof Locale) {
                return (Locale) candidate;
            }
        }
        return null;
    }

    /**
     * Returns a string representation of this object.
     * This method is for debugging purposes only.
     *
     * @return A string representation of this resources bundle.
     */
    @Debug
    @Override
    public synchronized String toString() {
        return getClass().getSimpleName() + '[' + getLocale() + ']';
    }
}
