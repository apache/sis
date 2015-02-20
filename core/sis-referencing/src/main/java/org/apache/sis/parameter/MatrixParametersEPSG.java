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

import java.util.Map;
import java.util.HashMap;
import java.io.ObjectStreamException;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;

import static org.apache.sis.internal.util.CollectionsExt.first;


/**
 * A special case of {@link MatrixParameters} implementing the "magic" for the EPSG:9624 parameters.
 * The "magical" behavior is to hide {@code "num_row"}, {@code "num_col"} and last row parameters if
 * the matrix has exactly the dimensions required by the EPSG:9624 operation method, which is 3×3.
 * The intend is to get a descriptor matching the one defined in the EPSG database.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class MatrixParametersEPSG extends MatrixParameters {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 476760046257432637L;

    /**
     * Constructs a descriptors provider.
     *
     * @param numRow The parameter for the number of rows.
     * @param numCol The parameter for the number of columns.
     */
    MatrixParametersEPSG(final ParameterDescriptor<Integer> numRow, final ParameterDescriptor<Integer> numCol) {
        super(numRow, numCol);
    }

    /**
     * Returns 0 if the dimension parameters ({@code "num_row"} and {@code "num_col"}) shall be hidden.
     * Those parameters need to be hidden for the EPSG:9624 operation method, since the EPSG database
     * does not define those parameters.
     */
    @Override
    final int numDimensions(final int[] actualSize) {
        if (actualSize[0] == Affine.EPSG_DIMENSION + 1 &&
            actualSize[1] == Affine.EPSG_DIMENSION + 1)
        {
            return 0;
        }
        return super.numDimensions(actualSize);
    }

    /**
     * Returns the number of elements (e.g. {@code "elt_0_0"}) when formatting the parameter descriptors for a tensor
     * of the given size.  This is the total number of elements in the tensor, except for matrices which are intended
     * to be affine (like {@link #EPSG}) where the last row is omitted.
     */
    @Override
    final int numElements(final int[] actualSize) {
        int numRow = actualSize[0];
        int numCol = actualSize[1];
        assert super.numElements(actualSize) == (numRow * numCol);
        if (numRow == Affine.EPSG_DIMENSION + 1 &&
            numCol == Affine.EPSG_DIMENSION + 1)
        {
            numRow--; // Ommit last row of an affine matrix.
        }
        return numRow * numCol;
    }

    /**
     * Returns the parameter descriptor name of a matrix element at the given indices.
     * Overridden as a matter of principle, but not used directly by this implementation.
     */
    @Override
    protected String indicesToName(final int[] indices) throws IllegalArgumentException {
        return indicesToAlias(indices);
    }

    /**
     * Creates a new parameter descriptor for a matrix element at the given indices. This method creates both the
     * OGC name (e.g. {@code "elt_1_2"}) and the EPSG name (e.g. {@code "B2"}), together with the EPSG identifier
     * (e.g. {@code "EPSG:8641"}) it it exists. See {@link org.apache.sis.internal.referencing.provider.Affine}
     * for a table summarizing the parameter names and identifiers.
     */
    @Override
    protected ParameterDescriptor<Double> createElementDescriptor(final int[] indices) throws IllegalArgumentException {
        /*
         * For the EPSG convention, we recycle the names created for the WKT1 convention but interchanging
         * the name with the alias (since our WKT1 convention adds the EPSG names as aliases). We use WKT1
         * as the primary source because it is still very widely used,  and works for arbitrary dimensions
         * while the EPSG parameters are (officially) restricted to 3×3 matrices.
         */
        if (WKT1 == this) {
            // Should never happen, but still unconditionally tested
            // (no 'assert' keyword) for preventing stack overflow.
            throw new AssertionError();
        }
        final ParameterDescriptor<Double> wkt = WKT1.getElementDescriptor(indices);   // Really 'WKT1', not 'super'.
        final Map<String,Object> properties = new HashMap<>(6);
        properties.put(ParameterDescriptor.NAME_KEY, first(wkt.getAlias()));
        properties.put(ParameterDescriptor.ALIAS_KEY, wkt.getName());
        /*
         * For the WKT1 convention, create an alias matching the EPSG pattern ("A0", "A1", etc.) for all
         * indices but declare the EPSG authority and identifier only for A0, A1, A2, B0, B1 and B2.
         */
        if (isEPSG(indices)) {
            final ImmutableIdentifier id;
            final int code = (indices[0] == 0 ? Constants.A0 : Constants.B0) + indices[1];
            id = new ImmutableIdentifier(Citations.OGP, Constants.EPSG, String.valueOf(code));
            properties.put(ParameterDescriptor.IDENTIFIERS_KEY, id);
        }
        return new DefaultParameterDescriptor<>(properties, 0, 1, Double.class, null, null, getDefaultValue(indices));
    }

    /**
     * On deserialization, replaces the deserialized instance by the unique instance.
     */
    @Override
    Object readResolve() throws ObjectStreamException {
        return EPSG;
    }
}
