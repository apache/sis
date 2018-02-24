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
package org.apache.sis.internal.referencing.j2d;

import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.LinearTransform;


/**
 * A two dimensional, linear transform.
 * The intent of this interface is to resolve type conflict in the {@link #inverse()} method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.7
 * @since   0.7
 * @module
 */
public interface LinearTransform2D extends MathTransform2D, LinearTransform {
    /**
     * Returns the inverse transform, which shall be linear and two-dimensional.
     *
     * @return the inverse transform.
     * @throws NoninvertibleTransformException if the transform can not be inverted.
     */
    @Override
    LinearTransform2D inverse() throws NoninvertibleTransformException;
}
