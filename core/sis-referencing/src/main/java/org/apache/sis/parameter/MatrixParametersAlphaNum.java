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
import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.referencing.provider.EPSGName;

import static org.apache.sis.internal.util.CollectionsExt.first;


/**
 * A special case of {@link MatrixParameters} which create EPSG:9624 parameter names and identifiers.
 * The parameters created by this class are close, but not identical, to the EPSG:9624 definition of
 * {@code "A0"}, {@code "A1"}, {@code "A2"}, {@code "B0"}, {@code "B1"} and {@code "B2"}.
 * The differences are:
 *
 * <ul>
 *   <li>EPSG:9624 is only for matrices of size 3×3 and consequently does not have {@code "num_row"} and
 *       {@code "num_col"} parameters. This class extends the definition to matrices of arbitrary size
 *       and consequently accepts {@code "num_row"} and {@code "num_col"} as optional parameters.</li>
 *   <li>EPSG:9624 is restricted to affine matrices and consequently define parameters only for the two
 *       first rows. This class accepts also parameters for the last row (namely {@code "C0"}, {@code "C1"}
 *       and {@code "C2"} in a 3×3 matrices).</li>
 * </ul>
 *
 * Because of the above-cited extensions, this class is not named like "EPSG matrix parameters", but rater
 * like "Alphanumeric matrix parameters"
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class MatrixParametersAlphaNum extends MatrixParameters {
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
    MatrixParametersAlphaNum(final ParameterDescriptor<Integer> numRow, final ParameterDescriptor<Integer> numCol) {
        super(numRow, numCol);
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
     * (e.g. {@code "EPSG:8641"}) if it exists. See {@link org.apache.sis.internal.referencing.provider.Affine}
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
        GenericName name = first(wkt.getAlias());
        if (name == null) {
            /*
             * Outside the range of names (e.g. more than 26 rows or more than 10 columns).
             * Returns the OGC name as-is.
             */
            return wkt;
        }
        final Map<String,Object> properties = new HashMap<String,Object>(6);
        /*
         * Declare the EPSG identifier only for A0, A1, A2, B0, B1 and B2.
         */
        if (isEPSG(indices)) {
            name = EPSGName.create(name.tip().toString()); // Put the name in EPSG namespace.
            final int code = (indices[0] == 0 ? Constants.EPSG_A0 : Constants.EPSG_B0) + indices[1];
            properties.put(ParameterDescriptor.IDENTIFIERS_KEY, EPSGName.identifier(code));
        }
        properties.put(ParameterDescriptor.NAME_KEY, name);
        properties.put(ParameterDescriptor.ALIAS_KEY, wkt.getName());
        return new DefaultParameterDescriptor<Double>(properties, 0, 1, Double.class, null, null, wkt.getDefaultValue());
    }

    /**
     * On deserialization, replaces the deserialized instance by the unique instance.
     */
    @Override
    Object readResolve() throws ObjectStreamException {
        return equals(ALPHANUM) ? ALPHANUM : this;
    }
}
