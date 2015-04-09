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
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * A special case of {@link TensorParameters} restricted to the two-dimensional case.
 * The main purpose for this class is to use the EPSG:9624 parameter names, either as
 * alias or as primary name.
 *
 * <table class="sis">
 *   <caption>{@code Affine} parameters</caption>
 *   <tr><th>EPSG code</th><th>EPSG name</th><th>OGC name</th><th>Default value</th></tr>
 *   <tr><td>    </td> <td>          </td> <td>{@code num_row}</td> <td>3</td></tr>
 *   <tr><td>    </td> <td>          </td> <td>{@code num_col}</td> <td>3</td></tr>
 *   <tr><td>8623</td> <td>{@code A0}</td> <td>{@code elt_0_0}</td> <td>1</td></tr>
 *   <tr><td>8624</td> <td>{@code A1}</td> <td>{@code elt_0_1}</td> <td>0</td></tr>
 *   <tr><td>8625</td> <td>{@code A2}</td> <td>{@code elt_0_2}</td> <td>0</td></tr>
 *   <tr><td>8639</td> <td>{@code B0}</td> <td>{@code elt_1_0}</td> <td>0</td></tr>
 *   <tr><td>8640</td> <td>{@code B1}</td> <td>{@code elt_1_1}</td> <td>1</td></tr>
 *   <tr><td>8641</td> <td>{@code B2}</td> <td>{@code elt_1_2}</td> <td>0</td></tr>
 *   <tr><td>    </td> <td>          </td> <td>{@code elt_2_0}</td> <td>0</td></tr>
 *   <tr><td>    </td> <td>          </td> <td>{@code elt_2_1}</td> <td>0</td></tr>
 *   <tr><td>    </td> <td>          </td> <td>{@code elt_2_2}</td> <td>1</td></tr>
 * </table>
 *
 * Note that the EPSG database contains A3, A4, A5, A6, A7, A8 and B3 parameters,
 * but they are for polynomial transformations, not affine transformations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
class MatrixParameters extends TensorParameters<Double> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8452879524565700115L;

    /**
     * Constructs a descriptors provider.
     *
     * @param numRow The parameter for the number of rows.
     * @param numCol The parameter for the number of columns.
     */
    @SuppressWarnings("unchecked")
    MatrixParameters(final ParameterDescriptor<Integer> numRow, final ParameterDescriptor<Integer> numCol) {
        super(Double.class, "elt_", "_", numRow, numCol);
    }

    /**
     * Returns {@code true} if an official EPSG parameter exists for the given indices. Those parameters
     * are {@code "A0"}, {@code "A1"}, {@code "A2"}, {@code "B0"}, {@code "B1"} and {@code "B2"}.
     */
    static boolean isEPSG(final int[] indices) {
        return indices[0] <  Affine.EPSG_DIMENSION &&
               indices[1] <= Affine.EPSG_DIMENSION;   // Include translation column.
    }

    /**
     * Returns an alias for the given indices, or {@code null} if none.
     * The current implementation formats:
     *
     * <ul>
     *   <li>the first index (the matrix row) as letter, starting from {@code 'A'},</li>
     *   <li>the second index (the matrix column) as digit, starting from {@code '0'}.</li>
     * </ul>
     *
     * Note that for <var>row</var> &lt; 2 and <var>column</var> &lt; 3, the returned aliases are the names
     * used by the EPSG database. For other row and column indices, the same pattern is still used but the
     * result is not an official EPSG parameter name.
     *
     * @param  indices The indices of the tensor element for which to create a parameter alias.
     * @return The parameter descriptor alias for the tensor element at the given indices, or {@code null} if none.
     */
    static String indicesToAlias(final int[] indices) {
        final int row = indices[0];
        if (row >= 0 && row < 26) {
            final int col = indices[1];
            if (col >= 0 && col < 10) {
                return String.valueOf(new char[] {(char) ('A' + row), (char) ('0' + col)});
            }
        }
        return null;
    }

    /**
     * Returns the indices for the given alias, or {@code null} if none.
     * This method is the converse of {@link #indicesToAlias(int[])}.
     */
    static int[] aliasToIndices(final String alias) {
        if (alias.length() == 2) {
            final int row = alias.charAt(0) - 'A';
            if (row >= 0 && row < 26) {
                final int col = alias.charAt(1) - '0';
                if (col >= 0 && col < 10) {
                    return new int[] {row, col};
                }
            }
        }
        return null;
    }

    /**
     * Returns the indices of matrix element for the given parameter name, or {@code null} if none.
     * This implementation unconditionally checks for the alphanumeric (EPSG-like) name first since
     * this is a very quick check. If the given name does not use the EPSG syntax, then this method
     * fallback on the WKT1 syntax.
     */
    @Override
    protected int[] nameToIndices(final String name) throws IllegalArgumentException {
        int[] indices = aliasToIndices(name);
        if (indices == null) {
            indices = super.nameToIndices(name);
        }
        return indices;
    }

    /**
     * Creates a new parameter descriptor for a matrix element at the given indices.
     * This method creates:
     *
     * <ul>
     *   <li>The OGC name (e.g. {@code "elt_1_2"}) as primary name.</li>
     *   <li>The alpha-numeric name (e.g. {@code "B2"}) as an alias.</li>
     * </ul>
     *
     * This method does <strong>not</strong> assign the alpha-numeric names to the EPSG authority in order to avoid
     * confusion when formatting the parameters as Well Known Text (WKT). However {@link MatrixParametersAlphaNum}
     * subclass will assign some names to the EPSG authority, as well as their identifier (e.g. EPSG:8641).
     */
    @Override
    protected ParameterDescriptor<Double> createElementDescriptor(final int[] indices) throws IllegalArgumentException {
        final Map<String,Object> properties = new HashMap<String,Object>(4);
        properties.put(ParameterDescriptor.NAME_KEY,
                new NamedIdentifier(Citations.OGC, Constants.OGC, indicesToName(indices), null, null));
        final String c = indicesToAlias(indices);
        if (c != null) {
            properties.put(ParameterDescriptor.ALIAS_KEY,
                    new NamedIdentifier(Citations.SIS, Constants.SIS, c, null, null));
        }
        return new DefaultParameterDescriptor<Double>(properties, 0, 1, Double.class, null, null, getDefaultValue(indices));
    }

    /**
     * On deserialization, replaces the deserialized instance by the unique instance if possible.
     */
    Object readResolve() throws ObjectStreamException {
        return equals(WKT1) ? WKT1 : this;
    }
}
