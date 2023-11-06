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
package org.apache.sis.storage.geotiff.base;

import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.*;
import org.apache.sis.util.resources.Errors;


/**
 * Possible values for {@code BaselineTIFFTagSet.TAG_PREDICTOR}.
 * A predictor is a mathematical operator that is applied to the image data
 * before an encoding scheme is applied.
 *
 * <p>This enumeration contains more values than what the Apache SIS reader and writer can support.
 * This enumeration is not put in public API for that reason.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public enum Predictor {
    /**
     * No prediction scheme used before coding.
     */
    NONE(PREDICTOR_NONE),

    /**
     * Horizontal differencing.
     */
    HORIZONTAL_DIFFERENCING(PREDICTOR_HORIZONTAL_DIFFERENCING),

    /**
     * Floating point prediction.
     */
    FLOAT(3),

    /**
     * Predictor code is not recognized.
     */
    UNKNOWN(0);

    /**
     * The TIFF code for this predictor.
     */
    public final int code;

    /**
     * Creates a new predictor enumeration.
     */
    private Predictor(final int code) {
        this.code = code;
    }

    /**
     * Returns the predictor for the given code.
     *
     * @param  code  value associated to TIFF "predictor" tag.
     * @return predictor for the given code.
     */
    public static Predictor valueOf(final int code) {
        switch (code) {
            case PREDICTOR_NONE: return NONE;
            case PREDICTOR_HORIZONTAL_DIFFERENCING: return HORIZONTAL_DIFFERENCING;
            case 3:  return FLOAT;
            default: return UNKNOWN;
        }
    }

    /**
     * Returns the predictor for the given code if supported.
     *
     * @param  code  value associated to TIFF "predictor" tag.
     * @return predictor for the given code.
     * @throws IllegalArgumentException if the given code is unsupported.
     */
    public static Predictor supported(final int code) {
        final Predictor value = valueOf(code);
        if (value.ordinal() <= HORIZONTAL_DIFFERENCING.ordinal()) {
            return value;
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedArgumentValue_1, code));
    }
}
