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

import java.lang.reflect.Field;
import java.io.InvalidClassException;
import org.apache.sis.internal.system.Modules;


/**
 * Convenience methods for setting the final field of an object.
 * This class shall be used only after deserialization or cloning of Apache SIS objects.
 * The usage pattern is:
 *
 * <p><b>On deserialization:</b></p>
 * {@preformat java
 *     private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
 *         in.defaultReadObject();
 *         Object someValue = ...;
 *         try {
 *             FinalFieldSetter.set(MyClass.class, "myField", this, someValue);
 *         } catch (ReflectiveOperationException e) {
 *             throw FinalFieldSetter.readFailure(e);
 *         }
 *     }
 * }
 *
 * <p><b>On clone:</b></p>
 * Same as above but invoking {@code cloneFailure(e)} if the operation failed.
 * The exception to be thrown is not the same.
 *
 * <h2>Historical note</h2>
 * Previous version was implementing {@code PrivilegedAction<FinalFieldSetter<T>>}
 * for working in the context of a security manager. This feature has been removed
 * since {@code java.security.AccessController} has been deprecated in Java 17.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see <a href="https://openjdk.java.net/jeps/411">JEP-411</a>
 * @see <a href="https://issues.apache.org/jira/browse/SIS-525">SIS-525</a>
 *
 * @since 1.0
 * @module
 */
public final class FinalFieldSetter {
    /**
     * Do not allow instantiation of this class.
     */
    private FinalFieldSetter() {
    }

    /**
     * Sets the value of the final field.
     *
     * @param  <T>       the type of object in which to set a final field. Should be Apache SIS classes only.
     * @param  classe    the Apache SIS class of object for which to set a final field.
     * @param  field     the name of the final field for which to set a value.
     * @param  instance  the instance on which to set the value.
     * @param  value     the value to set.
     * @throws NoSuchFieldException if the given field has not been found.
     * @throws IllegalAccessException if the value can not be set.
     */
    public static <T> void set(final Class<T> classe, final String field, final T instance,
            final Object value) throws NoSuchFieldException, IllegalAccessException
    {
        assert classe.getName().startsWith(Modules.CLASSNAME_PREFIX) : classe;
        final Field f = classe.getDeclaredField(field);
        f.setAccessible(true);
        f.set(instance, value);
    }

    /**
     * Sets the values of the final fields.
     *
     * @param  <T>       the type of object in which to set a final field. Should be Apache SIS classes only.
     * @param  classe    the Apache SIS class of object for which to set a final field.
     * @param  field     the name of the first final field for which to set a value.
     * @param  second    the name of the second final field for which to set a value.
     * @param  instance  the instance on which to set the value.
     * @param  value     the value of the first field to set.
     * @param  more      the value of the second field to set.
     * @throws NoSuchFieldException if a given field has not been found.
     * @throws IllegalAccessException if a value can not be set.
     */
    public static <T> void set(final Class<T> classe, final String field, final String second, final T instance,
            final Object value, final Object more) throws NoSuchFieldException, IllegalAccessException
    {
        set(classe, field, instance, value);
        final Field f = classe.getDeclaredField(second);
        f.setAccessible(true);
        f.set(instance, more);
    }

    /**
     * Creates an exception for a {@code readObject(ObjectInputStream)} method.
     *
     * @param  cause  the failure.
     * @return the exception to throw.
     */
    public static InvalidClassException readFailure(final ReflectiveOperationException cause) {
        return (InvalidClassException) new InvalidClassException(cause.getLocalizedMessage()).initCause(cause);
    }

    /**
     * Creates an exception for a {@code clone()} method.
     *
     * @param  cause  the failure.
     * @return the exception to throw.
     */
    public static RuntimeException cloneFailure(final ReflectiveOperationException cause) {
        return new RuntimeException(cause);
        // TODO: use InaccessibleObjectException in JDK9.
    }
}
