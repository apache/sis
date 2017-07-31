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

import java.awt.Rectangle;
import java.awt.image.*;

/**
 * Create an appropriate iterator.
 *
 * @author  Rémi Maréchal (Geomatys)
 */
final class PixelIteratorFactory {

    private PixelIteratorFactory() {
    }

    /**
     * Create and return an adapted default {@link Raster} iterator.
     *
     * @param raster   {@link Raster} will be traveled by iterator.
     * @return adapted {@link PixelIterator}.
     */
    public static PixelIterator createReadOnlyIterator(final Raster raster) {
        return PixelIteratorFactory.createReadOnlyIterator(raster, null);
    }

    /**
     * Create and return an adapted default read-only {@link Raster} iterator to read on raster sub-area.
     *
     * @param raster      {@link Raster} will be traveled by iterator from it's sub-area.
     * @param subReadArea {@link Rectangle} which define raster read area.
     * @return adapted    {@link PixelIterator}.
     */
    public static PixelIterator createReadOnlyIterator(final Raster raster, final Rectangle subReadArea) {
//        final SampleModel sampleM = raster.getSampleModel();
//
//        if (sampleM instanceof ComponentSampleModel) {
//            if (checkBankIndices(((ComponentSampleModel)sampleM).getBankIndices())) {
//                switch (sampleM.getDataType()) {
//                    case DataBuffer.TYPE_BYTE  : return new DefaultDirectByteIterator(raster, subReadArea);
//                    case DataBuffer.TYPE_FLOAT : return new DefaultDirectFloatIterator(raster, subReadArea);
//                    default : return new DefaultIterator(raster, subReadArea);
//                }
//            }
//        }
        return new DefaultIterator(raster, subReadArea);
    }

    /**
     * Create and return an adapted default read-only {@link RenderedImage} iterator.
     *
     * @param renderedImage {@link RenderedImage} will be traveled by iterator.
     * @return adapted      {@link PixelIterator}.
     */
    public static PixelIterator createReadOnlyIterator(final RenderedImage renderedImage) {
       return createReadOnlyIterator(renderedImage, null);
    }

    /**
     * Create and return an adapted default read-only {@link RenderedImage} iterator to read on raster sub-area.
     *
     * @param renderedImage {@link RenderedImage} will be traveled by iterator from it's sub-area.
     * @param subReadArea   {@link Rectangle} which define rendered image read area.
     * @return adapted      {@link PixelIterator}.
     */
    public static PixelIterator createReadOnlyIterator(final RenderedImage renderedImage, final Rectangle subReadArea) {
        if (isSingleRaster(renderedImage)){
            return PixelIteratorFactory.createReadOnlyIterator(renderedImage.getTile(renderedImage.getMinTileX(), renderedImage.getMinTileY()), subReadArea);
        }

//        final SampleModel sampleM = renderedImage.getSampleModel();
//        if (sampleM instanceof ComponentSampleModel ) {
//            if (checkBankIndices(((ComponentSampleModel)sampleM).getBankIndices())) {
//                switch (sampleM.getDataType()) {
//                    case DataBuffer.TYPE_BYTE  : return new DefaultDirectByteIterator(renderedImage, subReadArea);
//                    case DataBuffer.TYPE_FLOAT : return new DefaultDirectFloatIterator(renderedImage, subReadArea);
//                    default : return new DefaultIterator(renderedImage, subReadArea);
//                }
//            }
//        }
        return new DefaultIterator(renderedImage, subReadArea);
    }

    //-- NOT IMPLEMENTED YET
//
//    /**
//     * Create and return an adapted default read and write raster iterator.
//     *
//     * @param raster          {@link Raster} will be traveled by read-only iterator.
//     * @param writeableRaster {@link WritableRaster} raster wherein value is set (write).
//     * @return adapted        {@link PixelIterator} .
//     */
//    public static PixelIterator createDefaultWriteableIterator(final Raster raster, final WritableRaster writeableRaster) {
//        return createDefaultWriteableIterator(raster, writeableRaster, null);
//    }
//
//    /**
//     * Create and return an adapted default read and write raster iterator.
//     * Iterator move in a raster sub-area.
//     *
//     * @param raster          {@link Raster} will be traveled by read-only iterator.
//     * @param writeableRaster {@link WritableRaster} raster wherein value is set (write).
//     * @param subArea     {@link Rectangle} which define raster read and write area.
//     * @return adapted        {@link PixelIterator}.
//     */
//    public static PixelIterator createDefaultWriteableIterator(final Raster raster, final WritableRaster writeableRaster, final Rectangle subArea) {
//        final SampleModel srcSampleM = raster.getSampleModel();
//        final SampleModel destSampleM = raster.getSampleModel();
//        PixelIterator.checkRasters(raster, writeableRaster);
//
//        if (srcSampleM instanceof ComponentSampleModel && destSampleM instanceof ComponentSampleModel) {
//            ComponentSampleModel srcCSModel = (ComponentSampleModel) srcSampleM;
//            ComponentSampleModel destCSModel = (ComponentSampleModel) destSampleM;
//
//            // Source and destination image must have identical structure in order to allow a single iterator to move through them.
//            if (checkBankIndices(srcCSModel.getBankIndices()) && checkBankIndices(destCSModel.getBankIndices())
//             && Arrays.equals(srcCSModel.getBandOffsets(), destCSModel.getBandOffsets())
//             && srcCSModel.getPixelStride() == destCSModel.getPixelStride()
//             && srcCSModel.getScanlineStride() == destCSModel.getScanlineStride()) {
//
//                switch (srcSampleM.getDataType()) {
//                    case DataBuffer.TYPE_BYTE  : return new DefaultWritableDirectByteIterator(raster, writeableRaster, subArea);
//                    case DataBuffer.TYPE_FLOAT : return new DefaultWritableDirectFloatIterator(raster, writeableRaster, subArea);
//                    default : return new DefaultWritableIterator(raster, writeableRaster, subArea);
//                }
//            }
//        }
//        return new DefaultWritableIterator(raster, writeableRaster, subArea);
//    }
//
//    /**
//     * Create and return an adapted default read and write rendered image iterator.
//     *
//     * @param renderedImage         {@link RenderedImage} will be traveled by iterator.
//     * @param writableRenderedImage {@link WritableRenderedImage} rendered image wherein value is set (write).
//     * @return adapted              {@link PixelIterator}.
//     */
//    public static PixelIterator createDefaultWriteableIterator(final RenderedImage renderedImage, final WritableRenderedImage writableRenderedImage) {
//        return createDefaultWriteableIterator(renderedImage, writableRenderedImage, null);
//    }
//
//    /**
//     * Create and return an adapted default read and write rendered image iterator from it's sub-area.
//     *
//     * @param renderedImage         {@link RenderedImage} will be traveled by iterator from it's sub-area.
//     * @param writableRenderedImage {@link WritableRenderedImage} rendered image wherein value is set (write).
//     * @param subArea               {@link Rectangle} which define rendered image read and write area.
//     * @return adapted              {@link PixelIterator}.
//     */
//    public static PixelIterator createDefaultWriteableIterator(final RenderedImage renderedImage, final WritableRenderedImage writableRenderedImage, final Rectangle subArea) {
//        final SampleModel srcSampleM = renderedImage.getSampleModel();
//        final SampleModel destSampleM = renderedImage.getSampleModel();
//
//        if (srcSampleM instanceof ComponentSampleModel && destSampleM instanceof ComponentSampleModel) {
//            ComponentSampleModel srcCSModel = (ComponentSampleModel) srcSampleM;
//            ComponentSampleModel destCSModel = (ComponentSampleModel) destSampleM;
//
//            // Source and destination image must have identical structure in order to allow a single iterator to move through them.
//            if (checkBankIndices(srcCSModel.getBankIndices()) && checkBankIndices(destCSModel.getBankIndices())
//                    && Arrays.equals(srcCSModel.getBandOffsets(), destCSModel.getBandOffsets())
//                    && srcCSModel.getPixelStride() == destCSModel.getPixelStride()
//                    && srcCSModel.getScanlineStride() == destCSModel.getScanlineStride()) {
//
//                switch (srcSampleM.getDataType()) {
//                    case DataBuffer.TYPE_BYTE  : return new DefaultWritableDirectByteIterator(renderedImage, writableRenderedImage, subArea);
//                    case DataBuffer.TYPE_FLOAT : return new DefaultWritableDirectFloatIterator(renderedImage, writableRenderedImage, subArea);
//                    default : return new DefaultWritableIterator(renderedImage, writableRenderedImage, subArea);
//                }
//            }
//        }
//        return new DefaultWritableIterator(renderedImage, writableRenderedImage, subArea);
//    }

    ////////////////////////////// Row Major Iterator ////////////////////////////

//    /**
//     * Create and return an adapted Row Major read-only rendered image iterator.
//     * RowMajor : iterator move forward line per line one by one in downward order.
//     *
//     * @param renderedImage {@link RenderedImage} will be traveled by iterator.
//     * @return adapted      {@link PixelIterator}.
//     */
//    public static PixelIterator createRowMajorIterator(final RenderedImage renderedImage) {
//        return createRowMajorIterator(renderedImage, null);
//    }
//
//    /**
//     * Create and return an adapted Row Major read-only rendered image iterator from it's sub-area.
//     * RowMajor : iterator move forward line per line one by one in downward order.
//     *
//     * @param renderedImage {@link RenderedImage} will be traveled by iterator from it's sub-area.
//     * @param subReadArea   {@link Rectangle} which define rendered image read-only area.
//     * @return adapted      {@link PixelIterator}.
//     */
//    public static PixelIterator createRowMajorIterator(final RenderedImage renderedImage, final Rectangle subReadArea) {
//        final SampleModel sampleM = renderedImage.getSampleModel();
//        if (sampleM instanceof ComponentSampleModel) {
//            if (checkBankIndices(((ComponentSampleModel)sampleM).getBankIndices())) {
//                switch (sampleM.getDataType()) {
//                    case DataBuffer.TYPE_BYTE  : return new RowMajorDirectByteIterator(renderedImage, subReadArea);
//                    case DataBuffer.TYPE_FLOAT : return new RowMajorDirectFloatIterator(renderedImage, subReadArea);
//                    default : return new RowMajorIterator(renderedImage, subReadArea);
//                }
//            }
//        }
//        return new RowMajorIterator(renderedImage, subReadArea);
//    }
//
//    /**
//     * Create and return an adapted Row Major read and write rendered image iterator.
//     * RowMajor : iterator move forward line per line one by one in downward order.
//     *
//     * No build is allowed for single {@link java.awt.image.Raster} browsing, because {@link org.geotoolkit.image.iterator.DefaultDirectIterator}
//     * will do the job as fast as it, and with the same behavior.
//     *
//     * @param renderedImage         {@link RenderedImage} will be traveled by iterator.
//     * @param writableRenderedImage {@link WritableRenderedImage}  rendered image wherein value is set (write).
//     * @return adapted              {@link PixelIterator}.
//     */
//    public static PixelIterator createRowMajorWriteableIterator(final RenderedImage renderedImage, final WritableRenderedImage writableRenderedImage) {
//        return createRowMajorWriteableIterator(renderedImage, writableRenderedImage, null);
//    }
//
//    /**
//     * Create and return an adapted Row Major read and write rendered image iterator from it's sub-area.
//     * RowMajor : iterator move forward line per line one by one in downward order.
//     *
//     * No build is allowed for single {@link java.awt.image.Raster} browsing, because {@link org.geotoolkit.image.iterator.DefaultDirectIterator}
//     * will do the job as fast as it, and with the same behavior.
//     *
//     * @param renderedImage         {@link RenderedImage} will be traveled by iterator from it's sub-area.
//     * @param writableRenderedImage {@link WritableRenderedImage}  rendered image wherein value is set (write).
//     * @param subArea               {@link Rectangle} which define rendered image read and write area.
//     * @return adapted              {@link PixelIterator}.
//     */
//    public static PixelIterator createRowMajorWriteableIterator(final RenderedImage renderedImage, final WritableRenderedImage writableRenderedImage, final Rectangle subArea) {
//        final SampleModel srcSampleM = renderedImage.getSampleModel();
//        final SampleModel destSampleM = renderedImage.getSampleModel();
//
//        if (srcSampleM instanceof ComponentSampleModel && destSampleM instanceof ComponentSampleModel) {
//            ComponentSampleModel srcCSModel = (ComponentSampleModel) srcSampleM;
//            ComponentSampleModel destCSModel = (ComponentSampleModel) destSampleM;
//
//            // Source and destination image must have identical structure in order to allow a single iterator to move through them.
//            if (checkBankIndices(srcCSModel.getBankIndices()) && checkBankIndices(destCSModel.getBankIndices())
//                    && Arrays.equals(srcCSModel.getBandOffsets(), destCSModel.getBandOffsets())
//                    && srcCSModel.getPixelStride() == destCSModel.getPixelStride()
//                    && srcCSModel.getScanlineStride() == destCSModel.getScanlineStride()) {
//
//                switch (srcSampleM.getDataType()) {
//                    case DataBuffer.TYPE_BYTE  : return new RowMajorWritableDirectByteIterator(renderedImage, writableRenderedImage, subArea);
//                    case DataBuffer.TYPE_FLOAT : return new RowMajorWritableDirectFloatIterator(renderedImage, writableRenderedImage, subArea);
//                    default : return new RowMajorWritableIterator(renderedImage, writableRenderedImage, subArea);
//                }
//            }
//        }
//        return new RowMajorWritableIterator(renderedImage, writableRenderedImage, subArea);
//    }

    /**
     * Verify bandOffset table conformity.
     *
     * @param bandOffset band offset table.
     * @return true if bandOffset table is conform else false.
     */
    private static boolean checkBandOffset(int[] bandOffset) {
        for (int i = 0, l = bandOffset.length; i<l; i++) if (bandOffset[i] != i) return false;
        return true;
    }

    /**
     * Check image samples are stored in a single bank.
     *
     * @param bankIndices bank indice table retrieved from input image (see {@link java.awt.image.ComponentSampleModel#getBankIndices()}.
     * @return true if input image use a single bank. false otherwise.
     */
    private static boolean checkBankIndices(int[] bankIndices) {
        if (bankIndices.length == 1) return true;
        for (int i = 1, l = bankIndices.length; i<l; i++) if (bankIndices[i] != bankIndices[i-1]) return false;
        return true;
    }

    private static boolean isSingleRaster(final RenderedImage renderedImage){
        return renderedImage.getNumXTiles()==1 && renderedImage.getNumYTiles()==1;
    }

}
