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
package org.apache.sis.internal.geotiff;


/**
 * Possible values for {@link org.apache.sis.storage.geotiff.Tags#Predictor}.
 * A predictor is a mathematical operator that is applied to the image data
 * before an encoding scheme is applied.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public enum Predictor {
    /**
     * No prediction scheme used before coding.
     */
    NONE,

    /**
     * Horizontal differencing.
     */
    HORIZONTAL,

    /**
     * Floating point prediction.
     */
    FLOAT,

    /**
     * Predictor code is not recognized.
     */
    UNKNOWN;

    /**
     * Returns the predictor for the given code.
     *
     * @param  code  value associated to TIFF "predictor" tag.
     * @return predictor for the given code.
     */
    public static Predictor valueOf(final int code) {
        switch (code) {
            case 1:  return NONE;
            case 2:  return HORIZONTAL;
            case 3:  return FLOAT;
            default: return UNKNOWN;
        }
    }
}
