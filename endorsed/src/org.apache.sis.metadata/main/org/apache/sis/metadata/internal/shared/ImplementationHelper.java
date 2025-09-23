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
package org.apache.sis.metadata.internal.shared;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import org.apache.sis.xml.NilObject;
import org.apache.sis.xml.NilReason;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.IdentifiedObject;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.internal.shared.CollectionsExt;


/**
 * Miscellaneous utility methods for implementation of metadata classes.
 * Many methods in this class are related to (un)marshalling.
 * This is not a helper class for <em>usage</em> of metadata.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ImplementationHelper {
    /**
     * The root directory of ISO namespaces. Value is {@value}.
     */
    public static final String ISO_NAMESPACE = "http://standards.iso.org/iso/";

    /**
     * Do not allow instantiation of this class.
     */
    private ImplementationHelper() {
    }

    /**
     * Returns {@code true} if the given object is non-null and not an instance of {@link NilObject}.
     * This is a helper method for use in lambda expressions.
     *
     * @param  value  the value to test.
     * @return whether the given value is non-null and non-nil.
     *
     * @see Objects#nonNull(Object)
     */
    public static boolean nonNil(final Object value) {
        return (value != null) && !(value instanceof NilObject);
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
            if (!(strict ? value > 0 : value >= 0)) {                               // Use `!` for catching NaN.
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
            if (!(value >= minimum.doubleValue() && value <= maximum.doubleValue())) {      // Use `!` for catching NaN.
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
     * In the latter case, it is caller's responsibility to use the message for throwing an exception.
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
     * @param  classe  the caller class, used for logging.
     * @param  method  the caller method, used for logging.
     * @param  name    the property name, used for logging and exception message.
     * @throws IllegalStateException if we are not unmarshalling an object.
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
     * Sets the first element in the given collection to the given value.
     * Special cases:
     *
     * <ul>
     *   <li>If the given collection is null, a new collection will be returned.</li>
     *   <li>If the given new value  is null, then the first element in the collection is removed.</li>
     *   <li>Otherwise if the given collection is empty, the given value will be added to it.</li>
     * </ul>
     *
     * @param  <T>       the type of elements in the collection.
     * @param  values    the collection where to add the new value, or {@code null}.
     * @param  newValue  the new value to set, or {@code null} for instead removing the first element.
     * @return the collection (may or may not be the given {@code values} collection).
     *
     * @see org.apache.sis.util.internal.shared.CollectionsExt#first(Iterable)
     */
    public static <T> Collection<T> setFirst(Collection<T> values, final T newValue) {
        if (values == null) {
            return CollectionsExt.singletonOrEmpty(newValue);
        }
        if (newValue == null) {
            final Iterator<T> it = values.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        } else if (values.isEmpty()) {
            values.add(newValue);
        } else {
            if (!(values instanceof List<?>)) {
                values = new ArrayList<>(values);
            }
            ((List<T>) values).set(0, newValue);
        }
        return values;
    }

    /**
     * Returns the {@code gco:id} or {@code gml:id} value to use for the given object.
     * The returned identifier will be unique in the current XML document.
     *
     * @param  object  the object for which to get the unique identifier.
     * @return the unique XML identifier, or {@code null} if none.
     */
    public static String getObjectID(final IdentifiedObject object) {
        final Context context = Context.current();
        String id = Context.getObjectID(context, object);
        if (id == null) {
            id = object.getIdentifierMap().getSpecialized(IdentifierSpace.ID);
            if (id != null) {
                final StringBuilder buffer = new StringBuilder();
                if (!Strings.appendUnicodeIdentifier(buffer, (char) 0, id, ":-", false)) {
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
     */
    public static void setObjectID(final IdentifiedObject object, String id) {
        id = Strings.trimOrNull(id);
        if (id != null) {
            object.getIdentifierMap().putSpecialized(IdentifierSpace.ID, id);
            final Context context = Context.current();
            if (!Context.setObjectForID(context, object, id)) {
                Context.warningOccured(context, object.getClass(), "setID", Errors.class, Errors.Keys.DuplicatedIdentifier_1, id);
            }
        }
    }
}
