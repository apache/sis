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
package org.apache.sis.storage.isobmff.image;

import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;
import org.apache.sis.storage.isobmff.base.ItemPropertyContainer;


/**
 * A property containing the width and height of the associated image item.
 * Every image item shall be associated with one property of this type,
 * prior to the association of all transformative properties.
 *
 * <h4>Container</h4>
 * The container can be a {@link ItemPropertyContainer} box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ImageSpatialExtents extends FullBox {
    /**
     * Numerical representation of the {@code "ispe"} box type.
     */
    public static final int BOXTYPE = ((((('i' << 8) | 's') << 8) | 'p') << 8) | 'e';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * The width of the reconstructed image in pixels.
     */
    @Interpretation(Type.UNSIGNED)
    public final int imageWidth;

    /**
     * The height of the reconstructed image in pixels.
     */
    @Interpretation(Type.UNSIGNED)
    public final int imageHeight;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     */
    public ImageSpatialExtents(final Reader reader) throws IOException, UnsupportedVersionException {
        super(reader);
        requireVersionZero();
        final ChannelDataInput input = reader.input;
        imageWidth  = input.readInt();
        imageHeight = input.readInt();
    }
}
