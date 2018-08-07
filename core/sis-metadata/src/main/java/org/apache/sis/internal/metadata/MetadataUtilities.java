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
package org.apache.sis.internal.metadata;

import java.util.Date;
import java.util.Collection;
import org.apache.sis.xml.NilReason;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.IdentifiedObject;
import org.apache.sis.util.Static;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.util.Utilities;


/**
 * Miscellaneous utility methods for metadata.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 *
 * @todo Most methods in this class are related to (un)marshalling. We should rename as {@code MarshallingUtilities}
 *       and move to a JAXB package after we removed the {@code toMilliseconds(…)} and {@code toDate(…)} methods.
 */
public final class MetadataUtilities extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private MetadataUtilities() {
    }

    /**
     * Returns the milliseconds value of the given date, or {@link Long#MIN_VALUE}
     * if the date us null.
     *
     * @param  value  the date, or {@code null}.
     * @return the time in milliseconds, or {@code Long.MIN_VALUE} if none.
     */
    public static long toMilliseconds(final Date value) {
        return (value != null) ? value.getTime() : Long.MIN_VALUE;
    }

    /**
     * Converts the given milliseconds time to a date object, or returns null
     * if the given time is {@link Long#MIN_VALUE}.
     *
     * @param  value  the time in milliseconds.
     * @return the date for the given milliseconds value, or {@code null}.
     */
    public static Date toDate(final long value) {
        return (value != Long.MIN_VALUE) ? new Date(value) : null;
    }

    /**
     * Returns the given collection if non-null and non-empty, or {@code null} otherwise.
     * This method is used for calls to {@code checkWritePermission(Object)}.
     *
     * @param  value  the collection.
     * @return the given collection if non-empty, or {@code null} otherwise.
     */
    public static Collection<?> valueIfDefined(final Collection<?> value) {
        return (value == null) || value.isEmpty() ? null : value;
    }

    /**
     * Ensures that the given property value is positive. If the user gave a negative value or (in some case) zero,
     * then this method logs a warning if we are in process of (un)marshalling a XML document or throw an exception
     * otherwise.
     *
     * @param  classe    the class which invoke this method.
     * @param  property  the property name. Method name will be inferred by the usual Java bean convention.
     * @param  strict    {@code true} if the value was expected to be strictly positive, or {@code false} if 0 is accepted.
     * @param  newValue  the argument value to verify.
     * @return {@code true} if the value is valid.
     * @throws IllegalArgumentException if the given value is negative and the problem has not been logged.
     */
    public static boolean ensurePositive(final Class<?> classe,
            final String property, final boolean strict, final Number newValue) throws IllegalArgumentException
    {
        if (newValue != null) {
            final double value = newValue.doubleValue();
            if (!(strict ? value > 0 : value >= 0)) {                               // Use '!' for catching NaN.
                if (NilReason.forObject(newValue) == null) {
                    final String msg = logOrFormat(classe, property, strict
                            ? Errors.Keys.ValueNotGreaterThanZero_2
                            : Errors.Keys.NegativeArgument_2, property, newValue);
                    if (msg != null) {
                        throw new IllegalArgumentException(msg);
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Ensures that the given argument is either null or between the given minimum and maximum values.
     * If the user argument is outside the expected range of values, then this method logs a warning
     * if we are in process of (un)marshalling a XML document or throw an exception otherwise.
     *
     * @param  classe    the class which invoke this method.
     * @param  property  name of the property to check.
     * @param  minimum   the minimal legal value.
     * @param  maximum   the maximal legal value.
     * @param  newValue  the value given by the user.
     * @return {@code true} if the value is valid.
     * @throws IllegalArgumentException if the given value is out of range and the problem has not been logged.
     */
    public static boolean ensureInRange(final Class<?> classe, final String property,
            final Number minimum, final Number maximum, final Number newValue)
            throws IllegalArgumentException
    {
        if (newValue != null) {
            final double value = newValue.doubleValue();
            if (!(value >= minimum.doubleValue() && value <= maximum.doubleValue())) {      // Use '!' for catching NaN.
                if (NilReason.forObject(newValue) == null) {
                    final String msg = logOrFormat(classe, property,
                            Errors.Keys.ValueOutOfRange_4, property, minimum, maximum, newValue);
                    if (msg != null) {
                        throw new IllegalArgumentException(msg);
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Formats an error message and logs it if we are (un)marshalling a document, or return the message otherwise.
     * In the later case, it is caller's responsibility to use the message for throwing an exception.
     *
     * @param  classe     the caller class, used only in case of warning message to log.
     * @param  property   the property name. Method name will be inferred by the usual Java bean convention.
     * @param  key        an {@code Errors.Keys} value.
     * @param  arguments  the argument to use for formatting the error message.
     * @return {@code null} if the message has been logged, or the message to put in an exception otherwise.
     */
    private static String logOrFormat(final Class<?> classe, final String property, final short key, final Object... arguments) {
        final Context context = Context.current();
        if (context == null) {
            return Errors.format(key, arguments);
        } else {
            final StringBuilder buffer = new StringBuilder(property.length() + 3).append("set").append(property);
            buffer.setCharAt(3, Character.toUpperCase(buffer.charAt(3)));
            Context.warningOccured(context, classe, buffer.toString(), Errors.class, key, arguments);
            return null;
        }
    }

    /**
     * Invoked by private setter methods (themselves invoked by JAXB at unmarshalling time)
     * when an element is already set. Invoking this method from those setter methods serves
     * three purposes:
     *
     * <ul>
     *   <li>Make sure that a singleton property is not defined twice in the XML document.</li>
     *   <li>Protect ourselves against changes in immutable objects outside unmarshalling. It should
     *       not be necessary since the setter methods shall not be public, but we are paranoiac.</li>
     *   <li>Be a central point where we can trace all setter methods, in case we want to improve
     *       warning or error messages in future SIS versions.</li>
     * </ul>
     *
     * @param  classe  the caller class, used only in case of warning message to log.
     * @param  method  the caller method, used only in case of warning message to log.
     * @param  name    the property name, used only in case of error message to format.
     * @throws IllegalStateException if {@code isDefined} is {@code true} and we are not unmarshalling an object.
     *
     * @since 0.7
     */
    public static void propertyAlreadySet(final Class<?> classe, final String method, final String name)
            throws IllegalStateException
    {
        final Context context = Context.current();
        if (context != null) {
            Context.warningOccured(context, classe, method, Errors.class, Errors.Keys.ElementAlreadyPresent_1, name);
        } else {
            throw new IllegalStateException(Errors.format(Errors.Keys.ElementAlreadyPresent_1, name));
        }
    }

    /**
     * Returns the {@code gco:id} or {@code gml:id} value to use for the given object.
     * The returned identifier will be unique in the current XML document.
     *
     * @param  object  the object for which to get the unique identifier.
     * @return the unique XML identifier, or {@code null} if none.
     *
     * @since 0.7
     */
    public static String getObjectID(final IdentifiedObject object) {
        final Context context = Context.current();
        String id = Context.getObjectID(context, object);
        if (id == null) {
            id = object.getIdentifierMap().getSpecialized(IdentifierSpace.ID);
            if (id != null) {
                final StringBuilder buffer = new StringBuilder();
                if (!Utilities.appendUnicodeIdentifier(buffer, (char) 0, id, ":-", false)) {
                    return null;
                }
                id = buffer.toString();
                if (!Context.setObjectForID(context, object, id)) {
                    final int s = buffer.append('-').length();
                    int n = 0;
                    do {
                        if (++n == 100) return null;                        //  Arbitrary limit.
                        id = buffer.append(n).toString();
                        buffer.setLength(s);
                    } while (!Context.setObjectForID(context, object, id));
                }
            }
        }
        return id;
    }

    /**
     * Invoked by {@code setID(String)} method implementations for assigning an identifier to an object
     * at unmarshalling time.
     *
     * @param object  the object for which to assign an identifier.
     * @param id      the {@code gco:id} or {@code gml:id} value.
     *
     * @since 0.7
     */
    public static void setObjectID(final IdentifiedObject object, String id) {
        id = CharSequences.trimWhitespaces(id);
        if (id != null && !id.isEmpty()) {
            object.getIdentifierMap().putSpecialized(IdentifierSpace.ID, id);
            final Context context = Context.current();
            if (!Context.setObjectForID(context, object, id)) {
                Context.warningOccured(context, object.getClass(), "setID", Errors.class, Errors.Keys.DuplicatedIdentifier_1, id);
            }
        }
    }
}
