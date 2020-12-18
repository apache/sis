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
package org.apache.sis.swing;

import java.awt.geom.Point2D;


/**
 * An interface for viewers that may be deformed by some artefacts. For example the {@link ZoomPane}
 * viewer is capable to show a {@linkplain ZoomPane#setMagnifierVisible magnifying glass} on top of
 * the usual content. The presence of a magnifying glass deforms the viewer in that the apparent
 * position of pixels within the glass are moved. This interface allows for corrections of apparent
 * pixel position in order to get the position we would have if no deformations existed.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
public interface DeformableViewer {
    /**
     * Corrects a pixel coordinates for removing the effect of the magnifying glass. Without this
     * method, transformations from pixels to geographic coordinates would not give accurate results
     * for pixels inside the magnifying glass because the glass moves the apparent pixel position.
     * Invoking this method removes deformation effects using the following steps:
     *
     * <ul>
     *   <li>If the given pixel coordinates are outside the magnifying glass,
     *       then this method do nothing.</li>
     *   <li>Otherwise, this method update {@code point} in such a way that it contains the position
     *       that the same pixel would have in the absence of magnifying glass.</li>
     * </ul>
     *
     * @param point  on input, a pixel coordinates as it appears on the screen. On output, the
     *        coordinates that the same pixel would have if the magnifying glass was not presents.
     */
    void correctApparentPixelPosition(Point2D point);
}
