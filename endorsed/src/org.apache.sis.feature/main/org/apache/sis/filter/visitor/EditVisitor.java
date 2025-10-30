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
package org.apache.sis.filter.visitor;

import org.opengis.filter.Literal;
import org.opengis.filter.FilterFactory;


/**
 * Visitor used to copy expressions and filters with same parameterized types.
 * This class can be used when some filters need to be recreated with the same types
 * but potentially different values. For example a change of {@link Literal} value
 * requires to recreate all parents in the filter graph.
 *
 * <h2>Partially implemented factory</h2>
 * {@code EditVisitor} relaxes the usual factory API contract by allowing unsupported factory
 * methods to return {@code null} instead of throwing an {@link UnsupportedOperationException}.
 * A null value is interpreted as an instruction to continue to use the old filter or expression,
 * without replacing it by a new instance created by the {@linkplain #factory}.
 * By contrast, an {@link UnsupportedOperationException} causes the copy operation to fail.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of resources expected by the filters and expressions.
 * @param  <G>  base class of geometry objects.
 * @param  <T>  base class of temporal objects.
 */
public class EditVisitor<R,G,T> extends CopyVisitor<R,R,G,T> {
    /**
     * Creates a new edit visitor with given factory.
     * If the {@code force} argument is {@code false}, then the factory is used for
     * creating new filters and expressions only when at least one operand changed.
     *
     * @param  factory  the factory to use for creating the new filters and expressions.
     * @param  force    whether to force new filters or expressions even when existing instances could be reused.
     */
    public EditVisitor(final FilterFactory<R,G,T> factory, final boolean force) {
        super(factory, force, force);
    }
}
