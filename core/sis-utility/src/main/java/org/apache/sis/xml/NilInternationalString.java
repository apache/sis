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
package org.apache.sis.xml;

import java.util.Locale;
import java.io.Serializable;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.Errors;


/**
 * An empty {@link InternationalString} which is nil for the given reason.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
final class NilInternationalString implements InternationalString, NilObject, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4275403933320692351L;

    /**
     * The reason why the object is nil.
     */
    private final NilReason reason;

    /**
     * Creates a new international string which is nil for the given reason.
     */
    NilInternationalString(final NilReason reason) {
        this.reason = reason;
    }

    /**
     * Returns the reason why this object is nil.
     */
    @Override
    public NilReason getNilReason() {
        return reason;
    }

    /**
     * Returns the length, which is always 0.
     */
    @Override
    public int length() {
        return 0;
    }

    /**
     * Unconditionally throws {@link IndexOutOfBoundsException},
     * since we can not get any character from an empty string.
     */
    @Override
    public char charAt(final int index) {
        throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, index));
    }

    /**
     * Unconditionally returns en empty string.
     */
    @Override
    public String toString() {
        return "";
    }

    /**
     * Unconditionally returns en empty string.
     */
    @Override
    public String toString(final Locale locale) {
        return "";
    }

    /**
     * Returns {@code this} if the range is {@code [0…0]}, or throws an exception otherwise.
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        if (start == 0 && end == 0) {
            return this;
        }
        throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IllegalRange_2, start, end));
    }

    /**
     * Returns 0 if the other string is empty, or -1 otherwise.
     */
    @Override
    public int compareTo(final InternationalString other) {
        return other.length() == 0 ? 0 : -1;
    }

    /*
     * Do not override equals and hashCode. It is okay to keep the reference-equality semantic
     * because all NilInternationalString instances are uniques in the running JVM.
     */

    /**
     * Invoked on deserialization for replacing the deserialized instance by the unique instance.
     */
    private Object readResolve() {
        return reason.createNilObject(InternationalString.class);
    }
}
