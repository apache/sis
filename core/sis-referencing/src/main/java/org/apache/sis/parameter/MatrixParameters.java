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
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.internal.referencing.HardCoded;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;

import static org.apache.sis.internal.util.CollectionsExt.first;


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
final class MatrixParameters extends TensorParameters<Double> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8452879524565700115L;

    /**
     * Constructs a descriptors provider.
     *
     * @param prefix    The prefix to insert in front of parameter name for each tensor elements.
     * @param separator The separator between dimension (row, column, …) indices in parameter names.
     * @param numRow    The parameter for the number of rows.
     * @param numCol    The parameter for the number of columns.
     */
    MatrixParameters(final String prefix, final String separator,
            final ParameterDescriptor<Integer> numRow, final ParameterDescriptor<Integer> numCol)
    {
        super(Double.class, prefix, separator, numRow, numCol);
    }

    /**
     * {@code true} for using the EPSG names, or {@code false} for using the WKT1 names.
     * Current implementation uses {@link #separator} emptiness as a criterion. This is
     * an arbitrary choice that may change in any future SIS version. However we need a
     * criterion which is preserved during serialization.
     *
     * @see #EPSG
     */
    final boolean isEPSG() {
        return separator.isEmpty();
    }

    /**
     * Returns {@code true} if an official EPSG parameter exists for the given indices. Those parameters
     * are {@code "A0"}, {@code "A1"}, {@code "A2"}, {@code "B0"}, {@code "B1"} and {@code "B2"}.
     */
    private static boolean isEPSG(final int[] indices) {
        return indices[0] < 2 && indices[1] < 3;
    }

    /**
     * Returns an alias for the given indices, or {@code null} if none.
     * The default implementation formats:
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
     * Returns the parameter descriptor name of a matrix element at the given indices.
     * Overridden as a matter of principle, but not used directly by this implementation.
     */
    @Override
    protected String indicesToName(final int[] indices) throws IllegalArgumentException {
        return isEPSG() ? indicesToAlias(indices) : super.indicesToName(indices);
    }

    /**
     * Returns the indices of matrix element for the given parameter name, or {@code null} if none.
     * This implementation unconditionally checks for the EPSG name first since this is a very quick check.
     * If the given name does not use the EPSG syntax, then this method fallback on the WKT1 syntax.
     */
    @Override
    protected int[] nameToIndices(final String name) throws IllegalArgumentException {
        int[] indices = aliasToIndices(name);
        if (indices == null) {
            if (isEPSG()) {
                if (WKT1 == this) {
                    // Should never happen, but still unconditionally tested
                    // (no 'assert' keyword) for preventing stack overflow.
                    throw new AssertionError();
                }
                indices = WKT1.nameToIndices(name);
            } else {
                indices = super.nameToIndices(name);
            }
        }
        return indices;
    }

    /**
     * Creates a new parameter descriptor for a matrix element at the given indices. This method creates both the
     * OGC name (e.g. {@code "elt_1_2"}) and the EPSG name (e.g. {@code "B2"}), together with the EPSG identifier
     * (e.g. {@code "EPSG:8641"}) it it exists. See {@link org.apache.sis.internal.referencing.provider.Affine}
     * for a table summarizing the parameter names and identifiers.
     */
    @Override
    protected ParameterDescriptor<Double> createElementDescriptor(final int[] indices) throws IllegalArgumentException {
        final Object name, alias, identifier;
        if (isEPSG()) {
            /*
             * For the EPSG convention, we recycle the names and identifiers created for the WKT1 convention but
             * interchanging the name with the alias (since our WKT1 convention adds the EPSG names as aliases).
             * We use WKT1 as the primary source because it is still very widely used, and works for arbitrary
             * dimensions while the EPSG parameters are (officially) restricted to 3×3 matrices.
             */
            if (WKT1 == this) {
                // Should never happen, but still unconditionally tested
                // (no 'assert' keyword) for preventing stack overflow.
                throw new AssertionError();
            }
            final ParameterDescriptor<Double> wkt = WKT1.getElementDescriptor(indices);
            name       = first(wkt.getAlias());
            alias      = wkt.getName();
            identifier = first(wkt.getIdentifiers());
        } else {
            /*
             * For the WKT1 convention, create an alias matching the EPSG pattern ("A0", "A1", etc.) for all
             * indices but declare the EPSG authority and identifier only for A0, A1, A2, B0, B1 and B2.
             */
            name = new NamedIdentifier(Citations.OGC, HardCoded.OGC, super.indicesToName(indices), null, null);
            final Citation authority;
            final String codeSpace;
            if (isEPSG(indices)) {
                authority = Citations.OGP;
                codeSpace = HardCoded.EPSG;
                final int code = (indices[0] == 0 ? HardCoded.A0 : HardCoded.B0) + indices[1];
                identifier = new ImmutableIdentifier(authority, codeSpace, String.valueOf(code));
            } else {
                authority  = Citations.SIS;
                codeSpace  = HardCoded.SIS;
                identifier = null;
            }
            final String c = indicesToAlias(indices);
            alias = (c != null) ? new NamedIdentifier(authority, codeSpace, c, null, null) : null;
        }
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(ParameterDescriptor.NAME_KEY,        name);
        properties.put(ParameterDescriptor.ALIAS_KEY,       alias);
        properties.put(ParameterDescriptor.IDENTIFIERS_KEY, identifier);
        return new DefaultParameterDescriptor<>(properties, 0, 1, Double.class, null, null, getDefaultValue(indices));
    }

    /**
     * On deserialization, replaces the deserialized instance by the unique instance if possible.
     */
    Object readResolve() throws ObjectStreamException {
        final TensorParameters<?> candidate = isEPSG() ? EPSG : WKT1;
        return equals(candidate) ? candidate : this;
    }
}
