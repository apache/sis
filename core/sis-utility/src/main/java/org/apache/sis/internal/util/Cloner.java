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
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.resources.Errors;


/**
 * Clones objects of arbitrary type using reflection methods. This is a workaround
 * for the lack of public {@code clone()} method in the {@link Cloneable} interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.6
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
        SecurityException security = null;
        final Class<?> valueType = object.getClass();
        try {
            if (valueType != type) {
                method = valueType.getMethod("clone", (Class<?>[]) null);
                type = valueType; // Set only if the above line succeed.
                /*
                 * If the class implementing the 'clone()' method is not public, we may not be able to access that
                 * method even if it is public. Try to make the method accessible. If we fail for security reason,
                 * we will still attempt to clone (maybe a parent class is public), but we remember the exception
                 * in order to report it in case of failure.
                 */
                if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) try {
                    method.setAccessible(true);
                } catch (SecurityException e) {
                    security = e;
                }
            }
            /*
             * 'method' may be null if a previous call to this clone(Object) method threw NoSuchMethodException
             * (see the first 'catch' block below). In this context, 'null' means "no public clone() method".
             */
            if (method != null) {
                return method.invoke(object, (Object[]) null);
            }
        } catch (NoSuchMethodException e) {
            if (isCloneRequired(object)) {
                throw fail(e, valueType);
            }
            method = null;
            type = valueType;
        } catch (IllegalAccessException e) {
            // JDK7 branch has the following: e.addSuppressed(security);
            throw fail(e, valueType);
        } catch (InvocationTargetException e) {
            rethrow(e.getCause());
            throw fail(e, valueType);
        } catch (SecurityException e) {
            throw fail(e, valueType);
        }
        return object;
    }

    /**
     * Throws the given exception if it is an instance of {@code CloneNotSupportedException}
     * or an unchecked exception, or do nothing otherwise. If this method returns normally,
     * then it is caller's responsibility to throw an other exception.
     *
     * @param cause The value of {@link InvocationTargetException#getCause()}.
     */
    private static void rethrow(final Throwable cause) throws CloneNotSupportedException {
        if (cause instanceof CloneNotSupportedException) {
            throw (CloneNotSupportedException) cause;
        }
        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }
        if (cause instanceof Error) {
            throw (Error) cause;
        }
        // If we reach this point, caller should invoke "throw fail(e, type)".
    }

    /**
     * Returns an exception telling that the object can not be cloned because of the given error.
     *
     * @param  cause The cause for the failure to clone an object.
     * @param  type  The type of object that we failed to clone.
     * @return An exception with an error message and the given cause.
     */
    private static CloneNotSupportedException fail(final Throwable cause, final Class<?> type) {
        return (CloneNotSupportedException) new CloneNotSupportedException(
                Errors.format(Errors.Keys.CloneNotSupported_1, type)).initCause(cause);
    }

    /**
     * Clones the given object if its {@code clone()} method is public, or returns the same object otherwise.
     * This method may be convenient when there is only one object to clone, otherwise instantiating a new
     * {@code Cloner} object is more efficient.
     *
     * @param  object The object to clone, or {@code null}.
     * @return The given object (which may be {@code null}) or a clone of the given object.
     * @throws CloneNotSupportedException if the call to {@link Object#clone()} failed.
     *
     * @since 0.6
     */
    public static Object cloneIfPublic(final Object object) throws CloneNotSupportedException {
        if (object != null) {
            final Class<?> type = object.getClass();
            try {
                final Method m = type.getMethod("clone", (Class[]) null);
                if (Modifier.isPublic(m.getModifiers())) {
                    return m.invoke(object, (Object[]) null);
                }
            } catch (NoSuchMethodException e) {
                /*
                 * Should never happen because all objects have a clone() method
                 * and we verified that the method is public.
                 */
                throw new AssertionError(e);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e) {
                rethrow(e.getCause());
                throw fail(e, type);
            } catch (SecurityException e) {
                throw fail(e, type);
            }
        }
        return object;
    }
}
