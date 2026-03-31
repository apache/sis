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
 * An affine is a transform which preserve points, straight lines, planes
 * and parallel lines.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Affine<T extends Affine<T>> extends ReadOnly.Affine<T> {

    /**
     * Copy all values from given transform.
     *
     * @param toCopy transform to copy from
     * @return this Affine
     */
    T set(final ReadOnly.Affine<?> toCopy);

    /**
     * Set one affine value.
     *
     * @param row value row index
     * @param col value column index
     * @param value new cell value
     * @return this Affine
     * @throws IllegalArgumentException if coordinate is out of affine range
     */
    T set(int row, int col, double value) throws IllegalArgumentException;

    /**
     * Set affine row values.
     *
     * @param row row index
     * @param values new row values
     * @return this Affine
     * @throws IllegalArgumentException if coordinate is out of affine range
     */
    T setRow(int row, double[] values) throws IllegalArgumentException;

    /**
     * Set affine column values.
     *
     * @param col column index
     * @param values new column values
     * @return this Affine
     * @throws IllegalArgumentException if coordinate is out of affine range
     */
    T setCol(int col, double[] values) throws IllegalArgumentException;

    /**
     * Set affine values to identity .
     *
     * @return this affine instance
     */
    T setToIdentity();

    /**
     * Copy all values from given transform.
     * Given matrix must be affine.
     *
     * @param m matrix to copy from
     * @return this Affine
     * @throws IllegalArgumentException if matrix is not affine
     */
    T setFromMatrix(ReadOnly.Matrix<?> m) throws IllegalArgumentException;

    /**
     * Multiply this affine by given affine and store the result in this affine.
     *
     * @param affine multiplying affine
     * @return this Affine
     */
    T multiply(ReadOnly.Affine<?> affine);

    /**
     * Scale this affine.
     *
     * @param tuple scaling factor to apply, (one by columns).
     * @return this affine instance
     * @throws IllegalArgumentException if coordinate is out of affine range
     */
    T scale(double[] tuple) throws IllegalArgumentException;

    /**
     * Scale this affine.
     *
     * @param scale scaling factor to apply
     * @return this affine instance
     */
    T scale(double scale);

    /**
     * Inverse this affine transform.
     *
     * @return this affine instance
     */
    T invert();

    /**
     * Create a copy of this Affine.
     *
     * @return copy
     */
    T copy();

}
