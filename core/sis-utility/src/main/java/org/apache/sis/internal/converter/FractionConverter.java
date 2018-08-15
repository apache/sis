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
package org.apache.sis.internal.converter;

import java.util.Set;
import org.apache.sis.math.Fraction;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.resources.Errors;


/**
 * Handles conversions from {@link Fraction} to other kind of numbers.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final class FractionConverter extends SystemConverter<Fraction,Integer> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 862676960870569201L;

    /**
     * The unique instance of this converter.
     */
    public static final FractionConverter INSTANCE = new FractionConverter();

    /**
     * Creates a new converter. Only one instance is enough, but this constructor
     * needs to be public for allowing invocation by {@link java.util.ServiceLoader}.
     */
    public FractionConverter() {
        super(Fraction.class, Integer.class);
    }

    /**
     * Returns the unique instance of this converter.
     *
     * @return the unique instance of this converter.
     */
    @Override
    public ObjectConverter<Fraction,Integer> unique() {
        return INSTANCE;
    }

    /**
     * Declares that this converter is injective, surjective, invertible and preserve order.
     *
     * @return the properties of this bijective converter.
     */
    @Override
    public Set<FunctionProperty> properties() {
        return bijective();
    }

    /**
     * Converts the given fraction to an integer.
     *
     * @param  value  the fraction to convert.
     * @return the given fraction as an integer.
     * @throws UnconvertibleObjectException if the given fraction is not an integer.
     */
    @Override
    public Integer apply(final Fraction value) throws UnconvertibleObjectException {
        if ((value.numerator % value.denominator) == 0) {
            return value.numerator / value.denominator;
        }
        throw new UnconvertibleObjectException(Errors.format(Errors.Keys.CanNotConvertValue_2, value, Integer.class));
    }

    /**
     * Returns the converter from integers to fractions.
     *
     * @return the inverse converter.
     */
    @Override
    public ObjectConverter<Integer,Fraction> inverse() {
        return FromInteger.INSTANCE;
    }

    /**
     * The inverse of {@link FractionConverter}.
     */
    public static final class FromInteger extends SystemConverter<Integer,Fraction> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 7411811921783941007L;

        /**
         * The unique instance of this converter.
         */
        public static final FromInteger INSTANCE = new FromInteger();

        /**
         * Creates a new converter. Only one instance is enough, but this constructor
         * needs to be public for allowing invocation by {@link java.util.ServiceLoader}.
         */
        public FromInteger() {
            super(Integer.class, Fraction.class);
        }

        @Override public ObjectConverter<Integer,Fraction> unique()  {return INSTANCE;}
        @Override public ObjectConverter<Fraction,Integer> inverse() {return FractionConverter.INSTANCE;}
        @Override public Set<FunctionProperty> properties()          {return bijective();}

        /**
         * Creates a new fraction from the given integer.
         *
         * @param  value  the integer to convert.
         * @return a fraction equals to the given integer.
         */
        @Override
        public Fraction apply(Integer value) {
            return new Fraction(value, 1);
        }
    }
}
