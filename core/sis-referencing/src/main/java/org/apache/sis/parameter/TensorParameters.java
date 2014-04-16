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
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
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
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

// Related to JDK7
import java.util.Objects;


/**
 * Creates parameter groups for tensors (usually matrices).
 * Matrices are handled as a special case of tensors (<cite>second-order</cite> tensors).
 *
 * <p>Each group of parameters contains the following elements:</p>
 * <ul>
 *   <li>A mandatory parameter for the number of rows ({@code "num_row"} in WKT 1).</li>
 *   <li>A mandatory parameter for the number of columns ({@code "num_col"} in WKT 1).</li>
 *   <li>(<i>etc.</i> for third-order or higher-order tensors).</li>
 *   <li>A maximum of {@code num_row} × {@code num_col} × … optional parameters for the matrix or tensor element values.
 *       Parameter names depend on the formatting convention.</li>
 * </ul>
 *
 * For all matrix or tensor elements, the default value is 1 for elements on the diagonal (where all indices have
 * the same value) and 0 for all other elements. Those default values defines an <cite>identity matrix</cite>,
 * or (more generally) <cite>Kroenecker delta tensor</cite>.
 *
 * <p><b>Parameters are not an efficient storage format for large tensors.</b>
 * Parameters are used only for small matrices/tensors to be specified in coordinate operations or processing libraries.
 * In particular, those parameters integrate well in <cite>Well Known Text</cite> (WKT) format.
 * For a more efficient matrix storage, see {@link org.apache.sis.referencing.operation.matrix.MatrixSIS}.</p>
 *
 * {@section Formatting}
 * The parameters format for a matrix is typically like below:
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
 * matrix or tensor parameters is not immutable.
 *
 * {@section Usage}
 * For creating a new group of parameters for a matrix using the {@link #WKT1} naming conventions,
 * on can use the following code:
 *
 * {@preformat java
 *   Map<String,?> properties = Collections.singletonMap("name", "My operation");
 *   ParameterValueGroup p = TensorParameters.WKT1.createValueGroup(properties);
 * }
 *
 * @param <E> The type of tensor element values.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
@SuppressWarnings("unchecked")
public class TensorParameters<E> implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7386537348359343836L;

    /**
     * Parses and creates parameters names for matrices matching the
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
    public static final TensorParameters<Double> WKT1;
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
        WKT1 = new TensorParameters<>(Double.class, "elt_", "_", numRow, numCol);
    }

    /**
     * The height and weight of the matrix of {@link #parameters} to cache. Descriptors
     * for row or column indices greater than or equal to this value will not be cached.
     * A small value is sufficient since matrix sizes are usually the maximum number of
     * CRS dimensions (usually 4) plus one.
     */
    static final int CACHE_SIZE = 5;

    /**
     * Maximal cache rank. Memory required by the cache will be {@code pow(CACHE_SIZE, CACHE_RANK)},
     * so that value is better to be small.
     */
    private static final int CACHE_RANK = 3;

    /**
     * The type of tensor element values.
     */
    private final Class<E> elementType;

    /**
     * The parameters that define the number of rows, columns or other dimensions.
     * In WKT1, the parameter names are {@code "num_row"} and {@code "num_col"} respectively.
     *
     * <p>The length of this array determine the tensor {@linkplain #rank() rank}.</p>
     */
    private final ParameterDescriptor<Integer>[] dimensions;

    /**
     * The cached descriptors for each elements in a tensor. Descriptors do not depend on tensor element values.
     * Consequently, the same descriptors can be reused for all {@link MatrixParameterValues} instances.
     */
    private final transient ParameterDescriptor<E>[] parameters;

    /**
     * The elements for the 0 and 1 values, or {@code null} if unknown.
     * Computed by {@link #createCache()}.
     */
    private transient E zero, one;

    /**
     * The prefix of parameter names for tensor elements.
     * This is {@code "elt_"} in WKT 1.
     */
    protected final String prefix;

    /**
     * The separator between row and column in parameter names for tensor elements.
     * This is {@code "_"} in WKT 1.
     */
    protected final String separator;

    /**
     * Constructs a descriptors provider.
     *
     * @param elementType The type of tensor element values.
     * @param prefix      The prefix to insert in front of parameter name for each tensor elements.
     * @param separator   The separator between dimension (row, column, …) indices in parameter names.
     * @param dimensions  The parameter for the size of each dimension, usually in an array of length 2.
     *                    Length may be different if the caller wants to generalize usage of this class to tensors.
     */
    @SafeVarargs
    @SuppressWarnings("rawtypes")
    public TensorParameters(final Class<E> elementType, final String prefix, final String separator,
            final ParameterDescriptor<Integer>... dimensions)
    {
        ArgumentChecks.ensureNonNull("elementType", elementType);
        ArgumentChecks.ensureNonNull("prefix",      prefix);
        ArgumentChecks.ensureNonNull("separator",   separator);
        ArgumentChecks.ensureNonNull("dimensions",  dimensions);
        if (dimensions.length == 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "dimensions"));
        }
        this.elementType = elementType;
        this.prefix      = prefix;
        this.separator   = separator;
        this.dimensions  = new ParameterDescriptor[dimensions.length];
        for (int i=0; i<dimensions.length; i++) {
            ArgumentChecks.ensureNonNullElement("dimensions", i, this.dimensions[i] = dimensions[i]);
        }
        parameters = createCache();
    }

    /**
     * Initializes the fields used for cached values: {@link #zero}, {@link #one} and the {@link #parameters} array.
     * The later is not assigned to the {@code parameters} field, but rather returned.
     * Caller shall assign himself the returned value to the {@link #parameters} field.
     *
     * <p>This method is invoked by constructor and on deserialization.</p>
     */
    @SuppressWarnings("rawtypes")
    private <T> ParameterDescriptor<T>[] createCache() {
        if (Number.class.isAssignableFrom(elementType)) try {
            one  = (E) Numbers.wrap(1, (Class) elementType);
            zero = (E) Numbers.wrap(0, (Class) elementType);
        } catch (IllegalArgumentException e) {
            // Ignore - zero and one will be left to null.
        }
        int length = 1;
        for (int i = Math.min(dimensions.length, CACHE_RANK); --i >= 0;) {
            length *= CACHE_SIZE;
        }
        return new ParameterDescriptor[length];
    }

    /**
     * Returns the type of tensor element values.
     *
     * @return The type of tensor element values.
     */
    public final Class<E> getElementType() {
        return elementType;
    }

    /**
     * Returns the rank of the tensor objects for which this instance will create parameters.
     * The rank determines the type of objects represented by the parameters:
     *
     * <table class="sis">
     *   <caption>Tensor types implied by rank</caption>
     *   <tr><th>Rank</th> <th>Type</th></tr>
     *   <tr><td>0</td>    <td>scalar</td></tr>
     *   <tr><td>1</td>    <td>vector</td></tr>
     *   <tr><td>2</td>    <td>matrix</td></tr>
     *   <tr><td><var>k</var></td><td>rank <var>k</var> tensor</td></tr>
     * </table>
     *
     * @return The rank of the tensors for which to create parameters.
     */
    public final int rank() {
        return dimensions.length;
    }

    /**
     * Returns the parameter descriptor for the dimension at the given index.
     *
     * @param  i The dimension index, from 0 inclusive to {@link #rank()} exclusive.
     * @return The parameter descriptor for the dimension at the given index.
     */
    public final ParameterDescriptor<Integer> getDimensionDescriptor(final int i) {
        return dimensions[i];
    }

    /**
     * Returns the parameter descriptor for a matrix or tensor element at the given indices.
     * The length of the given {@code indices} array shall be equals to the {@linkplain #rank() rank}.
     * That length is usually 2, where {@code indices[0]} is the <var>row</var> index and {@code indices[1]}
     * is the <var>column</var> index.
     *
     * @param  indices The indices of the tensor element for which to get the descriptor.
     * @return The parameter descriptor for the given tensor element.
     * @throws IllegalArgumentException If the given array does not have the expected length or have illegal value.
     */
    public final ParameterDescriptor<E> getElementDescriptor(final int... indices) {
        final int cacheIndex = cacheIndex(indices);
        if (cacheIndex >= 0) {
            final ParameterDescriptor<E> param;
            synchronized (parameters) {
                param = parameters[cacheIndex];
            }
            if (param != null) {
                return param;
            }
        }
        /*
         * Parameter not found in the cache. Create a new one and cache it for future reuse.
         * Note that an other thread could have created the same descriptor in the main time,
         * so we will need to check again.
         */
        final ParameterDescriptor<E> param = createElementDescriptor(indices);
        if (cacheIndex >= 0) {
            synchronized (parameters) {
                final ParameterDescriptor<E> existing = parameters[cacheIndex];
                if (existing != null) {
                    return existing;
                }
                parameters[cacheIndex] = param;
            }
        }
        return param;
    }

    /**
     * Returns the index in the cache for the given indices, or -1 if that elements is not cached.
     */
    private static int cacheIndex(final int[] indices) {
        int cacheIndex = 0;
        for (int i=0; i<indices.length; i++) {
            final int index = indices[i];
            if (i < CACHE_RANK) {
                if (index >= 0 && index < CACHE_SIZE) {
                    cacheIndex = (cacheIndex * CACHE_SIZE) + index;
                    continue;
                }
            } else if (index == 0) {
                continue;
            }
            return -1;
        }
        return cacheIndex;
    }

    /**
     * Creates a new parameter descriptor for a matrix or tensor element at the given indices.
     * This method is invoked by {@link #getElementDescriptor(int[])} when a new descriptor needs
     * to be created.
     *
     * {@section Default implementation}
     * The default implementation converts the given indices to a parameter name by invoking the
     * {@link #indicesToName(int[])} method, then creates a descriptor for an optional parameter
     * of that name.
     *
     * {@section Subclassing}
     * Subclasses can override this method if they want more control on descriptor properties
     * like identification information, value domain and default values.
     *
     * @param  indices The indices of the tensor element for which to create a parameter.
     * @return The parameter descriptor for the given tensor element.
     * @throws IllegalArgumentException If the given array does not have the expected length or have illegal value.
     *
     * @see #indicesToName(int[])
     * @see #nameToIndices(String)
     */
    protected ParameterDescriptor<E> createElementDescriptor(final int[] indices) throws IllegalArgumentException {
        boolean isDiagonal = true;
        for (int i=1; i<indices.length; i++) {
            if (indices[i] != indices[i-1]) {
                isDiagonal = false;
                break;
            }
        }
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(ReferenceIdentifier.CODE_KEY, indicesToName(indices));
        properties.put(ReferenceIdentifier.AUTHORITY_KEY, dimensions[0].getName().getAuthority());
        return new DefaultParameterDescriptor<>(properties, elementType, null, null, isDiagonal ? one : zero, false);
    }

    /**
     * Returns the parameter descriptor name of a matrix or tensor element at the given indices.
     * The returned name shall be parsable by the {@link #nameToIndices(String)} method.
     *
     * {@section Default implementation}
     * The default implementation requires an {@code indices} array having a length equals to the {@linkplain #rank()
     * rank}. That length is usually 2, where {@code indices[0]} is the <var>row</var> index and {@code indices[1]} is
     * the <var>column</var> index. Then this method builds a name with the “{@link #prefix} + <var>row</var> +
     * {@link #separator} + <var>column</var> + …” pattern (e.g. {@code "elt_0_0"}).
     *
     * {@section Subclassing}
     * If a subclass overrides this method for creating different names, then that subclass shall
     * also override {@link #nameToIndices(String)} for parsing those names.
     *
     * @param  indices The indices of the tensor element for which to create a parameter name.
     * @return The parameter descriptor name for the tensor element at the given indices.
     * @throws IllegalArgumentException If the given array does not have the expected length or have illegal value.
     */
    protected String indicesToName(final int[] indices) throws IllegalArgumentException {
        if (indices.length != dimensions.length) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.UnexpectedArrayLength_2, dimensions.length, indices.length));
        }
        final StringBuilder name = new StringBuilder();
        String s = prefix;
        for (final int i : indices) {
            name.append(s).append(i);
            s = separator;
        }
        return name.toString();
    }

    /**
     * Returns the indices of matrix element for the given parameter name, or {@code null} if none.
     * This method is the converse of {@link #indicesToName(int[])}.
     *
     * {@section Default implementation}
     * The default implementation expects a name matching the “{@link #prefix} + <var>row</var> + {@link #separator} +
     * <var>column</var> + …” pattern and returns an array containing the <var>row</var>, <var>column</var> and other
     * indices, in that order.
     *
     * @param  name The parameter name to parse.
     * @return Indices of the tensor element of the given name, or {@code null} if the name is not recognized.
     * @throws IllegalArgumentException If the name has been recognized but an error occurred while parsing it
     *         (e.g. an {@link NumberFormatException}, which is an {@code IllegalArgumentException} subclass).
     */
    protected int[] nameToIndices(final String name) throws IllegalArgumentException {
        int s = prefix.length();
        if (!name.regionMatches(true, 0, prefix, 0, s)) {
            return null;
        }
        final int[] indices = new int[dimensions.length];
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
     * @param  actualSize The current values of parameters that define the matrix (or tensor) dimensions.
     * @return The parameter for the given name.
     * @throws ParameterNotFoundException if there is no parameter for the given name.
     */
    final ParameterDescriptor<?> descriptor(final ParameterDescriptorGroup caller,
            final String name, final int[] actualSize) throws ParameterNotFoundException
    {
        IllegalArgumentException cause = null;
        int[] indices = null;
        try {
            indices = nameToIndices(name);
        } catch (IllegalArgumentException exception) {
            cause = exception;
        }
        if (indices != null && isInBounds(indices, actualSize)) {
            return getElementDescriptor(indices);
        }
        /*
         * The given name is not a matrix (or tensor) element name.
         * Verify if the requested parameters is one of those that
         * specify the matrix/tensor size ("num_row" or "num_col").
         */
        for (final ParameterDescriptor<Integer> param : dimensions) {
            if (IdentifiedObjects.isHeuristicMatchForName(param, name)) {
                return param;
            }
        }
        throw (ParameterNotFoundException) new ParameterNotFoundException(Errors.format(
                Errors.Keys.ParameterNotFound_2, caller.getName(), name), name).initCause(cause);
    }

    /**
     * Returns {@code true} if the given indices are not out-of-bounds.
     *
     * @param indices    The indices parsed from a parameter name.
     * @param actualSize The current values of parameters that define the matrix (or tensor) dimensions.
     */
    static boolean isInBounds(final int[] indices, final int[] actualSize) {
        for (int i=0; i<indices.length; i++) {
            final int index = indices[i];
            if (index < 0 || index >= actualSize[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns all parameters in this group for a tensor of the specified dimensions.
     *
     * @param  actualSize The current values of parameters that define the matrix (or tensor) dimensions.
     *         It is caller's responsibility to ensure that this array does not contain negative values.
     * @return The matrix parameters, including all elements.
     */
    final List<GeneralParameterDescriptor> descriptors(final int[] actualSize) {
        final int rank = dimensions.length; // 2 for a matrix, may be higher for a tensor.
        int length = actualSize[0];
        for (int i=1; i<rank; i++) {
            length *= actualSize[i];
        }
        final GeneralParameterDescriptor[] parameters = new GeneralParameterDescriptor[rank + length];
        System.arraycopy(dimensions, 0, parameters, 0, rank);
        final int[] indices = new int[rank];
        /*
         * Iterates on all possible index values. Indes on the right side (usually the column index)
         * will vary faster, and index on the left side (usually the row index) will vary slowest.
         */
        for (int i=0; i<length; i++) {
            parameters[rank + i] = getElementDescriptor(indices);
            for (int j=rank; --j >= 0;) {
                if (++indices[j] < actualSize[j]) {
                    break;
                }
                indices[j] = 0; // We have done a full turn at that dimension. Will increment next dimension.
            }
        }
        return UnmodifiableArrayList.wrap(parameters);
    }

    /**
     * Creates a new instance of parameter group with default values of 1 on the diagonal, and 0 everywhere else.
     * The returned parameter group is extensible, i.e. the number of elements will depend upon the value associated
     * to the parameters that define the matrix (or tensor) dimension.
     *
     * <p>The properties map is given unchanged to the
     * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#AbstractIdentifiedObject(Map)
     * identified object constructor}. The following table is a reminder of main (not all) properties:</p>
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link DefaultParameterDescriptorGroup#getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link DefaultParameterDescriptorGroup#getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link DefaultParameterDescriptorGroup#getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link DefaultParameterDescriptorGroup#getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties The properties to be given to the identified object.
     * @return A new parameter group initialized to the default values.
     */
    public ParameterValueGroup createValueGroup(final Map<String,?> properties) {
        return new TensorValues<>(properties, this);
    }

    /**
     * Creates a new instance of parameter group initialized to the given matrix.
     * This operation is allowed only for tensors of {@linkplain #rank() rank} 2.
     *
     * @param  properties The properties to be given to the identified object.
     * @param  matrix The matrix to copy in the new parameter group.
     * @return A new parameter group initialized to the given matrix.
     */
    public ParameterValueGroup createValueGroup(final Map<String,?> properties, final Matrix matrix) {
        if (dimensions.length != 2) {
            throw new IllegalStateException();
        }
        ArgumentChecks.ensureNonNull("matrix", matrix);
        final TensorValues<E> values = new TensorValues<>(properties, this);
        values.setMatrix(matrix);
        return values;
    }

    /**
     * Constructs a matrix from a group of parameters.
     * This operation is allowed only for tensors of {@linkplain #rank() rank} 2.
     *
     * @param  parameters The group of parameters.
     * @return A matrix constructed from the specified group of parameters.
     * @throws InvalidParameterNameException if a parameter name was not recognized.
     */
    public Matrix toMatrix(final ParameterValueGroup parameters) throws InvalidParameterNameException {
        if (dimensions.length != 2) {
            throw new IllegalStateException();
        }
        ArgumentChecks.ensureNonNull("parameters", parameters);
        if (parameters instanceof TensorValues) {
            return ((TensorValues) parameters).toMatrix(); // More efficient implementation
        }
        // Fallback on the general case (others implementations)
        final ParameterValue<?> numRow = parameters.parameter(dimensions[0].getName().getCode());
        final ParameterValue<?> numCol = parameters.parameter(dimensions[1].getName().getCode());
        final Matrix matrix = Matrices.createDiagonal(numRow.intValue(), numCol.intValue());
        final List<GeneralParameterValue> values = parameters.values();
        if (values != null) {
            for (final GeneralParameterValue param : values) {
                if (param == numRow || param == numCol) {
                    continue;
                }
                final String name = param.getDescriptor().getName().getCode();
                IllegalArgumentException cause = null;
                int[] indices = null;
                try {
                    indices = nameToIndices(name);
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

    /**
     * Returns a hash code value for this object.
     *
     * @return A hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(elementType, prefix, separator) ^ Arrays.hashCode(dimensions);
    }

    /**
     * Compares this object with the given object for equality.
     *
     * @param other The other object to compare with this object.
     * @return {@code true} if both object are equal.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other.getClass() == getClass()) {
            final TensorParameters<?> that = (TensorParameters<?>) other;
            return elementType.equals(that.elementType) &&
                   prefix     .equals(that.prefix)      &&
                   separator  .equals(that.separator)   &&
                   Arrays.equals(dimensions, that.dimensions);
        }
        return false;
    }

    /**
     * Invoked on deserialization for restoring the {@link #parameters} array.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        try {
            final Field field = TensorParameters.class.getDeclaredField("parameters");
            field.setAccessible(true);
            field.set(this, createCache());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
