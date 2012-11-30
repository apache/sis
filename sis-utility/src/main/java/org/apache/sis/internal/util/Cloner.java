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
package org.apache.sis.internal.util;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.resources.Errors;


/**
 * Clones objects of arbitrary type using reflection methods. This is a workaround
 * for the lack of public {@code clone()} method in the {@link Cloneable} interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
@Workaround(library="JDK", version="1.7")
public final class Cloner {
    /**
     * The type of the object to clone, or {@code null} if not yet specified.
     */
    private Class<?> type;

    /**
     * The {@code clone()} method, or {@code null} if not yet determined.
     */
    private Method method;

    /**
     * Creates a new {@code Cloner} instance.
     */
    public Cloner() {
    }

    /**
     * Clones the given object.
     *
     * @param  object The object to clone, or {@code null}.
     * @return A clone of the given object, or {@code null} if {@code object} was null.
     * @throws CloneNotSupportedException If the given object can not be cloned.
     */
    public Object clone(final Object object) throws CloneNotSupportedException {
        if (object == null) {
            return null;
        }
        final Class<?> valueType = object.getClass();
        try {
            if (valueType != type) {
                method = valueType.getMethod("clone", (Class<?>[]) null);
                type = valueType; // Set only if the above line succeed.
            }
            return method.invoke(object, (Object[]) null);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof CloneNotSupportedException) {
                throw (CloneNotSupportedException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw fail(e);
        } catch (Exception e) { // (ReflectiveOperationException) on JDK7
            throw fail(e);
        }
    }

    /**
     * Returns an exception telling that the object can not be cloned because of the given error.
     * The {@link #clone(Object)} method must have been attempted before to invoke this method.
     *
     * @param  cause The cause for the failure to clone an object.
     * @return An exception with an error message and the given cause.
     */
    public CloneNotSupportedException fail(final Throwable cause) {
        CloneNotSupportedException e = new CloneNotSupportedException(
                Errors.format(Errors.Keys.CloneNotSupported_1, type));
        e.initCause(cause);
        return e;
    }
}
