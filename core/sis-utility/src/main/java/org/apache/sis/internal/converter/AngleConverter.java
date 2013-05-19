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
import net.jcip.annotations.Immutable;
import org.apache.sis.measure.Angle;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.math.FunctionProperty;


/**
 * Handles conversions between {@link Angle} and {@link Double}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
@Immutable
public final class AngleConverter extends SystemConverter<Angle,Double> {
    /** For cross-version compatibility. */
    private static final long serialVersionUID = -5124032874967170238L;

    /** The unique instance. */
    static final AngleConverter INSTANCE = new AngleConverter();

    /** Creates a new converter. */
    public AngleConverter() { // Instantiated by ServiceLoader.
        super(Angle.class, Double.class);
    }

    /** Returns the unique instance. */
    @Override public ObjectConverter<Angle,Double> unique() {
        return INSTANCE;
    }

    /** Returns the inverse converter. */
    @Override public ObjectConverter<Double,Angle> inverse() {
        return Inverse.INSTANCE;
    }

    /** Declares that the converter is bijective. */
    @Override public Set<FunctionProperty> properties() {
        return bijective();
    }

    /** Converts the given angle. */
    @Override public Double apply(final Angle object) {
        return object.degrees();
    }


    /**
     * The inverse of {@link AngleConverter}.
     */
    @Immutable
    public static final class Inverse extends SystemConverter<Double,Angle> {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -1736966474591258159L;

        /** The unique instance. */
        static final Inverse INSTANCE = new Inverse();

        /** Creates a new converter. */
        public Inverse() { // Instantiated by ServiceLoader.
            super(Double.class, Angle.class);
        }

        /** Returns the unique instance. */
        @Override public ObjectConverter<Double,Angle> unique() {
            return INSTANCE;
        }

        /** Returns the inverse converter. */
        @Override public ObjectConverter<Angle,Double> inverse() {
            return AngleConverter.INSTANCE;
        }

        /** Declares that the converter is bijective. */
        @Override public Set<FunctionProperty> properties() {
            return bijective();
        }

        /** Converts the given angle. */
        @Override public Angle apply(final Double object) {
            return new Angle(object);
        }
    }
}
