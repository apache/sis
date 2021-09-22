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
package org.apache.sis.storage;

import java.lang.ref.SoftReference;
import java.awt.image.RenderedImage;
import java.awt.image.ImagingOpException;
import org.opengis.coverage.CannotEvaluateException;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;


/**
 * Time when the "physical" loading of raster data should happen. Some resource implementations may
 * not load data immediately when the {@linkplain GridCoverageResource#read read method} is invoked,
 * but instead defer the actual loading until the {@linkplain GridCoverage#render image is rendered}.
 * This enumeration gives some control over the time when data loading happens.
 * The different strategies are compromises between memory consumption,
 * redundant loading of same data and early error detection.
 *
 * <p>Enumeration values are ordered from the most eager strategy to the laziest strategy.
 * The eager strategy is fastest when all pixels are used, at the cost of largest memory consumption.
 * The lazy strategy is more efficient when only a few tiles will be used and those tiles are not known in advance.
 * The lazy strategy is also the only applicable one if the image is too large for holding in memory.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public enum RasterLoadingStrategy {
    /**
     * Raster data are loaded at {@link GridCoverageResource#read(GridGeometry, int...)} invocation time.
     * This is the most eager loading strategy.
     *
     * <p><b>Advantages:</b></p>
     * <ul>
     *   <li>Fastest loading strategy if all pixels in the returned {@link GridCoverage} will be used.</li>
     *   <li>No redundant data loading: all data stay in memory until the resource is garbage-collected.</li>
     *   <li>Immediate error notification with a checked {@link DataStoreException}.</li>
     * </ul>
     *
     * <p><b>Disadvantages:</b></p>
     * <ul>
     *   <li>Slower than other strategies if only a subset of the returned {@link GridCoverage} will be used.</li>
     *   <li>Consume memory for the full {@link GridCoverage} as long as the resource is referenced.</li>
     * </ul>
     */
    AT_READ_TIME,

    /**
     * Raster data are loaded at {@link GridCoverage#render(GridExtent)} invocation time.
     * Speed and memory usage are at an intermediate level between {@link #AT_READ_TIME} and {@link #AT_GET_TILE_TIME}.
     *
     * <p><b>Advantages:</b></p>
     * <ul>
     *   <li>Faster than {@link #AT_GET_TILE_TIME} if all pixels in the returned {@link RenderedImage} will be used.</li>
     *   <li>Load less data than {@link #AT_READ_TIME} if the {@link GridExtent} is a sub-region of the coverage.</li>
     *   <li>Memory is released sooner (when the {@link RenderedImage} is garbage-collected) than above mode.</li>
     * </ul>
     *
     * <p><b>Disadvantages:</b></p>
     * <ul>
     *   <li>Slower than {@link #AT_GET_TILE_TIME} if only a few tiles will be used.</li>
     *   <li>Consume memory for the full {@link RenderedImage} as long as the image is referenced.</li>
     *   <li>May reload the same data many times if images are discarded and recreated.</li>
     *   <li>Unchecked {@link CannotEvaluateException} can happen relatively late
     *       (at {@code render(…)} invocation time) in a chain of coverage operations.</li>
     * </ul>
     */
    AT_RENDER_TIME,

    /**
     * Raster data are loaded at {@link RenderedImage#getTile(int, int)} invocation time.
     * This is the laziest loading strategy.
     * This is also the only strategy that can handle very large {@link RenderedImage}s.
     *
     * <p><b>Advantages:</b></p>
     * <ul>
     *   <li>Only the tiles that are actually used are loaded.</li>
     *   <li>Memory can be released at any time (tiles are kept by {@linkplain SoftReference soft references}).</li>
     * </ul>
     *
     * <p><b>Disadvantages:</b></p>
     * <ul>
     *   <li>Slower read operations (numerous seeks when tiles are read in random order).</li>
     *   <li>May reload the same data many times if tiles are discarded and recreated.</li>
     *   <li>Unchecked {@link ImagingOpException} can happen late
     *       (at {@code getTile(…)} invocation time) in a chain of image operations.</li>
     * </ul>
     */
    AT_GET_TILE_TIME
}
