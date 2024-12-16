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

import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.io.stream.InternalOptionKey;


/**
 * Characteristics of the GeoTIFF file to write.
 * The modifiers can control, for example, the maximal size and number of images that can be stored in a TIFF file.
 *
 * <p>The modifiers can be specified as an option when opening the data store.
 * For example for writing a BigTIFF file, the following code can be used:</p>
 *
 * {@snippet lang="java" :
 *     var file = Path.of("my_output_file.tiff");
 *     var connector = new StorageConnector(file);
 *     var modifiers = new FormatModifier[] {FormatModifier.BIG_TIFF};
 *     connector.setOption(FormatModifier.OPTION_KEY, modifiers);
 *     try (GeoTiffStore ds = new GeoTiffStore(null, connector)) {
 *         // Write data here.
 *     }
 *     }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see GeoTiffStore#getModifiers()
 *
 * @since 1.5
 */
public enum FormatModifier {
    /**
     * The Big TIFF extension (non-standard).
     * When this modifier is absent (which is the default), the standard TIFF format as defined by Adobe is used.
     * That standard uses the addressable space of 32-bits integers, which allows a maximal file size of about 4 GB.
     * When the {@code BIG_TIFF} modifier is present, the addressable space of 64-bits integers is used.
     * The BigTIFF format is non-standard and files written with this option may not be read by all TIFF readers.
     */
    BIG_TIFF,

    // TODO: COG, SPARSE.

    /**
     * Whether to allow the writing of tiles of any size.
     * The TIFF specification requires tile sizes to be multiples of 16 pixels.
     * At reading time, Apache <abbr>SIS</abbr> always accept tiles of any size and this option is ignored.
     * At writing time, by default Apache <abbr>SIS</abbr> checks the tile size and, if not compliant with
     * <abbr>TIFF</abbr> requirement, reorganizes the pixel values in a new tiling before to write image.
     * This reorganization may have a performance cost and consumes more disk space than needed.
     * This option allows to disable this reorganization,
     * which may result in smaller but non-standard TIFF files.
     */
    ANY_TILE_SIZE;

    /**
     * The key for declaring GeoTIFF format modifiers at store creation time.
     * See class Javadoc for usage example.
     *
     * @see StorageConnector#setOption(OptionKey, Object)
     */
    public static final OptionKey<FormatModifier[]> OPTION_KEY = new InternalOptionKey<>("TIFF_MODIFIERS", FormatModifier[].class);
}
