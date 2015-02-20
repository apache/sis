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

import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.internal.referencing.provider.EPSGName;

import static org.apache.sis.internal.util.CollectionsExt.first;


/**
 * The values for a group of matrix parameters. This value group provides the same extensibility than
 * the one described in {@link TensorValues}, plus the capability to adapt the group name and identifier
 * according the matrix size. Consequently, this {@code ParameterValueGroup} is also its own mutable
 * {@code ParameterDescriptorGroup}.
 *
 * <p>This class is where we implement the "magic" for using the EPSG:9624 parameter names when the
 * matrix is compliant with the EPSG restrictions, and using the OGC parameter names in other cases.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class MatrixValues extends TensorValues<Double> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2169049712239591110L;

    /**
     * Constructs a new group of matrix parameters.
     */
    MatrixValues() {
        super(EPSGName.properties((short) 9624, Affine.NAME, Constants.AFFINE), TensorParameters.EPSG);
    }

    /**
     * Constructs a copy of the given matrix parameters.
     * If {@code clone} is true, the new group will be a clone of the given group.
     * If {@code clone} is false, the new group will be initialized to default values.
     */
    private MatrixValues(final MatrixValues other, final boolean clone) {
        super(other, clone);
    }

    /**
     * Returns {@code true} if this matrix is compliant with the EPSG definition.
     * This is {@code true} if this matrix is affine and of size 3×3.
     */
    private boolean isEPSG() {
        return isAffine(Affine.EPSG_DIMENSION + 1);
    }

    /**
     * Returns the object to use for computing parameter descriptions.
     * We can use the EPSG descriptors only if this matrix size matches EPSG expectations.
     *
     * @return Object to use for computing parameter descriptions.
     */
    @Override
    TensorParameters<Double> provider() {
        return isEPSG() ? super.provider() : TensorParameters.WKT1;
    }

    /**
     * Returns the EPSG name if this matrix is compliant with the EPSG definition,
     * or the OGC name otherwise.
     *
     * @see #getAlias()
     */
    @Override
    public Identifier getName() {
        return isEPSG() ? super.getName() : (Identifier) first(super.getAlias());
    }

    /**
     * Returns the EPSG identifier if this matrix is compliant with the EPSG definition,
     * or an empty set otherwise.
     */
    @Override
    public Set<Identifier> getIdentifiers() {
        return isEPSG() ? super.getIdentifiers() : Collections.emptySet();
    }

    /**
     * Do not returns the OGC name if we already returned it with {@link #getName()}.
     */
    @Override
    public Collection<GenericName> getAlias() {
        return isEPSG() ? super.getAlias() : Collections.emptySet();
    }

    /**
     * Returns a clone of this group.
     */
    @Override
    public ParameterValueGroup clone() {
        return new MatrixValues(this, true);
    }

    /**
     * Returns a new group initialized to default values.
     */
    @Override
    public ParameterValueGroup createValue() {
        return new MatrixValues(this, false);
    }
}
