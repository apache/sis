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
import java.util.List;
import java.util.Arrays;
import java.io.Serializable;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterNameException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * Creates parameter groups from tensors (usually matrices), and conversely.
 * Each group of parameters contains the following elements:
 *
 * <ul>
 *   <li>A mandatory parameter for the number of rows ({@code "num_row"} in WKT 1).</li>
 *   <li>A mandatory parameter for the number of columns ({@code "num_col"} in WKT 1).</li>
 *   <li>(<i>etc.</i> in third-order or higher-order tensors).</li>
 *   <li>A maximum of {@code num_row} × {@code num_col} × … optional parameters for the matrix element values.
 *       Parameter names depend on the formatting convention. If a parameter is omitted, then its default value
 *       is 1 for parameters on the diagonal and 0 for all other parameters.</li>
 * </ul>
 *
 * The parameters format is typically like below:
 *
 * {@preformat wkt
 *   Parameter["num_row", 3],
 *   Parameter["num_col", 3],
 *   Parameter["elt_0_0", <value>],
 *   Parameter["elt_0_1", <value>],
 *   ...
 *   Parameter["elt_0_<num_col-1>", <value>],
 *   Parameter["elt_1_0", <value>],
 *   Parameter["elt_1_1", <value>],
 *   ...
 *   Parameter["elt_<num_row-1>_<num_col-1>", <value>]
 * }
 *
 * Those groups are extensible, i.e. the number of <code>"elt_<var>row</var>_<var>col</var>"</code> parameters
 * depends on the {@code "num_row"} and {@code "num_col"} parameter values. For this reason, the descriptor of
 * matrix parameters is not immutable.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
public class MatrixParameters implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7386537348359343836L;

    /**
     * Parses and creates parameters names matching the
     * <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html">Well Known Text</a>
     * version 1 (WKT 1) convention.
     *
     * <ul>
     *   <li>First parameter is {@code "num_row"}.</li>
     *   <li>Second parameter is {@code "num_col"}.</li>
     *   <li>All other parameters are of the form <code>"elt_<var>row</var>_<var>col</var>"</code>.</li>
     * </ul>
     *
     * <div class="note"><b>Example:</b> {@code "elt_2_1"} is the element name for the value at line 2 and row 1.</div>
     */
    public static final MatrixParameters WKT1;
    static {
        /*
         * Note: the upper limit given in the operation parameters is arbitrary. A high
         *       value doesn't make much sense anyway since matrix size for projective
         *       transform will usually not be much more than 5, and the storage scheme
         *       used in this implementation is inefficient for large amount of matrix
         *       elements.
         */
        final NumberRange<Integer> valueDomain = NumberRange.create(1, true, 50, true);
        final Integer defaultSize = 3;
        final ParameterDescriptor<Integer> numRow, numCol;
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(ReferenceIdentifier.AUTHORITY_KEY, Citations.OGC);
        properties.put(ReferenceIdentifier.CODE_KEY, "num_row");
        numRow = new DefaultParameterDescriptor<>(properties, Integer.class, valueDomain, null, defaultSize, true);
        properties.put(ReferenceIdentifier.CODE_KEY, "num_col");
        numCol = new DefaultParameterDescriptor<>(properties, Integer.class, valueDomain, null, defaultSize, true);
        WKT1 = new MatrixParameters("elt_", "_", numRow, numCol);
    }

    /**
     * The height and weight of the matrix of {@link #parameters} to cache. Descriptors
     * for row or column indices greater than or equal to this value will not be cached.
     */
    private static final int CACHE_SIZE = 8;

    /**
     * The parameters that define the number of rows and columns.
     * In WKT1, the parameter names are {@code "num_row"} and {@code "num_col"} respectively.
     */
    private final ParameterDescriptor<Integer>[] size;

    /**
     * The cached descriptors for each elements in a matrix. Descriptors do not depend on matrix element values.
     * Consequently, the same descriptors can be reused for all {@link MatrixParameterValues} instances.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private final transient ParameterDescriptor<Double>[] parameters = new ParameterDescriptor[CACHE_SIZE * CACHE_SIZE];

    /**
     * The prefix of parameter names for matrix elements.
     * This is {@code "elt_"} in WKT 1.
     */
    protected final String prefix;

    /**
     * The separator between row and column in parameter names for matrix elements.
     * This is {@code "_"} in WKT 1.
     */
    protected final String separator;

    /**
     * Constructs a descriptors provider.
     *
     * @param prefix     The prefix to insert in front of parameter name for each matrix elements.
     * @param separator  The separator between the row and the column index in parameter names.
     * @param size       The parameter for the number of rows and columns, usually in an array of length 2.
     *                   Length may be different if the caller wants to generalize usage of this class to tensors.
     */
    @SafeVarargs
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MatrixParameters(final String prefix, final String separator, final ParameterDescriptor<Integer>... size) {
        ArgumentChecks.ensureNonNull("prefix",    prefix);
        ArgumentChecks.ensureNonNull("separator", separator);
        ArgumentChecks.ensureNonNull("size",      size);
        if (size.length == 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "size"));
        }
        this.prefix    = prefix;
        this.separator = separator;
        this.size      = new ParameterDescriptor[size.length];
        for (int i=0; i<size.length; i++) {
            ArgumentChecks.ensureNonNullElement("size", i, this.size[i] = size[i]);
        }
    }

    /**
     * Creates a new parameter descriptor for a matrix or tensor element at the given indices.
     * The returned parameter shall have a {@linkplain DefaultParameterDescriptor#getName() name}
     * parsable by the {@link #parseElementName(String)} method.
     *
     * {@section Default implementation}
     * The default implementation requires an {@code indices} array having the same length than the {@code size} array
     * specified at construction time. That length is usually 2, where {@code indices[0]} is the <var>row</var> index
     * and {@code indices[1]} is the <var>column</var> index. Then this method builds a name with the "{@link #prefix}
     * + <var>row</var> + {@link #separator} + <var>column</var> + …" pattern (e.g. {@code "elt_0_0"}).
     * Finally this method creates a descriptor for an optional parameter of that name.
     *
     * <div class="note"><b>Note:</b> matrices can been seen as a special case of tensors with indices arrays
     * of length 2 (i.e. <cite>second-order</cite> tensor).</div>
     *
     * {@section Subclassing}
     * If a subclass overrides this method for creating parameters with different names, then that subclass shall
     * also override {@link #parseElementName(String)} for parsing those names.
     *
     * @param  indices The indices of the tensor element for which to create a parameter.
     * @return The parameter descriptor for the given tensor element.
     * @throws IllegalArgumentException If the given array does not have the expected length or have illegal value.
     *
     * @see #parseElementName(String)
     */
    protected ParameterDescriptor<Double> createElementDescriptor(final int[] indices) throws IllegalArgumentException {
        if (indices.length != size.length) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.UnexpectedArrayLength_2, size.length, indices.length));
        }
        final StringBuilder name = new StringBuilder();
        String s = prefix;
        int p = indices[0];
        boolean isDiagonal = false;
        for (final int i : indices) {
            isDiagonal &= (i == p);
            name.append(s).append(i);
            s = separator;
            p = i;
        }
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(ReferenceIdentifier.CODE_KEY, name.toString());
        properties.put(ReferenceIdentifier.AUTHORITY_KEY, size[0].getName().getAuthority());
        return new DefaultParameterDescriptor<>(properties, Double.class, null, null, isDiagonal ? 1.0 : 0.0, false);
    }

    /**
     * Checks in the cache before to delegate to {@link #createElementDescriptor(int[])}.
     */
    private ParameterDescriptor<Double> descriptor(final int[] indices) {
        int cacheIndex = -1;
        if (indices.length >= 2 && zeroTail(indices)) {
            final int row = indices[0];
            final int col = indices[1];
            if (row < CACHE_SIZE && col < CACHE_SIZE) {
                final ParameterDescriptor<Double> param;
                cacheIndex = row*CACHE_SIZE + col;
                synchronized (parameters) {
                    param = parameters[cacheIndex];
                }
                if (param != null) {
                    return param;
                }
            }
        }
        /*
         * Parameter not found in the cache. Create a new one and cache it for future reuse.
         * Note that an other thread could have created the same descriptor in the main time,
         * so we will need to check again.
         */
        final ParameterDescriptor<Double> param = createElementDescriptor(indices);
        if (cacheIndex >= 0) {
            synchronized (parameters) {
                final ParameterDescriptor<Double> existing = parameters[cacheIndex];
                if (existing != null) {
                    return existing;
                }
                parameters[cacheIndex] = param;
            }
        }
        return param;
    }

    /**
     * Returns {@code true} if all array elements starting at index 2 are 0.
     * Used only in order to determine if we can cache a descriptor, because
     * we limit the cache to a two-dimensional plane.
     */
    private static boolean zeroTail(final int[] indices) {
        for (int i=2; i<indices.length; i++) {
            if (indices[i] != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the indices of matrix element for the given parameter name.
     * This method is the converse of {@link #createElementDescriptor(int[])}.
     *
     * {@section Default implementation}
     * The default implementation expects a name matching the "{@link #prefix} + <var>row</var> + {@link #separator} +
     * <var>column</var> + …" pattern and returns an array containing the <var>row</var>, <var>column</var> and other
     * indices, in that order.
     *
     * @param  name The parameter name to parse.
     * @return Indices of the tensor element of the given name, or {@code null} if the name is not recognized.
     * @throws IllegalArgumentException If the name has been recognized but an error occurred while parsing it
     *         (e.g. an {@link NumberFormatException}, which is an {@code IllegalArgumentException} subclass).
     */
    protected int[] parseElementName(final String name) throws IllegalArgumentException {
        int s = prefix.length();
        if (!name.regionMatches(true, 0, prefix, 0, s)) {
            return null;
        }
        final int[] indices = new int[size.length];
        final int last = indices.length - 1;
        for (int i=0; i<last; i++) {
            final int split = name.indexOf(separator, s);
            if (split < 0) {
                return null;
            }
            indices[i] = Integer.parseInt(name.substring(s, split));
            s = split + 1;
        }
        indices[last] = Integer.parseInt(name.substring(s));
        return indices;
    }

    /**
     * Returns the descriptor in this group for the specified name.
     *
     * @param  name The case insensitive name of the parameter to search for.
     * @param  actualSize The current values of parameters that define the matrix (or tensor) size.
     * @return The parameter for the given name.
     * @throws ParameterNotFoundException if there is no parameter for the given name.
     */
    final ParameterDescriptor<?> descriptor(final ParameterDescriptorGroup caller,
            String name, final int[] actualSize) throws ParameterNotFoundException
    {
        ArgumentChecks.ensureNonNull("name", name);
        name = CharSequences.trimWhitespaces(name);
        IllegalArgumentException cause = null;
        int[] indices = null;
        try {
            indices = parseElementName(name);
        } catch (IllegalArgumentException exception) {
            cause = exception;
        }
        if (indices != null && isInBounds(indices, actualSize)) {
            return descriptor(indices);
        }
        /*
         * The given name is not a matrix (or tensor) element name.
         * Verify if the requested parameters is one of those that
         * specify the matrix/tensor size ("num_row" or "num_col").
         */
        for (final ParameterDescriptor<Integer> param : size) {
            if (IdentifiedObjects.isHeuristicMatchForName(param, name)) {
                return param;
            }
        }
        ParameterNotFoundException e = new ParameterNotFoundException(Errors.format(
                Errors.Keys.ParameterNotFound_2, caller.getName(), name), name);
        e.initCause(cause);
        throw e;
    }

    /**
     * Returns {@code true} if the given indices are not out-of-bounds.
     *
     * @param indices    The indices parsed from a parameter name.
     * @param actualSize The current values of parameters that define the matrix (or tensor) size.
     */
    private static boolean isInBounds(final int[] indices, final int[] actualSize) {
        for (int i=0; i<indices.length; i++) {
            final int index = indices[i];
            if (index < 0 || index >= actualSize[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns all parameters in this group for a tensor of the specified size.
     *
     * @param  actualSize The current values of parameters that define the matrix (or tensor) size.
     *         It is caller's responsibility to ensure that this array does not contain negative values.
     * @return The matrix parameters, including all elements.
     */
    final List<GeneralParameterDescriptor> descriptors(final int[] actualSize) {
        final int dimension = size.length; // 2 for a matrix, may be higher for a tensor.
        int length = actualSize[0];
        for (int i=1; i<dimension; i++) {
            length *= actualSize[i];
        }
        final GeneralParameterDescriptor[] parameters = new GeneralParameterDescriptor[dimension + length];
        System.arraycopy(size, 0, parameters, 0, dimension);
        final int[] indices = new int[dimension];
        /*
         * Iterates on all possible index values. Indes on the right side (usually the column index)
         * will vary faster, and index on the left side (usually the row index) will vary slowest.
         */
        for (int i=0; i<length; i++) {
            parameters[dimension + i] = descriptor(indices);
            for (int j=dimension; --j >= 0;) {
                if (++indices[j] < actualSize[j]) {
                    break;
                }
                indices[j] = 0; // We have done a full turn at that dimension. Will increment next dimension.
            }
        }
        assert Arrays.equals(actualSize, indices) : Arrays.toString(actualSize);
        return UnmodifiableArrayList.wrap(parameters);
    }

    /**
     * Creates a new instance of parameter group with matrix elements initialized to the 1 on the diagonal,
     * and 0 everywhere else. The returned parameter group is extensible, i.e. the number of elements will
     * depend upon the value associated to the {@code numRow} and {@code numCol} parameters.
     *
     * @return A new parameter initialized to the default value.
     */
//  public ParameterValueGroup createValue() {
//      return new MatrixParameterValues(this);
//  }

    /**
     * Constructs a matrix from a group of parameters. This operation is allowed only if this
     * {@code MatrixParameters} has been created with a {@code size} array of length 2.
     *
     * @param  parameters The group of parameters.
     * @return A matrix constructed from the specified group of parameters.
     * @throws InvalidParameterNameException if a parameter name was not recognized.
     */
    public Matrix getMatrix(final ParameterValueGroup parameters) throws InvalidParameterNameException {
        ArgumentChecks.ensureNonNull("parameters", parameters);
        if (size.length != 2) {
            throw new IllegalStateException();
        }
//      if (parameters instanceof MatrixParameterValues) {
//          // More efficient implementation
//          return ((MatrixParameterValues) parameters).getMatrix();
//      }
        // Fallback on the general case (others implementations)
        final ParameterValue<?> numRow = parameters.parameter(size[0].getName().getCode());
        final ParameterValue<?> numCol = parameters.parameter(size[1].getName().getCode());
        final Matrix matrix = Matrices.createDiagonal(numRow.intValue(), numCol.intValue());
        final List<GeneralParameterValue> values = parameters.values();
        if (values != null) {
            for (final GeneralParameterValue param : values) {
                if (param == numRow || param == numCol) {
                    continue;
                }
                final String name = param.getDescriptor().getName().toString();
                IllegalArgumentException cause = null;
                int[] indices = null;
                try {
                    indices = parseElementName(name);
                } catch (IllegalArgumentException e) {
                    cause = e;
                }
                if (indices == null) {
                    throw new InvalidParameterNameException(Errors.format(
                                Errors.Keys.UnexpectedParameter_1, name), cause, name);
                }
                matrix.setElement(indices[0], indices[1], ((ParameterValue<?>) param).doubleValue());
            }
        }
        return matrix;
    }

    // TODO: restore parameters on deserialization.
}
