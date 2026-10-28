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

import java.util.Objects;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.internal.Resources;


/**
 * Thrown when a coordinate operation cannot be executed because its {@code MathTransform} cannot be created.
 * This {@code TransformException} always has a {@link FactoryException} as its cause.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.8
 * @since   1.8
 */
public class TransformFactoryException extends TransformException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3075278492210160308L;

    /**
     * Creates a new exception with the given cause.
     * This constructor provides a default message.
     *
     * @param  cause  the cause (cannot be {@code null}).
     */
    public TransformFactoryException(FactoryException cause) {
        this(Resources.format(Resources.Keys.CanNotCreateTransform), cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param  message  the details message, or {@code null} if none.
     * @param  cause    the cause (cannot be {@code null}).
     */
    public TransformFactoryException(String message, FactoryException cause) {
        super(message, Objects.requireNonNull(cause));
    }

    /**
     * Returns the cause of this exception.
     *
     * @return  the factory exception which is the cause of this exception.
     */
    @Override
    public FactoryException getCause() {
        return (FactoryException) super.getCause();
    }
}
