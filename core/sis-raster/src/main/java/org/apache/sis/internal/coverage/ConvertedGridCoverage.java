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
package org.apache.sis.internal.coverage;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;


/**
 * Decorates a {@link GridCoverage} in order to convert sample values on the fly.
 *
 * <p><b>WARNING: this is a temporary class.</b>
 * This class produces a special {@link SampleModel} in departure with the contract documented in JDK javadoc.
 * That sample model does not only define the sample layout (pixel stride, scanline stride, <i>etc.</i>), but
 * also converts the sample values. This may be an issue for optimized pipelines accessing {@link DataBuffer}
 * directly. This class may be replaced by another mechanism (creating new tiles) in a future SIS version.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class ConvertedGridCoverage extends GridCoverage {
    /**
     * Returns a coverage for converted values. If the given coverage is already converted,
     * then this method returns the given {@code coverage} unchanged.
     *
     * @param  packed  the coverage containing packed values to convert.
     * @return the converted coverage. May be {@code coverage}.
     */
    public static GridCoverage convert(final GridCoverage packed) {
        final List<SampleDimension> sds = packed.getSampleDimensions();
        final List<SampleDimension> cfs = new ArrayList<>(sds.size());
        for (SampleDimension sd : sds) {
            cfs.add(sd.forConvertedValues(true));
        }
        return cfs.equals(sds) ? packed : new ConvertedGridCoverage(packed, sds, cfs);
    }

    /**
     * The coverage containing packed values. Sample values will be converted from this coverage.
     */
    private final GridCoverage packed;

    /**
     * Conversions from {@code packed} values to converted values. There is one transform for each band.
     */
    private final MathTransform1D[] toConverted;

    /**
     * Conversions from converted values to {@code packed} values. They are the inverse of {@link #toConverted}.
     */
    private final MathTransform1D[] toPacked;

    /**
     * Whether all transforms in the {@link #toConverted} array are identity transforms.
     */
    private final boolean isIdentity;

    /**
     * Creates a new coverage with the same grid geometry than the given coverage and the given converted sample dimensions.
     */
    private ConvertedGridCoverage(final GridCoverage packed, final List<SampleDimension> sampleDimensions, final List<SampleDimension> converted) {
        super(packed.getGridGeometry(), converted);
        final int numBands = sampleDimensions.size();
        toConverted = new MathTransform1D[numBands];
        toPacked    = new MathTransform1D[numBands];
        boolean isIdentity = true;
        final MathTransform1D identity = (MathTransform1D) MathTransforms.identity(1);
        for (int i = 0; i < numBands; i++) {
            MathTransform1D tr = sampleDimensions.get(i).getTransferFunction().orElse(identity);
            toConverted[i] = tr;
            isIdentity &= tr.isIdentity();
            try {
                tr = tr.inverse();
            } catch (NoninvertibleTransformException ex) {
                tr = (MathTransform1D) MathTransforms.linear(Double.NaN, 0.0);
            }
            toPacked[i] = tr;
        }
        this.isIdentity = isIdentity;
        this.packed     = packed;
    }

    /**
     * Creates a converted view over {@link #packed} data for the given extent.
     *
     * @return the grid slice as a rendered image, as a converted view.
     */
    @Override
    public RenderedImage render(final GridExtent sliceExtent) {
        final RenderedImage render = packed.render(sliceExtent);
        if (isIdentity) {
            return render;
        }
        final Raster raster;
        if (render.getNumXTiles() == 1 && render.getNumYTiles() == 1) {
            raster = render.getTile(render.getMinTileX(), render.getMinTileY());
        } else {
            /*
             * This fallback is very inefficient since it copies all data in one big raster.
             * We will replace this class by tiles management in a future Apache SIS version.
             */
            raster = render.getData();
        }
        final SampleModel baseSm = raster.getSampleModel();
        final DataBuffer dataBuffer = raster.getDataBuffer();
        final ConvertedSampleModel convSm = new ConvertedSampleModel(baseSm, toConverted, toPacked);
        final WritableRaster convRaster = WritableRaster.createWritableRaster(convSm, dataBuffer, null);
        /*
         * The default color models have a lot of constraints. Use a custom model with relaxed rules instead.
         * We arbitrarily use the range of values of the first band only; a future Apache SIS version will
         * need to perform another calculation.
         */
        final ColorModel cm = new ConvertedColorModel(getSampleDimensions().get(0).getSampleRange().get());
        return new BufferedImage(cm, convRaster, false, null);
    }

    /**
     * Returns the packed coverage if {@code converted} is {@code false}, or {@code this} otherwise.
     */
    @Override
    public GridCoverage forConvertedValues(final boolean converted) {
        return converted ? this : packed;
    }

    /**
     * A sample model which convert sample values on the fly.
     *
     * <p><b>WARNING: this is a temporary class.</b>
     * This sample model does not only define the sample layout (pixel stride, scanline stride, <i>etc.</i>), but
     * also converts the sample values. This may be an issue for optimized pipelines accessing {@link DataBuffer}
     * directly. This class may be replaced by another mechanism (creating new tiles) in a future SIS version.</p>
     */
    private static final class ConvertedSampleModel extends SampleModel {

        private final SampleModel base;
        private final int baseDataType;
        private final MathTransform1D[] toConverted;
        private final MathTransform1D[] toPacked;

        ConvertedSampleModel(SampleModel base, MathTransform1D[] toConverted, MathTransform1D[] toPacked) {
            super(DataBuffer.TYPE_FLOAT, base.getWidth(), base.getHeight(), base.getNumBands());
            this.base         = base;
            this.baseDataType = base.getDataType();
            this.toConverted  = toConverted;
            this.toPacked     = toPacked;
        }

        @Override
        public int getNumDataElements() {
            return base.getNumDataElements();
        }

        @Override
        public Object getDataElements(final int x, final int y, final Object obj, final DataBuffer data) {
            final Object buffer = base.getDataElements(x, y, null, data);
            final float[] pixel;
            if (obj == null) {
                pixel = new float[numBands];
            } else if (!(obj instanceof float[])) {
                throw new ClassCastException("Unsupported array type, expecting a float array.");
            } else {
                pixel = (float[]) obj;
            }
            switch (baseDataType) {
                case DataBuffer.TYPE_BYTE: {
                    final byte[] b = (byte[]) buffer;
                    for (int i = 0; i < b.length; i++) pixel[i] = b[i];
                    break;
                }
                case DataBuffer.TYPE_SHORT: {
                    final short[] b = (short[]) buffer;
                    for (int i = 0; i < b.length; i++) pixel[i] = b[i];
                    break;
                }
                case DataBuffer.TYPE_USHORT: {
                    final short[] b = (short[]) buffer;
                    for (int i = 0; i < b.length; i++) pixel[i] = Short.toUnsignedInt(b[i]);
                    break;
                }
                case DataBuffer.TYPE_INT: {
                    final int[] b = (int[]) buffer;
                    for (int i = 0; i < b.length; i++) pixel[i] = b[i];
                    break;
                }
                case DataBuffer.TYPE_FLOAT: {
                    final float[] b = (float[]) buffer;
                    System.arraycopy(b, 0, pixel, 0, b.length);
                    break;
                }
                case DataBuffer.TYPE_DOUBLE: {
                    final double[] b = (double[]) buffer;
                    for (int i = 0; i < b.length; i++) pixel[i] = (float) b[i];
                    break;
                }
                default: {
                    throw new ClassCastException("Unsupported base array type.");
                }
            }
            try {
                for (int i=0; i<toConverted.length; i++) {
                    pixel[i] = (float) toConverted[i].transform(pixel[i]);
                }
            } catch (TransformException ex) {
                Arrays.fill(pixel, Float.NaN);
            }
            return pixel;
        }

        @Override
        public void setDataElements(final int x, final int y, final Object obj, final DataBuffer data) {
            float[] pixel;
            Objects.requireNonNull(obj);
            if (!(obj instanceof float[])) {
                throw new ClassCastException("Unsupported array type, expecting a float array.");
            } else {
                pixel = (float[]) obj;
            }
            try {
                for (int i=0; i<toConverted.length; i++) {
                    pixel[i] = (float) toPacked[i].transform(pixel[i]);
                }
            } catch (TransformException ex) {
                Arrays.fill(pixel, Float.NaN);
            }
            switch (baseDataType) {
                case DataBuffer.TYPE_BYTE: {
                    final byte[] b = new byte[pixel.length];
                    for (int i = 0; i < b.length; i++) b[i] = (byte) pixel[i];
                    base.setDataElements(x, y, b, data);
                    break;
                }
                case DataBuffer.TYPE_SHORT: {
                    final short[] b = new short[pixel.length];
                    for (int i = 0; i < b.length; i++) b[i] = (short) pixel[i];
                    base.setDataElements(x, y, b, data);
                    break;
                }
                case DataBuffer.TYPE_USHORT: {
                    final short[] b = new short[pixel.length];
                    for (int i = 0; i < b.length; i++) b[i] = (short) pixel[i];
                    base.setDataElements(x, y, b, data);
                    break;
                }
                case DataBuffer.TYPE_INT: {
                    final int[] b = new int[pixel.length];
                    for (int i = 0; i < b.length; i++) b[i] = (int) pixel[i];
                    base.setDataElements(x, y, b, data);
                    break;
                }
                case DataBuffer.TYPE_FLOAT: {
                    base.setDataElements(x, y, pixel, data);
                    break;
                }
                case DataBuffer.TYPE_DOUBLE: {
                    final double[] b = new double[pixel.length];
                    for (int i = 0 ;i < b.length; i++) b[i] = pixel[i];
                    base.setDataElements(x, y, b, data);
                    break;
                }
                default: {
                    throw new ClassCastException("Unsupported base array type.");
                }
            }
        }

        @Override
        public int getSample(int x, int y, int b, DataBuffer data) {
            return (int) getSampleDouble(x, y, b, data);
        }

        @Override
        public float getSampleFloat(int x, int y, int b, DataBuffer data) {
            try {
                return (float) toConverted[b].transform(base.getSampleFloat(x, y, b, data));
            } catch (TransformException ex) {
                return Float.NaN;
            }
        }

        @Override
        public double getSampleDouble(int x, int y, int b, DataBuffer data) {
            try {
                return toConverted[b].transform(base.getSampleDouble(x, y, b, data));
            } catch (TransformException ex) {
                return Double.NaN;
            }
        }

        @Override
        public void setSample(int x, int y, int b, int s, DataBuffer data) {
            setSample(x,y,b, (double) s, data);
        }

        @Override
        public void setSample(int x, int y, int b, double s, DataBuffer data) {
            try {
                s = toPacked[b].transform(s);
            } catch (TransformException ex) {
                s = Double.NaN;
            }
            base.setSample(x, y, b, s, data);
        }

        @Override
        public void setSample(int x, int y, int b, float s, DataBuffer data) {
            setSample(x, y, b, (double) s, data);
        }

        @Override
        public SampleModel createCompatibleSampleModel(int w, int h) {
            final SampleModel cp = base.createCompatibleSampleModel(w, h);
            return new ConvertedSampleModel(cp, toConverted, toPacked);
        }

        @Override
        public SampleModel createSubsetSampleModel(int[] bands) {
            final SampleModel cp = base.createSubsetSampleModel(bands);
            final MathTransform1D[] trs = new MathTransform1D[bands.length];
            final MathTransform1D[] ivtrs = new MathTransform1D[bands.length];
            for (int i=0; i<bands.length;i++) {
                trs[i] = toConverted[bands[i]];
                ivtrs[i] = toPacked[bands[i]];
            }
            return new ConvertedSampleModel(cp, trs, ivtrs);
        }

        @Override
        public DataBuffer createDataBuffer() {
            return base.createDataBuffer();
        }

        @Override
        public int[] getSampleSize() {
            final int[] sizes = new int[numBands];
            Arrays.fill(sizes, Float.SIZE);
            return sizes;
        }

        @Override
        public int getSampleSize(int band) {
            return Float.SIZE;
        }
    }

    /**
     * Color model for working with {@link ConvertedSampleModel}.
     * Defined as a workaround for the validations normally performed by {@link ColorModel}.
     *
     * <p><b>WARNING: this is a temporary class.</b>
     * This color model disable validations normally performed by {@link ColorModel}, in order to enable the use
     * of {@link ConvertedSampleModel}. This class may be replaced by another mechanism (creating new tiles) in
     * a future SIS version.</p>
     */
    private static final class ConvertedColorModel extends ColorModel {

        private final float scale;
        private final float offset;

        /**
         * Creates a new color model for the given of converted values.
         */
        ConvertedColorModel(final NumberRange<?> range){
            super(Float.SIZE);
            final double scale  = (255.0) / (range.getMaxDouble() - range.getMinDouble());
            this.scale  = (float) scale;
            this.offset = (float) (range.getMinDouble() / scale);
        }

        @Override
        public boolean isCompatibleRaster(Raster raster) {
            return isCompatibleSampleModel(raster.getSampleModel());
        }

        @Override
        public boolean isCompatibleSampleModel(SampleModel sm) {
            return sm instanceof ConvertedSampleModel;
        }

        @Override
        public int getRGB(Object inData) {
            float value;
            // Most used cases. Compatible color model is designed for cases where indexColorModel cannot do the job (float or int samples).
            if (inData instanceof float[]) {
                value = ((float[]) inData)[0];
            } else if (inData instanceof int[]) {
                value = ((int[]) inData)[0];
            } else if (inData instanceof double[]) {
                value = (float) ((double[]) inData)[0];
            } else if (inData instanceof byte[]) {
                value = ((byte[]) inData)[0];
            } else if (inData instanceof short[]) {
                value = ((short[]) inData)[0];
            } else if (inData instanceof long[]) {
                value = ((long[]) inData)[0];
            } else if (inData instanceof Number[]) {
                value = ((Number[]) inData)[0].floatValue();
            } else if (inData instanceof Byte[]) {
                value = ((Byte[]) inData)[0];
            } else {
                value = 0.0f;
            }

            int c = (int) ((value - offset) * scale);
            if (c < 0) c = 0;
            else if (c > 255) c = 255;

            return (255 << 24) | (c << 16) | (c << 8) | c;
        }

        @Override
        public int getRed(int pixel) {
            final int argb = getRGB((Object) pixel);
            return 0xFF & (argb >>> 16);
        }

        @Override
        public int getGreen(int pixel) {
            final int argb = getRGB((Object) pixel);
            return 0xFF & (argb >>> 8);
        }

        @Override
        public int getBlue(int pixel) {
            final int argb = getRGB((Object) pixel);
            return 0xFF & argb;
        }

        @Override
        public int getAlpha(int pixel) {
            final int argb = getRGB((Object) pixel);
            return 0xFF & (argb >>> 24);
        }

        @Override
        public int getRed(Object pixel) {
            final int argb = getRGB(pixel);
            return 0xFF & (argb >>> 16);
        }

        @Override
        public int getGreen(Object pixel) {
            final int argb = getRGB(pixel);
            return 0xFF & (argb >>> 8);
        }

        @Override
        public int getBlue(Object pixel) {
            final int argb = getRGB(pixel);
            return 0xFF & argb;
        }

        @Override
        public int getAlpha(Object pixel) {
            final int argb = getRGB(pixel);
            return 0xFF & (argb >>> 24);
        }

        /*
         * createCompatibleWritableRaster(int w, int h) not implemented for this class.
         */
    }
}
