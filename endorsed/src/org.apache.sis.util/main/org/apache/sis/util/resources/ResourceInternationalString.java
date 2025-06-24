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

import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InvalidObjectException;
import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.AbstractInternationalString;


/**
 * A copy of {@link org.apache.sis.util.ResourceInternationalString} specialized for
 * {@link IndexedResourceBundle}. Compared to the public class, this specialization
 * works with integer resource keys and accepts arguments.
 *
 * <h2>Immutability and thread safety</h2>
 * This base class is immutable and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class ResourceInternationalString extends AbstractInternationalString implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3910920973710535739L;

    /**
     * The key for the resource to fetch.
     */
    private transient short key;

    /**
     * {@code true} if the key has arguments. If {@code false}, then the {@link #arguments}
     * field shall be ignored. We cannot rely on {@code null} arguments value because null
     * may be a valid value.
     */
    private final boolean hasArguments;

    /**
     * The argument(s), or {@code null} if none. Note that the user may also really want to
     * specify {@code null} as an argument value. We distinguish the two cases with the sign
     * of the {@link #key} value.
     */
    @SuppressWarnings("serial")         // Not statically typed as Serializable.
    private final Object arguments;

    /**
     * Creates a new international string for the given key.
     *
     * @param key  the key for the resource to fetch.
     */
    protected ResourceInternationalString(final short key) {
        this.key     = key;
        hasArguments = false;
        arguments    = null;
    }

    /**
     * Creates a new international string for the given key and arguments.
     *
     * @param key        the key for the resource to fetch.
     * @param arguments  the argument(s).
     */
    protected ResourceInternationalString(final short key, final Object arguments) {
        this.key          = key;
        this.hasArguments = true;
        this.arguments    = arguments;
    }

    /**
     * Returns a handler for the constants declared in the inner {@code Keys} class.
     * This is used at serialization time in order to serialize the constant name
     * rather than its numeric value.
     *
     * @return a handler for the constants declared in the inner {@code Keys} class.
     */
    protected abstract KeyConstants getKeyConstants();

    /**
     * Returns the resource bundle for the given locale.
     *
     * @param  locale  the locale for which to get the resource bundle, or {@code null} for the default locale.
     * @return the resource bundle for the given locale.
     */
    protected abstract IndexedResourceBundle getBundle(final Locale locale);

    /**
     * Converts this international string to a log record.
     *
     * @param  level  the logging level.
     * @return a log record with the message of this international string.
     */
    public final LogRecord toLogRecord(final Level level) {
        final IndexedResourceBundle resources = getBundle(null);
        final LogRecord record = resources.createLogRecord(level, key);
        if (hasArguments) {
            record.setParameters(resources.toArray(arguments));
        }
        return record;
    }

    /**
     * Returns a string in the specified locale.
     *
     * @param  locale  the desired locale for the string to be returned.
     * @return the string in the specified locale, or in a fallback locale.
     * @throws MissingResourceException if the key given to the constructor is invalid.
     */
    @Override
    public final String toString(final Locale locale) throws MissingResourceException {
        final IndexedResourceBundle resources = getBundle(locale);
        return hasArguments ? resources.getString(key, arguments) : resources.getString(key);
    }

    /**
     * Compares this international string with the specified object for equality.
     * Two {@code ResourceInternationalString} are considered equal if they are
     * of the same class and have been constructed with equal arguments.
     *
     * @param  object  the object to compare with this international string.
     * @return {@code true} if the given object is equal to this string.
     */
    @Override
    public final boolean equals(final Object object) {
        if (object == null || object.getClass() != getClass()) {
            return false;
        }
        final ResourceInternationalString that = (ResourceInternationalString) object;
        return (key == that.key) && (hasArguments == that.hasArguments) && Objects.deepEquals(arguments, that.arguments);
    }

    /**
     * Returns a hash code value for this international text.
     *
     * @return a hash code value for this international text.
     */
    @Override
    public final int hashCode() {
        return getClass().hashCode() ^ (key + 31*Utilities.deepHashCode(arguments)) ^ (int) serialVersionUID;
    }

    /**
     * Serializes this international string using the key name rather than numerical value.
     *
     * @param  out  the output stream where to serialize this object.
     * @throws IOException if an I/O error occurred while writing.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeUTF(getKeyConstants().getKeyName(key));
    }

    /**
     * Deserializes an object serialized by {@link #writeObject(ObjectOutputStream)}.
     *
     * @param  in  the input stream from which to deserialize an object.
     * @throws IOException if an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException if the class serialized on the stream is not on the module path.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        try {
            key = getKeyConstants().getKeyValue(in.readUTF());
        } catch (ReflectiveOperationException cause) {
            throw (InvalidObjectException) new InvalidObjectException(cause.toString()).initCause(cause);
        }
    }
}
