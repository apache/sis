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
package org.apache.sis.filter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.ServiceLoader;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.referencing.internal.shared.LazySet;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.filter.sqlmm.Registry;
import org.apache.sis.system.Reflect;


/**
 * Metadata about the specific elements that Apache <abbr>SIS</abbr> implementation supports.
 * This is also an unmodifiable map of functions supported by this factory,
 * together with their providers. This class is thread-safe.
 *
 * @todo Missing {@link SpatialCapabilities} and {@link TemporalCapabilities}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Capabilities {
    /**
     * The providers of functions.
     * Each {@code FunctionRegister} instance typically provides many functions.
     * There is one provider for SQL/MM, one provider for mathematical functions, <i>etc</i>.
     */
    private final LazySet<FunctionRegister> providers;

    /**
     * Providers of functions identified by their names.
     * Created when first needed and reset to {@code null} when not needed anymore.
     *
     * @see #nextFunctionRegister()
     */
    private Iterator<FunctionRegister> registerIterator;

    /**
     * Names of all functions sorted in increasing order ignoring case.
     * This array is initially empty and is increased when new providers are tested.
     * The array may contain duplicated values if two or more {@link FunctionRegister}
     * declare a function of the same name.
     *
     * @see #indexOfFunction(String)
     */
    private String[] functionNames;

    /**
     * The provider for each function name. The length of this array shall be equal to the length
     * of the {@link #functionNames} array. The provider at index <var>i</var> shall accept a function of
     * the name at index <var>i</var>.
     *
     * <h4>Design note</h4>
     * The combination of {@link #functionNames} and {@code providersForName} could be replaced by {@link java.util.TreeSet}.
     * But we use array as an efficient way to get both the key (function name) and value for a given key in order to
     * resolve the real names in case-insensitive searches. We also need to support duplicated names.
     *
     * @see #createFunction(String, Expression[])
     */
    private FunctionRegister[] registerForName;

    /**
     * Creates a new capability document and map for the specified geometry library.
     *
     * @param  geometries  the library to use for creating geometry objects.
     */
    Capabilities(final Geometries<?> geometries) {
        functionNames = CharSequences.EMPTY_ARRAY;
        providers = new LazySet<FunctionRegister>() {
            /** Declares that this set excludes null. */
            @Override protected int characteristics() {
                return Spliterator.DISTINCT | Spliterator.NONNULL;
            }

            /** Returns the SQLMM functions to check first. */
            @Override protected FunctionRegister[] initialValues() {
                return new FunctionRegister[] {new Registry(geometries)};
            }

            /** Returns the other functions (math, etc.) to check after SQLMM. */
            @Override protected Iterator<FunctionRegister> createSourceIterator() {
                return ServiceLoader.load(FunctionRegister.class, Reflect.getContextClassLoader()).iterator();
            }
        };
    }

    /**
     * Returns the next function register, or {@code null} if none.
     * This method shall be invoked in a block synchronized on {@code this}.
     * This method is invoked only for deferred loading of function registers.
     * After all function registers have been loaded, this method always returns {@code null}.
     */
    private FunctionRegister nextFunctionRegister() {
        assert Thread.holdsLock(this);
        if (registerIterator == null) {
            if (registerForName != null) {
                return null;   // We already extracted all providers.
            }
            registerIterator = providers.iterator();
        }
        if (!registerIterator.hasNext()) {
            return null;
        }
        final FunctionRegister register = registerIterator.next();
        /*
         * Combine the list of functions of the new register with the list of functions of all previous registers.
         * If the same name is used twice, the previous registers have precedence in `registerIterator` order.
         */
        final String[] combined = ArraysExt.concatenate(functionNames, register.getNames().toArray(String[]::new));
        Arrays.sort(combined, String.CASE_INSENSITIVE_ORDER);
        final var registers = new FunctionRegister[combined.length];
        Arrays.fill(registers, register);
        if (registerForName != null) {
            // ArrayIndexOutOfBoundsException on `i` should never happen.
            for (int i=0, s=0; s < functionNames.length; i++) {
                if (combined[i].equalsIgnoreCase(functionNames[s])) {
                    registers[i] = registerForName[s++];
                }
            }
        }
        registerForName = registers;
        functionNames = combined;
        return register;
    }

    /**
     * Returns the index of the provider for the function of the given name.
     * This method shall be invoked in a block synchronized on {@code this}.
     * If many providers define a function of the given name,
     * then there is no guaranteed about which provider is returned.
     *
     * @param  name  case-insensitive name of the function to get.
     * @return index of the provider for the given name, or -1 if not found.
     */
    private int indexOfFunction(final String name) {
        int i;
        while ((i = Arrays.binarySearch(functionNames, name, String.CASE_INSENSITIVE_ORDER)) < 0) {
            if (nextFunctionRegister() == null) {
                return -1;
            }
        }
        return i;
    }

    /**
     * Creates an implementation-specific function. If many registers have a function of the given name,
     * then this method tries all them in iteration order until one of them succeed.
     *
     * @param  <R>         type of resources.
     * @param  name        name of the function to call.
     * @param  parameters  expressions providing values for the function arguments.
     * @return an expression which will call the specified function, or {@code null} if the given name is unknown.
     * @throws IllegalArgumentException if the arguments are illegal for the specified function.
     */
    @SuppressWarnings("empty-statement")
    public <R> Expression<R,?> createFunction(String name, final Expression<R,?>[] parameters) {
        final String[] names;
        final FunctionRegister[] registers;
        synchronized (this) {
            int i = indexOfFunction(name);
            if (i < 0) return null;
            int upper = i;
            while (++upper < functionNames.length && name.equalsIgnoreCase(functionNames[upper]));
            while (i != 0 && name.equalsIgnoreCase(functionNames[i-1])) i--;
            names     = Arrays.copyOfRange(functionNames,   i, upper);
            registers = Arrays.copyOfRange(registerForName, i, upper);
        }
        IllegalArgumentException cause = null;
        for (int i=0; i<names.length; i++) try {
            return registers[i].create(names[i], parameters);
        } catch (IllegalArgumentException e) {
            if (cause == null) cause = e;
            else cause.addSuppressed(e);
        }
        if (cause != null) {
            throw cause;
        }
        return null;
    }
}
