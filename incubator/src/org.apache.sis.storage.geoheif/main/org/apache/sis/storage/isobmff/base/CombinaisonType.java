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
package org.apache.sis.storage.isobmff.base;

import java.io.IOException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.Reader;


/**
 * An element in the {@code ExtendedType} box indicating the brands required for decoding the file.
 *
 * <h4>Container</h4>
 * The container can be an {@link ExtendedType} box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class CombinaisonType extends Box {
    /**
     * Numerical representation of the {@code "tyco"} box type.
     */
    public static final int BOXTYPE = ((((('t' << 8) | 'y') << 8) | 'c') << 8) | 'o';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * List of compatible brands.
     */
    @Interpretation(Type.FOURCC)
    public final int[] compatibleBrands;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     */
    public CombinaisonType(final Reader reader) throws IOException {
        compatibleBrands = reader.readRemainingInts();
    }
}
