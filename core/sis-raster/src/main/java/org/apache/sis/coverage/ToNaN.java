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
package org.apache.sis.coverage;

import java.util.HashSet;
import java.util.function.DoubleToIntFunction;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.internal.raster.Resources;


/**
 * Keep trace of allocated {@link Float#NaN} ordinal values for avoiding range collisions when building categories.
 * This is a temporary object used only at {@link SampleDimension} construction time for producing values suitable
 * to {@link MathFunctions#toNanFloat(int)}. Instances of this class are given for {@code toNaN} argument in the
 * {@link Category} constructor.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@SuppressWarnings({"CloneableClassWithoutClone", "serial"})         // Not intended to be cloned or serialized.
final class ToNaN extends HashSet<Integer> implements DoubleToIntFunction {
    /**
     * The value which should be assigned ordinal 0 if that ordinal value is available.
     * For performance reason, the background value should be assigned ordinal 0 when possible.
     */
    double background;

    /**
     * To be constructed only from this package.
     */
    ToNaN() {
        background = Double.NaN;
    }

    /**
     * Returns a NaN ordinal value for the given sample value.
     * The returned value can be given to {@link MathFunctions#toNanFloat(int)}.
     */
    @Override
    public int applyAsInt(final double value) {
        if (value == background && add(0)) {
            return 0;
        }
        /*
         * For qualitative category, we need an ordinal in the [MIN_NAN_ORDINAL … MAX_NAN_ORDINAL] range.
         * This range is quite large (a few million of values) so using the sample directly usually work.
         * If it does not work, we will use an arbitrary value in that range.
         */
        int ordinal = Math.round((float) value);
        if (ordinal > MathFunctions.MAX_NAN_ORDINAL) {
            ordinal = (MathFunctions.MAX_NAN_ORDINAL + 1) / 2;
        } else if (ordinal < MathFunctions.MIN_NAN_ORDINAL) {
            ordinal = MathFunctions.MIN_NAN_ORDINAL / 2;
        }
search: if (!add(ordinal)) {
            /*
             * Following algorithms are inefficient, but those loops should be rarely needed.
             * They are executed only if many qualitative sample values are outside the range
             * of ordinal NaN values. The range allows a few million of values.
             */
            if (ordinal >= 0) {
                do if (add(++ordinal)) break search;
                while (ordinal < MathFunctions.MAX_NAN_ORDINAL);
            } else {
                do if (add(--ordinal)) break search;
                while (ordinal > MathFunctions.MIN_NAN_ORDINAL);
            }
            throw new IllegalStateException(Resources.format(Resources.Keys.TooManyQualitatives));
        }
        return ordinal;
    }
}
