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
package org.apache.sis.parameter;

import org.opengis.parameter.ParameterDescriptor;


/**
 * Creates parameter groups for tensors (usually matrices).
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @param <E>  the type of tensor element values.
 * @see org.apache.sis.referencing.operation.matrix.Matrices
 * @see <a href="https://issues.apache.org/jira/browse/SIS-619">SIS-619</a>
 *
 * @since 0.4
 *
 * @deprecated Renamed {@link MatrixParameters} because this is not really a builder of tensors.
 *             Furthermore, the declared <abbr>EPSG</abbr> parameter names were incorrect.
 *             See <a href="https://issues.apache.org/jira/browse/SIS-619">SIS-619</a>.
 */
@Deprecated(since = "1.5", forRemoval = true)
public class TensorParameters<E> extends MatrixParameters<E> {
    /**
     * Parses and creates matrix parameters with alphanumeric names.
     *
     * @since 0.6
     *
     * @deprecated Replaced by {@link MatrixParameters#ALPHANUM}.
     */
    @Deprecated(since = "1.5", forRemoval = true)
    public static final TensorParameters<Double> ALPHANUM = new TensorParameters<>(MatrixParameters.ALPHANUM) {
        @Override protected ParameterDescriptor<Double> createElementDescriptor(final int[] indices) {
            return MatrixParameters.ALPHANUM.createElementDescriptor(indices);
        }
    };

    /**
     * Parses and creates matrix parameters with names matching the Well Known Text version 1 (WKT 1) convention.
     *
     * @deprecated Replaced by {@link MatrixParameters#WKT1}.
     */
    @Deprecated(since = "1.5", forRemoval = true)
    public static final TensorParameters<Double> WKT1 = new TensorParameters<>(MatrixParameters.WKT1);

    TensorParameters(final MatrixParameters<E> other) {
        super(other);
    }

    /**
     * Constructs a descriptors provider.
     *
     * @param elementType  the type of tensor element values.
     * @param prefix       the prefix to insert in front of parameter name for each tensor elements.
     * @param separator    the separator between dimension (row, column, …) indices in parameter names.
     * @param dimensions   the parameter for the size of each dimension, usually in an array of length 2.
     *                     Length may be different if the caller wants to generalize usage of this class to tensors.
     */
    @SafeVarargs
    public TensorParameters(final Class<E> elementType, final String prefix, final String separator,
            final ParameterDescriptor<Integer>... dimensions)
    {
        super(elementType, prefix, separator, dimensions);
    }

    /**
     * @deprecated Renamed {@link #order()} because "rank" has a different meaning in linear algebra.
     */
    @Deprecated(since = "1.5", forRemoval = true)
    public final int rank() {
        return order();
    }
}
