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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.registry.XRegistryKey;


/**
 * The registration of all formulas provided in this package.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
public final class Registration {
    /**
     * Name of the logger to use for the add-ins.
     */
    static final String LOGGER = "org.apache.sis.openoffice";

    /**
     * Do not allow instantiation of this class.
     */
    private Registration() {
    }

    /**
     * Logs the given exception before to abort installation. We use logging service instead of
     * propagating the exception because OpenOffice does not report the exception message.
     */
    private static void fatalException(final String method, final String message, final Throwable exception) {
        final Logger logger = Logger.getLogger(LOGGER);
        final LogRecord record = new LogRecord(Level.SEVERE, message);
        record.setSourceClassName(Registration.class.getName());
        record.setSourceMethodName(method);
        record.setLoggerName(LOGGER);
        record.setThrown(exception);
        logger.log(record);
    }

    /**
     * Logs the given exception for a classpath problem.
     */
    private static void classpathException(final String method, final Throwable exception) {
        final String lineSeparator = System.lineSeparator();
        final StringBuilder message = new StringBuilder("Can not find Apache SIS classes.").append(lineSeparator)
                .append("Classpath = ").append(System.getProperty("java.class.path"));
        final ClassLoader loader = ReferencingFunctions.class.getClassLoader();
        if (loader instanceof URLClassLoader) {
            for (final URL url : ((URLClassLoader) loader).getURLs()) {
                message.append(lineSeparator).append("  + ").append(url);
            }
        }
        fatalException(method, message.toString(), exception);
    }

    /**
     * Returns a factory for creating the service.
     * This method is called by the {@code com.sun.star.comp.loader.JavaLoader}; do not rename!
     *
     * @param   implementation  the name of the implementation for which a service is desired.
     * @return  a factory for creating the component.
     */
    public static XSingleComponentFactory __getComponentFactory(final String implementation) {
        if (implementation.equals(ReferencingFunctions.IMPLEMENTATION_NAME)) {
            try {
                return Factory.createComponentFactory(ReferencingFunctions.class,
                        new String[] {ReferencingFunctions.SERVICE_NAME});
            } catch (LinkageError e) {
                classpathException("__getComponentFactory", e);
                throw e;
            }
        }
        return null;
    }

    /**
     * Writes the service information into the given registry key.
     * This method is called by the {@code com.sun.star.comp.loader.JavaLoader}; do not rename!
     *
     * @param  registry  the registry key.
     * @return {@code true} if the operation succeeded.
     */
    public static boolean __writeRegistryServiceInfo(final XRegistryKey registry) {
        try {
            return Factory.writeRegistryServiceInfo(ReferencingFunctions.IMPLEMENTATION_NAME,
                    new String[] {ReferencingFunctions.SERVICE_NAME}, registry);
        } catch (LinkageError e) {
            classpathException("__writeRegistryServiceInfo", e);
        }
        return false;
    }
}
