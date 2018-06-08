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
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.apache.sis.internal.system.Modules;


/**
 * Convenience methods for setting the final field of an object with privileged permissions.
 * This class shall be used only after deserialization or cloning of Apache SIS objects.
 * The usage pattern is:
 *
 * <p><b>On deserialization:</b></p>
 * {@preformat java
 *     private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
 *         in.defaultReadObject();
 *         Object someValue = ...;
 *         try {
 *             AccessController.doPrivileged(new FinalFieldSetter<>(MyClass.class, "myField")).set(this, someValue);
 *         } catch (ReflectiveOperationException e) {
 *             throw FinalFieldSetter.readFailure(e);
 *         }
 *     }
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @param <T> the type of object in which to set a final field. Should be Apache SIS classes only.
 *
 * @since 1.0
 * @module
 */
public final class FinalFieldSetter<T> implements PrivilegedAction<FinalFieldSetter<T>> {
    /**
     * The field to make accessible in a privileged context.
     */
    private final Field field;

    /**
     * A second field to make accessible, or {@code null} if none.
     */
    private Field second;

    /**
     * Creates a new setter for a final field.
     *
     * <div class="note"><b>API note:</b>
     * this constructor is executed in a non-privileged context because {@link SecurityException} may happen here
     * only if the given class was loaded with a different class loader than this {@code FinalFieldSetter} class.
     * If such situation happen, then the given class is probably not an Apache SIS one.</div>
     *
     * @param  classe  the Apache SIS class of object for which to set a final field.
     * @param  field   the name of the final field for which to set a value.
     * @throws NoSuchFieldException if the given field has not been found.
     * @throws SecurityException may happen if the given class has been loaded with an unexpected class loader.
     */
    public FinalFieldSetter(final Class<T> classe, final String field) throws NoSuchFieldException {
        assert classe.getName().startsWith(Modules.CLASSNAME_PREFIX) : classe;
        this.field = classe.getDeclaredField(field);
    }

    /**
     * Creates a new setter for two final fields.
     *
     * @param  classe  the Apache SIS class of object for which to set a final field.
     * @param  field   the name of the first final field for which to set a value.
     * @param  second  the name of the second final field for which to set a value.
     * @throws NoSuchFieldException if the given field has not been found.
     * @throws SecurityException may happen if the given class has been loaded with an unexpected class loader.
     */
    public FinalFieldSetter(final Class<T> classe, final String field, final String second) throws NoSuchFieldException {
        this(classe, field);
        this.second = classe.getDeclaredField(second);
    }

    /**
     * Makes the final fields accessible.
     * This is a callback for {@link AccessController#doPrivileged(PrivilegedAction)}.
     * That call must be done from the caller, not from this {@code FinalFieldSetter} class,
     * because {@link AccessController} check the caller for determining the permissions.
     *
     * @return {@code this}.
     * @throws SecurityException if write permission has been denied.
     */
    @Override
    public FinalFieldSetter<T> run() throws SecurityException {
        field.setAccessible(true);
        if (second != null) {
            second.setAccessible(true);
        }
        return this;
    }

    /**
     * Sets the value of the final field.
     *
     * @param  instance  the instance on which to set the value.
     * @param  value     the value to set.
     * @throws IllegalAccessException may happen if {@link #run()} has not been invoked before this method.
     */
    public final void set(final T instance, final Object value) throws IllegalAccessException {
        field.set(instance, value);
    }

    /**
     * Sets the values of the final fields.
     *
     * @param  instance  the instance on which to set the value.
     * @param  value     the value of the first field to set.
     * @param  more      the value of the second field to set.
     * @throws IllegalAccessException may happen if {@link #run()} has not been invoked before this method.
     */
    public final void set(final T instance, final Object value, final Object more) throws IllegalAccessException {
        field .set(instance, value);
        second.set(instance, more);
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
