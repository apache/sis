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
package org.apache.sis.geometries.math;


/**
 * Abstract transformation, all calls fallback on N tuple transform methods.
 *
 * @author Johann Sorel
 */
abstract class SimplifiedTransform implements Transform{

    protected final int input;
    protected final int output;

    public SimplifiedTransform(int inOutSize) {
        this(inOutSize,inOutSize);
    }

    public SimplifiedTransform(int inSize, int outSize) {
        this.input = inSize;
        this.output = outSize;
    }

    @Override
    public int getInputDimensions() {
        return input;
    }

    @Override
    public int getOutputDimensions() {
        return output;
    }

    @Override
    public Tuple<?> transform(Tuple<?> source, Tuple<?> dest) {
        final double[] array = new double[getOutputDimensions()];
        transform(source.toArrayDouble(), 0, array, 0, 1);
        if (dest == null) {
            dest = Vectors.create(getOutputDimensions(), DataType.DOUBLE);
        }
        dest.set(array);
        return dest;
    }

    @Override
    public final double[] transform(double[] source, double[] dest) {
        if (dest == null) dest = new double[getOutputDimensions()];
        transform1(source, 0, dest, 0);
        return dest;
    }

    @Override
    public final float[] transform(float[] source, float[] dest) {
        if (dest == null) dest = new float[getOutputDimensions()];
        transform1(source, 0, dest, 0);
        return dest;
    }

    @Override
    public void transform(double[] source, int sourceOffset, double[] dest, int destOffset, int nbTuple) {
        if (nbTuple == 1) {
            transform1(source, sourceOffset, dest, destOffset);
            return;
        }

        final int inDim = getInputDimensions();
        final int outDim = getOutputDimensions();
        for (int n=0; n<nbTuple; n++,sourceOffset+=inDim,destOffset+=outDim){
            transform1(source, sourceOffset, dest, destOffset);
        }
    }

    @Override
    public void transform(float[] source, int sourceOffset, float[] dest, int destOffset, int nbTuple) {
        if (nbTuple == 1) {
            transform1(source, sourceOffset, dest, destOffset);
            return;
        }

        final int inDim = getInputDimensions();
        final int outDim = getOutputDimensions();
        for (int n=0; n<nbTuple; n++,sourceOffset+=inDim,destOffset+=outDim){
            transform1(source, sourceOffset, dest, destOffset);
        }
    }

    protected abstract void transform1(double[] source, int sourceOffset, double[] dest, int destOffset);

    protected abstract void transform1(float[] source, int sourceOffset, float[] dest, int destOffset);

}
