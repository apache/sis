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
package org.apache.sis.internal.system;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;


/**
 * Base class of internal hooks for accessing optional dependencies.
 * This is used for example for allowing the {@code "sis-metadata"} module to access some
 * services of the {@code "sis-referencing"} module if the latter is present on the classpath.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.6
 * @since   0.6
 * @module
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
     * @param dependency  the Maven artifact name (<strong>not</strong> a name from the {@link Modules} class)
     *        of the optional module on which the {@code module} depend.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    protected OptionalDependency(final String module, final String dependency) {
        super(module);
        this.dependency = dependency;
        SystemListener.add(this);
    }

    /**
     * Invoked when the classpath is likely to have changed.
     * Subclasses must override like below:
     *
     * {@preformat java
     *     &#64;Override
     *     protected final void classpathChanged() {
     *         synchronized (MyServices.class) {
     *             super.classpathChanged();
     *             instance = null;
     *         }
     *     }
     * }
     */
    @Override
    protected void classpathChanged() {
        SystemListener.remove(this);
    }

    /**
     * Returns the optional dependency, or {@code null} if not found.
     * This is a helper method for implementation of {@code getInstance()} static method in subclasses.
     *
     * @param  <T>             compile-time type of the {@code type} argument.
     * @param  type            the subclass type.
     * @param  module          same argument value than the one given to the {@linkplain #OptionalDependency constructor}.
     * @param  dependency      same argument value than the one given to the {@linkplain #OptionalDependency constructor}.
     * @param  implementation  the fully-qualified name of the class to instantiate by reflection.
     * @return an instance of the {@code implementation} class, or {@code null} if not found.
     */
    protected static <T extends OptionalDependency> T getInstance(final Class<T> type,
            final String module, final String dependency, final String implementation)
    {
        try {
            return type.cast(Class.forName(implementation).newInstance());
        } catch (ClassNotFoundException exception) {
            final LogRecord record = Messages.getResources(null).getLogRecord(Level.CONFIG,
                    Messages.Keys.OptionalModuleNotFound_1, dependency);
            record.setLoggerName(module);
            Logging.log(type, "getInstance", record);
            return null;
        } catch (ReflectiveOperationException exception) {
            // Should never happen if we didn't broke our helper class.
            throw new AssertionError(exception);
        }
    }

    /**
     * Returns the exception to throw when a method requiring the optional dependency is invoked
     * but that module is not on the classpath.
     *
     * @return the exception to throw.
     */
    protected final UnsupportedOperationException moduleNotFound() {
        return new UnsupportedOperationException(Errors.format(Errors.Keys.MissingRequiredModule_1, dependency));
    }
}
