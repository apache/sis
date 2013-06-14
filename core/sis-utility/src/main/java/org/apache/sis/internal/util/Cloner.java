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
public class Cloner {
    /**
     * The type of the object to clone, or {@code null} if not yet specified.
     * Used for checking if the cached {@linkplain #method} is still valid.
     */
    private Class<?> type;

    /**
     * The {@code clone()} method, or {@code null} if not yet determined.
     * This is cached for better performance if many instances of the same type are cloned.
     */
    private Method method;

    /**
     * Creates a new {@code Cloner} instance.
     */
    public Cloner() {
    }

    /**
     * Invoked when the given object can not be cloned because no public {@code clone()} method
     * has been found. If this method returns {@code true}, then the {@link #clone(Object)}
     * method in this class will throw a {@link CloneNotSupportedException}. Otherwise the
     * {@code clone(Object)} method will return the original object.
     *
     * <p>The default implementation returns {@code true} in every cases.
     * Subclasses can override this method if they need a different behavior.</p>
     *
     * @param  object The object that can not be cloned.
     * @return {@code true} if the problem shall be considered a clone failure.
     */
    protected boolean isCloneRequired(final Object object) {
        return true;
    }

    /**
     * Clones the given object. If the given object does not provide a public {@code clone()}
     * method, then there is a choice:
     *
     * <ul>
     *   <li>If {@code isCloneRequired(object)} returns {@code true} (the default),
     *       then a {@link CloneNotSupportedException} is thrown.</li>
     *   <li>Otherwise the given object is returned.</li>
     * </ul>
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
            if (method != null) { // May be null if previous call threw NoSuchMethodException.
                return method.invoke(object, (Object[]) null);
            }
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
        } catch (NoSuchMethodException e) {
            if (isCloneRequired(object)) {
                throw fail(e);
            }
            method = null;
            type = valueType;
        } catch (Exception e) { // (ReflectiveOperationException) on JDK7
            throw fail(e);
        }
        return object;
    }

    /**
     * Returns an exception telling that the object can not be cloned because of the given error.
     * The {@link #clone(Object)} method must have been attempted before to invoke this method.
     *
     * @param  cause The cause for the failure to clone an object.
     * @return An exception with an error message and the given cause.
     */
    private CloneNotSupportedException fail(final Throwable cause) {
        CloneNotSupportedException e = new CloneNotSupportedException(
                Errors.format(Errors.Keys.CloneNotSupported_1, type));
        e.initCause(cause);
        return e;
    }
}
