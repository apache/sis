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

import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.ServiceLoader;
import java.util.Optional;
import org.opengis.util.LocalName;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.collection.CodeListSet;
import org.apache.sis.util.internal.shared.AbstractMap;
import org.apache.sis.referencing.internal.shared.LazySet;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.filter.sqlmm.Registry;
import org.apache.sis.system.Reflect;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.capability.Conformance;
import org.opengis.filter.capability.IdCapabilities;
import org.opengis.filter.capability.AvailableFunction;
import org.opengis.filter.capability.FilterCapabilities;
import org.opengis.filter.capability.ScalarCapabilities;
import org.opengis.filter.capability.SpatialCapabilities;
import org.opengis.filter.capability.TemporalCapabilities;


/**
 * Metadata about the specific elements that Apache <abbr>SIS</abbr> implementation supports.
 * This is also an unmodifiable map of all functions supported by this factory,
 * together with their providers. This class is thread-safe.
 *
 * @todo Missing {@link SpatialCapabilities} and {@link TemporalCapabilities}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("ReturnOfCollectionOrArrayField")
final class Capabilities extends AbstractMap<String, AvailableFunction>
        implements FilterCapabilities, Conformance, IdCapabilities, ScalarCapabilities
{
    /**
     * The providers of functions. Each {@code FunctionRegister} instance typically provides many functions.
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
     * Declaration of which conformance classes a particular implementation supports.
     */
    @Override
    public Conformance getConformance() {
        return this;
    }

    /**
     * Returns whether the implementation supports the <i>Resource Identification</i> conformance level.
     */
    @Override
    public boolean implementsResourceld() {
        return true;
    }

    /**
     * Provides capabilities used to convey supported identifier operators.
     */
    @Override
    public Optional<IdCapabilities> getIdCapabilities() {
        return Optional.of(this);
    }

    /**
     * Declares the names of the properties used for resource identifiers.
     */
    @Override
    public Collection<LocalName> getResourceIdentifiers() {
        return Set.of(AttributeConvention.IDENTIFIER_PROPERTY.tip());
    }

    /**
     * Returns whether the implementation supports the <i>Standard Filter</i> conformance level.
     * A value of {@code true} means that all the logical operators ({@code And}, {@code Or}, {@code Not})
     * are supported, together with all the standard {@link ComparisonOperatorName}. Those operators shall
     * be listed in the {@linkplain FilterCapabilities#getScalarCapabilities() scalar capabilities}.
     */
    @Override
    public boolean implementsStandardFilter() {
        return true;
    }

    /**
     * Indicates that SIS supports <i>And</i>, <i>Or</i> and <i>Not</i> operators.
     */
    @Override
    public boolean hasLogicalOperators() {
        return true;
    }

    /**
     * Advertises which logical, comparison and arithmetic operators the service supports.
     */
    @Override
    public Optional<ScalarCapabilities> getScalarCapabilities() {
        return Optional.of(this);
    }

    /**
     * Advertises that SIS supports all comparison operators.
     */
    @Override
    public Set<ComparisonOperatorName> getComparisonOperators() {
        return new CodeListSet<>(ComparisonOperatorName.class, true);
    }

    /**
     * Indicates that Apache SIS supports the <i>Spatial Filter</i> conformance level.
     *
     * @todo Need to implement {@linkplain FilterCapabilities#getSpatialCapabilities() temporal capabilities}.
     */
    @Override
    public boolean implementsSpatialFilter() {
        return true;
    }

    /**
     * Indicates that Apache SIS supports the <i>Temporal Filter</i> conformance level.
     *
     * @todo Need to implement {@linkplain FilterCapabilities#getTemporalCapabilities() temporal capabilities}.
     */
    @Override
    public boolean implementsTemporalFilter() {
        return true;
    }

    /**
     * Indicates that Apache SIS supports the <i>Sorting</i> conformance level.
     */
    @Override
    public boolean implementsSorting() {
        return true;
    }

    /**
     * Enumerates the functions that may be used in filter expressions.
     *
     * @return the function that may be used in filter expressions.
     */
    @Override
    public Map<String, AvailableFunction> getFunctions() {
        return this;
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
     * If many providers define a function of the given name, then there is
     * no guaranteed about which provider is returned.
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
     * Always returns {@code false} since we should always have at least the SQLMM function register.
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * Returns the number of functions.
     */
    @Override
    @SuppressWarnings("empty-statement")
    public synchronized int size() {
        while (nextFunctionRegister() != null);     // Ensure that all providers have been loaded.
        return functionNames.length;
    }

    /**
     * Returns whether this map contains a function of the given name, ignoring case.
     *
     * @param  key  name of the function to search.
     * @return whether this map contains a function of the given name, ignoring case.
     */
    @Override
    public boolean containsKey(final Object key) {
        if (key instanceof String) {
            synchronized (this) {
                return indexOfFunction((String) key) >= 0;
            }
        }
        return false;
    }

    /**
     * Returns the description of the first function (in iteration order) of the given name.
     * This method finds the register, then delegates to {@link FunctionRegister#describe(String)}.
     *
     * @param  key  name of the function to describe.
     * @return description of the requested function, or {@code null} if none.
     */
    @Override
    public AvailableFunction get(final Object key) {
        if (key instanceof String) {
            String name = (String) key;
            FunctionRegister register;
            synchronized (this) {
                int i = indexOfFunction(name);
                if (i < 0) return null;
                while (i != 0 && name.equalsIgnoreCase(functionNames[i-1])) i--;
                name = functionNames[i];
                register = registerForName[i];
            }
            return register.describe(name);
        }
        return null;
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

    /**
     * Returns an iterator over the entries in this map.
     * This is a lazy iterator which will load providers only if requested.
     */
    @Override
    protected EntryIterator<String, AvailableFunction> entryIterator() {
        final Iterator<FunctionRegister> it = providers.iterator();
        return new EntryIterator<>() {
            /** Iterator over the names of functions of the current provider. */
            private Iterator<String> names;

            /** The next name to return. */
            private String next;

            /** Returns whether there is a next element. */
            @Override protected boolean next() {
                for (;;) {
                    if (names != null && names.hasNext()) {
                        next = names.next();
                        return true;
                    }
                    names = null;
                    if (!it.hasNext()) {
                        next = null;
                        return false;
                    }
                    names = it.next().getNames().iterator();
                }
            }

            /** Returns the current function name. */
            @Override protected String getKey() {
                return next;
            }

            /** Returns the current function description. */
            @Override protected AvailableFunction getValue() {
                return get(next);
            }
        };
    }
}
