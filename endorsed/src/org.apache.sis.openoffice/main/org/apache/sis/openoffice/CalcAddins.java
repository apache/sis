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
package org.apache.sis.openoffice;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import com.sun.star.lang.Locale;
import com.sun.star.lang.XLocalizable;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XServiceName;
import com.sun.star.uno.XComponentContext;
import com.sun.star.lib.uno.helper.WeakBase;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Base class for methods to export as formulas in the Apache OpenOffice spread sheet.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.8
 * @since   0.8
 */
public abstract class CalcAddins extends WeakBase implements XServiceName, XServiceInfo, XLocalizable {
    /**
     * Indirectly provides access to the service manager.
     * For example, {@code com.sun.star.sdb.DatabaseContext} holds databases registered with OpenOffice.
     */
    protected final XComponentContext context;

    /**
     * Locale attribute required by {@code com.sun.star.lang.XLocalizable} interface.
     */
    private Locale locale;

    /**
     * The locale as an object from the standard Java SDK.
     * Will be fetched only when first needed.
     */
    private transient java.util.Locale javaLocale;

    /**
     * The logger, fetched when first needed.
     */
    private transient Logger logger;

    /**
     * Constructs add-ins for Calc.
     *
     * @param context  the value to assign to the {@link #context} field.
     */
    protected CalcAddins(final XComponentContext context) {
        this.context = context;
    }

    /**
     * The service name that can be used to create such an object by a factory.
     *
     * @return the service name.
     */
    @Override
    public abstract String getServiceName();

    /**
     * Provides the supported service names of the implementation, including also indirect service names.
     *
     * @return sequence of service names that are supported.
     */
    @Override
    public final String[] getSupportedServiceNames() {
        return new String[] {getServiceName()};
    }

    /**
     * Tests whether the specified service is supported, i.e. implemented by the implementation.
     *
     * @param  name  name of service to be tested.
     * @return {@code true} if the service is supported, {@code false} otherwise.
     */
    @Override
    public final boolean supportsService(final String name) {
        return name.equals(getServiceName());
    }

    /**
     * Sets the locale to be used by this object.
     *
     * @param locale the new locale.
     */
    @Override
    public final synchronized void setLocale(final Locale locale) {
        this.locale = locale;
        javaLocale = null;
    }

    /**
     * Returns the current locale used by this instance.
     *
     * @return the current locale.
     */
    @Override
    public final synchronized Locale getLocale() {
        if (locale == null) {
            locale = new Locale();
        }
        return locale;
    }

    /**
     * Returns the locale as an object from the Java standard SDK.
     *
     * @return the current locale.
     */
    protected final synchronized java.util.Locale getJavaLocale() {
        if (javaLocale == null) {
            if (locale != null) {
                String language = locale.Language; if (language == null) language = "";
                String country  = locale.Country;  if (country  == null) country  = "";
                String variant  = locale.Variant;  if (variant  == null) variant  = "";
                javaLocale = new java.util.Locale(language, country, variant);
            } else {
                javaLocale = java.util.Locale.getDefault();
            }
        }
        return javaLocale;
    }

    /**
     * The string to return when a formula does not have any value to return.
     *
     * @return the string with a message for missing values.
     */
    final String noResultString() {
        return Vocabulary.forLocale(getJavaLocale()).getString(Vocabulary.Keys.NotKnown);
    }

    /**
     * Returns the minimal length of the specified arrays. In the special case where one array
     * has a length of 1, we assume that this single element will be repeated for all elements
     * in the other array.
     */
    static int getMinimalLength(final Object[] array1, final Object[] array2) {
        if (array1 == null || array2 == null) {
            return 0;
        }
        if (array1.length == 1) return array2.length;
        if (array2.length == 1) return array1.length;
        return Math.min(array1.length, array2.length);
    }

    /**
     * Returns the localized message from the specified exception. If no message is available,
     * returns a default string. This method never return a null value.
     *
     * @param  exception  the exception for which to get the localized message.
     * @return an error message to report to the user.
     */
    protected final String getLocalizedMessage(final Throwable exception) {
        final String message = Exceptions.getLocalizedMessage(exception, getJavaLocale());
        if (message != null) {
            return message;
        }
        return Classes.getShortClassName(exception);
    }

    /**
     * Reports an exception. This is used if an exception occurred in a method that cannot return
     * a {@link String} instance. This method logs the stack trace at {@link Level#WARNING}.
     *
     * @param method     the method from which an exception occurred.
     * @param exception  the exception.
     */
    final void reportException(final String method, final Exception exception) {
        final Logger logger = getLogger();
        final LogRecord record = new LogRecord(Level.WARNING, getLocalizedMessage(exception));
        record.setLoggerName(logger.getName());
        record.setSourceClassName(getClass().getCanonicalName());
        record.setSourceMethodName(method);
        record.setThrown(exception);
        logger.log(record);
    }

    /**
     * Returns the logger to use for logging warnings.
     *
     * @return the logger to use.
     */
    protected final synchronized Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(Registration.LOGGER);
        }
        return logger;
    }
}
