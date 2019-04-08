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

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

/**
 * Warning : experimental class.
 *
 * Decorates a GridCoverage to convert values on the fly.
 * This class produces a special {@linkplain SampleModel} which may cause
 * issues in processing operations if the {@linkplain SampleModel} is not properly used as a fallback.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ConvertedGridCoverage extends GridCoverage {

    public static GridCoverage convert(GridCoverage coverage) {
        final List<SampleDimension> sds = coverage.getSampleDimensions();
        final List<SampleDimension> cfs = new ArrayList<>(sds.size());
        for (SampleDimension sd : sds) {
            cfs.add(sd.forConvertedValues(true));
        }
        return new ConvertedGridCoverage(coverage, cfs);
    }

    private final GridCoverage coverage;

    private ConvertedGridCoverage(GridCoverage base, List<SampleDimension> sampleDims) {
        super(base.getGridGeometry(), sampleDims);
        this.coverage = base;
    }

    @Override
    public RenderedImage render(GridExtent sliceExtent) throws CannotEvaluateException {
        final BufferedImage render = (BufferedImage) coverage.render(sliceExtent);
        final List<SampleDimension> sampleDimensions = getSampleDimensions();
        final int numBands = sampleDimensions.size();
        final MathTransform1D[] transforms = new MathTransform1D[numBands];
        final MathTransform1D[] ivtransforms = new MathTransform1D[numBands];
        boolean isIdentity = true;
        for (int i = 0; i < numBands; i++) {
            MathTransform1D transform = sampleDimensions.get(i).forConvertedValues(false).getTransferFunction().orElse(null);
            if (transform == null) transform = (MathTransform1D) MathTransforms.linear(1.0, 0.0);
            transforms[i] = transform;
            try {
                ivtransforms[i] = transform.inverse();
            } catch (NoninvertibleTransformException ex) {
                ivtransforms[i] = (MathTransform1D) MathTransforms.linear(Double.NaN, 0.0);
            }
            isIdentity &= transform.isIdentity();
        }
        if (isIdentity) {
            return render;
        }

        final WritableRaster raster = render.getRaster();
        final SampleModel baseSm = raster.getSampleModel();
        final DataBuffer dataBuffer = raster.getDataBuffer();
        final ConvertedSampleModel convSm = new ConvertedSampleModel(baseSm, transforms, ivtransforms);

        //default color models have a lot of constraints
        final WritableRaster convRaster = WritableRaster.createWritableRaster(convSm, dataBuffer, new Point(0, 0));
        final ColorModel cm = new ConvertedColorModel(32, sampleDimensions.get(0).getSampleRange().get());

        return new BufferedImage(cm, convRaster, false, null);
    }

    @Override
    public GridCoverage forConvertedValues(boolean converted) {
        return converted ? this : coverage;
    }

    private static final class ConvertedSampleModel extends SampleModel {

        private final SampleModel base;
        private final int baseDataType;
        private final MathTransform1D[] bandTransforms;
        private final MathTransform1D[] bandIvtransforms;
        private final MathTransform pixelTransform;
        private final MathTransform pixelIvTransform;

        public ConvertedSampleModel(SampleModel base, MathTransform1D[] transforms, MathTransform1D[] ivtransforms) {
            super(DataBuffer.TYPE_FLOAT, base.getWidth(), base.getHeight(), base.getNumBands());
            this.base = base;
            this.baseDataType = base.getDataType();
            this.bandTransforms = transforms;
            this.bandIvtransforms = ivtransforms;
            this.pixelTransform = MathTransforms.compound(bandTransforms);
            this.pixelIvTransform = MathTransforms.compound(bandIvtransforms);
        }

        @Override
        public int getNumDataElements() {
            return base.getNumDataElements();
        }

        @Override
        public Object getDataElements(int x, int y, Object obj, DataBuffer data) {
            Object buffer = base.getDataElements(x, y, null, data);
            float[] pixel;
            if (obj == null) {
                pixel = new float[numBands];
            } else if (!(obj instanceof float[])) {
                throw new ClassCastException("Unsupported array type, expecting a float array.");
            } else {
                pixel = (float[]) obj;
            }

            switch (baseDataType) {
                case DataBuffer.TYPE_BYTE : {
                    final byte[] b = (byte[]) buffer;
                    for (int i = 0; i < b.length; i++) pixel[i] = b[i];
                    } break;
                case DataBuffer.TYPE_SHORT : {
                    final short[] b = (short[]) buffer;
                    for (int i = 0; i < b.length; i++) pixel[i] = b[i];
                    } break;
                case DataBuffer.TYPE_USHORT : {
                    final short[] b = (short[]) buffer;
                    for (int i = 0; i < b.length; i++) pixel[i] = b[i] & 0xFFFF;
                    } break;
                case DataBuffer.TYPE_INT : {
                    final int[] b = (int[]) buffer;
                    for (int i = 0; i < b.length; i++) pixel[i] = b[i];
                    } break;
                case DataBuffer.TYPE_FLOAT : {
                    final float[] b = (float[]) buffer;
                    for (int i = 0; i < b.length; i++) pixel[i] = b[i];
                    } break;
                case DataBuffer.TYPE_DOUBLE : {
                    final double[] b = (double[]) buffer;
                    for (int i = 0; i < b.length; i++) pixel[i] = (float) b[i];
                    } break;
                default: {
                    throw new ClassCastException("Unsupported base array type.");
                    }
            }

            try {
                pixelTransform.transform(pixel, 0, pixel, 0, 1);
            } catch (TransformException ex) {
                Arrays.fill(pixel, Float.NaN);
            }
            return pixel;
        }

        @Override
        public void setDataElements(int x, int y, Object obj, DataBuffer data) {
            float[] pixel;
            if (obj == null) {
                throw new ClassCastException("Null array values");
            } else if (!(obj instanceof float[])) {
                throw new ClassCastException("Unsupported array type, expecting a float array.");
            } else {
                pixel = (float[]) obj;
            }

            try {
                pixelIvTransform.transform(pixel, 0, pixel, 0, 1);
            } catch (TransformException ex) {
                Arrays.fill(pixel, Float.NaN);
            }

            switch (baseDataType) {
                case DataBuffer.TYPE_BYTE : {
                    final byte[] b = new byte[pixel.length];
                    for (int i = 0; i < b.length; i++) b[i] = (byte) pixel[i];
                    base.setDataElements(x, y, b, data);
                    } break;
                case DataBuffer.TYPE_SHORT : {
                    final short[] b = new short[pixel.length];
                    for (int i = 0; i < b.length; i++) b[i] = (short) pixel[i];
                    base.setDataElements(x, y, b, data);
                    } break;
                case DataBuffer.TYPE_USHORT : {
                    final short[] b = new short[pixel.length];
                    for (int i = 0; i < b.length; i++) b[i] = (short) pixel[i];
                    base.setDataElements(x, y, b, data);
                    } break;
                case DataBuffer.TYPE_INT : {
                    final int[] b = new int[pixel.length];
                    for (int i = 0; i < b.length; i++) b[i] = (int) pixel[i];
                    base.setDataElements(x, y, b, data);
                    } break;
                case DataBuffer.TYPE_FLOAT : {
                    base.setDataElements(x, y, pixel, data);
                    } break;
                case DataBuffer.TYPE_DOUBLE : {
                    final double[] b = new double[pixel.length];
                    for (int i = 0 ;i < b.length; i++) b[i] = pixel[i];
                    base.setDataElements(x, y, b, data);
                    } break;
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
                return (float) bandTransforms[b].transform(base.getSampleFloat(x, y, b, data));
            } catch (TransformException ex) {
                return Float.NaN;
            }
        }

        @Override
        public double getSampleDouble(int x, int y, int b, DataBuffer data) {
            try {
                return bandTransforms[b].transform(base.getSampleDouble(x, y, b, data));
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
                s = bandIvtransforms[b].transform(s);
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
            return new ConvertedSampleModel(cp, bandTransforms, bandIvtransforms);
        }

        @Override
        public SampleModel createSubsetSampleModel(int[] bands) {
            final SampleModel cp = base.createSubsetSampleModel(bands);
            final MathTransform1D[] trs = new MathTransform1D[bands.length];
            final MathTransform1D[] ivtrs = new MathTransform1D[bands.length];
            for (int i=0; i<bands.length;i++) {
                trs[i] = bandTransforms[bands[i]];
                ivtrs[i] = bandIvtransforms[bands[i]];
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
            Arrays.fill(sizes, 32);
            return sizes;
        }

        @Override
        public int getSampleSize(int band) {
            return 32;
        }

    }

    private static final class ConvertedColorModel extends ColorModel {

        private final float scale;
        private final float offset;

        /**
         * @param nbbits
         * @param fct : Interpolate or Categorize function
         */
        public ConvertedColorModel(final int nbbits, final NumberRange range){
            super(nbbits);
            final double scale  = (255.0) / (range.getMaxDouble() - range.getMinDouble());
            this.scale  = (float) scale;
            this.offset = (float) (range.getMinDouble() / scale);
        }

        @Override
        public boolean isCompatibleRaster(Raster raster) {
            return true;
        }

        @Override
        public boolean isCompatibleSampleModel(SampleModel sm) {
            return true;
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
            return 0xFF & (argb >> 16);
        }

        @Override
        public int getGreen(int pixel) {
            final int argb = getRGB((Object) pixel);
            return 0xFF & ( argb >> 8);
        }

        @Override
        public int getBlue(int pixel) {
            final int argb = getRGB((Object) pixel);
            return 0xFF & ( argb >> 0);
        }

        @Override
        public int getAlpha(int pixel) {
            final int argb = getRGB((Object) pixel);
            return 0xFF & ( argb >> 24);
        }

        @Override
        public int getRed(Object pixel) {
            final int argb = getRGB((Object) pixel);
            return 0xFF & (argb >> 16);
        }

        @Override
        public int getGreen(Object pixel) {
            final int argb = getRGB((Object) pixel);
            return 0xFF & ( argb >> 8);
        }

        @Override
        public int getBlue(Object pixel) {
            final int argb = getRGB((Object) pixel);
            return 0xFF & ( argb >> 0);
        }

        @Override
        public int getAlpha(Object pixel) {
            final int argb = getRGB((Object) pixel);
            return 0xFF & ( argb >> 24);
        }

        @Override
        public WritableRaster createCompatibleWritableRaster(int w, int h) {
            return Raster.createPackedRaster(new DataBufferInt(w*h),w,h,16,null);
        }

    }

}
