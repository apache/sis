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
package org.apache.sis.referencing.operation.transform;

import org.opengis.referencing.operation.Matrix;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.internal.referencing.provider.GeocentricAffine;
import org.apache.sis.internal.referencing.provider.GeocentricTranslation;
import org.apache.sis.internal.referencing.provider.PositionVector7Param;
import org.apache.sis.internal.referencing.provider.CoordinateFrameRotation;
import org.apache.sis.parameter.Parameters;


/**
 * An affine transform applied on geocentric coordinates.
 * This operation is a step in a <cite>datum shift</cite> transformation chain.
 * It is typically used for geocentric translation, but a rotation can also be applied.
 *
 * <table class="sis">
 *   <caption>Operations using geocentric affine transform</caption>
 *   <tr><th>EPSG name</th>                               <th>EPSG code</th></tr>
 *   <tr><td>Geocentric translations</td>                 <td>1031</td></tr>
 *   <tr><td>Coordinate Frame rotation</td>               <td>1032</td></tr>
 *   <tr><td>Position Vector 7-param. transformation</td> <td>1033</td></tr>
 * </table>
 *
 * Conversions between geographic and geocentric coordinates are <strong>not</strong> part of this operation.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class GeocentricAffineTransform extends ProjectiveTransform {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3588786513463289242L;

    /**
     * The transform type.
     */
    private static final byte TRANSLATION=1, SEVEN_PARAM=2, FRAME_ROTATION=3;

    /**
     * The transform type, as one of {@link #TRANSLATION}, {@link #SEVEN_PARAM} or {@link #FRAME_ROTATION}.
     * We store a code of the type instead than a reference to the parameter descriptor in order to avoid
     * serialization of a full {@link ParameterDescriptorGroup} object.
     */
    private final byte type;

    /**
     * Creates a new geocentric affine transform using the specified matrix
     */
    GeocentricAffineTransform(final Matrix matrix, final byte type) {
        super(matrix);
        this.type = type;
    }

    /**
     * Returns the parameter descriptors for this math transform.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        switch (type) {
            case TRANSLATION:    return GeocentricTranslation  .PARAMETERS;
            case SEVEN_PARAM:    return PositionVector7Param   .PARAMETERS;
            case FRAME_ROTATION: return CoordinateFrameRotation.PARAMETERS;
            default: throw new AssertionError(type); // Should never happen.
        }
    }

    /**
     * Returns the parameters for this math transform.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        final BursaWolfParameters parameters = new BursaWolfParameters(null, null);
        parameters.setPositionVectorTransformation(getMatrix(), Double.POSITIVE_INFINITY);
        final Parameters values = Parameters.castOrWrap(getParameterDescriptors().createValue());
        if (type == FRAME_ROTATION) {
            parameters.reverseRotation();
        }
        values.getOrCreate(GeocentricAffine.TX).setValue(parameters.tX);
        values.getOrCreate(GeocentricAffine.TY).setValue(parameters.tY);
        values.getOrCreate(GeocentricAffine.TZ).setValue(parameters.tZ);
        if (type != TRANSLATION) {
            values.getOrCreate(GeocentricAffine.RX).setValue(parameters.rX);
            values.getOrCreate(GeocentricAffine.RY).setValue(parameters.rY);
            values.getOrCreate(GeocentricAffine.RZ).setValue(parameters.rZ);
            values.getOrCreate(GeocentricAffine.DS).setValue(parameters.dS);
        }
        return values;
    }

    /**
     * Creates an inverse transform using the specified matrix.
     */
    @Override
    final GeocentricAffineTransform createInverse(final Matrix matrix) {
        return new GeocentricAffineTransform(matrix, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int computeHashCode() {
        return super.computeHashCode() + type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsSameClass(final Object object) {
        return (((GeocentricAffineTransform) object).type == type) && super.equalsSameClass(object);
    }
}
