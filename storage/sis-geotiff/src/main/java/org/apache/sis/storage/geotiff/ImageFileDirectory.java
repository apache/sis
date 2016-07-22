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
package org.apache.sis.storage.geotiff;


/**
 * An Image File Directory (FID) in a TIFF image.
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class ImageFileDirectory {
    /**
     * The offset (in bytes since the beginning of the TIFF stream) of the Image File Directory (FID).
     * The directory may be at any location in the file after the header.
     * In particular, an Image File Directory may follow the image that it describes.
     */
    final long offset;

    /**
     * The size of the image described by this FID, or 0 if the information has not been found.
     * Should be interpreted as an unsigned value.
     */
    private int imageWidth, imageHeight;

    /**
     * The size of each tile, or 0 if the information has not be found.
     * Should be interpreted as an unsigned value.
     */
    private int tileWidth, tileHeight;

    private int samplesPerPixel;

    /**
     * Creates a new image file directory located at the given offset (in bytes) in the TIFF file.
     */
    ImageFileDirectory(final long offset) {
        this.offset = offset;
    }

    void addEntry(final int tag, final int type, final long value) {
        // TODO
    }
}
