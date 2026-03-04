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

import static org.apache.sis.geometries.math.DataType.FLOAT;


/**
 * A Transformation is a mathematic operation which transform a tuple values.
 * Example : Affine, Matrix, Similarity.
 *
 * This class is a experimental light and GIS neutral version of MathTransform.
 * This class will evolve to match requirements for the upcoming Java Vector API and Java HAT Kernel.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Transform {

    /**
     * Returns true if tranform is identity.
     *
     * @return true if tranform is identity
     */
    boolean isIdentity();

    /**
     * @return number of dimension expected in input tuple.
     */
    int getInputDimensions();

    /**
     * @return number of dimension expected in output tuple.
     */
    int getOutputDimensions();

    /**
     * Transform a single tuple.
     *
     * @param source tuple array, can not be null.
     * @param dest array, can be null.
     * @return destination tuple.
     */
    default double[] transform(double[] source, double[] dest) {
        if (dest == null) {
            dest = new double[getOutputDimensions()];
        }
        transform(source, 0, dest, 0, 1);
        return dest;
    }

    /**
     * Transform N tuples.
     *
     * @param source tuple array, can not be null.
     * @param sourceOffset index where to start.
     * @param dest array, can not be null.
     * @param destOffset index where start inserting converted values.
     * @param nbTuple number of tuples to transform.
     */
    void transform(double[] source, int sourceOffset, double[] dest, int destOffset, int nbTuple);

    /**
     * Transform a single tuple.
     *
     * @param source tuple array, can not be null.
     * @param dest array, can be null.
     * @return destination tuple.
     */
    default float[] transform(float[] source, float[] dest) {
        if (dest == null) {
            dest = new float[getOutputDimensions()];
        }
        transform(source, 0, dest, 0, 1);
        return dest;
    }

    /**
     * Transform N tuples.
     *
     * @param source tuple array, can not be null.
     * @param sourceOffset index where to start.
     * @param dest array, can not be null.
     * @param destOffset index where start inserting converted values.
     * @param nbTuple number of tuples to transform.
     */
    void transform(float[] source, int sourceOffset, float[] dest, int destOffset, int nbTuple);

    /**
     * Transform a single tuple.
     *
     * @param source tuple, can not be null.
     * @param dest tuple, can be null.
     * @return destination tuple.
     */
    default Tuple<?> transform(Tuple<?> source, Tuple<?> dest) {
        final int outSize = getOutputDimensions();
        if (dest == null) dest = Vectors.create(outSize, source.getDataType());

        final DataType numericType = dest.getDataType();
        switch (numericType) {
            case DOUBLE :
                final double[] arrayd = new double[outSize];
                transform(source.toArrayDouble(), 0, arrayd, 0, 1);
                dest.set(arrayd);
                return dest;
            default :
            case FLOAT :
                final float[] arrayf = new float[outSize];
                transform(source.toArrayFloat(), 0, arrayf, 0, 1);
                dest.set(arrayf);
                return dest;
        }
    }

    /**
     * Create the inverse transform.
     */
    Transform createInverse();

}
