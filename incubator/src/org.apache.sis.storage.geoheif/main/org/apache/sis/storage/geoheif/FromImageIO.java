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
package org.apache.sis.storage.geoheif;

import java.io.IOException;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.IllegalOpenParameterException;
import org.apache.sis.storage.UnsupportedEncodingException;
import org.apache.sis.storage.isobmff.ByteReader;
import org.apache.sis.util.ArraysExt;


/**
 * An image read with Image I/O. This is mainly for <abbr>JPEG</abbr> compression,
 * but this class can be used with any format supported by Image I/O.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
final class FromImageIO extends Image {
    /**
     * Index of the image to read.
     */
    private static final int IMAGE_INDEX = 0;

    /**
     * Provider of image readers.
     */
    private final ImageReaderSpi provider;

    /**
     * Creates a new tile.
     *
     * @param  builder   helper class for building the grid geometry and sample dimensions.
     * @param  locator   the provider of bytes to read from the <abbr>ISOBMFF</abbr> box.
     * @param  provider  provider of image readers for decoding the payload.
     * @param  name      a name that identifies this image, for debugging purpose.
     * @throws RasterFormatException if the sample model cannot be created.
     */
    FromImageIO(final CoverageBuilder builder, final ByteReader locator, final ImageReaderSpi provider, final String name) {
        super(builder, locator, name);
        this.provider = provider;
    }

    /**
     * First an image reader provider by format name.
     *
     * @param  format  name of the desired format.
     * @return image provider for the given format.
     * @throws UnsupportedEncodingException if no provider was found for the specified format.
     */
    static ImageReaderSpi byFormatName(final String format) throws UnsupportedEncodingException {
        var rg = IIORegistry.getDefaultInstance();
        var it = rg.getServiceProviders(
                ImageReaderSpi.class,
                (spi) -> ArraysExt.containsIgnoreCase(((ImageReaderSpi) spi).getFormatNames(), format),
                true);
        if (it.hasNext()) {
            return it.next();
        } else {
            throw new UnsupportedEncodingException("Could not find a JPEG reader.");
        }
    }

    /**
     * Sets the input of the given reader to an input stream positioned to the beginning of the image.
     *
     * @param  store   the store that opened the <abbr>HEIF</abbr> file.
     * @param  reader  the image reader for which to set the input.
     * @throws DataStoreException if the input cannot be set because of its type.
     * @throws IOException if an I/O error occurred while setting the input.
     */
    private void setReaderInput(final GeoHeifStore store, final ImageReader reader) throws DataStoreException, IOException {
        final var request = new ByteReader.FileRegion();
        request.input  = store.ensureOpen();
        request.length = -1;
        locator.resolve(request);
        request.input.seek(request.offset);
        request.input.buffer.order(byteOrder);
        try {
            reader.setInput(request.input, true, true);
        } catch (IllegalArgumentException e) {
            throw new IllegalOpenParameterException("Not an image input stream.", e);
        }
    }

    /**
     * Returns the sample model and color model of this image.
     * The size of the sample model is the tile size.
     *
     * @param  store   the store that opened the <abbr>HEIF</abbr> file.
     * @throws DataStoreContentException if this image does not include information about the sample/color models.
     * @throws DataStoreException if the input cannot be set because of its type.
     * @throws IOException if an I/O error occurred while setting the input.
     */
    @Override
    protected ImageTypeSpecifier getImageType(final GeoHeifStore store) throws DataStoreException, IOException {
        final ImageReader reader = provider.createReaderInstance();
        setReaderInput(store, reader);
        final var it = reader.getImageTypes(IMAGE_INDEX);
        ImageTypeSpecifier specifier;
        if (it.hasNext()) {
            specifier = it.next();
        } else {
            return super.getImageType(store);
        }
        final int width  = reader.getTileWidth (IMAGE_INDEX);
        final int height = reader.getTileHeight(IMAGE_INDEX);
        SampleModel sampleModel = specifier.getSampleModel();
        if (sampleModel.getWidth() != width || sampleModel.getHeight() != height) {
            sampleModel = specifier.getSampleModel(width, height);
            specifier = new ImageTypeSpecifier(specifier.getColorModel(), sampleModel);
        }
        reader.dispose();
        return specifier;
    }

    /**
     * Reads a single tile.
     *
     * @param  store    the data store reading a tile.
     * @param  tileX    0-based column index of the tile to read, starting from image left.
     * @param  tileY    0-based column index of the tile to read, starting from image top.
     * @param  context  contains the target raster or the image reader to use.
     * @return tile filled with the pixel values read by this method.
     */
    @Override
    protected Raster readTile(final GeoHeifStore store, final long tileX, final long tileY,
            final ImageResource.Coverage.ReadContext context) throws IOException, DataStoreException
    {
        final ImageReader reader = context.getReader(provider);
        setReaderInput(store, reader);
        final BufferedImage image;
        try {
            image = reader.readTile(IMAGE_INDEX, Math.toIntExact(tileX), Math.toIntExact(tileY));
        } finally {
            reader.setInput(null);
        }
        return image.getRaster();
    }
}
