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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import net.jcip.annotations.ThreadSafe;

import org.opengis.util.InternationalString;

import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.logging.Logging;

import static org.apache.sis.util.Arrays.resize;


/**
 * {@link ResourceBundle} implementation accepting integers instead of strings for resource keys.
 * Using integers allow implementations to avoid adding large string constants into their
 * {@code .class} files and runtime images. Developers still have meaningful labels in their
 * code (e.g. {@code DimensionMismatch}) through a set of constants defined in {@code Keys}
 * inner classes, with the side-effect of compile-time safety. Because integer constants are
 * inlined right into class files at compile time, the declarative classes is never loaded at
 * run time.
 *
 * <p>This class also provides facilities for string formatting using {@link MessageFormat}.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-1.2)
 * @version 0.3
 * @module
 */
@ThreadSafe
public class IndexedResourceBundle extends ResourceBundle {
    /**
     * Maximum string length for text inserted into another text. This parameter is used by
     * {@link #summarize}. Resource strings are never cut to this length. However, text replacing
     * {@code "{0}"} in a string like {@code "Parameter name is {0}"} will be cut to this length.
     */
    private static final int MAX_STRING_LENGTH = 200;

    /**
     * The resource name of the binary file containing resources. It may be a file name or an
     * entry in a JAR file. The path must be relative to the package containing the subclass
     * of {@code IndexedResourceBundle}.
     */
    private final String filename;

    /**
     * The key names. This is usually not needed, but may be created from the {@code Keys}
     * inner class in some occasions.
     *
     * @see #getKeyNames()
     * @see #getKeyName(int)
     */
    private transient String[] keys;

    /**
     * The array of resources. Keys are an array index. For example, the value for key "14" is
     * {@code values[14]}. This array will be loaded only when first needed. We should not load
     * it at construction time, because some {@code ResourceBundle} objects will never ask for
     * values. This is particularly the case for ancestor classes of {@code Resources_fr_CA},
     * {@code Resources_en}, {@code Resources_de}, etc., which will only be used if a key has
     * not been found in the subclass.
     *
     * @see #ensureLoaded(String)
     */
    private String[] values;

    /**
     * The locale for formatting objects like number, date, etc. There are two possible Locales
     * we could use: default locale or resource bundle locale. If the default locale uses the same
     * language as this ResourceBundle's locale, then we will use the default locale. This allows
     * dates and numbers to be formatted according to user conventions (e.g. French Canada) even
     * if the ResourceBundle locale is different (e.g. standard French). However, if languages
     * don't match, then we will use ResourceBundle locale for better coherence.
     */
    private transient Locale formatLocale;

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
    private transient int lastKey;

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param filename The file or the JAR entry containing resources. The path must be relative
     *        to the package of the {@code IndexedResourceBundle} subclass being constructed.
     */
    protected IndexedResourceBundle(final String filename) {
        this.filename = filename;
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
     * Returns the locale to use for formatters. It is often the same than {@link #getLocale()},
     * except if the later has the same language than the default locale, in which case this
     * method returns the default locale. For example if this {@code IndexResourceBundle} is
     * for the French locale but the user is French Canadian, we will format the dates using
     * Canada French conventions rather than France conventions.
     */
    private Locale getFormatLocale() {
        if (formatLocale == null) {
            formatLocale = Locale.getDefault();
            final Locale rl = getLocale(); // Sometime null with IBM's JDK.
            if (rl != null && !formatLocale.getLanguage().equalsIgnoreCase(rl.getLanguage())) {
                formatLocale = rl;
            }
        }
        return formatLocale;
    }

    /**
     * Returns the inner {@code Keys} class which declare the key constants.
     * Subclasses defined in the {@code org.apache.sis.util.resources} package
     * override this method for efficiency. However the default implementation
     * should work for other cases (we don't want to expose too much internal API).
     *
     * @return The inner {@code Keys} class.
     * @throws ClassNotFoundException If the inner class has not been found.
     */
    Class<?> getKeysClass() throws ClassNotFoundException {
        for (final Class<?> inner : getClass().getClasses()) {
            if ("Keys".equals(inner.getSimpleName())) {
                return inner;
            }
        }
        throw new ClassNotFoundException();
    }

    /**
     * Returns the internal array of key names. <strong>Do not modify the returned array.</strong>
     * This method should usually not be invoked, in order to avoid loading the inner Keys class.
     * The keys names are used only in rare situation, like {@link #list(Writer)} or in log records.
     */
    private synchronized String[] getKeyNames() {
        if (keys == null) {
            String[] names;
            int length = 0;
            try {
                final Field[] fields = getKeysClass().getFields();
                names = new String[fields.length];
                for (final Field field : fields) {
                    if (Modifier.isStatic(field.getModifiers()) && field.getType() == Integer.TYPE) {
                        final int index = (Integer) field.get(null);
                        if (index >= length) {
                            length = index + 1;
                            if (length > names.length) {
                                // Usually don't happen, except for incomplete bundles.
                                names = Arrays.copyOf(names, length*2);
                            }
                        }
                        names[index] = field.getName();
                    }
                }
            } catch (Exception e) {
                names = CharSequences.EMPTY_ARRAY;
            }
            keys = resize(names, length);
        }
        return keys;
    }

    /**
     * Returns an enumeration of the keys.
     *
     * @return All keys in this resource bundle.
     */
    @Override
    public final Enumeration<String> getKeys() {
        return new KeyEnum(getKeyNames());
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
     * Returns the name of the key at the given index. If there is no name at that given
     * index, format the index as a decimal number. Those decimal numbers are parsed by
     * our {@link #handleGetObject(String)} implementation.
     */
    private String getKeyNameAt(final int index) {
        final String[] keys = getKeyNames();
        if (index < keys.length) {
            final String key = keys[index];
            if (key != null) {
                return key;
            }
        }
        return String.valueOf(index);
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
        final String[] keys = getKeyNames();
        for (final String key : keys) {
            if (key != null) {
                keyLength = Math.max(keyLength, key.length());
            }
        }
        final String lineSeparator = System.getProperty("line.separator", "\n");
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
        final String methodName = (key != null) ? "getObject" : "getKeys";
        LogRecord record = null;
        try {
            String[] values;
            synchronized (this) {
                values = this.values;
                if (values != null) {
                    return values;
                }
                /*
                 * Prepares a log record.  We will wait for successful loading before
                 * posting this record.  If loading fails, the record will be changed
                 * into an error record. Note that the message must be logged outside
                 * the synchronized block, otherwise there is dead locks!
                 */
                record = new LogRecord(Level.FINER, "Loaded resources for {0} from bundle \"{1}\".");
                /*
                 * Loads resources from the UTF file.
                 */
                InputStream in;
                String name = filename;
                while ((in = getClass().getResourceAsStream(name)) == null) { // NOSONAR
                    final int ext  = name.lastIndexOf('.');
                    final int lang = name.lastIndexOf('_', ext-1);
                    if (lang <= 0) {
                        throw new FileNotFoundException(filename);
                    }
                    final int length = name.length();
                    name = new StringBuilder(lang + (length-ext))
                            .append(name, 0, lang).append(name, ext, length).toString();
                }
                final DataInputStream input = new DataInputStream(new BufferedInputStream(in));
                try {
                    this.values = values = new String[input.readInt()];
                    for (int i=0; i<values.length; i++) {
                        values[i] = input.readUTF();
                        if (values[i].isEmpty()) {
                            values[i] = null;
                        }
                    }
                } finally {
                    input.close();
                }
                /*
                 * Now, logs the message. This message is not localized.  Note that
                 * Locale.getDisplayName() may return different string on different
                 * Java implementation, but it doesn't matter here since we use the
                 * result only for logging purpose.
                 */
                String language = null;
                final Locale rl = getLocale(); // Sometime null with IBM's JDK.
                if (rl != null) {
                    language = rl.getDisplayName(Locale.US);
                }
                if (language == null || language.isEmpty()) {
                    language = "<default>";
                }
                record.setParameters(new String[] {language, getClass().getCanonicalName()});
            }
            Logging.log(IndexedResourceBundle.class, methodName, record);
            return values;
        } catch (IOException exception) {
            record.setLevel  (Level.WARNING);
            record.setMessage(exception.getLocalizedMessage());
            record.setThrown (exception);
            Logging.log(IndexedResourceBundle.class, methodName, record);
            final MissingResourceException error = new MissingResourceException(
                    exception.getLocalizedMessage(), getClass().getCanonicalName(), key);
            error.initCause(exception);
            throw error;
        }
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
        int keyID;
        try {
            keyID = Integer.parseInt(key);
        } catch (NumberFormatException exception) {
            /*
             * Maybe the full key name has been specified instead. We do that for localized
             * LogRecords, for easier debugging if the message has not been properly formatted.
             */
            try {
                keyID = (Integer) getKeysClass().getField(key).get(null);
            } catch (Exception e) {
                Logging.recoverableException(getClass(), "handleGetObject", e);
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
     * {@link Throwable} or {@link Class} instances. All {@code InternationalString} instances will
     * be localized according this resource bundle locale. Any characters sequences of length
     * greater than {@link #MAX_STRING_LENGTH} will be reduced using the
     * {@link CharSequences#shortSentence(CharSequence, int)} method.</p>
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
                    text = ((InternationalString) element).toString(getFormatLocale());
                }
                replacement = CharSequences.shortSentence(text, MAX_STRING_LENGTH);
            } else if (element instanceof Throwable) {
                String message = ((Throwable) element).getLocalizedMessage();
                if (message == null) {
                    message = Classes.getShortClassName(element);
                }
                replacement = message;
            } else if (element instanceof Class<?>) {
                replacement = Classes.getShortName((Class<?>) element);
            }
            // No need to check for Numbers or Dates instances, since they are
            // properly formatted in the ResourceBundle locale by MessageFormat.
            if (replacement != element) {
                if (array == arguments) {
                    array = array.clone(); // Protect the user-provided array from change.
                }
                array[i] = replacement;
            }
        }
        return array;
    }

    /**
     * Gets a string for the given key and appends "…" to it.
     * This method is typically used for creating menu items.
     *
     * @param  key The key for the desired string.
     * @return The string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public final String getMenuLabel(final int key) throws MissingResourceException {
        return getString(key) + '…';
    }

    /**
     * Gets a string for the given key and appends ": " to it.
     * This method is typically used for creating labels.
     *
     * @param  key The key for the desired string.
     * @return The string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public final String getLabel(final int key) throws MissingResourceException {
        return getString(key) + ": ";
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its parents.
     *
     * @param  key The key for the desired string.
     * @return The string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public final String getString(final int key) throws MissingResourceException {
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
     * @see #getString(int,Object,Object)
     * @see #getString(int,Object,Object,Object)
     * @see MessageFormat
     */
    public final String getString(final int key, final Object arg0) throws MissingResourceException {
        final String pattern = getString(key);
        final Object[] arguments = toArray(arg0);
        synchronized (this) {
            if (format == null) {
                /*
                 * Constructs a new MessageFormat for formatting the arguments.
                 */
                format = new MessageFormat(pattern, getFormatLocale());
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
    public final String getString(final int    key,
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
    public final String getString(final int    key,
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
    public final String getString(final int    key,
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
    public final String getString(final int    key,
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
    public final LogRecord getLogRecord(final Level level, final int key) {
        final LogRecord record = new LogRecord(level, getKeyNameAt(key));
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
    public final LogRecord getLogRecord(final Level level, final int key,
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
    public final LogRecord getLogRecord(final Level level, final int key,
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
    public final LogRecord getLogRecord(final Level level, final int key,
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
    public final LogRecord getLogRecord(final Level level, final int key,
                                        final Object arg0,
                                        final Object arg1,
                                        final Object arg2,
                                        final Object arg3)
    {
        return getLogRecord(level, key, new Object[] {arg0, arg1, arg2, arg3});
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
