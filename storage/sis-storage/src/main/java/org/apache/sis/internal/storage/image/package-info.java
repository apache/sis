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

/**
 * {@link org.apache.sis.storage.DataStore} implementation for Image I/O.
 * This data store wraps Image I/O reader and wrapper for image format such as TIFF, PNG or JPEG.
 * The data store delegates the reading and writing of pixel values to the wrapped reader or writer,
 * and additionally looks for two small text files in the same directory than the image file
 * with the same filename but a different extension:
 *
 * <ul class="verbose">
 *   <li>A text file containing the coefficients of the affine transform mapping pixel
 *       coordinates to geodesic coordinates. The reader expects one coefficient per line,
 *       in the same order than the one expected by the
 *       {@link java.awt.geom.AffineTransform#AffineTransform(double[]) AffineTransform(double[])}
 *       constructor, which is <var>scaleX</var>, <var>shearY</var>, <var>shearX</var>,
 *       <var>scaleY</var>, <var>translateX</var>, <var>translateY</var>.
 *       The reader looks for a file having the following extensions, in preference order:
 *       <ol>
 *         <li>The first letter of the image file extension, followed by the last letter of
 *             the image file extension, followed by {@code 'w'}. Example: {@code "tfw"} for
 *             {@code "tiff"} images, and {@code "jgw"} for {@code "jpeg"} images.</li>
 *         <li>The extension of the image file with a {@code 'w'} appended.</li>
 *         <li>The {@code "wld"} extension.</li>
 *       </ol>
 *   </li>
 *   <li>A text file containing the <cite>Coordinate Reference System</cite> (CRS)
 *       definition in <cite>Well Known Text</cite> (WKT) syntax. The reader looks
 *       for a file having the {@code ".prj"} extension.</li>
 * </ul>
 *
 * Every text file are expected to be encoded in ISO-8859-1 (a.k.a. ISO-LATIN-1)
 * and every numbers are expected to be formatted in US locale.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see <a href="https://en.wikipedia.org/wiki/World_file">World File Format Description</a>
 *
 * @since 1.2
 * @module
 */
package org.apache.sis.internal.storage.image;
