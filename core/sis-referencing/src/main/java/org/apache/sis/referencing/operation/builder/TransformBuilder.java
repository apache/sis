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
package org.apache.sis.referencing.operation.builder;

import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.util.FactoryException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;


/**
 * Creates a transform which will map approximately the given source positions to the given target positions.
 * The transform may be a linear approximation the minimize the errors in a <cite>least square</cite> sense,
 * or a more accurate transform using a localization grid.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public abstract class TransformBuilder {
    /**
     * For subclass constructors.
     */
    protected TransformBuilder() {
    }

    /**
     * Creates a transform from the source points to the target points.
     *
     * @param  factory  the factory to use for creating the transform, or {@code null} for the default factory.
     * @return the transform from source to target points.
     * @throws FactoryException if the transform can not be created,
     *         for example because the target points have not be specified.
     */
    public abstract MathTransform create(final MathTransformFactory factory) throws FactoryException;

    /**
     * Returns the given factory if non-null, or the default factory otherwise.
     */
    static MathTransformFactory nonNull(MathTransformFactory factory) {
        if (factory == null) {
            factory = DefaultFactories.forBuildin(MathTransformFactory.class, DefaultMathTransformFactory.class);
        }
        return factory;
    }
}
