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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.sql.Wrapper;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import org.apache.sis.util.logging.Logging;


/**
 * Base class for each JDBC class.
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
abstract class AbstractJDBC implements Wrapper {
    /**
     * The logger for JDBC operations. We use the {@code "org.apache.sis.storage.jdbc"} logger name instead than
     * the package name because this package is internal and may move in any future SIS version. The logger name
     * does not need to be the name of an existing package. The important thing is to not change it, because it
     * can been seen as a kind of public API since user may want to control verbosity level by logger names.
     */
    static final Logger LOGGER = Logging.getLogger("org.apache.sis.storage.jdbc");

    /**
     * Constructs a new instance of a JDBC interface.
     */
    AbstractJDBC() {
    }

    /**
     * Returns the JDBC interface implemented by this class.
     * This is used for formatting error messages.
     *
     * @return The JDBC interface implemented by this class.
     */
    abstract Class<?> getInterface();

    /**
     * Unsupported by default.
     *
     * @param  iface the type of the wrapped object.
     * @return The wrapped object.
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw unsupportedOperation("unwrap");
    }

    /**
     * Default to {@code false}, assuming that no non-standard features are handled.
     *
     * @param  iface the type of the wrapped object.
     * @return {@code true} if this instance is a wrapper for the given type of object.
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return false;
    }

    /**
     * Defaults to {@code null}.
     */
    public SQLWarning getWarnings() {
        return null;
    }

    /**
     * Defaults to nothing, since there is no SQL warning.
     */
    public void clearWarnings() {
    }

    /**
     * Returns an unsupported operation exception to be thrown.
     *
     * @param  methodOrWishedFeatureName The feature / call the caller attempted.
     * @return The exception to throw.
     */
    final SQLException unsupportedOperation(final String methodOrWishedFeatureName) {
        return new SQLFeatureNotSupportedException(Resources.format(Resources.Keys.UnsupportedDriverFeature_2,
                getInterface(), methodOrWishedFeatureName));
    }

    /**
     * log an unsupported feature as a warning.
     *
     * @param methodName The call the caller attempted.
     */
    final void logUnsupportedOperation(final String methodName) {
        logWarning(methodName, Resources.Keys.UnsupportedDriverFeature_2, getInterface(), methodName);
    }

    /**
     * Logs a warning with the given resource keys and arguments.
     *
     * @param methodName  The name of the method which is emitting the warning.
     * @param resourceKey One of the {@link org.apache.sis.internal.shapefile.jdbc.Resources.Keys} values.
     * @param arguments   Arguments to be given to {@link java.text.MessageFormat}, or {@code null} if none.
     */
    final void logWarning(final String methodName, final short resourceKey, final Object... arguments) {
        final LogRecord record = Resources.getResources(null).getLogRecord(Level.WARNING, resourceKey, arguments);
        record.setSourceClassName(getClass().getCanonicalName());
        record.setSourceMethodName(methodName);
        record.setLoggerName(LOGGER.getName());
        LOGGER.log(record);
    }
}
