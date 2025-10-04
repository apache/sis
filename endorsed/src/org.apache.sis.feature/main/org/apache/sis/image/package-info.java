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
 * Provides helper classes for handling Java2D rendered images together with some operations.
 * This package does not provide any geospatial functionalities;
 * it works only on sample or pixel values stored in {@link java.awt.image.RenderedImage}s.
 * Those rendered images have the following capabilities:
 *
 * <ul>
 *   <li>Images may have an arbitrary number of bands (not necessarily RGB).</li>
 *   <li>Sample values can be bytes, shorts (signed or unsigned), integers or floating-point values.</li>
 *   <li>Images can be tiled.</li>
 * </ul>
 *
 * This package is used as a basis for
 * {@linkplain org.apache.sis.metadata.iso.spatial.DefaultGeorectified georectified} or
 * {@linkplain org.apache.sis.metadata.iso.spatial.DefaultGeoreferenceable georeferenceable}
 * <i>grid coverages</i>.
 *
 * <h2>Usage note</h2>
 * Some images are writable. But modifying pixel values should be done by invoking the {@code getWritableTile(…)}
 * and {@code releaseWritableTile(…)} methods of {@link java.awt.image.WritableRenderedImage} interface.
 * Do not cast directly a {@link java.awt.image.Raster} to {@link java.awt.image.WritableRaster}
 * even when the cast is safe, because some raster data may be shared by many tiles having identical content.
 * Furthermore, changes in pixel values may be lost if {@code releaseWritableTile(…)} is not invoked.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.6
 * @since   1.0
 */
package org.apache.sis.image;
