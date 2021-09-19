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
package org.apache.sis.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Locale;


/**
 * An international string consisting of a single string for all locales.
 * For such a particular case, this implementation is more effective than
 * other implementations provided in this package.
 *
 * <h2>Instantiation</h2>
 * If the characters sequence to wrap is known to be a {@code String} instance, then
 * the {@link #SimpleInternationalString(String)} constructor is okay. Otherwise use
 * the {@link org.apache.sis.util.iso.Types#toInternationalString(CharSequence)} method.
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus inherently thread-safe.
 * Subclasses may or may not be immutable, at implementation choice. But implementers are
 * encouraged to make sure that subclasses remain immutable for more predictable behavior.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class SimpleInternationalString extends AbstractInternationalString implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7600371840915793379L;

    /**
     * Creates a new instance from the given string. If the type of the text
     * to wrap is the more generic {@code CharSequence} interface, then use the
     * {@link org.apache.sis.util.iso.Types#toInternationalString(CharSequence)} method instead.
     *
     * @param text the string for all locales.
     */
    public SimpleInternationalString(final String text) {
        ArgumentChecks.ensureNonNull("text", text);
        defaultValue = text;
    }

    /**
     * Returns the string representation, which is unique for all locales.
     */
    @Override
    public String toString() {
        return defaultValue;
    }

    /**
     * Returns the same string for all locales. This is the string given to the constructor.
     *
     * @param  locale  ignored in the {@code SimpleInternationalString} implementation.
     * @return the international string as a {@code String}.
     */
    @Override
    public String toString(final Locale locale) {
        return defaultValue;
    }

    /**
     * Compares this international string with the specified object for equality.
     *
     * @param  object  the object to compare with this international string.
     * @return {@code true} if the given object is equal to this string.
     */
    @Override
    @SuppressWarnings("OverlyStrongTypeCast")
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            return defaultValue.equals(((SimpleInternationalString) object).defaultValue);
        }
        return false;
    }

    /**
     * Returns a hash code value for this international text.
     *
     * @return the hash code value.
     */
    @Override
    public int hashCode() {
        return defaultValue.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Writes the string. This is required since {@link #defaultValue} is not serialized.
     *
     * @param  out  the output stream where to serialize this international string.
     * @throws IOException if an I/O error occurred while writing.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeUTF(defaultValue);
    }

    /**
     * Reads the string. This is required since {@link #defaultValue} is not serialized.
     *
     * @param  in  the input stream from which to deserialize an international string.
     * @throws IOException if an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException if the class serialized on the stream is not on the classpath.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        defaultValue = in.readUTF();
    }
}
