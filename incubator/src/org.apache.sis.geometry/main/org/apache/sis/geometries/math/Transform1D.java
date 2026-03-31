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
 * Single dimension transform.
 *
 * @author Johann Sorel
 */
public interface Transform1D extends Transform {

    /**
     * @return 1
     */
    @Override
    default int getInputDimensions() {
        return 1;
    }

    /**
     * @return 1
     */
    @Override
    default int getOutputDimensions() {
        return 1;
    }

    @Override
    default void transform(double[] source, int sourceOffset, double[] dest, int destOffset, int nbTuple) {
        for (int i=0;i<nbTuple;i++) {
            dest[destOffset+i] = transform(source[sourceOffset+i]);
        }
    }

    @Override
    default void transform(float[] source, int sourceOffset, float[] dest, int destOffset, int nbTuple) {
        for (int i=0;i<nbTuple;i++) {
            dest[destOffset+i] = transform(source[sourceOffset+i]);
        }
    }

    default float transform(float number) {
        return (float) transform((double) number);
    }

    double transform(double number);

    @Override
    Transform1D createInverse();

}
