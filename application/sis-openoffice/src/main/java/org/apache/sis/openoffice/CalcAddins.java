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

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

import com.sun.star.sheet.XAddIn;
import com.sun.star.util.Date;
import com.sun.star.lang.Locale;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XServiceName;
import com.sun.star.beans.XPropertySet;
import com.sun.star.uno.AnyConverter;
import com.sun.star.lib.uno.helper.WeakBase;

import org.apache.sis.util.Classes;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * Base class for methods to export as formulas in the Apache OpenOffice spread sheet.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
abstract class CalcAddins extends WeakBase implements XAddIn, XServiceName, XServiceInfo {
    /**
     * {@code true} for throwing an exception in case of failure, or {@code false} for returning {@code NaN} instead.
     * This apply only to numerical computations; formulas returning a text value will returns the exception message
     * in case of failure.
     */
    static final boolean THROW_EXCEPTION = true;

    /**
     * Factor for conversions of days to milliseconds.
     * Used for date conversions as in {@link #toDate(XPropertySet, double)}.
     */
    static final long DAY_TO_MILLIS = 24*60*60*1000L;

    /**
     * The name of the provided service.
     */
    static final String ADDIN_SERVICE = "com.sun.star.sheet.AddIn";

    /**
     * Informations about exported methods.
     * This map shall be populated at construction time and be unmodified after construction.
     */
    final Map<String,MethodInfo> methods;

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
     * The calendar to uses for date conversions. Will be created only when first needed.
     */
    private transient Calendar calendar;

    /**
     * The logger, fetched when first needed.
     */
    private static Logger logger;

    /**
     * Default constructor. Subclass constructors need to add entries in the {@link #methods} map.
     */
    CalcAddins() {
        methods = new HashMap<>();
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
     * Provides the supported service names of the implementation,
     * including also indirect service names.
     *
     * @return sequence of service names that are supported.
     */
    @Override
    public final String[] getSupportedServiceNames() {
        return new String[] {ADDIN_SERVICE, getServiceName()};
    }

    /**
     * Tests whether the specified service is supported, i.e. implemented by the implementation.
     *
     * @param  name  name of service to be tested.
     * @return {@code true} if the service is supported, {@code false} otherwise.
     */
    @Override
    public final boolean supportsService(final String name) {
        return name.equals(ADDIN_SERVICE) || name.equals(getServiceName());
    }

    /**
     * The service name that can be used to create such an object by a factory.
     * This is defined as a field in the subclass with exactly the following signature:
     *
     * {@preformat java
     *     private static final String __serviceName;
     * }
     *
     * @return the service name.
     */
    @Override
    public abstract String getServiceName();

    /**
     * Provides the implementation name of the service implementation.
     *
     * @return unique name of the implementation.
     */
    @Override
    public final String getImplementationName() {
        return getClass().getName();
    }

    /**
     * Returns the programmatic name of the category the function belongs to.
     * The category name is used to group similar functions together.
     * The programmatic category name should always be in English, it is never shown to the user.
     * It is usually one of the names listed in {@code com.sun.star.sheet.XAddIn} interface.
     *
     * @param  function the exact name of a method within its interface.
     * @return the category name the specified function belongs to.
     */
    @Override
    public final String getProgrammaticCategoryName(final String function) {
        final MethodInfo info = methods.get(function);
        return (info != null) ? info.category : "Add-In";
    }

    /**
     * Returns the user-visible name of the category the function belongs to.
     * This is used when category names are shown to the user.
     *
     * @param  function  the exact name of a method within its interface.
     * @return the user-visible category name the specified function belongs to.
     */
    @Override
    public final String getDisplayCategoryName(final String function) {
        return getProgrammaticCategoryName(function);
    }

    /**
     * Returns the internal function name for an user-visible name. The user-visible name of a function
     * is the name shown to the user. It may be translated to the {@linkplain #getLocale() current locale},
     * so it is never stored in files. It should be a single word and is used when entering or displaying formulas.
     *
     * <p>Attention: The method name contains a spelling error.
     * Due to compatibility reasons the name cannot be changed.</p>
     *
     * @param  display the user-visible name of a function.
     * @return the exact name of the method within its interface.
     */
    @Override
    public final String getProgrammaticFuntionName(final String display) {
        for (final Map.Entry<String,MethodInfo> entry : methods.entrySet()) {
            if (display.equals(entry.getValue().display)) {
                return entry.getKey();
            }
        }
        return "";
    }

    /**
     * Returns the user-visible function name for an internal name.
     * The user-visible name of a function is the name shown to the user.
     * It may be translated to the {@linkplain #getLocale() current locale}, so it is never stored in files.
     * It should be a single word and is used when entering or displaying formulas.
     *
     * @param  function  the exact name of a method within its interface.
     * @return the user-visible name of the specified function.
     */
    @Override
    public final String getDisplayFunctionName(final String function) {
        final MethodInfo info = methods.get(function);
        return (info != null) ? info.display : "";
    }

    /**
     * Returns the description of a function.
     * The description is shown to the user when selecting functions.
     * It may be translated to the {@linkplain #getLocale() current locale}.
     *
     * @param  function the exact name of a method within its interface.
     * @return the description of the specified function.
     */
    @Override
    public final String getFunctionDescription(final String function) {
        final MethodInfo info = methods.get(function);
        return (info != null) ? info.description : "";
    }

    /**
     * Returns the user-visible name of the specified argument.
     * The argument name is shown to the user when prompting for arguments.
     * It should be a single word and may be translated to the {@linkplain #getLocale() current locale}.
     *
     * @param  function  the exact name of a method within its interface.
     * @param  argument  the index of the argument (0-based).
     * @return the user-visible name of the specified argument.
     */
    @Override
    public final String getDisplayArgumentName(final String function, int argument) {
        final MethodInfo info = methods.get(function);
        if (info != null) {
            argument <<= 1;
            final String[] arguments = info.arguments;
            if (argument >= 0 && argument < arguments.length) {
                return arguments[argument];
            }
        }
        return "";
    }

    /**
     * Returns the description of the specified argument.
     * The argument description is shown to the user when prompting for arguments.
     * It may be translated to the {@linkplain #getLocale() current locale}.
     *
     * @param  function  the exact name of a method within its interface.
     * @param  argument  the index of the argument (0-based).
     * @return the description of the specified argument.
     */
    @Override
    public final String getArgumentDescription(final String function, int argument) {
        final MethodInfo info = methods.get(function);
        if (info != null) {
            argument = (argument << 1) + 1;
            final String[] arguments = info.arguments;
            if (argument >= 0 && argument < arguments.length) {
                return arguments[argument];
            }
        }
        return "";
    }

    /**
     * Sets the timezone for time values to be provided to {@link #toDate(XPropertySet, double)}.
     * If this method is never invoked, then the default timezone is the locale one.
     *
     * @param timezone the new timezone.
     */
    protected final void setTimeZone(final String timezone) {
        final TimeZone tz = TimeZone.getTimeZone(timezone);
        synchronized (this) {
            if (calendar == null) {
                calendar = new GregorianCalendar(tz);
            } else {
                calendar.setTimeZone(tz);
            }
        }
    }

    /**
     * Returns the spreadsheet epoch.
     * The timezone is the one specified during the last invocation of {@link #setTimeZone(String)}.
     * The epoch is used for date conversions as in {@link #toDate(XPropertySet, double)}.
     *
     * @param  xOptions  provided by OpenOffice.
     * @return the spreadsheet epoch as a new Java Date object, or {@code null}.
     */
    protected final java.util.Date getEpoch(final XPropertySet xOptions) {
        final Date date;
        try {
            date = (Date) AnyConverter.toObject(Date.class, xOptions.getPropertyValue("NullDate"));
        } catch (Exception e) {     // Too many possible exceptions for enumerating all of them.
            reportException("getEpoch", e, THROW_EXCEPTION);
            return null;
        }
        synchronized (this) {
            if (calendar == null) {
                calendar = new GregorianCalendar();
            }
            calendar.clear();
            calendar.set(date.Year, date.Month-1, date.Day);
            return calendar.getTime();
        }
    }

    /**
     * Converts a date from a spreadsheet value to a Java {@link java.util.Date} object.
     * The timezone is the one specified during the last invocation of {@link #setTimeZone(String)}.
     *
     * @param  xOptions  provided by OpenOffice.
     * @param  time      the spreadsheet numerical value for a date, by default in the local timezone.
     * @return the date as a Java object.
     */
    protected final java.util.Date toDate(final XPropertySet xOptions, final double time) {
        final java.util.Date date = getEpoch(xOptions);
        if (date != null) {
            date.setTime(date.getTime() + Math.round(time * DAY_TO_MILLIS));
        }
        return date;
    }

    /**
     * Converts a date from a Java {@link java.util.Date} object to a spreadsheet value.
     * The timezone is the one specified during the last invocation of {@link #setTimeZone(String)}.
     *
     * @param  xOptions  provided by OpenOffice.
     * @param  time      the date as a Java object.
     * @return the spreadsheet numerical value for a date, by default in the local timezone.
     */
    protected final double toDouble(final XPropertySet xOptions, final java.util.Date time) {
        final java.util.Date epoch = getEpoch(xOptions);
        if (epoch != null) {
            return (time.getTime() - epoch.getTime()) / (double) DAY_TO_MILLIS;
        } else {
            return Double.NaN;
        }
    }

    /**
     * The string to returns when a formula does not have any value to return.
     *
     * @return the string with a message for missing values.
     * @todo localize.
     */
    static String noResultString() {
        return "(none)";
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
     * Returns a table filled with {@link Double#NaN} values.
     * This method is invoked when an operation failed for a whole table.
     *
     * @param  rows  the number of rows.
     * @param  cols  the number of columns.
     * @return A table of the given size filled with NaN values.
     */
    static double[][] getFailure(final int rows, final int cols) {
        final double[][] dummy = new double[rows][];
        for (int i=0; i<rows; i++) {
            final double[] row = new double[cols];
            Arrays.fill(row, Double.NaN);
            dummy[i] = row;
        }
        return dummy;
    }

    /**
     * Reports an exception. This is used if an exception occurred in a method that can not return
     * a {@link String} instance. This method logs the stack trace at {@link Level#WARNING}.
     *
     * @param method     the method from which an exception occurred.
     * @param exception  the exception.
     * @param rethrow    {@code true} for rethrowing the exception after the report.
     */
    final void reportException(final String method, final Exception exception, final boolean rethrow) {
        final Logger logger = getLogger();
        final LogRecord record = new LogRecord(Level.WARNING, getLocalizedMessage(exception));
        record.setLoggerName(logger.getName());
        record.setSourceClassName(getClass().getName());
        record.setSourceMethodName(method);
        record.setThrown(exception);
        logger.log(record);
        if (rethrow) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            }
            throw new BackingStoreException(exception);
        }
    }

    /**
     * Returns the logger to use for logging warnings.
     *
     * @return the logger to use.
     */
    protected static synchronized Logger getLogger() {
        if (logger == null) {
            logger = Logging.getLogger(Registration.LOGGER);
        }
        return logger;
    }
}
