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
package org.apache.sis.internal.shapefile.jdbc;

import java.text.*;
import java.util.*;
import java.util.logging.*;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;


/**
 * Base class for each JDBC class.
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
public abstract class AbstractJDBC {
    /** Logger. */
    private Logger logger = Logger.getLogger(getClass().getSimpleName());

    /**
     * Format a resource bundle message.
     *
     * @param classForResourceBundleName class from which ResourceBundle name will be extracted.
     * @param key Message key.
     * @param args Message arguments.
     * @return Message.
     */
    final String format(Class<?> classForResourceBundleName, String key, Object... args) {
        Objects.requireNonNull(classForResourceBundleName, "Class from with the ResourceBundle name is extracted cannot be null.");
        Objects.requireNonNull(key, "Message key cannot be bull.");

        ResourceBundle rsc = ResourceBundle.getBundle(classForResourceBundleName.getName());
        MessageFormat format = new MessageFormat(rsc.getString(key));
        return format.format(args);
    }

    /**
     * Format a resource bundle message.
     *
     * @param key Message key.
     * @param args Message arguments.
     * @return Message.
     */
    final String format(String key, Object... args) {
        return format(getClass(), key, args);
    }

    /**
     * Format a resource bundle message and before returning it, log it.
     *
     * @param logLevel Log Level.
     * @param key Message key.
     * @param args Message arguments.
     * @return Message.
     */
    final String format(Level logLevel, String key, Object... args) {
        String message = format(key, args);
        logger.log(logLevel, message);
        return(message);
    }

    /**
     * Format a resource bundle message and before returning it, log it.
     *
     * @param classForResourceBundleName class from which ResourceBundle name will be extracted.
     * @param logLevel Log Level.
     * @param key Message key.
     * @param args Message arguments.
     * @return Message.
     */
    final String format(Level logLevel, Class<?> classForResourceBundleName, String key, Object... args) {
        String message = format(classForResourceBundleName, key, args);
        logger.log(logLevel, message);
        return(message);
    }

    /**
     * Returns an unsupported operation exception.
     *
     * @param unhandledInterface Interface we cannot handle.
     * @param methodOrWishedFeatureName The feature / call the caller attempted.
     */
    final SQLException unsupportedOperation(Class<?> unhandledInterface, String methodOrWishedFeatureName) {
        Objects.requireNonNull(unhandledInterface, "The unhandled interface cannot be null.");

        String message = logUnsupportedOperation(Level.SEVERE, unhandledInterface, methodOrWishedFeatureName);
        return new SQLFeatureNotSupportedException(message);
    }

    /**
     * log an unsupported feature as a warning.
     *
     * @param logLevel Log Level.
     * @param unhandledInterface Interface we cannot handle.
     * @param methodOrWishedFeatureName The feature / call the caller attempted.
     * @return The message that has been logged.
     */
    final String logUnsupportedOperation(Level logLevel, Class<?> unhandledInterface, String methodOrWishedFeatureName) {
        Objects.requireNonNull(unhandledInterface, "The unhandled interface cannot be null.");
        return format(logLevel, AbstractJDBC.class, "excp.unsupported_driver_feature", unhandledInterface.getClass(), methodOrWishedFeatureName);
    }

    /**
     * Return the class logger.
     * @return logger.
     */
    public Logger getLogger() {
        return logger;
    }
}
