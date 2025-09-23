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
package org.apache.sis.storage.netcdf.base;

import java.util.Collection;
import org.apache.sis.math.Vector;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.storage.netcdf.internal.Resources;


/**
 * Base class of variables or groups.
 * The common characteristic of those objects is to have attributes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class Node extends NamedElement {
    /**
     * The netCDF file where this node is stored.
     */
    protected final Decoder decoder;

    /**
     * Creates a new node.
     *
     * @param decoder  the netCDF file where this node is stored.
     */
    protected Node(final Decoder decoder) {
        this.decoder = decoder;
    }

    /**
     * Returns the names of all attributes associated to this variable.
     *
     * @return names of all attributes associated to this variable.
     */
    public abstract Collection<String> getAttributeNames();

    /**
     * Returns the type of the attribute of the given name, or {@code null} if the given attribute is not found.
     * If the attribute contains more than one value, then this method returns {@code Vector.class}.
     *
     * @param  attributeName  the name of the attribute for which to get the type.
     * @return type of the given attribute, or {@code null} if the attribute does not exist.
     *
     * @see Variable#getDataType()
     */
    public abstract Class<?> getAttributeType(String attributeName);

    /**
     * Returns the single value or vector of values for the given attribute, or {@code null} if none.
     * The returned value can be an instance of:
     *
     * <ul>
     *   <li>{@link String} if the attribute contains a single textual value.</li>
     *   <li>{@link Number} if the attribute contains a single numerical value.</li>
     *   <li>{@link Vector} if the attribute contains many numerical values.</li>
     *   <li>{@code String[]} if the attribute contains many textual values.</li>
     * </ul>
     *
     * If the value is a {@code String}, then leading and trailing spaces and control characters
     * should be trimmed by {@link String#trim()}.
     *
     * @param  attributeName  the name of the attribute for which to get the values.
     * @return value(s) for the named attribute, or {@code null} if none.
     */
    protected abstract Object getAttributeValue(String attributeName);

    /**
     * Returns the value of the given attribute as a non-blank string with leading/trailing spaces removed.
     * If the attribute value is an array, this method returns a non-null value only if the array contains
     * a single value (possibly duplicated), ignoring null or empty values.
     *
     * @param  attributeName  the name of the attribute for which to get the value.
     * @return the singleton attribute value, or {@code null} if none, empty, blank or ambiguous.
     */
    public final String getAttributeAsString(final String attributeName) {
        final Object value = getAttributeValue(attributeName);
        if (value == null || value instanceof String) {
            return (String) value;
        }
        final String[] values = toArray(value);
        if (values == null) {
            return value.toString();
        }
        String singleton = null;
        for (final String c : values) {
            if (c != null) {
                if (singleton == null) {
                    singleton = c;
                } else if (!singleton.equals(c)) {
                    return null;
                }
            }
        }
        return singleton;
    }

    /**
     * Returns the values of the given attribute as an array of non-blank texts.
     * If the attribute is not stored as an array in the netCDF file, then this
     * method splits the single {@link String} value around the given separator.
     * Empty or blank strings are replaced by {@code null} values.
     *
     * <p>This method may return a direct reference to an internal array;
     * <strong>do not modify array content</strong>.</p>
     *
     * @param  attributeName  the name of the attribute for which to get the values.
     * @param  separator      separator to use for splitting a single {@link String} value into a list of values.
     * @return the attribute values, or {@code null} if none. The array may contain {@code null} elements.
     */
    public final CharSequence[] getAttributeAsStrings(final String attributeName, final char separator) {
        final Object value = getAttributeValue(attributeName);
        if (value != null) {
            CharSequence[] ts = toArray(value);
            if (ts == null) {
                ts = CharSequences.split(value.toString(), separator);
            }
            if (ts.length != 0) {
                return ts;
            }
        }
        return null;
    }

    /**
     * Converts the given value into an array of strings, or returns {@code null}
     * if the given value is not an array or a vector or contains only null values.
     * The returned array contains no blank strings, but may contain null values.
     *
     * <p>This method may return a direct reference to an internal array;
     * <strong>do not modify array content</strong>.</p>
     */
    private static String[] toArray(final Object value) {
        final String[] array;
        if (value instanceof String[]) {
            // Empty strings are already replaced by null values.
            return (String[]) value;
        } else if (value instanceof Object[]) {
            final Object[] values = (Object[]) value;
            array = new String[values.length];
            for (int i=0; i<array.length; i++) {
                final Object e = values[i];
                if (e != null) {
                    array[i] = e.toString();
                }
            }
        } else if (value instanceof Vector) {
            final Vector values = (Vector) value;
            array = new String[values.size()];
            for (int i=0; i<array.length; i++) {
                array[i] = values.stringValue(i);
            }
        } else {
            return null;
        }
        boolean hasValues = false;
        for (int i=0; i<array.length; i++) {
            hasValues |= (array[i] = Strings.trimOrNull(array[i])) != null;
        }
        return hasValues ? array : null;
    }

    /**
     * Returns the value of the given attribute as a number, or {@code null}.
     * This method returns a number of the type that most closely matches the
     * type in the netCDF file.
     *
     * @param  attributeName  the name of the attribute for which to get the value.
     * @return the singleton attribute value, or {@code null} if none or ambiguous.
     */
    public final Number getAttributeAsNumber(final String attributeName) {
        Number singleton = null;
        final Object value = getAttributeValue(attributeName);
        if (value instanceof Number) {
            singleton = (Number) value;
        } else if (value instanceof String) {
            singleton = decoder.parseNumber(attributeName, (String) value);
        } else if (value instanceof Vector) {
            final Vector data = (Vector) value;
            final int length = data.size();
            for (int i=0; i<length; i++) {
                final Number n = data.get(i);
                if (n != null) {
                    if (singleton == null) {
                        singleton = n;
                    } else if (!singleton.equals(n)) {
                        return null;
                    }
                }
            }
        }
        return singleton;
    }

    /**
     * Returns the value of the given attribute as a number, or {@link Double#NaN}.
     * If the number is stored with single-precision, it is assumed cast from a
     * representation in base 10.
     *
     * @param  attributeName  the name of the attribute for which to get the value.
     * @return the singleton attribute value, or {@code NaN} if none or ambiguous.
     */
    public final double getAttributeAsDouble(final String attributeName) {
        Number singleton = getAttributeAsNumber(attributeName);
        if (singleton == null) {
            return Double.NaN;
        }
        double dp = singleton.doubleValue();
        final float sp = (float) dp;
        if (sp == dp) {                                     // May happen even if the number was stored as a double.
            dp = DecimalFunctions.floatToDouble(sp);
        }
        return dp;
    }

    /**
     * Returns the values of the given attribute as a vector of numbers, or {@code null} if none.
     * If the numbers are stored with single-precision, they are assumed cast from a representation in base 10.
     *
     * @param  attributeName  the name of the attribute for which to get the values.
     * @return the attribute values, or {@code null} if none, ambiguous or not a vector.
     */
    public final Vector getAttributeAsVector(final String attributeName) {
        final Object value = getAttributeValue(attributeName);
        if (value instanceof Vector) {
            return (Vector) value;
        } else if (value instanceof Float) {
            return Vector.createForDecimal(new float[] {(Float) value});
        } else if (value instanceof Number) {
            return Vector.create(new Number[] {(Number) value}, false);
        } else {
            return null;
        }
    }

    /**
     * Returns the resources to use for error messages.
     *
     * @return the resources for error messages using the locales specified to the decoder.
     */
    final Errors errors() {
        return Errors.forLocale(decoder.getLocale());
    }

    /**
     * Reports a warning to the listeners specified at construction time.
     * This method is for Apache SIS internal purpose only since resources may change at any time.
     *
     * @param  caller     the caller class to report, preferably a public class.
     * @param  method     the caller method to report, preferable a public method.
     * @param  exception  the exception that occurred, or {@code null} if none.
     * @param  key        one or {@link Resources.Keys} constants.
     * @param  arguments  values to be formatted in the {@link java.text.MessageFormat} pattern.
     */
    protected final void warning(final Class<?> caller, final String method, final Exception exception, final short key, final Object... arguments) {
        warning(decoder.listeners, caller, method, null, null, key, arguments);
    }

    /**
     * Reports a warning to the listeners specified at construction time.
     *
     * @param  caller     the caller class to report, preferably a public class.
     * @param  method     the caller method to report, preferable a public method.
     * @param  exception  the exception that occurred, or {@code null} if none.
     * @param  key        one or {@link Errors.Keys} constants.
     * @param  arguments  values to be formatted in the {@link java.text.MessageFormat} pattern.
     */
    protected final void error(final Class<?> caller, final String method, final Exception exception, final short key, final Object... arguments) {
        warning(decoder.listeners, caller, method, exception, errors(), key, arguments);
    }

    /*
     * Do not override `equals(Object)` and `hashCode()`. Some subclasses are
     * used in `HashSet` and the identity comparison is well suited for them.
     * For example, `Variable` is used as keys in `GridMapping.forVariable(…)`.
     */
}
