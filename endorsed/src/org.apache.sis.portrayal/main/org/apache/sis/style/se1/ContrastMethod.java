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
package org.apache.sis.style.se1;


/**
 * Contrast enhancement for an image channel.
 * In the case of a color image, the relative grayscale brightness of a pixel color is used.
 *
 * @author  Johann Sorel (Geomatys)
 */
public enum ContrastMethod {
    /**
     * Dimmest color is stretched to black and the brightest color is stretched to white.
     * All colors in between are stretched out linearly.
     */
    NORMALIZE,

    /**
     * Contrast based on a histogram of how many colors are at each brightness level on input.
     * The goal is to produce equal number of pixels in the image at each brightness level on output.
     * This has the effect of revealing many subtle ground features.
     */
    HISTOGRAM,

    /**
     * Contrast based on a gamma value.
     * A gamma value tells how much to brighten (value greater than 1)
     * or dim (value less than 1) an image, with 1 meaning no change.
     */
    GAMMA,

    /**
     * No enhancement.
     * This is the default value.
     */
    NONE;
}
