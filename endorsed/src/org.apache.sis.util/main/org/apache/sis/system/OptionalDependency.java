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
package org.apache.sis.system;

import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;


/**
 * Base class of internal hooks for accessing optional dependencies.
 * This is used for example for allowing the {@code org.apache.sis.metadata} module to access some
 * services of the {@code org.apache.sis.referencing} module if the latter is present on the module path.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class OptionalDependency extends SystemListener {
    /**
     * The name of the optional module on which we depend.
     */
    private final String dependency;

    /**
     * Creates a new optional dependency.
     *
     * @param module  a constant from the {@link Modules} class which identify the module that need the optional dependency.
     * @param dependency  the name of the optional module on which the specified {@code module} depends.
     */
    @SuppressWarnings("this-escape")            // This class is internal API.
    protected OptionalDependency(final String module, final String dependency) {
        super(module);
        this.dependency = dependency;
        SystemListener.add(this);
    }

    /**
     * Returns the optional dependency, or {@code null} if not found.
     * This is a helper method for implementation of {@code getInstance()} static method in subclasses.
     * The service loader needs to be created by the caller because of Java module encapsulation rules.
     *
     * @param  <T>         compile-time type of the {@code type} argument.
     * @param  type        the service type, used only if a warning needs to be logged.
     * @param  loader      the service loader created in the module that needs the service.
     * @param  dependency  same argument value as the one given to the {@linkplain #OptionalDependency constructor}.
     * @return an instance of the {@code implementation} class, or {@code null} if not found.
     */
    protected static <T extends OptionalDependency> T getInstance(final Class<T> type,
                                final ServiceLoader<T> loader, final String dependency)
    {
        final T first = loader.findFirst().orElse(null);
        if (first == null) {
            LogRecord record = Messages.forLocale(null).createLogRecord(
                    Level.CONFIG,
                    Messages.Keys.OptionalModuleNotFound_1,
                    dependency);
            record.setLoggerName(type.getModule().getName());
            Logging.completeAndLog(null, type, "getInstance", record);
        }
        return first;
    }

    /**
     * Returns the exception to throw when a method requiring the optional dependency is invoked
     * but that module is not on the module path.
     *
     * @return the exception to throw.
     */
    protected final UnsupportedOperationException moduleNotFound() {
        return new UnsupportedOperationException(Errors.format(Errors.Keys.MissingRequiredModule_1, dependency));
    }
}
