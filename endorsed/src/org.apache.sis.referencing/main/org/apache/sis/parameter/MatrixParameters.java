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
import java.util.Objects;
import java.io.Serializable;
import java.io.ObjectStreamException;
import org.apache.sis.math.NumberType;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterNameException;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.provider.Affine;
import org.apache.sis.referencing.operation.provider.EPSGName;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;


/**
 * Builder of parameter groups for matrices or mathematical objects of higher dimensions.
 * While this class is primarily used for reading and writing matrix parameters in Well Known Text format
 * (i.e., in two-dimensional arrays of coefficients for multi-dimensional transforms),
 * this class can also be used for describing parameters in multidimensional arrays.
 * Each group of parameters contains the following elements:
 * <ul>
 *   <li>Parameters for specifying the size along each dimension:
 *     <ul>
 *       <li>number of rows (named {@code "num_row"} in {@linkplain #WKT1} conventions),</li>
 *       <li>number of columns (named {@code "num_col"} in <abbr>WKT</abbr> 1 convention),</li>
 *       <li><i>etc.</i> for multi-dimensional arrays of more than two dimensions.</li>
 *     </ul>
 *   </li>
 *   <li>A maximum of {@code num_row} × {@code num_col} × … optional parameters for the matrix element values.
 *       The parameter names depend on the formatting convention.</li>
 * </ul>
 *
 * In matrices, the default value is 1 for all elements on the diagonal and 0 everywhere else.
 * For object of higher dimensions, this is generalized to 1 in elements where all indices have the same value.
 * Those default values defines an <i>identity matrix</i> (or more generally, a Kroenecker delta tensor).
 *
 * <h2>Formatting</h2>
 * In the usual case of a matrix, the parameters are typically formatted as below.
 * Note that in the <abbr>EPSG</abbr> convention, the matrix is implicitly {@linkplain Matrices#isAffine affine}
 * and of dimension 3×3. The <abbr>EPSG</abbr> database also contains A3, A4, A5, A6, A7, A8 and B3 parameters,
 * but they are for polynomial transformations, not for affine transformations.
 *
 * <table class="sis">
 *   <caption>Well Known Text (<abbr>WKT</abbr>) formats for matrix parameters</caption>
 * <tr>
 *   <th>Using <abbr>EPSG</abbr>:9624 names and identifiers</th>
 *   <th class="sep">Using <abbr>OGC</abbr> names</th>
 * </tr>
 * <tr><td>
 * {@snippet lang="wkt" :
 *   Parameter["A0", <value>, Id["EPSG", 8623]],
 *   Parameter["A1", <value>, Id["EPSG", 8624]],
 *   Parameter["A2", <value>, Id["EPSG", 8625]],
 *   Parameter["B0", <value>, Id["EPSG", 8639]],
 *   Parameter["B1", <value>, Id["EPSG", 8640]],
 *   Parameter["B2", <value>, Id["EPSG", 8641]]
 * }
 *
 * </td><td class="sep">
 * {@snippet lang="wkt" :
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
 * </td></tr></table>
 *
 * Those groups are extensible, i.e. the number of <code>"elt_<var>row</var>_<var>col</var>"</code> parameters
 * depends on the {@code "num_row"} and {@code "num_col"} parameter values. For this reason, the descriptor of
 * matrix parameters is not immutable.
 *
 * <h2>Usage examples</h2>
 * For creating a new group of parameters for a matrix using the {@link #WKT1} naming conventions,
 * one can use the following code:
 *
 * {@snippet lang="java" :
 *     Map<String,?> properties = Map.of(ParameterValueGroup.NAME_KEY, "Affine");
 *     ParameterValueGroup p = MatrixParameters.WKT1.createValueGroup(properties);
 *     }
 *
 * For setting the elements of a few values, the next step is to create a matrix from the parameter values:
 *
 * {@snippet lang="java" :
 *     p.parameter("elt_0_0").setValue(4);
 *     p.parameter("elt_1_1").setValue(6);
 *     Matrix m = MatrixParameters.WKT1.toMatrix(p);
 *     }
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.6
 *
 * @param <E>  the type of matrix element values.
 *
 * @see org.apache.sis.referencing.operation.matrix.Matrices
 * @since 1.5
 */
public class MatrixParameters<E> implements CheckedContainer<E>, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8161073830003749123L;

    /**
     * Parses and creates matrix parameters with names matching the
     * Well Known Text version 1 (<abbr>WKT</abbr> 1) convention.
     *
     * <ul>
     *   <li>First parameter is {@code "num_row"}.</li>
     *   <li>Second parameter is {@code "num_col"}.</li>
     *   <li>All other parameters are of the form <code>"elt_<var>row</var>_<var>col</var>"</code>
     *       where <var>row</var> and <var>col</var> are zero-based.</li>
     * </ul>
     *
     * For example, {@code "elt_1_2"} is the element name for the value at row 1 and column 2 (zero-based indices).
     * There are no alphanumeric aliases for avoiding confusion between {@link #ALPHANUM} and {@link #EPSG}.
     */
    public static final MatrixParameters<Double> WKT1 =
            new MatrixParameters<>(Double.class, "elt_", "_", dimensionsForMatrix(1));

    /**
     * Return the dimensions for a matrix with the names defined by <abbr>OGC</abbr> <abbr>WKT</abbr> 1.
     * For <abbr>WKT</abbr> 1 matrices, the {@code num_row} and {@code num_col} parameters are mandatory.
     *
     * <h4>Implementation note</h4>
     * The parameters use an arbitrary upper limit. A high size value doesn't make much sense
     * anyway because matrix size for projective transforms will usually not be much more than 5,
     * and the storage scheme used in this implementation is inefficient for large number of matrix elements.
     *
     * @param  minimumOccurs  1 if the parameters should be mandatory, or 0 if the parameters should be optional.
     * @return the parameters for specifying the size of a two-dimensional array (i.e., a matrix).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})    // Generic array creation.
    private static ParameterDescriptor<Integer>[] dimensionsForMatrix(final int minimumOccurs) {
        final NumberRange<Integer> valueDomain = NumberRange.create(1, true, 50, true);     // See javadoc.
        final Integer defaultSize = Affine.EPSG_DIMENSION + 1;
        return new DefaultParameterDescriptor[] {
            new DefaultParameterDescriptor<>(
                    Map.of(Identifier.AUTHORITY_KEY, Citations.OGC,
                           Identifier.CODE_KEY, Constants.NUM_ROW),
                    minimumOccurs, 1, Integer.class, valueDomain, null, defaultSize),
            new DefaultParameterDescriptor<>(
                    Map.of(Identifier.AUTHORITY_KEY, Citations.OGC,
                           Identifier.CODE_KEY, Constants.NUM_COL),
                    minimumOccurs, 1, Integer.class, valueDomain, null, defaultSize)
        };
    }

    /**
     * Parses and creates matrix parameters with alphanumeric names.
     * {@linkplain ParameterDescriptor#getName() Names} are made of a letter indicating the row
     * (first row is {@code "A"}), followed by a digit indicating the column index (first column is {@code "0"}).
     * {@linkplain ParameterDescriptor#getAlias() Aliases} are the names as they were defined in version 1
     * of <i>Well Known Text</i> (<abbr>WKT</abbr>) format.
     *
     * <table class="sis">
     *   <caption>Parameter names for a 3×3 matrix</caption>
     *   <tr>
     *     <th>Primary name</th>
     *     <th class="sep">Alias</th>
     *   </tr>
     * <tr><td>
     * <pre class="text">
     *   ┌            ┐
     *   │ A0  A1  A2 │
     *   │ B0  B1  B2 │
     *   │ C0  C1  C2 │
     *   └            ┘</pre>
     * </td><td class="sep">
     * <pre class="text">
     *   ┌                             ┐
     *   │ elt_0_0   elt_0_1   elt_0_2 │
     *   │ elt_1_0   elt_1_1   elt_1_2 │
     *   │ elt_2_0   elt_2_1   elt_2_2 │
     *   └                             ┘</pre>
     * </td></tr>
     * </table>
     */
    public static final MatrixParameters<Double> ALPHANUM = new MatrixParameters<>(WKT1) {
        /** Returns the indices for the given name or alias, or {@code null} if not recognized. */
        @Override protected int[] nameToIndices(final String name) {
            if (name.length() == 2) {
                final int row = name.charAt(0) - 'A';
                if (row >= 0 && row <= 'Z' - 'A') {
                    final int col = name.charAt(1) - '0';
                    if (col >= 0 && col <= 9) {
                        return new int[] {row, col};
                    }
                }
            }
            return super.nameToIndices(name);   // Parse as OGC's WKT 1 name.
        }

        /** Returns the alphanumeric name for the given indices, or {@code null} if none. */
        @Override protected String indicesToName(final int[] indices) {
            final int row = indices[0];
            if (row >= 0 && row <= 'Z' - 'A') {
                final int col = indices[1];
                if (col >= 0 && col <= 9) {
                    return String.valueOf(new char[] {(char) ('A' + row), (char) ('0' + col)});
                }
            }
            return null;
        }

        /** Creates the name and alias for the given indices. */
        @Override protected Map<String, ?> properties(final int[] indices) {
            final var alias = new NamedIdentifier(Citations.OGC, super.indicesToName(indices));
            final var properties = new HashMap<String, Object>(4);
            final String name = indicesToName(indices);
            if (name != null) {
                properties.put(ParameterDescriptor.NAME_KEY, new NamedIdentifier(Citations.SIS, name));
                properties.put(ParameterDescriptor.ALIAS_KEY, alias);
            } else {
                properties.put(ParameterDescriptor.NAME_KEY, alias);
            }
            return properties;
        }

        /** Returns a string representation for debugging purposes. */
        @Override public String toString() {
            return "ALPHANUM";
        }
    };

    /**
     * Parses and creates matrix parameters with alphanumeric names in the order defined by <abbr>EPSG</abbr>.
     * This is similar to {@link #ALPHANUM}, except that coefficients with index 0 are in the translation column.
     * These parameter names are defined by the <q>Affine parametric transformation</q> (EPSG:9624) operation method.
     * The last row cannot be specified by <abbr>EPSG</abbr> names.
     *
     * <table class="sis">
     *   <caption>Parameter names for a 3×3 matrix</caption>
     *   <tr>
     *     <th>Primary name</th>
     *     <th class="sep">Alias</th>
     *   </tr>
     * <tr><td>
     * <pre class="text">
     *   ┌            ┐
     *   │ A1  A2  A0 │
     *   │ B1  B2  B0 │
     *   │  0   0   1 │
     *   └            ┘</pre>
     * </td><td class="sep">
     * <pre class="text">
     *   ┌                             ┐
     *   │ elt_0_0   elt_0_1   elt_0_2 │
     *   │ elt_1_0   elt_1_1   elt_1_2 │
     *   │ elt_2_0   elt_2_1   elt_2_2 │
     *   └                             ┘</pre>
     * </td></tr>
     * </table>
     */
    @SuppressWarnings("unchecked")
    public static final MatrixParameters<Double> EPSG = new MatrixParameters<>(Double.class, "elt_", "_", dimensionsForMatrix(0)) {
        /** Returns the indices for the given name or alias, or {@code null} if not recognized. */
        @Override protected int[] nameToIndices(final String name) {
            if (name.length() == 2) {
                final int row = name.charAt(0) - 'A';
                if (row == 0 || row == 1) {
                    int col = name.charAt(1) - ('0' + 1);
                    if (col >= -1 && col <= 1) {
                        if (col < 0) col = 2;
                        return new int[] {row, col};
                    }
                }
            }
            return super.nameToIndices(name);   // Parse as OGC's WKT 1 name.
        }

        /** Returns the alphanumeric name for the given indices, or {@code null} if none. */
        @Override protected String indicesToName(final int[] indices) {
            final int row = indices[0];
            if (row == 0 || row == 1) {
                int col = indices[1];
                if (col >= 0 && col <= 2) {
                    if (col == 2) col = -1;
                    return String.valueOf(new char[] {(char) ('A' + row), (char) (('0' + 1) + col)});
                }
            }
            return null;
        }

        /** Creates the name, identifier and alias for the given indices. */
        @Override protected Map<String, ?> properties(final int[] indices) {
            final var alias = new NamedIdentifier(Citations.OGC, super.indicesToName(indices));
            final var properties = new HashMap<String, Object>(6);
            final String name = indicesToName(indices);
            if (name != null) {
                properties.put(ParameterDescriptor.NAME_KEY, new NamedIdentifier(Citations.EPSG, name));
                properties.put(ParameterDescriptor.ALIAS_KEY, alias);
                int code;
                switch (name.charAt(0)) {
                    case 'A': code = Constants.EPSG_A0 - '0'; break;
                    case 'B': code = Constants.EPSG_B0 - '0'; break;
                    default:  return properties;
                }
                code += name.charAt(1);
                properties.put(ParameterDescriptor.IDENTIFIERS_KEY, EPSGName.identifier(code));
            } else {
                properties.put(ParameterDescriptor.NAME_KEY, alias);
            }
            return properties;
        }

        /** Returns a string representation for debugging purposes. */
        @Override public String toString() {
            return "EPSG";
        }
    };

    /**
     * The height and width of the matrix of {@linkplain #parameters} to cache.
     * Descriptors for row or column indices greater than or equal to this value will not be cached.
     * A small value is sufficient since matrix sizes are usually the maximum number of <abbr>CRS</abbr>
     * dimensions (usually 4) plus one.
     */
    static final int MAX_CACHE_SIZE = 5;

    /**
     * Maximal dimension of the mathematical objects to cache. The memory required by the cache will be
     * {@code pow(MAX_CACHE_SIZE, MAX_CACHE_DIMENSION)}, so the value of this constant should be small.
     * For now, we do not create object of higher dimension than matrices,
     * so there is no need to allocate memory for more than matrices.
     */
    private static final int MAX_CACHE_DIMENSION = 2;

    /**
     * The type of matrix element values.
     *
     * @see #getElementType()
     */
    private final Class<E> elementType;

    /**
     * The parameters that define the number of rows, columns or other dimensions.
     * In <abbr>WKT</abbr> 1, the parameter names are {@code "num_row"} and {@code "num_col"} respectively.
     * The length of this array determine the number of dimensions of the multi-dimensional array.
     *
     * @see #order()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final ParameterDescriptor<Integer>[] dimensions;

    /**
     * The cached descriptors for all elements in a matrix. Descriptors do not depend on matrix element values.
     * Consequently, the same descriptors can be reused for all {@link MatrixParameterValues} instances.
     *
     * <p>Consider this field as final.
     * It is not final only for {@link #readObject(ObjectInputStream)} implementation.</p>
     */
    private transient ParameterDescriptor<E>[] parameters;

    /**
     * The elements for the 0 and 1 values, or {@code null} if unknown.
     * Computed by {@link #createCache()}. Consider as final.
     */
    private transient E zero, one;

    /**
     * The prefix of parameter names for matrix elements.
     * This is {@code "elt_"} in <abbr>WKT</abbr> 1.
     */
    protected final String prefix;

    /**
     * The separator between row and column in parameter names for matrix elements.
     * This is {@code "_"} in <abbr>WKT</abbr> 1.
     */
    protected final String separator;

    /**
     * Creates a copy of the given builder of matrix descriptors.
     * The new builder will use the same prefix, separator and dimensions than the given builder.
     * This constructor is for subclasses which use an existing instance as a basis, then override methods.
     *
     * @param  other  the existing builder from which to copy the prefix, separator and dimensions.
     */
    protected MatrixParameters(final MatrixParameters<E> other) {
        elementType = other.elementType;
        prefix      = other.prefix;
        separator   = other.separator;
        dimensions  = other.dimensions;
        parameters  = createCache();
    }

    /**
     * Creates a builder of matrix descriptors.
     *
     * @param elementType  the type of matrix element values.
     * @param prefix       the prefix to insert in front of parameter name for each matrix elements.
     * @param separator    the separator between dimension (row, column, …) indices in parameter names.
     * @param dimensions   the parameter for the size of each dimension, usually in an array of length 2.
     *                     Length may be different if the caller wants to generalize usage of this class
     *                     to multi-dimensional arrays.
     */
    @SafeVarargs
    @SuppressWarnings({"unchecked", "rawtypes", "varargs"})
    public MatrixParameters(final Class<E> elementType, final String prefix, final String separator,
            final ParameterDescriptor<Integer>... dimensions)
    {
        ArgumentChecks.ensureNonNull ("elementType", elementType);
        ArgumentChecks.ensureNonNull ("prefix",      prefix);
        ArgumentChecks.ensureNonNull ("separator",   separator);
        ArgumentChecks.ensureNonEmpty("dimensions",  dimensions);
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
     * Initializes the fields used for cached values.
     * The values are {@link #zero}, {@link #one} and the {@link #parameters} array.
     * Callers shall assign the returned value to the {@link #parameters} field.
     */
    private <T> ParameterDescriptor<T>[] createCache() {
        try {
            final NumberType type = NumberType.forNumberClass(elementType);
            one  = elementType.cast(type.wrapExact(1));
            zero = elementType.cast(type.wrapExact(0));
        } catch (RuntimeException e) {
            Logging.ignorableException(DefaultParameterValue.LOGGER, MatrixParameters.class, "<init>", e);
            // Ignore - zero and one will be left to null.
        }
        int length = 1;
        for (int i = Math.min(order(), MAX_CACHE_DIMENSION); --i >= 0;) {
            length *= MAX_CACHE_SIZE;
        }
        return new ParameterDescriptor[length];
    }

    /**
     * Returns the type of matrix element values.
     *
     * @return the type of matrix element values.
     */
    @Override
    public final Class<E> getElementType() {
        return elementType;
    }

    /**
     * Indicates that this collection is modifiable (at least by default).
     *
     * @return {@link Mutability#MODIFIABLE} by default.
     * @since 1.6
     */
    @Override
    public Mutability getMutability() {
        return Mutability.MODIFIABLE;
    }

    /**
     * Returns the number of dimensions of the multi-dimensional array for which this builder will create parameters.
     * The number of array dimensions (matrix order) determines the type of objects represented by the parameters:
     *
     * <table class="sis">
     *   <caption>Type of mathematical object implied by the number of array dimensions</caption>
     *   <tr><th>dimension</th> <th>Type</th>   <th>Used with</th></tr>
     *   <tr><td>0</td>         <td>scalar</td> <td></td></tr>
     *   <tr><td>1</td>         <td>vector</td> <td></td></tr>
     *   <tr><td>2</td>         <td>matrix</td> <td>Affine parametric transformation</td></tr>
     *   <tr><td><var>k</var></td><td>array of <var>k</var> dimensions</td></tr>
     * </table>
     *
     * @return the number of dimensions of the multi-dimensional array for which to create parameters.
     */
    public final int order() {
        return dimensions.length;
    }

    /**
     * Verifies that the length of the given array is equal to the matrix order (number of array dimensions).
     */
    private void verifyOrder(final int[] indices) {
        if (indices.length != order()) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.UnexpectedArrayLength_2, order(), indices.length));
        }
    }

    /**
     * Returns the parameter descriptor for the dimension at the given index.
     *
     * @param  i  the dimension index, from 0 inclusive to {@link #order()} exclusive.
     * @return the parameter descriptor for the dimension at the given index.
     *
     * @see #getElementDescriptor(int...)
     * @see #getAllDescriptors(int...)
     */
    public final ParameterDescriptor<Integer> getDimensionDescriptor(final int i) {
        return dimensions[i];
    }

    /**
     * Returns the parameter descriptor for a matrix element at the given indices.
     * The length of the given {@code indices} array shall be equal to the {@linkplain #order() dimension}.
     * That length is usually 2, where {@code indices[0]} is the <var>row</var> index and {@code indices[1]}
     * is the <var>column</var> index.
     *
     * @param  indices  the indices of the matrix element for which to get the descriptor,
     *                  in (<var>row</var>, <var>column</var>, …) order.
     * @return the parameter descriptor for the given matrix element.
     * @throws IllegalArgumentException if the given array does not have the expected length or have illegal value.
     *
     * @see #getDimensionDescriptor(int)
     * @see #getAllDescriptors(int...)
     */
    public final ParameterDescriptor<E> getElementDescriptor(final int... indices) {
        verifyOrder(indices);
        int cacheIndex = 0;
        for (int i=0; i<indices.length; i++) {
            final int index = indices[i];
            ArgumentChecks.ensurePositive("indices", index);
            if (i < MAX_CACHE_DIMENSION && index < MAX_CACHE_SIZE) {
                cacheIndex = (cacheIndex * MAX_CACHE_SIZE) + index;
            } else if (index != 0) {
                return createElementDescriptor(indices);
            }
        }
        ParameterDescriptor<E> param;
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final ParameterDescriptor<E>[] parameters = this.parameters;
        synchronized (parameters) {
            param = parameters[cacheIndex];
        }
        if (param == null) {
            param = createElementDescriptor(indices);
            synchronized (parameters) {
                // Another thread could have created the same descriptor in the meantime.
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
     * Creates a new parameter descriptor for a matrix element at the given indices.
     * This method is invoked by {@link #getElementDescriptor(int[])} when a new descriptor
     * needs to be created.
     *
     * <h4>Default implementation</h4>
     * The default implementation converts the given indices to a parameter name by invoking the
     * {@link #indicesToName(int[])} method, then creates a descriptor for an optional parameter
     * of that name. The default value is given by {@link #getDefaultValue(int[])}.
     *
     * <h4>Subclassing</h4>
     * Subclasses can override this method if they want more control on descriptor properties
     * like identification information, aliases or value domain.
     *
     * @param  indices  the indices of the matrix element for which to create a parameter,
     *                  in (<var>row</var>, <var>column</var>, …) order.
     * @return the parameter descriptor for the given matrix element.
     * @throws IllegalArgumentException if the given array does not have the expected length or have illegal value.
     *
     * @see #indicesToName(int[])
     * @see #getDefaultValue(int[])
     */
    protected ParameterDescriptor<E> createElementDescriptor(final int[] indices) {
        return new DefaultParameterDescriptor<>(properties(indices),
                0, 1, elementType, null, null, getDefaultValue(indices));
    }

    /**
     * Returns the properties of the parameter descriptor at the given indices.
     * This is an alternative to overriding {@link #createElementDescriptor(int[])}
     * when only the identification (name, aliases, <i>etc.</i>) need to be modified.
     *
     * @param  indices  the indices of the matrix element for which to create parameter aliases,
     *                  in (<var>row</var>, <var>column</var>, …) order.
     * @return the parameter descriptor properties for the matrix element at the given indices.
     * @throws IllegalArgumentException if the given array does not have the expected length or have illegal value.
     */
    protected Map<String, ?> properties(final int[] indices) {
        final String name = indicesToName(indices);
        if (name == null) {
            return Map.of();
        }
        final Citation authority = dimensions[0].getName().getAuthority();
        return Map.of(ParameterDescriptor.NAME_KEY, new NamedIdentifier(authority, name));
    }

    /**
     * Returns the parameter descriptor name of a matrix element at the given indices.
     * The returned name shall be parsable by the {@link #nameToIndices(String)} method.
     *
     * <h4>Default implementation</h4>
     * The default implementation requires an {@code indices} array having a length equals to the
     * {@linkplain #order() matrix order}. That length is usually 2, where {@code indices[0]} is
     * the <var>row</var> index and {@code indices[1]} is the <var>column</var> index.
     * Then, this method builds a name with the “{@link #prefix} + <var>row</var> +
     * {@link #separator} + <var>column</var> + …” pattern (e.g. {@code "elt_0_0"}).
     *
     * <h4>Subclassing</h4>
     * If a subclass overrides this method for creating different names, then that subclass
     * should also override the {@link #nameToIndices(String)} method for parsing those names.
     *
     * @param  indices  the indices of the matrix element for which to create a parameter name,
     *                  in (<var>row</var>, <var>column</var>, …) order.
     * @return the parameter descriptor name for the matrix element at the given indices.
     * @throws IllegalArgumentException if the given array does not have the expected length or have illegal value.
     */
    protected String indicesToName(final int[] indices) {
        verifyOrder(indices);
        final var name = new StringBuilder();
        String s = prefix;
        for (final int i : indices) {
            name.append(s).append(i);
            s = separator;
        }
        return name.toString();
    }

    /**
     * Returns the indices of matrix element for the given parameter name, or {@code null} if none.
     * This method is the converse of the {@link #indicesToName(int[])} method, eventually extended
     * for recognizing also the aliases (if any).
     *
     * <h4>Default implementation</h4>
     * The default implementation expects a name matching the “{@link #prefix} + <var>row</var> + {@link #separator} +
     * <var>column</var> + …” pattern and returns an array containing the <var>row</var>, <var>column</var> and other
     * indices, in that order.
     *
     * @param  name  the parameter name to parse.
     * @return indices of the matrix element of the given name, or {@code null} if the name is not recognized.
     * @throws IllegalArgumentException if the name has been recognized but an error occurred while parsing it
     *         (e.g. an {@link NumberFormatException}, which is an {@code IllegalArgumentException} subclass).
     */
    protected int[] nameToIndices(final String name) {
        int s = prefix.length();
        if (!name.regionMatches(true, 0, prefix, 0, s)) {
            return null;
        }
        final int[] indices = new int[order()];
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
     * Returns the default value for the parameter descriptor at the given indices.
     * The default implementation returns 1 if all indices are equals, or 0 otherwise.
     *
     * @param  indices  the indices of the matrix element for which to get the default value,
     *                  in (<var>row</var>, <var>column</var>, …) order.
     * @return the default value for the matrix element at the given indices, or {@code null} if none.
     *
     * @see DefaultParameterDescriptor#getDefaultValue()
     */
    protected E getDefaultValue(final int[] indices) {
        for (int i=1; i<indices.length; i++) {
            if (indices[i] != indices[i-1]) {
                return zero;
            }
        }
        return one;
    }

    /**
     * Returns the descriptor in this group for the specified name.
     *
     * @param  caller      the {@link MatrixParameterValues} instance invoking this method, used only in case of errors.
     * @param  name        the case insensitive name of the parameter to search for.
     * @param  actualSize  the current values of parameters that define the matrix size.
     * @return the parameter for the given name.
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
         * The given name is not a matrix element name. Verify if the requested parameters
         * is one of the parameters that specify the matrix size ("num_row" or "num_col").
         */
        for (final ParameterDescriptor<Integer> param : dimensions) {
            if (IdentifiedObjects.isHeuristicMatchForName(param, name)) {
                return param;
            }
        }
        throw (ParameterNotFoundException) new ParameterNotFoundException(Resources.format(
                Resources.Keys.ParameterNotFound_2, caller.getName(), name), name).initCause(cause);
    }

    /**
     * Returns {@code true} if the given indices are not out-of-bounds.
     * Arguments are in (<var>row</var>, <var>column</var>, …) order.
     *
     * @param  indices     the indices parsed from a parameter name.
     * @param  actualSize  the current values of parameters that define the matrix size.
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
     * Returns all parameters in this group for a matrix of the specified size.
     * The returned array contains all descriptors returned by {@link #getDimensionDescriptor(int)}
     * and {@link #getElementDescriptor(int...)} for all values that exist for the given size.
     *
     * @param  actualSize  the matrix size in ({@code num_row}, {@code num_col}, …) order.
     * @return the matrix parameters, including all elements.
     *
     * @see #getDimensionDescriptor(int)
     * @see #getElementDescriptor(int...)
     */
    public ParameterDescriptor<?>[] getAllDescriptors(final int... actualSize) {
        verifyOrder(actualSize);
        int numElements = 1;
        for (int s : actualSize) {
            ArgumentChecks.ensurePositive("actualSize", s);
            numElements *= s;
        }
        final int order = order();
        final var descriptors = new ParameterDescriptor<?>[order + numElements];
        System.arraycopy(dimensions, 0, descriptors, 0, order);
        final int[] indices = new int[order];
        /*
         * Iterates on all possible index values. Index on the right side (usually the column index)
         * will vary faster, and index on the left side (usually the row index) will vary slowest.
         */
        for (int i=0; i<numElements; i++) {
            descriptors[order + i] = getElementDescriptor(indices);
            for (int j=indices.length; --j >= 0;) {
                if (++indices[j] < actualSize[j]) {
                    break;
                }
                indices[j] = 0;         // We have done a full turn at that dimension. Will increment next dimension.
            }
        }
        return descriptors;
    }

    /**
     * Creates a new instance of parameter group with default values of 1 on the diagonal, and 0 everywhere else.
     * The returned parameter group is extensible, i.e. the number of elements will depend upon the value associated
     * to the parameters that define the matrix size.
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
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link DefaultParameterDescriptorGroup#getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link DefaultParameterDescriptorGroup#getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link DefaultParameterDescriptorGroup#getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link DefaultParameterDescriptorGroup#getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties  the properties to be given to the identified object.
     * @return a new parameter group initialized to the default values.
     */
    public ParameterValueGroup createValueGroup(final Map<String,?> properties) {
        return new MatrixParameterValues<>(properties, this);
    }

    /**
     * Creates a new instance of parameter group initialized to the given matrix.
     * This operation is allowed only for two-dimensional arrays.
     *
     * @param  properties  the properties to be given to the identified object.
     * @param  matrix      the matrix to copy in the new parameter group.
     * @return a new parameter group initialized to the given matrix.
     * @throws IllegalStateException if {@link #order()} does not return 2.
     *
     * @see #toMatrix(ParameterValueGroup)
     */
    public ParameterValueGroup createValueGroup(final Map<String,?> properties, final Matrix matrix) {
        if (order() != 2) {
            throw new IllegalStateException();
        }
        ArgumentChecks.ensureNonNull("matrix", matrix);
        final var values = new MatrixParameterValues<E>(properties, this);
        values.setMatrix(matrix);
        return values;
    }

    /**
     * Constructs a matrix from a group of parameters.
     * This operation is allowed only for two-dimensional arrays.
     *
     * @param  parameters  the group of parameters.
     * @return a matrix constructed from the specified group of parameters.
     * @throws IllegalStateException if {@link #order()} does not return 2.
     * @throws InvalidParameterNameException if a parameter name was not recognized.
     *
     * @see #createValueGroup(Map, Matrix)
     */
    public Matrix toMatrix(final ParameterValueGroup parameters) {
        if (order() != 2) {
            throw new IllegalStateException();
        }
        if (Objects.requireNonNull(parameters) instanceof MatrixParameterValues) {
            return ((MatrixParameterValues) parameters).toMatrix();     // More efficient implementation
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
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(elementType, prefix, separator) ^ Arrays.hashCode(dimensions);
    }

    /**
     * Compares this object with the given object for equality.
     *
     * @param  other  the other object to compare with this object.
     * @return {@code true} if both object are equal.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other.getClass() == getClass()) {
            final var that = (MatrixParameters<?>) other;
            return elementType.equals(that.elementType) &&
                   prefix     .equals(that.prefix)      &&
                   separator  .equals(that.separator)   &&
                   Arrays.equals(dimensions, that.dimensions);
        }
        return false;
    }

    /**
     * Returns a string representation of this object for debugging purposes.
     */
    @Override
    public String toString() {
        if (equals(WKT1)) {
            return "WKT1";
        }
        final int order = order();
        final var names = new String[order + 1];
        for (int i=0; i<order; i++) {
            names[i] = dimensions[i].getName().getCode();
        }
        names[order] = prefix + '*';
        return Strings.toString(getClass(), "descriptors", names);
    }

    /**
     * Invoked on deserialization for restoring the {@link #parameters} array.
     * Then, replaces the deserialized instance by the unique instance if possible.
     *
     * @throws ObjectStreamException if an error occurred.
     */
    final Object readResolve() throws ObjectStreamException {
        if (equals(WKT1))     return WKT1;
        if (equals(EPSG))     return EPSG;
        if (equals(ALPHANUM)) return ALPHANUM;
        parameters = createCache();
        return this;
    }
}
