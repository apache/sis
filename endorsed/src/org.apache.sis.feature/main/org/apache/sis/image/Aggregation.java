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
package org.apache.sis.image;


/**
 * Algorithm for image resampling based on aggregation methods.
 * Aggregating is the action of computing the pixel values of an image
 * from the values of overlapping pixels in one or many source images.
 * The aggregation is performed by the {@link AggregatedImage} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.8
 *
 * @see AggregatedImage
 * @see Interpolation
 *
 * @since SIS 1.8
 */
public class Aggregation {
    /**
     * Creates a new aggregation method.
     */
    Aggregation() {
    }

    /**
     * Aggregation calculated by averages of pixel values with raster-space weighting.
     * For each pixel in the aggregated image, the value is computed by the average of all
     * overlapping pixels in source images weighted by the area of overlap in pixel units.
     */
    public static final Aggregation RASTER_SPACE_WEIGHTING = new Aggregation();

    /*
     * Aggregation calculated by averages of pixel values with real world weighting.
     * For each pixel in the aggregated image, the value is computed by the average of all
     * overlapping pixels in source images weighted by the surface area of pixel footprint
     * on the ground, typically in square meters.
     */
//  private static final Aggregation GROUND_AREA_WEIGHTING = new Aggregation();
}
