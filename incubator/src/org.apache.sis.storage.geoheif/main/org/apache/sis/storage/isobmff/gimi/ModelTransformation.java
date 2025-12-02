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
package org.apache.sis.storage.isobmff.gimi;

import java.util.UUID;
import java.io.IOException;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.util.resources.Errors;


/**
 * Coefficients of the matrix that defines the "grid to <abbr>CRS</abbr>" coordinate conversion.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ModelTransformation extends FullBox {
    /**
     * Numerical representation of the {@code "mtxf"} box type.
     */
    public static final int BOXTYPE = ((((('m' << 8) | 't') << 8) | 'x') << 8) | 'f';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * The most significant bits of the <abbr>UUID</abbr> as a long integer.
     * It was used in an older version of the <abbr>GIMI</abbr> specification.
     * Should not be used anymore, but nevertheless kept for compatibility.
     */
    public static final long UUID_HIGH_BITS = 0x763cf838_b630_440bL;

    /**
     * The <abbr>UUID</abbr> that identify this extension.
     * It was used in an older version of the <abbr>GIMI</abbr> specification.
     * Should not be used anymore, but nevertheless kept for compatibility.
     */
    public static final UUID EXTENDED_TYPE = new UUID(UUID_HIGH_BITS, 0x84f8_be44bf9910afL);

    /**
     * Returns the identifier of this extension.
     * This value is fixed to {@link #EXTENDED_TYPE}.
     */
    @Override
    public final UUID extendedType() {
        return EXTENDED_TYPE;
    }

    /**
     * The matrix coefficients as an array of 6 or 12 elements, for the 2D and 3D case respectively.
     */
    public final double[] coefficients;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws DataStoreContentException if the box version is unsupported.
     */
    public ModelTransformation(final Reader reader) throws IOException, DataStoreContentException {
        super(reader);
        requireVersionZero();
        final int n = ((flags & 0x01) != 0) ? 6 : 12;
        coefficients = reader.input.readDoubles(n);
    }

    /**
     * Returns the transform encoded in this box.
     *
     * @throws DataStoreContentException if the array length is inconsistent.
     */
    public MathTransform toMathTransform() throws DataStoreContentException {
        final int dimension = (coefficients.length > 6) ? 3 : 2;
        final MatrixSIS m = Matrices.createIdentity(dimension + 1);
        int k = 0;
        for (int j=0; j<dimension; j++) {           // n   rows
            for (int i=0; i<=dimension; i++) {      // n+1 columns
                m.setElement(j, i, coefficients[k++]);
            }
        }
        if (k != coefficients.length) {
            throw new DataStoreContentException(Errors.format(Errors.Keys.UnexpectedArrayLength_2, k, coefficients.length));
        }
        return MathTransforms.linear(m);
    }
}
