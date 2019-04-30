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
package org.apache.sis.internal.netcdf;

import java.util.Locale;
import java.util.Collection;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.util.resources.Errors;


/**
 * Base class of variables or groups.
 * The common characteristic of those objects is to have attributes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
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
     * Returns the type of the attribute of the given name,
     * or {@code null} if the given attribute is not found.
     *
     * @param  attributeName  the name of the attribute for which to get the type.
     * @return type of the given attribute, or {@code null} if the attribute does not exist.
     *
     * @see Variable#getDataType()
     */
    public abstract Class<?> getAttributeType(String attributeName);

    /**
     * Returns the sequence of values for the given attribute, or an empty array if none.
     * The elements will be of class {@link String} if {@code numeric} is {@code false},
     * or {@link Number} if {@code numeric} is {@code true}. Some elements may be null
     * if they are not of the expected type.
     *
     * @param  attributeName  the name of the attribute for which to get the values.
     * @param  numeric        {@code true} if the values are expected to be numeric, or {@code false} for strings.
     * @return the sequence of {@link String} or {@link Number} values for the named attribute.
     *         May contain null elements.
     */
    public abstract Object[] getAttributeValues(String attributeName, boolean numeric);

    /**
     * Returns the singleton value for the given attribute, or {@code null} if none or ambiguous.
     *
     * @param  attributeName  the name of the attribute for which to get the value.
     * @param  numeric        {@code true} if the value is expected to be numeric, or {@code false} for string.
     * @return the {@link String} or {@link Number} value for the named attribute.
     */
    private Object getAttributeValue(final String attributeName, final boolean numeric) {
        Object singleton = null;
        for (final Object value : getAttributeValues(attributeName, numeric)) {
            if (value != null) {
                if (singleton != null && !singleton.equals(value)) {              // Paranoiac check.
                    return null;
                }
                singleton = value;
            }
        }
        return singleton;
    }

    /**
     * Returns the value of the given attribute as a non-blank string with leading/trailing spaces removed.
     * This is a convenience method for {@link #getAttributeValues(String, boolean)} when a singleton value
     * is expected and blank strings ignored.
     *
     * @param  attributeName  the name of the attribute for which to get the value.
     * @return the singleton attribute value, or {@code null} if none, empty, blank or ambiguous.
     */
    public String getAttributeAsString(final String attributeName) {
        final Object value = getAttributeValue(attributeName, false);
        if (value != null) {
            final String text = value.toString().trim();
            if (!text.isEmpty()) return text;
        }
        return null;
    }

    /**
     * Returns the value of the given attribute as a number, or {@link Double#NaN}.
     * If the number is stored with single-precision, it is assumed casted from a
     * representation in base 10.
     *
     * @param  attributeName  the name of the attribute for which to get the value.
     * @return the singleton attribute value, or {@code NaN} if none or ambiguous.
     */
    public final double getAttributeAsNumber(final String attributeName) {
        final Object value = getAttributeValue(attributeName, true);
        if (value instanceof Number) {
            double dp = ((Number) value).doubleValue();
            final float sp = (float) dp;
            if (sp == dp) {                              // May happen even if the number was stored as a double.
                dp = DecimalFunctions.floatToDouble(sp);
            }
            return dp;
        }
        return Double.NaN;
    }

    /**
     * Returns the locale to use for warnings and error messages.
     *
     * @return the locale for warnings and error messages.
     */
    protected final Locale getLocale() {
        return decoder.listeners.getLocale();
    }

    /**
     * Returns the resources to use for warnings or error messages.
     *
     * @return the resources for the locales specified to the decoder.
     */
    protected final Resources resources() {
        return Resources.forLocale(getLocale());
    }

    /**
     * Returns the resources to use for error messages.
     *
     * @return the resources for error messages using the locales specified to the decoder.
     */
    final Errors errors() {
        return Errors.getResources(getLocale());
    }

    /**
     * Reports a warning to the listeners specified at construction time.
     * This method is for Apache SIS internal purpose only since resources may change at any time.
     *
     * @param  caller     the caller class to report, preferably a public class.
     * @param  method     the caller method to report, preferable a public method.
     * @param  key        one or {@link Resources.Keys} constants.
     * @param  arguments  values to be formatted in the {@link java.text.MessageFormat} pattern.
     */
    protected final void warning(final Class<?> caller, final String method, final short key, final Object... arguments) {
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
    final void error(final Class<?> caller, final String method, final Exception exception, final short key, final Object... arguments) {
        warning(decoder.listeners, caller, method, exception, errors(), key, arguments);
    }
}
