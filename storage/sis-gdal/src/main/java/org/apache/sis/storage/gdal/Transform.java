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
package org.apache.sis.storage.gdal;

import java.util.Collections;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform;


/**
 * A math transform which delegate its work to the {@literal Proj.4} native library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class Transform extends AbstractMathTransform {
    /**
     * The operation method for a transformation between two {@link PJ} instances.
     * The parameter names are taken from
     * <a href="http://proj4.org/development/api.html#pj-transform">Proj.4 API</a>.
     */
    static final OperationMethod METHOD;
    static {
        final ParameterBuilder builder = new ParameterBuilder().setCodeSpace(Citations.PROJ4, Constants.PROJ4);
        final ParameterDescriptor<?>[] p = {
            builder.addName("srcdefn").setDescription("Source (input) coordinate system.").create(String.class, null),
            builder.addName("dstdefn").setDescription("Destination (output) coordinate system.").create(String.class, null)
        };
        final ParameterDescriptorGroup params = builder.addName("pj_transform").createGroup(1, 1, p);
        METHOD = new DefaultOperationMethod(Collections.singletonMap(OperationMethod.NAME_KEY, params.getName()), null, null, params);
    }

    /**
     * The source and target CRS.
     */
    private final PJ source, target;

    /**
     * Whether the source and destination are 2 or 3 dimensional.
     */
    private final boolean source3D, target3D;

    /**
     * The inverse transform, created only when first needed.
     */
    private transient Transform inverse;

    /**
     * Creates a new operation for the given source and target CRS.
     */
    Transform(final PJ source, final boolean source3D, final PJ target, final boolean target3D) {
        this.source   = source;
        this.target   = target;
        this.source3D = source3D;
        this.target3D = target3D;
    }

    /**
     * Returns the parameter descriptors for this math transform.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return METHOD.getParameters();
    }

    /**
     * Returns a copy of the parameter values for this parameterized object.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        final ParameterValueGroup pg = getParameterDescriptors().createValue();
        pg.parameter("srcdefn").setValue(source.getCode().trim());
        pg.parameter("dstdefn").setValue(target.getCode().trim());
        return pg;
    }

    /**
     * Returns the number of source dimensions.
     */
    @Override
    public final int getSourceDimensions() {
        return source3D ? 3 : 2;
    }

    /**
     * Returns the number of target dimensions.
     */
    @Override
    public final int getTargetDimensions() {
        return target3D ? 3 : 2;
    }

    /**
     * Returns {@code true} if this transform is the identity transform. Note that a value of
     * {@code false} does not mean that the transform is not an identity transform, since this
     * case is a bit difficult to determine from Proj.4 API.
     */
    @Override
    public boolean isIdentity() {
        return source3D == target3D && source.equals(target);
    }

    /**
     * Transforms a single point. This method is inefficient; callers should prefer the methods
     * transforming multiple points at once.
     */
    @Override
    public Matrix transform(double[] srcPts, int srcOff,
                            double[] dstPts, int dstOff,
                            boolean derivate) throws TransformException
    {
        transform(srcPts, srcOff, dstPts, dstOff, 1);
        return null;
    }

    /**
     * Transforms an array of coordinate tuples.
     */
    @Override
    public void transform(double[] srcPts, int srcOff,
                          double[] dstPts, int dstOff,
                          int numPts) throws TransformException
    {
        if (source3D != target3D) {
            super.transform(srcPts, srcOff, dstPts, dstOff, numPts);
        } else {
            final int dim = getTargetDimensions();
            if (srcPts != dstPts || srcOff != dstOff) {
                final int length = dim * numPts;
                System.arraycopy(srcPts, srcOff, dstPts, dstOff, length);
            }
            source.transform(target, dim, dstPts, dstOff, numPts);
        }
    }

    /**
     * Returns the inverse transform.
     */
    @Override
    public synchronized MathTransform inverse() {
        if (inverse == null) {
            inverse = new Transform(target, target3D, source, source3D);
            inverse.inverse = this;
        }
        return inverse;
    }
}
