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
package org.apache.sis.maths;

import static org.apache.sis.maths.DataType.FLOAT;


/**
 * A Transformation is a mathematic operation which transform a tuple values.
 * Example : Affine, Matrix, Similarity.
 *
 * This class is an experimental light and GIS neutral version of MathTransform.
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
     * @param source tuple array, cannot be null.
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
     * @param source tuple array, cannot be null.
     * @param sourceOffset index where to start.
     * @param dest array, cannot be null.
     * @param destOffset index where start inserting converted values.
     * @param nbTuple number of tuples to transform.
     */
    void transform(double[] source, int sourceOffset, double[] dest, int destOffset, int nbTuple);

    /**
     * Transform a single tuple.
     *
     * @param source tuple array, cannot be null.
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
     * @param source tuple array, cannot be null.
     * @param sourceOffset index where to start.
     * @param dest array, cannot be null.
     * @param destOffset index where start inserting converted values.
     * @param nbTuple number of tuples to transform.
     */
    void transform(float[] source, int sourceOffset, float[] dest, int destOffset, int nbTuple);

    /**
     * Transform a single tuple.
     * The result is a new vector with the data type of the source tuple.
     *
     * @param source tuple, cannot be null.
     * @return new transformed tuple.
     */
    default Vector<?> transform(Vector<?> source) {
        final double[] array = new double[getOutputDimensions()];
        transform(source.toArrayDouble(), 0, array, 0, 1);
        return Vectors.create(array, source.getDataType());
    }

    /**
     * Create the inverse transform.
     */
    Transform createInverse();

}
